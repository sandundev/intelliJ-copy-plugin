package com.example.plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangesUtil
import com.intellij.openapi.vcs.changes.ui.ChangesListView
import com.intellij.openapi.vcs.changes.ui.VcsTreeModelData
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile

class OpenAllChangedFilesAction : AnAction() {

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabledAndVisible = project != null && collectChanges(e).isNotEmpty()
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val changes = collectChanges(e)
        val files = changes.asSequence()
            .mapNotNull { changeToVirtualFile(it) }
            .distinctBy { it.path }
            .toList()

        val fem = FileEditorManager.getInstance(project)
        for (vf in files) {
            fem.openFile(vf, true)
        }
    }

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
        // Prefer "after" path
        val fp: FilePath = ChangesUtil.getFilePath(change)
        return fp.virtualFile
            ?: LocalFileSystem.getInstance().findFileByPath(fp.path)
    }
}

