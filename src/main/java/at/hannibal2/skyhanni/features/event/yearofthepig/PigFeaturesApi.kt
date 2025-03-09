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
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.matchGroup
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.passive.EntityPig
import java.util.regex.Matcher
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object PigFeaturesApi {
    class ShinyOrbDataSet(
        var shinyOrbEntityId: Int? = null,
        var clickedPigEntityId: Int? = null,
        var beingClicked: Boolean = false
    ) : ResettableStorageSet() {
        private val pigEntity get() = clickedPigEntityId?.let {
            EntityUtils.getEntityByID(it) as EntityPig?
        }
        val pigLocation get() = pigEntity?.getLorenzVec()

        private val shinyOrbEntity get() = shinyOrbEntityId?.let {
            EntityUtils.getEntityByID(it) as EntityArmorStand?
        }
        val shinyOrbLocation get() = shinyOrbEntity?.getLorenzVec()
    }

    private val patternGroup = RepoPattern.group("event.year-of-the-pig")
    private val armorStands get() = EntityUtils.getEntities<EntityArmorStand>()
    private val dataSets: MutableList<ShinyOrbDataSet> = mutableListOf()
    val activeDataSet = dataSets.minByOrNull {
        it.pigLocation?.distanceToPlayer() ?: Double.MAX_VALUE
    } ?: dataSets.lastOrNull()

    private fun LorenzVec.findClosestDataSet(
        acceptableMargin: Double = 5.0
    ) = dataSets.minByOrNull {
        it.shinyOrbLocation?.distance(this) ?: Double.MAX_VALUE
    }?.takeIf {
        (it.shinyOrbLocation?.distance(this) ?: Double.MAX_VALUE) <= acceptableMargin
    }

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

    private val orbNamePattern by patternGroup.pattern(
        "entity.orb.name",
        "§6§lSHINY ORB"
    )

    private val chargedOrbTagPattern by patternGroup.pattern(
        "entity.tag.charged.name",
        "§e§lCLICK"
    )

    private val shinyPigTagPattern by patternGroup.pattern(
        "entity.tag.pig.name",
        "§6§lSHINY PIG"
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
     */
    private val orbLootedChatPattern by patternGroup.pattern(
        "chat.orb.looted",
        "§6§lSHINY! §r§eYou extracted (?:§r)?(?<reward>.*) §r§efrom the piglet's orb!"
    )

    /**
     * REGEX-TEST: §r§6+9,721 Coins
     */
    private val coinsRewardPattern by patternGroup.pattern(
        "orb.reward.coins",
        "(?:§r)?§6\\+(?<amount>[\\d,]+) Coins"
    )

    /**
     * REGEX-TEST: §r§3+1,000 Mining XP
     */
    private val skillXpRewardPattern by patternGroup.pattern(
        "orb.reward.skillxp",
        "(?:§r)?§.\\+(?<amount>[\\d,]+) (?<skill>.*) XP"
    )
    // </editor-fold>

    @HandleEvent
    fun onWorldChange(event: WorldChangeEvent) {
        dataSets.clear()
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent) {
        val playerVec = LocationUtils.playerLocation()
        val message = event.message
        orbUsedChatPattern.matchMatcher(message) {
            val dataSet = activeDataSet ?: return@matchMatcher
            ShinyOrbUsedEvent(dataSet).post()
        }

        orbChargedChatPattern.matchMatcher(message) {
            val orbEntity = tryFindPlayerOrb(playerVec)
            val dataSet = dataSets.first { it.shinyOrbEntityId == orbEntity?.entityId }
            ShinyOrbChargedEvent(dataSet.shinyOrbLocation, orbEntity?.entityId).post()
        }

        orbLootedChatPattern.matchMatcher(message) {
            this.handleLootedOrb(event)
            val dataSet = playerVec.findClosestDataSet() ?: return@matchMatcher
            dataSets.remove(dataSet)
        }

        orbExpiredChatPattern.matchMatcher(message) {
            // Always remove the "oldest" dataset
            dataSets.removeFirst()
        }
    }

    private fun tryFindPlayerOrb(
        location: LorenzVec
    ): EntityArmorStand? = location.findClosestDataSet()?.shinyOrbEntityId?.let {
        EntityUtils.getEntityByID(it) as EntityArmorStand?
    } ?: armorStands.firstOrNull {
        it.distanceTo(location) <= 3.0 && armorStands.any { labelArmorStand ->
            val tagMatchesIgn = orbTagPattern.matchGroup(labelArmorStand.name, "player") == LorenzUtils.getPlayerName();
            val tagCharged = chargedOrbTagPattern.matches(labelArmorStand.name)
            val tagMatches = tagMatchesIgn || tagCharged

            tagMatches && labelArmorStand.distanceTo(location) <= 3.0 && labelArmorStand.entityId != it.entityId
        }
    }

    private fun Matcher.handleLootedOrb(event: SkyHanniChatEvent) {
        ChatUtils.chat("in loot handle")
        val dataSet = dataSets.firstOrNull { it.beingClicked } ?: LocationUtils.playerLocation().findClosestDataSet() ?: return
        ChatUtils.chat("in loot handle, found dataset: $dataSet")
        val reward = group("reward")
        val shinyOrbLocation = dataSet.shinyOrbLocation ?: return
        ChatUtils.chat("in loot handle, found location: $shinyOrbLocation")

        coinsRewardPattern.matchMatcher(reward) {
            val amount = group("amount").toIntOrNull() ?: return@matchMatcher
            ShinyOrbLootedEvent(shinyOrbLocation, coins = amount).post()
            return
        }

        skillXpRewardPattern.matchMatcher(reward) {
            val amount = group("amount").toIntOrNull() ?: return@matchMatcher
            val skill = SkillType.getByNameOrNull(group("skill")) ?: return@matchMatcher
            ShinyOrbLootedEvent(shinyOrbLocation, skillXp = skill to amount.toLong()).post()
            return
        }

        val (lootName, lootAmount) = ItemUtils.readItemAmount(event.message) ?: return
        val lootInternalName = NeuInternalName.fromItemNameOrNull(lootName) ?: return
        ShinyOrbLootedEvent(shinyOrbLocation, loot = lootInternalName to lootAmount).post()
    }

    @HandleEvent(onlyOnIsland = IslandType.HUB)
    fun onEntityClick(event: EntityClickEvent) {
        if (event.clickType != ClickType.RIGHT_CLICK) return
        val entity = event.clickedEntity ?: return

        if (entity is EntityPig && entity.getPigTagEntity() != null) entity.handlePigClick()
        if (entity is EntityArmorStand && orbNamePattern.matches(entity.name)) entity.handleOrbClick()
    }

    private fun EntityPig.getPigTagEntity() = armorStands.firstOrNull { tagArmorStand ->
        tagArmorStand.distanceTo(this.getLorenzVec()) <= 5.0 &&
            shinyPigTagPattern.matches(tagArmorStand.name)
    }

    private fun EntityPig.handlePigClick() {
        val locationNow = this.getLorenzVec()
        if (dataSets.any { it.clickedPigEntityId == this.entityId }) return
        dataSets.add(ShinyOrbDataSet(
            clickedPigEntityId = this.entityId,
        ))
        DelayedRun.runDelayed(1.seconds) {
            val orbEntity = tryFindPlayerOrb(locationNow) ?: return@runDelayed
            dataSets.last().apply {
                shinyOrbEntityId = orbEntity.entityId
            }
            ChatUtils.chat("Updated dataset for pig: ${dataSets.last()}")
        }
        ChatUtils.chat("Added new dataset for pig: ${dataSets.last()}")
    }

    private fun EntityArmorStand.handleOrbClick() {
        dataSets.firstOrNull { it.shinyOrbEntityId == this.entityId }?.beingClicked = true
    }
}
