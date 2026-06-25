package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.InteractClickType
import at.hannibal2.skyhanni.data.jsonobjects.repo.ItemsJson
import at.hannibal2.skyhanni.events.fishing.BaitUpdateEvent
import at.hannibal2.skyhanni.events.ItemInHandChangeEvent
import at.hannibal2.skyhanni.events.OwnInventoryMenuUpdateEvent
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.WorldClickEvent
import at.hannibal2.skyhanni.events.entity.EntityEnterWorldEvent
import at.hannibal2.skyhanni.events.fishing.FishingBobberCastEvent
import at.hannibal2.skyhanni.events.fishing.FishingBobberInLiquidEvent
import at.hannibal2.skyhanni.events.fishing.FishingCatchEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonApi
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyFishManager
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyFishManager.getFilletValue
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyRarity
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalNames
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getExtraAttributes
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.compat.addLavas
import at.hannibal2.skyhanni.utils.compat.addWaters
import at.hannibal2.skyhanni.utils.compat.deceased
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
import at.hannibal2.skyhanni.utils.compat.getCompoundOrDefault
import at.hannibal2.skyhanni.utils.compat.getStringOrDefault
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.projectile.FishingHook
import kotlin.time.Duration.Companion.seconds

@Suppress("MemberVisibilityCanBePrivate")
@SkyHanniModule
object FishingApi {

    enum class RodPart {
        HOOK,
        LINE,
        SINKER,
        ;

        val tagName get() = name.lowercase()
    }

    data class BaitType(val displayName: String, val internalName: NeuInternalName) {
        override fun toString(): String {
            return internalName.asString()
        }
    }

    /**
     * REGEX-TEST: BRONZE_HUNTER_HELMET
     * REGEX-TEST: SILVER_HUNTER_CHESTPLATE
     * REGEX-TEST: GOLD_HUNTER_LEGGINGS
     * REGEX-TEST: DIAMOND_HUNTER_BOOTS
     */
    private val trophyArmorNames by RepoPattern.pattern(
        "fishing.trophyfishing.armor",
        "(?<tier>BRONZE|SILVER|GOLD|DIAMOND)_HUNTER_(?:HELMET|CHESTPLATE|LEGGINGS|BOOTS)",
    )

    /**
     * REGEX-TEST: EMBER_HELMET
     * REGEX-TEST: EMBER_CHESTPLATE
     * REGEX-TEST: EMBER_LEGGINGS
     * REGEX-TEST: EMBER_BOOTS
     */
    private val emberArmorNames by RepoPattern.pattern(
        "fishing.trophyfishing.emberarmor",
        "EMBER_(?:HELMET|CHESTPLATE|LEGGINGS|BOOTS)",
    )

    /**
     * REGEX-TEST: Bait Remaining: 49
     */
    private val baitRemainingPattern by RepoPattern.pattern(
        "fishing.bait.inventory",
        "Bait Remaining: (?<amount>[\\d,]+)",
    )

    private val obfuscatedBaits = setOf(
        "OBFUSCATED_FISH_1_BRONZE",
        "OBFUSCATED_FISH_1_SILVER",
        "OBFUSCATED_FISH_1_GOLD",
        "OBFUSCATED_FISH_1_DIAMOND",
        "OBFUSCATED_FISH_2_BRONZE",
        "OBFUSCATED_FISH_2_SILVER",
        "OBFUSCATED_FISH_2_GOLD",
        "OBFUSCATED_FISH_2_DIAMOND",
    ).toInternalNames()

    const val babySlugName = "Baby Magma Slug"

    val lavaBlocks = buildList { addLavas() }
    private val waterBlocks = buildList { addWaters() }

    var lastCastTime = SimpleTimeMark.farPast()
        private set
    var lastReelTime = SimpleTimeMark.farPast()
        private set
    var lastCatchSound = SimpleTimeMark.farPast()
        private set
    var holdingRod = false
        private set
    var holdingLavaRod = false
        private set
    var holdingWaterRod = false
        private set
    var hasTreasureHook = false
        private set

    private var lavaRods = listOf<NeuInternalName>()
    private var waterRods = listOf<NeuInternalName>()
    private val TREASURE_HOOK = "TREASURE_HOOK".toInternalName()

