package com.standoff2.injector

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class RootChecker {

    fun isRooted(): Boolean {
        return checkRootMethod1() || checkRootMethod2() || checkRootMethod3()
    }

    private fun checkRootMethod1(): Boolean {
        val buildTags = android.os.Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }

    private fun checkRootMethod2(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }
        return false
    }

    private fun checkRootMethod3(): Boolean {
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            reader.readLine() != null
        } catch (e: Exception) {
            false
        } finally {
            process?.destroy()
        }
    }

    fun executeRootCommand(command: String): ShellResult {
        return executeCommand("su", "-c", command)
    }

    fun executeCommand(vararg command: String): ShellResult {
        var process: Process? = null
        try {
            process = Runtime.getRuntime().exec(command)
            
            val output = StringBuilder()
            val error = StringBuilder()
            
            val outputReader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            
            outputReader.forEachLine { output.append(it).append("\n") }
            errorReader.forEachLine { error.append(it).append("\n") }
            
            val exitCode = process.waitFor()
            
            return ShellResult(
                exitCode = exitCode,
                output = output.toString().trim(),
                error = error.toString().trim()
            )
        } catch (e: Exception) {
            return ShellResult(
                exitCode = -1,
                output = "",
                error = e.message ?: "Unknown error"
            )
        } finally {
            process?.destroy()
        }
    }

    data class ShellResult(
        val exitCode: Int,
        val output: String,
        val error: String
    ) {
        val isSuccess: Boolean get() = exitCode == 0
    }
}
