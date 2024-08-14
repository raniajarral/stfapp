package com.example.stfapp

import android.content.Intent
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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var branchesListView: ListView
    private lateinit var createBranchButton: Button
    private lateinit var generateBranchReportButton: ImageButton
    private lateinit var branchesAdapter: ArrayAdapter<String>
    private val branchesList = mutableListOf<String>()
    private var selectedBranchName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        firestore = FirebaseFirestore.getInstance()
        branchesListView = findViewById(R.id.branchesListView)
        createBranchButton = findViewById(R.id.createBranchButton)
        generateBranchReportButton = findViewById(R.id.generateBranchReportButton)

        branchesAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, branchesList)
        branchesListView.adapter = branchesAdapter

        createBranchButton.setOnClickListener {
            val intent = Intent(this, CreateBranchActivity::class.java)
            startActivity(intent)
        }

        fetchBranches()
        setBranchClickListener()
        setBranchLongClickListener()
        setupGenerateReportButton()
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
                    Log.d("MainActivity", "Branch fetched: $branchName")
                }
                branchesAdapter.notifyDataSetChanged()
                Log.d("MainActivity", "Branches list updated: $branchesList")
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching branches", Toast.LENGTH_SHORT).show()
                Log.e("MainActivity", "Error fetching branches", e)
            }
    }

    private fun setBranchClickListener() {
        branchesListView.setOnItemClickListener { _, _, position, _ ->
            selectedBranchName = branchesList[position]
            val intent = Intent(this, BranchDetailsActivity::class.java)
            intent.putExtra("branchName", selectedBranchName)
            startActivity(intent)
        }
    }

    private fun setBranchLongClickListener() {
        branchesListView.onItemLongClickListener = AdapterView.OnItemLongClickListener { _, _, position, _ ->
            selectedBranchName = branchesList[position]
            AlertDialog.Builder(this).apply {
                setTitle("Delete Branch")
                setMessage("Are you sure you want to delete the branch '$selectedBranchName' and all associated collectors and clients?")
                setPositiveButton("Delete") { _, _ -> deleteBranch(selectedBranchName!!) }
                setNegativeButton("Cancel", null)
            }.show()
            true
        }
    }

    private fun deleteBranch(branchName: String) {
        // Start a coroutine to delete the branch, collectors, and clients
        GlobalScope.launch(Dispatchers.IO) {
            try {
                // Fetch all collectors linked to the branch
                val collectors = firestore.collection("collectors")
                    .whereEqualTo("branch", branchName)
                    .get()
                    .await()

                // Delete all clients linked to each collector
                for (collector in collectors) {
                    val collectorId = collector.id
                    deleteClients(collectorId)

                    // Delete the collector
                    firestore.collection("collectors")
                        .document(collectorId)
                        .delete()
                        .await()
                }

                // Delete the branch
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

                // Update UI on the main thread
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Branch and associated data deleted", Toast.LENGTH_SHORT).show()
                    fetchBranches()  // Refresh the branches list
                }

            } catch (e: Exception) {
                Log.e("MainActivity", "Error deleting branch and associated data", e)
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Error deleting branch", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun deleteClients(collectorId: String) {
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
    }

    private fun setupGenerateReportButton() {
        generateBranchReportButton.setOnClickListener {
            if (selectedBranchName != null) {
                val intent = Intent(this, BranchReportActivity::class.java)
                intent.putExtra("branchName", selectedBranchName)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Please select a branch first", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
