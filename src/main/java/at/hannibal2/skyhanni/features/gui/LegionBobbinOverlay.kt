package at.hannibal2.skyhanni.features.gui


import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.mob.MobFilter.isRealPlayer
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getHypixelEnchantments
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getItemUuid
import at.hannibal2.skyhanni.utils.collection.TimeLimitedCache
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat.isLocalPlayer
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.projectile.EntityFishHook
import kotlin.reflect.KMutableProperty0
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object LegionBobbinOverlay {

    private val config get() = SkyHanniMod.feature.gui.legionBobbinOverlay

    private var BOBBERS_DISTANCE = 30.0
    private var BOBBERS_LIMIT = 5
    private var BOBBIN_MULT = 0.2

    private var LEGION_DISTANCE = 30.0
    private var LEGION_LIMIT = 20
    private var LEGION_MULT = 0.07

    private var nearbyBobbers: Int = 0
    private var nearbyPlayers: Int = 0
    private var armorLegionBuff: Double = 0.0
    private var armorBobbinBuff: Double = 0.0

    private val wearingLegion: Boolean get() = armorLegionBuff != 0.0
    private val wearingBobbin: Boolean get() = armorBobbinBuff != 0.0

    private data class ArmorData(
        val legion: Int,
        val bobbin: Int,
    )

    private val armorDataCache = TimeLimitedCache<String, ArmorData>(5.seconds)

    private var display: List<Renderable>? = null

//     @HandleEvent
//     fun onRepoReload(event: RepositoryReloadEvent) {
//         val data = event.getConstant<ItemsJson>("Items").distanceEnchantData
//         LEGION_DISTANCE = data.entries.first().value.distance
//         LEGION_LIMIT = data.entries.first().value.maxamount
//         LEGION_MULT = data.entries.first().value.perstackmultiplier
//
//         BOBBERS_DISTANCE = data.entries.last().value.distance
//         BOBBERS_LIMIT = data.entries.last().value.maxamount
//         BOBBIN_MULT = data.entries.last().value.perstackmultiplier
//     }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTick(event: SkyHanniTickEvent) {
        if (!event.isMod(2) || !isEnabled()) return
        var bobbers = 0
        var players = 0
        val playerPos = LocationUtils.playerLocation()
        for (entity in EntityUtils.getAllEntities()) {
            when (entity) {
                is EntityFishHook -> {
                    if (entity.distanceTo(playerPos) > BOBBERS_DISTANCE) continue
                    ++bobbers
                }

                is EntityPlayer -> {
                    if (entity.isLocalPlayer || !entity.isRealPlayer()) continue
                    if (entity.distanceTo(playerPos) > LEGION_DISTANCE) continue
                    ++players
                }
            }
        }
        modifyValue(::nearbyBobbers, bobbers.coerceAtMost(BOBBERS_LIMIT))
        modifyValue(::nearbyPlayers, players.coerceAtMost(LEGION_LIMIT))
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onSecondPassed() {
        if (!isEnabled()) return
        val armor = InventoryUtils.getArmor()
        var newLegionBuff = 0.0
        var newBobbinBuff = 0.0
        for (piece in armor) {
            if (piece == null) continue
            val uuid = piece.getItemUuid() ?: continue
            val data = armorDataCache.getOrPut(uuid) {
                val enchants = piece.getHypixelEnchantments() ?: return@getOrPut ArmorData(0, 0)
                val legion = enchants["ultimate_legion"] ?: 0
                val bobbin = enchants["ultimate_bobbin_time"] ?: 0
                ArmorData(legion, bobbin)
            }
            newLegionBuff += data.legion * LEGION_MULT
            newBobbinBuff += data.bobbin * BOBBIN_MULT
        }
        modifyValue(::armorLegionBuff, newLegionBuff)
        modifyValue(::armorBobbinBuff, newBobbinBuff)
    }

    // Modifies the passed property with the new value, and if the value is different it resets the display
    private fun <T> modifyValue(property: KMutableProperty0<T>, newValue: T) {
        if (property.get() == newValue) return
        property.set(newValue)
        display = null
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onGuiRender(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        val renderables = display ?: createRenderable().also { display = it }
        config.position.renderRenderables(renderables, posLabel = "Legion Bobbin Display")
    }

    private fun createRenderable(): List<Renderable> {
        return buildList {
            if (!config.hideWithoutEnchant || wearingLegion) add(
                Renderable.horizontal(
                    listOf(
                        Renderable.text("§d§lLegion: "),
                        Renderable.text("§b$nearbyPlayers §7(${(armorLegionBuff * nearbyPlayers).roundTo(2)}%)"),
                    ),
                ),
            )
            if (!config.hideWithoutEnchant || wearingBobbin) add(
                Renderable.horizontal(
                    listOf(
                        Renderable.text("§3§lBobbin': "),
                        Renderable.text("§b$nearbyBobbers §7(${(armorBobbinBuff * nearbyBobbers).roundTo(2)}%)"),
                    ),
                ),
            )
        }
    }

    private fun isEnabled() = config.enabled

}
