package com.example.stfapp

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import com.google.firebase.firestore.FirebaseFirestore

class BranchDetailsActivity : AppCompatActivity() {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var collectorsListView: ListView
    private lateinit var createCollectorButton: Button
    private lateinit var branch: String
    private lateinit var generateReportButton: AppCompatImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_branch_details)

        firestore = FirebaseFirestore.getInstance()
        collectorsListView = findViewById(R.id.collectorsListView)
        createCollectorButton = findViewById(R.id.createCollectorButton)
        generateReportButton = findViewById(R.id.generateReportButton)

        branch = intent.getStringExtra("branchName") ?: return

        fetchCollectors(branch)

        createCollectorButton.setOnClickListener {
            val intent = Intent(this, CreateCollectorActivity::class.java)
            intent.putExtra("branchName", branch)
            startActivity(intent)
        }

        collectorsListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val selectedCollectorName = collectorsListView.getItemAtPosition(position) as String
            fetchCollectorId(branch, selectedCollectorName) { collectorId ->
                val intent = Intent(this, CollectorDetailsActivity::class.java)
                intent.putExtra("collectorId", collectorId)
                startActivity(intent)
            }
        }

        collectorsListView.onItemLongClickListener = AdapterView.OnItemLongClickListener { _, _, position, _ ->
            val selectedCollectorName = collectorsListView.getItemAtPosition(position) as String
            showDeleteConfirmationDialog(branch, selectedCollectorName)
            true
        }

        generateReportButton.setOnClickListener {
            val intent = Intent(this, ReportActivity::class.java)
            intent.putExtra("branchName", branch) // Pass the branch name to the report activity
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

    private fun fetchCollectorId(branchName: String, collectorName: String, callback: (String) -> Unit) {
        firestore.collection("collectors")
            .whereEqualTo("branch", branchName)
            .whereEqualTo("name", collectorName)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val collectorId = documents.documents[0].id
                    callback(collectorId)
                } else {
                    Toast.makeText(this, "Collector not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.w("BranchDetailsActivity", "Error fetching collector ID", e)
                Toast.makeText(this, "Error fetching collector ID", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteConfirmationDialog(branchName: String, collectorName: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete Collector")
            .setMessage("Are you sure you want to delete this collector and all associated clients?")
            .setPositiveButton("Delete") { _, _ ->
                deleteCollectorAndClients(branchName, collectorName)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteCollectorAndClients(branchName: String, collectorName: String) {
        fetchCollectorId(branchName, collectorName) { collectorId ->
            // Delete associated clients
            firestore.collection("clients")
                .whereEqualTo("collectorId", collectorId)
                .get()
                .addOnSuccessListener { clientDocuments ->
                    for (clientDocument in clientDocuments) {
                        clientDocument.reference.delete()
                    }

                    // Delete the collector
                    firestore.collection("collectors")
                        .document(collectorId)
                        .delete()
                        .addOnSuccessListener {
                            Toast.makeText(this, "Collector and associated clients deleted", Toast.LENGTH_SHORT).show()
                            fetchCollectors(branchName) // Refresh the list after deletion
                        }
                        .addOnFailureListener { e ->
                            Log.e("BranchDetailsActivity", "Error deleting collector", e)
                            Toast.makeText(this, "Error deleting collector", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("BranchDetailsActivity", "Error fetching associated clients", e)
                    Toast.makeText(this, "Error deleting associated clients", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
