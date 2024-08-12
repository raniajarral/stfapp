package com.example.stfapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class CreateCollectorActivity : AppCompatActivity() {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var collectorNameEditText: EditText
    private lateinit var saveCollectorButton: Button
    private lateinit var branch: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_collector)

        firestore = FirebaseFirestore.getInstance()
        collectorNameEditText = findViewById(R.id.collectorNameEditText)
        saveCollectorButton = findViewById(R.id.saveCollectorButton)
        branch = intent.getStringExtra("branchName") ?: return

        saveCollectorButton.setOnClickListener {
            val collectorName = collectorNameEditText.text.toString().trim()
            if (collectorName.isEmpty()) {
                Toast.makeText(this, "Collector name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val collector = hashMapOf(
                "name" to collectorName,
                "branch" to branch // Store the branch name in the collector document
            )

            firestore.collection("collectors")
                .add(collector)
                .addOnSuccessListener {
                    Toast.makeText(this, "Collector added", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error adding collector", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
        }
    }
}
