package at.hannibal2.skyhanni.features.gui

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.jsonobjects.repo.ItemsJson
import at.hannibal2.skyhanni.data.mob.MobFilter.isRealPlayer
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getHypixelEnchantments
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getItemUuid
import at.hannibal2.skyhanni.utils.collection.TimeLimitedCache
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat.isLocalPlayer
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.FishingHook
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

    @Suppress("VarCouldBeVal")
    private var nearbyBobbers: Int = 0

    @Suppress("VarCouldBeVal")
    private var nearbyPlayers: Int = 0

    @Suppress("VarCouldBeVal")
    private var armorLegionBuff: Double = 0.0

    @Suppress("VarCouldBeVal")
    private var armorBobbinBuff: Double = 0.0

    private val wearingLegion: Boolean get() = armorLegionBuff != 0.0
    private val wearingBobbin: Boolean get() = armorBobbinBuff != 0.0

    private data class ArmorData(
        val legion: Int,
        val bobbin: Int,
    )

    private val armorDataCache = TimeLimitedCache<String, ArmorData>(5.seconds)

    private var display: List<Renderable>? = null

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<ItemsJson>("Items").distanceEnchantData
        val legion = data["LEGION"]
        LEGION_DISTANCE = legion?.distance ?: 30.0
        LEGION_LIMIT = legion?.maxAmount ?: 20
        LEGION_MULT = legion?.perStackMultiplier ?: 0.07

        val bobbin = data["BOBBIN"]
        BOBBERS_DISTANCE = bobbin?.distance ?: 30.0
        BOBBERS_LIMIT = bobbin?.maxAmount ?: 5
        BOBBIN_MULT = bobbin?.perStackMultiplier ?: 0.2

        display = null
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTick() {
        if (!isEnabled()) return
        val bobbers = EntityUtils.getEntitiesNextToPlayer<FishingHook>(BOBBERS_DISTANCE).size
        val players = EntityUtils.getEntitiesNextToPlayer<Player>(LEGION_DISTANCE).count {
            !it.isLocalPlayer && it.isRealPlayer()
        }
        nearbyBobbers = modifyValue(nearbyBobbers, bobbers.coerceAtMost(BOBBERS_LIMIT))
        nearbyPlayers = modifyValue(nearbyPlayers, players.coerceAtMost(LEGION_LIMIT))
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
        armorLegionBuff = modifyValue(armorLegionBuff, newLegionBuff)
        armorBobbinBuff = modifyValue(armorBobbinBuff, newBobbinBuff)
    }

    private fun <T> modifyValue(old: T, new: T): T {
        if (old != new) display = null
        return new
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
