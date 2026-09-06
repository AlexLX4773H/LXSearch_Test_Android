package com.example.lxsearch.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.example.lxsearch.*
import com.example.lxsearch.theme.*

@Composable
fun HomeScreen(
    onNavigate: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
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
            modifier = Modifier.padding(bottom = 28.dp)
        )

        // ── Build Tools Section ──
        SectionHeader("Build Index")
        Spacer(Modifier.height(8.dp))

        ActionCard(
            title = "Create File List (V1)",
            description = "Scan folders and files, extract bracket metadata, generate filename_list.csv",
            accentColor = Primary,
            onClick = { onNavigate(CreateFileListRoute) }
        )
        Spacer(Modifier.height(12.dp))

        ActionCard(
            title = "Create File List (V2)",
            description = "Enhanced scan with ComicInfo.xml/json parsing, file sizes, and statistics",
            accentColor = Primary,
            onClick = { onNavigate(CreateFileListRoute) }
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
