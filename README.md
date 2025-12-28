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

### 🔄 Git Integration
- **Open All Changed Files**  
  Opens all changed files from the current diff/changes list directly in the editor. Available in:
  - Changes view popup menu (when viewing Compare with Local results)
  - Git Log commit context menu (uses a git-diff fallback to compute changed files)
  - Works with selected changes or all visible changes if none selected


---

## 🔧 Build & Install

### 1. Clone the repository
```bash
git clone https://github.com/sandundev/intelliJ-copy-plugin.git
cd intelliJ-copy-plugin
```

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

## 💡 Usage: where to find "Open All Changed Files"

The action can appear in two places depending on your workflow:

1) Compare / Changes view (recommended flow)
- Open `Git` → `Log`.
- Right‑click an older commit and choose `Compare with Local`.
- A new tool window / popup will show the list of changed files (the Changes view).
- Right‑click inside *that list* (the changes/diff view) — you should see `Open All Changed Files`. This will open each changed file in an editor tab.

2) Git Log commit context menu (convenience)
- Right‑click a commit in the Git Log directly. The action is also registered in the commit popup, but in this context the plugin needs to compute changed files itself. The plugin will run `git diff --name-only <commit> HEAD` in your project root to find changed files and open them.

Note: if the action is visible but disabled in a given context, it means the plugin couldn't locate changes or resolve a commit id from that menu context.

---

## 📚 References

* [Gradle IntelliJ Plugin Docs](https://plugins.jetbrains.com/docs/intellij/gradle-build-system.html)
* [Manual Plugin Installation](https://www.jetbrains.com/help/idea/managing-plugins.html#install_plugin_from_disk)

---

Happy coding & prompting 🚀
© Sandun Lewke Bandara
