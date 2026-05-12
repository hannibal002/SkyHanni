package at.hannibal2.skyhanni.features.garden.tracker

import at.hannibal2.skyhanni.SkyHanniMod.launch
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.jsonobjects.repo.RareCropDropInfo
import at.hannibal2.skyhanni.data.jsonobjects.repo.RareCropDropsJson
import at.hannibal2.skyhanni.events.IslandJoinEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RecalculatingValue
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sortedDesc
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.tracker.SessionUptime
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import at.hannibal2.skyhanni.utils.tracker.TrackerData
import com.google.gson.JsonObject
import com.google.gson.annotations.Expose
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object RareCropTracker {

    private val config get() = GardenApi.config.rareCropTracker

    private val patternGroup = RepoPattern.group("garden.rarecrops")
    private val repoReloadCoroutine = CoroutineSettings("rare crop tracker repo reload")

    /**
     * REGEX-TEST: FERMENTO_CHESTPLATE
     * REGEX-TEST: CROPIE_BOOTS
     * REGEX-TEST: SQUASH_HELMET
     */
    private val armorPattern by patternGroup.pattern(
        "armor",
        "(?:HELIANTHUS|FERMENTO|CROPIE|SQUASH|MELON)_(?:LEGGINGS|CHESTPLATE|BOOTS|HELMET)",
    )

    val hasArmor by RecalculatingValue(1.seconds) {
        GardenApi.inGarden() && checkArmor()
    }

    val tracker = SkyHanniTracker(
        "Rare Crop Tracker",
        ::Data,
        { it.garden.rareCropTracker },
        trackerConfig = { config.perTrackerConfig },
        customUptimeControl = true,
    ) {
        drawDisplay(it)
    }

    data class Data(
        @Expose
        var drops: MutableMap<RareCropDropType, Int> = mutableMapOf(),
    ) : TrackerData<SessionUptime.Garden>(SessionUptime.Garden::class)


    init {
        RareCropDropType.entries.forEach { it.chatPattern }

        tracker.initRenderer({ config.position }) { shouldShowDisplay() }
    }

    enum class RareCropDropType(val dropName: String, private val messageName: String) {
        CROPIE("§aCropie", "Cropie"),
        SQUASH("§9Squash", "Squash"),
        FERMENTO("§5Fermento", "Fermento"),
        HELIANTHUS("§6Helianthus", "Helianthus"),
        SEASONING("§2Seasoning", "Seasoning"),
        CORNUCOPIA("§aCornucopia", "Cornucopia"),
        CARROT_ZEST("§aCarrot Zest", "Carrot Zest"),
        DEEPFRIES("§aDeepfries", "Deepfries"),
        AGGOURDIAN("§aAggourdian", "Aggourdian"),
        CANE_KNOT("§aCane Knot", "Cane Knot"),
        MELON_JUICE("§aMelon Juice", "Melon Juice"),
        CACTUS_FLOWER("§aCactus Flower", "Cactus Flower"),
        DESIGNER_COFFEE_BEANS("§aDesigner Coffee Beans", "Designer Coffee Beans"),
        FEASTFUNGUS("§aFeastfungus", "Feastfungus"),
        BOTROOT("§aBotroot", "Botroot"),
        SALTED_SUNFLOWER_SEEDS("§aSalted Sunflower Seeds", "Salted Sunflower Seeds"),
        CRYSTALIZED_MOONLIGHT("§aCrystalized Moonlight", "Crystalized Moonlight"),
        FLORAL_GELATIN("§aFloral Gelatin", "Floral Gelatin"),
        RAREFINDER_CHIP("§9Rarefinder Chip", "Rarefinder Chip"),
        BURROWING_SPORES("§9Burrowing Spores", "Burrowing Spores"),
        WARTY("§5Warty", "Warty"),
        ;

        val chatPattern by patternGroup.pattern(
            name.lowercase().replace('_', '-'),
            "(?:§.)*(?:VERY )?RARE CROP! (?:§.)*$messageName.*",
        )
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent.Allow) {
        for (dropType in RareCropDropType.entries) {
            if (!dropType.chatPattern.matches(event.message)) continue
            addDrop(dropType)
            if (config.hideChat) {
                event.blockedReason = "rare_crop_tracker"
            }
        }
    }

    private fun addDrop(drop: RareCropDropType) {
        tracker.modify {
            it.drops.addOrPut(drop, 1)
        }
    }

    private fun drawDisplay(data: Data): List<Searchable> = buildList {
        addSearchString("§7Rare Crop Tracker:")
        val sorted = data.drops.sortedDesc().entries
        val maxLines = config.maxDisplayLines
        for ((drop, amount) in if (maxLines > 0) sorted.take(maxLines) else sorted) {
            val dropName = drop.dropName
            addSearchString(" §7- §e${amount.addSeparators()}x $dropName", dropName)
        }
    }

    private fun shouldShowDisplay(): Boolean {
        if (!GardenApi.inGarden()) return false
        if (!config.enabled) return false
        if (!GardenApi.hasFarmingToolInHand()) return false

        return true
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onIslandJoin(event: IslandJoinEvent) {
        tracker.firstUpdate()
    }

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) = repoReloadCoroutine.launch {
        val data = event.getConstantAsync<RareCropDropsJson>("ArmorDrops")
        rareCropDropInfo = data.specialCrops
    }

    private var rareCropDropInfo = mapOf<String, RareCropDropInfo>()
    private var currentRareCropDropChance = 0.0
    private var lastCalculationTime = SimpleTimeMark.farPast()

    private fun checkArmor(): Boolean {
        val armorPieces = InventoryUtils.getArmor()
            .mapNotNull { it?.getInternalName()?.asString() }
            .count { armorPattern.matcher(it).matches() }
        return armorPieces > 1
    }

    fun getDropsPerHour(crop: CropType?): Double {
        if (crop == null) return 0.0

        if (lastCalculationTime.passedSince() > 5.seconds) {
            lastCalculationTime = SimpleTimeMark.now()

            val rareCropDropName = crop.specialDropType
            val armorName = rareCropDropInfo[rareCropDropName]?.armorType ?: return 0.0
            val pieceCount = InventoryUtils.getArmor()
                .mapNotNull { it?.getInternalName()?.asString() }
                .count { it.contains(armorName) || it.contains("FERMENTO") || it.contains("HELIANTHUS") }

            val dropRates = rareCropDropInfo[rareCropDropName]?.chance ?: return 0.0
            var dropRate = 0.0
            if (pieceCount > 0 && dropRates.size >= pieceCount) {
                dropRate = dropRates[pieceCount - 1]
            }
            currentRareCropDropChance = (dropRate * 60 * 60.0) / 100
        }
        return currentRareCropDropChance
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
        event.move(133, "garden.armorDropTracker", "garden.rareCropTracker")
        event.move(133, "#profile.garden.armorDropTracker", "#profile.garden.rareCropTracker")
        event.move(
            133,
            "storage.trackerDisplayModes.Armor Drop Tracker",
            "storage.trackerDisplayModes.Rare Crop Tracker",
        )
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shresetrarecroptracker") {
            description = "Resets the Rare Crop Tracker"
            category = CommandCategory.USERS_RESET
            simpleCallback { tracker.resetCommand() }
        }
        event.registerBrigadier("shresetarmordroptracker") {
            description = "Resets the Rare Crop Tracker"
            category = CommandCategory.USERS_RESET
            simpleCallback { tracker.resetCommand() }
        }
    }
}
