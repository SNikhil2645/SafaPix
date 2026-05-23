package com.example.myapplication.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.myapplication.viewmodel.DuplicateDetectorViewModel
import com.example.myapplication.viewmodel.ScanState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.accompanist.permissions.*
/**
 * The main screen of the application, which handles permission requests and displays the scan state.
 *
 * @param viewModel The ViewModel that drives the screen's logic.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(
    viewModel: DuplicateDetectorViewModel
) {
    val scanState by viewModel.scanState.collectAsState()
    val context = LocalContext.current

    // Modern permission handling using Accompanist
    val permissionState = rememberPermissionState(permission = Manifest.permission.READ_MEDIA_IMAGES)

    // Launcher for starting the scan after permission is confirmed
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                viewModel.startScan()
            }
        }
    )

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text("Duplicate Photo Detector", style = MaterialTheme.typography.headlineLarge)
            Text("Powered by Liquid Morphism UI", style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(48.dp))

            AnimatedContent(targetState = scanState, label = "MainScreenAnimation") { state ->
                when (state) {
                    is ScanState.Idle -> {
                        IdleContent(onScanClicked = {
                            when {
                                permissionState.status.isGranted -> viewModel.startScan()
                                permissionState.status.shouldShowRationale -> launcher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                                else -> launcher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                            }
                        }, permissionState.status.isGranted, context)
                    }
                    is ScanState.Scanning -> {
                        ProgressIndicator(text = "Scanning Gallery...", progress = state.progress.toFloat() / state.total.toFloat())
                    }
                    is ScanState.Hashing -> {
                        ProgressIndicator(text = "Analyzing duplicates...", progress = state.progress.toFloat() / state.total.toFloat())
                    }
                     is ScanState.Deleting -> {
                        ProgressIndicator(text = "Deleting duplicates...", progress = state.progress.toFloat() / state.total.toFloat())
                    }
                    is ScanState.Complete -> {
                        val duplicateGroups by viewModel.duplicateGroups.collectAsState()
                        if (duplicateGroups.isNotEmpty()) {
                            DuplicateGroupsScreen(duplicateGroups = duplicateGroups, viewModel = viewModel)
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("No duplicates found!", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(16.dp))
                                Button(onClick = { viewModel.resetScan() }) {
                                    Text("Scan Again")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Composable for the idle state, showing the scan button or permission rationale.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun IdleContent(onScanClicked: () -> Unit, isPermissionGranted: Boolean, context: Context) {

    var showRationale by remember { mutableStateOf(false) }
    val permissionState = rememberPermissionState(
        permission = Manifest.permission.READ_MEDIA_IMAGES
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (isPermissionGranted) {
            Button(onClick = onScanClicked) {
                Text("Scan Gallery")
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Storage permission is required to find duplicate photos. Please grant the permission to continue.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    if(showRationale) {
                        // Open app settings if permission is permanently denied
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = Uri.fromParts("package", context.packageName, null)
                        context.startActivity(intent)
                    } else {
                        permissionState.launchPermissionRequest()
                    }
                }) {
                    Text(if (showRationale) "Open Settings" else "Grant Permission")
                }
            }
        }
    }
}

/**
 * A composable for displaying a progress indicator with text.
 */
@Composable
fun ProgressIndicator(text: String, progress: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))
        LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth(0.8f))
    }
}