    private const val BAIT_HOTBAR_INDEX = 8

    var bobber: FishingHook? = null
        private set
    var bobberHasTouchedLiquid = false
        private set

    var currentBait: BaitType? = null
        private set
    var currentBaitAmount: Int = 0
        private set

    var wearingTrophyArmor = false
        private set

    var wearingEmberArmor = false
        private set

    @HandleEvent(onlyOnSkyblock = true)
    fun onJoinWorld(event: EntityEnterWorldEvent<FishingHook>) {
        if (!holdingRod) return
        if (event.entity.playerOwner?.isLocalPlayer == false) return

        lastCastTime = SimpleTimeMark.now()
        bobber = event.entity
        bobberHasTouchedLiquid = false
        FishingBobberCastEvent(event.entity).post()
    }

    private fun resetBobber() {
        bobber = null
        bobberHasTouchedLiquid = false
    }

    @HandleEvent
    fun onWorldChange() {
        resetBobber()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTick(event: SkyHanniTickEvent) {
        if (event.isMod(5)) {
            wearingTrophyArmor = isWearingTrophyArmor()
            wearingEmberArmor = isWearingEmberArmor()
        }

        val bobber = bobber ?: return
        if (bobber.deceased) {
            if (lastReelTime.passedSince() < 0.5.seconds && lastCatchSound.passedSince() < 0.5.seconds) FishingCatchEvent.post()
            resetBobber()
            return
        }

        if (bobberHasTouchedLiquid) return
        val isWater = when {
            bobber.isInLava && holdingLavaRod -> false
            bobber.isInWater && holdingWaterRod -> true
            else -> return
        }

        bobberHasTouchedLiquid = true
        FishingBobberInLiquidEvent(bobber, isWater).post()
    }

    @HandleEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        checkAndUpdateBaitFromInventory()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onOwnInventoryMenuUpdate(event: OwnInventoryMenuUpdateEvent) {
        extractAndPostBaitUpdate(event.itemStack)
    }

    private fun checkAndUpdateBaitFromInventory() {
        if (hasGuiOpen()) return
        val stack = InventoryUtils.getItemsInOwnInventoryWithNull()?.getOrNull(BAIT_HOTBAR_INDEX) ?: run {
            postEmptyBaitUpdate()
            return
        }
        extractAndPostBaitUpdate(stack)
    }

    private fun extractAndPostBaitUpdate(stack: SafeItemStack) {
        if (!stack.isBait()) {
            postEmptyBaitUpdate()
            return
        }

        val baitAmount = stack.getLoreComponent().asSequence()
            .map { it.formattedTextCompatLessResets() }
            .firstNotNullOfOrNull { lineText ->
                baitRemainingPattern.matchMatcher(lineText.removeColor()) { group("amount").formatInt() }
            } ?: return

        val baitType = BaitType(stack.hoverName.formattedTextCompatLessResets(), stack.getInternalName())
        postBaitUpdate(baitType, baitAmount, stack)
    }

    private fun postBaitUpdate(baitType: BaitType?, amount: Int, itemStack: SafeItemStack) {
        if (currentBait?.internalName == baitType?.internalName && currentBaitAmount == amount) return
        currentBait = baitType
        currentBaitAmount = amount
        BaitUpdateEvent(currentBait, currentBaitAmount, itemStack).post()
    }

    private fun postEmptyBaitUpdate() {
        postBaitUpdate(null, 0, SafeItemStack.EMPTY)
    }

    private fun hasGuiOpen(): Boolean {
        val screen = Minecraft.getInstance().screen
        return screen is AbstractContainerScreen<*> && screen !is InventoryScreen
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onPlaySound(event: PlaySoundEvent) {
        if (!holdingRod) return
        if (event.soundName == "entity.experience_orb.pickup" && event.volume == .5F) lastCatchSound = SimpleTimeMark.now()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onClick(event: WorldClickEvent) {
        if (event.clickType != InteractClickType.RIGHT_CLICK || !holdingRod || !bobberHasTouchedLiquid) return
        if (lastReelTime.passedSince() < .3.seconds) return
        lastReelTime = SimpleTimeMark.now()
    }

    fun SafeItemStack.isFishingRod() = getInternalName().isFishingRod()
    fun NeuInternalName.isFishingRod() = isLavaRod() || isWaterRod()

    fun NeuInternalName.isLavaRod() = this in lavaRods

    fun NeuInternalName.isWaterRod() = this in waterRods

    fun SafeItemStack.getFishingRodPart(part: RodPart): NeuInternalName? {
        val rodPartName = getExtraAttributes()?.getCompoundOrDefault(part.tagName)?.getStringOrDefault("part")
        if (rodPartName.isNullOrEmpty()) return null
        return rodPartName.toInternalName()
    }

    fun SafeItemStack.isBait(): Boolean {
        val category = getItemCategoryOrNull() ?: return false
        if (category == ItemCategory.BAIT) return true
        val internalName = getInternalNameOrNull() ?: return false
        return internalName in obfuscatedBaits
    }

    @HandleEvent
    fun onItemInHandChange(event: ItemInHandChangeEvent) {
        val wasHoldingRod = holdingRod
        // TODO correct rod type per island water/lava
        holdingRod = event.newItem.isFishingRod()
        holdingLavaRod = event.newItem.isLavaRod()
        holdingWaterRod = event.newItem.isWaterRod()

        if (holdingRod) {
            // If the player is not holding a rod, we want to just save the last state
            hasTreasureHook = InventoryUtils.getItemInHand()?.getFishingRodPart(RodPart.HOOK) == TREASURE_HOOK

            // Check bait when switching to a fishing rod
            checkAndUpdateBaitFromInventory()
        } else if (wasHoldingRod) {
            postEmptyBaitUpdate()
        }
    }

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<ItemsJson>("Items")
        lavaRods = data.lavaFishingRods
        waterRods = data.waterFishingRods
    }

    private fun getAllowedBlocks() = if (holdingLavaRod) lavaBlocks else waterBlocks

    fun getFilletPerTrophy(internalName: NeuInternalName): Int {
        val internal = internalName.asString()
        val trophyFishName = internal.substringBeforeLast("_")
            .replace("_", "").lowercase()
        val trophyRarityName = internal.substringAfterLast("_")
        val info = TrophyFishManager.getInfo(trophyFishName)
        val rarity = TrophyRarity.getByName(trophyRarityName) ?: TrophyRarity.BRONZE
        return info?.getFilletValue(rarity) ?: 0
    }

    fun isFishing(checkRodInHand: Boolean = true) =
        (IsFishingDetection.isFishing || (checkRodInHand && holdingRod)) && !DungeonApi.inDungeon()

    fun seaCreatureCount(entity: ArmorStand): Int {
        if (countIsZero(entity)) return 0

        return when (entity.name.string) {
            "Sea Emperor", "Rider of the Deep" -> 2

            else -> 1
        }
    }

    private val frostyNpcLocation = LorenzVec(-1.5, 76.0, 92.5)

    private fun countIsZero(entity: ArmorStand): Boolean {
        val name = entity.name.formattedTextCompatLessResets()
        // a dragon, will always be fought
        if (name == "Reindrake") return true

        // a npc shop
        if (name == "§5Frosty the Snow Blaster") return true

        if (name == "Frosty") {
            if (entity.getLorenzVec().distance(frostyNpcLocation) < 1) {
                return true
            }
        }

        val isSummonedSoul = name.contains("'")
        val hasFishingMobName = SeaCreatureManager.allFishingMobs.keys.any { name.contains(it) }
        return !hasFishingMobName || isSummonedSoul
    }

    private fun isWearingTrophyArmor(): Boolean =
        InventoryUtils.getArmor()
            .mapNotNull { stack ->
                val internalName = stack?.getInternalName()?.asString() ?: return@mapNotNull null
                trophyArmorNames.matchMatcher(internalName) { group("tier") }
            }
            .groupingBy { it }
            .eachCount()
            .any { (_, count) -> count >= 2 }

    fun isWearingEmberArmor(): Boolean =
        InventoryUtils.getArmor().all {
            emberArmorNames.matches(it?.getInternalName()?.asString())
        }
}
