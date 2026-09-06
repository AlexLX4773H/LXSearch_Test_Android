package com.example.lxsearch.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lxsearch.MainActivity
import com.example.lxsearch.core.SearchEngine
import com.example.lxsearch.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onNavigateToInputFiles: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val scope = rememberCoroutineScope()

    val searchEngine = remember { SearchEngine() }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<SearchEngine.SearchResponse?>(null) }
    var showHelp by remember { mutableStateOf(true) }

    // Load data on first composition
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val outputDir = MainActivity.getOutputDir(activity)
            val inputDir = MainActivity.getInputDir(activity)
            val success = searchEngine.load(outputDir, inputDir)
            if (!success) {
                loadError = "Could not load filename_list.csv. Run 'Create File List' first."
            }
        }
        isLoading = false
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
                "Search Files",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.weight(1f))
            if (searchEngine.isDataLoaded()) {
                Text(
                    "${searchEngine.getDataCount()} entries",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                )
                Spacer(Modifier.width(4.dp))
            }
            if (onNavigateToInputFiles != null) {
                IconButton(onClick = { onNavigateToInputFiles("exclude_input_chapter_sep.txt") }) {
                    Icon(Icons.Default.Tune, "Edit chapter separators", tint = Primary)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
            return
        }

        if (loadError != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ErrorContainer),
            ) {
                Text(
                    loadError!!,
                    color = OnErrorContainer,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            return
        }

        // Search input
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Enter search query...") },
            leadingIcon = { Icon(Icons.Default.Search, "Search", tint = OnSurfaceVariant) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                query = ""
                                searchResults = null
                                showHelp = true
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = OnSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        Button(
                            onClick = {
                                showHelp = false
                                scope.launch {
                                    searchResults = withContext(Dispatchers.IO) {
                                        searchEngine.search(query)
                                    }
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        ) {
                            Text("Go", color = OnPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                            val clipText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
                            if (!clipText.isNullOrBlank()) {
                                val trimmed = clipText.trim()
                                query = trimmed
                                showHelp = false
                                scope.launch {
                                    searchResults = withContext(Dispatchers.IO) {
                                        searchEngine.search(trimmed)
                                    }
                                }
                            }
                        },
                        modifier = Modifier.padding(end = 4.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    ) {
                        Text("Paste & Go", color = OnPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = Outline,
                focusedContainerColor = SurfaceVariant,
                unfocusedContainerColor = SurfaceVariant,
            ),
        )

        Spacer(Modifier.height(8.dp))

        // Help or Results
        if (showHelp && searchResults == null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Search Modes", fontWeight = FontWeight.SemiBold, color = Primary, fontSize = 13.sp)
                    Spacer(Modifier.height(6.dp))
                    HelpRow("[text]", "Default name search (parsed & pressed)")
                    HelpRow("#text", "Exact folder path search")
                    HelpRow("-text", "Use '-' as separator")
                    HelpRow("@ctext", "Use char 'c' as separator")
                    HelpRow("~text~skip~more", "Strip ~...~ blocks before search")
                }
            }
        }

        // Search results
        searchResults?.let { response ->
            Spacer(Modifier.height(4.dp))

            // Parsed terms and count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Terms: ${response.parsedTerms.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "Count: ${response.count}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (response.found) Success else Error,
                )
            }

            Spacer(Modifier.height(8.dp))

            if (response.results.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
                ) {
                    Text(
                        "No results found.",
                        modifier = Modifier.padding(16.dp),
                        color = OnSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(response.results) { result ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    result.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    result.folder,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnSurfaceVariant,
                                    fontSize = 11.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HelpRow(prefix: String, description: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            prefix,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = Secondary,
            modifier = Modifier.width(100.dp),
        )
        Text(
            description,
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariant,
        )
    }
}
