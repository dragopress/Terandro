package com.example.data

import com.example.model.ProjectConfig
import com.example.model.StackType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProjectRepository(private val dao: ProjectDao) {

    val allProjects: Flow<List<ProjectConfig>> = dao.getAllProjects().map { list ->
        list.map { it.toProjectConfig() }
    }

    val allLogs: Flow<List<BuildLogEntity>> = dao.getAllLogs()

    val allGitCommits: Flow<List<com.example.model.GitCommitItem>> = dao.getAllGitCommits().map { list ->
        list.map { it.toCommitItem() }
    }

    suspend fun saveGitCommit(commit: com.example.model.GitCommitItem): Long {
        return dao.insertGitCommit(GitCommitEntity.fromCommitItem(commit))
    }

    suspend fun clearGitCommits() {
        dao.clearGitCommits()
    }

    suspend fun saveProject(config: ProjectConfig): Long {
        val entity = ProjectEntity.fromProjectConfig(config)
        return if (config.id == 0L) {
            dao.insertProject(entity)
        } else {
            dao.updateProject(entity)
            config.id
        }
    }

    suspend fun getProjectById(id: Long): ProjectConfig? {
        return dao.getProjectById(id)?.toProjectConfig()
    }

    suspend fun deleteProject(id: Long) {
        dao.deleteProjectById(id)
    }

    suspend fun saveBuildLog(log: BuildLogEntity): Long {
        return dao.insertBuildLog(log)
    }

    suspend fun deleteBuildLog(id: Long) {
        dao.deleteLogById(id)
    }

    companion object {
        fun getDefaultProjectPresets(): List<ProjectConfig> {
            return listOf(
                ProjectConfig(
                    id = 1,
                    name = "Python FastAPI Service",
                    primaryStack = StackType.PYTHON,
                    packageManager = "pip",
                    enableInstall = true,
                    enableLint = true,
                    enableBuild = true,
                    enableTest = true,
                    termuxCpuLimit = 2,
                    termuxForceSourceBuild = true
                ),
                ProjectConfig(
                    id = 2,
                    name = "Node.js Fullstack & Addon",
                    primaryStack = StackType.NODEJS,
                    packageManager = "npm",
                    enableInstall = true,
                    enableLint = true,
                    enableBuild = true,
                    enableTest = true,
                    termuxCpuLimit = 2,
                    termuxForceSourceBuild = true
                ),
                ProjectConfig(
                    id = 3,
                    name = "Rust Core Engine",
                    primaryStack = StackType.RUST,
                    packageManager = "cargo",
                    enableInstall = true,
                    enableLint = true,
                    enableBuild = true,
                    enableTest = true,
                    termuxCpuLimit = 2,
                    termuxForceSourceBuild = true
                ),
                ProjectConfig(
                    id = 4,
                    name = "Polyglot: Node + Rust Native",
                    primaryStack = StackType.MIXED,
                    secondaryStack = StackType.RUST,
                    packageManager = "npm",
                    enableInstall = true,
                    enableLint = true,
                    enableBuild = true,
                    enableTest = true,
                    termuxCpuLimit = 2,
                    termuxForceSourceBuild = true
                )
            )
        }
    }
}
