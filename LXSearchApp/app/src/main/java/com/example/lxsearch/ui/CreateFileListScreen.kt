package com.example.lxsearch.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val scope = rememberCoroutineScope()

    var isV2 by remember { mutableStateOf(false) }
    var isRunning by remember { mutableStateOf(false) }
    var logLines by remember { mutableStateOf(listOf<String>()) }
    var resultCount by remember { mutableIntStateOf(-1) }

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
                onClick = { if (!isRunning) isV2 = false },
                label = { Text("V1 — Basic") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryContainer,
                    selectedLabelColor = OnPrimaryContainer,
                )
            )
            Spacer(Modifier.width(12.dp))
            FilterChip(
                selected = isV2,
                onClick = { if (!isRunning) isV2 = true },
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

        Spacer(Modifier.height(16.dp))

        // Run Button
        Button(
            onClick = {
                isRunning = true
                logLines = listOf("Starting ${if (isV2) "V2" else "V1"} scan...")
                resultCount = -1
                scope.launch {
                    val outputDir = MainActivity.getOutputDir(activity)
                    val inputDir = MainActivity.getInputDir(activity)
                    val count = withContext(Dispatchers.IO) {
                        if (isV2) {
                            CreateFileListV2.run(outputDir, inputDir) { progress ->
                                val newLine = "${progress.current}: ${progress.name}"
                                logLines = (logLines + newLine).takeLast(100)
                            }
                        } else {
                            CreateFileListV1.run(outputDir, inputDir) { progress ->
                                val newLine = "${progress.current}: ${progress.name}"
                                logLines = (logLines + newLine).takeLast(100)
                            }
                        }
                    }
                    resultCount = count
                    logLines = logLines + "✓ Complete! Processed $count items."
                    isRunning = false
                }
            },
            enabled = !isRunning,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
        ) {
            if (isRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = OnPrimary,
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(8.dp))
                Text("Running...", color = OnPrimary)
            } else {
                Text("Run ${if (isV2) "V2" else "V1"} Scan", color = OnPrimary, fontWeight = FontWeight.SemiBold)
            }
        }

        // Result count
        if (resultCount >= 0) {
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
                        "$resultCount",
                        color = OnSecondaryContainer,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Log output
        Text(
            "Log Output",
            style = MaterialTheme.typography.labelMedium,
            color = OnSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
        )
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
