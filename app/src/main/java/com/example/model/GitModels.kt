package com.example.model

enum class GitFileStatusType(val label: String, val code: String) {
    MODIFIED("Modified", "M"),
    ADDED("Added", "A"),
    DELETED("Deleted", "D"),
    UNTRACKED("Untracked", "??")
}

data class GitFileChange(
    val path: String,
    val status: GitFileStatusType,
    val isStaged: Boolean = false,
    val diffPreview: String = "",
    val linesAdded: Int = 0,
    val linesDeleted: Int = 0
)

data class GitCommitItem(
    val sha: String,
    val message: String,
    val author: String = "Termux Dev <dev@termux.mobile>",
    val timestamp: Long = System.currentTimeMillis(),
    val branch: String = "main",
    val filesCount: Int = 1
)

data class GitCommandResult(
    val command: String,
    val exitCode: Int = 0,
    val output: String,
    val success: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

data class GitRepositoryState(
    val branch: String = "main",
    val remoteName: String = "origin",
    val remoteUrl: String = "https://github.com/developer/unified-ci-pipeline.git",
    val upstreamBranch: String = "origin/main",
    val aheadCount: Int = 0,
    val behindCount: Int = 0,
    val isClean: Boolean = false,
    val stagedFiles: List<GitFileChange> = emptyList(),
    val unstagedFiles: List<GitFileChange> = emptyList(),
    val untrackedFiles: List<GitFileChange> = emptyList(),
    val lastFetchedAt: Long? = null,
    val lastFetchSummary: String? = null,
    val commitHistory: List<GitCommitItem> = emptyList()
) {
    val totalModifiedCount: Int
        get() = stagedFiles.size + unstagedFiles.size + untrackedFiles.size

    val hasStagedChanges: Boolean
        get() = stagedFiles.isNotEmpty()
}
