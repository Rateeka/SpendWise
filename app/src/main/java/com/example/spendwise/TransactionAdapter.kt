package com.example.spendwise.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import androidx.core.content.ContextCompat
import android.widget.BaseAdapter
import com.example.spendwise.R
import com.example.spendwise.model.TransactionData
import com.google.android.material.card.MaterialCardView

class TransactionAdapter(private val context: Context, private val transactions: List<TransactionData>) : BaseAdapter() {

    override fun getCount(): Int = transactions.size

    override fun getItem(position: Int): Any = transactions[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.transaction_item, parent, false)

        val amountView: TextView = view.findViewById(R.id.amount)
        val dateView: TextView = view.findViewById(R.id.date)
        val timeView: TextView = view.findViewById(R.id.time)
        val senderView: TextView = view.findViewById(R.id.senderName)
        val categoryView: TextView = view.findViewById(R.id.tvCategory)
        val iconContainer: MaterialCardView = view.findViewById(R.id.iconContainer)
        val categoryIcon: ImageView = view.findViewById(R.id.ivCategoryIcon)

        val transaction = transactions[position]

        amountView.text = if (transaction.isCredit) "+Rs.${transaction.amount}" else "-Rs.${transaction.amount}"
        dateView.text = transaction.date
        timeView.text = transaction.time
        senderView.text = transaction.sender
        categoryView.text = transaction.category ?: "Bank"

        if (transaction.isCredit) {
            amountView.setTextColor(ContextCompat.getColor(context, R.color.income_green))
            iconContainer.setCardBackgroundColor(ContextCompat.getColor(context, R.color.card_income_bg))
            categoryIcon.setColorFilter(ContextCompat.getColor(context, R.color.income_green))
            categoryIcon.setImageResource(android.R.drawable.arrow_down_float)
        } else {
            amountView.setTextColor(ContextCompat.getColor(context, R.color.expense_red))
            iconContainer.setCardBackgroundColor(ContextCompat.getColor(context, R.color.card_expense_bg))
            categoryIcon.setColorFilter(ContextCompat.getColor(context, R.color.expense_red))
            categoryIcon.setImageResource(android.R.drawable.arrow_up_float)
        }

        return view
    }
}
