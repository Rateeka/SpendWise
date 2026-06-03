package com.example.spendwise

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class SmsAdapter(
    context: Context,
    resource: Int,
    private val smsList: List<Triple<String, String, String>>
) : ArrayAdapter<Triple<String, String, String>>(context, resource, smsList) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.list_item_sms, parent, false)

        val (sender, message, dateTime) = smsList[position]

        // ── Parse date and time from combined string ──────────────────────
        val parts    = dateTime.split(" ", limit = 2)
        val datePart = parts.getOrElse(0) { dateTime }
        val timePart = parts.getOrElse(1) { "" }

        // ── Bind to new premium layout ────────────────────────────────────
        view.findViewById<TextView>(R.id.tvSmsSender).text = sender
        view.findViewById<TextView>(R.id.tvSmsBody).text   = message
        view.findViewById<TextView>(R.id.tvSmsDate).text   = datePart
        view.findViewById<TextView>(R.id.tvSmsTime).text   = timePart

        // ── Show credit/debit tags based on message keywords ──────────────
        val tvCredit = view.findViewById<TextView>(R.id.tvTagCredit)
        val tvDebit  = view.findViewById<TextView>(R.id.tvTagDebit)
        val msgLower = message.lowercase()

        when {
            msgLower.contains("credit") || msgLower.contains("credited") || msgLower.contains("deposit") -> {
                tvCredit.visibility = View.VISIBLE
                tvDebit.visibility  = View.GONE
            }
            msgLower.contains("debit") || msgLower.contains("debited") || msgLower.contains("withdrawal") -> {
                tvDebit.visibility  = View.VISIBLE
                tvCredit.visibility = View.GONE
            }
            else -> {
                tvCredit.visibility = View.GONE
                tvDebit.visibility  = View.GONE
            }
        }

        return view
    }
}