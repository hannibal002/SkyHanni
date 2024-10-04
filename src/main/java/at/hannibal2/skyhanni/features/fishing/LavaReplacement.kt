package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object LavaReplacement {

    private val config get() = SkyHanniMod.feature.fishing.lavaReplacement

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        ConditionalUtils.onToggle(config.enabled, config.onlyInCrimsonIsle) {
            Minecraft.getMinecraft().renderGlobal.loadRenderers()
        }
    }
}
