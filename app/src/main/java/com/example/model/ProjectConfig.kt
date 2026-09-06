package com.example.model

data class ProjectConfig(
    val id: Long = 0,
    val name: String = "My Robust Project",
    val primaryStack: StackType = StackType.PYTHON,
    val secondaryStack: StackType? = null,
    val packageManager: String = "pip",
    // Phases
    val enableInstall: Boolean = true,
    val enableLint: Boolean = true,
    val enableBuild: Boolean = true,
    val enableTest: Boolean = true,
    val enablePackage: Boolean = false,
    // Termux Safeguards
    val termuxCpuLimit: Int = 2,
    val termuxMaxMemoryMb: Int = 1024,
    val termuxForceSourceBuild: Boolean = true,
    val termuxDisableDaemons: Boolean = true,
    val termuxFixShebangs: Boolean = true,
    // CI Configuration
    val ciLinuxDistro: String = "ubuntu-latest",
    val ciCacheEnabled: Boolean = true,
    val ciFailFast: Boolean = true,
    // Shell Abstraction Guardrails
    val strictModePipefail: Boolean = true,
    val unmaskRealErrors: Boolean = true,
    val recordExecutionReport: Boolean = true,
    val customBuildCommand: String = "",
    val customTestCommand: String = ""
)
