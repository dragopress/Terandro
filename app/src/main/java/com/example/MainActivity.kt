package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Commit
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.ProjectRepository
import com.example.ui.components.PillBadge
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DiagnosticsScreen
import com.example.ui.screens.ErrorAnalyzerScreen
import com.example.ui.screens.GitHelperScreen
import com.example.ui.screens.SavedProjectsScreen
import com.example.ui.screens.ScriptGeneratorScreen
import com.example.ui.screens.StackConfigScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TerminalCyan
import com.example.ui.theme.TerminalGreen
import com.example.ui.viewmodel.BuildViewModel
import com.example.ui.viewmodel.BuildViewModelFactory
import com.example.ui.viewmodel.ScreenTab

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val database = AppDatabase.getDatabase(this)
        val repository = ProjectRepository(database.projectDao())
        val factory = BuildViewModelFactory(repository)

        setContent {
            MyApplicationTheme {
                val viewModel: BuildViewModel = viewModel(factory = factory)
                TermuxCIApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermuxCIApp(viewModel: BuildViewModel) {
    val context = LocalContext.current
    val currentProject by viewModel.currentProject.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val selectedScriptTab by viewModel.selectedScriptTab.collectAsStateWithLifecycle()
    val diagnosticReport by viewModel.diagnosticReport.collectAsStateWithLifecycle()
    val isAnalyzingDiagnostics by viewModel.isAnalyzingDiagnostics.collectAsStateWithLifecycle()
    val inputLog by viewModel.inputLog.collectAsStateWithLifecycle()
    val currentDiagnosis by viewModel.currentDiagnosis.collectAsStateWithLifecycle()
    val savedProjects by viewModel.savedProjects.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    // Git CI/CD helper state
    val gitRepoState by viewModel.gitRepoState.collectAsStateWithLifecycle()
    val gitConsoleLogs by viewModel.gitConsoleLogs.collectAsStateWithLifecycle()
    val commitMessageInput by viewModel.commitMessageInput.collectAsStateWithLifecycle()
    val isGitOperating by viewModel.isGitOperating.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = "Terminal",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "UnifiedBuild",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.3).sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Termux & CI Orchestrator",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(50))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = currentProject.primaryStack.displayName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            val navItems = listOf(
                ScreenTab.DASHBOARD to Icons.Default.Dashboard,
                ScreenTab.GIT to Icons.Default.Commit,
                ScreenTab.CONFIG to Icons.Default.Tune,
                ScreenTab.SCRIPTS to Icons.Default.Code,
                ScreenTab.DIAGNOSTICS to Icons.Default.Speed,
                ScreenTab.ERROR_ANALYZER to Icons.Default.BugReport,
                ScreenTab.SAVED to Icons.Default.Folder
            )
            val navScrollState = rememberScrollState()

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant
                ),
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(navScrollState)
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    navItems.forEach { (tab, icon) ->
                        val isSelected = activeTab == tab
                        Surface(
                            onClick = { viewModel.selectTab(tab) },
                            shape = RoundedCornerShape(50),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)) else null,
                            modifier = Modifier.testTag("nav_item_${tab.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = tab.title,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tab.title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                ScreenTab.DASHBOARD -> DashboardScreen(
                    currentProject = currentProject,
                    onStackSelected = { viewModel.switchPrimaryStack(it) },
                    onNavigateTab = { viewModel.selectTab(it) },
                    onRunDiagnostics = { viewModel.runDiagnostics(context) },
                    diagnosticScore = diagnosticReport?.overallScore
                )
                ScreenTab.GIT -> GitHelperScreen(
                    repoState = gitRepoState,
                    consoleLogs = gitConsoleLogs,
                    commitMessage = commitMessageInput,
                    isOperating = isGitOperating,
                    onFetch = { viewModel.executeGitFetch() },
                    onStatus = { viewModel.executeGitStatus() },
                    onCommit = { viewModel.executeGitCommit() },
                    onToggleStage = { path, stage -> viewModel.toggleStageFile(path, stage) },
                    onStageAll = { stage -> viewModel.stageAllFiles(stage) },
                    onDiscardChanges = { path -> viewModel.discardFileChanges(path) },
                    onAddSimulatedChange = { path, type -> viewModel.addSimulatedChange(path, type) },
                    onCommitMessageChanged = { viewModel.setCommitMessage(it) },
                    onApplyPrefix = { viewModel.applyCommitPrefix(it) },
                    onClearConsole = { viewModel.clearGitConsole() }
                )
                ScreenTab.CONFIG -> StackConfigScreen(
                    config = currentProject,
                    onConfigChanged = { viewModel.updateProject(it) },
                    onSaveProject = { viewModel.saveCurrentProject() },
                    onGenerateScripts = { viewModel.selectTab(ScreenTab.SCRIPTS) }
                )
                ScreenTab.SCRIPTS -> ScriptGeneratorScreen(
                    config = currentProject,
                    selectedTab = selectedScriptTab,
                    onTabSelected = { viewModel.selectScriptTab(it) }
                )
                ScreenTab.DIAGNOSTICS -> DiagnosticsScreen(
                    report = diagnosticReport,
                    isLoading = isAnalyzingDiagnostics,
                    onRunDiagnostics = { viewModel.runDiagnostics(context) }
                )
                ScreenTab.ERROR_ANALYZER -> ErrorAnalyzerScreen(
                    rawLog = inputLog,
                    diagnosis = currentDiagnosis,
                    onLogChanged = { viewModel.setInputLog(it) },
                    onAnalyze = { viewModel.analyzeCurrentLog() },
                    onSelectSample = { viewModel.loadSampleLog(it) }
                )
                ScreenTab.SAVED -> SavedProjectsScreen(
                    projects = savedProjects,
                    currentProjectId = currentProject.id,
                    onLoadProject = { viewModel.loadProject(it) },
                    onDeleteProject = { viewModel.deleteProject(it) },
                    onApplyPreset = { viewModel.loadProject(it) }
                )
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme { Greeting("Android") }
}
