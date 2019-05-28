package com.ramnani.alexaskills.gradle.task

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.Regions
import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.AWSLambdaClientBuilder
import com.amazonaws.services.lambda.model.GetFunctionRequest
import org.gradle.api.DefaultTask
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.plugin.devel.plugins.JavaGradlePluginPlugin
import java.nio.ByteBuffer
import java.nio.file.Files

open class PublishFunctionTask : DefaultTask() {

    @Input
    var region: String? = null

    @Input
    var awsAccessKeyId: String? = null

    @Input
    var awsSecretAccessKey: String? = null

    @Input
    var functionName: String? = null

    var testEvent: String? = null


    private fun validateProperties() {
        if (region ==  null) {
            throw IllegalArgumentException("Please configure 'region'")
        }

        if (functionName == null) {
            throw IllegalArgumentException("Please configure 'functionName'")
        }
    }

    @TaskAction
    fun publishFunction() {
        validateProperties()
        println("Provided AWS Region: $region" )

        val awsLambda = buildAWSLambdaClient()

        val getFunctionRequest = GetFunctionRequest().withFunctionName(functionName!!)
        val getFunctionResult = awsLambda.getFunction(getFunctionRequest)
        val functionArn = getFunctionResult?.configuration?.functionArn

        if (functionArn.isNullOrBlank()) {
            println("Could not find function with name $functionName")
            System.exit(0)
        }
        println("Target Function ARN: $functionArn. Updating the function now.")

        println("Project ZIP path: ${getProjectZip()}")
        //val updateFunctionCodeRequest = UpdateFunctionCodeRequest()
        //        .withFunctionName(functionName!!)
        //        .withZipFile(zipFile)

    }

    fun getProjectZip() : String {

        return project.properties["distsDir"]!!.toString()
    }

    fun buildAWSLambdaClient() : AWSLambda {
        var awsCredsProvider: AWSCredentialsProvider

        if (awsAccessKeyId.isNullOrBlank() || awsSecretAccessKey.isNullOrBlank()) {
            println("AWS Credentials not provided in the property declaration. Falling back to Default Credentials Provider chain.")
            awsCredsProvider = DefaultAWSCredentialsProviderChain()
        } else  {
            println("Creating Basic AWS Credentials.")
            awsCredsProvider = AWSStaticCredentialsProvider(BasicAWSCredentials(awsAccessKeyId, awsSecretAccessKey))
        }
        val awsLambdaBuilder: AWSLambdaClientBuilder = AWSLambdaClientBuilder.standard()

        try {
            awsLambdaBuilder.region = Regions.fromName(region!!).getName()
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Illegal AWS region name: $region")
        }
        awsLambdaBuilder.credentials = awsCredsProvider

        return awsLambdaBuilder.build()
    }
}