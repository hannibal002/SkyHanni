package at.hannibal2.skyhanni.features.misc.update

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.data.jsonobjects.other.ChangelogJson
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.APIUtil
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.CollectionUtils.containsKeys
import at.hannibal2.skyhanni.utils.CollectionUtils.getOrNull
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.ColorUtils.withAlpha
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.isInt
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.json.fromJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.minecraft.client.Minecraft
import java.util.NavigableMap
import java.util.TreeMap

object ChangelogViewer {

    private var dispatcher = Dispatchers.IO

    internal val cache: NavigableMap<VersionTag, Map<String, List<String>>> = TreeMap()

    internal var openTime = SimpleTimeMark.farPast()

    internal lateinit var startVersion: VersionTag
    internal lateinit var endVersion: VersionTag

    internal var shouldMakeNewList = false

    internal var shouldShowBeta = LorenzUtils.isBetaVersion()
    internal var showTechnicalDetails = false

    internal val primaryColor = LorenzColor.DARK_GRAY.toColor().addAlpha(218)
    internal val primary2Color = LorenzColor.DARK_GRAY.toColor().darker().withAlpha(220)

    fun showChangelog(currentVersion: String, targetVersion: String) =
        showChangelog(currentVersion.toVersionTag(), targetVersion.toVersionTag())

