package com.example.spendwise

import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.spendwise.model.TransactionData
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import com.example.spendwise.data.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.example.spendwise.data.Budget
import com.example.spendwise.data.Expense
import kotlinx.coroutines.flow.combine
import android.widget.TextView
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import android.widget.EditText
import android.text.InputType
import android.widget.Toast
import kotlinx.coroutines.flow.collectLatest

class Home : Fragment() {

    data class DailyTransactionSummary(
        val date: String,
        val totalCredit: Float,
        val totalDebit: Float
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Initialize Views
        val barChart: BarChart           = view.findViewById(R.id.barChart)
        val summaryRecyclerView: RecyclerView = view.findViewById(R.id.summaryRecyclerView)
        val tvBudgetText: TextView       = view.findViewById(R.id.tvBudgetText)
        val budgetProgress: ProgressBar  = view.findViewById(R.id.budgetProgress)
        val tvSetBudget: TextView        = view.findViewById(R.id.tvSetBudget)
        val tvTotalIncome: TextView      = view.findViewById(R.id.tvTotalIncome)
        val tvTotalExpenses: TextView    = view.findViewById(R.id.tvTotalExpenses)
        val tvTotalBalance: TextView     = view.findViewById(R.id.tvTotalBalance)
        val tvSeeAll: View               = view.findViewById(R.id.tvSeeAll)
        val tvGreeting: TextView         = view.findViewById(R.id.expensesHeadingTextView)

        // Set Greeting based on time
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        tvGreeting.text = when (hour) {
            in 0..11 -> "Good morning,"
            in 12..16 -> "Good afternoon,"
            in 17..20 -> "Good evening,"
            else -> "Good night,"
        }

        val db = AppDatabase.getDatabase(requireContext())
        val currentMonthYear = SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(Date())

        tvSeeAll.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.frame_layout, Transactions())
                .commit()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val budgetFlow   = db.budgetDao().getBudgetForMonth(currentMonthYear)
            val expensesFlow = db.expenseDao().getAllExpenses()

