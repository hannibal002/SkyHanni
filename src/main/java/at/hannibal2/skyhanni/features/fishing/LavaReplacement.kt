package at.hannibal2.hanni.features.fishing

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.events.ConfigLoadEvent
import at.hannibal2.hanni.events.IslandChangeEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ConditionalUtils
import at.hannibal2.hanni.utils.SkyBlockUtils
import com.google.gson.JsonArray
import com.google.gson.JsonPrimitive
import net.minecraft.client.Minecraft

@HanniModule
object LavaReplacement {

    private val config get() = HanniMod.feature.fishing.lavaReplacement

    @JvmStatic
    var isActive: Boolean = false
        private set

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        if (event.newIsland != IslandType.NONE) {
            update()
        }
    }

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        ConditionalUtils.onToggle(config.enabled, config.everywhere, config.islands) {
            update()
        }
    }

    private fun update() {
        val newActive = shouldReplace()
        if (newActive == isActive) return
        isActive = newActive
        Minecraft.getMinecraft().renderGlobal.loadRenderers()
    }

    private fun shouldReplace(): Boolean {
        if (!SkyBlockUtils.inSkyBlock || !config.enabled.get()) return false
        if (config.everywhere.get()) return true
        return config.islands.get().any(IslandsToReplace::inIsland)
    }

    enum class IslandsToReplace(private val displayName: String, val island: IslandType) {
        KUUDRA("§4Kuudra", IslandType.KUUDRA_ARENA),
        CATACOMBS("§2Dungeons", IslandType.CATACOMBS),
        CRIMSON_ISLE("§cCrimson Isle", IslandType.CRIMSON_ISLE),
        ;

        override fun toString() = displayName

        fun inIsland() = island.isCurrent()
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(65, "fishing.lavaReplacement.onlyInCrimsonIsle", "fishing.lavaReplacement.everywhere") { element ->
            JsonPrimitive(!element.asBoolean)
        }
        event.move(65, "fishing.lavaReplacement.onlyInCrimsonIsle", "fishing.lavaReplacement.islands") { element ->
            JsonArray().apply { if (element.asBoolean) add(JsonPrimitive(IslandsToReplace.CRIMSON_ISLE.name)) }
        }
    }
}