    private fun showChangelog(currentVersion: VersionTag, targetVersion: VersionTag) {
        if (currentVersion > targetVersion) {
            ErrorManager.logErrorStateWithData(
                "Invalid versions for changelog",
                "current version is larger than target version",
                "current" to currentVersion,
                "target" to targetVersion
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

    internal data class VersionTag(
        val first: Int,
        val second: Int,
        val third: Int,
        val fourth: Int,
        val isBeta: Boolean,
    ) : Comparable<VersionTag> {

        constructor(l: List<Int>, beta: Boolean) : this(
            l.getOrNull(0) ?: -1, l.getOrNull(1) ?: -1, l.getOrNull(2) ?: -1, l.getOrNull(3) ?: -1, beta
        )

        override operator fun compareTo(other: VersionTag): Int {
            val first = first.compareTo(other.first)
            if (first != 0) return first
            val second = second.compareTo(other.second)
            if (second != 0) return second
            val beta = -isBeta.compareTo(other.isBeta)
            if (beta != 0) return beta
            val third = third.compareTo(other.third)
            if (third != 0) return third
            return fourth.compareTo(other.fourth)
        }

        override fun toString(): String {
            return if (isBeta) {
                "$first" + if (second == -1) " Beta" else ".$second" + if (third == -1) " Beta" else " Beta $third" + if (fourth == -1) "" else ".$fourth"
            } else {
                "$first" + if (second == -1) "" else ".$second" + if (third == -1) "" else ".$third" + if (fourth == -1) "" else ".$fourth"
            }
        }

        fun isValid() = first != -1
    }

    /** Inclusive for both borders */
    private fun VersionTag.isInBetween(current: VersionTag, target: VersionTag): Boolean {
        if (this > target) return false
        if (this < current) return false
        if (this == current) return true
        return true
    }

    private fun String.toVersionTag(): VersionTag {
        val split = this.split('.')
        val ints = split.filter { it.isInt() }.map { it.toInt() }
        return VersionTag(ints, split.contains("Beta"))
    }

    private fun getChangelog(currentVersion: VersionTag, targetVersion: VersionTag) {
        startVersion = currentVersion
        endVersion = targetVersion
        if (cache.containsKeys(startVersion, endVersion)) return
        SkyHanniMod.coroutineScope.launch {
            try {
                val url = "https://api.github.com/repos/hannibal002/SkyHanni/releases?per_page=100&page="
                val data = mutableListOf<ChangelogJson>()
                var pageNumber = 1
                while (data.isEmpty() || data.last().tagName.toVersionTag() > startVersion) {
                    val jsonObject = withContext(dispatcher) {
                        APIUtil.getJSONResponseAsElement(
                            url + pageNumber, apiName = "github"
                        )
                    }
                    val page = ConfigManager.gson.fromJson<List<ChangelogJson>>(jsonObject)
                    data.addAll(page)
                    pageNumber++
                }
                val neededData = data.filter {
                    val sub = it.tagName.toVersionTag()
                    sub.isInBetween(startVersion, endVersion)
                }
                neededData.forEach {
                    var headline = 0
                    cache[it.tagName.toVersionTag()] = it.body.replace(
                        "[^]]\\(https://github[\\w/.?$&#]*\\)".toRegex(), ""
                    ) // Remove GitHub link
                        .replace("#+\\s*".toRegex(), "§l§9") // Formatting for headings
                        .replace("(\n[ \t]+)[+\\-*][^+\\-*]".toRegex(), "$1§7") // Formatting for sub points
                        .replace("\n[+\\-*][^+\\-*]".toRegex(), "\n§a") // Formatting for points
                        .replace("(- [^-\r\n]*\r\n)".toRegex(), "§b$1") // Color contributors
                        .replace("\\[(.+)\\]\\(.+\\)".toRegex(), "$1") // Random Links
                        .replace("§l§9(?:Version|SkyHanni)[^\r\n]*\r\n".toRegex(), "") // Remove Version from Body
                        .replace("(?<rest>(?<format>§[kmolnrKMOLNR])?.*?(?<colour>§[0-9a-fA-F])?.*)\\*\\*(?<content>.*)\\*\\*".toRegex()) {
                            val rest = it.groups["rest"]?.value ?: ""
                            val foramt = it.groups["format"]?.value ?: ""
                            val colour = it.groups["colour"]?.value ?: ""
                            val content = it.groups["content"]?.value ?: ""
                            "$rest§l$content§r$foramt$colour"
                        } // Bolding markdown
                        .replace("\\s*\r\n$".toRegex(), "") // Remove trailing empty Lines
                        .split("\r\n") // Split at newlines
                        .map { it.trimEnd() } // Remove trailing empty stuff
                        .groupBy {
                            if (it.startsWith("§l§9")) {
                                headline++
                            }
                            headline
                        }.mapKeys { it.value.firstOrNull() ?: "" }.toMutableMap().also {// Change §a to §c if in removed
                            val key = "§l§9Removed Features"
                            val subgroup = it[key] ?: return@also
                            it[key] = subgroup.map {
                                it.replace("§a", "§c")
                            }
                        }.toMap()
                }
            } catch (e: Exception) {
                ErrorManager.logErrorWithData(e, "Changelog Loading Failed")
            }
        }
    }

    fun handelCommand(args: Array<String>) {
        when (args.size) {
            0 -> UpdateManager.getNextVersion()?.let { showChangelog(SkyHanniMod.version, it) }
                ?: ChatUtils.userError("You are up to date, if you want to look at past change logs use the command with arguments. Usage: [version you want to look at] [your version]")

            1 -> {
                val tag = args[0].toVersionTag()
                if (!tag.isValid()) {
                    ChatUtils.userError("Version shape requirement failed")
                    return
                }
                val current = SkyHanniMod.version.toVersionTag()
                if (tag <= current) {
                    showChangelog(tag, tag)
                } else {
                    showChangelog(current, tag)
                }
            }

            2 -> {
                val target = args[0].toVersionTag()
                if (!target.isValid()) {
                    ChatUtils.userError("Version shape requirement failed, first argument")
                    return
                }
                val current = args[1].toVersionTag()
                if (!current.isValid()) {
                    ChatUtils.userError("Version shape requirement failed, second argument")
                    return
                }
                showChangelog(current, target)
            }

            else -> ChatUtils.userError("Invalid amount of arguments. Usage: [version you want to look at] [your version]")
        }

    }
}
