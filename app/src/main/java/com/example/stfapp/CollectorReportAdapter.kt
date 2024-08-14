package com.example.stfapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class CollectorReport(
    val collectorName: String,
    val activeCount: Int,
    val inactiveCount: Int,
    val totalLoan: Double,
    val totalPayable: Double,
    val totalBadClients: Int
)

class CollectorReportAdapter(private val reports: List<CollectorReport>) :
    RecyclerView.Adapter<CollectorReportAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val collectorNameTextView: TextView = view.findViewById(R.id.collectorName)
        val activeCountTextView: TextView = view.findViewById(R.id.activeClients)
        val inactiveCountTextView: TextView = view.findViewById(R.id.inactiveClients)
        val totalLoanTextView: TextView = view.findViewById(R.id.loanAmount)
        val totalPayableTextView: TextView = view.findViewById(R.id.payableAmount)
        val totalBadClientsTextView: TextView = view.findViewById(R.id.badClients)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_collector_report, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val report = reports[position]
        holder.collectorNameTextView.text = report.collectorName
        holder.activeCountTextView.text = "Active Clients: ${report.activeCount}"
        holder.inactiveCountTextView.text = "Inactive Clients: ${report.inactiveCount}"
        holder.totalLoanTextView.text = "Loan: ${report.totalLoan}"
        holder.totalPayableTextView.text = "Payable: ${report.totalPayable}"
        holder.totalBadClientsTextView.text = "Bad Clients: ${report.totalBadClients}"
    }

    override fun getItemCount(): Int {
        return reports.size
    }
}
