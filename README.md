# IntelliJ Copy Plugin

A lightweight IntelliJ IDEA plugin that makes it easier to copy **file contents** or **file names** directly from the IDE.  
This is especially useful when working with **AI chat prompts** (e.g., GPT-5, ChatGPT, or other LLMs) or when preparing snippets for documentation and code reviews.

---

## ✨ Features

The plugin adds several context menu actions inside IntelliJ IDEA:

### 📂 File Content Copying
- **Copy Contents of Selected Files to Clipboard**  
  Copies the full contents of one or more selected files, with a header showing the file path.

- **Copy All Opened File Contents**  
  Copies the contents of all currently opened editor tabs, with headers for each file.

- **Copy This and Related Dart Files**  
  Special Dart support: copies a selected Dart file along with all project-local imports it references (following `import` statements).

### 📄 File Name Copying
- **Copy Opened File Names**  
  Copies the relative paths (from the project root) of all currently opened editor tabs.

- **Copy Selected File Names**  
  Copies the relative paths (from the project root) of selected files in the Project tool window.

---

## 🔧 Build & Install

### 1. Clone the repository
```bash
git clone https://github.com/sandundev/intelliJ-copy-plugin.git
cd intelliJ-copy-plugin
````

### 2. Build the plugin

Use the included Gradle wrapper to build:

* On **macOS/Linux**:

  ```bash
  ./gradlew build
  ```
* On **Windows**:

  ```cmd
  gradlew.bat build
  ```

Artifacts will be in:

```
build/libs/
```

Example: `intelliJ-copy-plugin.jar`

### 3. Install in IntelliJ IDEA

1. Open **IntelliJ IDEA**.
2. Go to **File → Settings** (or **Preferences** on macOS) → **Plugins**.
3. Click the **gear icon (⚙️)** → **Install Plugin from Disk...**.
4. Select the generated `.jar` from `build/libs`.
5. Restart IntelliJ when prompted.

---

## ▶️ Development Mode

Run the plugin in a sandbox IDE for testing:

```bash
./gradlew runIde
```

---

## 📦 Packaging for Distribution

To create a distributable `.zip`:

```bash
./gradlew buildPlugin
```

Find it in:

```
build/distributions/
```

---

## 💡 Example Use Cases

* Quickly copying multiple files to paste into **AI prompts**.
* Exporting **Dart files with their dependencies** for sharing.
* Grabbing a list of **relative file paths** for documentation, bug reports, or project planning.
* Preparing **clean snippets** for code reviews.

---

## 📝 Notes

* Works with IntelliJ IDEA 2021.1+ (IC/Ultimate).
* If you hit build issues, try:

  ```bash
  ./gradlew clean build
  ```

---

## 📚 References

* [Gradle IntelliJ Plugin Docs](https://plugins.jetbrains.com/docs/intellij/gradle-build-system.html)
* [Manual Plugin Installation](https://www.jetbrains.com/help/idea/managing-plugins.html#install_plugin_from_disk)

---

Happy coding & prompting 🚀

```