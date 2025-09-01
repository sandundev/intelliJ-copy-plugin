import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import java.awt.datatransfer.StringSelection

class CopyOpenedFileNamesAction : AnAction(
    "Copy Opened File Names",
    "Copy the relative file names of all opened editor tabs",
    null
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val basePath = project.basePath ?: return
        val openFiles: Array<VirtualFile> = FileEditorManager.getInstance(project).openFiles

        if (openFiles.isEmpty()) return

        val names = openFiles.map { f ->
            VfsUtil.getRelativePath(f, project.baseDir) ?: f.name
        }

        CopyPasteManager.getInstance().setContents(StringSelection(names.joinToString("\n")))
    }

    override fun update(e: AnActionEvent) {
        val project: Project? = e.getData(CommonDataKeys.PROJECT)
        val hasOpenFiles = project?.let {
            FileEditorManager.getInstance(it).openFiles.isNotEmpty()
        } ?: false
        e.presentation.isEnabledAndVisible = hasOpenFiles
    }
}
