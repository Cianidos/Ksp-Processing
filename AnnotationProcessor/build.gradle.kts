import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.31"
}

group = "me.ariel"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(project(":Annotations"))
    testImplementation(kotlin("test"))
    implementation("com.google.devtools.ksp:symbol-processing-api:1.5.31-1.0.0")
    implementation("com.squareup:kotlinpoet-ksp:1.10.1")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.5.31")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}