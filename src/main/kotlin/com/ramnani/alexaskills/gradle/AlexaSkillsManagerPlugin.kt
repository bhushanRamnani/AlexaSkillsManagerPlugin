package com.ramnani.alexaskills.gradle

import com.ramnani.alexaskills.gradle.task.PublishFunctionTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.bundling.Zip
import java.io.File


class AlexaSkillsManagerPlugin : Plugin<Project> {

    companion object {
        const val PUBLISH_FUNCTION_TASK = "publishFunction"
        const val BUILD_ZIP_TASK = "buildZip"
    }

    lateinit var archivePath: File

    /**
     * Step 1: Upload the jar to lambda with a new version
     * Step 2: Test the lambda function. If the test fails then post error
     * Step 3: Using Alexa dev API, update the new lambda endpoint information in Alexa
     * Step 4: Build Alexa model
     * Step 5: Test Alexa model
     * Step 6: If the test fails, rollback to previous version of the lambda
     */
    override fun apply(project: Project) {
        val extension = project.extensions.create<AlexaSkillsManagerExtension>("alexaExt",
                AlexaSkillsManagerExtension::class.java)

        if (!project.pluginManager.hasPlugin("java")) {
            println("Applying java plugin")
            project.pluginManager.apply(JavaPlugin::class.java)
        }

        project.tasks.register(BUILD_ZIP_TASK, Zip::class.java) {
            it.from(project.tasks.getByPath("compileJava"))
            it.from(project.tasks.getByPath("processResources"))
            it.into("lib") { d ->
                d.from(project.configurations.getByName("compileClasspath"))
            }
            this.archivePath = it.archivePath
        }

        project.tasks.register(PUBLISH_FUNCTION_TASK,
                PublishFunctionTask::class.java) {
            it.dependsOn(project.tasks.getByPath(BUILD_ZIP_TASK))

            it.awsAccessKeyId = extension.awsAccessKeyId
            it.awsSecretAccessKey = extension.awsSecretAccessKey
            it.region = extension.region
            it.functionName = extension.functionName
            it.archivePath = this.archivePath
        }
    }
}
