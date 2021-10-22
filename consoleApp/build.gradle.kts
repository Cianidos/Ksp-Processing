import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.31"
    id("com.google.devtools.ksp") version "1.5.31-1.0.0"
    application
}

group = "me.ariel"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(project(":AnnotationProcessor"))
    implementation(project(":Annotations"))
    testImplementation(kotlin("test"))
    ksp(project(":AnnotationProcessor"))
}


kotlin.sourceSets.main {
    kotlin.srcDirs(
        file("$buildDir/generated/ksp/main/kotlin"),
    )
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}

application {
    mainClass.set("MainKt")
}