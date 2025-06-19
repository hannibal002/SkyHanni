package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.jsonobjects.repo.ItemsJson
import at.hannibal2.skyhanni.events.ItemInHandChangeEvent
import at.hannibal2.skyhanni.events.PlaySoundEvent
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
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getExtraAttributes
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat.isLocalPlayer
import at.hannibal2.skyhanni.utils.compat.addLavas
import at.hannibal2.skyhanni.utils.compat.addWaters
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.projectile.EntityFishHook
import net.minecraft.item.ItemStack
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

    /**
     * REGEX-TEST: BRONZE_HUNTER_HELMET
     * REGEX-TEST: SILVER_HUNTER_CHESTPLATE
     * REGEX-TEST: GOLD_HUNTER_LEGGINGS
     * REGEX-TEST: DIAMOND_HUNTER_BOOTS
     */
    private val trophyArmorNames by RepoPattern.pattern(
        "fishing.trophyfishing.armor",
        "(?:BRONZE|SILVER|GOLD|DIAMOND)_HUNTER_(?:HELMET|CHESTPLATE|LEGGINGS|BOOTS)",
    )

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

    var bobber: EntityFishHook? = null
        private set
    var bobberHasTouchedLiquid = false
        private set

    var wearingTrophyArmor = false
        private set

    @HandleEvent(onlyOnSkyblock = true)
    fun onJoinWorld(event: EntityEnterWorldEvent<EntityFishHook>) {
        if (!holdingRod) return
        if (event.entity.angler?.isLocalPlayer == false) return

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
        }

        val bobber = bobber ?: return
        if (bobber.isDead) {
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

    @HandleEvent(onlyOnSkyblock = true)
    fun onPlaySound(event: PlaySoundEvent) {
        if (!holdingRod) return
        if (event.soundName == "random.orb" && event.volume == .5F) lastCatchSound = SimpleTimeMark.now()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onClick(event: WorldClickEvent) {
        if (event.clickType != ClickType.RIGHT_CLICK || !holdingRod || !bobberHasTouchedLiquid) return
        if (lastReelTime.passedSince() < .3.seconds) return
        lastReelTime = SimpleTimeMark.now()
    }

    fun ItemStack.isFishingRod() = getInternalName().isFishingRod()
    fun NeuInternalName.isFishingRod() = isLavaRod() || isWaterRod()

    fun NeuInternalName.isLavaRod() = this in lavaRods

    fun NeuInternalName.isWaterRod() = this in waterRods

    fun ItemStack.getFishingRodPart(part: RodPart): NeuInternalName? =
        getExtraAttributes()?.getCompoundTag(part.tagName)?.getString("part")?.toInternalName()

    fun ItemStack.isBait(): Boolean = stackSize == 1 && getItemCategoryOrNull() == ItemCategory.BAIT

    @HandleEvent
    fun onItemInHandChange(event: ItemInHandChangeEvent) {
        // TODO correct rod type per island water/lava
        holdingRod = event.newItem.isFishingRod()
        holdingLavaRod = event.newItem.isLavaRod()
        holdingWaterRod = event.newItem.isWaterRod()

        if (holdingRod) {
            // If the player is not holding a rod, we want to just save the last state
            hasTreasureHook = InventoryUtils.getItemInHand()?.getFishingRodPart(RodPart.HOOK) == TREASURE_HOOK
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

    fun seaCreatureCount(entity: EntityArmorStand): Int {
        if (countIsZero(entity)) return 0

        return when (entity.name) {
            "Sea Emperor", "Rider of the Deep" -> 2

            else -> 1
        }
    }

    private val frostyNpcLocation = LorenzVec(-1.5, 76.0, 92.5)

    private fun countIsZero(entity: EntityArmorStand): Boolean {
        val name = entity.name
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
        //#if TODO
        InventoryUtils.getArmor().all {
            trophyArmorNames.matches(it?.getInternalName()?.asString())
        }
    //#else
    //$$ false
    //#endif
}
