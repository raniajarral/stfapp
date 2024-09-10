package com.example.stfapp

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class ReportActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var collectorsRecyclerView: RecyclerView
    private lateinit var reportAdapter: CollectorReportAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        firestore = FirebaseFirestore.getInstance()
        collectorsRecyclerView = findViewById(R.id.collectorsRecyclerView)

        collectorsRecyclerView.layoutManager = LinearLayoutManager(this)
        reportAdapter = CollectorReportAdapter(emptyList())
        collectorsRecyclerView.adapter = reportAdapter

        fetchAndCalculateProfitForCollectors()
    }

    private fun fetchAndCalculateProfitForCollectors() {
        val branchName = intent.getStringExtra("branchName") ?: return

        firestore.collection("collectors")
            .whereEqualTo("branch", branchName)
            .get()
            .addOnSuccessListener { collectorDocuments ->
                val collectorIds = mutableListOf<String>()
                val collectorMap = mutableMapOf<String, CollectorReport>()

                for (document in collectorDocuments) {
                    val collectorId = document.id
                    val collectorName = document.getString("name") ?: "Unknown Collector"
                    val activeCount = document.getLong("totalActiveClients")?.toInt() ?: 0
                    val inactiveCount = document.getLong("totalInactiveClients")?.toInt() ?: 0
                    val totalLoan = document.getDouble("totalActiveAmount") ?: 0.0
                    val totalPayable = document.getDouble("totalActivePayable") ?: 0.0
                    val totalBadClients = document.getLong("totalBadClients")?.toInt() ?: 0

                    collectorIds.add(collectorId)
                    collectorMap[collectorId] = CollectorReport(
                        collectorName,
                        activeCount,
                        inactiveCount,
                        totalLoan,
                        totalPayable,
                        totalBadClients,
                        0.0 // Placeholder for totalProfit
                    )
                }

                if (collectorIds.isNotEmpty()) {
                    fetchProfitRecordsForClients(collectorIds, collectorMap)
                } else {
                    reportAdapter = CollectorReportAdapter(collectorMap.values.toList())
                    collectorsRecyclerView.adapter = reportAdapter
                }
            }
            .addOnFailureListener { e ->
                Log.w("ReportActivity", "Error fetching collector reports", e)
                Toast.makeText(this, "Error fetching report data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchProfitRecordsForClients(collectorIds: List<String>, collectorMap: MutableMap<String, CollectorReport>) {
        firestore.collection("clients")
            .whereIn("collectorId", collectorIds)
            .get()
            .addOnSuccessListener { clientDocuments ->
                val clientIds = clientDocuments.map { it.id }

                if (clientIds.isNotEmpty()) {
                    firestore.collection("ProfitRecord")
                        .whereIn("clientId", clientIds)
                        .get()
                        .addOnSuccessListener { profitDocuments ->
                            val profitMap = mutableMapOf<String, Double>()
                            for (document in profitDocuments) {
                                val clientId = document.getString("clientId") ?: continue
                                val profit = document.getDouble("profit") ?: 0.0
                                profitMap[clientId] = (profitMap[clientId] ?: 0.0) + profit
                            }

                            for ((collectorId, report) in collectorMap) {
                                val collectorClientIds = clientDocuments
                                    .filter { it.getString("collectorId") == collectorId }
                                    .map { it.id }
                                val totalProfit = collectorClientIds.sumOf { profitMap[it] ?: 0.0 }
                                report.totalProfit = totalProfit

                                // Update the collector document with the new totalProfit
                                firestore.collection("collectors").document(collectorId)
                                    .update("totalProfit", totalProfit)
                                    .addOnSuccessListener {
                                        Log.d("ReportActivity", "Successfully updated totalProfit for collector $collectorId")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.w("ReportActivity", "Error updating totalProfit for collector $collectorId", e)
                                    }
                            }

                            reportAdapter = CollectorReportAdapter(collectorMap.values.toList())
                            collectorsRecyclerView.adapter = reportAdapter
                        }
                        .addOnFailureListener { e ->
                            Log.w("ReportActivity", "Error fetching profit records", e)
                            Toast.makeText(this, "Error fetching profit records", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    reportAdapter = CollectorReportAdapter(collectorMap.values.toList())
                    collectorsRecyclerView.adapter = reportAdapter
                }
            }
            .addOnFailureListener { e ->
                Log.w("ReportActivity", "Error fetching clients", e)
                Toast.makeText(this, "Error fetching clients", Toast.LENGTH_SHORT).show()
            }
    }
}
