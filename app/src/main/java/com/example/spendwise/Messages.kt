package com.example.spendwise

import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.provider.Telephony
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial

class Messages : Fragment() {

    private val SMS_PERMISSION_CODE = 101

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_messages, container, false)

        // ── Same IDs as before — logic completely unchanged ───────────────
        val switchToBankMsgs = view.findViewById<SwitchMaterial>(R.id.switchToBankMsgs)
        val listView         = view.findViewById<ListView>(R.id.lvSms)

        if (checkSmsPermission()) {
            displaySms(listView, false)
        } else {
            requestSmsPermission()
        }

        switchToBankMsgs.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(context, "Showing bank messages", Toast.LENGTH_SHORT).show()
                displaySms(listView, true)
            } else {
                Toast.makeText(context, "Showing all messages", Toast.LENGTH_SHORT).show()
                displaySms(listView, false)
            }
        }

        return view
    }

    // ── All methods below are UNCHANGED ───────────────────────────────────

    private fun checkSmsPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

    private fun requestSmsPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.READ_SMS),
            SMS_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, "SMS Permission Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displaySms(listView: ListView, showBankMessages: Boolean) {
        val smsList = mutableListOf<Triple<String, String, String>>()

        val cursor: Cursor? = requireContext().contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            null,
            Telephony.Sms.TYPE + " = ?",
            arrayOf(Telephony.Sms.MESSAGE_TYPE_INBOX.toString()),
            Telephony.Sms.DATE + " DESC"
        )

        cursor?.let {
            val addressColumn = it.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyColumn    = it.getColumnIndex(Telephony.Sms.BODY)
            val dateColumn    = it.getColumnIndex(Telephony.Sms.DATE)

            while (it.moveToNext()) {
                val sender     = it.getString(addressColumn)
                val message    = it.getString(bodyColumn)
                val dateMillis = it.getLong(dateColumn)
                val date       = DateFormat.format("dd-MM-yyyy hh:mm a", dateMillis).toString()

                if (!showBankMessages || TransactionUtils.isBankMessage(sender, message)) {
                    smsList.add(Triple(sender, message, date))
                }
            }
            it.close()
        }

        val adapter = SmsAdapter(requireContext(), R.layout.list_item_sms, smsList)
        listView.adapter = adapter
    }
}