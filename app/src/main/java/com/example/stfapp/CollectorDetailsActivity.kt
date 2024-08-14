package com.example.stfapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.SearchView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.google.firebase.firestore.FirebaseFirestore

class CollectorDetailsActivity : AppCompatActivity() {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var activeClientsListView: ListView
    private lateinit var inactiveClientsListView: ListView
    private lateinit var createClientButton: Button
    private lateinit var badClientsButton: AppCompatImageButton
    private lateinit var searchView: SearchView // Add this line
    private lateinit var collectorId: String

    private val activeClients = mutableListOf<String>()
    private val inactiveClients = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_collector_details)

        firestore = FirebaseFirestore.getInstance()
        activeClientsListView = findViewById(R.id.activeClientsListView)
        inactiveClientsListView = findViewById(R.id.inactiveClientsListView)
        createClientButton = findViewById(R.id.createClientButton)
        badClientsButton = findViewById(R.id.badClientsButton)
        searchView = findViewById(R.id.clientSearchView) // Initialize SearchView

        // Retrieve collectorId from the Intent
        collectorId = intent.getStringExtra("collectorId") ?: ""

        // Set up button listeners
        createClientButton.setOnClickListener {
            val intent = Intent(this, CreateClientActivity::class.java)
            intent.putExtra("COLLECTOR_ID", collectorId)
            startActivity(intent)
        }

        badClientsButton.setOnClickListener {
            val intent = Intent(this, BadClientsActivity::class.java)
            intent.putExtra("collectorId", collectorId)
            startActivity(intent)
        }

        // Set up SearchView listener
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterClients(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterClients(newText)
                return true
            }
        })

        // Fetch and display clients
        fetchClients()

        // Handle item long clicks for active clients
        activeClientsListView.setOnItemLongClickListener { parent, view, position, id ->
            val selectedClientName = parent.getItemAtPosition(position) as String
            showDeleteConfirmationDialog(selectedClientName)
            true
        }

        // Handle item long clicks for inactive clients
        inactiveClientsListView.setOnItemLongClickListener { parent, view, position, id ->
            val selectedClientName = parent.getItemAtPosition(position) as String
            showDeleteConfirmationDialog(selectedClientName)
            true
        }

        // Handle item clicks for active clients
        activeClientsListView.setOnItemClickListener { parent, view, position, id ->
            val selectedClientName = parent.getItemAtPosition(position) as String
            navigateToClientDetails(selectedClientName)
        }

        // Handle item clicks for inactive clients
        inactiveClientsListView.setOnItemClickListener { parent, view, position, id ->
            val selectedClientName = parent.getItemAtPosition(position) as String
            navigateToInvestmentScreen(selectedClientName)
        }
    }

    override fun onResume() {
        super.onResume()
        fetchClients()
    }

    private fun fetchClients() {
        // Fetch active clients
        firestore.collection("clients")
            .whereEqualTo("status", "active")
            .whereEqualTo("collectorId", collectorId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("CollectorDetailsActivity", "Error fetching active clients", e)
                    Toast.makeText(this, "Error fetching active clients", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                activeClients.clear()
                for (document in snapshots?.documents ?: emptyList()) {
                    val clientName = document.getString("name") ?: "Unnamed Client"
                    activeClients.add(clientName)
                }
                updateActiveClientListView()
                updateCollectorStats()
            }

        // Fetch inactive clients
        firestore.collection("clients")
            .whereEqualTo("status", "inactive")
            .whereEqualTo("collectorId", collectorId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("CollectorDetailsActivity", "Error fetching inactive clients", e)
                    Toast.makeText(this, "Error fetching inactive clients", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                inactiveClients.clear()
                for (document in snapshots?.documents ?: emptyList()) {
                    val clientName = document.getString("name") ?: "Unnamed Client"
                    inactiveClients.add(clientName)
                }
                updateInactiveClientListView()
                updateCollectorStats()
            }
    }

    private fun updateActiveClientListView() {
        val activeAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, activeClients)
        activeClientsListView.adapter = activeAdapter
    }

    private fun updateInactiveClientListView() {
        val inactiveAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, inactiveClients)
        inactiveClientsListView.adapter = inactiveAdapter
    }

    private fun filterClients(query: String?) {
        val filteredActiveClients = activeClients.filter {
            it.contains(query ?: "", ignoreCase = true)
        }
        val filteredInactiveClients = inactiveClients.filter {
            it.contains(query ?: "", ignoreCase = true)
        }
        val activeAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, filteredActiveClients)
        val inactiveAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, filteredInactiveClients)
        activeClientsListView.adapter = activeAdapter
        inactiveClientsListView.adapter = inactiveAdapter
    }

    private fun updateCollectorStats() {
        if (collectorId.isEmpty()) return

        firestore.collection("clients")
            .whereEqualTo("status", "active")
            .whereEqualTo("collectorId", collectorId)
            .get()
            .addOnSuccessListener { clientDocuments ->
                var totalActiveAmount = 0.0
                var totalActivePayable = 0.0

                for (clientDoc in clientDocuments) {
                    val amount = clientDoc.getDouble("amount") ?: 0.0
                    val payable = clientDoc.getDouble("payable") ?: 0.0

                    totalActiveAmount += amount
                    totalActivePayable += payable
                }

                val activeCount = activeClientsListView.count
                val inactiveCount = inactiveClientsListView.count

                val collectorUpdates: MutableMap<String, Any> = hashMapOf(
                    "totalActiveClients" to activeCount,
                    "totalInactiveClients" to inactiveCount,
                    "totalActiveAmount" to totalActiveAmount,
                    "totalActivePayable" to totalActivePayable
                )

                firestore.collection("collectors").document(collectorId)
                    .update(collectorUpdates)
                    .addOnSuccessListener {
                        Log.d("CollectorDetailsActivity", "Collector stats successfully updated")
                    }
                    .addOnFailureListener { e ->
                        Log.w("CollectorDetailsActivity", "Error updating collector stats", e)
                        Toast.makeText(this, "Error updating collector stats", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.w("CollectorDetailsActivity", "Error fetching active clients", e)
                Toast.makeText(this, "Error fetching active clients", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteConfirmationDialog(clientName: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete Client")
            .setMessage("Are you sure you want to delete $clientName?")
            .setPositiveButton("Delete") { dialog, _ ->
                deleteClient(clientName)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteClient(clientName: String) {
        firestore.collection("clients")
            .whereEqualTo("name", clientName)
            .whereEqualTo("collectorId", collectorId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "Client not found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                for (document in documents) {
                    firestore.collection("clients").document(document.id)
                        .delete()
                        .addOnSuccessListener {
                            Toast.makeText(this, "Client deleted successfully", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Log.w("CollectorDetailsActivity", "Error deleting client", e)
                            Toast.makeText(this, "Error deleting client", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.w("CollectorDetailsActivity", "Error fetching client", e)
                Toast.makeText(this, "Error fetching client", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateToClientDetails(clientName: String) {
        val intent = Intent(this, ClientDetailsActivity::class.java)
        intent.putExtra("clientName", clientName)
        startActivity(intent)
    }

    private fun navigateToInvestmentScreen(clientName: String) {
        val intent = Intent(this, InvestmentScreenActivity::class.java)
        intent.putExtra("clientName", clientName)
        startActivity(intent)
    }
}
