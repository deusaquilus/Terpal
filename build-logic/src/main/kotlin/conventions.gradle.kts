import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

repositories {
    mavenLocal()
    mavenCentral()
}

plugins {
  kotlin("jvm")
}

group = "io.exoquery"
// Everything inherits the version from here
version = "2.0.0-0.1.1"

check("$version".isNotBlank() && version != "unspecified")
    { "invalid version $version" }

java {
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "11"
    }
}
