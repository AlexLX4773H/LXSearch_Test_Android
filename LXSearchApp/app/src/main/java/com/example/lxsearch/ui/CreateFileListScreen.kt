package com.example.lxsearch.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
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
import com.example.lxsearch.core.CreateFileListV1
import com.example.lxsearch.core.CreateFileListV2
import com.example.lxsearch.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateFileListScreen(
    onBack: () -> Unit,
    onNavigateToInputFiles: ((String) -> Unit)? = null,
    initialIsV2: Boolean? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val jobState by com.example.lxsearch.service.LXJobManager.jobState.collectAsState()
    val allLogs by com.example.lxsearch.service.LXJobManager.recentLogs.collectAsState()

    val currentJob = (jobState as? com.example.lxsearch.service.JobState.Running)?.job
    val completedJob = (jobState as? com.example.lxsearch.service.JobState.Completed)?.job
    val cancelledJob = (jobState as? com.example.lxsearch.service.JobState.Cancelled)?.job

    var isV2 by remember {
        mutableStateOf(
            initialIsV2
                ?: (currentJob as? com.example.lxsearch.service.LXJob.CreateFileList)?.isV2
                ?: (completedJob as? com.example.lxsearch.service.LXJob.CreateFileList)?.isV2
                ?: (cancelledJob as? com.example.lxsearch.service.LXJob.CreateFileList)?.isV2
                ?: false
        )
    }

    var showCancelDialog by remember { mutableStateOf(false) }

    LaunchedEffect(initialIsV2) {
        if (initialIsV2 != null) {
            isV2 = initialIsV2
        }
    }

    LaunchedEffect(currentJob) {
        if (currentJob is com.example.lxsearch.service.LXJob.CreateFileList) {
            isV2 = currentJob.isV2
        }
    }

    val isThisJobRunning = currentJob is com.example.lxsearch.service.LXJob.CreateFileList && currentJob.isV2 == isV2
    val isThisJobCompleted = completedJob is com.example.lxsearch.service.LXJob.CreateFileList && completedJob.isV2 == isV2
    val isThisJobCancelled = cancelledJob is com.example.lxsearch.service.LXJob.CreateFileList && cancelledJob.isV2 == isV2
    val isAnyJobRunning = jobState is com.example.lxsearch.service.JobState.Running

    val logLines = if (isThisJobRunning || isThisJobCompleted || isThisJobCancelled) allLogs else emptyList()

    val completedCount = if (isThisJobCompleted) {
        (jobState as? com.example.lxsearch.service.JobState.Completed)?.resultData as? Int
    } else null

    var lastRunTimestamp by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isV2, jobState) {
        val key = if (isV2) com.example.lxsearch.data.JobHistoryManager.JobKey.CREATE_FILE_LIST_V2 else com.example.lxsearch.data.JobHistoryManager.JobKey.CREATE_FILE_LIST_V1
        lastRunTimestamp = com.example.lxsearch.data.JobHistoryManager.getLastRunFormatted(context, key)
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
                "Create File List",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.weight(1f))
            if (onNavigateToInputFiles != null) {
                IconButton(onClick = { onNavigateToInputFiles("exclude_folders_chapter_re.txt") }) {
                    Icon(Icons.Default.Tune, "Edit exclusion rules", tint = Primary)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Version Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = !isV2,
                onClick = {
                    if (isV2) {
                        isV2 = false
                        if (!isAnyJobRunning) {
                            com.example.lxsearch.service.LXJobManager.clearLogs()
                            com.example.lxsearch.service.LXJobManager.resetToIdle()
                        }
                    }
                },
                label = { Text("V1 — Basic") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryContainer,
                    selectedLabelColor = OnPrimaryContainer,
                )
            )
            Spacer(Modifier.width(12.dp))
            FilterChip(
                selected = isV2,
                onClick = {
                    if (!isV2) {
                        isV2 = true
                        if (!isAnyJobRunning) {
                            com.example.lxsearch.service.LXJobManager.clearLogs()
                            com.example.lxsearch.service.LXJobManager.resetToIdle()
                        }
                    }
                },
                label = { Text("V2 — Enhanced") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryContainer,
                    selectedLabelColor = OnPrimaryContainer,
                )
            )
        }

        Spacer(Modifier.height(8.dp))

        // Description
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
        ) {
            Text(
                text = if (isV2)
                    "Enhanced scan with ComicInfo.xml/json parsing, parodies, groups, genre, writer, penciller, file sizes and statistics. Outputs filename_list_v2.csv."
                else
                    "Scans configured directories for folders and files, extracts bracket metadata from names. Outputs filename_list.csv, list.txt, count.txt.",
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant,
                modifier = Modifier.padding(12.dp),
            )
        }

        Spacer(Modifier.height(8.dp))

        // Last Run Timestamp
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceVariant.copy(alpha = 0.6f)),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Last run",
                        tint = Primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Last Run",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = OnSurfaceVariant
                    )
                }
                Text(
                    text = lastRunTimestamp ?: "Never run",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = if (lastRunTimestamp != null) MaterialTheme.colorScheme.onSurface else OnSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Run / Cancel Action Area
        if (isThisJobRunning) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Primary.copy(alpha = 0.85f),
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = OnPrimary,
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Running in background...",
                            color = OnPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Button(
                    onClick = { showCancelDialog = true },
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Cancel",
                        color = MaterialTheme.colorScheme.onError,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            Button(
                onClick = {
                    com.example.lxsearch.service.LXJobManager.startJob(
                        context,
                        com.example.lxsearch.service.LXJob.CreateFileList(isV2)
                    )
                },
                enabled = !isAnyJobRunning,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
            ) {
                Text("Run ${if (isV2) "V2" else "V1"} Scan", color = OnPrimary, fontWeight = FontWeight.SemiBold)
            }
        }

        if (showCancelDialog) {
            AlertDialog(
                onDismissRequest = { showCancelDialog = false },
                title = { Text("Abort ${if (isV2) "V2" else "V1"} Scan?") },
                text = {
                    Text("Are you sure you want to cancel the running job? Scanning will stop immediately and existing index files will be kept untouched.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showCancelDialog = false
                            com.example.lxsearch.service.LXJobManager.cancelJob(context)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Abort Job", color = MaterialTheme.colorScheme.onError)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCancelDialog = false }) {
                        Text("Keep Running")
                    }
                }
            )
        }

        // Result count
        if (completedCount != null && completedCount >= 0) {
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SecondaryContainer),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Total Items Processed", color = OnSecondaryContainer)
                    Text(
                        "$completedCount",
                        color = OnSecondaryContainer,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Log output
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Log Output",
                style = MaterialTheme.typography.labelMedium,
                color = OnSurfaceVariant,
            )
            if (logLines.isNotEmpty() && !isAnyJobRunning) {
                Text(
                    "Clear",
                    style = MaterialTheme.typography.labelMedium,
                    color = Primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable {
                        com.example.lxsearch.service.LXJobManager.clearLogs()
                        com.example.lxsearch.service.LXJobManager.resetToIdle()
                    }
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Background),
        ) {
            val scrollState = rememberScrollState()
            LaunchedEffect(logLines.size) {
                if (logLines.isNotEmpty()) {
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(12.dp),
            ) {
                for (line in logLines) {
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = if (line.startsWith("✓")) Success else OnSurfaceVariant,
                        modifier = Modifier.padding(vertical = 1.dp),
                    )
                }
            }
        }
    }
}
