package com.standoff2.injector

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private val rootChecker = RootChecker()
    private val nativeBuilder = NativeBuilder()
    private var hasStarted = false

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_STORAGE_PERMISSION = 100
        private const val REQUEST_OVERLAY_PERMISSION = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        Log.d(TAG, "MainActivity started - checking permissions")
        checkPermissions()
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                requestManageExternalStorage()
                return
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission()
            return
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), REQUEST_STORAGE_PERMISSION)
        } else {
            checkRootAndStartOverlay()
        }
    }

    private fun requestManageExternalStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        intent.data = Uri.parse("package:$packageName")
        startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                checkRootAndStartOverlay()
            } else {
                showToast("Storage permissions required")
                finish()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Settings.canDrawOverlays(this)) {
                checkPermissions()
            } else {
                showToast("Overlay permission required")
                finish()
            }
        }
    }

    private fun checkRootAndStartOverlay() {
        if (hasStarted) return
        hasStarted = true
        
        lifecycleScope.launch {
            Log.d(TAG, "Checking root access...")
            
            val hasRoot = withContext(Dispatchers.IO) {
                rootChecker.isRooted()
            }

            if (hasRoot) {
                Log.d(TAG, "Root access granted")
                startInjection()
            } else {
                Log.e(TAG, "Root access denied")
                showRootDialog()
            }
        }
    }

    private fun showRootDialog() {
        AlertDialog.Builder(this)
            .setTitle("Root Required")
            .setMessage("Root access is required to run the overlay. Please grant root permission.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun startInjection() {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Building native overlay...")
                showToast("Building overlay...")
                
                val buildSuccess = withContext(Dispatchers.IO) {
                    nativeBuilder.buildNative(this@MainActivity)
                }

                if (!buildSuccess) {
                    throw Exception("Failed to build native overlay")
                }

                Log.d(TAG, "Starting overlay process...")
                showToast("Starting overlay...")

                val injectionSuccess = withContext(Dispatchers.IO) {
                    InjectorService.startOverlay(this@MainActivity)
                }

                if (injectionSuccess) {
                    Log.d(TAG, "Overlay started successfully")
                    showToast("Overlay running - launching game...")
                    
                    // Launch Standoff 2
                    launchGame()
                    
                    // Close MainActivity after a short delay
                    kotlinx.coroutines.delay(2000)
                    finish()
                } else {
                    throw Exception("Failed to inject overlay")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Injection failed: ${e.message}", e)
                showToast("Error: ${e.message}")
                finish()
            }
        }
    }

    private fun launchGame() {
        try {
            val gamePackage = "com.axlebolt.standoff2"
            val intent = packageManager.getLaunchIntentForPackage(gamePackage)
            if (intent != null) {
                startActivity(intent)
                Log.d(TAG, "Launching Standoff 2")
            } else {
                Log.w(TAG, "Standoff 2 not installed")
                showToast("Standoff 2 not installed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch game: ${e.message}")
        }
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check permissions when returning to activity
        if (!hasStarted && Settings.canDrawOverlays(this) && 
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager() ||
             Build.VERSION.SDK_INT < Build.VERSION_CODES.R)) {
            checkRootAndStartOverlay()
        }
    }
}
