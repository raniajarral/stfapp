package com.example.stfapp

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var firestore: FirebaseFirestore
    private lateinit var branchesListView: ListView
    private lateinit var createBranchButton: Button
    private lateinit var generateBranchReportButton: ImageButton
    private lateinit var toolbar: Toolbar
    private lateinit var navButton: ImageButton
    private lateinit var wisdom_button: ImageButton
    private val branchesList = mutableListOf<String>()
    private var selectedBranchName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI elements
        toolbar = findViewById(R.id.custom_toolbar)
        navButton = findViewById(R.id.nav_button)
        wisdom_button = findViewById(R.id.wisdom_button)
        setSupportActionBar(toolbar)

        firestore = FirebaseFirestore.getInstance()
        branchesListView = findViewById(R.id.branchesListView)
        createBranchButton = findViewById(R.id.createBranchButton)
        generateBranchReportButton = findViewById(R.id.generateBranchReportButton)

        branchesListView.setOnItemClickListener { _, _, position, _ ->
            selectedBranchName = branchesList[position]
            val intent = Intent(this, BranchDetailsActivity::class.java)
            intent.putExtra("branchName", selectedBranchName)
            startActivity(intent)
        }

        branchesListView.setOnItemLongClickListener { _, _, position, _ ->
            selectedBranchName = branchesList[position]
            AlertDialog.Builder(this).apply {
                setTitle("Delete Branch")
                setMessage("Are you sure you want to delete the branch '$selectedBranchName' and all associated collectors and clients?")
                setPositiveButton("Delete") { _, _ -> deleteBranch(selectedBranchName!!) }
                setNegativeButton("Cancel", null)
            }.show()
            true
        }

        // In your Activity or Fragment where you set up the Toolbar
        val toolbar = findViewById<Toolbar>(R.id.custom_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = ""

        navButton.setOnClickListener {
            val intent = Intent(this, ReportCalculationActivity::class.java)
            startActivity(intent)
        }

        wisdom_button.setOnClickListener {
            val intent = Intent(this, WisdomActivity::class.java)
            startActivity(intent)
        }

        createBranchButton.setOnClickListener {
            val intent = Intent(this, CreateBranchActivity::class.java)
            startActivity(intent)
        }

        sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
        Log.d("MainActivity", "Checking login state: ${sharedPreferences.getBoolean("is_logged_in", false)}")
        if (!sharedPreferences.getBoolean("is_logged_in", false)) {
            // Redirect to LoginActivity if not logged in
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            // Continue with MainActivity as usual
            fetchBranches()
            setupGenerateReportButton()
        }
    }

    override fun onResume() {
        super.onResume()
        fetchBranches()
    }

    private fun fetchBranches() {
        firestore.collection("branches")
            .get()
            .addOnSuccessListener { documents ->
                branchesList.clear()
                for (document in documents) {
                    val branchName = document.getString("name") ?: "Unnamed Branch"
                    branchesList.add(branchName)
                }
                branchesListView.adapter = object : android.widget.BaseAdapter() {
                    override fun getCount(): Int = branchesList.size

                    override fun getItem(position: Int): Any = branchesList[position]

                    override fun getItemId(position: Int): Long = position.toLong()

                    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                        val view: View = convertView ?: LayoutInflater.from(parent?.context)
                            .inflate(R.layout.list_item_card, parent, false)

                        val branchName = branchesList[position]
                        val cardText = view.findViewById<TextView>(R.id.card_text)
                        cardText.text = branchName

                        return view
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching branches", Toast.LENGTH_SHORT).show()
                Log.e("MainActivity", "Error fetching branches", e)
            }
    }

    private fun deleteBranch(branchName: String) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val collectors = firestore.collection("collectors")
                    .whereEqualTo("branch", branchName)
                    .get()
                    .await()

                for (collector in collectors) {
                    val collectorId = collector.id
                    deleteClients(collectorId)

                    firestore.collection("collectors")
                        .document(collectorId)
                        .delete()
                        .await()
                }

                val branchQuery = firestore.collection("branches")
                    .whereEqualTo("name", branchName)
                    .get()
                    .await()

                for (branchDoc in branchQuery.documents) {
                    firestore.collection("branches")
                        .document(branchDoc.id)
                        .delete()
                        .await()
                }

                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Branch and associated data deleted", Toast.LENGTH_SHORT).show()
                    fetchBranches()
                }

            } catch (e: Exception) {
                Log.e("MainActivity", "Error deleting branch and associated data", e)
            }
        }
    }

    private suspend fun deleteClients(collectorId: String) {
        try {
            val clients = firestore.collection("clients")
                .whereEqualTo("collectorId", collectorId)
                .get()
                .await()

            for (client in clients) {
                firestore.collection("clients")
                    .document(client.id)
                    .delete()
                    .await()
            }

        } catch (e: Exception) {
            Log.e("MainActivity", "Error deleting clients", e)
        }
    }

    private fun setupGenerateReportButton() {
        generateBranchReportButton.setOnClickListener {
            val intent = Intent(this, BranchReportActivity::class.java)
            startActivity(intent)
        }
    }
}

