import com.intellij.ide.UiActivity
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*

class ConvertFromAutoValueToDataClassAction : AnAction("Convert from Abstract Class to Data Class") {

    override fun actionPerformed(event: AnActionEvent) {
        val conversionHelper = DataClassConversionHelper()
        conversionHelper.startConversion(event)

//    override fun update(e: AnActionEvent) {
//        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
//        val project = e.project ?: return
//        e.presentation.isEnabled = isAnyJavaFileSelected(project, virtualFiles)
//    }
//
    }

    private fun isAnyJavaFileSelected(project: Project, files: Array<VirtualFile>): Boolean {
        val manager = PsiManager.getInstance(project)
        if (files.any { manager.findFile(it) is PsiJavaFile && it.isWritable }) return true
        return files.any { it.isDirectory && isAnyJavaFileSelected(project, it.children) }
    }
    private fun selectedJavaFiles(e: AnActionEvent): Sequence<PsiJavaFile> {
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return sequenceOf()
        val project = e.project ?: return sequenceOf()
        return allJavaFiles(virtualFiles, project)
    }

    private fun allJavaFiles(filesOrDirs: Array<VirtualFile>, project: Project): Sequence<PsiJavaFile> {
        val manager = PsiManager.getInstance(project)
        return allFiles(filesOrDirs)
            .asSequence()
            .mapNotNull { manager.findFile(it) as? PsiJavaFile }
    }

    private fun allFiles(filesOrDirs: Array<VirtualFile>): Collection<VirtualFile> {
        val result = ArrayList<VirtualFile>()
        for (file in filesOrDirs) {
            VfsUtilCore.visitChildrenRecursively(file, object : VirtualFileVisitor<Unit>() {
                override fun visitFile(file: VirtualFile): Boolean {
                    result.add(file)
                    return true
                }
            })
        }
        return result
    }
}