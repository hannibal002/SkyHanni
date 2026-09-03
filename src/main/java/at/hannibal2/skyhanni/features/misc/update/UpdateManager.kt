package at.hannibal2.skyhanni.features.misc.update

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.SkyHanniMod.launch
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.config.features.About.UpdateStream
import at.hannibal2.skyhanni.data.NotificationManager
import at.hannibal2.skyhanni.data.SkyHanniNotification
import at.hannibal2.skyhanni.data.jsonobjects.repo.DiscontinuedMinecraftVersion
import at.hannibal2.skyhanni.data.jsonobjects.repo.DiscontinuedMinecraftVersionsJson
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.UserLuckCalculateEvent
import at.hannibal2.skyhanni.events.hypixel.HypixelJoinEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils.onToggle
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.SkyHanniLogger
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.compat.withColor
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import at.hannibal2.skyhanni.utils.system.ModVersion
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import com.google.gson.JsonPrimitive
import io.github.notenoughupdates.moulconfig.processor.MoulConfigProcessor
import net.minecraft.ChatFormatting
import net.minecraft.world.item.Items
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Job

@SkyHanniModule
object UpdateManager {

    private val logger = SkyHanniLogger("update_manager")

    private val repoReloadCoroutine = CoroutineSettings("update manager repo reload")
    private val updateCheckCoroutine = CoroutineSettings("update manager update check", timeout = 15.seconds).withIOContext()

    private var updateCheckJob: Job? = null

    var updateState: UpdateState = UpdateState.NONE
        private set

    fun getNextVersion(): String? {
        return nextUpdate?.version?.asString
    }

    @HandleEvent
    private fun onConfigLoad() {
        config.updateStream.onToggle {
            reset()
        }
        debugConfig.updateSource.whenChanged { _, new ->
            logger.log("Update source changed to $new")
            reset()
            updateSource = new.source
        }

        updateSource = debugConfig.updateSource.get().source
    }

    private var hasCheckedForUpdate = false

    @HandleEvent
    private fun onTick() {
        if (hasCheckedForUpdate) return
        checkUpdate()
    }

    fun injectConfigProcessor(processor: MoulConfigProcessor<*>) {
        processor.registerConfigEditor(ConfigVersionDisplay::class.java) { option, _ ->
            GuiOptionEditorUpdateCheck(option)
        }
        processor.registerConfigEditor(ConfigVersionDeprecatedDisplay::class.java) { option, _ ->
            GuiOptionEditorDeprecatedVersion(option)
        }
    }

    private val config get() = SkyHanniMod.feature.about
    private val debugConfig get() = SkyHanniMod.feature.dev.debug

    fun reset() {
        updateState = UpdateState.NONE
        updateCheckJob = null
        nextUpdate = null
        hasCheckedForUpdate = false
        logger.log("Reset update state")
    }

    fun checkUpdate(force: Boolean = false, forcedUpdateStream: UpdateStream = config.updateStream.get()) {
        val source = updateSource ?: return
        hasCheckedForUpdate = true

        if (updateState != UpdateState.NONE) {
            if (updateState == UpdateState.AVAILABLE && force) {
                updateState = UpdateState.NONE
                logger.log("Resetting update state to force check")
            } else {
                logger.log("Trying to perform update check while another update is already in progress")
                return
            }
        }

        logger.log("Starting update check (source: ${source.javaClass.simpleName})")

        if (forcedUpdateStream == UpdateStream.BETA && config.updateStream.get() != UpdateStream.BETA) {
            config.updateStream.set(UpdateStream.BETA)
        }

        updateCheckJob?.cancel()
        updateCheckJob = updateCheckCoroutine.launch {
            val update = source.checkUpdate(forcedUpdateStream)
            DelayedRun.runNextTick { handleUpdateCheckResult(update, force) }
        }
    }

    private fun handleUpdateCheckResult(update: UpdateData?, force: Boolean) {
        logger.log("Update check completed")
        if (updateState != UpdateState.NONE) {
            logger.log("This appears to be the second update check. Ignoring this one")
            return
        }
        nextUpdate = update
        if (update == null) return
        if (isOutdatedComparedTo(update.version)) {
            updateState = UpdateState.AVAILABLE
            ChatUtils.chat("§aSkyHanni found a new update: ${update.versionName}.")
            ChatUtils.clickableLinkChat(
                "§e§lCLICK HERE §r§eto open the download page.",
                update.downloadPage,
            )
            ChatUtils.clickableChat(
                "§e§lCLICK HERE §r§eto view changes in-game.",
                onClick = {
                    ChangelogViewer.showChangelog(SkyHanniMod.VERSION, update.version.asString)
                },
            )
        } else if (force) {
            ChatUtils.chat(
                componentBuilder {
                    append("SkyHanni didn't find a new update.")
                    withColor(ChatFormatting.GREEN)
                },
            )
        }
    }

    private fun isOutdatedComparedTo(version: ModVersion): Boolean =
        debugConfig.alwaysOutdated || SkyHanniMod.modVersion < version

