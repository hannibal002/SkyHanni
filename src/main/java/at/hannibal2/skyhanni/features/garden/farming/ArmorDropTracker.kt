package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.jsonobjects.repo.ArmorDropInfo
import at.hannibal2.skyhanni.data.jsonobjects.repo.ArmorDropsJson
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sortedDesc
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import at.hannibal2.skyhanni.utils.tracker.TrackerData
import com.google.gson.JsonObject
import com.google.gson.annotations.Expose
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object ArmorDropTracker {

    private val config get() = GardenApi.config.armorDropTracker

    private val patternGroup = RepoPattern.group("garden.armordrops")

    /**
     * REGEX-TEST: FERMENTO_CHESTPLATE
     * REGEX-TEST: CROPIE_BOOTS
     * REGEX-TEST: SQUASH_HELMET
     */
    private val armorPattern by patternGroup.pattern(
        "armor",
        "(?:HELIANTHUS|FERMENTO|CROPIE|SQUASH|MELON)_(?:LEGGINGS|CHESTPLATE|BOOTS|HELMET)",
    )

    private var hasArmor = false

    private val tracker = SkyHanniTracker("Armor Drop Tracker", ::Data, { it.garden.armorDropTracker }) {
        drawDisplay(it)
    }

    data class Data(
        @Expose
        var drops: MutableMap<ArmorDropType, Int> = mutableMapOf(),
    ) : TrackerData()

    init {
        ArmorDropType.entries.forEach { it.chatPattern }
    }

    enum class ArmorDropType(val dropName: String, chatMessage: String) {
        CROPIE("§aCropie", "§6§lRARE CROP! §r§f§r§aCropie §r§b\\(Armor Set Bonus\\)"),
        SQUASH("§9Squash", "§6§lRARE CROP! §r§f§r§9Squash §r§b\\(Armor Set Bonus\\)"),
        FERMENTO("§5Fermento", "§6§lRARE CROP! §r§f§r§5Fermento §r§b\\(Armor Set Bonus\\)"),
        HELIANTHUS("§6Helianthus", "§6§lRARE CROP! §r§f§r§6Helianthus §r§b\\(Armor Set Bonus\\)"),
        ;

        val chatPattern by patternGroup.pattern(
            name.lowercase(),
            chatMessage,
        )
    }

    @HandleEvent
    fun onProfileJoin() {
        hasArmor = false
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent) {
        for (dropType in ArmorDropType.entries) {
            if (!dropType.chatPattern.matches(event.message)) continue
            addDrop(dropType)
            if (config.hideChat) {
                event.blockedReason = "farming_armor_drops"
            }
        }
    }

    private fun addDrop(drop: ArmorDropType) {
        tracker.modify {
            it.drops.addOrPut(drop, 1)
        }
    }

    private fun drawDisplay(data: Data): List<Searchable> = buildList {
        addSearchString("§7Armor Drop Tracker:")
        for ((drop, amount) in data.drops.sortedDesc()) {
            val dropName = drop.dropName
            addSearchString(" §7- §e${amount.addSeparators()}x $dropName", dropName)
        }
    }

    init {
        tracker.initRenderer({ config.position }) { shouldShowDisplay() }
    }

    private fun shouldShowDisplay(): Boolean {
        if (!GardenApi.inGarden()) return false
        if (!config.enabled) return false
        if (!hasArmor) return false
        if (!GardenApi.hasFarmingToolInHand()) return false

        return true
    }

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        if (event.newIsland == IslandType.GARDEN) {
            tracker.firstUpdate()
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onSecondPassed() {
        checkArmor()
    }

    private fun checkArmor() {
        val armorPieces = InventoryUtils.getArmor()
            .mapNotNull { it?.getInternalName()?.asString() }
            .count { armorPattern.matcher(it).matches() }
        hasArmor = armorPieces > 1
    }

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<ArmorDropsJson>("ArmorDrops")
        armorDropInfo = data.specialCrops
    }

    private var armorDropInfo = mapOf<String, ArmorDropInfo>()
    private var currentArmorDropChance = 0.0
    private var lastCalculationTime = SimpleTimeMark.farPast()

    fun getDropsPerHour(crop: CropType?): Double {
        if (crop == null) return 0.0

        if (lastCalculationTime.passedSince() > 5.seconds) {
            lastCalculationTime = SimpleTimeMark.now()

            val armorDropName = crop.specialDropType
            val armorName = armorDropInfo[armorDropName]?.armorType ?: return 0.0
            val pieceCount = InventoryUtils.getArmor()
                .mapNotNull { it?.getInternalName()?.asString() }
                .count { it.contains(armorName) || it.contains("FERMENTO") || it.contains("HELIANTHUS") }

            val dropRates = armorDropInfo[armorDropName]?.chance ?: return 0.0
            var dropRate = 0.0
            if (pieceCount > 0 && dropRates.size >= pieceCount) {
                dropRate = dropRates[pieceCount - 1]
            }
            currentArmorDropChance = (dropRate * 60 * 60.0) / 100
        }
        return currentArmorDropChance
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "garden.farmingArmorDropsEnabled", "garden.farmingArmorDrop.enabled")
        event.move(3, "garden.farmingArmorDropsHideChat", "garden.farmingArmorDrop.hideChat")
        event.move(3, "garden.farmingArmorDropsPos", "garden.farmingArmorDrop.pos")

        event.move(8, "#profile.garden.farmArmorDrops", "#profile.garden.armorDropTracker") { old ->
            val new = JsonObject()
            new.add("drops", old)
            new
        }
        event.move(87, "garden.farmingArmorDrop.pos", "garden.armorDropTracker.position")
        event.move(88, "garden.farmingArmorDrop", "garden.armorDropTracker")
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shresetarmordroptracker") {
            description = "Resets the Armor Drop Tracker"
            category = CommandCategory.USERS_RESET
            callback { tracker.resetCommand() }
        }
    }
}
