package com.example.stfapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class CreateClientActivity : AppCompatActivity() {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var clientNameEditText: EditText
    private lateinit var saveClientButton: Button
    private lateinit var collectorId: String // To store the collector ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_client)

        firestore = FirebaseFirestore.getInstance()
        clientNameEditText = findViewById(R.id.clientNameEditText)
        saveClientButton = findViewById(R.id.saveButton)

        // Retrieve collector ID from the intent
        collectorId = intent.getStringExtra("COLLECTOR_ID") ?: ""

        saveClientButton.setOnClickListener {
            val clientName = clientNameEditText.text.toString().trim()
            if (clientName.isEmpty()) {
                Toast.makeText(this, "Client name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (collectorId.isEmpty()) {
                Toast.makeText(this, "Collector ID not found", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check if a client with the same name already exists under the same collector
            firestore.collection("clients")
                .whereEqualTo("name", clientName)
                .whereEqualTo("collectorId", collectorId)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (querySnapshot.documents.isEmpty()) {
                        // No client with the same name found, add the new client
                        val client = hashMapOf(
                            "name" to clientName,
                            "status" to "inactive", // Default status
                            "collectorId" to collectorId // Store collectorId directly
                        )

                        firestore.collection("clients")
                            .add(client)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Client added", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error adding client", Toast.LENGTH_SHORT).show()
                                e.printStackTrace()
                            }
                    } else {
                        // A client with the same name already exists
                        Toast.makeText(this, "Client with this name already exists under this collector", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error checking client existence", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
        }
    }
}
