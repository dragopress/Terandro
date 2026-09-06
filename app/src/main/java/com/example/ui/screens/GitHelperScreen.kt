package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Commit
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.GitHelperEngine
import com.example.model.GitCommandResult
import com.example.model.GitCommitItem
import com.example.model.GitFileChange
import com.example.model.GitFileStatusType
import com.example.model.GitRepositoryState
import com.example.ui.components.PillBadge
import com.example.ui.theme.CleanStatusPass
import com.example.ui.theme.CodeAmber
import com.example.ui.theme.DarkCodeBackground
import com.example.ui.theme.ErrorRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GitHelperScreen(
    repoState: GitRepositoryState,
    consoleLogs: List<GitCommandResult>,
    commitMessage: String,
    isOperating: Boolean,
    onFetch: () -> Unit,
    onStatus: () -> Unit,
    onCommit: () -> Unit,
    onToggleStage: (String, Boolean) -> Unit,
    onStageAll: (Boolean) -> Unit,
    onDiscardChanges: (String) -> Unit,
    onAddSimulatedChange: (String, GitFileStatusType) -> Unit,
    onCommitMessageChanged: (String) -> Unit,
    onApplyPrefix: (String) -> Unit,
    onClearConsole: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDiffDialogForFile by remember { mutableStateOf<GitFileChange?>(null) }
    var showSimulatedMenu by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header & Repository Overview
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("git_repo_overview_card"),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Commit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Git CI/CD Helper",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Branch: ${repoState.branch} → ${repoState.upstreamBranch}",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (repoState.isClean) {
                            PillBadge(text = "Working Tree Clean", color = CleanStatusPass)
                        } else {
                            PillBadge(
                                text = "${repoState.totalModifiedCount} Changes",
                                color = CodeAmber
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Remote & Sync Statistics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Ahead of CI/CD
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = if (repoState.aheadCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "${repoState.aheadCount} Ahead",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Ready to push",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Behind remote CI
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = if (repoState.behindCount > 0) CodeAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "${repoState.behindCount} Behind",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Remote commits",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    repoState.lastFetchSummary?.let { summary ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = summary,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 2. Primary Action Bar (Fetch, Status, Simulate Changes)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Fetch Button
                Button(
                    onClick = onFetch,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("git_fetch_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(50),
                    enabled = !isOperating
                ) {
                    if (isOperating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Fetch", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Status Button
                Button(
                    onClick = onStatus,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("git_status_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Status", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                // Simulate Change Button (Dropdown)
                Box {
                    OutlinedButton(
                        onClick = { showSimulatedMenu = true },
                        modifier = Modifier
                            .height(46.dp)
                            .testTag("git_simulate_change_button"),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ File", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    DropdownMenu(
                        expanded = showSimulatedMenu,
                        onDismissRequest = { showSimulatedMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit 'scripts/build-bridge.sh'", fontSize = 12.sp) },
                            onClick = {
                                onAddSimulatedChange("scripts/build-bridge.sh", GitFileStatusType.MODIFIED)
                                showSimulatedMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit '.github/workflows/ci.yml'", fontSize = 12.sp) },
                            onClick = {
                                onAddSimulatedChange(".github/workflows/ci.yml", GitFileStatusType.MODIFIED)
                                showSimulatedMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Create 'tests/test_safeguards.py'", fontSize = 12.sp) },
                            onClick = {
                                onAddSimulatedChange("tests/test_safeguards.py", GitFileStatusType.UNTRACKED)
                                showSimulatedMenu = false
                            }
                        )
                    }
                }
            }
        }

        // 3. Staging Area (Staged Files & Working Tree)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("git_staging_area_card"),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Working Tree & Staging",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row {
                            if (repoState.unstagedFiles.isNotEmpty() || repoState.untrackedFiles.isNotEmpty()) {
                                Text(
                                    text = "Stage All",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clickable { onStageAll(true) }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                        .testTag("git_stage_all_btn")
                                )
                            }
                            if (repoState.stagedFiles.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Unstage All",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier
                                        .clickable { onStageAll(false) }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                        .testTag("git_unstage_all_btn")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (repoState.isClean) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = CleanStatusPass)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Working tree is clean. No unstaged or staged modifications.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        // Section: Staged for commit
                        if (repoState.stagedFiles.isNotEmpty()) {
                            Text(
                                text = "STAGED FOR COMMIT (${repoState.stagedFiles.size})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp,
                                color = CleanStatusPass
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            repoState.stagedFiles.forEach { file ->
                                GitFileItemRow(
                                    file = file,
                                    onToggleStage = { onToggleStage(file.path, false) },
                                    onViewDiff = { showDiffDialogForFile = file },
                                    onDiscard = null
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Section: Unstaged Modifications
                        if (repoState.unstagedFiles.isNotEmpty()) {
                            Text(
                                text = "CHANGES NOT STAGED (${repoState.unstagedFiles.size})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp,
                                color = CodeAmber
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            repoState.unstagedFiles.forEach { file ->
                                GitFileItemRow(
                                    file = file,
                                    onToggleStage = { onToggleStage(file.path, true) },
                                    onViewDiff = { showDiffDialogForFile = file },
                                    onDiscard = { onDiscardChanges(file.path) }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Section: Untracked Files
                        if (repoState.untrackedFiles.isNotEmpty()) {
                            Text(
                                text = "UNTRACKED FILES (${repoState.untrackedFiles.size})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            repoState.untrackedFiles.forEach { file ->
                                GitFileItemRow(
                                    file = file,
                                    onToggleStage = { onToggleStage(file.path, true) },
                                    onViewDiff = { showDiffDialogForFile = file },
                                    onDiscard = { onDiscardChanges(file.path) }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        }

        // 4. Commit Composer
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("git_commit_composer_card"),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Commit Changes",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Stage changes and create a local Git commit to trigger the next CI phase.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Conventional Commit Prefix Chips
                    Text(
                        text = "CONVENTIONAL COMMIT TYPES:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val prefixScroll = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(prefixScroll),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("ci:", "feat:", "fix:", "chore:", "test:", "refactor:").forEach { prefix ->
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(50))
                                    .clickable { onApplyPrefix(prefix) }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                    .testTag("commit_prefix_$prefix")
                            ) {
                                Text(
                                    text = prefix,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = commitMessage,
                        onValueChange = onCommitMessageChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("git_commit_message_input"),
                        placeholder = {
                            Text(
                                "e.g. ci: configure strict mode error trap in build-bridge.sh",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        ),
                        singleLine = false,
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Suggested Messages
                    Text(
                        text = "Suggested CI/CD Messages:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    GitHelperEngine.getSuggestedCommitMessages().take(2).forEach { suggestion ->
                        Text(
                            text = "• $suggestion",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { onCommitMessageChanged(suggestion) }
                                .padding(vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onCommit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("git_commit_button"),
                        enabled = repoState.hasStagedChanges && commitMessage.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(50)
                    ) {
                        Icon(imageVector = Icons.Default.Done, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (repoState.hasStagedChanges) "Commit (${repoState.stagedFiles.size} staged)" else "Stage files to commit",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // 5. Monospace Terminal Console Output
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("git_terminal_console_card"),
                shape = RoundedCornerShape(24.dp),
                color = DarkCodeBackground,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(CleanStatusPass, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Git CLI Terminal Output",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Row {
                            IconButton(
                                onClick = {
                                    val script = GitHelperEngine.generateCliScript(repoState, commitMessage)
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Git CI Script", script))
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy script",
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = onClearConsole,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear logs",
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (consoleLogs.isEmpty()) {
                        Text(
                            text = "# No Git operations logged yet.\n# Run 'Fetch' or 'Status' to inspect the repository.",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.Gray
                        )
                    } else {
                        consoleLogs.take(5).forEach { cmd ->
                            Text(
                                text = "$ ${cmd.command}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF80D8FF)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = cmd.output,
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (cmd.exitCode == 0) Color(0xFFE0E0E0) else Color(0xFFFF8A80)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        // 6. Recent Commits History
        item {
            Text(
                text = "RECENT COMMITS (${repoState.commitHistory.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(repoState.commitHistory) { commit ->
            CommitHistoryItemCard(commit = commit)
        }
    }

    // Diff Preview Dialog
    showDiffDialogForFile?.let { file ->
        androidx.compose.ui.window.Dialog(onDismissRequest = { showDiffDialogForFile = null }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = file.path,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { showDiffDialogForFile = null }) {
                            Icon(Icons.Default.Clear, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = DarkCodeBackground
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            file.diffPreview.lines().forEach { line ->
                                val color = when {
                                    line.startsWith("+") -> Color(0xFF81C784)
                                    line.startsWith("-") -> Color(0xFFE57373)
                                    line.startsWith("@") -> Color(0xFF64B5F6)
                                    else -> Color.White
                                }
                                Text(
                                    text = line,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = color
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { showDiffDialogForFile = null },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("Close Diff")
                    }
                }
            }
        }
    }
}

@Composable
fun GitFileItemRow(
    file: GitFileChange,
    onToggleStage: () -> Unit,
    onViewDiff: () -> Unit,
    onDiscard: (() -> Unit)?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Code Badge
                val badgeColor = when (file.status) {
                    GitFileStatusType.MODIFIED -> CodeAmber
                    GitFileStatusType.ADDED -> CleanStatusPass
                    GitFileStatusType.DELETED -> ErrorRed
                    GitFileStatusType.UNTRACKED -> MaterialTheme.colorScheme.primary
                }
                Box(
                    modifier = Modifier
                        .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = file.status.code,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = badgeColor
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = file.path,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (file.linesAdded > 0 || file.linesDeleted > 0) {
                        Text(
                            text = "+${file.linesAdded} -${file.linesDeleted}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (file.diffPreview.isNotBlank()) {
                    Text(
                        text = "Diff",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { onViewDiff() }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }

                if (onDiscard != null) {
                    IconButton(
                        onClick = onDiscard,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Discard changes",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Button(
                    onClick = onToggleStage,
                    modifier = Modifier.height(30.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (file.isStaged) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (file.isStaged) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                ) {
                    Text(
                        text = if (file.isStaged) "Unstage" else "Stage",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun CommitHistoryItemCard(commit: GitCommitItem) {
    val dateStr = SimpleDateFormat("MMM dd, HH:mm", Locale.US).format(Date(commit.timestamp))

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = commit.sha,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = commit.message,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${commit.author} • $dateStr",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
