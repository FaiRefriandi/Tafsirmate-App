package com.froztlass.tafsirmate

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    // Minta izin untuk menggunakan mikrofon
    private val requestAudioPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            // Mikrofon diizinkan, lanjutkan aplikasi
        } else {
            // Mikrofon tidak diizinkan, tampilkan notifikasi atau dialog
            showPermissionDeniedDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Meminta izin mikrofon
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // Jika belum, minta izin dari pengguna
            requestAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
        }

        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)

        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        // Set initial status bar icon color based on the background
        setStatusBarIconColor(isLightBackground(Color.WHITE)) // replace with your background color

        // Example usage of Coroutine
        GlobalScope.launch(Dispatchers.Main) {
            val result = withContext(Dispatchers.IO) {
                // Perform heavy operation here
                performHeavyOperation()
            }
            // Update UI with result
            updateUI(result)
        }
    }

    private fun performHeavyOperation(): String {
        // Heavy operation like accessing database or network
        return "Heavy Operation Result"
    }

    private fun updateUI(result: String) {
        // Update UI with the result of the heavy operation
    }

    private fun setStatusBarIconColor(isLight: Boolean) {
        window.decorView.systemUiVisibility = if (isLight) {
            window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else {
            window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        }
    }

    private fun isLightBackground(color: Int): Boolean {
        val darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return darkness < 0.5
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Izin Diperlukan")
            .setMessage("Untuk menggunakan fitur mikrofon, Anda perlu memberikan izin.")
            .setPositiveButton("OK") { _, _ ->
                // Arahkan ke pengaturan untuk mengaktifkan izin
                openAppSettings()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }
}