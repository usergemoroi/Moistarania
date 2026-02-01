package com.standoff2.injector

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

class InjectorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var overlayProcess: Process? = null
    private val rootChecker = RootChecker()

    companion object {
        private const val TAG = "InjectorService"
        private const val GAME_PACKAGE = "com.axlebolt.standoff2"
        
        @Volatile
        private var isRunning = false

        fun startOverlay(context: Context): Boolean {
            return try {
                val intent = Intent(context, InjectorService::class.java)
                intent.action = "START_OVERLAY"
                context.startService(intent)
                
                Thread.sleep(2000)
                isRunning
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start overlay: ${e.message}", e)
                false
            }
        }

        fun stopOverlay(context: Context) {
            val intent = Intent(context, InjectorService::class.java)
            intent.action = "STOP_OVERLAY"
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_OVERLAY" -> {
                serviceScope.launch {
                    startOverlayProcess()
                }
            }
            "STOP_OVERLAY" -> {
                stopOverlayProcess()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private suspend fun startOverlayProcess() {
        try {
            Log.d(TAG, "Starting overlay process...")

            val gamePid = getGamePid()
            if (gamePid == -1) {
                Log.w(TAG, "Game not running, starting overlay anyway...")
            } else {
                Log.d(TAG, "Game PID: $gamePid")
            }

            val binaryPath = NativeBuilder().getBinaryPath()
            
            var result = rootChecker.executeRootCommand("test -f $binaryPath")
            if (!result.isSuccess) {
                Log.e(TAG, "Binary not found at $binaryPath")
                isRunning = false
                return
            }

            result = rootChecker.executeRootCommand("chmod 755 $binaryPath")
            
            Log.d(TAG, "Launching overlay binary...")
            
            overlayProcess = Runtime.getRuntime().exec(arrayOf("su", "-c", binaryPath))
            
            val outputReader = Thread {
                try {
                    BufferedReader(InputStreamReader(overlayProcess?.inputStream)).use { reader ->
                        reader.forEachLine { line ->
                            Log.d(TAG, "Overlay: $line")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Output reader error: ${e.message}")
                }
            }
            
            val errorReader = Thread {
                try {
                    BufferedReader(InputStreamReader(overlayProcess?.errorStream)).use { reader ->
                        reader.forEachLine { line ->
                            Log.e(TAG, "Overlay Error: $line")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error reader error: ${e.message}")
                }
            }
            
            outputReader.start()
            errorReader.start()

            delay(1000)
            
            if (overlayProcess?.isAlive == true) {
                isRunning = true
                Log.d(TAG, "Overlay started successfully")
                
                serviceScope.launch {
                    overlayProcess?.waitFor()
                    Log.d(TAG, "Overlay process terminated")
                    isRunning = false
                }
            } else {
                Log.e(TAG, "Overlay process failed to start")
                isRunning = false
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start overlay: ${e.message}", e)
            isRunning = false
        }
    }

    private fun stopOverlayProcess() {
        try {
            Log.d(TAG, "Stopping overlay process...")
            
            overlayProcess?.destroy()
            overlayProcess = null
            
            rootChecker.executeRootCommand("pkill -f standof.sh")
            
            isRunning = false
            Log.d(TAG, "Overlay stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop overlay: ${e.message}", e)
        }
    }

    private fun getGamePid(): Int {
        return try {
            val result = rootChecker.executeCommand("pidof", GAME_PACKAGE)
            if (result.isSuccess && result.output.isNotEmpty()) {
                result.output.trim().toInt()
            } else {
                -1
            }
        } catch (e: Exception) {
            -1
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopOverlayProcess()
        serviceScope.coroutineContext[Job]?.cancel()
    }
}
