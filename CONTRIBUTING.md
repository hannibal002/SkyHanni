# How to Contribute

This tutorial explains how to set up a development environment to work on SkyHanni. We
use [IntelliJ](https://www.jetbrains.com/idea/) as an example.

## Basics

SkyHanni is a Forge mod for Minecraft 1.8.9, written in [Kotlin](https://kotlinlang.org/) and [Java](https://www.java.com/en/).
We use a [gradle config](https://gradle.org/) to build the mod,
written in [Kotlin DSL](https://docs.gradle.org/current/userguide/kotlin_dsl.html).

## Setup

### Download IntelliJ

- Download IntelliJ from [JetBrains Website](https://www.jetbrains.com/idea/download/).
- Use Community Edition. (Scroll down a bit)

### Downloading SkyHanni source code

- Create an account on GitHub
    - Go to https://github.com/hannibal002/SkyHanni
    - Click on the Fork button to create a fork
        - leave the settings unchanged
        - click on "create fork"
- Open IntelliJ
    - Log in into your GitHub account with intellij
    - Go to "new" -> "project from version control"
    - Select SkyHanni from the list
    - Open the project

### Setting up IntelliJ

SkyHannis' gradle configuration is very similar to the one used in **NotEnoughUpdates**, you can just follow their
guide:
https://github.com/NotEnoughUpdates/NotEnoughUpdates/blob/master/CONTRIBUTING.md

## Additional Tools

### [Dev Auth](https://github.com/DJtheRedstoner/DevAuth)

What is Dev Auth? It is a tool that allows you to log in to your Minecraft account inside IntelliJ. That way you can
debug and test code live on Hypixel, without the need to compile a jar, move it into a mods folder and start the
Minecraft launcher manually.

- Gradle already downloads dev auth for you.
- You only need to create a configuration folder:
    - Navigate to `C:\Users\<your username>`
    - Create a new folder `.devauth`
    - Naviga to `C:\Users\<your username>\.devauth`
    - Create a new file `config.toml`
    - Paste this text in the file: (Don't change anyhting)

```
defaultEnabled = true

defaultAccount = "main"

[accounts.main]
type = "microsoft"
```

- Start Minecraft inside IntelliJ normal.
    - You should see a link in the console.
    - Click on the link, verify yourself with the mojang account.
    - You are done. The verify process will reappear every couple of days (as your session token does expire)

### Hot Swapping

What is Hot Swapping? This allows you to reload edited code while debugging to remove the need to restart
the whole game every time.
We [dcevm](https://dcevm.github.io/) to hot swap SkyHanni. 

Follow this tutorial:

https://forums.Minecraftforge.net/topic/82228-1152-3110-intellij-and-gradlew-forge-hotswap-and-dcevm-tutorial/
