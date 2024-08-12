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

class MainActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var branchesListView: ListView
    private lateinit var createBranchButton: Button
    private lateinit var branchesAdapter: ArrayAdapter<String>
    private val branchesList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        firestore = FirebaseFirestore.getInstance()
        branchesListView = findViewById(R.id.branchesListView)
        createBranchButton = findViewById(R.id.createBranchButton)

        branchesAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, branchesList)
        branchesListView.adapter = branchesAdapter

        createBranchButton.setOnClickListener {
            val intent = Intent(this, CreateBranchActivity::class.java)
            startActivity(intent)
        }

        fetchBranches()
        setBranchClickListener()
    }

    override fun onResume() {
        super.onResume()
        fetchBranches()
    }

    private fun fetchBranches() {
        firestore.collection("branches") // Updated to root level branches collection
            .get()
            .addOnSuccessListener { documents ->
                branchesList.clear()
                for (document in documents) {
                    val branchName = document.getString("name") ?: "Unnamed Branch"
                    branchesList.add(branchName)
                    Log.d("MainActivity", "Branch fetched: $branchName")
                }
                branchesAdapter.notifyDataSetChanged()
                Log.d("MainActivity", "Branches list updated: $branchesList")
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching branches", Toast.LENGTH_SHORT).show()
                Log.e("MainActivity", "Error fetching branches", e)
            }
    }

    private fun setBranchClickListener() {
        branchesListView.setOnItemClickListener { _, _, position, _ ->
            val selectedBranchName = branchesList[position]
            val intent = Intent(this, BranchDetailsActivity::class.java)
            intent.putExtra("branchName", selectedBranchName)
            startActivity(intent)
        }
    }
}
