package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.BuildLogEntity
import com.example.data.ProjectRepository
import com.example.engine.EnvironmentDiagnosticEngine
import com.example.engine.ErrorAnalyzerEngine
import com.example.engine.GitHelperEngine
import com.example.model.BuildErrorDiagnosis
import com.example.model.DiagnosticReport
import com.example.model.GitCommandResult
import com.example.model.GitFileStatusType
import com.example.model.GitRepositoryState
import com.example.model.ProjectConfig
import com.example.model.StackType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ScreenTab(val title: String) {
    DASHBOARD("Overview"),
    GIT("Git Ops"),
    CONFIG("Pipeline"),
    SCRIPTS("Shell Layer"),
    DIAGNOSTICS("Diagnostics"),
    ERROR_ANALYZER("Error Audit"),
    SAVED("Projects")
}

enum class ScriptTab(val title: String, val filename: String) {
    BUILD_BRIDGE("build-bridge.sh", "build-bridge.sh"),
    CI_WORKFLOW("ci.yml", ".github/workflows/ci.yml"),
    TERMUX_BOOTSTRAP("termux-bootstrap.sh", "termux-bootstrap.sh"),
    VERIFY_UNMASKED("verify-unmasked.sh", "verify-unmasked.sh")
}

class BuildViewModel(
    private val repository: ProjectRepository
) : ViewModel() {

    val savedProjects: StateFlow<List<ProjectConfig>> = repository.allProjects
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ProjectRepository.getDefaultProjectPresets()
        )

    private val _currentProject = MutableStateFlow(
        ProjectConfig(
            id = 1,
            name = "Termux & CI Universal Build",
            primaryStack = StackType.PYTHON,
            packageManager = "pip",
            enableInstall = true,
            enableLint = true,
            enableBuild = true,
            enableTest = true,
            termuxCpuLimit = 2,
            termuxForceSourceBuild = true,
            strictModePipefail = true,
            unmaskRealErrors = true
        )
    )
    val currentProject: StateFlow<ProjectConfig> = _currentProject.asStateFlow()

    private val _activeTab = MutableStateFlow(ScreenTab.DASHBOARD)
    val activeTab: StateFlow<ScreenTab> = _activeTab.asStateFlow()

    private val _selectedScriptTab = MutableStateFlow(ScriptTab.BUILD_BRIDGE)
    val selectedScriptTab: StateFlow<ScriptTab> = _selectedScriptTab.asStateFlow()

    private val _diagnosticReport = MutableStateFlow<DiagnosticReport?>(null)
    val diagnosticReport: StateFlow<DiagnosticReport?> = _diagnosticReport.asStateFlow()

    private val _isAnalyzingDiagnostics = MutableStateFlow(false)
    val isAnalyzingDiagnostics: StateFlow<Boolean> = _isAnalyzingDiagnostics.asStateFlow()

    private val _inputLog = MutableStateFlow(
        ErrorAnalyzerEngine.getSampleLogs().first().second
    )
    val inputLog: StateFlow<String> = _inputLog.asStateFlow()

    private val _currentDiagnosis = MutableStateFlow<BuildErrorDiagnosis?>(
        ErrorAnalyzerEngine.analyzeLog(ErrorAnalyzerEngine.getSampleLogs().first().second)
    )
    val currentDiagnosis: StateFlow<BuildErrorDiagnosis?> = _currentDiagnosis.asStateFlow()

    // Git CI/CD Operations State
    private val _gitRepoState = MutableStateFlow(GitHelperEngine.getInitialState())
    val gitRepoState: StateFlow<GitRepositoryState> = _gitRepoState.asStateFlow()

    private val _gitConsoleLogs = MutableStateFlow<List<GitCommandResult>>(
        listOf(
            GitCommandResult(
                command = "git status",
                exitCode = 0,
                output = "On branch main\nYour branch is ahead of 'origin/main' by 1 commit.\nChanges to be committed:\n  (use \"git restore --staged <file>...\" to unstage)\n\tmodified:  scripts/build-bridge.sh\n\nChanges not staged for commit:\n\tmodified:  .github/workflows/ci.yml\n\nUntracked files:\n\ttests/test_exit_codes.py"
            )
        )
    )
    val gitConsoleLogs: StateFlow<List<GitCommandResult>> = _gitConsoleLogs.asStateFlow()

    private val _commitMessageInput = MutableStateFlow("")
    val commitMessageInput: StateFlow<String> = _commitMessageInput.asStateFlow()

    private val _isGitOperating = MutableStateFlow(false)
    val isGitOperating: StateFlow<Boolean> = _isGitOperating.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        // Hydrate persisted git commits if any exist
        viewModelScope.launch {
            repository.allGitCommits.collect { savedCommits ->
                if (savedCommits.isNotEmpty()) {
                    val current = _gitRepoState.value
                    val combinedHistory = (savedCommits + current.commitHistory).distinctBy { it.sha }
                    _gitRepoState.value = current.copy(commitHistory = combinedHistory)
                }
            }
        }
    }

    fun selectTab(tab: ScreenTab) {
        _activeTab.value = tab
    }

    fun selectScriptTab(tab: ScriptTab) {
        _selectedScriptTab.value = tab
    }

    fun updateProject(config: ProjectConfig) {
        _currentProject.value = config
    }

    fun switchPrimaryStack(stack: StackType) {
        val current = _currentProject.value
        _currentProject.value = current.copy(
            primaryStack = stack,
            packageManager = stack.defaultPackageManager,
            name = "${stack.displayName} Unified Service"
        )
    }

    fun saveCurrentProject() {
        viewModelScope.launch {
            val id = repository.saveProject(_currentProject.value)
            _currentProject.value = _currentProject.value.copy(id = id)
            _snackbarMessage.value = "Project '${_currentProject.value.name}' saved successfully!"
        }
    }

    fun loadProject(config: ProjectConfig) {
        _currentProject.value = config
        _snackbarMessage.value = "Loaded project: ${config.name}"
    }

    fun deleteProject(id: Long) {
        viewModelScope.launch {
            repository.deleteProject(id)
            _snackbarMessage.value = "Project removed"
        }
    }

    fun runDiagnostics(context: Context) {
        viewModelScope.launch {
            _isAnalyzingDiagnostics.value = true
            try {
                val report = EnvironmentDiagnosticEngine.runDiagnostics(context)
                _diagnosticReport.value = report
            } finally {
                _isAnalyzingDiagnostics.value = false
            }
        }
    }

    fun setInputLog(log: String) {
        _inputLog.value = log
    }

    fun analyzeCurrentLog() {
        val diagnosis = ErrorAnalyzerEngine.analyzeLog(_inputLog.value)
        _currentDiagnosis.value = diagnosis
        viewModelScope.launch {
            repository.saveBuildLog(
                BuildLogEntity(
                    projectId = _currentProject.value.id,
                    title = diagnosis.title,
                    rawLog = _inputLog.value,
                    detectedCategory = diagnosis.category.name,
                    exitCode = diagnosis.exitCode
                )
            )
        }
    }

    fun loadSampleLog(index: Int) {
        val samples = ErrorAnalyzerEngine.getSampleLogs()
        if (index in samples.indices) {
            val sample = samples[index]
            _inputLog.value = sample.second
            _currentDiagnosis.value = ErrorAnalyzerEngine.analyzeLog(sample.second)
        }
    }

    // Git Operations
    fun executeGitStatus() {
        val (newState, result) = GitHelperEngine.executeStatus(_gitRepoState.value)
        _gitRepoState.value = newState
        _gitConsoleLogs.value = listOf(result) + _gitConsoleLogs.value
    }

    fun executeGitFetch() {
        viewModelScope.launch {
            _isGitOperating.value = true
            try {
                val (newState, result) = GitHelperEngine.executeFetch(_gitRepoState.value)
                _gitRepoState.value = newState
                _gitConsoleLogs.value = listOf(result) + _gitConsoleLogs.value
                _snackbarMessage.value = newState.lastFetchSummary ?: "Fetch completed"
            } finally {
                _isGitOperating.value = false
            }
        }
    }

    fun executeGitCommit() {
        val message = _commitMessageInput.value
        val (newState, result) = GitHelperEngine.executeCommit(
            state = _gitRepoState.value,
            message = message
        )
        _gitRepoState.value = newState
        _gitConsoleLogs.value = listOf(result) + _gitConsoleLogs.value

        if (result.success) {
            _commitMessageInput.value = ""
            _snackbarMessage.value = "Commit created: ${result.command}"
            // Persist the newly created commit in Room
            val newestCommit = newState.commitHistory.firstOrNull()
            if (newestCommit != null) {
                viewModelScope.launch {
                    repository.saveGitCommit(newestCommit)
                }
            }
        } else {
            _snackbarMessage.value = "Commit failed: ${result.output.lines().firstOrNull()}"
        }
    }

    fun toggleStageFile(path: String, stage: Boolean) {
        val (newState, result) = GitHelperEngine.toggleStageFile(_gitRepoState.value, path, stage)
        _gitRepoState.value = newState
        _gitConsoleLogs.value = listOf(result) + _gitConsoleLogs.value
    }

    fun stageAllFiles(stage: Boolean) {
        val (newState, result) = GitHelperEngine.stageAll(_gitRepoState.value, stage)
        _gitRepoState.value = newState
        _gitConsoleLogs.value = listOf(result) + _gitConsoleLogs.value
    }

    fun discardFileChanges(path: String) {
        val (newState, result) = GitHelperEngine.discardChanges(_gitRepoState.value, path)
        _gitRepoState.value = newState
        _gitConsoleLogs.value = listOf(result) + _gitConsoleLogs.value
        _snackbarMessage.value = "Discarded changes in $path"
    }

    fun addSimulatedChange(path: String, type: GitFileStatusType) {
        val (newState, result) = GitHelperEngine.addSimulatedChange(_gitRepoState.value, path, type)
        _gitRepoState.value = newState
        _gitConsoleLogs.value = listOf(result) + _gitConsoleLogs.value
        _snackbarMessage.value = "Added mock change: $path"
    }

    fun setCommitMessage(message: String) {
        _commitMessageInput.value = message
    }

    fun applyCommitPrefix(prefix: String) {
        val current = _commitMessageInput.value
        if (!current.startsWith(prefix)) {
            _commitMessageInput.value = "$prefix $current".trim()
        }
    }

    fun clearGitConsole() {
        _gitConsoleLogs.value = emptyList()
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }
}

class BuildViewModelFactory(
    private val repository: ProjectRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BuildViewModel::class.java)) {
            return BuildViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
