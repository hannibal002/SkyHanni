# How to Contribute

This is a technical guide that helps Kotlin and Java developers to understand how SkyHanni works and provide first steps for new forge developers.

# Setup the developement enviroment

When making changed to the code, it is recommended to use an IDE for live debugging and testing.
This tutorial explains how to set up the developement enviroment for SkyHanni.
We use [IntelliJ](https://www.jetbrains.com/idea/) as an example.

## Download IntelliJ

- Download IntelliJ from [JetBrains Website](https://www.jetbrains.com/idea/download/).
    - Use Community Edition. (Scroll down a bit)

## Cloning the project

- Create an account on GitHub
    - Go to https://github.com/hannibal002/SkyHanni
    - Click on the Fork button to create a fork
        - Leave the settings unchanged
        - Click on `create fork`
    - Open IntelliJ
        - Link the GitHub account with IntelliJ.
        - Install Git in IntelliJ.
        - In IntelliJ, go to `new` -> `project from version control`.
        - Select `SkyHanni` from the list.
        - Open the project.

## Setting up IntelliJ

SkyHannis' gradle configuration is very similar to the one used in **NotEnoughUpdates**, just follow this guide:
https://github.com/NotEnoughUpdates/NotEnoughUpdates/blob/master/CONTRIBUTING.md

# Software Used in SkyHanni

## Core

SkyHanni is a Forge mod for Minecraft 1.8.9, written in [Kotlin](https://kotlinlang.org/)
and [Java](https://www.java.com/en/).

We use a [gradle config](https://gradle.org/) to build the mod,
written in [Kotlin DSL](https://docs.gradle.org/current/userguide/kotlin_dsl.html):
[build.gradle.kts](https://github.com/hannibal002/SkyHanni/blob/beta/build.gradle.kts)

This start script will download all required libraries automatic.

## NotEnoughUpdates

SkyHanni requires NEU.
We use NEU to get auction house and bazaar price data for items, and to read
the [NEU Item Repo](https://github.com/NotEnoughUpdates/NotEnoughUpdates-REPO) for recipe and basic

For more info see https://github.com/NotEnoughUpdates/NotEnoughUpdates

## Config

Skyhanni uses the config system from NEU.

For more info see https://github.com/NotEnoughUpdates/MoulConfig

## Mixin

A system to inject code into original minecraft code.
This library is not part of SkyHanni itself, it comes preinstalled with forge.

For more info see https://github.com/SpongePowered/Mixin.

## Repo

SkyHanni uses a repo system to easily change static variables without the need for a mod update.
The repo is located at https://github.com/hannibal002/SkyHanni-REPO.
A copy of all files in the repo is stored for every SkyHanni user under `.minecraft\config\skyhanni\repo`.
On every game start, the copy gets updated (if outdated, and if not manually disabled)
When working with the repo, it is recommended to disable the manual repo update to prevent to override your local
changes by acident.

# Coding Styles and Conventions

- Follow the [Hypixel Rules](https://hypixel.net/rules).
- Use the coding conventions for [Kotlin](https://kotlinlang.org/docs/coding-conventions.html)
  and [Java](https://www.oracle.com/java/technologies/javase/codeconventions-contents.html).
- Do not copy features from other mods. Exceptions:
    - Paid only mods.
    - Mods that have reached the end of life. (Rip SBA, Dulkir, Soopy)
    - The mod has, according to hypixel rules, illegal features ("cheat mod").
    - If you can improve the existing feature meaningful.
- All classes should be written in Kotlin, with few exceptions:
    - Config files in `at.hannibal2.skyhanni.config.features`
    - Mixin classes in `at.hannibal2.skyhanni.mixins.transformers`
    - Java classes that represent json data objects in `at.hannibal2.skyhanni.utils.jsonobjects`
- Please use the existing event system, or expand on it. Do not use forge events.
    - (We inject the calls with mixins)
- Please use existing utils methods.
- We try to avoid calling NEU code too often.
    - (We plan to remove NEU as dependency in the future)
- We try to not use forge specific methods if possible
    - (We plan to switch to gradle and minecraft 1.20 in the future)

# Additional Useful Developement Tools

## DevAuth

[DevAuth](https://github.com/DJtheRedstoner/DevAuth) is a tool that allows logging in a Minecraft account
whilde debugging in IntelliJ. This is very useful for coding live on Hypixel, without the need to compile a jar, move it
into a mods folder, and start the
Minecraft launcher manually.

- The library gets downloaded by Gradle already.
- Create a configuration folder: (Windows only. For other OS, use the guide from DJtheRedstoner)
    - Navigate to `C:\Users\<your username>`
    - Create a new folder `.devauth`
    - Navigate to `C:\Users\<your username>\.devauth`
    - Create a new file `config.toml`
    - Paste this text in the file: (Don't change anyhting)

```
defaultEnabled = true

defaultAccount = "main"

[accounts.main]
type = "microsoft"
```

- Start Minecraft inside IntelliJ normal.
    - Click on the link in the console, verify with a mojang account.
    - The verify process will reappear every few of days (The session token expire)

## Hot Swap

Hot Swap allows to reload edited code while debugging to remove the need to restart
the whole game every time.
We use [dcevm](https://dcevm.github.io/) and the IntelliJ
Plugin [HotSwap Agent](https://plugins.jetbrains.com/plugin/9552-hotswapagent) to quickly reload code changes.

Follow [this](https://forums.Minecraftforge.net/topic/82228-1152-3110-intellij-and-gradlew-forge-hotswap-and-dcevm-tutorial/)
tutorial.