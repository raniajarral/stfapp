package com.example.stfapp

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class InvestmentScreenActivity : AppCompatActivity() {
    private lateinit var clientNameTextView: TextView
    private lateinit var dateTextView: TextView
    private lateinit var dueDateTextView: TextView
    private lateinit var amountEditText: EditText
    private lateinit var payableEditText: EditText
    private lateinit var saveButton: Button

    private lateinit var firestore: FirebaseFirestore
    private lateinit var clientName: String

    private val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_investment_screen)

        firestore = FirebaseFirestore.getInstance()
        clientNameTextView = findViewById(R.id.clientNameSlot)
        dateTextView = findViewById(R.id.dateValue)
        dueDateTextView = findViewById(R.id.dueDateValue)
        amountEditText = findViewById(R.id.amountValue)
        payableEditText = findViewById(R.id.payableValue)
        saveButton = findViewById(R.id.saveButton)

        clientName = intent.getStringExtra("clientName") ?: "Unknown Client"

        if (clientName == "Unknown Client") {
            Log.e("InvestmentScreenActivity", "Client name is missing")
            Toast.makeText(this, "Error: Missing client name", Toast.LENGTH_SHORT).show()
            return
        }

        clientNameTextView.text = clientName

        updateDateDisplay()
        updateDueDate()

        dateTextView.setOnClickListener {
            showDatePicker { selectedDate ->
                calendar.time = selectedDate
                updateDateDisplay()
                updateDueDate()
            }
        }

        amountEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                calculatePayableAmount()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        saveButton.setOnClickListener {
            saveInvestment()
        }
    }

    private fun updateDateDisplay() {
        dateTextView.text = dateFormat.format(calendar.time)
    }

    private fun updateDueDate() {
        val dueDateCalendar = calendar.clone() as Calendar
        dueDateCalendar.add(Calendar.DAY_OF_MONTH, 5)
        dueDateTextView.text = dateFormat.format(dueDateCalendar.time)
    }

    private fun showDatePicker(onDateSelected: (Date) -> Unit) {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                onDateSelected(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun calculatePayableAmount() {
        val amountText = amountEditText.text.toString()
        val amount = amountText.toDoubleOrNull() ?: 0.0

        val percentage = when {
            amount in 1.0..500.0 -> 0.10
            amount in 501.0..1000.0 -> 0.09
            amount in 1001.0..3000.0 -> 0.08
            amount in 3001.0..5000.0 -> 0.07
            amount in 5001.0..10000.0 -> 0.06
            amount in 10001.0..20000.0 -> 0.05
            amount in 20001.0..50000.0 -> 0.04
            else -> 0.0
        }

        val payableAmount = amount + (amount * percentage)
        payableEditText.setText(String.format("%.2f", payableAmount))
    }

    private fun saveInvestment() {
        val date = dateTextView.text.toString()
        val dueDate = dueDateTextView.text.toString()
        val amount = amountEditText.text.toString().toDoubleOrNull() ?: 0.0
        val payable = payableEditText.text.toString().toDoubleOrNull() ?: 0.0

        if (amount > 0) {
            val investmentData = mapOf(
                "date" to date,
                "dueDate" to dueDate,
                "amount" to amount,
                "payable" to payable
            )

            firestore.collection("clients")
                .whereEqualTo("name", clientName)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val clientDoc = querySnapshot.documents[0]
                        val clientId = clientDoc.id

                        // Update the existing client document with the new investment data
                        val clientRef = firestore.collection("clients").document(clientId)

                        clientRef.update(
                            mapOf(
                                "status" to "active",
                                "lastInvestmentDate" to date,
                                "dueDate" to dueDate,
                                "amount" to amount,
                                "payable" to payable
                            ) + investmentData
                        )
                            .addOnSuccessListener {
                                Log.d("InvestmentScreenActivity", "Client updated successfully.")
                                // Redirect to CollectorDetailsActivity or any other action
                                val intent = Intent(this, CollectorDetailsActivity::class.java)
                                startActivity(intent)
                                finish() // Optional: Close the current activity
                            }
                            .addOnFailureListener { e ->
                                Log.e("InvestmentScreenActivity", "Error updating client: ${e.message}")
                                Toast.makeText(this, "Error updating client: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "Client not found", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("InvestmentScreenActivity", "Error finding client: ${e.message}")
                    Toast.makeText(this, "Error finding client: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
        }
    }
}
