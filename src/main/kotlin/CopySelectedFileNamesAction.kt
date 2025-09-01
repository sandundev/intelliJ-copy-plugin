import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import java.awt.datatransfer.StringSelection

class CopySelectedFileNamesAction : AnAction(
    "Copy Selected File Names",
    "Copy the relative file names of selected files",
    null
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val files: Array<VirtualFile> = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
        val names = files.filter { it.isValid && !it.isDirectory }.map { f ->
            VfsUtil.getRelativePath(f, project.baseDir) ?: f.name
        }
        if (names.isNotEmpty()) {
            CopyPasteManager.getInstance().setContents(StringSelection(names.joinToString("\n")))
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = true
    }
}
