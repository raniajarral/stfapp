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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_client)

        firestore = FirebaseFirestore.getInstance()
        clientNameEditText = findViewById(R.id.clientNameEditText)
        saveClientButton = findViewById(R.id.saveButton)

        saveClientButton.setOnClickListener {
            val clientName = clientNameEditText.text.toString().trim()
            if (clientName.isEmpty()) {
                Toast.makeText(this, "Client name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val client = hashMapOf(
                "name" to clientName,
                "status" to "inactive" // Default status
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
        }
    }
}
