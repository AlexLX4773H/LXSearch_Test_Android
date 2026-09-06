package com.example.lxsearch.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lxsearch.MainActivity
import com.example.lxsearch.core.MoveFromSource
import com.example.lxsearch.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MoveFromSourceScreen(
    onBack: () -> Unit,
    onNavigateToInputFiles: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val scope = rememberCoroutineScope()

    var isScanning by remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf<MoveFromSource.ScanResult?>(null) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var isExecuting by remember { mutableStateOf(false) }
    var executionLogs by remember { mutableStateOf(listOf<String>()) }
    var phase by remember { mutableStateOf("idle") }

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
                "Move From Source",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.weight(1f))
            if (onNavigateToInputFiles != null) {
                IconButton(onClick = { onNavigateToInputFiles("exclude_in_brackets.txt") }) {
                    Icon(Icons.Default.Tune, "Edit bracket rules", tint = Primary)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Description
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
        ) {
            Text(
                text = "Matches source releases to series folders using bracket extraction, ComicInfo metadata, and the name-circle JSON mapping. Requires 'Build Name-Circle Relations' to be run first.",
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
                    isScanning = true
                    scope.launch {
                        val outputDir = MainActivity.getOutputDir(activity)
                        val inputDir = MainActivity.getInputDir(activity)
                        scanResult = withContext(Dispatchers.IO) {
                            MoveFromSource.scan(outputDir, inputDir)
                        }
                        phase = "scanned"
                        isScanning = false
                    }
                },
                enabled = !isScanning,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Tertiary),
            ) {
                if (isScanning) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = OnTertiary, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Scanning...", color = OnTertiary)
                } else {
                    Text("Scan & Match Sources", color = OnTertiary, fontWeight = FontWeight.SemiBold)
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
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Matched Items", color = OnTertiaryContainer)
                    Text("${result.items.size}", color = OnTertiaryContainer, fontWeight = FontWeight.Bold)
                }
            }

            // Move button
            if (phase == "scanned" && result.items.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { showMoveDialog = true },
                    enabled = !isExecuting,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Tertiary),
                ) {
                    Text("Move ${result.items.size} Items", color = OnTertiary, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Execution logs
            if (executionLogs.isNotEmpty()) {
                Text("Execution Log", style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Background),
                ) {
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp).padding(12.dp)) {
                        items(executionLogs) { line ->
                            Text(
                                text = line, fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                                color = if (line.startsWith("ERROR")) Error else if (line.contains("Moved")) Success else OnSurfaceVariant,
                                modifier = Modifier.padding(vertical = 1.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Matched items list
            Text("Match Details", style = MaterialTheme.typography.labelMedium, color = OnSurfaceVariant)
            Spacer(Modifier.height(4.dp))

            if (result.items.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(result.items.entries.toList()) { (name, info) ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "→ ${info.series}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Secondary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    "[${info.source}: '${info.matchedToken}']",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 10.sp,
                                    color = OnSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
                ) {
                    LazyColumn(modifier = Modifier.weight(1f).padding(12.dp)) {
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
    }

    // Confirmation dialog
    if (showMoveDialog) {
        AlertDialog(
            onDismissRequest = { showMoveDialog = false },
            title = { Text("Confirm Move", fontWeight = FontWeight.Bold) },
            text = { Text("Move ${scanResult?.items?.size ?: 0} items to their matched series folders?") },
            confirmButton = {
                Button(
                    onClick = {
                        showMoveDialog = false
                        isExecuting = true
                        scope.launch {
                            val logs = withContext(Dispatchers.IO) {
                                MoveFromSource.executeMove(scanResult!!.items)
                            }
                            executionLogs = logs
                            phase = "moved"
                            isExecuting = false
                        }
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
}
