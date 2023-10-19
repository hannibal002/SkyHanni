# How to Contribute

This is a technical guide that helps Kotlin and Java developers understand how SkyHanni works, and provides the first
steps for new Forge developers to take.

## Development Environment Setup

When making changes to the code, it is recommended to use an IDE for live debugging and testing.
This tutorial explains how to set up the development environment for SkyHanni.
We use [IntelliJ](https://www.jetbrains.com/idea/) as an example.

### Download IntelliJ

- Download IntelliJ from the [JetBrains Website](https://www.jetbrains.com/idea/download/).
    - Use the Community Edition. (Scroll down a bit.)

### Cloning the project

- Create an account on GitHub
    - Go to https://github.com/hannibal002/SkyHanni
    - Click on the fork button to create a fork.
        - Leave the settings unchanged
        - Click on `create fork`
    - Open IntelliJ
        - Link the GitHub account with IntelliJ.
        - Install Git in IntelliJ.
        - In IntelliJ, go to `new` → `project from version control`.
        - Select `SkyHanni` from the list.
        - Open the project.

### Setting up IntelliJ

SkyHanni's Gradle configuration is very similar to the one used in **NotEnoughUpdates**, just
follow [their guide](https://github.com/NotEnoughUpdates/NotEnoughUpdates/blob/master/CONTRIBUTING.md).

## Creating a Pull Request

If you are not very familiar with git, you might want to try this out: https://learngitbranching.js.org/.

_An explanation how to use intellij and branches will follow here soon.

Please use a prefix for the name of the PR (E.g. Feature, Fix, Backend, Change).

You can write in the description of the pr the wording for the changelog as well (optional).

## Coding Styles and Conventions

- Follow the [Hypixel Rules](https://hypixel.net/rules).
- Use the coding conventions for [Kotlin](https://kotlinlang.org/docs/coding-conventions.html)
  and [Java](https://www.oracle.com/java/technologies/javase/codeconventions-contents.html).
- Do not copy features from other mods. Exceptions:
    - Mods that are paid to use.
    - Mods that have reached their end of life. (Rip SBA, Dulkir and Soopy)
    - The mod has, according to Hypixel rules, illegal features ("cheat mod/client").
    - If you can improve the existing feature in a meaningful way.
- All new classes should be written in Kotlin, with a few exceptions:
    - Config files in `at.hannibal2.skyhanni.config.features`
    - Mixin classes in `at.hannibal2.skyhanni.mixins.transformers`
    - Java classes that represent JSON data objects in `at.hannibal2.skyhanni.utils.jsonobjects`
- Please use the existing event system, or expand on it. Do not use Forge events.
    - (We inject the calls with Mixin)
- Please use existing utils methods.
- We try to avoid calling the NEU code too often.
    - (We plan to remove NEU as a dependency in the future.)
- We try not to use Forge-specific methods if possible.
    - (We plan to switch to Fabric and Minecraft 1.20 in the future.)
- Please try to avoid using `System.currentTimeMillis()`. Use our own class `SimpleTimeMark` instead.
    - See [this commit](https://github.com/hannibal002/SkyHanni/commit/3d748cb79f3a1afa7f1a9b7d0561e5d7bb284a9b)
      as an example.
- Try to avoid using kotlin's `!!` (catch if not null) feature.
    - Replace it with `?:` (ff null return this).
    - This will most likely not be possible to avoid when working with obects from java.
- Don't forget to add `@FeatureToggle` to new standalone features (not options to that feature) in the config.
- Do not use `e.printStackTrace()`, use `CopyErrorCommand.logError(e, "explanation for users")` instead.
- Do not use `MinecraftForge.EVENT_BUS.post(event)`, use `event.postAndCatch()` instead.
- Do not use wildcards in imports (see the imgae below for setting this up in IntelliJ)
- ![image](https://github.com/hannibal002/SkyHanni/assets/24389977/84c3a640-b39a-407e-a73c-12e548f33e88)


## Software Used in SkyHanni

### Basics

SkyHanni is a Forge mod for Minecraft 1.8.9, written in [Kotlin](https://kotlinlang.org/)
and [Java](https://www.java.com/en/).

We use a [Gradle configuration](https://gradle.org/) to build the mod,
written in [Kotlin DSL](https://docs.gradle.org/current/userguide/kotlin_dsl.html):
[build.gradle.kts](https://github.com/hannibal002/SkyHanni/blob/beta/build.gradle.kts)

This start script will automatically download all required libraries.

### NotEnoughUpdates

SkyHanni requires NEU.
We use NEU to get auction house and bazaar price data for items and to read
the [NEU Item Repo](https://github.com/NotEnoughUpdates/NotEnoughUpdates-REPO) for item internal names, display names
and recipes.

For more information, see https://github.com/NotEnoughUpdates/NotEnoughUpdates

### Config

SkyHanni stores the config (settings and user data) as a json object in a single text file.
For rendering the /sh config (categories, toggles, search, etc.),
SkyHanni uses **MoulConfig**, the same config system as NotEnoughUpdates.

For more information, see https://github.com/NotEnoughUpdates/MoulConfig

### Elite Farmers API

SkyHanni utilizes the [Elite API](https://api.elitebot.dev/) (view the [public site here](https://elitebot.dev)) for
some farming features.

This includes features relating to Farming Weight, as well as syncing jacob contests amongst players for conviencience.
All data sent is anonymonized and opt-in.

### Mixin

A system to inject code into the original Minecraft code.
This library is not part of SkyHanni itself; it comes preinstalled with Forge.

For more information, see https://github.com/SpongePowered/Mixin.

### Repo

SkyHanni uses a repo system to easily change static variables without the need for a mod update.
The repo is located at https://github.com/hannibal002/SkyHanni-REPO.
A copy of all json files is stored on the computer under `.minecraft\config\skyhanni\repo`.
On every game start, the copy gets updated (if outdated and if not manually disabled).
If you add stuff to the repo make sure it gets serialised. See the [jsonobjects](src/main/java/at/hannibal2/skyhanni/utils/jsonobjects)
folder for how to properly do this. You also may have to disable repo auto update in game.

### Discord IPC

DiscordIPC is a service that SkyHanni uses to send information from SkyBlock to Discord in Rich Presence. <br>
Specifically, we use [TirelessTraveler's Fork](https://github.com/ILikePlayingGames/DiscordIPC) of a fork of a fork of
the [original](https://github.com/jagrosh/DiscordIPC).
For info on usage, look
at [DiscordRPCManager.kt](https://github.com/hannibal002/SkyHanni/blob/beta/src/main/java/at/hannibal2/skyhanni/features/misc/discordrpc/DiscordRPCManager.kt)

### Auto Updater

We use the [auto update library](https://repo.nea.moe/#/releases/moe/nea/libautoupdate) from nea.

## Additional Useful Developement Tools

### DevAuth

[DevAuth](https://github.com/DJtheRedstoner/DevAuth) is a tool that allows logging in to a Minecraft account while
debugging in IntelliJ. This is very useful for coding live on Hypixel without the need to compile a jar, put it into the
`mods` folder, and start the Minecraft launcher manually.

- The library is already downloaded by Gradle.
- Create the config folder (Windows only). For other OSes, use the guide from DJtheRedstoner.
    - Navigate to `C:\Users\<your username>`
    - Create a new folder `.devauth`
    - Navigate to `C:\Users\<your username>\.devauth`
    - Create a new file `config.toml`
    - Paste this text into the file: (Don't change anything.)

```
defaultEnabled = true

defaultAccount = "main"

[accounts.main]
type = "microsoft"
```

- Start Minecraft inside IntelliJ normally.
    - Click on the link in the console and verify with a Mojang account.
    - The verification process will reappear every few days (after the session token expires).

### Hot Swap

Hot Swap allows reloading edited code while debugging, removing the need to restart the whole game every time.

We use [dcevm](https://dcevm.github.io/) and the IntelliJ
Plugin [HotSwap Agent](https://plugins.jetbrains.com/plugin/9552-hotswapagent) to quickly reload code changes.

Follow [this](https://forums.Minecraftforge.net/topic/82228-1152-3110-intellij-and-gradlew-forge-hotswap-and-dcevm-tutorial/)
tutorial.
