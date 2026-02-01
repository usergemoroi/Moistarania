package com.standoff2.injector

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream

class NativeBuilder {

    companion object {
        private const val TAG = "NativeBuilder"
        private const val NATIVE_DIR = "/data/local/tmp/so2_external"
        private const val OUTPUT_BINARY = "standof.sh"
    }

    fun buildNative(context: Context): Boolean {
        try {
            Log.d(TAG, "Starting native build process...")
            
            val rootChecker = RootChecker()
            
            val sourceDir = File(context.filesDir, "so2-external-main")
            if (!sourceDir.exists()) {
                if (!extractSources(context)) {
                    Log.e(TAG, "Failed to extract sources")
                    return false
                }
            }

            var result = rootChecker.executeRootCommand("mkdir -p $NATIVE_DIR")
            if (!result.isSuccess) {
                Log.e(TAG, "Failed to create build directory: ${result.error}")
                return false
            }

            val sourceArchive = File(context.cacheDir, "so2-external-main.zip")
            if (sourceArchive.exists() || copyAssetToFile(context, "so2-external-main.zip", sourceArchive)) {
                result = rootChecker.executeRootCommand("cp ${sourceArchive.absolutePath} $NATIVE_DIR/")
                result = rootChecker.executeRootCommand("cd $NATIVE_DIR && unzip -o so2-external-main.zip")
                
                if (!result.isSuccess) {
                    Log.e(TAG, "Failed to extract sources: ${result.error}")
                    return false
                }
            }

            val ndkPath = findNDK()
            if (ndkPath == null) {
                Log.e(TAG, "NDK not found, using prebuilt binary if available")
                return copyPrebuiltBinary(context, rootChecker)
            }

            Log.d(TAG, "Building with NDK: $ndkPath")
            val buildCmd = """
                cd $NATIVE_DIR/so2-external-main && 
                export NDK_PROJECT_PATH=. && 
                $ndkPath/ndk-build clean && 
                $ndkPath/ndk-build NDK_APPLICATION_MK=Application.mk
            """.trimIndent()

            result = rootChecker.executeRootCommand(buildCmd)
            
            if (!result.isSuccess) {
                Log.e(TAG, "Build failed: ${result.error}")
                return copyPrebuiltBinary(context, rootChecker)
            }

            result = rootChecker.executeRootCommand("chmod 755 $NATIVE_DIR/so2-external-main/libs/arm64-v8a/$OUTPUT_BINARY")
            
            Log.d(TAG, "Native build completed successfully")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Build exception: ${e.message}", e)
            return false
        }
    }

    private fun extractSources(context: Context): Boolean {
        try {
            val zipFile = File(context.cacheDir, "so2-external-main.zip")
            if (!zipFile.exists()) {
                return false
            }

            val extractDir = File(context.filesDir, "so2-external-main")
            extractDir.mkdirs()

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract sources: ${e.message}", e)
            return false
        }
    }

    private fun copyAssetToFile(context: Context, assetName: String, destFile: File): Boolean {
        return try {
            context.assets.open(assetName).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy asset: ${e.message}", e)
            false
        }
    }

    private fun findNDK(): String? {
        val possiblePaths = listOf(
            "/data/data/com.termux/files/home/android-ndk",
            "/data/local/tmp/android-ndk",
            System.getenv("NDK_ROOT"),
            System.getenv("ANDROID_NDK_HOME"),
            System.getenv("ANDROID_NDK_ROOT")
        )

        for (path in possiblePaths) {
            if (path != null && File(path, "ndk-build").exists()) {
                return path
            }
        }

        val result = RootChecker().executeCommand("which", "ndk-build")
        if (result.isSuccess && result.output.isNotEmpty()) {
            return File(result.output.trim()).parentFile?.absolutePath
        }

        return null
    }

    private fun copyPrebuiltBinary(context: Context, rootChecker: RootChecker): Boolean {
        try {
            Log.d(TAG, "Attempting to use prebuilt binary...")
            
            val prebuiltAsset = "standof.sh"
            val tempFile = File(context.cacheDir, prebuiltAsset)
            
            if (!copyAssetToFile(context, prebuiltAsset, tempFile)) {
                Log.e(TAG, "Prebuilt binary not found in assets")
                return false
            }

            val targetDir = "$NATIVE_DIR/so2-external-main/libs/arm64-v8a"
            var result = rootChecker.executeRootCommand("mkdir -p $targetDir")
            
            if (!result.isSuccess) {
                Log.e(TAG, "Failed to create target directory")
                return false
            }

            result = rootChecker.executeRootCommand("cp ${tempFile.absolutePath} $targetDir/$OUTPUT_BINARY")
            if (!result.isSuccess) {
                Log.e(TAG, "Failed to copy prebuilt binary")
                return false
            }

            result = rootChecker.executeRootCommand("chmod 755 $targetDir/$OUTPUT_BINARY")
            
            Log.d(TAG, "Prebuilt binary copied successfully")
            return result.isSuccess

        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy prebuilt binary: ${e.message}", e)
            return false
        }
    }

    fun getBinaryPath(): String {
        return "$NATIVE_DIR/so2-external-main/libs/arm64-v8a/$OUTPUT_BINARY"
    }
}