            combine(budgetFlow, expensesFlow) { budget, expenses ->
                val currentMonthExpenses = expenses.filter {
                    SimpleDateFormat("MM-yyyy", Locale.getDefault())
                        .format(Date(it.date)) == currentMonthYear
                }
                val monthlySpent  = currentMonthExpenses.filter { !it.isIncome }.sumOf { it.amount }
                val monthlyIncome = currentMonthExpenses.filter {  it.isIncome }.sumOf { it.amount }
                Triple(budget, monthlySpent, monthlyIncome)

            }.collectLatest { (budget, totalSpent, totalIncome) ->
                val budgetAmount = budget?.amount ?: 0.0

                tvBudgetText.text    = "Spent: Rs.${totalSpent.toInt()} / Rs.${budgetAmount.toInt()}"
                tvTotalIncome.text   = "Rs.${totalIncome.toInt()}"
                tvTotalExpenses.text = "Rs.${totalSpent.toInt()}"
                tvTotalBalance.text  = "Rs.${(totalIncome - totalSpent).toInt()}"

                budgetProgress.progress = if (budgetAmount > 0) {
                    (totalSpent / budgetAmount * 100).toInt().coerceAtMost(100)
                } else {
                    0
                }
            }
        }

        tvSetBudget.setOnClickListener {
            showSetBudgetDialog(db, currentMonthYear)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val smsTransactions = TransactionUtils.getTransactions(requireContext(), true)
            db.expenseDao().getAllExpenses().collectLatest { roomExpenses ->
                val weeklySummary = getCombinedWeeklySummary(smsTransactions, roomExpenses)

                setupBarChart(barChart, weeklySummary)
                setupRecyclerView(summaryRecyclerView, weeklySummary)
            }
        }

        view.findViewById<View>(R.id.fabAddExpense).setOnClickListener {
            AddExpenseDialogFragment().show(parentFragmentManager, "AddExpense")
        }

        return view
    }

    private fun showSetBudgetDialog(db: AppDatabase, monthYear: String) {
        val input = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Set Budget for $monthYear")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val amount = input.text.toString().toDoubleOrNull() ?: 0.0
                viewLifecycleOwner.lifecycleScope.launch {
                    db.budgetDao().setBudget(Budget(monthYear, amount))
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
            .show()
    }

    private fun getCombinedWeeklySummary(
        smsTransactions: List<TransactionData>,
        roomExpenses: List<Expense>
    ): List<DailyTransactionSummary> {
        val calendar  = Calendar.getInstance()
        val summaries = mutableMapOf<String, DailyTransactionSummary>()

        for (i in 0 until 7) {
            val date = DateFormat.format("dd-MM-yyyy", calendar.time).toString()
            summaries[date] = DailyTransactionSummary(date, 0f, 0f)
            calendar.add(Calendar.DAY_OF_MONTH, -1)
        }

        smsTransactions.forEach { transaction ->
            summaries[transaction.date]?.let { summary ->
                summaries[transaction.date] = if (transaction.isCredit) {
                    summary.copy(totalCredit = summary.totalCredit + transaction.amount.toFloat())
                } else {
                    summary.copy(totalDebit = summary.totalDebit + transaction.amount.toFloat())
                }
            }
        }

        val roomDateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        roomExpenses.forEach { expense ->
            val date = roomDateFormat.format(Date(expense.date))
            summaries[date]?.let { summary ->
                summaries[date] = if (expense.isIncome) {
                    summary.copy(totalCredit = summary.totalCredit + expense.amount.toFloat())
                } else {
                    summary.copy(totalDebit = summary.totalDebit + expense.amount.toFloat())
                }
            }
        }

        val inputDateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        return summaries.values.toList()
            .filter { it.totalCredit > 0 || it.totalDebit > 0 }
            .sortedBy { inputDateFormat.parse(it.date)?.time ?: 0L }
    }

    private fun setupRecyclerView(
        recyclerView: RecyclerView,
        weeklySummary: List<DailyTransactionSummary>
    ) {
        recyclerView.layoutManager = LinearLayoutManager(requireContext()).apply {
            reverseLayout = true
            stackFromEnd  = true
        }
        recyclerView.adapter = SummaryAdapter(weeklySummary)
    }

    private fun setupBarChart(
        barChart: BarChart,
        weeklySummary: List<DailyTransactionSummary>
    ) {
        val creditEntries = ArrayList<BarEntry>()
        val debitEntries  = ArrayList<BarEntry>()
        val labels        = ArrayList<String>()

        val inputDateFormat  = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val outputDateFormat = SimpleDateFormat("dd'\n'EEE",  Locale.getDefault())

        weeklySummary.forEachIndexed { index, summary ->
            creditEntries.add(BarEntry(index.toFloat(), summary.totalCredit))
            debitEntries.add(BarEntry(index.toFloat(), summary.totalDebit))

            val date          = inputDateFormat.parse(summary.date)
            val formattedDate = if (date != null) outputDateFormat.format(date) else summary.date
            labels.add(formattedDate)
        }

        val creditDataSet = BarDataSet(creditEntries, "Credits").apply {
            color = resources.getColor(R.color.creditColor, null)
            setDrawValues(false)
            valueTextSize = 9f
        }

        val debitDataSet = BarDataSet(debitEntries, "Debits").apply {
            color = resources.getColor(R.color.debitColor, null)
            setDrawValues(false)
            valueTextSize = 9f
        }

        val barData = BarData(creditDataSet, debitDataSet).apply {
            barWidth = 0.3f
        }

        barChart.apply {
            data = barData

            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setDrawGridBackground(false)
            setBorderColor(android.graphics.Color.TRANSPARENT)

            xAxis.apply {
                valueFormatter       = IndexAxisValueFormatter(labels)
                position             = XAxis.XAxisPosition.BOTTOM
                granularity          = 1f
                isGranularityEnabled = true
                setDrawGridLines(false)
                textColor            = resources.getColor(R.color.text_secondary, null)
                textSize             = 9f
                axisLineColor        = resources.getColor(R.color.divider_color, null)
            }

            axisLeft.apply {
                setDrawGridLines(true)
                gridColor     = 0x1A3949AB.toInt()   // faint indigo grid lines
                axisLineColor = android.graphics.Color.TRANSPARENT
                textColor     = resources.getColor(R.color.text_secondary, null)
                textSize      = 9f
            }

            axisRight.isEnabled = false

            legend.apply {
                textColor = resources.getColor(R.color.text_secondary, null)
                textSize  = 11f
            }

            description.isEnabled = false

            groupBars(-0.5f, 0.3f, 0.05f)
            setVisibleXRangeMaximum(7f)
            animateY(1000)
            invalidate()
        }
    }
}
