package com.example.lxsearch

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.lxsearch.service.JobState
import com.example.lxsearch.service.LXJob
import com.example.lxsearch.service.LXJobManager
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
    val context = LocalContext.current

    DisposableEffect(context) {
        val activity = context as? ComponentActivity
        fun checkAndNavigate(intent: Intent?) {
            if (intent?.getBooleanExtra("NAVIGATE_RUNNING_JOB", false) == true) {
                intent.removeExtra("NAVIGATE_RUNNING_JOB")
                val runningJob = (LXJobManager.jobState.value as? JobState.Running)?.job
                when (runningJob) {
                    is LXJob.CreateFileList -> backStack.add(CreateFileListRoute(runningJob.isV2))
                    is LXJob.NameCircle -> backStack.add(NameCircleRoute)
                    is LXJob.MoveToTempScan,
                    is LXJob.MoveToTempMove,
                    is LXJob.MoveToTempRemove -> backStack.add(MoveToTempRoute)
                    is LXJob.MoveFromSourceScan,
                    is LXJob.MoveFromSourceMove -> backStack.add(MoveFromSourceRoute)
                    null -> {}
                }
            }
        }

        val listener = androidx.core.util.Consumer<Intent> { newIntent ->
            checkAndNavigate(newIntent)
        }
        activity?.addOnNewIntentListener(listener)
        checkAndNavigate(activity?.intent)

        onDispose {
            activity?.removeOnNewIntentListener(listener)
        }
    }

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
            entry<CreateFileListRoute> { route ->
                CreateFileListScreen(
                    initialIsV2 = route.initialIsV2,
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
