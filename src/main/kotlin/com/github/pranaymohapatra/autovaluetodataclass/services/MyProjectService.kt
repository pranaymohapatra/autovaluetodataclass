package com.github.pranaymohapatra.autovaluetodataclass.services

import com.intellij.openapi.project.Project
import com.github.pranaymohapatra.autovaluetodataclass.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
