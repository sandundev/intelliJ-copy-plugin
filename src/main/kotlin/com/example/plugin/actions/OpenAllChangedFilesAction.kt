package com.example.plugin.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangesUtil
import com.intellij.openapi.vcs.changes.ui.ChangesListView
import com.intellij.openapi.vcs.changes.ui.VcsTreeModelData
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.util.regex.Pattern
import javax.swing.JTable

class OpenAllChangedFilesAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabledAndVisible = project != null &&
                (detectSelectedCommitFromGitLog(e) != null || collectChanges(e).isNotEmpty())
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        // 1) If we are in Git Log context, prefer that ALWAYS
        val logCommit = detectSelectedCommitFromGitLog(e)
        if (logCommit != null) {
            val (commitHash, repoRootHint) = logCommit

            // Get all files touched since the selected commit (includes all files touched by commits in the range,
            // and local uncommitted/untracked files).
            val paths = runGitTouchedFilesSinceCommit(project, commitHash, repoRootHint)

            if (paths.isNotEmpty()) {
                openVirtualFiles(project, paths)
            } else {
                Messages.showInfoMessage(project, "No changed files found since $commitHash.", "Open All Changed Files")
            }
            return
        }

        // 2) Otherwise use Changes UI selection (Compare with Local / Changes view)
        val changes = collectChanges(e)
        if (changes.isNotEmpty()) {
            openFilesFromChanges(project, changes)
            return
        }

        Messages.showInfoMessage(project, "Couldn't locate a Git Log selection or UI changes in this context.", "Open All Changed Files")
    }

    // ===== Git Log commit detection =====

    private fun detectSelectedCommitFromGitLog(e: AnActionEvent): Pair<String, String?>? {
        // Try to get VCS_LOG from context to confirm we're in Git Log
        val log = try {
            e.getData(com.intellij.vcs.log.VcsLogDataKeys.VCS_LOG)
        } catch (_: Throwable) { null }
        if (log == null) return null

        // Try LOG_UI_EX (internal but used by JetBrains actions like GoToHashOrRefAction)
        val logUi = try {
            val dataKeyClass = Class.forName("com.intellij.vcs.log.ui.VcsLogInternalDataKeys")
            val field = dataKeyClass.getField("LOG_UI_EX")
            val dataKey = field.get(null) as? com.intellij.openapi.actionSystem.DataKey<*>
            dataKey?.getData(e.dataContext)
        } catch (_: Throwable) { null }

        val table: JTable? = if (logUi != null) {
            try {
                logUi.javaClass.getMethod("getTable").invoke(logUi) as? JTable
            } catch (_: Throwable) { null }
        } else null

        val hexPattern = Pattern.compile("\\b[0-9a-fA-F]{7,40}\\b")

        if (table != null) {
            val viewRow = table.selectedRow
            if (viewRow >= 0) {
                // Convert to model row if possible
                val modelRow = try {
                    val m = table.javaClass.getMethod("convertRowIndexToModel", Int::class.javaPrimitiveType)
                    m.invoke(table, viewRow) as? Int ?: viewRow
                } catch (_: Throwable) { viewRow }

                // Try to extract commit hash from the table model
                val model = table.model
                val commitIdObj: Any? = run {
                    val candidates = listOf("getCommitIdAtRow", "getCommitId", "getIdAtRow", "getCommitIdAtModelRow")
                    for (name in candidates) {
                        try {
                            val m = model.javaClass.getMethod(name, Int::class.javaPrimitiveType)
                            return@run m.invoke(model, modelRow)
                        } catch (_: Throwable) {}
                    }
                    null
                }

                val hashFromObj = extractHashFromObject(commitIdObj, hexPattern)
                if (hashFromObj != null) {
                    val rootPath = extractRootPathFromCommitId(commitIdObj)
                    return hashFromObj to rootPath
                }

                // Fallback: scan visible row values for a hash-looking string
                for (col in 0 until table.columnCount) {
                    val v = try { table.getValueAt(viewRow, col)?.toString() } catch (_: Throwable) { null } ?: continue
                    val m = hexPattern.matcher(v)
                    if (m.find()) return m.group() to null
                }
            }
        }

        // Fallback: try VCS_LOG selected commits directly
        try {
            val selectedCommits = log.javaClass.getMethod("getSelectedCommits").invoke(log)
            if (selectedCommits is List<*> && selectedCommits.isNotEmpty()) {
                val first = selectedCommits[0]
                val hash = extractHashFromObject(first, hexPattern)
                if (hash != null) {
                    val rootPath = extractRootPathFromCommitId(first)
                    return hash to rootPath
                }
            }
        } catch (_: Throwable) {}

        return null
    }

    private fun extractHashFromObject(value: Any?, hexPattern: Pattern): String? {
        if (value == null) return null
        try {
            // Direct string representation
            val s = value.toString()
            val m = hexPattern.matcher(s)
            if (m.find()) return m.group()

            // If iterable, inspect elements
            if (value is Iterable<*>) {
                for (elem in value) {
                    val h = extractHashFromObject(elem, hexPattern)
                    if (h != null) return h
                }
            }

            // Inspect common getter method names on the object
            val methodNames = listOf("getId", "getCommitId", "getHash", "getFullHash", "asString", "toShortString")
            val cls = value.javaClass
            for (name in methodNames) {
                try {
                    val mth = cls.getMethod(name)
                    if (mth.parameterCount == 0) {
                        val res = mth.invoke(value)
                        if (res != null) {
                            val resStr = res.toString()
                            val mm = hexPattern.matcher(resStr)
                            if (mm.find()) return mm.group()
                        }
                    }
                } catch (_: Throwable) {}
            }
        } catch (_: Throwable) {}
        return null
    }

    private fun extractRootPathFromCommitId(commitIdObj: Any?): String? {
        if (commitIdObj == null) return null
        // CommitId often has getRoot(): VirtualFile
        return try {
            val root = commitIdObj.javaClass.getMethod("getRoot").invoke(commitIdObj)
            val vfPath = root?.javaClass?.getMethod("getPath")?.invoke(root) as? String
            vfPath
        } catch (_: Throwable) {
            null
        }
    }

    // ===== "From this revision onward" (touched files), not just net diff =====

    private fun runGitTouchedFilesSinceCommit(project: Project, commitHash: String, repoRootHint: String?): List<String> {
        val base = repoRootHint ?: project.basePath ?: return emptyList()
        val repoDir = File(base)

        // Include the selected commit's changes too by starting from its parent when possible
        val startRef = if (gitRevExists(repoDir, "$commitHash^")) "$commitHash^" else commitHash
        val range = "$startRef..HEAD"

        // 1) All files touched by commits in the range
        val touchedByCommits = runGitLines(repoDir, listOf("git", "log", "--name-only", "--pretty=format:", range))

        // 2) Include current local (uncommitted) changes and untracked files
        val workingTreeDiff = runGitLines(repoDir, listOf("git", "diff", "--name-only"))
        val modifiedTracked = runGitLines(repoDir, listOf("git", "ls-files", "-m"))
        val untracked = runGitLines(repoDir, listOf("git", "ls-files", "--others", "--exclude-standard"))

        val relPaths = (touchedByCommits + workingTreeDiff + modifiedTracked + untracked)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        return relPaths
            .map { File(repoDir, it).absolutePath }
            .distinct()
    }

    private fun gitRevExists(repoDir: File, rev: String): Boolean {
        return try {
            val proc = ProcessBuilder(listOf("git", "rev-parse", "--verify", rev))
                .directory(repoDir)
                .redirectErrorStream(true)
                .start()
            proc.inputStream.bufferedReader().readText()
            proc.waitFor() == 0
        } catch (_: Throwable) {
            false
        }
    }

    private fun runGitLines(repoDir: File, cmd: List<String>): List<String> {
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

    // ===== Changes UI fallback =====

    private fun collectChanges(e: AnActionEvent): List<Change> {
        val dc = e.dataContext

        // 1) direct selected changes (if provided by the UI)
        val selected = VcsDataKeys.SELECTED_CHANGES.getData(dc)
        if (!selected.isNullOrEmpty()) return selected.toList()

        // 2) fallback to changes tree/list view model (works in many VCS UIs)
        val view = ChangesListView.DATA_KEY.getData(dc)
        if (view != null) {
            val selectedFromTree = VcsTreeModelData.selected(view).userObjects(Change::class.java)
            if (selectedFromTree.isNotEmpty()) return selectedFromTree

            // if nothing selected, open *all visible* changes in that view
            val allVisible = VcsTreeModelData.all(view).userObjects(Change::class.java)
            if (allVisible.isNotEmpty()) return allVisible
        }

        return emptyList()
    }

    private fun changeToVirtualFile(change: Change): VirtualFile? {
        val fp: FilePath = ChangesUtil.getFilePath(change)
        return fp.virtualFile
            ?: LocalFileSystem.getInstance().findFileByPath(fp.path)
    }

    private fun openFilesFromChanges(project: Project, changes: List<Change>) {
        val files = changes.asSequence()
            .mapNotNull { changeToVirtualFile(it) }
            .distinctBy { it.path }
            .toList()

        openVirtualFiles(project, files.map { it.path })
    }

    // ===== File opening =====

    private fun openVirtualFiles(project: Project, paths: List<String>) {
        val fem = FileEditorManager.getInstance(project)
        val fs = LocalFileSystem.getInstance()
        val opened = mutableSetOf<String>()
        for (p in paths) {
            val vf = fs.findFileByPath(p) ?: fs.findFileByIoFile(File(p))
            if (vf != null && opened.add(vf.path)) {
                fem.openFile(vf, true)
            }
        }
    }
}
