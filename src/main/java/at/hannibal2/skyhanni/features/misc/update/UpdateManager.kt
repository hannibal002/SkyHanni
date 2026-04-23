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
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.SkyHanniLogger
import at.hannibal2.skyhanni.utils.api.ApiInternalUtils
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.compat.withColor
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import at.hannibal2.skyhanni.utils.system.ModVersion
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import io.github.notenoughupdates.moulconfig.processor.MoulConfigProcessor
import moe.nea.libautoupdate.CurrentVersion
import moe.nea.libautoupdate.GithubReleaseUpdateData
import moe.nea.libautoupdate.PotentialUpdate
import moe.nea.libautoupdate.UpdateContext
import moe.nea.libautoupdate.UpdateUtils
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.world.item.Items
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.net.ssl.HttpsURLConnection
import kotlin.time.Duration

@SkyHanniModule
object UpdateManager {

    private val logger = SkyHanniLogger("update_manager")

    private val repoReloadCoroutine = CoroutineSettings("update manager repo reload")

    private var _activePromise: CompletableFuture<*>? = null
    private var activePromise: CompletableFuture<*>?
        get() = _activePromise
        set(value) {
            _activePromise?.cancel(true)
            _activePromise = value
        }

    var updateState: UpdateState = UpdateState.NONE
        private set

    fun getNextVersion(): String? {
        return potentialUpdate?.update?.versionNumber?.asString
    }

    @HandleEvent
    fun onConfigLoad() {
        config.updateStream.onToggle {
            reset()
        }
        debugConfig.updateSource.whenChanged { _, new ->
            logger.log("Update source changed to $new")
            reset()
            refreshContext(new)
        }

        refreshContext(debugConfig.updateSource.get())
    }

    private var hasCheckedForUpdate = false

    @HandleEvent
    fun onTick() {
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
        _activePromise = null
        potentialUpdate = null
        hasCheckedForUpdate = false
        logger.log("Reset update state")
    }

