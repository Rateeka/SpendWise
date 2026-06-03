package com.example.spendwise

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.spendwise.data.AppDatabase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Profile : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // ── Settings Click Listeners ─────────────────────────────────────────
        view.findViewById<View>(R.id.rowExportCsv)?.setOnClickListener {
            exportToCsv()
        }

        updateStats(view)

        return view
    }

    private fun updateStats(view: View) {
        val tvExpenses = view.findViewById<TextView>(R.id.tvProfileExpenses)
        val tvIncome = view.findViewById<TextView>(R.id.tvProfileIncome)
        val db = AppDatabase.getDatabase(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            db.expenseDao().getTotalExpenses().collectLatest { total ->
                val amount = total ?: 0.0
                tvExpenses?.text = "Rs.${amount.toInt()}"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            db.expenseDao().getTotalIncome().collectLatest { total ->
                val amount = total ?: 0.0
                tvIncome?.text = "Rs.${amount.toInt()}"
            }
        }
    }

    private fun toggleTheme() {
        val currentMode = AppCompatDelegate.getDefaultNightMode()
        if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }

    // ── exportToCsv — Saves locally to Downloads folder ────────────────────
    private fun exportToCsv() {
        viewLifecycleOwner.lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val expenses = db.expenseDao().getAllExpenses().first()

            if (expenses.isEmpty()) {
                Toast.makeText(context, "No transactions to export", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val csvHeader = "ID,Title,Amount,Date,Category,Type\n"
            val csvData = StringBuilder(csvHeader)
            val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

            expenses.sortedBy { it.id }.forEach {
                val formattedDate = dateFormat.format(Date(it.date))
                val type = if (it.isIncome) "Income" else "Expense"
                csvData.append("${it.id},\"${it.title}\",${it.amount},$formattedDate,\"${it.category}\",$type\n")
            }

            try {
                val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                val fileName = "SpendWise_Report_${fileDateFormat.format(Date())}.csv"

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = requireContext().contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    uri?.let {
                        resolver.openOutputStream(it)?.use { out ->
                            out.write(csvData.toString().toByteArray())
                        }
                        Toast.makeText(context, "Report downloaded to Downloads folder", Toast.LENGTH_LONG).show()
                    } ?: throw Exception("Could not create file")
                } else {
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!downloadsDir.exists()) downloadsDir.mkdirs()
                    val file = File(downloadsDir, fileName)
                    FileOutputStream(file).use { out ->
                        out.write(csvData.toString().toByteArray())
                    }
                    Toast.makeText(context, "Report saved to: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
