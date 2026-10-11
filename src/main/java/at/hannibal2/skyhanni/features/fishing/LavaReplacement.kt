package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import com.google.gson.JsonArray
import com.google.gson.JsonPrimitive
import net.minecraft.client.renderer.block.FluidModel
import net.minecraft.client.renderer.chunk.ChunkSectionLayer
import net.minecraft.world.level.material.Fluid
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.Fluids

@SkyHanniModule
object LavaReplacement {
    private val config get() = SkyHanniMod.feature.fishing.lavaReplacement

    private var isActive: Boolean = false
    private var opaqueWaterModel: FluidModel? = null

    @HandleEvent
    private fun onIslandJoin() = update()

    @HandleEvent
    private fun onConfigLoad() = ConditionalUtils.onToggle(config.enabled, config.everywhere, config.islands) {
        update()
    }

    private fun update() {
        val newActive = shouldReplace()
        if (newActive == isActive) return
        isActive = newActive
        MinecraftCompat.reloadChunks()
    }

    private fun shouldReplace(): Boolean {
        if (!SkyBlockUtils.inSkyBlock || !config.enabled.get()) return false
        if (config.everywhere.get()) return true
        return config.islands.get().any(IslandsToReplace::inIsland)
    }

    @JvmStatic
    fun onModelsBaked(models: Map<Fluid, FluidModel>) {
        opaqueWaterModel = models[Fluids.WATER]?.let {
            FluidModel(ChunkSectionLayer.SOLID, it.stillMaterial(), it.flowingMaterial(), it.overlayMaterial(), it.tintSource())
        }
    }

    @JvmStatic
    fun getReplacementModel(state: FluidState, original: FluidModel): FluidModel {
        if (!isActive) return original
        return when (state.type) {
            Fluids.LAVA, Fluids.FLOWING_LAVA -> opaqueWaterModel ?: original
            else -> original
        }
    }

    // None of the enum constants are actually unused, they are exposed to the user in the config.
    @Suppress("unused")
    enum class IslandsToReplace(private val displayName: String, val island: IslandType) {
        KUUDRA("§4Kuudra", IslandType.KUUDRA_ARENA),
        CATACOMBS("§2Dungeons", IslandType.CATACOMBS),
        CRIMSON_ISLE("§cCrimson Isle", IslandType.CRIMSON_ISLE),
        ;

        override fun toString() = displayName

        fun inIsland() = island.isInIsland()
    }

    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(65, "fishing.lavaReplacement.onlyInCrimsonIsle", "fishing.lavaReplacement.everywhere") { element ->
            JsonPrimitive(!element.asBoolean)
        }
        event.move(65, "fishing.lavaReplacement.onlyInCrimsonIsle", "fishing.lavaReplacement.islands") { element ->
            JsonArray().apply { if (element.asBoolean) add(JsonPrimitive(IslandsToReplace.CRIMSON_ISLE.name)) }
        }
    }
}
