package com.example.spendwise

import android.database.Cursor
import android.os.Bundle
import android.provider.Telephony
import android.text.format.DateFormat
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.lifecycle.lifecycleScope
import com.example.spendwise.adapter.TransactionAdapter
import com.example.spendwise.data.AppDatabase
import com.example.spendwise.model.TransactionData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import kotlinx.coroutines.flow.collectLatest

class Transactions : Fragment() {

    private var allTransactions = mutableListOf<TransactionData>()
    private lateinit var adapter: TransactionAdapter
    private lateinit var listView: ListView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_transactions, container, false)
        listView = view.findViewById(R.id.transaction_list)
        val etSearch: EditText = view.findViewById(R.id.etSearch)

        viewLifecycleOwner.lifecycleScope.launch {
            val smsTransactions = TransactionUtils.getTransactions(requireContext(), true)
            AppDatabase.getDatabase(requireContext()).expenseDao().getAllExpenses().collectLatest { roomExpenses ->
                updateCombinedList(smsTransactions, roomExpenses)
            }
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterTransactions(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        return view
    }

    private fun updateCombinedList(smsTransactions: List<TransactionData>, roomExpenses: List<com.example.spendwise.data.Expense>) {
        allTransactions.clear()
        allTransactions.addAll(smsTransactions)

        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

        roomExpenses.forEach { expense ->
            val date = Date(expense.date)
            allTransactions.add(
                TransactionData(
                    sender = expense.title,
                    messageBody = expense.title,
                    amount = expense.amount.toString(),
                    date = dateFormat.format(date),
                    time = timeFormat.format(date),
                    isCredit = expense.isIncome,
                    category = expense.category
                )
            )
        }

        allTransactions.sortByDescending { it.date }
        adapter = TransactionAdapter(requireContext(), allTransactions)
        listView.adapter = adapter
    }

    private fun filterTransactions(query: String) {
        val filtered = if (query.isEmpty()) {
            allTransactions
        } else {
            allTransactions.filter {
                it.sender.contains(query, ignoreCase = true) ||
                it.messageBody.contains(query, ignoreCase = true) ||
                it.amount.contains(query, ignoreCase = true) ||
                (it.category?.contains(query, ignoreCase = true) ?: false)
            }
        }
        adapter = TransactionAdapter(requireContext(), filtered)
        listView.adapter = adapter
    }
}
