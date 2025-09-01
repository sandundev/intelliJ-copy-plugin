import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.awt.datatransfer.StringSelection

class CopyAllOpenedFileContentsAction : AnAction(
    "Copy All Opened File Contents",
    "Copy the contents of all currently opened editor tabs to the clipboard",
    null
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val openFiles: Array<VirtualFile> = FileEditorManager.getInstance(project).openFiles
        if (openFiles.isEmpty()) return

        val fdm = FileDocumentManager.getInstance()
        val out = StringBuilder()

        openFiles.forEachIndexed { idx, vf ->
            val doc = fdm.getDocument(vf) ?: return@forEachIndexed
            // Add a friendly header per file so prompts are clear (optional)
            out.append("===== ").append(vf.path).append(" =====\n")
            out.append(doc.text)
            if (idx != openFiles.lastIndex) out.append("\n\n")
        }

        CopyPasteManager.getInstance().setContents(StringSelection(out.toString()))
    }

    override fun update(e: AnActionEvent) {
        val project: Project? = e.getData(CommonDataKeys.PROJECT)
        val hasOpenFiles = project?.let {
            FileEditorManager.getInstance(it).openFiles.isNotEmpty()
        } ?: false
        e.presentation.isEnabledAndVisible = hasOpenFiles
    }
}
