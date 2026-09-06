package com.example.lxsearch

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.lxsearch.ui.CreateFileListScreen
import com.example.lxsearch.ui.HomeScreen
import com.example.lxsearch.ui.InputFilesScreen
import com.example.lxsearch.ui.MoveFromSourceScreen
import com.example.lxsearch.ui.MoveToTempScreen
import com.example.lxsearch.ui.NameCircleScreen
import com.example.lxsearch.ui.SearchScreen

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(Main)
    val screenModifier = Modifier.safeDrawingPadding().padding(horizontal = 16.dp)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<Main> {
                HomeScreen(
                    onNavigate = { navKey -> backStack.add(navKey) },
                    modifier = screenModifier
                )
            }
            entry<CreateFileListRoute> {
                CreateFileListScreen(
                    onBack = { backStack.removeLastOrNull() },
                    onNavigateToInputFiles = { fileName -> backStack.add(InputFilesRoute(fileName)) },
                    modifier = screenModifier
                )
            }
            entry<SearchRoute> {
                SearchScreen(
                    onBack = { backStack.removeLastOrNull() },
                    onNavigateToInputFiles = { fileName -> backStack.add(InputFilesRoute(fileName)) },
                    modifier = screenModifier
                )
            }
            entry<NameCircleRoute> {
                NameCircleScreen(
                    onBack = { backStack.removeLastOrNull() },
                    onNavigateToInputFiles = { fileName -> backStack.add(InputFilesRoute(fileName)) },
                    modifier = screenModifier
                )
            }
            entry<MoveToTempRoute> {
                MoveToTempScreen(
                    onBack = { backStack.removeLastOrNull() },
                    modifier = screenModifier
                )
            }
            entry<MoveFromSourceRoute> {
                MoveFromSourceScreen(
                    onBack = { backStack.removeLastOrNull() },
                    onNavigateToInputFiles = { fileName -> backStack.add(InputFilesRoute(fileName)) },
                    modifier = screenModifier
                )
            }
            entry<InputFilesRoute> { route ->
                InputFilesScreen(
                    initialFileName = route.initialFileName,
                    onBack = { backStack.removeLastOrNull() },
                    modifier = screenModifier
                )
            }
        },
    )
}
