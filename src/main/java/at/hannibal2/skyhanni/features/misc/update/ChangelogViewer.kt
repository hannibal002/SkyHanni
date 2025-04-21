package at.hannibal2.skyhanni.features.misc.update

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.jsonobjects.other.ChangelogJson
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ApiUtils
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.containsKeys
import at.hannibal2.skyhanni.utils.json.fromJson
import at.hannibal2.skyhanni.utils.system.ModVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.minecraft.client.Minecraft
import java.util.NavigableMap
import java.util.TreeMap

@SkyHanniModule
object ChangelogViewer {

    private val dispatcher = Dispatchers.IO

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
        getChangelog(currentVersion, targetVersion)
        openChangelog()
    }

    private fun openChangelog() {
        if (Minecraft.getMinecraft().currentScreen !is ChangeLogViewerScreen) SkyHanniMod.screenToOpen =
            ChangeLogViewerScreen()
    }

    private fun getChangelog(currentVersion: ModVersion, targetVersion: ModVersion) {
        startVersion = currentVersion
        endVersion = targetVersion
        if (cache.containsKeys(startVersion, endVersion)) return
        SkyHanniMod.coroutineScope.launch {
            try {
                val url = "https://api.github.com/repos/hannibal002/SkyHanni/releases?per_page=100&page="
                val data = mutableListOf<ChangelogJson>()
                var pageNumber = 1
                while (data.isEmpty() || ModVersion.fromString(data.last().tagName) > startVersion) {
                    val jsonObject = withContext(dispatcher) {
                        ApiUtils.getJSONResponseAsElement(
                            url + pageNumber, apiName = "github",
                        )
                    }
                    val page = ConfigManager.gson.fromJson<List<ChangelogJson>>(jsonObject)
                    data.addAll(page)
                    pageNumber++
                }
                val neededData = data.filter {
                    val sub = ModVersion.fromString(it.tagName)
                    sub.isInBetween(startVersion, endVersion)
                }
                neededData.forEach { entry ->
                    var headline = 0
                    cache[ModVersion.fromString(entry.tagName)] = entry.body.replace(
                        "[^]]\\(https://github[\\w/.?$&#]*\\)".toRegex(), "",
                    ) // Remove GitHub link
                        .replace("#+\\s*".toRegex(), "§l§9§r") // Formatting for headings
                        .replace("(\n[ \t]+)[+\\-*][^+\\-*]".toRegex(), "$1§7") // Formatting for sub points
                        .replace("\n[+\\-*][^+\\-*]".toRegex(), "\n§a") // Formatting for points
                        .replace("(- [^-\r\n]*\r\n)".toRegex(), "§b§l$1") // Color contributors
                        .replace("\\[(.+)\\]\\(.+\\)".toRegex(), "$1") // Random Links
                        .replace("§l§9(?:Version|SkyHanni)[^\r\n]*\r\n".toRegex(), "") // Remove Version from Body
                        .replace("(?<rest>(?<format>§[kmolnrKMOLNR])?.*?(?<color>§[0-9a-fA-F])?.*)\\*\\*(?<content>.*)\\*\\*".toRegex()) {
                            val rest = it.groups["rest"]?.value.orEmpty()
                            val format = it.groups["format"]?.value.orEmpty()
                            val color = it.groups["color"]?.value.orEmpty()
                            val content = it.groups["content"]?.value.orEmpty()
                            "$rest§l$content§r$format$color"
                        } // Bolding markdown
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
                        }.toMap()
                }
            } catch (e: Exception) {
                ErrorManager.logErrorWithData(e, "Changelog Loading Failed")
            }
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register(
            "shchangelog",
        ) {
            description = "Shows the specified changelog."
            category = CommandCategory.USERS_ACTIVE
            callback(::handelCommand)
        }
    }

    fun handelCommand(args: Array<String>) {
        when (args.size) {
            0 -> UpdateManager.getNextVersion()?.let { showChangelog(SkyHanniMod.VERSION, it) }
                ?: ChatUtils.userError(
                    "You are up to date, if you want to look at past change logs use the command " +
                        "with arguments. Usage: [version you want to look at] [your version]",
                )

            1 -> {
                val tag = ModVersion.fromString(args[0])
                if (!tag.isValid()) {
                    ChatUtils.userError("Version shape requirement failed")
                    return
                }
                val current = ModVersion.fromString(SkyHanniMod.VERSION)
                if (tag <= current) {
                    showChangelog(tag, tag)
                } else {
                    showChangelog(current, tag)
                }
            }

            2 -> {
                val target = ModVersion.fromString(args[0])
                if (!target.isValid()) {
                    ChatUtils.userError("Version shape requirement failed, first argument")
                    return
                }
                val current = ModVersion.fromString(args[1])
                if (!current.isValid()) {
                    ChatUtils.userError("Version shape requirement failed, second argument")
                    return
                }
                showChangelog(current, target)
            }

            else -> ChatUtils.userError(
                "Invalid amount of arguments. Usage: [version you want to look at] " +
                    "[your version]",
            )
        }

    }
}
