package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.model.OpaqueWaterFluid
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import com.google.gson.JsonArray
import com.google.gson.JsonPrimitive
import net.minecraft.client.Minecraft
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.material.Fluid
import net.minecraft.world.level.material.Fluids

//? if >= 26.1 {
import com.mojang.blaze3d.platform.Transparency
import net.minecraft.client.renderer.block.FluidModel
import net.minecraft.client.renderer.block.FluidStateModelSet
import net.minecraft.client.resources.model.sprite.MaterialBaker
//?} else {
/*import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap
import net.minecraft.client.renderer.BiomeColors
import net.minecraft.client.renderer.chunk.ChunkSectionLayer
import net.minecraft.core.BlockPos
import net.minecraft.world.level.BlockAndTintGetter
import net.minecraft.world.level.material.FluidState
*///?}

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

    //? if >= 26.1 {
    private val OPAQUE_WATER_MODEL = FluidModel.Unbaked(
        FluidStateModelSet.WATER_MODEL.stillMaterial(),
        FluidStateModelSet.WATER_MODEL.flowingMaterial(),
        FluidStateModelSet.WATER_MODEL.overlayMaterial(),
        FluidStateModelSet.WATER_MODEL.tintSource(),
    )
    //?} else {
    /*init {
        FluidRenderHandlerRegistry.INSTANCE.register(
            OPAQUE_WATER,
            OPAQUE_FLOWING_WATER,
            object : SimpleFluidRenderHandler(
                WATER_STILL,
                WATER_FLOWING,
                WATER_OVERLAY,
            ) {
                override fun getFluidColor(view: BlockAndTintGetter?, pos: BlockPos?, state: FluidState): Int =
                    MinecraftCompat.localWorldOrNull?.calculateBlockTint(
                        pos ?: BlockPos.ZERO,
                        BiomeColors.WATER_COLOR_RESOLVER,
                    ) ?: super.getFluidColor(view, pos, state)
            }
        )
        BlockRenderLayerMap.putFluids(ChunkSectionLayer.SOLID, OPAQUE_WATER, OPAQUE_FLOWING_WATER)
    }
    *///?}

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
        DelayedRun.runNextTick {
            Minecraft.getInstance().levelRenderer.allChanged()
        }
    }

    private fun shouldReplace(): Boolean {
        if (!SkyBlockUtils.inSkyBlock || !config.enabled.get()) return false
        if (config.everywhere.get()) return true
        return config.islands.get().any(IslandsToReplace::inIsland)
    }

    //? if >= 26.1 {
    @JvmStatic
    fun addOpaqueWaterModel(original: Map<Fluid, FluidModel>, materials: MaterialBaker) = buildMap {
        val opaqueWaterModel = OPAQUE_WATER_MODEL.bake(materials) { "Opaque Water" }
        opaqueWaterModel.transparency = Transparency.NONE

        putAll(original)
        put(OPAQUE_WATER, opaqueWaterModel)
        put(OPAQUE_FLOWING_WATER, opaqueWaterModel)
    }
    //?}

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
