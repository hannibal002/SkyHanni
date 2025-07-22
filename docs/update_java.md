# Optimize Minecraft Performance by Updating Java

## Background
The Vanilla Minecraft Launcher uses Java 8 Update 51. This version is over nine years old and lacks modern optimizations. Upgrading to a newer Java 8 build or an optimized open-source distribution can boost FPS and reduce lag.

## Recommended Java Distributions
Choose one of these Java 8 options:

- [Oracle Java](https://www.java.com/download/ie_manual.jsp)  
  *The original distribution from Oracle.*
- [Eclipse Temurin (Adoptium)](https://adoptium.net/temurin/releases/?version=8)  
  *A free, open-source build with performance optimizations.*
- [Zulu OpenJDK](https://www.azul.com/downloads/?version=java-8-lts&package=jdk#zulu)  
  *Reliable builds maintained by Azul Systems.*
- [Amazon Corretto](https://docs.aws.amazon.com/corretto/latest/corretto-8-ug/downloads-list.html)  
  *A production-ready build supported by Amazon.*

## Instructions

### 1. Download and Install Java 8
1. Choose a distribution from the list above.
2. Download the installer or package for your operating system.
3. Follow the provided instructions to install the latest Java 8 update.

## Configuring the Launcher

### A. [Vanilla Minecraft Launcher](https://www.minecraft.net/en-us/download)
1. **Open the Minecraft Launcher.**
2. **Go to "Installations":** Click the "Installations" tab.
3. **Edit Your Profile:** Select your profile and click **"More Options"**.
4. **Set the Java Executable:**
    - Locate the **"Java Executable"** field.
    - Click **"Browse"** and navigate to the installed Java binary:
        - **Windows:**  
          `C:\Program Files\Java\jre1.8.0_xxx\bin\javaw.exe`
        - **Linux:**  
          `/usr/lib/jvm/<your-java-version>/bin/java`
        - **macOS:**  
          `/Library/Java/JavaVirtualMachines/<your-java-version>/Contents/Home/bin/java`
5. **Save and Launch:** Click "Save" and start Minecraft.

### B. [Prism Launcher](https://prismlauncher.org/)
1. **Open Prism Launcher.**
2. **Select or Create an Instance:**
    - To edit an existing instance, select it.
    - Or click **"Add Instance"** to create a new one.
3. **Edit Instance Settings:**
    - Right-click the instance and select **"Edit"** (or click the pencil icon).
    - Navigate to the **"Java"** section.
4. **Set the Java Executable:**
    - Under **"Java Installation"** or **"Java Executable"**, click **"Auto-Detect"** or **"Browse"**.
    - Choose the correct Java binary:
        - **Windows:**  
          `C:\Program Files\Java\jre1.8.0_xxx\bin\javaw.exe`
        - **Linux:**  
          `/usr/lib/jvm/<your-java-version>/bin/java`
        - **macOS:**  
          `/Library/Java/JavaVirtualMachines/<your-java-version>/Contents/Home/bin/java`
5. **Save and Launch:** Save your settings and launch the instance.

For additional support, join our [Discord](https://discord.gg/skyhanni-997079228510117908).
