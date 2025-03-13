package at.hannibal2.skyhanni.features.event.yearofthepig

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.storage.ResettableStorageSet
import at.hannibal2.skyhanni.data.ClickType
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
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.MobUtils.mob
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatIntOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchGroup
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.passive.EntityPig
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object PigFeaturesApi {
    class ShinyOrbDataSet(
        var pigEntityId: Int? = null,
        var shinyOrbEntityId: Int? = null,
        private var shinyOrbLocationCache: LorenzVec? = null
    ) : ResettableStorageSet() {
        private val shinyOrbEntity get() = shinyOrbEntityId?.let {
            EntityUtils.getEntityByID(it) as EntityArmorStand?
        }
        val shinyOrbLocation get() = shinyOrbLocationCache ?: shinyOrbEntity?.getLorenzVec()?.let {
            shinyOrbLocationCache = it
            it
        }
    }

    private val patternGroup = RepoPattern.group("event.year-of-the-pig")
    private val armorStands get() = EntityUtils.getEntities<EntityArmorStand>()
    private val writableDataSet: ShinyOrbDataSet = ShinyOrbDataSet()
    val dataSet get() = writableDataSet

    // <editor-fold desc="Patterns">
    private val orbUsedChatPattern by patternGroup.pattern(
        "chat.orb.used",
        "§dOink! §r§eBring the pig back to the §r§6Shiny Orb§r§e!"
    )

    private val orbChargedChatPattern by patternGroup.pattern(
        "chat.orb.charged",
        "§6§lSHINY! §r§eThe orb is charged! Click on it for loot!"
    )

    private val orbExpiredChatPattern by patternGroup.pattern(
        "chat.orb.expired",
        "§cYour Shiny Orb and associated pig expired and disappeared\\."
    )

    private val chargedOrbTagPattern by patternGroup.pattern(
        "entity.tag.charged.name",
        "§e§lCLICK"
    )

    /**
     * REGEX-TEST: §6[MVP§3++§6] oBlazin§f§r
     */
    private val orbTagPattern by patternGroup.pattern(
        "entity.tag.normal.name",
        "(?:(?:§.)+\\[.*] )?(?<player>[^§]+)(?:§.)+"
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
        "§6§lSHINY! §r§eYou extracted (?:§.)+(?<reward>.*) §r§efrom the piglet's orb!"
    )

    /**
     * REGEX-TEST: +9,721 Coins
     */
    private val coinsRewardPattern by patternGroup.pattern(
        "orb.reward.coins",
        "\\+(?<amount>[\\d,]+) Coins"
    )

    /**
     * REGEX-TEST: +1,000 Mining XP
     * REGEX-TEST: +1,000 Alchemy XP
     */
    private val skillXpRewardPattern by patternGroup.pattern(
        "orb.reward.skillxp",
        "\\+(?<amount>[\\d,]+) (?<skill>.*) XP"
    )
    // </editor-fold>

    @HandleEvent
    fun onWorldChange(event: WorldChangeEvent) {
        writableDataSet.reset()
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent) {
        val playerVec = LocationUtils.playerLocation()
        val message = event.message
        orbUsedChatPattern.matchMatcher(message) {
            ShinyOrbUsedEvent(writableDataSet).post()
        }

        orbChargedChatPattern.matchMatcher(message) {
            val orbEntity = tryFindPlayerOrb(playerVec)
            ShinyOrbChargedEvent(writableDataSet.shinyOrbLocation, orbEntity?.entityId).post()
        }

        orbLootedChatPattern.matchMatcher(message) {
            handleLootedOrb(group("reward"))
            writableDataSet.reset()
        }

        orbExpiredChatPattern.matchMatcher(message) {
            val pigId = writableDataSet.pigEntityId ?: return@matchMatcher
            val pigEntity = EntityUtils.getEntityByID(pigId)
            if (pigEntity == null) writableDataSet.reset()
        }
    }

    private fun tryFindPlayerOrb(
        location: LorenzVec
    ): EntityArmorStand? = writableDataSet.shinyOrbEntityId?.let {
        EntityUtils.getEntityByID(it) as EntityArmorStand?
    } ?: armorStands.firstOrNull {
        it.distanceTo(location) <= 3.0 && armorStands.any { labelArmorStand ->
            val tagMatchesIgn = orbTagPattern.matchGroup(labelArmorStand.name, "player") == LorenzUtils.getPlayerName()
            val tagCharged = chargedOrbTagPattern.matches(labelArmorStand.name)
            val tagMatches = tagMatchesIgn || tagCharged

            tagMatches && labelArmorStand.distanceTo(location) <= 3.0 && labelArmorStand.entityId != it.entityId
        }
    }

    private fun handleLootedOrb(reward: String) {
        val shinyOrbLocation = writableDataSet.shinyOrbLocation ?: return

        coinsRewardPattern.matchMatcher(reward) {
            val amount = group("amount").formatIntOrNull() ?: return@matchMatcher
            ShinyOrbLootedEvent(shinyOrbLocation, coins = amount).post()
            return writableDataSet.reset()
        }

        skillXpRewardPattern.matchMatcher(reward) {
            val amount = group("amount").formatIntOrNull() ?: return@matchMatcher
            val skill = SkillType.getByNameOrNull(group("skill")) ?: return@matchMatcher
            ShinyOrbLootedEvent(shinyOrbLocation, skillXp = skill to amount.toLong()).post()
            return writableDataSet.reset()
        }

        val (lootName, lootAmount) = ItemUtils.readItemAmount(reward) ?: return
        val lootInternalName = NeuInternalName.fromItemNameOrNull(lootName) ?: run {
            ErrorManager.skyHanniError("Could not find internal name for §c\"$lootName§c\"")
        }
        ShinyOrbLootedEvent(shinyOrbLocation, loot = lootInternalName to lootAmount).post()
        writableDataSet.reset()
    }

    @HandleEvent(onlyOnIsland = IslandType.HUB)
    fun onEntityClick(event: EntityClickEvent) {
        if (event.clickType != ClickType.RIGHT_CLICK) return
        val entity = event.clickedEntity ?: return

        if (entity is EntityPig && entity.mob?.name == "SHINY PIG") entity.handlePigClick()
    }

    private fun EntityPig.handlePigClick() {
        val pigStartingLocation = this.getLorenzVec()
        DelayedRun.runDelayed(1.seconds) {
            if (writableDataSet.pigEntityId == this.entityId) return@runDelayed
            val orbEntity = tryFindPlayerOrb(pigStartingLocation) ?: return@runDelayed
            writableDataSet.reset()
            writableDataSet.shinyOrbEntityId = orbEntity.entityId
            writableDataSet.pigEntityId = this.entityId
        }
    }
}
