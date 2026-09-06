package com.example.engine

import com.example.model.GitCommandResult
import com.example.model.GitCommitItem
import com.example.model.GitFileChange
import com.example.model.GitFileStatusType
import com.example.model.GitRepositoryState
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GitHelperEngine {

    fun getInitialState(): GitRepositoryState {
        val initialHistory = listOf(
            GitCommitItem(
                sha = "4b2e8a1",
                message = "ci: integrate Termux bionic libc flags and CPU limits",
                author = "Termux Dev <dev@termux.mobile>",
                timestamp = System.currentTimeMillis() - 3600000 * 2,
                branch = "main",
                filesCount = 3
            ),
            GitCommitItem(
                sha = "19c72d4",
                message = "feat: add build-bridge.sh unified execution layer",
                author = "CI Bot <bot@github.actions>",
                timestamp = System.currentTimeMillis() - 3600000 * 18,
                branch = "main",
                filesCount = 4
            ),
            GitCommitItem(
                sha = "9a8f301",
                message = "chore: initialize cross-platform CI repository",
                author = "Lead Architect <dev@unified.build>",
                timestamp = System.currentTimeMillis() - 3600000 * 48,
                branch = "main",
                filesCount = 2
            )
        )

        val staged = listOf(
            GitFileChange(
                path = "scripts/build-bridge.sh",
                status = GitFileStatusType.MODIFIED,
                isStaged = true,
                diffPreview = """
                    @@ -12,4 +12,6 @@
                     set -Eeuo pipefail
                    +trap 'report_error ${'$'}? ${'$'}LINENO' ERR
                    +export CFLAGS="-D__ANDROID_API__=24"
                """.trimIndent(),
                linesAdded = 2,
                linesDeleted = 0
            )
        )

        val unstaged = listOf(
            GitFileChange(
                path = ".github/workflows/ci.yml",
                status = GitFileStatusType.MODIFIED,
                isStaged = false,
                diffPreview = """
                    @@ -28,3 +28,4 @@
                     - name: Run Build-Bridge Abstraction
                    -  run: bash scripts/build-bridge.sh
                    +  run: bash scripts/build-bridge.sh --ci-strict
                """.trimIndent(),
                linesAdded = 1,
                linesDeleted = 1
            )
        )

        val untracked = listOf(
            GitFileChange(
                path = "tests/test_exit_codes.py",
                status = GitFileStatusType.UNTRACKED,
                isStaged = false,
                diffPreview = """
                    +def test_pipeline_preserves_pytest_failure():
                    +    assert verify_exit_code(1) == 1
                """.trimIndent(),
                linesAdded = 3,
                linesDeleted = 0
            )
        )

        return GitRepositoryState(
            branch = "main",
            remoteName = "origin",
            remoteUrl = "https://github.com/developer/unified-ci-pipeline.git",
            upstreamBranch = "origin/main",
            aheadCount = 1,
            behindCount = 0,
            isClean = false,
            stagedFiles = staged,
            unstagedFiles = unstaged,
            untrackedFiles = untracked,
            lastFetchedAt = System.currentTimeMillis() - 1800000,
            lastFetchSummary = "Already up to date with origin/main",
            commitHistory = initialHistory
        )
    }

    /**
     * Executes `git status` logic and formats realistic terminal output.
     */
    fun executeStatus(state: GitRepositoryState): Pair<GitRepositoryState, GitCommandResult> {
        val totalChanges = state.stagedFiles.size + state.unstagedFiles.size + state.untrackedFiles.size
        val isClean = totalChanges == 0

        val outputBuilder = StringBuilder()
        outputBuilder.appendLine("On branch ${state.branch}")

        if (state.behindCount > 0 && state.aheadCount > 0) {
            outputBuilder.appendLine("Your branch and '${state.upstreamBranch}' have diverged,")
            outputBuilder.appendLine("and have ${state.aheadCount} and ${state.behindCount} different commits each, respectively.")
        } else if (state.aheadCount > 0) {
            outputBuilder.appendLine("Your branch is ahead of '${state.upstreamBranch}' by ${state.aheadCount} commit${if (state.aheadCount > 1) "s" else ""}.")
            outputBuilder.appendLine("  (use \"git push\" to publish your local commits)")
        } else if (state.behindCount > 0) {
            outputBuilder.appendLine("Your branch is behind '${state.upstreamBranch}' by ${state.behindCount} commit${if (state.behindCount > 1) "s" else ""}, and can be fast-forwarded.")
            outputBuilder.appendLine("  (use \"git pull\" to update your local branch)")
        } else {
            outputBuilder.appendLine("Your branch is up to date with '${state.upstreamBranch}'.")
        }
        outputBuilder.appendLine()

        if (state.stagedFiles.isNotEmpty()) {
            outputBuilder.appendLine("Changes to be committed:")
            outputBuilder.appendLine("  (use \"git restore --staged <file>...\" to unstage)")
            state.stagedFiles.forEach { file ->
                val prefix = when (file.status) {
                    GitFileStatusType.MODIFIED -> "modified:  "
                    GitFileStatusType.ADDED -> "new file:  "
                    GitFileStatusType.DELETED -> "deleted:   "
                    GitFileStatusType.UNTRACKED -> "new file:  "
                }
                outputBuilder.appendLine("\t$prefix${file.path}")
            }
            outputBuilder.appendLine()
        }

        if (state.unstagedFiles.isNotEmpty()) {
            outputBuilder.appendLine("Changes not staged for commit:")
            outputBuilder.appendLine("  (use \"git add <file>...\" to update what will be committed)")
            outputBuilder.appendLine("  (use \"git restore <file>...\" to discard changes in working directory)")
            state.unstagedFiles.forEach { file ->
                val prefix = when (file.status) {
                    GitFileStatusType.MODIFIED -> "modified:  "
                    GitFileStatusType.DELETED -> "deleted:   "
                    else -> "modified:  "
                }
                outputBuilder.appendLine("\t$prefix${file.path}")
            }
            outputBuilder.appendLine()
        }

        if (state.untrackedFiles.isNotEmpty()) {
            outputBuilder.appendLine("Untracked files:")
            outputBuilder.appendLine("  (use \"git add <file>...\" to include in what will be committed)")
            state.untrackedFiles.forEach { file ->
                outputBuilder.appendLine("\t${file.path}")
            }
            outputBuilder.appendLine()
        }

        if (isClean) {
            outputBuilder.appendLine("nothing to commit, working tree clean")
        }

        val updatedState = state.copy(isClean = isClean)
        val result = GitCommandResult(
            command = "git status",
            exitCode = 0,
            output = outputBuilder.toString().trimEnd(),
            success = true
        )

        return Pair(updatedState, result)
    }

    /**
     * Executes `git fetch` against the configured remote origin.
     */
    fun executeFetch(state: GitRepositoryState): Pair<GitRepositoryState, GitCommandResult> {
        val now = System.currentTimeMillis()
        val timeFormatted = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(now))

        val outputBuilder = StringBuilder()
        outputBuilder.appendLine("Fetching ${state.remoteName} (${state.remoteUrl})...")

        val newBehindCount = if (state.behindCount == 0 && (now % 2L == 0L)) 1 else state.behindCount
        val summaryText = if (newBehindCount > state.behindCount) {
            outputBuilder.appendLine("remote: Enumerating objects: 5, done.")
            outputBuilder.appendLine("remote: Counting objects: 100% (5/5), done.")
            outputBuilder.appendLine("remote: Compressing objects: 100% (3/3), done.")
            outputBuilder.appendLine("remote: Total 5 (delta 2), reused 0 (delta 0)")
            outputBuilder.appendLine("From ${state.remoteUrl}")
            outputBuilder.appendLine(" * [new commit]       main       -> ${state.remoteName}/main")
            "Fetched updates from ${state.remoteName}/main at $timeFormatted ($newBehindCount commit pending)"
        } else {
            outputBuilder.appendLine("From ${state.remoteUrl}")
            outputBuilder.appendLine(" * branch            ${state.branch}     -> FETCH_HEAD")
            outputBuilder.appendLine("Already up to date with ${state.upstreamBranch}.")
            "Already up to date with ${state.upstreamBranch} (checked at $timeFormatted)"
        }

        val updatedState = state.copy(
            lastFetchedAt = now,
            lastFetchSummary = summaryText,
            behindCount = newBehindCount
        )

        val result = GitCommandResult(
            command = "git fetch ${state.remoteName}",
            exitCode = 0,
            output = outputBuilder.toString().trimEnd(),
            success = true,
            timestamp = now
        )

        return Pair(updatedState, result)
    }

    /**
     * Executes `git commit -m "<message>"`.
     */
    fun executeCommit(
        state: GitRepositoryState,
        message: String,
        author: String = "Termux Dev <dev@termux.mobile>"
    ): Pair<GitRepositoryState, GitCommandResult> {
        val trimmedMsg = message.trim()

        if (trimmedMsg.isEmpty()) {
            return Pair(
                state,
                GitCommandResult(
                    command = "git commit -m \"\"",
                    exitCode = 1,
                    output = "error: empty commit message specified. Aborting commit.\n(use conventional commit e.g. ci: update pipeline)",
                    success = false
                )
            )
        }

        if (state.stagedFiles.isEmpty()) {
            return Pair(
                state,
                GitCommandResult(
                    command = "git commit -m \"$trimmedMsg\"",
                    exitCode = 1,
                    output = """
                        On branch ${state.branch}
                        nothing added to commit but untracked files present or unstaged changes exist.
                        (use "git add" to stage changes before committing)
                    """.trimIndent(),
                    success = false
                )
            )
        }

        // Generate a 7-character sha
        val sha = generateSha7(trimmedMsg + System.currentTimeMillis())
        val committedFiles = state.stagedFiles
        val totalLinesAdded = committedFiles.sumOf { it.linesAdded }
        val totalLinesDeleted = committedFiles.sumOf { it.linesDeleted }

        val newCommit = GitCommitItem(
            sha = sha,
            message = trimmedMsg,
            author = author,
            timestamp = System.currentTimeMillis(),
            branch = state.branch,
            filesCount = committedFiles.size
        )

        val outputBuilder = StringBuilder()
        outputBuilder.appendLine("[${state.branch} $sha] $trimmedMsg")
        outputBuilder.appendLine(" ${committedFiles.size} file${if (committedFiles.size > 1) "s" else ""} changed, $totalLinesAdded insertions(+), $totalLinesDeleted deletions(-)")
        committedFiles.forEach { file ->
            outputBuilder.appendLine(" create mode 100644 ${file.path}")
        }

        val remainingUnstaged = state.unstagedFiles
        val remainingUntracked = state.untrackedFiles
        val isClean = remainingUnstaged.isEmpty() && remainingUntracked.isEmpty()

        val updatedState = state.copy(
            aheadCount = state.aheadCount + 1,
            isClean = isClean,
            stagedFiles = emptyList(),
            commitHistory = listOf(newCommit) + state.commitHistory
        )

        val result = GitCommandResult(
            command = "git commit -m \"$trimmedMsg\"",
            exitCode = 0,
            output = outputBuilder.toString().trimEnd(),
            success = true
        )

        return Pair(updatedState, result)
    }

    /**
     * Toggles staging for a specific file (`git add <file>` or `git restore --staged <file>`).
     */
    fun toggleStageFile(state: GitRepositoryState, path: String, stage: Boolean): Pair<GitRepositoryState, GitCommandResult> {
        if (stage) {
            // Find in unstaged or untracked
            val unstagedMatch = state.unstagedFiles.find { it.path == path }
            val untrackedMatch = state.untrackedFiles.find { it.path == path }
            val target = unstagedMatch ?: untrackedMatch

            if (target != null) {
                val stagedFile = target.copy(isStaged = true)
                val newStaged = state.stagedFiles + stagedFile
                val newUnstaged = state.unstagedFiles.filterNot { it.path == path }
                val newUntracked = state.untrackedFiles.filterNot { it.path == path }

                val updatedState = state.copy(
                    stagedFiles = newStaged,
                    unstagedFiles = newUnstaged,
                    untrackedFiles = newUntracked,
                    isClean = false
                )
                val cmd = GitCommandResult(
                    command = "git add $path",
                    exitCode = 0,
                    output = "Staged '$path' for commit",
                    success = true
                )
                return Pair(updatedState, cmd)
            }
        } else {
            // Unstage file
            val stagedMatch = state.stagedFiles.find { it.path == path }
            if (stagedMatch != null) {
                val unStagedFile = stagedMatch.copy(isStaged = false)
                val newStaged = state.stagedFiles.filterNot { it.path == path }
                val newUnstaged = if (stagedMatch.status == GitFileStatusType.UNTRACKED) {
                    state.unstagedFiles
                } else {
                    state.unstagedFiles + unStagedFile
                }
                val newUntracked = if (stagedMatch.status == GitFileStatusType.UNTRACKED) {
                    state.untrackedFiles + unStagedFile
                } else {
                    state.untrackedFiles
                }

                val updatedState = state.copy(
                    stagedFiles = newStaged,
                    unstagedFiles = newUnstaged,
                    untrackedFiles = newUntracked,
                    isClean = newStaged.isEmpty() && newUnstaged.isEmpty() && newUntracked.isEmpty()
                )
                val cmd = GitCommandResult(
                    command = "git restore --staged $path",
                    exitCode = 0,
                    output = "Unstaged '$path'",
                    success = true
                )
                return Pair(updatedState, cmd)
            }
        }
        return Pair(state, GitCommandResult("git", 0, "No changes to file", true))
    }

    /**
     * Stages or unstages all modified and untracked files (`git add -A`).
     */
    fun stageAll(state: GitRepositoryState, stage: Boolean): Pair<GitRepositoryState, GitCommandResult> {
        if (stage) {
            val allToStage = (state.unstagedFiles + state.untrackedFiles).map { it.copy(isStaged = true) }
            val updatedState = state.copy(
                stagedFiles = state.stagedFiles + allToStage,
                unstagedFiles = emptyList(),
                untrackedFiles = emptyList(),
                isClean = false
            )
            return Pair(
                updatedState,
                GitCommandResult(
                    command = "git add -A",
                    exitCode = 0,
                    output = "Staged ${allToStage.size} file(s) for commit",
                    success = true
                )
            )
        } else {
            val allStaged = state.stagedFiles
            val newUnstaged = allStaged.filter { it.status != GitFileStatusType.UNTRACKED }.map { it.copy(isStaged = false) }
            val newUntracked = allStaged.filter { it.status == GitFileStatusType.UNTRACKED }.map { it.copy(isStaged = false) }

            val updatedState = state.copy(
                stagedFiles = emptyList(),
                unstagedFiles = state.unstagedFiles + newUnstaged,
                untrackedFiles = state.untrackedFiles + newUntracked,
                isClean = (state.unstagedFiles + newUnstaged).isEmpty() && (state.untrackedFiles + newUntracked).isEmpty()
            )
            return Pair(
                updatedState,
                GitCommandResult(
                    command = "git reset HEAD",
                    exitCode = 0,
                    output = "Unstaged all changes",
                    success = true
                )
            )
        }
    }

    /**
     * Discards unstaged modifications (`git checkout -- <file>`).
     */
    fun discardChanges(state: GitRepositoryState, path: String): Pair<GitRepositoryState, GitCommandResult> {
        val newUnstaged = state.unstagedFiles.filterNot { it.path == path }
        val newUntracked = state.untrackedFiles.filterNot { it.path == path }
        val isClean = state.stagedFiles.isEmpty() && newUnstaged.isEmpty() && newUntracked.isEmpty()

        val updatedState = state.copy(
            unstagedFiles = newUnstaged,
            untrackedFiles = newUntracked,
            isClean = isClean
        )
        return Pair(
            updatedState,
            GitCommandResult(
                command = "git checkout -- $path",
                exitCode = 0,
                output = "Reverted modifications to '$path'",
                success = true
            )
        )
    }

    /**
     * Injects a mock change to simulate code edits in the CI/CD development loop.
     */
    fun addSimulatedChange(
        state: GitRepositoryState,
        path: String,
        type: GitFileStatusType
    ): Pair<GitRepositoryState, GitCommandResult> {
        val newChange = GitFileChange(
            path = path,
            status = type,
            isStaged = false,
            diffPreview = """
                + # CI/CD Update applied via Termux Dev Loop
                + export CI_PIPELINE_GUARD=true
            """.trimIndent(),
            linesAdded = 2,
            linesDeleted = 0
        )

        val newUnstaged = if (type == GitFileStatusType.UNTRACKED) state.unstagedFiles else state.unstagedFiles + newChange
        val newUntracked = if (type == GitFileStatusType.UNTRACKED) state.untrackedFiles + newChange else state.untrackedFiles

        val updatedState = state.copy(
            unstagedFiles = newUnstaged,
            untrackedFiles = newUntracked,
            isClean = false
        )
        return Pair(
            updatedState,
            GitCommandResult(
                command = "# edit $path",
                exitCode = 0,
                output = "Created changes in '$path'",
                success = true
            )
        )
    }

    /**
     * Generates a ready-to-run shell script for Termux / local bash.
     */
    fun generateCliScript(state: GitRepositoryState, commitMessage: String): String {
        val msg = if (commitMessage.isBlank()) "ci: update pipeline build configuration" else commitMessage.trim()
        val stageCommands = if (state.stagedFiles.isNotEmpty()) {
            state.stagedFiles.joinToString(" ") { it.path }
        } else {
            "."
        }

        return """
            #!/usr/bin/env bash
            # UnifiedBuild CI/CD Loop Script
            set -Eeuo pipefail

            echo "[1/4] Fetching remote origin..."
            git fetch ${state.remoteName} ${state.branch}

            echo "[2/4] Checking repository status..."
            git status --short

            echo "[3/4] Staging changes..."
            git add $stageCommands

            echo "[4/4] Creating commit..."
            git commit -m "$msg"

            echo "Repository updated! Run 'git push ${state.remoteName} ${state.branch}' to trigger CI."
        """.trimIndent()
    }

    fun getSuggestedCommitMessages(): List<String> {
        return listOf(
            "ci: configure strict error traps in build-bridge.sh",
            "fix: prevent exit code masking on test failures",
            "feat: add bionic libc native build flags for Termux",
            "test: verify non-zero exit propagation in GitHub Actions",
            "chore: optimize CI cache keys and memory limits"
        )
    }

    private fun generateSha7(seed: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-1")
            val bytes = md.digest(seed.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }.take(7)
        } catch (_: Exception) {
            "a" + (System.currentTimeMillis() % 1000000L).toString()
        }
    }
}
