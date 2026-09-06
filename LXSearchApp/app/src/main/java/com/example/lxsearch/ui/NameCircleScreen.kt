package com.example.lxsearch.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.lxsearch.core.NameCircleRelation
import com.example.lxsearch.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun NameCircleScreen(
    onBack: () -> Unit,
    onNavigateToInputFiles: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val jobState by com.example.lxsearch.service.LXJobManager.jobState.collectAsState()
    val allLogs by com.example.lxsearch.service.LXJobManager.recentLogs.collectAsState()

    val currentJob = (jobState as? com.example.lxsearch.service.JobState.Running)?.job
    val completedState = jobState as? com.example.lxsearch.service.JobState.Completed
    val isThisJobRunning = currentJob is com.example.lxsearch.service.LXJob.NameCircle
    val isThisJobCompleted = completedState?.job is com.example.lxsearch.service.LXJob.NameCircle
    val isAnyJobRunning = jobState is com.example.lxsearch.service.JobState.Running

    val logLines = if (isThisJobRunning || isThisJobCompleted) allLogs else emptyList()

    val result = if (isThisJobCompleted) {
        completedState.resultData as? NameCircleRelation.Result
    } else null

    var lastRunTimestamp by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(jobState) {
        lastRunTimestamp = com.example.lxsearch.data.JobHistoryManager.getLastRunFormatted(
            context,
            com.example.lxsearch.data.JobHistoryManager.JobKey.NAME_CIRCLE
        )
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
                "Name-Circle Relations",
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

        Spacer(Modifier.height(16.dp))

        // Description
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
        ) {
            Text(
                text = "Scans series folders, extracts circle/parody names from folder brackets and ComicInfo metadata. Builds a mapping and outputs JSON, CSV, and a duplicate report.",
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
                        tint = Secondary,
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

        // Run Button
        Button(
            onClick = {
                com.example.lxsearch.service.LXJobManager.startJob(
                    context,
                    com.example.lxsearch.service.LXJob.NameCircle
                )
            },
            enabled = !isAnyJobRunning,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Secondary),
        ) {
            if (isThisJobRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = OnSecondary,
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(8.dp))
                Text("Running in background...", color = OnSecondary)
            } else {
                Text("Build Relations", color = OnSecondary, fontWeight = FontWeight.SemiBold)
            }
        }

        // Result summary
        result?.let { r ->
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SecondaryContainer),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Series Processed", color = OnSecondaryContainer)
                        Text("${r.seriesCount}", color = OnSecondaryContainer, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Duplicates Found", color = OnSecondaryContainer)
                        Text(
                            "${r.duplicateCount}",
                            color = if (r.duplicateCount > 0) Warning else OnSecondaryContainer,
                            fontWeight = FontWeight.Bold,
                        )
                    }
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
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
