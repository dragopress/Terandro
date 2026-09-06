package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.ProjectConfig
import com.example.model.StackType

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val primaryStackId: String,
    val secondaryStackId: String?,
    val packageManager: String,
    val enableInstall: Boolean,
    val enableLint: Boolean,
    val enableBuild: Boolean,
    val enableTest: Boolean,
    val enablePackage: Boolean,
    val termuxCpuLimit: Int,
    val termuxMaxMemoryMb: Int,
    val termuxForceSourceBuild: Boolean,
    val termuxDisableDaemons: Boolean,
    val termuxFixShebangs: Boolean,
    val ciLinuxDistro: String,
    val ciCacheEnabled: Boolean,
    val ciFailFast: Boolean,
    val strictModePipefail: Boolean,
    val unmaskRealErrors: Boolean,
    val recordExecutionReport: Boolean,
    val customBuildCommand: String,
    val customTestCommand: String,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toProjectConfig(): ProjectConfig {
        return ProjectConfig(
            id = id,
            name = name,
            primaryStack = StackType.fromId(primaryStackId),
            secondaryStack = secondaryStackId?.let { StackType.fromId(it) },
            packageManager = packageManager,
            enableInstall = enableInstall,
            enableLint = enableLint,
            enableBuild = enableBuild,
            enableTest = enableTest,
            enablePackage = enablePackage,
            termuxCpuLimit = termuxCpuLimit,
            termuxMaxMemoryMb = termuxMaxMemoryMb,
            termuxForceSourceBuild = termuxForceSourceBuild,
            termuxDisableDaemons = termuxDisableDaemons,
            termuxFixShebangs = termuxFixShebangs,
            ciLinuxDistro = ciLinuxDistro,
            ciCacheEnabled = ciCacheEnabled,
            ciFailFast = ciFailFast,
            strictModePipefail = strictModePipefail,
            unmaskRealErrors = unmaskRealErrors,
            recordExecutionReport = recordExecutionReport,
            customBuildCommand = customBuildCommand,
            customTestCommand = customTestCommand
        )
    }

    companion object {
        fun fromProjectConfig(config: ProjectConfig): ProjectEntity {
            return ProjectEntity(
                id = config.id,
                name = config.name,
                primaryStackId = config.primaryStack.id,
                secondaryStackId = config.secondaryStack?.id,
                packageManager = config.packageManager,
                enableInstall = config.enableInstall,
                enableLint = config.enableLint,
                enableBuild = config.enableBuild,
                enableTest = config.enableTest,
                enablePackage = config.enablePackage,
                termuxCpuLimit = config.termuxCpuLimit,
                termuxMaxMemoryMb = config.termuxMaxMemoryMb,
                termuxForceSourceBuild = config.termuxForceSourceBuild,
                termuxDisableDaemons = config.termuxDisableDaemons,
                termuxFixShebangs = config.termuxFixShebangs,
                ciLinuxDistro = config.ciLinuxDistro,
                ciCacheEnabled = config.ciCacheEnabled,
                ciFailFast = config.ciFailFast,
                strictModePipefail = config.strictModePipefail,
                unmaskRealErrors = config.unmaskRealErrors,
                recordExecutionReport = config.recordExecutionReport,
                customBuildCommand = config.customBuildCommand,
                customTestCommand = config.customTestCommand
            )
        }
    }
}

@Entity(tableName = "build_logs")
data class BuildLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long = 0,
    val title: String,
    val rawLog: String,
    val detectedCategory: String,
    val exitCode: Int,
    val timestamp: Long = System.currentTimeMillis()
)
