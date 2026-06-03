package com.example.spendwise

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.example.spendwise.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val PERMISSION_CODE = 101
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // Force light mode as requested
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ── Handle edge-to-edge insets so nav bar doesn't overlap content ──
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            binding.bottomNavigationView2.updatePadding(bottom = navBarHeight)
            insets
        }

        // ── Check permissions (SMS + Storage) ──────────────────────────────
        checkAndRequestPermissions()

        // ── Initial fragment (only if not already restored) ──────────────────
        if (savedInstanceState == null) {
            replaceFragment(Home())
        }

        // ── Bottom nav listener (unchanged) ──────────────────────────────────
        binding.bottomNavigationView2.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.home_item        -> replaceFragment(Home())
                R.id.transitions_item -> replaceFragment(Transactions())
                R.id.message_item     -> replaceFragment(Messages())
                R.id.profile_item     -> replaceFragment(Profile())
                else -> Toast.makeText(
                    applicationContext,
                    "Unknown item selected",
                    Toast.LENGTH_SHORT
                ).show()
            }
            true
        }
    }

    // ── replaceFragment — adds a subtle fade transition, logic unchanged ──
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.frame_layout, fragment)
            .commit()
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(Manifest.permission.READ_SMS)
        
        // Add storage permission for downloading CSV if needed
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        val listPermissionsNeeded = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (listPermissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                listPermissionsNeeded.toTypedArray(),
                PERMISSION_CODE
            )
        }
    }
}
