package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.EnvironmentDiagnosticEngine
import com.example.engine.ErrorAnalyzerEngine
import com.example.engine.UnifiedScriptGenerator
import com.example.model.ErrorCategory
import com.example.model.ProjectConfig
import com.example.model.StackType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EngineRobolectricTest {

    @Test
    fun `diagnostic engine produces valid report`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val report = EnvironmentDiagnosticEngine.runDiagnostics(context)
        assertNotNull(report)
        assertTrue(report.overallScore in 0..100)
        assertTrue(report.items.isNotEmpty())
        assertNotNull(report.architecture)
    }

    @Test
    fun `error analyzer unmasks real pytest test failure`() {
        val log = """
            ============================= test session starts ==============================
            collected 12 items
            tests/test_auth.py .F...
            FAILED tests/test_auth.py::test_jwt_signature - AssertionError: assert False
            1 failed, 11 passed in 0.42s
        """.trimIndent()

        val diagnosis = ErrorAnalyzerEngine.analyzeLog(log)
        assertEquals(ErrorCategory.REAL_APPLICATION_TEST_FAILURE, diagnosis.category)
        assertTrue("Real app error must never be masked", diagnosis.isRealAppError)
        assertEquals(1, diagnosis.exitCode)
    }

    @Test
    fun `error analyzer unmasks real rust syntax compiler error`() {
        val log = """
            error[E0382]: borrow of moved value: `user_session`
              --> src/main.rs:42:15
               |
            40 |     let token = user_session.into_token();
               |                 ------------ `user_session` moved due to this call
            42 |     println!("{:?}", user_session.id);
               |                      ^^^^^^^^^^^^^^ value borrowed here after move
            error: could not compile `bridge_core` (bin "bridge_core") due to 1 previous error
        """.trimIndent()

        val diagnosis = ErrorAnalyzerEngine.analyzeLog(log)
        assertEquals(ErrorCategory.REAL_APPLICATION_SYNTAX_ERROR, diagnosis.category)
        assertTrue(diagnosis.isRealAppError)
        assertEquals(2, diagnosis.exitCode)
    }

    @Test
    fun `error analyzer detects bionic glibc mismatch`() {
        val log = """
            Traceback (most recent call last):
              File "main.py", line 1, in <module>
                import cryptography
            ImportError: dlopen failed: library "libc.so.6" not found: needed by /data/data/com.termux/files/usr/lib/python3.11/site-packages/cryptography/hazmat/bindings/_rust.abi3.so
        """.trimIndent()

        val diagnosis = ErrorAnalyzerEngine.analyzeLog(log)
        assertEquals(ErrorCategory.TERMUX_BIONIC_GLIBC_MISMATCH, diagnosis.category)
        assertFalse("Environment issue, not an application code bug", diagnosis.isRealAppError)
        assertTrue(diagnosis.fixScript.contains("--no-binary :all:"))
    }

    @Test
    fun `error analyzer detects android lmk sigkill 137`() {
        val log = """
            Compiling heavy_crate v0.1.0 (/home/project)
            Killed
            error: could not compile `heavy_crate` (lib)
        """.trimIndent()

        val diagnosis = ErrorAnalyzerEngine.analyzeLog(log)
        assertEquals(ErrorCategory.MEMORY_EXHAUSTION_LMK, diagnosis.category)
        assertEquals(137, diagnosis.exitCode)
        assertFalse(diagnosis.isRealAppError)
        assertTrue(diagnosis.fixScript.contains("CARGO_BUILD_JOBS=1"))
    }

    @Test
    fun `unified script generator generates strict unmasked shell bridge`() {
        val config = ProjectConfig(
            id = 1,
            name = "Test Rust App",
            primaryStack = StackType.RUST,
            packageManager = "cargo",
            strictModePipefail = true,
            unmaskRealErrors = true,
            termuxCpuLimit = 2
        )

        val script = UnifiedScriptGenerator.generateBuildBridgeScript(config)
        assertTrue("Must enforce strict error behavior", script.contains("set -Eeuo pipefail"))
        assertTrue("Must trap errors", script.contains("trap 'bb_trap_error"))
        assertTrue("Must contain cargo command", script.contains("cargo test"))
        assertTrue("Must configure Termux CPU limits", script.contains("CARGO_BUILD_JOBS=2"))
        assertFalse("Must never blindly swallow errors with || true", script.contains("|| true"))
    }

    @Test
    fun `github actions workflow includes linux runner and caching`() {
        val config = ProjectConfig(
            id = 2,
            name = "Test Python Service",
            primaryStack = StackType.PYTHON,
            packageManager = "pip"
        )

        val workflow = UnifiedScriptGenerator.generateGitHubActionsWorkflow(config)
        assertTrue(workflow.contains("runs-on: ubuntu-latest"))
        assertTrue(workflow.contains("./build-bridge.sh test"))
        assertTrue(workflow.contains("cache: 'pip'"))
    }

    @Test
    fun `git helper engine status and fetch operations`() {
        val initial = com.example.engine.GitHelperEngine.getInitialState()
        assertNotNull(initial)
        assertEquals("main", initial.branch)
        assertTrue(initial.stagedFiles.isNotEmpty())
        assertTrue(initial.unstagedFiles.isNotEmpty())

        val (fetchedState, fetchResult) = com.example.engine.GitHelperEngine.executeFetch(initial)
        assertTrue(fetchResult.success)
        assertTrue(fetchResult.command.contains("git fetch"))
        assertNotNull(fetchedState.lastFetchSummary)

        val (statusState, statusResult) = com.example.engine.GitHelperEngine.executeStatus(fetchedState)
        assertTrue(statusResult.success)
        assertTrue(statusResult.output.contains("On branch main"))
    }

    @Test
    fun `git helper engine staging and commit workflow`() {
        var state = com.example.engine.GitHelperEngine.getInitialState()

        // Stage all
        val (allStagedState, _) = com.example.engine.GitHelperEngine.stageAll(state, true)
        assertTrue(allStagedState.hasStagedChanges)
        assertEquals(0, allStagedState.unstagedFiles.size)

        // Commit
        val commitMsg = "ci: add unmasked error verification script"
        val (committedState, commitResult) = com.example.engine.GitHelperEngine.executeCommit(allStagedState, commitMsg)
        assertTrue(commitResult.success)
        assertTrue(commitResult.output.contains("1 file changed") || commitResult.output.contains("files changed"))
        assertEquals(commitMsg, committedState.commitHistory.first().message)
        assertEquals(allStagedState.aheadCount + 1, committedState.aheadCount)

        // CLI script generator
        val cliScript = com.example.engine.GitHelperEngine.generateCliScript(committedState, "ci: test push")
        assertTrue(cliScript.contains("git fetch origin main"))
        assertTrue(cliScript.contains("git status"))
        assertTrue(cliScript.contains("git push origin main"))
    }
}
