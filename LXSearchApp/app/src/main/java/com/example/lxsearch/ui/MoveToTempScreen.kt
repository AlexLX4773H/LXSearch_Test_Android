package com.example.lxsearch.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lxsearch.core.MoveToTemp
import com.example.lxsearch.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MoveToTempScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val jobState by com.example.lxsearch.service.LXJobManager.jobState.collectAsState()
    val recentLogs by com.example.lxsearch.service.LXJobManager.recentLogs.collectAsState()

    var scanResult by remember { mutableStateOf<MoveToTemp.ScanResult?>(null) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }
    var phase by remember { mutableStateOf("idle") } // idle, scanned, moved, removed

    val currentJob = (jobState as? com.example.lxsearch.service.JobState.Running)?.job
    val isAnyJobRunning = jobState is com.example.lxsearch.service.JobState.Running
    val isScanningJob = currentJob is com.example.lxsearch.service.LXJob.MoveToTempScan
    val isExecutingJob = currentJob is com.example.lxsearch.service.LXJob.MoveToTempMove ||
            currentJob is com.example.lxsearch.service.LXJob.MoveToTempRemove

    LaunchedEffect(jobState) {
        val completed = jobState as? com.example.lxsearch.service.JobState.Completed
        if (completed != null) {
            when (completed.job) {
                is com.example.lxsearch.service.LXJob.MoveToTempScan -> {
                    (completed.resultData as? MoveToTemp.ScanResult)?.let {
                        scanResult = it
                        phase = "scanned"
                    }
                }
                is com.example.lxsearch.service.LXJob.MoveToTempMove -> {
                    phase = "moved"
                }
                is com.example.lxsearch.service.LXJob.MoveToTempRemove -> {
                    phase = "removed"
                }
                else -> {}
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Primary)
            }
            Text(
                "Move To Temp",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Spacer(Modifier.height(12.dp))

        // Description
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
        ) {
            Text(
                text = "Finds empty/incomplete folders and moves them to temp/cleanup destinations. Two phases: first move empty items, then remove empty parent folders.",
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant,
                modifier = Modifier.padding(12.dp),
            )
        }

        Spacer(Modifier.height(12.dp))

        // Scan Button
        if (phase == "idle") {
            Button(
                onClick = {
                    com.example.lxsearch.service.LXJobManager.startJob(
                        context,
                        com.example.lxsearch.service.LXJob.MoveToTempScan
                    )
                },
                enabled = !isAnyJobRunning,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Tertiary),
            ) {
                if (isScanningJob) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = OnTertiary, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Scanning in background...", color = OnTertiary)
                } else {
                    Text("Scan for Empty/Incomplete Folders", color = OnTertiary, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        scanResult?.let { result ->
            Spacer(Modifier.height(8.dp))

            // Summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = TertiaryContainer),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Items to Move", color = OnTertiaryContainer)
                        Text("${result.moveItems.size}", color = OnTertiaryContainer, fontWeight = FontWeight.Bold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Empty Folders to Remove", color = OnTertiaryContainer)
                        Text("${result.emptyFolders.size}", color = OnTertiaryContainer, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Action buttons
            if (phase == "scanned") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { showMoveDialog = true },
                        enabled = result.moveItems.isNotEmpty() && !isAnyJobRunning,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Tertiary),
                    ) {
                        Text("Move (${result.moveItems.size})", color = OnTertiary, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = { showRemoveDialog = true },
                        enabled = result.emptyFolders.isNotEmpty() && !isAnyJobRunning,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text("Remove Empty (${result.emptyFolders.size})", color = Error, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Execution logs
            if (recentLogs.isNotEmpty() && (phase == "moved" || phase == "removed" || isExecutingJob)) {
                Text("Execution Log", style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Background),
                ) {
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp).padding(12.dp)) {
                        items(recentLogs) { line ->
                            Text(
                                text = line, fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                                color = if (line.startsWith("ERROR")) Error else if (line.contains("Moved") || line.contains("Removed") || line.startsWith("✓")) Success else OnSurfaceVariant,
                                modifier = Modifier.padding(vertical = 1.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Scan logs
            Text("Scan Details", style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Background),
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    items(result.logs) { line ->
                        Text(
                            text = line, fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                            color = OnSurfaceVariant, modifier = Modifier.padding(vertical = 1.dp),
                        )
                    }
                }
            }
        }
    }

    // Confirmation dialogs
    if (showMoveDialog) {
        AlertDialog(
            onDismissRequest = { showMoveDialog = false },
            title = { Text("Confirm Move", fontWeight = FontWeight.Bold) },
            text = { Text("Move ${scanResult?.moveItems?.size ?: 0} items to temp destinations in the background?") },
            confirmButton = {
                Button(
                    onClick = {
                        showMoveDialog = false
                        com.example.lxsearch.service.LXJobManager.startJob(
                            context,
                            com.example.lxsearch.service.LXJob.MoveToTempMove(scanResult!!.moveItems)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Tertiary),
                ) { Text("Move", color = OnTertiary) }
            },
            dismissButton = {
                TextButton(onClick = { showMoveDialog = false }) { Text("Cancel") }
            },
            containerColor = Surface,
        )
    }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Confirm Remove", fontWeight = FontWeight.Bold, color = Error) },
            text = { Text("Remove ${scanResult?.emptyFolders?.size ?: 0} empty folders in the background? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showRemoveDialog = false
                        com.example.lxsearch.service.LXJobManager.startJob(
                            context,
                            com.example.lxsearch.service.LXJob.MoveToTempRemove(scanResult!!.emptyFolders)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Error),
                ) { Text("Remove", color = OnError) }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) { Text("Cancel") }
            },
            containerColor = Surface,
        )
    }
}
