# Download and Install SkyHanni

SkyHanni is a Fabric mod for **modern Minecraft versions**.
Legacy versions of Minecraft (like 1.8.9) are **no longer supported** by SkyBlock or SkyHanni.
Follow this guide to install SkyHanni for the Fabric loader. Pick whichever launcher section matches what you use.

> **1: Install Java**
> To play with modern Fabric mods, you need a recent version of Java matching your Minecraft version (1.21.x: **Java 21**, 26.x: **Java 25**).
> We recommend [Adoptium Temurin](https://adoptium.net/temurin/releases/).
> Download and run the installer for your operating system.

---

## Option A: Prism Launcher

Prism Launcher can handle Fabric, the dependencies, RAM, and Java all from one instance setup screen, so it's the fastest option.

> **2: Install Prism Launcher**
> Download it from the [Official Prism Launcher website](https://prismlauncher.org/download/) for your OS.

> **3: Create a new instance**
> 1. Click **Add Instance**.
> 2. Select the **Minecraft version** you want (e.g. 26.1.2).
> 3. Under **Loader**, choose **Fabric** and select the latest compatible loader version.
> 4. Click **Create Instance**.

> **4: Add SkyHanni**
> 1. Right-click your new instance and select **Edit**.
> 2. Select **Mods** category and click on **Download Mods** - this searches Modrinth's catalog directly inside Prism.
> 3. Under Modrinth, search for **SkyHanni** and click it to add it straight to your instance.
> 4. You can add many other mods the same way.

> **5: Setup RAM**
> 1. Right-click the instance and select **Edit**.
> 2. Select **Settings** on the left, then click the **Java** tab.
> 3. Enable the **Memory** checkbox and set **Maximum Memory** to at least **4096 MiB** (4GB).

> **6: Launch**
> Launch the instance you just created.

---

## Option B: Vanilla Minecraft Launcher

> **2: Install Fabric Loader**
> Fabric is the system that loads your mods into the game.
> 1. Download the [Fabric Installer](https://fabricmc.net/use/installer/).
> 2. Run the installer and select the Minecraft version you want (e.g., 1.21.11 or 26.1+).
> 3. Click "Install" to create a new profile in your Minecraft Launcher.

> **3: Download SkyHanni Dependencies**
> - [Fabric API](https://modrinth.com/mod/fabric-api)
> - [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin)
> - [Hypixel Mod API](https://modrinth.com/mod/hypixel-mod-api)

> **4: Download SkyHanni**
> SkyHanni Official Downloads:
> - [GitHub](https://github.com/hannibal002/SkyHanni/releases).
> - [Modrinth](https://modrinth.com/mod/skyhanni/).


> **5: Find .minecraft**
> Once you have downloaded SkyHanni, you need to navigate to your mods folder.
> - **Windows:** Open File Explorer, type `%appdata%` in the address bar, and open `.minecraft`.
> - **macOS:** Open Finder, click "Go" at the top of your screen, then "Go to Folder", and type `~/Library/Application Support/Minecraft`.
> - **Linux:** `.minecraft` is located in your home folder: `~/.minecraft`.

> **6: Move mods into mods folder**
> 1. Open the `mods` folder inside `.minecraft`. If it doesn't exist, create it.
> 2. Move the SkyHanni `.jar` and all the dependencies' `.jar` into this folder.
> 3. Remove any older versions of SkyHanni before adding the new files.

> **7: Setup RAM**
> Modern Minecraft versions require more memory than older versions.
> 1. Open the Minecraft Launcher and go to the **Installations** tab.
> 2. Press the 3 dots next to your Fabric installation, click **Edit**, and press **More Options**.
> 3. In **JVM Arguments**, look for `-Xmx2G`.
> 4. Change it to `-Xmx4G` (4GB) or more if your computer has at least 8GB of RAM.

> **8: Launch the Fabric profile**
> Select the Fabric profile you just created in the launcher and press **Play**.

---

## Option C: Modrinth App

The Modrinth App can install Fabric and mods directly from Modrinth's own catalog.

> **2: Install the Modrinth App**
> Download it from the [Official Modrinth website](https://modrinth.com/app).

> **3: Create a new instance**
> 1. Click **Create Instance**.
> 2. Select the **Minecraft version** you want (e.g. 26.1.2).
> 3. Under **Mod Loader**, choose **Fabric** and select the latest compatible loader version.
> 4. Click **Create**.

> **4: Add SkyHanni**
> 1. Open the instance and go to the **Mods** tab.
> 2. Click **Add Content**, search for **SkyHanni**, and add it.


> **5: Setup RAM**
> 1. Click your instance to open its page, then click **Settings** on the left.
> 2. Select the **Java** tab.
> 3. Toggle **Override global settings** to ON and set **Maximum Memory Allocation** to at least **4096 MB** (4GB).

> **6: Launch**
> Click the instance, then press **Play**.

---

## For All Launchers

> **Recommended additional mods (optional)**
> These mods are highly recommended for the best experience on modern versions:
> - [Sodium](https://modrinth.com/mod/sodium): Significant performance improvements.
> - [Mod Menu](https://modrinth.com/mod/modmenu): View and configure your installed mods.

*Ask in #support on our [Discord](https://discord.gg/skyhanni-997079228510117908) if you need help with installation.*
*If you have any other questions about the mod, please read our #faq before asking in #support for help.*

> **Important note**
> Never run files sent to you via Discord or other messaging apps. Only download from official sources like Modrinth or GitHub. Be careful with GitHub, especially less popular repositories.
*This guide was last updated on July 4th, 2026.*
