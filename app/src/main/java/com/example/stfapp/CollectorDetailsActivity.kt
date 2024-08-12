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

class CollectorDetailsActivity : AppCompatActivity() {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var activeClientsListView: ListView
    private lateinit var inactiveClientsListView: ListView
    private lateinit var createClientButton: Button
    private lateinit var badClientsButton: Button // Step 1: Add the button variable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_collector_details)

        firestore = FirebaseFirestore.getInstance()
        activeClientsListView = findViewById(R.id.activeClientsListView)
        inactiveClientsListView = findViewById(R.id.inactiveClientsListView)
        createClientButton = findViewById(R.id.createClientButton)
        badClientsButton = findViewById(R.id.badClientsButton) // Step 2: Find the button by ID

        // Set up the Create Client button
        createClientButton.setOnClickListener {
            val intent = Intent(this, CreateClientActivity::class.java)
            startActivity(intent)
        }

        // Set up the Bad Clients button
        badClientsButton.setOnClickListener { // Step 3: Set the click listener
            val intent = Intent(this, BadClientsActivity::class.java) // Step 4: Start BadClientsActivity
            startActivity(intent)
        }

        // Fetch clients when the activity starts
        fetchClients()

        // Handle clicks on active clients
        activeClientsListView.setOnItemClickListener { parent, view, position, id ->
            val selectedClientName = parent.getItemAtPosition(position) as String
            val intent = Intent(this, ClientDetailsActivity::class.java)
            intent.putExtra("clientName", selectedClientName)
            startActivity(intent)
        }

        // Handle clicks on inactive clients
        inactiveClientsListView.setOnItemClickListener { parent, view, position, id ->
            val selectedClientName = parent.getItemAtPosition(position) as String
            val intent = Intent(this, InvestmentScreenActivity::class.java)
            intent.putExtra("clientName", selectedClientName)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh client lists when the activity is resumed
        fetchClients()
    }

    private fun fetchClients() {
        // Fetch active clients
        firestore.collection("clients")
            .whereEqualTo("status", "active")
            .get()
            .addOnSuccessListener { documents ->
                val activeClients = mutableListOf<String>()
                for (document in documents) {
                    val clientName = document.getString("name") ?: "Unnamed Client"
                    activeClients.add(clientName)
                }
                val activeAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, activeClients)
                activeClientsListView.adapter = activeAdapter
            }
            .addOnFailureListener { e ->
                Log.w("CollectorDetailsActivity", "Error fetching active clients", e)
                Toast.makeText(this, "Error fetching active clients", Toast.LENGTH_SHORT).show()
            }

        // Fetch inactive clients
        firestore.collection("clients")
            .whereEqualTo("status", "inactive")
            .get()
            .addOnSuccessListener { documents ->
                val inactiveClients = mutableListOf<String>()
                for (document in documents) {
                    val clientName = document.getString("name") ?: "Unnamed Client"
                    inactiveClients.add(clientName)
                }
                val inactiveAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, inactiveClients)
                inactiveClientsListView.adapter = inactiveAdapter
            }
            .addOnFailureListener { e ->
                Log.w("CollectorDetailsActivity", "Error fetching inactive clients", e)
                Toast.makeText(this, "Error fetching inactive clients", Toast.LENGTH_SHORT).show()
            }
    }
}
