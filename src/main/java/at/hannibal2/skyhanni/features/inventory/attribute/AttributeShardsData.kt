package at.hannibal2.skyhanni.features.inventory.attribute

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.inventory.AttributeShardsConfig
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuAttributeShardData
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuAttributeShardJson
import at.hannibal2.skyhanni.events.NeuRepositoryReloadEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.orNull
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object AttributeShardsData {

    val config get(): AttributeShardsConfig = SkyHanniMod.feature.inventory.attributeShards
    private val storage get() = ProfileStorageData.profileSpecific?.attributeShards

    private var attributeLevelling = mapOf<LorenzRarity, List<Int>>()
    private var unconsumableAttributes = listOf<String>()
    private var attributeInfo = mapOf<String, NeuAttributeShardData>()
    private var internalNameToShard = mapOf<NeuInternalName, String>()
    private var attributeAbilityNameToShard = mapOf<String, String>()

    var maxShards = 0
        private set

    val attributeMenuInventory = InventoryDetector(
        openInventory = { DelayedRun.runNextTick { processAttributeMenuItems() } },
    ) { name -> name == "Attribute Menu" }
    val huntingBoxInventory = InventoryDetector(
        openInventory = { DelayedRun.runNextTick { processHuntingBoxItems() } },
    ) { name -> name == "Hunting Box" }
    val bazaarShardsInventory = InventoryDetector(
        pattern = "\\(\\d+/\\d+\\) Oddities ➜ Shards".toPattern(),
        openInventory = { DelayedRun.runNextTick { AttributeShardOverlay.updateDisplay() } },
    )

    private var lastSyphonedMessage = SimpleTimeMark.farPast()

    private val patternGroup = RepoPattern.group("inventory.attributeshards")

    /**
     * REGEX-TEST: §6Nature Elemental
     * REGEX-TEST: §6Berry Eater IX
     * REGEX-TEST: §6Essence of Ice I
     * REGEX-TEST: §6Advanced Mode
     */
    val attributeShardNamePattern by patternGroup.pattern(
        "name",
        "§6(?<name>.+?) ?(?<tier>[IVXL]+)?$",
    )

    /**
     * REGEX-TEST: §7Syphon §b3 §7more to level up!
     * REGEX-TEST: §7Syphon §b1 §7shard to unlock!
     * REGEX-TEST: §7Syphon §b1 §7more to level up!
     */
    private val syphonAmountPattern by patternGroup.pattern(
        "syphon.amount",
        "§7Syphon §b(?<amount>\\d+) §7(?:more to level up|shard to unlock)!",
    )

    /**
     * REGEX-TEST: §6Veil §8(Combat)
     * REGEX-TEST: §6Yummy X §8(Foraging)
     */
    private val attributeShardNameLorePattern by patternGroup.pattern(
        "name.lore",
        "§6(?<name>.+?) ?(?<tier>[IVXL]+)? §8\\(\\w+\\)$",
    )

    /**
     * REGEX-TEST: §7Owned: §b1 Shard
     * REGEX-TEST: §7Owned: §b3 Shards
     * REGEX-TEST: §7Owned: §b71 Shards
     * REGEX-TEST: §7Owned: §b1,729 Shards
     */
    val amountOwnedPattern by patternGroup.pattern(
        "owned",
        "§7Owned: §b(?<amount>[\\d,]+) Shards?",
    )

    /**
     * REGEX-TEST: §a+1 Arthropod Ruler Attribute §r§7(Level 1) - 2 more to upgrade!
     * REGEX-TEST: §a+1 Arthropod Ruler Attribute §r§7(Level 2) - 3 more to upgrade!
     * REGEX-TEST: §a+2 Essence of Ice Attribute §r§7(Level 2) - 1 more to upgrade!
     * REGEX-TEST: §a+6 Ender Ruler Attribute §r§7(Level 3) - 3 more to upgrade!
     * REGEX-FAIL: §a+43 Essence of Ice Attribute §r§7(Level 10) §r§a§lMAXED
     */
    private val shardSyphonedPattern by patternGroup.pattern(
        "chat.syphoned",
        "§a\\+\\d+ (?<attributeName>.+) Attribute §r§7\\(Level (?<level>\\d+)\\) - (?<untilNext>\\d+) more to upgrade!",
    )

    /**
     * REGEX-TEST: §a+43 Essence of Ice Attribute §r§7(Level 10) §r§a§lMAXED
     * REGEX-FAIL: §a+2 Essence of Ice Attribute §r§7(Level 2) - 1 more to upgrade!
     */
    private val shardSyphonedMaxedPattern by patternGroup.pattern(
        "chat.syphoned.maxed",
        "§a\\+\\d+ (?<attributeName>.+) Attribute §r§7\\(Level (?<level>\\d+)\\) §r§a§lMAXED",
    )

    /**
     * REGEX-TEST: §aand 7 more...
     */
    private val andMoreMessagePattern by patternGroup.pattern(
        "chat.and.more",
        "§aand (?<amount>\\d+) more\\.\\.\\.",
    )

    private val advancedModeNotUnlocked by patternGroup.pattern(
        "advanced.mode",
        "§7§cAdvanced Mode unlocked at 30",
    )

    @HandleEvent(priority = HandleEvent.LOWEST)
    fun onNEURepoReload(event: NeuRepositoryReloadEvent) {
        val attributesJson = event.getConstant<NeuAttributeShardJson>("attribute_shards")
        attributeLevelling = attributesJson.attributeLevelling
        unconsumableAttributes = attributesJson.unconsumableAttributes
        attributeInfo = attributesJson.attributes.associateBy { it.bazaarName.asString() }
        maxShards = attributeInfo.size - unconsumableAttributes.size
        internalNameToShard = attributeInfo.map { (name, info) ->
            info.internalName to name
        }.toMap()
        attributeAbilityNameToShard = attributeInfo.map { (name, info) ->
            info.abilityName to name
        }.toMap()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: SkyHanniChatEvent) {
        shardSyphonedPattern.matchMatcher(event.message) {
            val attributeName = group("attributeName")
            val level = group("level").toInt()
            val untilNext = group("untilNext").toInt()
            val shardName = attributeAbilityNameToShard[attributeName]
                ?: ErrorManager.skyHanniError("Unknown attribute shard name for ability: $attributeName")
            val shardInternalName = shardNameToInternalName(shardName)
            processShard(shardInternalName, level, untilNext)
            lastSyphonedMessage = SimpleTimeMark.now()
            return
        }
        shardSyphonedMaxedPattern.matchMatcher(event.message) {
            val attributeName = group("attributeName")
            val shardName = attributeAbilityNameToShard[attributeName]
                ?: ErrorManager.skyHanniError("Unknown attribute shard name for ability: $attributeName")
            val shardInternalName = shardNameToInternalName(shardName)
            processShard(shardInternalName, 10, 0)
            lastSyphonedMessage = SimpleTimeMark.now()
            return
        }
        andMoreMessagePattern.matchMatcher(event.message) {
            if (lastSyphonedMessage.passedSince() > 1.seconds) return
            if (!config.enabled) return
            val amount = group("amount").toInt()
            DelayedRun.runNextTick {
                ChatUtils.clickableChat(
                    "§aClick here and scroll through to refresh SkyHanni's attribute overlay data with $amount shards",
                    { HypixelCommands.attributeMenu() },
                )
            }
        }
    }

    private fun processAttributeMenuItems() {
        val items = InventoryUtils.getItemsInOpenChest().map { it.stack }
        for (item in items) {
            val internalName = item.getInternalNameOrNull() ?: continue
            if (!isAttributeShard(internalName)) continue
            var tier = 0
            var toNextTier = 0
            attributeShardNamePattern.matchMatcher(item.displayName) {
                tier = groupOrNull("tier")?.romanToDecimal() ?: 0
            }
            syphonAmountPattern.firstMatcher(item.getLore()) {
                toNextTier = group("amount").toInt()
            }
            processShard(internalName, tier, toNextTier)
        }

        val advancedModeStack = InventoryUtils.getSlotAtIndex(52)?.stack?.orNull()
        val advancedModeLore = advancedModeStack?.getLore().orEmpty()
        advancedModeNotUnlocked.firstMatcher(advancedModeLore) {
            addAllMissingShards()
        }

        AttributeShardOverlay.updateDisplay()
    }

    private fun addAllMissingShards() {
        val currentShards = storage?.keys.orEmpty()
        if (currentShards.size > 30) return
        for ((shardName, shardInfo) in attributeInfo) {
            if (shardName in currentShards) continue
            if (shardName in unconsumableAttributes) continue

            val internalName = shardInfo.internalName
            processShard(internalName, 0, 1)
        }
    }

    private fun processHuntingBoxItems() {
        val slots = InventoryUtils.getItemsInOpenChest()
        val items = slots.map { it.stack }
        for (item in items) {
            val internalName = item.getInternalNameOrNull() ?: continue
            if (!isAttributeShard(internalName)) continue
            var tier = 0
            var toNextTier = 0
            for (line in item.getLore()) {
                attributeShardNameLorePattern.matchMatcher(line) {
                    tier = groupOrNull("tier")?.romanToDecimal() ?: 0
                }
                syphonAmountPattern.matchMatcher(line) {
                    toNextTier = group("amount").toInt()
                }
            }
            processShard(internalName, tier, toNextTier)
        }
        HuntingBoxValue.processInventory(slots)
    }

    private fun processShard(
        internalName: NeuInternalName,
        currentTier: Int,
        toNextTier: Int,
    ) {
        val attributeName = shardInternalNameToShardName(internalName)
        if (attributeName in unconsumableAttributes) {
            return
        }
        val rarity = attributeInfo[attributeName]?.rarity
            ?: ErrorManager.skyHanniError("Unknown attribute shard rarity for $attributeName")
        val totalAmount = findTotalAmount(currentTier, toNextTier, rarity)
        storage?.getOrPut(attributeName) { ProfileSpecificStorage.AttributeShardData(0) }?.amountSyphoned = totalAmount
    }

    private fun findTotalAmount(currentTier: Int, toNextTier: Int, rarity: LorenzRarity): Int {
        val tierLevelling = attributeLevelling[rarity] ?: ErrorManager.skyHanniError("Unknown attribute rarity: $rarity")
        if (currentTier > tierLevelling.size) {
            ErrorManager.skyHanniError("Current attribute tier $currentTier exceeds the maximum tier")
        }
        val cumulativeAmount = tierLevelling.take(currentTier + 1).sum()
        return cumulativeAmount - toNextTier
    }

    fun findTierAndAmountUntilNext(shardName: String, totalAmount: Int): Triple<Int, Int, Int> {
        val rarity = attributeInfo[shardName]?.rarity
            ?: ErrorManager.skyHanniError("Unknown attribute shard rarity for $shardName")
        val tierLevelling = attributeLevelling[rarity]
            ?: ErrorManager.skyHanniError("Unknown attribute rarity: $rarity")

        var tier = 0
        var cumulativeCount = 0
        var amountToNextTier = 0

        for (amount in tierLevelling) {
            cumulativeCount += amount
            if (cumulativeCount > totalAmount && amountToNextTier == 0) {
                amountToNextTier = cumulativeCount - totalAmount
            }
            if (cumulativeCount > totalAmount) continue
            tier++
        }
        val amountToMax = (cumulativeCount - totalAmount).coerceAtLeast(0)
        return Triple(tier, amountToNextTier, amountToMax)
    }

    private fun shardInternalNameToShardName(internalName: NeuInternalName): String {
        return internalNameToShard[internalName]
            ?: ErrorManager.skyHanniError("Unknown attribute shard internal name: $internalName")
    }

    fun shardNameToInternalName(shardName: String): NeuInternalName {
        return attributeInfo[shardName]?.internalName
            ?: ErrorManager.skyHanniError("Unknown attribute shard name: $shardName")
    }

    fun isAttributeShard(internalName: NeuInternalName): Boolean {
        val asString = internalName.asString()
        return asString.startsWith("ATTRIBUTE_SHARD_") && asString.endsWith(";1")
    }
}