    fun getDownloadPage(): String? {
        val update = nextUpdate
        if (update == null) {
            ErrorManager.logErrorStateWithData(
                "Error while getting update download information",
                "Attempted to call getDownloadPage with no update",
            )
            return null
        }
        return update.downloadPage
    }

    private var updateSource: UpdateSource? = null

    enum class UpdateState {
        AVAILABLE,
        NONE
    }

    private var nextUpdate: UpdateData? = null

    private val releaseStreamPattern = "(?i)(?:full|release)s?".toRegex()
    private val betaStreamPattern = "(?i)(?:beta|latest)s?".toRegex()

    private fun updateCommand(arg: String) {
        val currentStream = config.updateStream.get()
        val updateStream = when {
            arg.matches(releaseStreamPattern) -> UpdateStream.RELEASES
            arg.matches(betaStreamPattern) -> UpdateStream.BETA
            else -> currentStream
        }

        val switchingToBeta = updateStream == UpdateStream.BETA && (currentStream != UpdateStream.BETA || !SkyHanniMod.isBetaVersion)
        if (switchingToBeta) {
            ChatUtils.clickableChat(
                "Are you sure you want to switch to beta? These versions may be less stable.",
                onClick = {
                    if (updateStream != currentStream) {
                        config.updateStream.set(updateStream)
                    }
                    checkUpdate(true, updateStream)
                },
                "§eClick to confirm!",
                oneTimeClick = true,
            )
        } else {
            if (updateStream != currentStream) {
                config.updateStream.set(updateStream)
            }
            checkUpdate(true, updateStream)
        }
    }

    @HandleEvent
    private fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shupdate") {
            description = "Updates the mod to the specified update stream."
            category = CommandCategory.USERS_BUG_FIX
            arg("updateStream", BrigadierArguments.string()) { stream ->
                callback {
                    updateCommand(getArg(stream))
                }
            }
            callback {
                updateCommand("current")
            }
        }
    }

    var discontinuedVersions: Map<String, DiscontinuedMinecraftVersion> = mapOf()
        private set
    private var hasWarned = false

    @HandleEvent
    private fun onRepoReload(event: RepositoryReloadEvent) = repoReloadCoroutine.launch {
        discontinuedVersions = event.getConstantAsync<DiscontinuedMinecraftVersionsJson>(
            "DiscontinuedMinecraftVersions",
        ).versions.orEmpty()
    }

    @HandleEvent(HypixelJoinEvent::class)
    private fun onHypixelJoin() {
        if (hasWarned) return

        if (PlatformUtils.MC_VERSION in discontinuedVersions) {
            val extraInfo = discontinuedVersions[PlatformUtils.MC_VERSION]?.extraInfo ?: return

            val notification = SkyHanniNotification(
                listOf(
                    "§cSkyHanni is no longer receiving updates for Minecraft §e${PlatformUtils.MC_VERSION}§c.",
                    "§cPlaying on a discontinued version is not recommended and may lead to issues.",
                    "§cPlease update to a newer Minecraft version.",
                ) + extraInfo,
                Duration.INFINITE,
            )

            NotificationManager.queueNotification(notification)
        }

        hasWarned = true
    }

    @HandleEvent
    private fun onUserLuck(event: UserLuckCalculateEvent) {
        if (PlatformUtils.MC_VERSION in discontinuedVersions) {
            val luck = discontinuedVersions[PlatformUtils.MC_VERSION]?.luckAmount ?: -10f
            event.addLuck(luck)
            val stack = ItemUtils.createItemStack(
                Items.OMINOUS_BOTTLE,
                "§a✴ ${PlatformUtils.MC_VERSION} Tax",
                arrayOf(
                    "§8Minecraft",
                    "",
                    "§7Value: §c$luck§a✴",
                    "",
                    "§8${PlatformUtils.MC_VERSION} is an outdated version :(",
                    "§8You should update to a newer version :)!",
                ),
            )
            event.addItem(stack)
        } else {
            event.addLuck(5f)
            val stack = ItemUtils.createItemStack(
                Items.TRIDENT,
                "§a✴ Modern Minecraft Bonus",
                arrayOf(
                    "§8Minecraft",
                    "",
                    "§7Value: §a+5✴",
                    "",
                    "§8We put a lot of effort into updating SkyHanni.",
                    "§8This is a small bonus for using modern Minecraft.",
                ),
            )
            event.addItem(stack)
        }
    }

    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.transform(131, "about.updateStream") { element ->
            when (element.asString) {
                "NONE" -> JsonPrimitive(if (SkyHanniMod.isBetaVersion) "BETA" else "RELEASES")
                else -> element
            }
        }
        // Users who installed a beta version were previously left on the full release stream by default.
        event.transform(143, "about.updateStream") { element ->
            when {
                element.asString == "RELEASES" && SkyHanniMod.isBetaVersion -> JsonPrimitive("BETA")
                else -> element
            }
        }
    }
}
