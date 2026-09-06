package com.example.lxsearch.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lxsearch.data.InputFileManager
import com.example.lxsearch.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputFilesScreen(
    initialFileName: String? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Managed files list
    val managedFiles = remember { InputFileManager.MANAGED_FILES }

    // Current selected file
    var selectedFileName by remember {
        mutableStateOf(
            initialFileName?.takeIf { name -> managedFiles.any { it.fileName == name } }
                ?: managedFiles.first().fileName
        )
    }

    // Editor state
    var currentContent by remember { mutableStateOf("") }
    var savedContent by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    // Quick add rule state
    var newRuleText by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }

    // Dialog states
    var showResetDialog by remember { mutableStateOf(false) }
    var pendingSwitchFileName by remember { mutableStateOf<String?>(null) }
    var showUnsavedDialog by remember { mutableStateOf(false) }
    var isBackPending by remember { mutableStateOf(false) }

    val isModified = currentContent != savedContent
    val currentFileInfo = managedFiles.find { it.fileName == selectedFileName }

    // Function to load file content
    fun loadFile(fileName: String) {
        isLoading = true
        scope.launch {
            val content = withContext(Dispatchers.IO) {
                InputFileManager.readFileContent(context, fileName)
            }
            currentContent = content
            savedContent = content
            isLoading = false
        }
    }

    // Load initial file
    LaunchedEffect(selectedFileName) {
        loadFile(selectedFileName)
    }

    // Function to save content
    fun saveCurrentFile() {
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                InputFileManager.saveFileContent(context, selectedFileName, currentContent)
            }
            if (result.isSuccess) {
                savedContent = currentContent
                snackbarHostState.showSnackbar("Saved ${currentFileInfo?.shortName ?: selectedFileName}")
            } else {
                snackbarHostState.showSnackbar("Failed to save: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    // Function to handle tab selection with unsaved check
    fun selectTab(targetFileName: String) {
        if (targetFileName == selectedFileName) return
        if (isModified) {
            pendingSwitchFileName = targetFileName
            showUnsavedDialog = true
        } else {
            selectedFileName = targetFileName
        }
    }

    // Function to handle back with unsaved check
    fun handleBack() {
        if (isModified) {
            isBackPending = true
            showUnsavedDialog = true
        } else {
            onBack()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── Top Bar ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { handleBack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Primary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Input Files Editor",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (isModified) "● Unsaved changes" else "✓ Synced to disk",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isModified) Tertiary else Success,
                        fontSize = 11.sp,
                    )
                }

                // Search toggle button
                IconButton(onClick = { isSearchVisible = !isSearchVisible }) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search in file",
                        tint = if (isSearchVisible) Primary else OnSurfaceVariant
                    )
                }

                // Reset button
                IconButton(onClick = { showResetDialog = true }) {
                    Icon(
                        Icons.Default.RestartAlt,
                        contentDescription = "Reset to default",
                        tint = OnSurfaceVariant
                    )
                }

                // Save button
                IconButton(
                    onClick = { saveCurrentFile() },
                    enabled = isModified && !isLoading
                ) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = "Save file",
                        tint = if (isModified) Primary else OnSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── File Selection Chips ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (file in managedFiles) {
                    val isSelected = file.fileName == selectedFileName
                    val tabModified = isSelected && isModified

                    FilterChip(
                        selected = isSelected,
                        onClick = { selectTab(file.fileName) },
                        label = {
                            Text(
                                text = if (tabModified) "${file.shortName} •" else file.shortName,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryContainer,
                            selectedLabelColor = OnPrimaryContainer,
                        )
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── File Info Banner ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentFileInfo?.title ?: selectedFileName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = currentFileInfo?.description ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant,
                        fontSize = 12.sp,
                    )

                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = currentFileInfo?.usedBy ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = Secondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        )

                        val stats = InputFileManager.computeStats(currentContent)
                        Text(
                            text = "${stats.nonEmptyCount} active / ${stats.lineCount} lines",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariant,
                            fontSize = 11.sp,
                        )
                    }
                }
            }

            // ── Search Bar (Collapsible) ──
            AnimatedVisibility(visible = isSearchVisible) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search pattern or keyword...") },
                        leadingIcon = { Icon(Icons.Default.Search, "Search", tint = OnSurfaceVariant) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                val matchCount = currentContent.lines().count {
                                    it.contains(searchQuery, ignoreCase = true)
                                }
                                Text(
                                    text = "$matchCount matches",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (matchCount > 0) Primary else Error,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = Outline,
                            focusedContainerColor = SurfaceVariant,
                            unfocusedContainerColor = SurfaceVariant,
                        )
                    )
                }
            }

            // ── Quick Add Entry Row ──
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newRuleText,
                    onValueChange = { newRuleText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            currentFileInfo?.syntaxHint ?: "Add new rule...",
                            fontSize = 12.sp
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Outline,
                        focusedContainerColor = SurfaceVariant,
                        unfocusedContainerColor = SurfaceVariant,
                    )
                )

                Spacer(Modifier.width(8.dp))

                Button(
                    onClick = {
                        val trimmed = newRuleText.trim()
                        if (trimmed.isNotEmpty()) {
                            val separator = if (currentContent.isEmpty() || currentContent.endsWith("\n")) "" else "\n"
                            currentContent = "$currentContent$separator$trimmed\n"
                            newRuleText = ""
                        }
                    },
                    enabled = newRuleText.isNotBlank(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Default.Add, "Add rule", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add", fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Main Text Editor ──
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            } else {
                OutlinedTextField(
                    value = currentContent,
                    onValueChange = { currentContent = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Outline,
                        focusedContainerColor = Background,
                        unfocusedContainerColor = Background,
                    ),
                    placeholder = {
                        Text(
                            "Enter one rule per line...",
                            color = OnSurfaceVariant.copy(alpha = 0.5f),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── Bottom Action Row ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Revert button
                OutlinedButton(
                    onClick = {
                        currentContent = savedContent
                    },
                    enabled = isModified && !isLoading,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("Revert", fontSize = 13.sp)
                }

                // Save button
                Button(
                    onClick = { saveCurrentFile() },
                    enabled = isModified && !isLoading,
                    modifier = Modifier.weight(1.5f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Icon(Icons.Default.Save, "Save", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save Changes", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }
    }

    // ── Dialog: Reset to Default Confirmation ──
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset to Default?") },
            text = {
                Text(
                    "This will replace all contents of \"$selectedFileName\" with the original bundled default template. Any custom rules you added will be removed."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                InputFileManager.resetToDefault(context, selectedFileName)
                            }
                            if (result.isSuccess) {
                                val text = result.getOrNull() ?: ""
                                currentContent = text
                                savedContent = text
                                snackbarHostState.showSnackbar("Restored $selectedFileName to defaults")
                            } else {
                                snackbarHostState.showSnackbar("Failed to reset: ${result.exceptionOrNull()?.message}")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Error)
                ) {
                    Text("Reset", color = OnError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ── Dialog: Unsaved Changes Confirmation ──
    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = {
                showUnsavedDialog = false
                pendingSwitchFileName = null
                isBackPending = false
            },
            title = { Text("Unsaved Changes") },
            text = {
                Text(
                    "You have unsaved changes in \"$selectedFileName\". Do you want to save your changes before leaving?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUnsavedDialog = false
                        // Save then proceed
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                InputFileManager.saveFileContent(context, selectedFileName, currentContent)
                            }
                            savedContent = currentContent
                            if (isBackPending) {
                                onBack()
                            } else if (pendingSwitchFileName != null) {
                                selectedFileName = pendingSwitchFileName!!
                                pendingSwitchFileName = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("Save & Proceed")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            showUnsavedDialog = false
                            // Discard and proceed
                            currentContent = savedContent
                            if (isBackPending) {
                                onBack()
                            } else if (pendingSwitchFileName != null) {
                                selectedFileName = pendingSwitchFileName!!
                                pendingSwitchFileName = null
                            }
                        }
                    ) {
                        Text("Discard")
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            showUnsavedDialog = false
                            pendingSwitchFileName = null
                            isBackPending = false
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}
