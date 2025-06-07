import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ide.CopyPasteManager
import java.awt.datatransfer.StringSelection

class CopyFileContentsAction : AnAction("Copy Contents of Selected Files to Clipboard") {
    override fun actionPerformed(e: AnActionEvent) {
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
        val builder = StringBuilder()

        files.filter { it.isValid && !it.isDirectory }.forEach { file ->
            val filePath = file.path
            val content = file.inputStream.bufferedReader().use { it.readText() }

            builder.append("// === File: ").append(filePath).append(" ===\n")
            builder.append(content).append("\n\n")
        }

        val selection = StringSelection(builder.toString())
        CopyPasteManager.getInstance().setContents(selection)
    }

    override fun update(e: AnActionEvent) {
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        e.presentation.isEnabledAndVisible = files != null && files.any { !it.isDirectory }
    }
}
