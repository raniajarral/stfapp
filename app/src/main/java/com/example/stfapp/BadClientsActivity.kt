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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bad_clients)

        firestore = FirebaseFirestore.getInstance()
        badClientsListView = findViewById(R.id.badClientsListView)

        // Fetch bad clients when the activity starts
        fetchBadClients()
    }

    override fun onResume() {
        super.onResume()
        // Refresh the list when the activity is resumed
        fetchBadClients()
    }

    private fun fetchBadClients() {
        firestore.collection("clients")
            .whereEqualTo("status", "bad")
            .get()
            .addOnSuccessListener { documents ->
                val badClients = mutableListOf<String>()
                for (document in documents) {
                    val clientName = document.getString("name") ?: "Unnamed Client"
                    badClients.add(clientName)
                }
                val badAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, badClients)
                badClientsListView.adapter = badAdapter
            }
            .addOnFailureListener { e ->
                Log.w("BadClientsActivity", "Error fetching bad clients", e)
                Toast.makeText(this, "Error fetching bad clients", Toast.LENGTH_SHORT).show()
            }
    }
}
