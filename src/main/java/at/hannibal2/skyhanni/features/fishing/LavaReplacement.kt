package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.OpaqueWaterFluid
import at.hannibal2.skyhanni.mixins.hooks.FluidModelTransparencyOverride.Companion.transparency
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import com.google.gson.JsonArray
import com.google.gson.JsonPrimitive
import com.mojang.blaze3d.platform.Transparency
import net.minecraft.client.renderer.block.FluidModel
import net.minecraft.client.renderer.block.FluidStateModelSet
import net.minecraft.client.resources.model.sprite.MaterialBaker
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.material.Fluid
import net.minecraft.world.level.material.Fluids

@SkyHanniModule
object LavaReplacement {

    init {
        // Force initialize vanilla fluid registry to avoid load order race conditions
        checkNotNull(Fluids.LAVA)
    }

    private val OPAQUE_WATER = Registry.register(
        BuiltInRegistries.FLUID,
        SkyHanniMod.id("opaque_water"),
        OpaqueWaterFluid.Source,
    )

    private val OPAQUE_FLOWING_WATER = Registry.register(
        BuiltInRegistries.FLUID,
        SkyHanniMod.id("opaque_flowing_water"),
        OpaqueWaterFluid.Flowing,
    )

    private val OPAQUE_WATER_MODEL = FluidModel.Unbaked(
        FluidStateModelSet.WATER_MODEL.stillMaterial(),
        FluidStateModelSet.WATER_MODEL.flowingMaterial(),
        FluidStateModelSet.WATER_MODEL.overlayMaterial(),
        FluidStateModelSet.WATER_MODEL.tintSource(),
    )
    private val config get() = SkyHanniMod.feature.fishing.lavaReplacement

    private var isActive: Boolean = false

    @HandleEvent
    fun onIslandJoin() = update()

    @HandleEvent
    fun onConfigLoad() = ConditionalUtils.onToggle(config.enabled, config.everywhere, config.islands) {
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
    fun addOpaqueWaterModel(original: Map<Fluid, FluidModel>, materials: MaterialBaker) = buildMap {
        val opaqueWaterModel = OPAQUE_WATER_MODEL.bake(materials) { "Opaque Water" }
        opaqueWaterModel.transparency = Transparency.NONE

        putAll(original)
        put(OPAQUE_WATER, opaqueWaterModel)
        put(OPAQUE_FLOWING_WATER, opaqueWaterModel)
    }

    @JvmStatic
    fun getReplacementFluid(original: Fluid): Fluid {
        if (!isActive) return original
        return when (original) {
            Fluids.LAVA -> OPAQUE_WATER
            Fluids.FLOWING_LAVA -> OPAQUE_FLOWING_WATER
            else -> original
        }
    }

    // False positive
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
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(65, "fishing.lavaReplacement.onlyInCrimsonIsle", "fishing.lavaReplacement.everywhere") { element ->
            JsonPrimitive(!element.asBoolean)
        }
        event.move(65, "fishing.lavaReplacement.onlyInCrimsonIsle", "fishing.lavaReplacement.islands") { element ->
            JsonArray().apply { if (element.asBoolean) add(JsonPrimitive(IslandsToReplace.CRIMSON_ISLE.name)) }
        }
    }
}
