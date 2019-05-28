import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    id("org.jetbrains.kotlin.jvm") version "1.2.51"
    id("maven-publish")
    id("java-gradle-plugin")
}

group = "com.ramnani.alexaskills"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    compile(gradleApi())
    compile("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compile("com.amazonaws:aws-java-sdk-lambda:1.11.486")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

publishing {
    publications {
        create<MavenPublication>("alexaSkillsManagerLib") {
            from(components["java"])
        }
    }

    repositories {
        mavenLocal()
    }
}