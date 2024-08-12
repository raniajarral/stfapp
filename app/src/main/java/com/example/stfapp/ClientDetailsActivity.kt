package com.example.stfapp

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class ClientDetailsActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var clientName: String

    private lateinit var clientNameTextView: TextView
    private lateinit var amountTextView: TextView
    private lateinit var dateTextView: TextView
    private lateinit var payableTextView: TextView
    private lateinit var dueDateTextView: TextView
    private lateinit var collectionEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var markAsBadButton: Button
    private lateinit var editButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_details)

        firestore = FirebaseFirestore.getInstance()

        // Initialize UI elements
        clientNameTextView = findViewById(R.id.clientNameSlot)
        amountTextView = findViewById(R.id.amountValue)
        dateTextView = findViewById(R.id.dateValue)
        payableTextView = findViewById(R.id.payableValue)
        dueDateTextView = findViewById(R.id.dueDateValue)
        collectionEditText = findViewById(R.id.collectionValue)
        saveButton = findViewById(R.id.saveButton)
        markAsBadButton = findViewById(R.id.badButton)
        editButton = findViewById(R.id.editButton)

        // Get the client name from the intent
        clientName = intent.getStringExtra("clientName") ?: ""

        Log.d("ClientDetailsActivity", "Received Client Name: $clientName")

        if (clientName.isEmpty()) {
            Log.e("ClientDetailsActivity", "Client name is missing")
            Toast.makeText(this, "Error: Missing client name", Toast.LENGTH_SHORT).show()
            return
        }

        fetchClientDetails()

        // Set up button listeners
        saveButton.setOnClickListener { saveClientDetails() }
        markAsBadButton.setOnClickListener { markClientAsBad() }
        editButton.setOnClickListener { toggleEditMode() }
    }

    private fun fetchClientDetails() {
        firestore.collection("clients")
            .whereEqualTo("name", clientName)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    Log.d("ClientDetailsActivity", "DocumentSnapshot data: ${document.data}")
                    populateFields(document)
                } else {
                    Log.d("ClientDetailsActivity", "No such document")
                    Toast.makeText(this, "No such client", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.d("ClientDetailsActivity", "Error fetching document: ", exception)
                Toast.makeText(this, "Error fetching document: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun populateFields(document: DocumentSnapshot) {
        val clientName = document.getString("name") ?: "N/A"
        val amount = document.getDouble("amount") ?: 0.0
        val date = document.getString("date") ?: "N/A"
        val payable = document.getDouble("payable") ?: 0.0
        val dueDate = document.getString("dueDate") ?: "N/A"
        val collection = document.getDouble("collection") ?: 0.0

        clientNameTextView.text = clientName
        amountTextView.text = amount.toString()
        dateTextView.text = date
        payableTextView.text = payable.toString()
        dueDateTextView.text = dueDate
        collectionEditText.setText(collection.toString())

        // Disable editing if needed
        collectionEditText.isEnabled = false
    }

    private fun saveClientDetails() {
        val collectionValue = collectionEditText.text.toString().toDoubleOrNull() ?: 0.0

        val updatedClient = mapOf(
            "collection" to collectionValue
            // Include other fields to update if needed
        )

        firestore.collection("clients")
            .whereEqualTo("name", clientName)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val clientDoc = querySnapshot.documents[0]
                    val clientId = clientDoc.id

                    // Update the existing client document with the new data
                    firestore.collection("clients").document(clientId)
                        .update(updatedClient)
                        .addOnSuccessListener {
                            Log.d("ClientDetailsActivity", "Client details successfully updated")
                            Toast.makeText(this, "Client details successfully updated", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { exception ->
                            Log.d("ClientDetailsActivity", "Error updating client details: ", exception)
                            Toast.makeText(this, "Error updating client details: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Client not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.d("ClientDetailsActivity", "Error finding client: ${exception.message}")
                Toast.makeText(this, "Error finding client: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun markClientAsBad() {
        firestore.collection("clients")
            .whereEqualTo("name", clientName)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val clientDoc = querySnapshot.documents[0]
                    val clientId = clientDoc.id

                    firestore.collection("clients").document(clientId)
                        .update("status", "bad")
                        .addOnSuccessListener {
                            Log.d("ClientDetailsActivity", "Client status successfully updated to 'bad'")
                            Toast.makeText(this, "Client status successfully updated to 'bad'", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { exception ->
                            Log.d("ClientDetailsActivity", "Error updating client status: ", exception)
                            Toast.makeText(this, "Error updating client status: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Client not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.d("ClientDetailsActivity", "Error finding client: ${exception.message}")
                Toast.makeText(this, "Error finding client: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun toggleEditMode() {
        val isEditMode = collectionEditText.isEnabled
        collectionEditText.isEnabled = !isEditMode
        editButton.text = if (isEditMode) "Edit" else "Save"
    }
}
