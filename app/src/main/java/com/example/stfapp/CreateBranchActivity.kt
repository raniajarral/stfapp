package com.example.stfapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class CreateBranchActivity : AppCompatActivity() {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var branchNameEditText: EditText
    private lateinit var saveBranchButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_branch)

        firestore = FirebaseFirestore.getInstance()
        branchNameEditText = findViewById(R.id.branchNameEditText)
        saveBranchButton = findViewById(R.id.saveBranchButton)

        saveBranchButton.setOnClickListener {
            val branchName = branchNameEditText.text.toString().trim()
            if (branchName.isEmpty()) {
                Toast.makeText(this, "Branch name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val branch = hashMapOf("name" to branchName)

            // Add branch to the branches collection
            firestore.collection("branches")
                .add(branch)
                .addOnSuccessListener {
                    Toast.makeText(this, "Branch added successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error adding branch", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
        }
    }
}
