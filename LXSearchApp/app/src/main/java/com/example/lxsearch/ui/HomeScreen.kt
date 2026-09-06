package com.example.lxsearch.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.example.lxsearch.*
import com.example.lxsearch.service.LXJob
import com.example.lxsearch.service.LXJobManager
import com.example.lxsearch.theme.*

@Composable
fun HomeScreen(
    onNavigate: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val jobState by LXJobManager.jobState.collectAsState()
    val runningJob = (jobState as? com.example.lxsearch.service.JobState.Running)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Title
        Text(
            text = "LX Search",
            style = MaterialTheme.typography.headlineLarge,
            color = Primary,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "File Management Tools",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant,
            modifier = Modifier.padding(bottom = if (runningJob != null) 16.dp else 28.dp)
        )

        // Background Running Job Banner
        if (runningJob != null) {
            Card(
                onClick = {
                    when (runningJob.job) {
                        is LXJob.CreateFileList -> onNavigate(CreateFileListRoute(runningJob.job.isV2))
                        is LXJob.NameCircle -> onNavigate(NameCircleRoute)
                        is LXJob.MoveToTempScan,
                        is LXJob.MoveToTempMove,
                        is LXJob.MoveToTempRemove -> onNavigate(MoveToTempRoute)
                        is LXJob.MoveFromSourceScan,
                        is LXJob.MoveFromSourceMove -> onNavigate(MoveFromSourceRoute)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryContainer),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = OnPrimaryContainer
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "JOB RUNNING IN BACKGROUND",
                            style = MaterialTheme.typography.labelSmall,
                            color = Primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = runningJob.job.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = OnPrimaryContainer,
                        )
                    }
                    Text(
                        text = "View ›",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                }
            }
        }

        // ── Build Tools Section ──
        SectionHeader("Build Index")
        Spacer(Modifier.height(8.dp))

        ActionCard(
            title = "Create File List",
            description = "Scan folders and files, extract bracket metadata, and parse ComicInfo (V1 / V2)",
            accentColor = Primary,
            onClick = { onNavigate(CreateFileListRoute()) }
        )
        Spacer(Modifier.height(12.dp))

        ActionCard(
            title = "Build Name-Circle Relations",
            description = "Scan series folders, extract circle/parody names, build mapping JSON",
            accentColor = Secondary,
            onClick = { onNavigate(NameCircleRoute) }
        )

        Spacer(Modifier.height(24.dp))

        // ── Search Section ──
        SectionHeader("Search")
        Spacer(Modifier.height(8.dp))

        ActionCard(
            title = "Search Files",
            description = "Search indexed files by name with multiple modes: default, exact, dash-separated, custom",
            accentColor = Primary,
            onClick = { onNavigate(SearchRoute) }
        )

        Spacer(Modifier.height(24.dp))

        // ── Move Tools Section ──
        SectionHeader("Move & Cleanup")
        Spacer(Modifier.height(8.dp))

        ActionCard(
            title = "Move To Temp",
            description = "Find empty/incomplete folders and move them to temp destinations",
            accentColor = Tertiary,
            onClick = { onNavigate(MoveToTempRoute) }
        )
        Spacer(Modifier.height(12.dp))

        ActionCard(
            title = "Move From Source",
            description = "Match source releases to series folders and move them to destinations",
            accentColor = Tertiary,
            onClick = { onNavigate(MoveFromSourceRoute) }
        )

        Spacer(Modifier.height(24.dp))

        // ── Configuration Section ──
        SectionHeader("Configuration")
        Spacer(Modifier.height(8.dp))

        ActionCard(
            title = "Edit Input Files",
            description = "Customize folder regexes, bracket filters, and chapter separators",
            accentColor = Secondary,
            onClick = { onNavigate(InputFilesRoute()) }
        )

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = OnSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp)
    )
}

@Composable
private fun ActionCard(
    title: String,
    description: String,
    accentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Accent bar
            Surface(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(2.dp),
                color = accentColor,
            ) {}

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                )
            }

            Text(
                text = "›",
                style = MaterialTheme.typography.headlineMedium,
                color = accentColor,
            )
        }
    }
}
