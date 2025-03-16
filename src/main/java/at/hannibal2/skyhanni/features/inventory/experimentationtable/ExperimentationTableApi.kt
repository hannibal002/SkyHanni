package at.hannibal2.skyhanni.features.inventory.experimentationtable

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.storage.ResettableStorageSet
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.PetApi
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.experiments.RareSuperpairUncoverEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.wearingSkullTexture
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.InventoryUtils.openInventoryName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SkullTextureHolder
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.entity.item.EntityArmorStand

@SkyHanniModule
object ExperimentationTableApi {

    private val patternGroup = RepoPattern.group("enchanting.experiments")
    private val storage get() = ProfileStorageData.profileSpecific?.experimentation
    private val EXPERIMENTATION_TABLE_SKULL by lazy { SkullTextureHolder.getTexture("EXPERIMENTATION_TABLE") }
    private val inTable get() = inventoriesPattern.matches(openInventoryName())

    class ExperimentationDataSet(
        var type: ExperimentTaskType? = null,
        var tier: ExperimentTier? = null,
        var enchantingXpGained: Long? = null,
        var otherRewards: Map<NeuInternalName, Int>? = null,
        var rareFoundFired: Boolean = false,
    ) : ResettableStorageSet()
    var currentData = ExperimentationDataSet()

    val currentExperimentTier get() = currentData.tier

    val superpairInventory = InventoryDetector(
        openInventory = { name ->
            currentData.tier = superpairsPattern.matchMatcher(name) {
                ExperimentTier.byNameOrNone(group("experiment"))
            }
        },
    ) { name -> inventoriesPattern.matches(name) }

    // <editor-fold desc="Patterns">
    val instantFindNamePattern by patternGroup.pattern(
        "powerups.instantfind.name",
        "Instant Find",
    )

    val waitingMessagesPattern by patternGroup.pattern(
        "waiting.messages",
        "Click any button!|Click a second button!|Next button is instantly rewarded!",
    )

    /**     * REGEX-TEST: Superpairs (Metaphysical)

     */
    private val superpairsPattern by patternGroup.pattern(
        "superpairs",
        "Superpairs \\((?<experiment>\\w+)\\)",

    )
    /**
     * REGEX-TEST: Gained +3 Clicks
     */
    val powerUpPattern by patternGroup.pattern(
        "powerups",
        "Gained \\+\\d Clicks?|Instant Find|\\+\\S* XP",
    )

    /**
     * REGEX-TEST: 123k Enchanting Exp
     * REGEX-TEST: Titanic Experience Bottle
     */
    val rewardPattern by patternGroup.pattern(
        "rewards",
        "\\d{1,3}k Enchanting Exp|Enchanted Book|(?:Titanic |Grand |\\b)Experience Bottle|Metaphysical Serum|ExperimentTier the Fish",
    )

    /**
     * REGEX-TEST: Superpairs (Metaphysical)
     * REGEX-TEST: Chronomatron (Metaphysical)
     */
    val inventoriesPattern by patternGroup.pattern(
        "inventories",
        "(?:Superpairs|Chronomatron|Ultrasequencer) (?:\\(.+\\)|➜ Stakes|Rewards)|Experimentation Table",
    )

    /**
     * REGEX-TEST:  +42,000 Enchanting Exp
     */
    val enchantingExpChatPattern by patternGroup.pattern(
        "chatexp",
        "^ \\+(?<amount>\\d+|\\d+,\\d+)k? Enchanting Exp$",
    )

    /**
     * REGEX-TEST:  +Smite VII
     * REGEX-TEST:  +42,000 Enchanting Exp
     */
    val experimentsDropPattern by patternGroup.pattern(
        "drop",
        "^ \\+(?<reward>.*)\$",
    )

    /**
     * REGEX-TEST: You claimed the Superpairs rewards!
     */
    val claimMessagePattern by patternGroup.pattern(
        "claim",
        "You claimed the \\S+ rewards!",
    )

    /**
     * REGEX-TEST: 131k Enchanting Exp
     * REGEX-TEST: 42,000 Enchanting Exp
     */
    val enchantingExpPattern by patternGroup.pattern(
        "exp",
        "(?<amount>\\d+|\\d+,\\d+)k? Enchanting Exp",
    )

    /**
     * REGEX-TEST: Titanic Experience Bottle
     */
    val experienceBottleChatPattern by patternGroup.pattern(
        "chat.xpbottle",
        "(?:Colossal |Titanic |Grand |\\b)Experience Bottle",
    )

    /**
     * REGEX-TEST: TITANIC_EXP_BOTTLE
     */
    val experienceBottlePattern by patternGroup.pattern(
        "xpbottle",
        "(?:COLOSSAL_|TITANIC_|GRAND_|\\b)EXP_BOTTLE",
    )

    /**
     * REGEX-TEST: Remaining Clicks: 22
     */
    val remainingClicksPattern by patternGroup.pattern(
        "clicks",
        "Remaining Clicks: (?<clicks>\\d+)",
    )

    /**
     * REGEX-TEST: ☕ You bought a bonus charge for the Experimentation Table! (1/3)
     */
    val experimentRenewPattern by patternGroup.pattern(
        "renew",
        "☕ You bought a bonus charge for the Experimentation Table! \\((?<current>\\d)/3\\)",
    )

    /**
     * REGEX-TEST: §d§kXX§5 ULTRA-RARE BOOK! §d§kXX
     */
    val ultraRarePattern by patternGroup.pattern(
        "ultrarare",
        "§d§kXX§5 ULTRA-RARE BOOK! §d§kXX",
    )

    /**
     * REGEX-TEST: §9Smite VII
     */
    val bookPattern by patternGroup.pattern(
        "book",
        "§9(?<enchant>.*)",
    )

    /**
     * REGEX-TEST: §dGuardian
     * REGEX-TEST: §9Guardian§e
     */
    private val guardianPetNamePattern by patternGroup.pattern(
        "guardianpet",
        "§[956d]Guardian.*",
    )
    // </editor-fold>

    fun inDistanceToTable(max: Double): Boolean {
        val vec = LorenzVec.getBlockBelowPlayer()
        return storage?.tablePos?.let { it.distance(vec) <= max } ?: false
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!inTable) return currentData.reset()

        updateTablePos()
        fireUncoveredRare(event)
    }

    private fun fireUncoveredRare(event: InventoryUpdatedEvent) {
        if (currentData.isEqualToDefault() || currentData.rareFoundFired) return

        if (!fireRareBookUncovered(event)) return

        currentData.rareFoundFired = true
    }

    private fun fireRareBookUncovered(event: InventoryUpdatedEvent): Boolean {
        for (lore in event.inventoryItems.map { it.value.getLore() }) {
            val firstLine = lore.firstOrNull() ?: continue
            if (!ultraRarePattern.matches(firstLine)) continue
            val bookNameLine = lore.getOrNull(2) ?: continue
            bookPattern.matchMatcher(bookNameLine) {
                val enchant = group("enchant")
                RareSuperpairUncoverEvent(enchant).post()
                return true
            }
        }
        return false
    }

    private fun updateTablePos() {
        storage?.tablePos = EntityUtils.getEntities<EntityArmorStand>().find {
            it.wearingSkullTexture(EXPERIMENTATION_TABLE_SKULL)
        }?.getLorenzVec().takeIf { it != storage?.tablePos } ?: return
    }

    fun hasGuardianPet(): Boolean = guardianPetNamePattern.matches(PetApi.currentPet)
}
