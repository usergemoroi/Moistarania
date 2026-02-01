package com.standoff2.injector

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.ProgressBar
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    private lateinit var startButton: MaterialButton
    private lateinit var launchGameButton: MaterialButton
    private lateinit var statusTextView: TextView
    private lateinit var progressBar: ProgressBar
    
    private var isInjected = false
    private val rootChecker = RootChecker()
    private val nativeBuilder = NativeBuilder()

    companion object {
        private const val REQUEST_STORAGE_PERMISSION = 100
        private const val REQUEST_OVERLAY_PERMISSION = 101
        private const val GAME_PACKAGE = "com.axlebolt.standoff2"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        checkPermissions()
    }

    private fun initViews() {
        startButton = findViewById(R.id.startButton)
        launchGameButton = findViewById(R.id.launchGameButton)
        statusTextView = findViewById(R.id.statusTextView)
        progressBar = findViewById(R.id.progressBar)

        startButton.setOnClickListener {
            if (isInjected) {
                stopInjection()
            } else {
                startInjection()
            }
        }

        launchGameButton.setOnClickListener {
            launchGame()
        }
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
            checkRoot()
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
                checkRoot()
            } else {
                showError("Storage permissions required")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Settings.canDrawOverlays(this)) {
                checkPermissions()
            } else {
                showError("Overlay permission required")
            }
        }
    }

    private fun checkRoot() {
        lifecycleScope.launch {
            updateStatus(getString(R.string.status_checking), true)
            
            val hasRoot = withContext(Dispatchers.IO) {
                rootChecker.isRooted()
            }

            if (hasRoot) {
                updateStatus(getString(R.string.status_idle), false)
                startButton.isEnabled = true
            } else {
                updateStatus(getString(R.string.root_required), false)
                showRootDialog()
            }
        }
    }

    private fun showRootDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.root_required)
            .setMessage(R.string.root_denied)
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
                startButton.isEnabled = false
                launchGameButton.isEnabled = false

                updateStatus(getString(R.string.status_building), true)
                
                val buildSuccess = withContext(Dispatchers.IO) {
                    nativeBuilder.buildNative(this@MainActivity)
                }

                if (!buildSuccess) {
                    throw Exception(getString(R.string.native_build_error))
                }

                updateStatus(getString(R.string.status_injecting), true)

                val injectionSuccess = withContext(Dispatchers.IO) {
                    InjectorService.startOverlay(this@MainActivity)
                }

                if (injectionSuccess) {
                    isInjected = true
                    updateStatus(getString(R.string.status_running), false)
                    startButton.text = getString(R.string.btn_stop)
                    startButton.isEnabled = true
                    launchGameButton.isEnabled = true
                    
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "Overlay started successfully!",
                        Snackbar.LENGTH_LONG
                    ).show()
                } else {
                    throw Exception(getString(R.string.injection_error))
                }

            } catch (e: Exception) {
                updateStatus(getString(R.string.status_failed), false)
                showError(e.message ?: "Unknown error")
                startButton.isEnabled = true
                launchGameButton.isEnabled = true
            }
        }
    }

    private fun stopInjection() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                InjectorService.stopOverlay(this@MainActivity)
            }
            isInjected = false
            updateStatus(getString(R.string.status_idle), false)
            startButton.text = getString(R.string.btn_start)
        }
    }

    private fun launchGame() {
        try {
            val intent = packageManager.getLaunchIntentForPackage(GAME_PACKAGE)
            if (intent != null) {
                startActivity(intent)
            } else {
                showError("Standoff 2 not installed")
            }
        } catch (e: Exception) {
            showError("Failed to launch game: ${e.message}")
        }
    }

    private fun updateStatus(status: String, showProgress: Boolean) {
        statusTextView.text = status
        progressBar.visibility = if (showProgress) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        Snackbar.make(
            findViewById(android.R.id.content),
            message,
            Snackbar.LENGTH_LONG
        ).show()
    }
}
