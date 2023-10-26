package at.hannibal2.skyhanni.features.misc.update

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.About
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.onToggle
import io.github.moulberry.moulconfig.processor.MoulConfigProcessor
import io.github.moulberry.notenoughupdates.util.MinecraftExecutor
import moe.nea.libautoupdate.CurrentVersion
import moe.nea.libautoupdate.PotentialUpdate
import moe.nea.libautoupdate.UpdateContext
import moe.nea.libautoupdate.UpdateSource
import moe.nea.libautoupdate.UpdateTarget
import net.minecraft.client.Minecraft
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.concurrent.CompletableFuture

object UpdateManager {

    private val logger = LorenzLogger("update_manager")
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

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        SkyHanniMod.feature.about.updateStream.onToggle {
            reset()
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        Minecraft.getMinecraft().thePlayer ?: return
        MinecraftForge.EVENT_BUS.unregister(this)
        if (config.autoUpdates)
            checkUpdate()
    }

    fun getCurrentVersion(): String {
        return SkyHanniMod.version
    }

    fun injectConfigProcessor(processor: MoulConfigProcessor<*>) {
        processor.registerConfigEditor(ConfigVersionDisplay::class.java) { option, _ ->
            GuiOptionEditorUpdateCheck(option)
        }
    }

    fun isBetaRelease(): Boolean {
        return getCurrentVersion().contains("beta", ignoreCase = true)
    }

    val config get() = SkyHanniMod.feature.about

    fun reset() {
        updateState = UpdateState.NONE
        _activePromise = null
        potentialUpdate = null
        logger.log("Reset update state")
    }

    fun checkUpdate() {
        if (updateState != UpdateState.NONE) {
            logger.log("Trying to perform update check while another update is already in progress")
            return
        }
        logger.log("Starting update check")
        var updateStream = config.updateStream.get()
        if (updateStream == About.UpdateStream.RELEASES && isBetaRelease()) {
            updateStream = About.UpdateStream.BETA
        }
        activePromise = context.checkUpdate(updateStream.stream)
            .thenAcceptAsync({
                logger.log("Update check completed")
                if (updateState != UpdateState.NONE) {
                    logger.log("This appears to be the second update check. Ignoring this one")
                    return@thenAcceptAsync
                }
                potentialUpdate = it
                if (it.isUpdateAvailable) {
                    updateState = UpdateState.AVAILABLE
                    LorenzUtils.clickableChat(
                        "§e[SkyHanni] §aSkyHanni found a new update: ${it.update.versionName}. " +
                            "Check §b/sh download update §afor more info.",
                        "sh"
                    )
                }
            }, MinecraftExecutor.OnThread)
    }

    fun queueUpdate() {
        if (updateState != UpdateState.AVAILABLE) {
            logger.log("Trying to enqueue an update while another one is already downloaded or none is present")
        }
        updateState = UpdateState.QUEUED
        activePromise = CompletableFuture.supplyAsync {
            logger.log("Update download started")
            potentialUpdate!!.prepareUpdate()
        }.thenAcceptAsync({
            logger.log("Update download completed, setting exit hook")
            updateState = UpdateState.DOWNLOADED
            potentialUpdate!!.executeUpdate()
        }, MinecraftExecutor.OnThread)
    }

    val context = UpdateContext(
        UpdateSource.githubUpdateSource("hannibal002", "SkyHanni"),
        UpdateTarget.deleteAndSaveInTheSameFolder(UpdateManager::class.java),
        CurrentVersion.ofTag(SkyHanniMod.version),
        SkyHanniMod.MODID,
    )

    init {
        context.cleanup()
    }

    enum class UpdateState {
        AVAILABLE,
        QUEUED,
        DOWNLOADED,
        NONE
    }

    var potentialUpdate: PotentialUpdate? = null
}
