package at.hannibal2.skyhanni.features.rift.area.westvillage

import at.hannibal2.skyhanni.data.jsonobjects.repo.ParkourJson
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.features.rift.RiftAPI
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.ParkourHelper
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object RiftRaceHelper {

    private val config get() = RiftAPI.config.area.westVillage.riftRace
    private var parkourHelper: ParkourHelper? = null

    private val patternGroup = RepoPattern.group("rift.area.westvillage.riftrace")
    /**
     * REGEX-TEST: §3§lRIFT RACING §r§eRace finished in 00:36.539!
     */
    private val raceFinishedPattern by patternGroup.pattern(
        "finish",
        "§3§lRIFT RACING §r§eRace finished in \\d+:\\d+.\\d+!"
    )

    @SubscribeEvent
    fun onIslandChange(event: IslandChangeEvent) {
        parkourHelper?.reset()
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        RiftAPI.inRiftRace = false
    }

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<ParkourJson>("RiftRace")
        parkourHelper = ParkourHelper(
            data.locations,
            data.shortCuts,
            detectionRange = 5.0,
            goInOrder = true,
        )
        updateConfig()
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        ConditionalUtils.onToggle(config.rainbowColor, config.monochromeColor, config.lookAhead) {
            updateConfig()
        }
    }

    private fun updateConfig() {
        parkourHelper?.run {
            rainbowColor = config.rainbowColor.get()
            monochromeColor = config.monochromeColor.get().toChromaColor()
            lookAhead = config.lookAhead.get() + 1
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return

        if (event.message == "§3§lRIFT RACING §r§eRace started! Good luck!") {
            RiftAPI.inRiftRace = true
        }
        if (event.message.startsWith("§3§lRIFT RACING §r§cRace cancelled!")) {
            parkourHelper?.reset()
            RiftAPI.inRiftRace = false
        }
        raceFinishedPattern.matchMatcher(event.message) {
            parkourHelper?.reset()
            RiftAPI.inRiftRace = false
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!isEnabled() || !RiftAPI.inRiftRace) return

        parkourHelper?.render(event)
    }

    fun isEnabled() =
        RiftAPI.inRift() && RiftAPI.inWestVillage() && config.enabled
}
