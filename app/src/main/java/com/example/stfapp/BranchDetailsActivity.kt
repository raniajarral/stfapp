package com.example.stfapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class BranchDetailsActivity : AppCompatActivity() {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var collectorsListView: ListView
    private lateinit var createCollectorButton: Button
    private lateinit var branch: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_branch_details)

        firestore = FirebaseFirestore.getInstance()
        collectorsListView = findViewById(R.id.collectorsListView)
        createCollectorButton = findViewById(R.id.createCollectorButton)

        branch = intent.getStringExtra("branchName") ?: return

        fetchCollectors(branch)

        createCollectorButton.setOnClickListener {
            val intent = Intent(this, CreateCollectorActivity::class.java)
            intent.putExtra("branchName", branch)
            startActivity(intent)
        }

        collectorsListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val selectedCollector = collectorsListView.getItemAtPosition(position) as String
            Log.d("BranchDetailsActivity", "Collector selected: $selectedCollector")
            val intent = Intent(this, CollectorDetailsActivity::class.java)
            intent.putExtra("branchName", branch)
            intent.putExtra("collectorName", selectedCollector)
            startActivity(intent)
        }
    }

    private fun fetchCollectors(branchName: String) {
        firestore.collection("collectors")
            .whereEqualTo("branch", branchName)
            .get()
            .addOnSuccessListener { documents ->
                val collectors = mutableListOf<String>()
                for (document in documents) {
                    val collectorName = document.getString("name") ?: "Unnamed Collector"
                    collectors.add(collectorName)
                }
                if (collectors.isEmpty()) {
                    Toast.makeText(this, "No collectors found", Toast.LENGTH_SHORT).show()
                }
                val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, collectors)
                collectorsListView.adapter = adapter
            }
            .addOnFailureListener { e ->
                Log.w("BranchDetailsActivity", "Error fetching collectors", e)
                Toast.makeText(this, "Error fetching collectors", Toast.LENGTH_SHORT).show()
            }
    }
}
