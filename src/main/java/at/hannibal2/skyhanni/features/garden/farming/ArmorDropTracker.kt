package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.PreProfileSwitchEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzUtils.addOrPut
import at.hannibal2.skyhanni.utils.LorenzUtils.sortedDesc
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.data.jsonobjects.repo.ArmorDropsJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.ArmorDropsJson.DropInfo
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import at.hannibal2.skyhanni.utils.tracker.TrackerData
import com.google.gson.JsonObject
import com.google.gson.annotations.Expose
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

object ArmorDropTracker {
    private var display = emptyList<List<Any>>()

    private var hasArmor = false
    private val armorPattern = "(FERMENTO|CROPIE|SQUASH|MELON)_(LEGGINGS|CHESTPLATE|BOOTS|HELMET)".toPattern()
    private val config get() = SkyHanniMod.feature.garden.farmingArmorDrop

    private val tracker = SkyHanniTracker("Armor Drop Tracker", { Data() }, { it.garden.armorDropTracker }) { update() }

    class Data : TrackerData() {
        override fun reset() {
            drops.clear()
        }

        @Expose
        var drops: MutableMap<ArmorDropType, Int> = mutableMapOf()
    }

    enum class ArmorDropType(val dropName: String, val chatMessage: String) {
        CROPIE("§9Cropie", "§6§lRARE CROP! §r§f§r§9Cropie §r§b(Armor Set Bonus)"),
        SQUASH("§5Squash", "§6§lRARE CROP! §r§f§r§5Squash §r§b(Armor Set Bonus)"),
        FERMENTO("§6Fermento", "§6§lRARE CROP! §r§f§r§6Fermento §r§b(Armor Set Bonus)"),
    }

    @SubscribeEvent
    fun onPreProfileSwitch(event: PreProfileSwitchEvent) {
        display = emptyList()
        hasArmor = false
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        for (dropType in ArmorDropType.entries) {
            if (dropType.chatMessage == event.message) {
                addDrop(dropType)
                if (config.hideChat) {
                    event.blockedReason = "farming_armor_drops"
                }
            }
        }
    }

    private fun addDrop(drop: ArmorDropType) {
        tracker.modify {
            it.drops.addOrPut(drop, 1)
        }
        update()
    }

    private fun update() {
        display = drawDisplay()
    }

    private fun drawDisplay() = buildList<List<Any>> {
        val drops = tracker.currentDisplay()?.drops ?: return@buildList

        addAsSingletonList("§7RNG Drops for Farming Armor:")
        tracker.addDisplayModeToggle(this)
        for ((drop, amount) in drops.sortedDesc()) {
            val dropName = drop.dropName
            addAsSingletonList(" §7- §e${amount.addSeparators()}x $dropName")
        }

        tracker.addSessionResetButton(this)
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        update()
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!GardenAPI.inGarden()) return
        if (!config.enabled) return
        if (!hasArmor) return

        tracker.renderDisplay(config.pos, display)
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!GardenAPI.inGarden()) return
        if (!config.enabled) return

        if (event.isMod(30)) {
            checkArmor()
        }
    }

    private fun checkArmor() {
        val armorPieces = InventoryUtils.getArmor()
            .mapNotNull { it?.getInternalName()?.asString() }
            .count { armorPattern.matcher(it).matches() }
        hasArmor = armorPieces > 1
    }

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<ArmorDropsJson>("ArmorDrops")
        armorDropInfo = data.special_crops
    }

    private var armorDropInfo = mapOf<String, DropInfo>()
    private var currentArmorDropChance = 0.0
    private var lastCalculationTime = SimpleTimeMark.farPast()

    fun getDropsPerHour(crop: CropType?): Double {
        if (crop == null) return 0.0

        if (lastCalculationTime.passedSince() > 5.seconds) {
            lastCalculationTime = SimpleTimeMark.now()

            val armorDropName = crop.specialDropType
            val armorName = armorDropInfo[armorDropName]?.armor_type ?: return 0.0
            val pieceCount = InventoryUtils.getArmor()
                .mapNotNull { it?.getInternalName()?.asString() }
                .count { it.contains(armorName) || it.contains("FERMENTO") }

            val dropRates = armorDropInfo[armorDropName]?.chance ?: return 0.0
            var dropRate = 0.0
            if (pieceCount > 0 && dropRates.size >= pieceCount) {
                dropRate = dropRates[pieceCount - 1]
            }
            currentArmorDropChance = (dropRate * 60 * 60.0) / 100
        }
        return currentArmorDropChance
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "garden.farmingArmorDropsEnabled", "garden.farmingArmorDrop.enabled")
        event.move(3, "garden.farmingArmorDropsHideChat", "garden.farmingArmorDrop.hideChat")
        event.move(3, "garden.farmingArmorDropsPos", "garden.farmingArmorDrop.pos")

        event.move(8, "#profile.garden.farmArmorDrops", "#profile.garden.armorDropTracker") { old ->
            val new = JsonObject()
            new.add("drops", old)
            new
        }
    }

    fun resetCommand(args: Array<String>) {
        tracker.resetCommand(args, "shresetarmordroptracker")
    }
}
