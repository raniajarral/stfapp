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

        fetchCollectorReports()
    }

    private fun fetchCollectorReports() {
        // Get the branch name from the intent
        val branchName = intent.getStringExtra("branchName") ?: return

        // Query collectors belonging to the specific branch
        firestore.collection("collectors")
            .whereEqualTo("branch", branchName) // Filter by the branch
            .get()
            .addOnSuccessListener { collectorDocuments ->
                val reports = mutableListOf<CollectorReport>()

                for (document in collectorDocuments) {
                    val collectorName = document.getString("name") ?: "Unknown Collector"
                    val activeCount = document.getLong("totalActiveClients")?.toInt() ?: 0
                    val inactiveCount = document.getLong("totalInactiveClients")?.toInt() ?: 0
                    val totalLoan = document.getDouble("totalActiveAmount") ?: 0.0
                    val totalPayable = document.getDouble("totalActivePayable") ?: 0.0
                    val totalBadClients = document.getLong("totalBadClients")?.toInt() ?: 0

                    val report = CollectorReport(
                        collectorName,
                        activeCount,
                        inactiveCount,
                        totalLoan,
                        totalPayable,
                        totalBadClients
                    )

                    reports.add(report)
                }

                reportAdapter = CollectorReportAdapter(reports)
                collectorsRecyclerView.adapter = reportAdapter
            }
            .addOnFailureListener { e ->
                Log.w("ReportActivity", "Error fetching collector reports", e)
                Toast.makeText(this, "Error fetching report data", Toast.LENGTH_SHORT).show()
            }
    }

}
