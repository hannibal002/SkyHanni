package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object LavaReplacement {

    private val config get() = SkyHanniMod.feature.fishing.lavaReplacement

    @SubscribeEvent
    fun onIslandChange(event: IslandChangeEvent) {
        if (replaceLava()) {
            Minecraft.getMinecraft().renderGlobal.loadRenderers()
        }
    }
    fun onConfigLoad(event: ConfigLoadEvent) {

        ConditionalUtils.onToggle(
            config.enabled,
            config.IslandsToReplace,
            ) {
            Minecraft.getMinecraft().renderGlobal.loadRenderers()
        }

    }
    enum class IslandsToReplace(private val displayName: String, public val island: IslandType)
    {
        KUUDRA("§4Kuudra", IslandType.KUUDRA_ARENA),
        CATACOMBS("§2Dungeons", IslandType.CATACOMBS),
        CRIMSON_ISLE("§cCrimson Isle", IslandType.CRIMSON_ISLE),
        ;
        override fun toString() = displayName

        fun isSelected() = SkyHanniMod.feature.fishing.lavaReplacement.IslandsToReplace.get().contains(this)
    }
    @JvmStatic
    fun replaceLava(): Boolean {
        if (!LorenzUtils.inSkyBlock || !config.enabled.get()) return false
        if (config.enabledeverywehere.get()) return true
        if (IslandsToReplace.CATACOMBS.isSelected() && IslandType.CATACOMBS.isInIsland()) return true
        if (IslandsToReplace.KUUDRA.isSelected() && IslandType.KUUDRA_ARENA.isInIsland()) return true
        if (IslandsToReplace.CRIMSON_ISLE.isSelected() && IslandType.CRIMSON_ISLE.isInIsland()) return true
        return false
}}
