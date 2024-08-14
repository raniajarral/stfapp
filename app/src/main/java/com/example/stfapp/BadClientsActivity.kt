package com.example.stfapp

import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class BadClientsActivity : AppCompatActivity() {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var badClientsListView: ListView
    private lateinit var collectorId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bad_clients)

        firestore = FirebaseFirestore.getInstance()
        badClientsListView = findViewById(R.id.badClientsListView)

        // Retrieve collectorId from the Intent
        collectorId = intent.getStringExtra("collectorId") ?: ""

        // Fetch and display bad clients
        fetchBadClients()

        // Handle item clicks for bad clients
        badClientsListView.setOnItemClickListener { parent, view, position, id ->
            val selectedClientName = parent.getItemAtPosition(position) as String
            // Handle client click here (e.g., show client details or open another activity)
        }
    }

    private fun fetchBadClients() {
        val badClients = mutableListOf<String>()

        // Fetch bad clients based on collectorId
        firestore.collection("clients")
            .whereEqualTo("status", "bad")
            .whereEqualTo("collectorId", collectorId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("BadClientsActivity", "Error fetching bad clients", e)
                    Toast.makeText(this, "Error fetching bad clients", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                badClients.clear()
                for (document in snapshots?.documents ?: emptyList()) {
                    val clientName = document.getString("name") ?: "Unnamed Client"
                    badClients.add(clientName)
                }
                val badClientsAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, badClients)
                badClientsListView.adapter = badClientsAdapter
            }
    }
}
