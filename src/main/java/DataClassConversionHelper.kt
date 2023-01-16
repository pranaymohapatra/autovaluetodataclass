import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.ui.Messages
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.search.searches.ReferencesSearch
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.jetbrains.kotlin.asJava.toLightAnnotation
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.findFunctionByName
import org.jetbrains.kotlin.psi.psiUtil.isAbstract

class DataClassConversionHelper {
    //private var progressValue = 0
    //private val progressDialog: ConvertToDataClassDialog = ConvertToDataClassDialog(this)
    fun startConversion(conversionEvent: AnActionEvent) {
//        progressDialog.pack()
//        progressDialog.isVisible = true
//        val myAction = ActionManager.getInstance().getAction("ConvertJavaToKotlin")!!
//        conversionEvent.apply {
//            ActionManager.getInstance().addAnActionListener(object : AnActionListener {
//                override fun afterActionPerformed(action: AnAction, dataContext: DataContext, event: AnActionEvent) {
//                    if (action.templateText == "Convert from Abstract Class to Data Class")
//                        convertClass(conversionEvent)
//                }
//            })
//            myAction.actionPerformed(conversionEvent)
//        }
        convertClass(conversionEvent)
    }

    private fun convertClass(anActionEvent: AnActionEvent) {
        anActionEvent.apply {
            val classMap = mutableMapOf<KtClass, MutableList<KtNamedFunction>>()
            val file = getRequiredData(LangDataKeys.PSI_FILE)
            //Go through all abstract classes and their corresponding abstract methods in the file
            file.accept(object : KtTreeVisitor<Any>() {
                override fun visitClass(klass: KtClass, data: Any?): Void? {
                    klass.annotationEntries[0].toLightAnnotation()?.qualifiedName
                    if (!klass.isData() && klass.isAbstract() && klass.annotationEntries[0].toLightAnnotation()?.qualifiedName == "com.google.auto.value.AutoValue") {
                        classMap[klass] = mutableListOf()
                    }
                    return super.visitClass(klass, data)
                }

                override fun visitNamedFunction(function: KtNamedFunction, data: Any?): Void? {
                    val klass = function.containingClass()
                    if (klass != null
                        && classMap.contains(function.containingClass())
                        && function.hasModifier(KtTokens.ABSTRACT_KEYWORD)
                    ) {
                        classMap.getValue(klass).add(function)
                    }
                    return super.visitNamedFunction(function, data)
                }
            })
            //setProgressConversion(10)
            val write = WriteCommandAction.writeCommandAction(project)
            var factory: KtPsiFactory? = null
//            val incrementValue = 90 / classMap.size
            write.run<Throwable> {
                //Convert each class to data class
                classMap.flatMap { (klass, functions) ->
                    factory = KtPsiFactory(klass)
                    klass.addModifier(KtTokens.DATA_KEYWORD)
                    klass.removeModifier(KtTokens.ABSTRACT_KEYWORD)
                    klass.createPrimaryConstructorIfAbsent()
                    klass.createPrimaryConstructorParameterListIfAbsent()
                    klass.getProperties()
                    val annt = klass.annotationEntries[0]
                    annt.replace(factory!!.createAnnotationEntry("@${JsonClass::class.java.name}(generateAdapter = true)"))
                    if (klass.getSuperTypeList()?.text == "Parcelable") {
                        klass.addAnnotationEntry(factory!!.createAnnotationEntry("@kotlinx.android.parcel.Parcelize"))
                    }
                    //setProgressConversion((incrementValue * 0.25).toInt())
                    klass.companionObjects.forEach {
                        findFunction(it, "typeAdapter")?.delete()
                        findFunction(it, "create")?.apply {
                            this.typeReference?.replace(factory!!.createType("${klass.name}"))
                            (this.lastChild as KtBlockExpression).children.forEach { element ->
                                if (element is KtReturnExpression) {
                                    val namedExpression = element.children[0].children[0] as KtNameReferenceExpression
                                    namedExpression.replace(factory!!.createSimpleName("${klass.name}"))
                                }
                            }
                        }
                        if (it.declarations.isEmpty())
                            it.delete()
                    }
                    //setProgressConversion((incrementValue * 0.25).toInt())
                    return@flatMap functions.map { Pair(klass, it) }
                }.forEach { (klass, function) ->
                    //Convert each abstract function to a data class field
                    val oldName = function.name!!
                    val paramName = if (oldName.length <= 3)
                        oldName
                    else if (oldName.length > 3 && oldName.substring(0, 3) == "get")
                        oldName.substring(3)
                    else
                        oldName
                    val newName = if (oldName.length > 3 && oldName.substring(0, 3) == "get") {
                        oldName
                    } else {
                        "get${oldName.capitalize()}"
                    }

                    val param = factory!!.createParameter("val $paramName: ${function.typeReference?.text}")
                    function.annotationEntries.forEach {
                        val annotation = it?.toLightAnnotation()
                        if (annotation != null && annotation.qualifiedName == "com.google.gson.annotations.SerializedName") {
                            val jsonName = annotation.findAttributeValue("value")?.text
                            param.addAnnotationEntry(factory!!.createAnnotationEntry("@${Json::class.java.name}(name = $jsonName)"))
                        }
                    }
                    klass.getPrimaryConstructorParameterList()?.addParameter(param)

                    ReferencesSearch.search(function).findAll().forEach {
                        when (it.element.language) {
                            JavaLanguage.INSTANCE -> it.handleElementRename(newName)
                            KotlinLanguage.INSTANCE -> it.element.parent.replace(factory!!.createExpression(param.name!!))
                        }
                    }
                    function.delete()
                    JavaCodeStyleManager.getInstance(project).shortenClassReferences(klass)
                    //setProgressConversion((incrementValue * 0.50).toInt())
                }
                classMap.forEach {
                    it.key.apply {
                        if (body?.declarations?.isEmpty() == true) {
                            body?.lBrace?.delete()
                            body?.rBrace?.delete()
                        }
                    }
                }
            }
            //progressDialog.setProgressValue(progressValue)
            Messages.showMessageDialog(
                project,
                "Conversion to Data Class completed successfully",
                "Done!",
                Messages.getInformationIcon()
            )
        }
    }

    private fun findFunction(ktObj: KtObjectDeclaration, functionName: String): KtNamedFunction? {
        return ktObj.findFunctionByName(functionName) as KtNamedFunction?
    }

//    private fun setProgressConversion(increment: Int) {
//        progressValue += increment
//        progressDialog.setProgressValue(progressValue)
//    }
}