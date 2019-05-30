package com.ramnani.alexaskills.gradle.task

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.Regions
import com.amazonaws.services.lambda.AWSLambda
import com.amazonaws.services.lambda.AWSLambdaClientBuilder
import com.amazonaws.services.lambda.model.GetFunctionRequest
import com.amazonaws.services.lambda.model.UpdateFunctionCodeRequest
import org.apache.commons.io.IOUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

import java.io.File
import java.nio.ByteBuffer
import java.io.FileInputStream


open class PublishFunctionTask : DefaultTask() {

    @Input
    lateinit var region: String

    @Input
    lateinit var awsAccessKeyId: String

    @Input
    lateinit var awsSecretAccessKey: String

    @Input
    lateinit var functionName: String

    @Input
    lateinit var archivePath: File

    lateinit var testEvent: String


    private fun validateProperties() {
        if (!::region.isInitialized) {
            throw IllegalArgumentException("Please configure 'region'")
        }

        if (!::functionName.isInitialized) {
            throw IllegalArgumentException("Please configure 'functionName'")
        }

        if (!::archivePath.isInitialized) {
            throw IllegalArgumentException("Could not find the archivePath for the artifact.")
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
        println("Target Function ARN: $functionArn. Updating the function now. "
                + "Project Archive: ${archivePath.absolutePath}")

        var byteArray: ByteArray? = null

        FileInputStream(archivePath).use {
            byteArray = IOUtils.toByteArray(it)
        }
        val byteBuff = ByteBuffer.wrap(byteArray)

        val updateFunctionCodeRequest = UpdateFunctionCodeRequest()
                .withFunctionName(functionName)
                .withZipFile(byteBuff)

        val updateFunctionCodeResponse = awsLambda.updateFunctionCode(updateFunctionCodeRequest)
        val codeSize = updateFunctionCodeResponse?.codeSize

        println("Lambda function code size: $codeSize in function ARN: $functionArn")
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