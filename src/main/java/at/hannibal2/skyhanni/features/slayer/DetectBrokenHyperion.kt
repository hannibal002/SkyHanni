package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.SlayerAPI
import at.hannibal2.skyhanni.data.TitleUtils
import at.hannibal2.skyhanni.events.PurseChangeCause
import at.hannibal2.skyhanni.events.PurseChangeEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getAbilityScrolls
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class DetectBrokenHyperion {
    private val config get() = SkyHanniMod.feature.slayer
    private var brokenInRow = 0
    private val logger = LorenzLogger("detect_broken_hyperion")

    @SubscribeEvent
    fun onPurseChange(event: PurseChangeEvent) {
        if (!isEnabled()) return
        if (event.reason != PurseChangeCause.GAIN_MOB_KILL) return
        if (!SlayerAPI.hasActiveSlayerQuest()) return
        if (!SlayerAPI.isInSlayerArea) return
        if (SlayerAPI.latestWrongAreaWarning + 5_000 > System.currentTimeMillis()) return

        val abilityScrolls = InventoryUtils.getItemInHand()?.getAbilityScrolls() ?: return
        if (!abilityScrolls.contains("IMPLOSION_SCROLL")) return

        val diff = System.currentTimeMillis() - SlayerAPI.getLatestProgressChangeTime()
        logger.log("diff: $diff")

        if (diff < 2_500) {
            if (brokenInRow != 0) {

                brokenInRow = 0
                logger.log(" reset to 0")
            }
            return
        }

        brokenInRow++
        logger.log(" add: $brokenInRow")

        if (brokenInRow > 5) {
            logger.log(" send warning!")
            TitleUtils.sendTitle("§eBroken Hyperion!", 3_000)
            LorenzUtils.chat(
                "§e[SkyHanni] Your Hyperion is broken! It no longer collects combat exp. " +
                        "Kill a mob with meele-hits to fix this hypixel bug"
            )
        }

        LorenzUtils.debug("diff: $diff")
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.brokenHyperion
}
