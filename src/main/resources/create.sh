#!/bin/bash

# Base path
PROJECT_ROOT="/Users/sandun/Documents/PersonalWorkSpace/IntelliJ-plugins"
SRC_PATH="$PROJECT_ROOT/src/main"
KOTLIN_PATH="$SRC_PATH/kotlin"
RESOURCES_PATH="$SRC_PATH/resources/META-INF"

# Ensure directory structure
mkdir -p "$KOTLIN_PATH"
mkdir -p "$RESOURCES_PATH"

# ✅ 1. build.gradle.kts
cat > "$PROJECT_ROOT/build.gradle.kts" <<EOF
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
        sinceBuild.set("233")
        untilBuild.set("999.*")
    }
}
EOF

# ✅ 2. settings.gradle.kts
cat > "$PROJECT_ROOT/settings.gradle.kts" <<EOF
rootProject.name = "CopyFileContentsPlugin"
EOF

# ✅ 3. plugin.xml
cat > "$RESOURCES_PATH/plugin.xml" <<EOF
<idea-plugin>
    <id>com.example.copyfilecontents</id>
    <name>Copy File Contents</name>
    <version>1.0</version>
    <vendor>Sandun</vendor>

    <description>
        Adds a context menu action to copy contents of selected files (with full paths) to the clipboard.
    </description>

    <actions>
        <action id="CopyFileContentsAction"
                class="CopyFileContentsAction"
                text="Copy Contents of Selected Files to Clipboard"
                description="Copies the full content of selected files to clipboard.">
            <add-to-group group-id="ProjectViewPopupMenu" anchor="last"/>
        </action>
    </actions>
</idea-plugin>
EOF

echo "✅ IntelliJ plugin project is set up. Open it in IntelliJ and run 'runIde' Gradle task to launch the plugin sandbox."
