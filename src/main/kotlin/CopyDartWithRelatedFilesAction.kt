import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import java.awt.datatransfer.StringSelection
import com.intellij.openapi.diagnostic.Logger
class CopyDartWithRelatedFilesAction : AnAction("Copy This and Related Dart Files") {

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val file: VirtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        if (!file.name.endsWith(".dart")) return

        val fileIndex = ProjectRootManager.getInstance(project).fileIndex
        val packageName: String? = getProjectPackageName(project)
        val visited = mutableSetOf<VirtualFile>()
        val builder = StringBuilder()

        fun processFile(f: VirtualFile) {
            if (f in visited || !fileIndex.isInContent(f) || f.isDirectory) return
            visited += f

            val content = f.inputStream.bufferedReader().use { it.readText() }
            builder.append("// === File: ${f.path} ===\n")
            builder.append(content).append("\n\n")

            extractDartImports(content)
                .mapNotNull { resolveImportPath(it, f, project, packageName) }
                .forEach { processFile(it) }
        }

        processFile(file)
        CopyPasteManager.getInstance().setContents(StringSelection(builder.toString()))  // :contentReference[oaicite:0]{index=0}
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = true
    }

    private fun extractDartImports(content: String): List<String> =
        Regex("""import\s+['"]([^'"]+)['"]""")
            .findAll(content)
            .map { it.groupValues[1] }
            .toList()

    private fun getProjectPackageName(project: Project): String? {
        val pubspec = project.baseDir.findChild("pubspec.yaml") ?: return null
        return pubspec.inputStream
            .bufferedReader()
            .readLines()
            .firstOrNull { it.trim().startsWith("name:") }
            ?.substringAfter("name:")
            ?.trim()
    }

    private fun resolveImportPath(
        import: String,
        currentFile: VirtualFile,
        project: Project,
        packageName: String?
    ): VirtualFile? {
        val fileIndex = ProjectRootManager.getInstance(project).fileIndex

        return when {
            // project-local package import
            import.startsWith("package:$packageName/") -> {
                project.baseDir.findFileByRelativePath(
                    import.removePrefix("package:$packageName/")
                )
            }

            // any other package import → skip
            import.startsWith("package:") -> null

            // relative import
            import.startsWith("../") || import.startsWith("./") -> {
                currentFile.parent.findFileByRelativePath(import)
            }

            else -> null
        }?.takeIf { it != null && fileIndex.isInContent(it) }
    }
}
