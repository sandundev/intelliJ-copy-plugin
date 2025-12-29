// === File: /Users/sandun/Documents/PersonalWorkSpace/intelliJ-copy-plugin/src/main/kotlin/com/example/plugin/actions/OpenAllChangedFilesAction.kt ===
package com.example.plugin.actions

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File
import java.util.regex.Pattern
import javax.swing.JComponent
import javax.swing.JTable
import javax.swing.SwingUtilities

class OpenAllChangedFilesAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        // Keep enabled; we will give a notification if we can't resolve commit/root.
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        try {
            val gitRoots = getGitRoots(project)
            if (gitRoots.isEmpty()) {
                notify(project, NotificationType.ERROR, "No Git root detected",
                    "I couldn't find any Git roots for this project.")
                return
            }

            val candidates = collectCandidateCommitHashes(e)
            if (candidates.isEmpty()) {
                notify(project, NotificationType.WARNING, "Couldn't read selected commit",
                    "Tip: left-click the commit row (not the details pane), then right-click it.\n" +
                            "Also ensure you're right-clicking the commit list, not a file in the details.")
                return
            }

            // Choose first (root, commit) where commit is a real commit object
            val chosen = chooseValidCommitAndRoot(gitRoots, candidates)
            if (chosen == null) {
                notify(
                    project,
                    NotificationType.ERROR,
                    "Couldn't resolve commit in any Git root",
                    buildString {
                        appendLine("Candidates:")
                        candidates.distinct().take(20).forEach { appendLine("• $it") }
                        if (candidates.size > 20) appendLine("• … (+${candidates.size - 20} more)")
                        appendLine()
                        appendLine("Git roots:")
                        gitRoots.forEach { appendLine("• ${it.absolutePath}") }
                        appendLine()
                        appendLine("This usually means the popup isn't on the commit row selection.")
                    }
                )
                return
            }

            val (repoDir, commitHash) = chosen

            // Get net-changed files from commit..HEAD + local changes
            val relPaths = linkedSetOf<String>()
            relPaths.addAll(runGitNameOnly(repoDir, listOf("git", "-c", "core.quotepath=false", "diff", "--name-only", "--diff-filter=ACMR", "$commitHash..HEAD")))
            relPaths.addAll(runGitNameOnly(repoDir, listOf("git", "-c", "core.quotepath=false", "diff", "--name-only", "--diff-filter=ACMR")))
            relPaths.addAll(runGitNameOnly(repoDir, listOf("git", "-c", "core.quotepath=false", "diff", "--name-only", "--cached", "--diff-filter=ACMR")))
            relPaths.addAll(runGitNameOnly(repoDir, listOf("git", "-c", "core.quotepath=false", "ls-files", "--others", "--exclude-standard")))

            if (relPaths.isEmpty()) {
                notify(
                    project,
                    NotificationType.WARNING,
                    "No changed files found",
                    "No changed files found since $commitHash.\nRoot: ${repoDir.absolutePath}\n\n" +
                            "Note: This is net diff (commit..HEAD) + local changes."
                )
                return
            }

            // Convert to existing absolute files and open them
            val absFiles = relPaths
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { File(repoDir, it).absolutePath }
                .distinct()
                .toList()

            val openedCount = openVirtualFiles(project, absFiles)
            notify(
                project,
                NotificationType.INFORMATION,
                "Opened changed files",
                "Opened $openedCount file(s) changed since $commitHash.\nRoot: ${repoDir.absolutePath}"
            )
        } catch (t: Throwable) {
            notify(
                project,
                NotificationType.ERROR,
                "Open All Changed Files failed",
                "${t.javaClass.simpleName}: ${t.message ?: "Unknown error"}"
            )
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Notifications (always visible, unlike silent returns)
    // ---------------------------------------------------------------------------------------------

    private fun notify(project: Project, type: NotificationType, title: String, content: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("OpenAllChangedFiles")
            .createNotification(title, content, type)
            .notify(project)
    }

    // ---------------------------------------------------------------------------------------------
    // Git roots (robust for multi-root projects)
    // ---------------------------------------------------------------------------------------------

    private fun getGitRoots(project: Project): List<File> {
        val rootsFromVcs = ProjectLevelVcsManager.getInstance(project).allVcsRoots
            .mapNotNull { it.path }
            .map { File(it.path) }
            .filter { File(it, ".git").exists() }
            .distinctBy { it.absolutePath }

        if (rootsFromVcs.isNotEmpty()) return rootsFromVcs

        // Fallback: walk up from basePath
        val base = project.basePath ?: return emptyList()
        return findGitRoot(File(base))?.let { listOf(it) } ?: emptyList()
    }

    private fun findGitRoot(start: File): File? {
        var dir: File? = if (start.isFile) start.parentFile else start
        while (dir != null) {
            if (File(dir, ".git").exists()) return dir
            dir = dir.parentFile
        }
        return null
    }

    // ---------------------------------------------------------------------------------------------
    // Commit hash candidates from UI context
    // ---------------------------------------------------------------------------------------------

    private val hexPattern = Pattern.compile("\\b[0-9a-fA-F]{7,40}\\b")

    private fun collectCandidateCommitHashes(e: AnActionEvent): List<String> {
        val out = linkedSetOf<String>()

        // 1) VCS_LOG_COMMIT_SELECTION (best when present)
        try {
            val sel = e.getData(com.intellij.vcs.log.VcsLogDataKeys.VCS_LOG_COMMIT_SELECTION)
            out.addAll(extractAllHashesFromObject(sel))
        } catch (_: Throwable) {}

        // 2) VCS_LOG (sometimes present)
        try {
            val log = e.getData(com.intellij.vcs.log.VcsLogDataKeys.VCS_LOG)
            if (log != null) {
                val selected = invokeFirstExistingNoArg(log, listOf("getSelectedCommits", "getSelectedCommitIds", "getSelectedCommitId"))
                when (selected) {
                    is List<*> -> selected.forEach { out.addAll(extractAllHashesFromObject(it)) }
                    else -> out.addAll(extractAllHashesFromObject(selected))
                }
            }
        } catch (_: Throwable) {}

        // 3) JTable from popup context component
        val table = findTableFromContextComponent(e)
        if (table != null) out.addAll(extractAllHashesFromTable(table))

        return out.toList()
    }

    private fun findTableFromContextComponent(e: AnActionEvent): JTable? {
        val comp = e.getData(PlatformDataKeys.CONTEXT_COMPONENT) as? JComponent ?: return null
        return when (comp) {
            is JTable -> comp
            else -> SwingUtilities.getAncestorOfClass(JTable::class.java, comp) as? JTable
        }
    }

    private fun extractAllHashesFromTable(table: JTable): List<String> {
        val out = linkedSetOf<String>()
        val rows = table.selectedRows?.toList().orEmpty()
        if (rows.isEmpty()) return emptyList()

        // Scan visible cell values
        for (r in rows) {
            for (c in 0 until table.columnCount) {
                val v = try { table.getValueAt(r, c)?.toString() } catch (_: Throwable) { null } ?: continue
                out.addAll(extractAllHashesFromString(v))
            }
        }

        // Probe model methods (int)->Any, looking for commit objects
        val model = table.model
        for (viewRow in rows) {
            val modelRow = try { table.convertRowIndexToModel(viewRow) } catch (_: Throwable) { viewRow }
            out.addAll(probeAllIntReturningMethodsForHashes(model, modelRow))
        }

        return out.toList()
    }

    private fun extractAllHashesFromObject(value: Any?): List<String> {
        if (value == null) return emptyList()
        val out = linkedSetOf<String>()

        // toString scan
        try { out.addAll(extractAllHashesFromString(value.toString())) } catch (_: Throwable) {}

        // Iterable scan
        if (value is Iterable<*>) {
            for (elem in value) out.addAll(extractAllHashesFromObject(elem))
        }

        // common getters
        val getters = listOf("getHash", "getId", "getCommitId", "getFullHash", "asString", "toShortString", "getName")
        for (g in getters) {
            try {
                val m = value.javaClass.methods.firstOrNull { it.name == g && it.parameterCount == 0 } ?: continue
                out.addAll(extractAllHashesFromObject(m.invoke(value)))
            } catch (_: Throwable) {}
        }

        return out.toList()
    }

    private fun extractAllHashesFromString(s: String): List<String> {
        val out = mutableListOf<String>()
        val m = hexPattern.matcher(s)
        while (m.find()) out.add(m.group())
        return out
    }

    private fun probeAllIntReturningMethodsForHashes(target: Any, arg: Int): List<String> {
        val out = linkedSetOf<String>()
        return try {
            val methods = target.javaClass.methods.filter { m ->
                m.parameterCount == 1 &&
                        m.parameterTypes[0] == Int::class.javaPrimitiveType &&
                        m.returnType != Void.TYPE
            }

            val sorted = methods.sortedByDescending { m ->
                val n = m.name.lowercase()
                when {
                    "hash" in n -> 4
                    "commit" in n -> 3
                    "id" in n -> 2
                    else -> 0
                }
            }

            for (m in sorted) {
                val res = try { m.invoke(target, arg) } catch (_: Throwable) { null }
                out.addAll(extractAllHashesFromObject(res))
            }
            out.toList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun invokeFirstExistingNoArg(target: Any, names: List<String>): Any? {
        for (name in names) {
            try {
                val m = target.javaClass.methods.firstOrNull { it.name == name && it.parameterCount == 0 } ?: continue
                return m.invoke(target)
            } catch (_: Throwable) {}
        }
        return null
    }

    // ---------------------------------------------------------------------------------------------
    // Validate candidates and choose correct root
    // ---------------------------------------------------------------------------------------------

    private fun chooseValidCommitAndRoot(gitRoots: List<File>, candidates: List<String>): Pair<File, String>? {
        for (root in gitRoots) {
            for (c in candidates) {
                if (gitCommitExists(root, c)) return root to c
            }
        }
        return null
    }

    private fun gitCommitExists(repoDir: File, hash: String): Boolean {
        return try {
            val proc = ProcessBuilder(listOf("git", "cat-file", "-e", "$hash^{commit}"))
                .directory(repoDir)
                .redirectErrorStream(true)
                .start()
            proc.inputStream.bufferedReader().readText()
            proc.waitFor() == 0
        } catch (_: Throwable) {
            false
        }
    }

    private fun runGitNameOnly(repoDir: File, cmd: List<String>): List<String> {
        return try {
            val proc = ProcessBuilder(cmd)
                .directory(repoDir)
                .redirectErrorStream(true)
                .start()
            val out = proc.inputStream.bufferedReader().readText()
            val exit = proc.waitFor()
            if (exit != 0) emptyList()
            else out.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Open files
    // ---------------------------------------------------------------------------------------------

    private fun openVirtualFiles(project: Project, absPaths: List<String>): Int {
        val fem = FileEditorManager.getInstance(project)
        val fs = LocalFileSystem.getInstance()
        val opened = mutableSetOf<String>()
        var count = 0

        for (p in absPaths) {
            val vf = fs.refreshAndFindFileByPath(p) ?: fs.findFileByIoFile(File(p))
            if (vf != null && !vf.isDirectory && opened.add(vf.path)) {
                fem.openFile(vf, true)
                count++
            }
        }

        return count
    }
}
