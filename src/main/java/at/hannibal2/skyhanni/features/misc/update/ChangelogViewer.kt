package at.hannibal2.skyhanni.features.misc.update

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.jsonobjects.other.ChangelogJson
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.CommandArgument
import at.hannibal2.skyhanni.utils.CommandContextAwareObject
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.api.ApiUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.containsKeys
import at.hannibal2.skyhanni.utils.json.fromJson
import at.hannibal2.skyhanni.utils.system.ModVersion
import net.minecraft.client.Minecraft
import java.util.NavigableMap
import java.util.TreeMap

@SkyHanniModule
object ChangelogViewer {
    internal val cache: NavigableMap<ModVersion, Map<String, List<String>>> = TreeMap()

    internal var openTime = SimpleTimeMark.farPast()

    internal lateinit var startVersion: ModVersion
    internal lateinit var endVersion: ModVersion

    internal var shouldMakeNewList = false

    internal var shouldShowBeta = SkyHanniMod.isBetaVersion
    internal var showTechnicalDetails = false

    internal val primaryColor = LorenzColor.DARK_GRAY.toColor().addAlpha(218)
    internal val primary2Color = LorenzColor.DARK_GRAY.toColor().darker().addAlpha(220)

    fun showChangelog(currentVersion: String, targetVersion: String) =
        showChangelog(ModVersion.fromString(currentVersion), ModVersion.fromString(targetVersion))

    private fun showChangelog(currentVersion: ModVersion, targetVersion: ModVersion) {
        if (currentVersion > targetVersion) {
            ErrorManager.logErrorStateWithData(
                "Invalid versions for changelog",
                "current version is larger than target version",
                "current" to currentVersion,
                "target" to targetVersion,
            )
            return
        }
        startVersion = currentVersion
        endVersion = targetVersion
        if (!cache.containsKeys(startVersion, endVersion)) {
            SkyHanniMod.launchIOCoroutine { getChangelog() }
        }
        openChangelog()
    }

    private fun openChangelog() {
        if (Minecraft.getMinecraft().currentScreen !is ChangeLogViewerScreen) SkyHanniMod.screenToOpen = ChangeLogViewerScreen()
    }

    private suspend fun getChangelog() {
        val url = "https://api.github.com/repos/hannibal002/SkyHanni/releases?per_page=100&page="
        val data = mutableListOf<ChangelogJson>()
        var pageNumber = 1
        while (data.isEmpty() || ModVersion.fromString(data.last().tagName) > startVersion) {
            val pagedUrl = "$url$pageNumber"
            val (_, jsonObject) = ApiUtils.getJsonResponse(pagedUrl, apiName = "github").assertSuccessWithData()
                ?: ErrorManager.skyHanniError("Changelog Loading Failed")
            val page = ConfigManager.gson.fromJson<List<ChangelogJson>>(jsonObject)
            data.addAll(page)
            pageNumber++
        }
        val neededData = data.filter {
            val sub = ModVersion.fromString(it.tagName)
            sub.isInBetween(startVersion, endVersion)
        }
        neededData.forEach { entry ->
            cache[ModVersion.fromString(entry.tagName)] = formatData(formatString(getBasic(entry.body)))
        }
    }

    private fun formatData(text: String): Map<String, List<String>> {
        var headline = 0
        return text // Bolding markdown
            .replace("\\s*\r\n$".toRegex(), "") // Remove trailing empty Lines
            .split("\r\n") // Split at newlines
            .map { it.trimEnd() } // Remove trailing empty stuff
            .groupBy {
                if (it.startsWith("§l§9")) {
                    headline++
                }
                headline
            }
            // Change §a to §c if in removed
            .mapKeys { it.value.firstOrNull().orEmpty() }.toMutableMap().also { map ->
                val key = "§l§9Removed Features"
                val subgroup = map[key] ?: return@also
                map[key] = subgroup.map {
                    it.replace("§a", "§c")
                }
            }
    }

    private fun formatString(basic: String): String = basic.replace("\\*\\*(?<content>.*?)\\*\\*".toRegex()) {
        fun String.help(s: String): String =
            toRegex().find(basic.subSequence(0, it.range.first).reversed())?.groups?.get(s)?.value?.reversed().orEmpty()

        val format = "\n|(?<format>[kmolnrKMOLNR]§)".help("format")
        val color = "\n|(?<color>[0-9a-fA-F]§)".help("color")
        val content = it.groups["content"]?.value.orEmpty()
        "§l$content§r$format$color"
    }

    private fun getBasic(body: String): String = body.replace("[^]]\\(https://github[\\w/.?$&#]*\\)".toRegex(), "") // Remove GitHub link
        .replace("#+\\s*".toRegex(), "§l§9") // Formatting for headings
        .replace("(\n[ \t]+)[+\\-*][^+\\-*]".toRegex(), "$1§7") // Formatting for sub points
        .replace("\n[+\\-*][^+\\-*]".toRegex(), "\n§a") // Formatting for points
        .replace("(- [^-\r\n]*(?:\r\n|$))".toRegex(), "§b§l$1") // Color contributors
        .replace("\\[(.+?)\\]\\(.+?\\)".toRegex(), "$1") // Random Links
        .replace("`", "\"") // Fix Code Blocks to look better
        .replace("§l§9(?:Version|SkyHanni)[^\r\n]*\r\n".toRegex(), "") // Remove Version from Body

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerComplex("shchangelog") {
            description = "Shows the specified changelog. No arguments shows the latest changelog."
            category = CommandCategory.USERS_ACTIVE
            //#if TODO
            context = { CommandContext() }
            //#endif
            specifiers = listOf<CommandArgument<CommandContext>>(
                CommandArgument(
                    documentation = "<version> - Shows the changelog of the versions until this, " +
                        "or only that version if no since is specified.",
                    prefix = "until",
                    defaultPosition = 1,
                    handler = { argument, context ->
                        context.until = context.getModVersion(argument)
                        1
                    },
                ),
                CommandArgument(
                    documentation = "<version> - Shows the changelog of the versions since this. (Exclusive)",
                    prefix = "since",
                    defaultPosition = 0,
                    handler = { argument, context ->
                        context.since = context.getModVersion(argument)
                        1
                    },
                ),
                CommandArgument(
                    documentation = "<version> - Shows the changelog of this specific versions",
                    prefix = "show",
                    defaultPosition = -1,
                    handler = { argument, context ->
                        context.since = context.getModVersion(argument)
                        context.until = context.since
                        1
                    },
                ),
            )
        }
    }

    private fun CommandContext.getModVersion(argument: Iterable<String>): ModVersion? {
        val input = argument.first()
        val version = ModVersion.fromString(input)
        return if (!version.isValid()) {
            errorMessage =
                "'$input' is not a valid mod version. Version Syntax is: 'Major.Beta.Patch' " +
                "anything not written is assumed 0. Eg: 1.1 = 1.1.0"
            null
        } else {
            version
        }
    }

    private class CommandContext : CommandContextAwareObject {

        override var errorMessage: String? = null

        var until: ModVersion? = null
        var since: ModVersion? = null

        override fun post() {
            val since = since ?: ModVersion.fromString(SkyHanniMod.VERSION)
            val until =
                until ?: UpdateManager.getNextVersion()?.let { ModVersion.fromString(it) } ?: ModVersion.fromString(SkyHanniMod.VERSION)

            if (until < since) {
                errorMessage = "until:'$until' is less than since:'$since', where it is expected to be greater"
                return
            }
            showChangelog(since, until)
        }

    }
}