    fun checkUpdate(force: Boolean = false, forcedUpdateStream: UpdateStream = config.updateStream.get()) {
        val context = updateContext ?: return
        hasCheckedForUpdate = true

        var updateStream = forcedUpdateStream
        if (updateState != UpdateState.NONE) {
            if (updateState == UpdateState.AVAILABLE && force) {
                updateState = UpdateState.NONE
                logger.log("Resetting update state to force check")
            } else {
                logger.log("Trying to perform update check while another update is already in progress")
                return
            }
        }

        logger.log("Starting update check (source: ${context.source.javaClass.simpleName}")

        val currentStream = config.updateStream.get()
        if (currentStream != UpdateStream.BETA && (updateStream == UpdateStream.BETA || SkyHanniMod.isBetaVersion)) {
            config.updateStream.set(UpdateStream.BETA)
            updateStream = UpdateStream.BETA
        }

        activePromise = context.checkUpdate(updateStream.stream)
            .orTimeout(15, TimeUnit.SECONDS)
            .whenCompleteAsync(
                { update, throwable ->
                    if (throwable != null) {
                        if (throwable is TimeoutException) {
                            ErrorManager.logErrorWithData(throwable, "Update check timed out")
                        } else {
                            ErrorManager.logErrorWithData(throwable, "Update check failed")
                        }
                        return@whenCompleteAsync
                    }
                    logger.log("Update check completed")
                    if (updateState != UpdateState.NONE) {
                        logger.log("This appears to be the second update check. Ignoring this one")
                        return@whenCompleteAsync
                    }
                    potentialUpdate = update
                    if (update.isUpdateAvailable) {
                        updateState = UpdateState.AVAILABLE
                        ChatUtils.chat("§aSkyHanni found a new update: ${update.update.versionName}.")
                        getDownloadPage(update)?.let { url ->
                            ChatUtils.clickableLinkChat(
                                "§e§lCLICK HERE §r§eto open the download page.",
                                url,
                            )
                        }
                        ChatUtils.clickableChat(
                            "§e§lCLICK HERE §r§eto view changes in-game.",
                            onClick = {
                                ChangelogViewer.showChangelog(SkyHanniMod.VERSION, update.update.versionName)
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
                },
                Minecraft.getInstance(),
            )
    }

    fun getDownloadPage(update: PotentialUpdate? = potentialUpdate): String? {
        if (update == null) {
            ErrorManager.logErrorWithData(
                IllegalStateException("Attempted to call getDownloadPage with no potentialUpdate"),
                "Error while getting update download information",
            )
            return null
        }
        return when (val data = update.update) {
            is ModrinthUpdateData -> data.htmlUrl
            is GithubReleaseUpdateData -> data.htmlUrl
            else -> {
                ErrorManager.logErrorWithData(
                    IllegalStateException("Unsupported update data type"),
                    "Error while getting update download information",
                    "updateData" to data,
                )
                null
            }
        }
    }

    private fun buildContext(updateSource: SkyHanniUpdateSource) = UpdateContext(
        updateSource.source,
        NoOpUpdateTarget,
        object : CurrentVersion {
            private val debug get() = debugConfig.alwaysOutdated
            override fun display(): String = if (debug) "Force Outdated" else SkyHanniMod.VERSION

            override fun isOlderThan(element: JsonElement?): Boolean {
                if (debug) return true
                val asString = element?.asString ?: return true
                val otherVersion = ModVersion.fromString(asString)
                return SkyHanniMod.modVersion < otherVersion
            }
        },
        SkyHanniMod.MODID,
    )

    private fun refreshContext(updateSource: SkyHanniUpdateSource) {
        val newContext = buildContext(updateSource)
        newContext.cleanup()
        updateContext = newContext
    }

    private var updateContext: UpdateContext? = null

    init {
        UpdateUtils.patchConnection {
            it.setRequestProperty("User-Agent", SkyHanniMod.userAgent)
            if (it is HttpsURLConnection) {
                ApiInternalUtils.patchHttpsRequest(it)
            }
        }
    }

    enum class UpdateState {
        AVAILABLE,
        NONE
    }

    private var potentialUpdate: PotentialUpdate? = null

    private fun updateCommand(arg: String) {
        val currentStream = SkyHanniMod.feature.about.updateStream.get()
        val updateStream = when {
            arg.equals("(?i)(?:full|release)s?".toRegex()) -> UpdateStream.RELEASES
            arg.equals("(?i)(?:beta|latest)s?".toRegex()) -> UpdateStream.BETA
            else -> currentStream
        }

        val switchingToBeta = updateStream == UpdateStream.BETA && (currentStream != UpdateStream.BETA || !SkyHanniMod.isBetaVersion)
        if (switchingToBeta) {
            ChatUtils.clickableChat(
                "Are you sure you want to switch to beta? These versions may be less stable.",
                onClick = {
                    val newUpdateStream = SkyHanniMod.feature.about.updateStream
                    newUpdateStream.set(UpdateStream.BETA)
                    checkUpdate(true, newUpdateStream.get())
                },
                "§eClick to confirm!",
                oneTimeClick = true,
            )
        } else {
            checkUpdate(true, updateStream)
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
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
    fun onRepoReload(event: RepositoryReloadEvent) = repoReloadCoroutine.launch {
        discontinuedVersions = event.getConstantAsync<DiscontinuedMinecraftVersionsJson>(
            "DiscontinuedMinecraftVersions",
        ).versions.orEmpty()
    }

    @HandleEvent(HypixelJoinEvent::class)
    fun onHypixelJoin() {
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
    fun onUserLuck(event: UserLuckCalculateEvent) {
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
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.transform(131, "about.updateStream") { element ->
            when (element.asString) {
                "NONE" -> JsonPrimitive(if (SkyHanniMod.isBetaVersion) "BETA" else "RELEASES")
                else -> element
            }
        }
    }
}
