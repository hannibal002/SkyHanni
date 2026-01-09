package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.jsonobjects.repo.LauncherEntry
import at.hannibal2.skyhanni.data.jsonobjects.repo.LaunchersJson
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import java.lang.management.ManagementFactory
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object ComputerEnvDebug {

    private var launchers: List<LauncherEntry> = listOf()
    private var genericStacks: List<String> = listOf()

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        os(event)
        launcher(event)
        ram(event)
        uptime(event)
        performanceMods(event)
    }

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val repoJson = event.getConstant<LaunchersJson>("Launchers")
        launchers = repoJson.launchers
        genericStacks = repoJson.genericStacks
    }

    private fun launcher(event: DebugDataCollectEvent) {
        event.title("Computer Minecraft Launcher")

        val firstStack = getFirstStack() ?: run {
            event.addData("Could not load data!")
            return
        }

        val launcherBrand = runCatching {
            System.getProperty("minecraft.launcher.brand")
        }.getOrNull().orEmpty()
        val (launcher, relevant) = findLauncher(firstStack, launcherBrand)

        launcher?.let {
            if (relevant) event.addData(it)
            else event.addIrrelevant(it)
            return
        }

        event.addData {
            add("Unknown launcher!")
            add("System property of 'minecraft.launcher.brand': '$launcherBrand'")
            add("firstStack: '$firstStack'")
        }
    }

    private fun findLauncher(firstStack: String, launcherBrand: String): Pair<String?, Boolean> {
        val isGeneric = genericStacks.any { firstStack.contains(it) }
        val matchingLaunchers = launchers.filter { launcher ->
            val firstStackMatch = launcher.firstStacks.any { firstStack.contains(it) }
            val brandMatch = launcher.brand.isEmpty() || launcher.brand.equals(launcherBrand, ignoreCase = true)
            (isGeneric || firstStackMatch) && brandMatch
        }
        val fallbackPair = null to true
        return when (matchingLaunchers.size) {
            0 -> fallbackPair
            1 -> matchingLaunchers.first().getIdPair()
            else -> matchingLaunchers.firstOrNull {
                it.brand.equals(launcherBrand, ignoreCase = true)
            }?.getIdPair() ?: fallbackPair
        }
    }

    private fun getFirstStack(): String? = kotlin.runCatching {
        Thread.currentThread().stackTrace.last().toString()
    }.onFailure { e ->
        ErrorManager.logErrorWithData(e, "Failed loading current thread stack trace info")
    }.getOrNull()

    private fun os(event: DebugDataCollectEvent) {
        event.title("Computer Operating System")
        val osType = OSUtils.getOperatingSystem()
        val exactName = OSUtils.getOperatingSystemRaw()
        if (osType != OSUtils.OperatingSystem.UNKNOWN) {
            event.addIrrelevant {
                add("OS type: $osType")
                add("Exact name: $exactName")
            }
        } else {
            event.addData("Unknown OS: '$exactName'")
        }
    }

    private fun ram(event: DebugDataCollectEvent) {
        event.title("Computer RAM")
        val runtime = Runtime.getRuntime()

        val text = mutableListOf<String>()

        // Retrieve memory values in bytes
        val totalMemory = runtime.totalMemory() // Total memory currently allocated to JVM
        val maxMemory = runtime.maxMemory() // Maximum memory JVM can use
        val freeMemory = runtime.freeMemory() // Free memory within currently allocated memory
        val usedMemory = totalMemory - freeMemory // Memory currently in use

        // Calculate percentages
        val allocatedPercentage = (totalMemory.toDouble() / maxMemory * 100).toInt() // Allocated percentage
        val usedPercentage = (usedMemory.toDouble() / maxMemory * 100).toInt() // Used percentage

        // Convert memory values to GB for readability
        val totalMemoryGB = totalMemory.toDouble() / (1024 * 1024 * 1024)
        val maxMemoryGB = maxMemory.toDouble() / (1024 * 1024 * 1024)
        val usedMemoryGB = usedMemory.toDouble() / (1024 * 1024 * 1024)

        // Clear the console (optional, for better readability)
        text.add("Minecraft Memory: $usedPercentage% ${usedMemoryGB.formatGB()}/${maxMemoryGB.formatGB()} GB")
        text.add("Minecraft Allocated: $allocatedPercentage% ${totalMemoryGB.formatGB()} GB")

        // Get total system memory using OS-specific APIs
        val osBean = ManagementFactory.getOperatingSystemMXBean()
        val totalPhysicalMemory = (osBean as com.sun.management.OperatingSystemMXBean).totalPhysicalMemorySize
        val freePhysicalMemory = osBean.freePhysicalMemorySize
        val usedPhysicalMemory = totalPhysicalMemory - freePhysicalMemory

        // Convert system memory to GB
        val totalPhysicalGB = totalPhysicalMemory.toDouble() / (1024 * 1024 * 1024)
        val usedPhysicalGB = usedPhysicalMemory.toDouble() / (1024 * 1024 * 1024)
        val usedPhysicalPercentage = (usedPhysicalMemory.toDouble() / totalPhysicalMemory * 100).roundToInt()

        // System Memory Usage
        text.add("System Memory: $usedPhysicalPercentage% ${usedPhysicalGB.formatGB()}/${totalPhysicalGB.formatGB()} GB")

        var important = false
        if (maxMemoryGB < 3.5) {
            text.add("")
            text.add(
                "Minecraft has less than 3.5 GB of RAM! Change this to 4-6 GB! " +
                    "(Currently at ${maxMemoryGB.formatGB()} GB RAM)",
            )
            important = true
        } else if (maxMemoryGB > 6) {
            text.add("")
            text.add(
                "Minecraft has more than 6 GB of RAM! Change this to 4-6 GB! " +
                    "(Currently at ${maxMemoryGB.formatGB()} GB RAM)",
            )
            important = true
        }
        if (usedPhysicalPercentage > 90) {
            text.add("")
            text.add(
                "The computer uses more than 90% of system memory. Maybe close background apps! " +
                    "($usedPhysicalPercentage% used)",
            )
            important = true
        }

        if (important) {
            event.addData(text)
        } else {
            event.addIrrelevant(text)
        }
    }

    private fun Double.formatGB(): String {
        return roundTo(1).addSeparators()
    }

    private fun uptime(event: DebugDataCollectEvent) {
        event.title("Minecraft Uptime")
        val uptime = getUptime()
        val info = "The game is running for ${uptime.format()}"
        if (uptime > 5.hours) {
            event.addData {
                add("The game runs for more than 5 hours, memory leaks may accumulate to dangerous levels.")
                add(info)
            }
        } else {
            event.addIrrelevant(info)
        }
    }

    private fun getUptime() = ManagementFactory.getRuntimeMXBean().uptime.milliseconds

    private fun performanceMods(event: DebugDataCollectEvent) {
        if (PlatformUtils.isDevEnvironment) return
        event.title("Performance Mods")
        val hasSodium = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("sodium")
        if (!hasSodium) {
            event.addData {
                add("Sodium is not installed")
                add("This mod greatly improve performance")
                add("https://modrinth.com/mod/sodium")
            }
        } else {
            event.addIrrelevant {
                add("Sodium is installed")
            }
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shuptime") {
            description = "Shows the time since the start of minecraft"
            category = CommandCategory.USERS_RESET
            callback {
                val uptime = getUptime()
                ChatUtils.chat("Minecraft is running for §b${uptime.format()}§e.")
            }
        }
    }
}
