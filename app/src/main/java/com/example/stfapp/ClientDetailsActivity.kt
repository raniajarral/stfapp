package com.example.stfapp

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class ClientDetailsActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var clientName: String

    private lateinit var clientNameTextView: TextView
    private lateinit var amountTextView: EditText
    private lateinit var dateTextView: TextView
    private lateinit var payableTextView: TextView
    private lateinit var dueDateTextView: TextView
    private lateinit var collectionEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var markAsBadButton: Button
    private lateinit var balanceTextView: TextView
    private lateinit var editButton: Button
    private val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_details)

        firestore = FirebaseFirestore.getInstance()

        // Initialize UI elements
        clientNameTextView = findViewById(R.id.clientNameSlot)
        amountTextView = findViewById(R.id.amountValue)
        dateTextView = findViewById(R.id.dateValue)
        balanceTextView = findViewById(R.id.balanceValue)
        payableTextView = findViewById(R.id.payableValue)
        dueDateTextView = findViewById(R.id.dueDateValue)
        collectionEditText = findViewById(R.id.collectionValue)
        saveButton = findViewById(R.id.saveButton)
        markAsBadButton = findViewById(R.id.badButton)
        editButton = findViewById(R.id.editButton)

        // Initially set amountTextView as disabled
        amountTextView.isEnabled = false
        dateTextView.isEnabled = false
        clientNameTextView.isEnabled = false

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

        // Add a TextWatcher to amountTextView
        amountTextView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val amountStr = s.toString()
                val amount = amountStr.toDoubleOrNull() ?: 0.0
                calculatePayableAmount(amount)

                val collectionText = s.toString()
                val collection = collectionText.toDoubleOrNull() ?: 0.0

                // Get the payable amount from the TextView
                val payableText = payableTextView.text.toString()
                val payable = payableText.toDoubleOrNull() ?: 0.0


            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        collectionEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val collectionText = s.toString()
                val collection = collectionText.toDoubleOrNull() ?: 0.0

                // Get the payable amount from the TextView
                val payableText = payableTextView.text.toString()
                val payable = payableText.toDoubleOrNull() ?: 0.0

                // Calculate the balance
                val balance = calculateBalance(payable, collection)

                // Update the balance TextView
                balanceTextView.text = String.format(Locale.getDefault(), "%.2f", balance)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

    }

    private fun calculateBalance(payable: Double, collection: Double): Double {
        return payable - collection
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
        amountTextView.setText(amount.toString())
        dateTextView.text = date
        payableTextView.text = payable.toString()
        dueDateTextView.text = dueDate

    }

    private fun saveClientDetails() {
        val newCollection = collectionEditText.text.toString().toDoubleOrNull() ?: 0.0
        val currentAmount = amountTextView.text.toString().toDoubleOrNull() ?: 0.0
        val currentPayable = payableTextView.text.toString().toDoubleOrNull() ?: 0.0
        val newDate = dateTextView.text.toString()
        val newDueDate = dueDateTextView.text.toString()

        // Deduct the collection amount from both the current amount and payable
        val updatedAmount = currentAmount - newCollection
        val updatedPayable = currentPayable - newCollection

        // Determine the client's status based on the payable amount
        val updatedStatus = if (updatedPayable <= 0) "inactive" else "active"

        // Prepare the updated data
        val updatedClient = mapOf(
            "amount" to updatedAmount,
            "payable" to updatedPayable,
            "date" to newDate,
            "dueDate" to newDueDate,
            "status" to updatedStatus // Update status
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
        clientNameTextView.isEnabled = true
        val isEditMode = !amountTextView.isEnabled
        amountTextView.isEnabled = isEditMode
        dateTextView.isEnabled = isEditMode
        editButton.text = if (isEditMode) "Editing" else "Edit"

        // Set up DatePicker if in edit mode
        if (isEditMode) {
            dateTextView.setOnClickListener {
                showDatePicker()
            }
        } else {
            dateTextView.setOnClickListener(null)
        }
    }

    private fun calculatePayableAmount(amount: Double) {
        val amountText = amountTextView.text.toString()  // Make sure to use the correct TextView ID
        val amount = amountText.toDoubleOrNull() ?: 0.0

        // Determine the percentage based on the amount
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

        // Calculate payable based on the percentage
        val payable = amount + (amount * percentage)

        // Update the payable TextView
        payableTextView.text = String.format(Locale.getDefault(), "%.2f", payable)
    }


    private fun updateDueDate() {
        val selectedDate = dateTextView.text.toString()
        if (selectedDate.isNotEmpty()) {
            val parsedDate = dateFormat.parse(selectedDate)
            val dueDateCalendar = Calendar.getInstance()
            dueDateCalendar.time = parsedDate
            dueDateCalendar.add(Calendar.DAY_OF_MONTH, 5)
            dueDateTextView.text = dateFormat.format(dueDateCalendar.time)
        }
    }

    private fun showDatePicker() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDay: Int ->
                val selectedDate = "${selectedMonth + 1}/$selectedDay/$selectedYear"
                dateTextView.text = selectedDate
                updateDueDate()
            },
            year, month, day
        )
        datePickerDialog.show()
    }
}
