package at.hannibal2.skyhanni.features.event.yearofthepig

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.entity.EntityClickEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.events.yearofthepig.ShinyOrbChargedEvent
import at.hannibal2.skyhanni.events.yearofthepig.ShinyOrbLootedEvent
import at.hannibal2.skyhanni.events.yearofthepig.ShinyOrbUsedEvent
import at.hannibal2.skyhanni.features.skillprogress.SkillType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.wearingSkullTexture
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.MobUtils.mob
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatIntOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkullTextureHolder
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.passive.EntityPig
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object PigFeaturesApi {

    class ShinyOrbData(
        val pigEntityId: Int,
        val shinyOrbEntityId: Int,
        val shinyOrbLocation: LorenzVec,
        val spawnTime: SimpleTimeMark = SimpleTimeMark.farPast(),
    )

    private val patternGroup = RepoPattern.group("event.year-of-the-pig")

    private val SHINY_ORB_ITEM = "SHINY_ORB".toInternalName()

    private val data: MutableList<ShinyOrbData> = mutableListOf()
    val dataSetList get() = data

    private fun tryToRemoveOrb() {
        DelayedRun.runDelayed(1.seconds) {
            dataSetList.removeIf { dataSet ->
                val pigId = dataSet.pigEntityId
                EntityUtils.getEntityByID(pigId) == null && dataSet.spawnTime.passedSince() > 2.seconds
            }
        }
    }

    // <editor-fold desc="Patterns">
    private val orbChargedChatPattern by patternGroup.pattern(
        "chat.orb.charged",
        "§6§lSHINY! §r§eThe orb is charged! Click on it for loot!",
    )

    private val orbExpiredChatPattern by patternGroup.pattern(
        "chat.orb.expired",
        "§cYour Shiny Orb and associated pig expired and disappeared\\.",
    )

    /**
     * REGEX-TEST: §6§lSHINY! §r§eYou extracted §r§3+1,000 Mining XP §r§efrom the piglet's orb!
     * REGEX-TEST: §6§lSHINY! §r§eYou extracted §r§5Potato Spreading §r§efrom the piglet's orb!
     * REGEX-TEST: §6§lSHINY! §r§eYou extracted §r§b3x §r§aGrand Experience Bottle §r§efrom the piglet's orb!
     * REGEX-TEST: §6§lSHINY! §r§eYou extracted §r§6+9,721 Coins §r§efrom the piglet's orb!
     * REGEX-TEST: §6§lSHINY! §r§eYou extracted §r§5Farming for Dummies §r§efrom the piglet's orb!
     * REGEX-TEST: §6§lSHINY! §r§eYou extracted §r§3+1,000 Alchemy XP §r§efrom the piglet's orb!
     * REGEX-TEST: §6§lSHINY! §r§eYou extracted §r§9Harvesting VI §r§efrom the piglet's orb!
     * REGEX-TEST: §6§lSHINY! §r§eYou extracted §r§a8x Enchanted Pork §r§efrom the piglet's orb!
     */
    private val orbLootedChatPattern by patternGroup.pattern(
        "chat.orb.looted",
        "§6§lSHINY! §r§eYou extracted (?:§.)+(?<reward>.*) §r§efrom the piglet's orb!",
    )

    /**
     * REGEX-TEST: +3,000 Coins
     * REGEX-TEST: +9,721 Coins
     */
    private val coinsRewardPattern by patternGroup.pattern(
        "orb.reward.coins",
        "\\+(?<amount>[\\d,]+) Coins",
    )

    /**
     * REGEX-TEST: +1,000 Mining XP
     * REGEX-TEST: +1,000 Alchemy XP
     */
    private val skillXpRewardPattern by patternGroup.pattern(
        "orb.reward.skillxp",
        "\\+(?<amount>[\\d,]+) (?<skill>.*) XP",
    )
    // </editor-fold>

    private const val YEAR_OF_THE_PIG_OFFSET = 11

    fun isYearOfThePig(): Boolean {
        val sbYear = SkyBlockTime.now().year
        val yearOffset = sbYear % 12
        return yearOffset == YEAR_OF_THE_PIG_OFFSET || SkyHanniMod.feature.dev.debug.alwaysYearOfThePig
    }

    @HandleEvent
    fun onWorldChange(event: WorldChangeEvent) {
        data.clear()
    }

    @HandleEvent(onlyOnIsland = IslandType.HUB)
    fun onChat(event: SkyHanniChatEvent) {
        if (!isYearOfThePig()) return
        val message = event.message

        orbChargedChatPattern.matchMatcher(message) {
            val orbEntity = tryFindOrb(LocationUtils.playerLocation())
            ShinyOrbChargedEvent(orbEntity?.entityId).post()
        }

        orbLootedChatPattern.matchMatcher(message) {
            handleLootedOrb(group("reward"))
            tryToRemoveOrb()
        }

        orbExpiredChatPattern.matchMatcher(message) {
            val oldestData = dataSetList.maxByOrNull { it.spawnTime.passedSince() } ?: return@matchMatcher

            val pigId = oldestData.pigEntityId
            val pigEntity = EntityUtils.getEntityByID(pigId)
            if (pigEntity == null) data.removeIf { it.pigEntityId == pigId }
        }
    }

    private val ORB_SKULL by lazy { SkullTextureHolder.getTexture("SHINY_PIG_ORB") }

    private fun tryFindOrb(location: LorenzVec): EntityArmorStand? {
        val nearbyStands = EntityUtils.getEntitiesNearby<EntityArmorStand>(location, 5.0).toList()
        val sortedStands = nearbyStands.sortedBy { it.distanceTo(location) }
        return sortedStands.firstOrNull { stand ->
            stand.wearingSkullTexture(ORB_SKULL)
        }
    }

    private fun handleLootedOrb(reward: String) {
        coinsRewardPattern.matchMatcher(reward) {
            val amount = group("amount").formatIntOrNull() ?: return@matchMatcher
            ShinyOrbLootedEvent(coins = amount).post()
            return tryToRemoveOrb()
        }

        skillXpRewardPattern.matchMatcher(reward) {
            val amount = group("amount").formatIntOrNull() ?: return@matchMatcher
            val skill = SkillType.getByNameOrNull(group("skill")) ?: return@matchMatcher
            ShinyOrbLootedEvent(skillXp = skill to amount.toLong()).post()
            return tryToRemoveOrb()
        }

        val (lootName, lootAmount) = ItemUtils.readItemAmount(reward) ?: return
        val lootInternalName = NeuInternalName.fromItemNameOrNull(lootName) ?: run {
            ErrorManager.skyHanniError("Could not find internal name for §c\"$lootName§c\"")
        }
        ShinyOrbLootedEvent(loot = lootInternalName to lootAmount).post()
        tryToRemoveOrb()
    }

    @HandleEvent(onlyOnIsland = IslandType.HUB)
    fun onEntityClick(event: EntityClickEvent) {
        if (!isYearOfThePig()) return

        if (InventoryUtils.getItemInHand()?.getInternalNameOrNull() != SHINY_ORB_ITEM) return

        val entity = event.clickedEntity
        if (entity is EntityPig && entity.mob?.name == "SHINY PIG") entity.handlePigClick()
    }

    private fun EntityPig.handlePigClick() {
        val pigStartingLocation = this.getLorenzVec()
        DelayedRun.runDelayed(1.seconds) {
            if (dataSetList.any { it.pigEntityId == this.entityId }) return@runDelayed
            val orbEntity = tryFindOrb(pigStartingLocation)
            orbEntity ?: return@runDelayed
            data.add(
                ShinyOrbData(
                    pigEntityId = this.entityId,
                    shinyOrbEntityId = orbEntity.entityId,
                    shinyOrbLocation = orbEntity.getLorenzVec() + LorenzVec(0, 2, 0),
                    spawnTime = SimpleTimeMark.now(),
                ),
            )
            ShinyOrbUsedEvent().post()
        }
    }
}
