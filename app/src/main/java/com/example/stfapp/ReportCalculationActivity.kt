package com.example.stfapp

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import com.google.firebase.Timestamp
import android.widget.Button
import android.widget.EditText
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.google.firebase.firestore.FirebaseFirestore
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class ReportCalculationActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var dropdownMenu: AutoCompleteTextView
    private lateinit var startDateEditText: EditText
    private lateinit var endDateEditText: EditText
    private lateinit var saveButton: Button

    private val dateFormat = SimpleDateFormat("MM-dd-yyyy", Locale.getDefault())
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_calculation)

        firestore = FirebaseFirestore.getInstance()
        dropdownMenu = findViewById(R.id.dropdown_menu)
        startDateEditText = findViewById(R.id.start_timestamp_edit_text)
        endDateEditText = findViewById(R.id.end_timestamp_edit_text)
        saveButton = findViewById(R.id.save_button)

        // Set up dropdown menu with search feature
        setupDropdownMenu()

        // Set up date pickers
        startDateEditText.setOnClickListener {
            showDatePicker { date -> startDateEditText.setText(date) }
        }

        endDateEditText.setOnClickListener {
            showDatePicker { date -> endDateEditText.setText(date) }
        }

        // Save button click listener
        saveButton.setOnClickListener {
            val selectedItem = dropdownMenu.text.toString()
            val startDate = startDateEditText.text.toString()
            val endDate = endDateEditText.text.toString()

            if (selectedItem.isEmpty() || startDate.isEmpty() || endDate.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            calculateProfit(selectedItem, startDate, endDate)
        }
    }

    private fun setupDropdownMenu() {
        firestore.collection("branches").get().addOnSuccessListener { branchDocuments ->
            firestore.collection("collectors").get().addOnSuccessListener { collectorDocuments ->
                val items = mutableListOf<String>()
                val branches = mutableMapOf<String, String>()  // Map branch names to IDs
                val collectors = mutableMapOf<String, String>()  // Map collector names to IDs

                for (document in branchDocuments) {
                    val branchName = document.getString("name")
                    val branchId = document.id
                    if (branchName != null) {
                        items.add("Branch: $branchName")
                        branches[branchName] = branchId
                    }
                }
                for (document in collectorDocuments) {
                    val collectorName = document.getString("name")
                    val collectorId = document.id
                    if (collectorName != null) {
                        items.add("Collector: $collectorName")
                        collectors[collectorName] = collectorId
                    }
                }

                val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, items)
                dropdownMenu.setAdapter(adapter)

                dropdownMenu.setOnItemClickListener { _, _, position, _ ->
                    val selectedItem = dropdownMenu.adapter.getItem(position) as String
                    // Handle item selection if needed
                }
            }
        }.addOnFailureListener { e ->
            Log.e("ReportCalculation", "Error fetching data", e)
            Toast.makeText(this, "Error fetching data", Toast.LENGTH_SHORT).show()
        }
    }


    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val selectedDate = dateFormat.format(calendar.time)
                onDateSelected(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private fun calculateProfit(selectedItem: String, startDate: String, endDate: String) {
        val start = try {
            dateFormat.parse(startDate)
        } catch (e: ParseException) {
            Log.e("ReportCalculation", "Invalid start date format: $startDate", e)
            null
        }

        val end = try {
            dateFormat.parse(endDate)
        } catch (e: ParseException) {
            Log.e("ReportCalculation", "Invalid end date format: $endDate", e)
            null
        }

        if (start == null || end == null) {
            Toast.makeText(this, "Invalid date format", Toast.LENGTH_SHORT).show()
            return
        }

        // Create the start timestamp at the start of the day (00:00:00)
        val startCalendar = Calendar.getInstance().apply {
            time = start
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTimestamp = Timestamp(startCalendar.time)

        // Create the end timestamp at the end of the day (23:59:59)
        val endCalendar = Calendar.getInstance().apply {
            time = end
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 0)
        }
        val endTimestamp = Timestamp(endCalendar.time)

        Log.d("ReportCalculation", "Calculating profit for: $selectedItem from $startDate to $endDate")

        when {
            selectedItem.startsWith("Branch") -> {
                val branchName = selectedItem.removePrefix("Branch: ").trim()

                firestore.collection("collectors")
                    .whereEqualTo("branch", branchName)
                    .get()
                    .addOnSuccessListener { collectorDocs ->
                        val collectorIds = collectorDocs.documents.mapNotNull { it.id }
                        Log.d("ReportCalculation", "Collector IDs: $collectorIds")

                        if (collectorIds.isEmpty()) {
                            Log.d("ReportCalculation", "No collectors found for branch: $branchName")
                            Toast.makeText(this, "No collectors found for the selected branch", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        firestore.collection("clients")
                            .whereIn("collectorId", collectorIds)
                            .get()
                            .addOnSuccessListener { clientDocs ->
                                val clientIds = clientDocs.documents.mapNotNull { it.id }
                                Log.d("ReportCalculation", "Client IDs: $clientIds")

                                if (clientIds.isEmpty()) {
                                    Log.d("ReportCalculation", "No clients found for the selected collectors")
                                    Toast.makeText(this, "No clients found for the selected collectors", Toast.LENGTH_SHORT).show()
                                    return@addOnSuccessListener
                                }

                                firestore.collection("ProfitRecord")
                                    .whereIn("clientId", clientIds)
                                    .whereGreaterThanOrEqualTo("date", startTimestamp)
                                    .whereLessThanOrEqualTo("date", endTimestamp)
                                    .get()
                                    .addOnSuccessListener { profitDocs ->
                                        var totalProfit = 0.0
                                        Log.d("ReportCalculation", "Profit records fetched: ${profitDocs.size()}")

                                        for (document in profitDocs) {
                                            val profit = document.getDouble("profit") ?: 0.0
                                            Log.d("ReportCalculation", "Document ID: ${document.id}, Profit: $profit")
                                            totalProfit += profit
                                        }

                                        // Ensure UI update on the main thread
                                        runOnUiThread {
                                            displayProfit(totalProfit)
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("ReportCalculation", "Error fetching profit records", e)
                                        Toast.makeText(this, "Error calculating profit", Toast.LENGTH_SHORT).show()
                                    }
                            }
                            .addOnFailureListener { e ->
                                Log.e("ReportCalculation", "Error fetching clients", e)
                                Toast.makeText(this, "Error fetching clients", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e("ReportCalculation", "Error fetching collectors", e)
                        Toast.makeText(this, "Error fetching collectors", Toast.LENGTH_SHORT).show()
                    }

                return
            }

            selectedItem.startsWith("Collector") -> {
                val collectorName = selectedItem.removePrefix("Collector: ").trim()

                firestore.collection("collectors")
                    .whereEqualTo("name", collectorName)
                    .get()
                    .addOnSuccessListener { collectorDocs ->
                        if (collectorDocs.isEmpty) {
                            Log.d("ReportCalculation", "No collector found with name: $collectorName")
                            Toast.makeText(this, "No collector found with the specified name", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        val collectorId = collectorDocs.documents.firstOrNull()?.id ?: return@addOnSuccessListener
                        Log.d("ReportCalculation", "Collector ID: $collectorId")

                        firestore.collection("clients")
                            .whereEqualTo("collectorId", collectorId)
                            .get()
                            .addOnSuccessListener { clientDocs ->
                                val clientIds = clientDocs.documents.mapNotNull { it.id }
                                Log.d("ReportCalculation", "Client IDs: $clientIds")

                                if (clientIds.isEmpty()) {
                                    Log.d("ReportCalculation", "No clients found for the selected collector")
                                    Toast.makeText(this, "No clients found for the selected collector", Toast.LENGTH_SHORT).show()
                                    return@addOnSuccessListener
                                }

                                firestore.collection("ProfitRecord")
                                    .whereIn("clientId", clientIds)
                                    .whereGreaterThanOrEqualTo("date", startTimestamp)
                                    .whereLessThanOrEqualTo("date", endTimestamp)
                                    .get()
                                    .addOnSuccessListener { profitDocs ->
                                        var totalProfit = 0.0
                                        Log.d("ReportCalculation", "Profit records fetched: ${profitDocs.size()}")

                                        for (document in profitDocs) {
                                            val profit = document.getDouble("profit") ?: 0.0
                                            Log.d("ReportCalculation", "Document ID: ${document.id}, Profit: $profit")
                                            totalProfit += profit
                                        }

                                        // Ensure UI update on the main thread
                                        runOnUiThread {
                                            displayProfit(totalProfit)
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("ReportCalculation", "Error fetching profit records", e)
                                        Toast.makeText(this, "Error calculating profit", Toast.LENGTH_SHORT).show()
                                    }
                            }
                            .addOnFailureListener { e ->
                                Log.e("ReportCalculation", "Error fetching clients", e)
                                Toast.makeText(this, "Error fetching clients", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e("ReportCalculation", "Error fetching collectors", e)
                        Toast.makeText(this, "Error fetching collectors", Toast.LENGTH_SHORT).show()
                    }

                return
            }
        }
    }


    private fun displayProfit(totalProfit: Double) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Profit Report")
        builder.setMessage("Total Profit: $totalProfit")
        builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        val alertDialog = builder.create()
        alertDialog.show()
    }
}
