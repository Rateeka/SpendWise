package com.example.spendwise

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.spendwise.data.AppDatabase
import com.example.spendwise.data.Expense
import com.example.spendwise.databinding.DialogAddExpenseBinding
import kotlinx.coroutines.launch

class AddExpenseDialogFragment : DialogFragment() {

    private var _binding: DialogAddExpenseBinding? = null
    private val binding get() = _binding!!

    private val expenseCategories = arrayOf(
        "Food", "Dining", "Shopping", "Bills", "Transport",
        "Health", "Entertainment", "Personal", "Other"
    )

    private val incomeCategories = arrayOf(
        "Salary", "Freelancing", "Gift", "Other"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddExpenseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        // Make dialog full-width with rounded corners
        dialog?.window?.apply {
            setLayout(
                (resources.displayMetrics.widthPixels * 0.92).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            setBackgroundDrawableResource(android.R.color.transparent)
            attributes = attributes.also { attrs ->
                attrs.dimAmount = 0.6f
                addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initial setup
        updateCategoryDropdown(isIncome = false)

        // Show/hide custom category field based on selection
        binding.etCategory.setOnItemClickListener { parent, _, position, _ ->
            val selectedCategory = parent.getItemAtPosition(position) as String
            if (selectedCategory == "Other") {
                binding.tilCustomCategory.visibility = View.VISIBLE
            } else {
                binding.tilCustomCategory.visibility = View.GONE
                binding.etCustomCategory.setText("")
            }
        }

        // Ensure dropdown shows on click even if empty
        binding.etCategory.setOnClickListener {
            binding.etCategory.showDropDown()
        }

        // Tab toggle — drives the hidden switch to keep logic intact
        binding.tabExpense.setOnClickListener {
            binding.switchIncome.isChecked = false
            setTabState(isIncome = false)
            updateCategoryDropdown(isIncome = false)
        }
        binding.tabIncome.setOnClickListener {
            binding.switchIncome.isChecked = true
            setTabState(isIncome = true)
            updateCategoryDropdown(isIncome = true)
        }

        binding.btnSave.setOnClickListener {
            saveExpense()
        }

        binding.btnClose.setOnClickListener {
            dismiss()
        }
    }

    private fun updateCategoryDropdown(isIncome: Boolean) {
        val currentCategories = if (isIncome) incomeCategories else expenseCategories
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            currentCategories
        )
        binding.etCategory.setAdapter(adapter)
        binding.etCategory.threshold = 1 // Show all on 1st char or click
        binding.etCategory.setText("", false)
        binding.tilCustomCategory.visibility = View.GONE
        binding.etCustomCategory.setText("")
    }

    private fun setTabState(isIncome: Boolean) {
        if (isIncome) {
            binding.tabIncome.setBackgroundResource(R.drawable.bg_tab_income_active)
            binding.tabIncome.setTextColor(requireContext().getColor(android.R.color.white))
            binding.tabExpense.setBackgroundResource(android.R.color.transparent)
            binding.tabExpense.setTextColor(requireContext().getColor(R.color.text_secondary))
        } else {
            binding.tabExpense.setBackgroundResource(R.drawable.bg_tab_expense_active)
            binding.tabExpense.setTextColor(requireContext().getColor(android.R.color.white))
            binding.tabIncome.setBackgroundResource(android.R.color.transparent)
            binding.tabIncome.setTextColor(requireContext().getColor(R.color.text_secondary))
        }
    }

    // ── saveExpense() is UNCHANGED ─────────────────────────────────────────
    private fun saveExpense() {
        val title = binding.etTitle.text.toString()
        val amount = binding.etAmount.text.toString().toDoubleOrNull()
        var category = binding.etCategory.text.toString()
        val isIncome = binding.switchIncome.isChecked

        if (category == "Other") {
            category = binding.etCustomCategory.text.toString()
        }

        if (title.isBlank() || amount == null || category.isBlank()) {
            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val expense = Expense(
            title = title,
            amount = amount,
            date = System.currentTimeMillis(),
            category = category,
            isIncome = isIncome
        )

        lifecycleScope.launch {
            AppDatabase.getDatabase(requireContext()).expenseDao().insertExpense(expense)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}