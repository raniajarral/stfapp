package com.example.stfapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class BranchReport(
    val branchName: String,
    val totalActiveClients: Int,
    val totalInactiveClients: Int,
    val totalLoan: Double,
    val totalPayable: Double,
    val totalBadClients: Int,
    val totalCollectors: Int,
    val totalProfit: Double // Add this field
)


class BranchReportAdapter(private val reports: MutableList<BranchReport>) :
    RecyclerView.Adapter<BranchReportAdapter.BranchReportViewHolder>() {

    class BranchReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val branchNameTextView: TextView = itemView.findViewById(R.id.branchName)
        val activeClientsTextView: TextView = itemView.findViewById(R.id.activeClients)
        val inactiveClientsTextView: TextView = itemView.findViewById(R.id.inactiveClients)
        val totalLoanTextView: TextView = itemView.findViewById(R.id.loanAmount)
        val totalPayableTextView: TextView = itemView.findViewById(R.id.payableAmount)
        val totalBadClientsTextView: TextView = itemView.findViewById(R.id.badClients)
        val totalCollectorsTextView: TextView = itemView.findViewById(R.id.totalCollectors)
        val totalProfitTextView: TextView = itemView.findViewById(R.id.totalProfit) // New TextView for total profit
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BranchReportViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_branch_report, parent, false)
        return BranchReportViewHolder(view)
    }

    override fun onBindViewHolder(holder: BranchReportViewHolder, position: Int) {
        val report = reports[position]
        holder.branchNameTextView.text = report.branchName
        holder.activeClientsTextView.text = "Active Clients: ${report.totalActiveClients}"
        holder.inactiveClientsTextView.text = "Inactive Clients: ${report.totalInactiveClients}"
        holder.totalLoanTextView.text = "Total Loan: ${report.totalLoan}"
        holder.totalPayableTextView.text = "Total Payable: ${report.totalPayable}"
        holder.totalBadClientsTextView.text = "Bad Clients: ${report.totalBadClients}"
        holder.totalCollectorsTextView.text = "Total Collectors: ${report.totalCollectors}"
        holder.totalProfitTextView.text = "Total Profit: ${report.totalProfit}" // Bind total profit data
    }

    override fun getItemCount(): Int {
        return reports.size
    }

    fun updateReports(newReports: List<BranchReport>) {
        reports.clear()
        reports.addAll(newReports)
        notifyDataSetChanged()
    }
}
