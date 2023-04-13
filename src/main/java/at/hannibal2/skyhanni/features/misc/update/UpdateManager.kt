package at.hannibal2.skyhanni.features.misc.update

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.About
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import io.github.moulberry.moulconfig.processor.MoulConfigProcessor
import io.github.moulberry.notenoughupdates.util.MinecraftExecutor
import moe.nea.libautoupdate.*
import net.minecraft.client.Minecraft
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.util.concurrent.CompletableFuture

object UpdateManager {

    private val logger = SkyHanniMod.getLogger("UpdateManager")
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
        SkyHanniMod.feature.about.updateStream.whenChanged { oldValue, newValue ->
            if (oldValue != newValue)
                reset()
        }
    }

    @SubscribeEvent
    fun onPlayerAvailableOnce(event: TickEvent.ClientTickEvent) {
        val p = Minecraft.getMinecraft().thePlayer ?: return
        MinecraftForge.EVENT_BUS.unregister(this)
        if (config.autoUpdates)
            checkUpdate()
    }

    fun getCurrentVersion(): String {
        return SkyHanniMod.getVersion()
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
        logger.info("Reset update state")
    }

    fun checkUpdate() {
        if (updateState != UpdateState.NONE) {
            logger.error("Trying to perform update check while another update is already in progress")
            return
        }
        logger.info("Starting update check")
        var updateStream = config.updateStream.get()
        if (updateStream == About.UpdateStream.RELEASES && isBetaRelease()) {
            updateStream = About.UpdateStream.BETA
        }
        activePromise = context.checkUpdate(updateStream.stream)
            .thenAcceptAsync({
                logger.info("Update check completed")
                if (updateState != UpdateState.NONE) {
                    logger.warn("This appears to be the second update check. Ignoring this one")
                    return@thenAcceptAsync
                }
                potentialUpdate = it
                if (it.isUpdateAvailable) {
                    updateState = UpdateState.AVAILABLE
                    LorenzUtils.clickableChat("§e[SkyHanni] §aSkyhanni found a new update: ${it.update.versionName}. Go check §b/sh download update §afor more info.", "sh")
                }
            }, MinecraftExecutor.OnThread)
    }

    fun queueUpdate() {
        if (updateState != UpdateState.AVAILABLE) {
            logger.error("Trying to enqueue an update while another one is already downloaded or none is present")
        }
        updateState = UpdateState.QUEUED
        activePromise = CompletableFuture.supplyAsync {
            logger.info("Update download started")
            potentialUpdate!!.prepareUpdate()
        }.thenAcceptAsync({
            logger.info("Update download completed, setting exit hook")
            updateState = UpdateState.DOWNLOADED
            potentialUpdate!!.executeUpdate()
        }, MinecraftExecutor.OnThread)
    }

    val context = UpdateContext(
        UpdateSource.githubUpdateSource("hannibal002", "Skyhanni"),
        UpdateTarget.deleteAndSaveInTheSameFolder(UpdateManager::class.java),
        CurrentVersion.ofTag(SkyHanniMod.getVersion()),
        SkyHanniMod.MODID,
    )

    enum class UpdateState {
        AVAILABLE,
        QUEUED,
        DOWNLOADED,
        NONE
    }

    var potentialUpdate: PotentialUpdate? = null
}