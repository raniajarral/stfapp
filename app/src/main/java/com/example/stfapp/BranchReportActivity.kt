package com.example.stfapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class BranchReportActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var branchesRecyclerView: RecyclerView
    private lateinit var branchReportAdapter: BranchReportAdapter
    private val reportsList = mutableListOf<BranchReport>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_branch_report)

        firestore = FirebaseFirestore.getInstance()
        branchesRecyclerView = findViewById(R.id.branchReportRecyclerView)

        // Initialize adapter with empty list
        branchReportAdapter = BranchReportAdapter(reportsList)
        branchesRecyclerView.layoutManager = LinearLayoutManager(this)
        branchesRecyclerView.adapter = branchReportAdapter

        fetchBranchReports()
    }

    private fun fetchBranchReports() {
        firestore.collection("branches")
            .get()
            .addOnSuccessListener { branches ->
                val branchReports = mutableListOf<BranchReport>()

                for (branch in branches) {
                    val branchName = branch.getString("name") ?: "Unnamed Branch"
                    fetchCollectorsForBranch(branchName) { report ->
                        branchReports.add(report)
                        // Update the adapter with new reports
                        branchReportAdapter.updateReports(branchReports)
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching branches", Toast.LENGTH_SHORT).show()
                Log.e("BranchReportActivity", "Error fetching branches", e)
            }
    }

    private fun fetchCollectorsForBranch(branchName: String, callback: (BranchReport) -> Unit) {
        firestore.collection("collectors")
            .whereEqualTo("branch", branchName)
            .get()
            .addOnSuccessListener { collectors ->
                var totalActiveClients = 0
                var totalInactiveClients = 0
                var totalActiveAmount = 0.0
                var totalActivePayable = 0.0
                var totalBadClients = 0
                var totalCollectors = 0

                for (collector in collectors) {
                    totalActiveClients += collector.getLong("totalActiveClients")?.toInt() ?: 0
                    totalInactiveClients += collector.getLong("totalInactiveClients")?.toInt() ?: 0
                    totalActiveAmount += collector.getDouble("totalActiveAmount") ?: 0.0
                    totalActivePayable += collector.getDouble("totalActivePayable") ?: 0.0
                    totalBadClients += collector.getLong("totalBadClients")?.toInt() ?: 0
                    totalCollectors += 1 // Count each collector
                }

                val branchReport = BranchReport(
                    branchName,
                    totalActiveClients,
                    totalInactiveClients,
                    totalActiveAmount,
                    totalActivePayable,
                    totalBadClients,
                    totalCollectors // Add total collectors to the report
                )

                // Optionally update branch data
                updateBranchData(branchName, totalActiveClients, totalInactiveClients, totalActiveAmount, totalActivePayable, totalBadClients, totalCollectors)

                callback(branchReport)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching collectors", Toast.LENGTH_SHORT).show()
                Log.e("BranchReportActivity", "Error fetching collectors", e)
            }
    }

    private fun updateBranchData(branchName: String, totalActiveClients: Int, totalInactiveClients: Int, totalActiveAmount: Double, totalActivePayable: Double, totalBadClients: Int, totalCollectors: Int) {
        firestore.collection("branches")
            .whereEqualTo("name", branchName)
            .get()
            .addOnSuccessListener { branches ->
                if (branches.isEmpty) {
                    Log.e("BranchReportActivity", "No branch found for name: $branchName")
                    return@addOnSuccessListener
                }

                val branchId = branches.documents[0].id

                val branchData = mapOf(
                    "totalActiveClients" to totalActiveClients,
                    "totalInactiveClients" to totalInactiveClients,
                    "totalActiveAmount" to totalActiveAmount,
                    "totalActivePayable" to totalActivePayable,
                    "totalBadClients" to totalBadClients,
                    "totalCollectors" to totalCollectors // Add total collectors to branch data
                )

                firestore.collection("branches").document(branchId)
                    .set(branchData, SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d("BranchReportActivity", "Branch data updated successfully for $branchName")
                    }
                    .addOnFailureListener { e ->
                        Log.e("BranchReportActivity", "Error updating branch data", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("BranchReportActivity", "Error fetching branch by name", e)
            }
    }
}
