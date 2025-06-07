plugins {
    kotlin("jvm") version "1.9.10"
    id("org.jetbrains.intellij") version "1.16.0"
}

group = "com.example"
version = "1.0"

repositories {
    mavenCentral()
}

intellij {
    version.set("2023.3.2")
    type.set("IC") // IC = Community Edition
}

tasks {
    patchPluginXml {
        sinceBuild.set("211")
        untilBuild.set("999.*")
    }
}
