# IntelliJ Copy Plugin

A simple IntelliJ IDEA plugin that makes it easier to copy code and text cleanly from the IDE.  
This is especially useful when working with **AI chat prompts** (e.g., GPT-5, ChatGPT, or other LLMs) where you want to quickly copy code snippets without extra formatting or distractions.

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

This will generate the plugin artifact in:

```
build/libs/
```

Example: `intelliJ-copy-plugin.jar`

### 3. Install in IntelliJ IDEA

1. Open **IntelliJ IDEA**.
2. Go to **File → Settings** (or **Preferences** on macOS) → **Plugins**.
3. Click the **gear icon (⚙️)** at the top.
4. Select **Install Plugin from Disk...**.
5. Navigate to the `build/libs` directory and choose the generated `.jar` file.
6. Click **OK** and restart IntelliJ when prompted.

---

## ▶️ Run the Plugin in Development Mode (Optional)

For testing, you can run the plugin directly in a sandbox IDE using:

```bash
./gradlew runIde
```

This will launch a new IntelliJ instance with the plugin loaded.

---

## 📦 Packaging (Optional)

If you want to package the plugin for distribution (ZIP format):

```bash
./gradlew buildPlugin
```

The distributable `.zip` file will be available under:

```
build/distributions/
```

---

## 💡 Usage

* Select the code or text in IntelliJ IDEA.
* Use the plugin's copy command to grab the snippet.
* Paste it directly into your **AI assistant (like GPT-5 or ChatGPT)** for clean prompts.

This helps streamline workflows where you frequently copy code into AI chats for:

* Debugging help
* Code reviews
* Refactoring suggestions
* Generating documentation

---

## 📝 Notes

* Make sure you are using a compatible IntelliJ IDEA version.
* If you face build issues, try cleaning first:

  ```bash
  ./gradlew clean build
  ```

---

## 📚 References

* [Gradle IntelliJ Plugin Docs](https://plugins.jetbrains.com/docs/intellij/gradle-build-system.html)
* [Installing plugins manually](https://www.jetbrains.com/help/idea/managing-plugins.html#install_plugin_from_disk)

---

Happy coding & prompting 🚀

``` 
