package com.example.lxsearch

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.lxsearch.data.InputFileManager
import com.example.lxsearch.theme.LXSearchTheme
import java.io.File

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { copyAssetsIfNeeded() }

    private val manageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { copyAssetsIfNeeded() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Copy assets on first launch
        copyAssetsIfNeeded()

        // Request storage permissions
        requestStoragePermissions()

        enableEdgeToEdge()
        setContent {
            LXSearchTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        copyAssetsIfNeeded()
    }

    private fun requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ needs MANAGE_EXTERNAL_STORAGE
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:$packageName")
                    manageStorageLauncher.launch(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    manageStorageLauncher.launch(intent)
                }
            }
        } else {
            // Android 10 and below
            val perms = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            val needsPermission = perms.any {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (needsPermission) {
                requestPermissionLauncher.launch(perms)
            }
        }
    }

    /**
     * Copies bundled input text files from assets (or fallback defaults) to app's
     * data directory on first launch (or if files are missing).
     */
    private fun copyAssetsIfNeeded() {
        InputFileManager.migrateLegacyFilesIfNeeded(this)
        val inputDir = InputFileManager.getInputDir(this)

        for (fileInfo in InputFileManager.MANAGED_FILES) {
            val targetFile = File(inputDir, fileInfo.fileName)
            if (!targetFile.exists()) {
                InputFileManager.readFileContent(this, fileInfo.fileName)
            }
        }

        // Also ensure output directory exists
        val outputDir = getOutputDir(this)
        if (!outputDir.exists()) outputDir.mkdirs()
    }

    companion object {
        /** Gets the input directory path for the app. */
        fun getInputDir(context: Context): File {
            return InputFileManager.getInputDir(context)
        }

        /** Gets the output directory path for the app. */
        fun getOutputDir(context: Context): File {
            return InputFileManager.getOutputDir(context)
        }
    }
}
