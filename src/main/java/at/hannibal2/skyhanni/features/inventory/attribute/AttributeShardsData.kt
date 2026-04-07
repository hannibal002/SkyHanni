package at.hannibal2.skyhanni.features.inventory.attribute

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.enoughupdates.ItemResolutionQuery
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.features.inventory.AttributeShardsConfig
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuAttributeShardData
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuAttributeShardJson
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.NeuRepositoryReloadEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.item.ShardEvent
import at.hannibal2.skyhanni.events.item.ShardGainEvent
import at.hannibal2.skyhanni.events.item.ShardSource
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getCleanLore
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
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

    private var attributeLevelling = emptyMap<LorenzRarity, List<Int>>()
    var unconsumableAttributes = emptyList<String>()
    private var attributeInfo = emptyMap<String, NeuAttributeShardData>()
    private var internalNameToShard = emptyMap<NeuInternalName, String>()
    private var attributeAbilityNameToShard = emptyMap<String, String>()

    var maxShards = 0
        private set

    val attributeMenuInventory = InventoryDetector(
        onOpenInventory = { DelayedRun.runNextTick { processAttributeMenuItems() } },
    ) { name -> name == "Attribute Menu" }
    val huntingBoxInventory = InventoryDetector(
        onOpenInventory = { DelayedRun.runNextTick { processHuntingBoxItems() } },
    ) { name -> name == "Hunting Box" }
    val bazaarShardsInventory = InventoryDetector(
        pattern = "\\(\\d+/\\d+\\) Oddities ➜ Shards".toPattern(),
        onOpenInventory = { DelayedRun.runNextTick { AttributeShardOverlay.updateDisplay() } },
    )
    val confirmFusionInventory = InventoryDetector(
        onOpenInventory = { DelayedRun.runNextTick { FusionData.updateFusionData() } },
    ) { name -> name == "Confirm Fusion" }
    val fusionBoxInventory = InventoryDetector { name -> name == "Fusion Box" }
    val shardFusionInventory = InventoryDetector { name -> name == "Shard Fusion" }

    private var lastSyphonedMessage = SimpleTimeMark.farPast()

    private val patternGroup = RepoPattern.group("inventory.attributeshards")

    // <editor-fold desc="Patterns">
    /**
     * REGEX-TEST: Nature Elemental
     * REGEX-TEST: Berry Eater IX
     * REGEX-TEST: Essence of Ice I
     * REGEX-TEST: Advanced Mode
     */
    val attributeShardNamePattern by patternGroup.pattern(
        "name.colorless",
        "(?<name>.+?) ?(?<tier>[IVXL]+)?$",
    )

    /**
     * REGEX-TEST: Enabled: Yes
     * REGEX-TEST: Enabled: No
     */
    private val attributeStatePattern by patternGroup.pattern(
        "state.colorless",
        "Enabled: (?<state>.+)",
    )

    /**
     * REGEX-TEST: Syphon 3 more to level up!
     * REGEX-TEST: Syphon 1 shard to unlock!
     * REGEX-TEST: Syphon 1 more to level up!
     */
    private val syphonAmountPattern by patternGroup.pattern(
        "syphon.amount.colorless",
        "Syphon (?<amount>\\d+) (?:more to level up|shard to unlock)!",
    )

    /**
     * REGEX-TEST: Veil (Combat)
     * REGEX-TEST: Yummy X (Foraging)
     */
    private val attributeShardNameLorePattern by patternGroup.pattern(
        "name.lore.colorless",
        "(?<name>.+?) ?(?<tier>[IVXL]+)? \\(\\w+\\)$",
    )

    /**
     * REGEX-TEST: Owned: 1 Shard
     * REGEX-TEST: Owned: 3 Shards
     * REGEX-TEST: Owned: 71 Shards
     * REGEX-TEST: Owned: 1,729 Shards
     */
    val amountOwnedPattern by patternGroup.pattern(
        "owned.colorless",
        "Owned: (?<amount>[\\d,]+) Shards?",
    )

    /**
     * REGEX-TEST: Required to fuse: 5
     */
    val requiredToFusePattern by patternGroup.pattern(
        "fuse.required.colorless",
        "Required to fuse: (?<amount>\\d)",
    )

    /**
     * REGEX-TEST: +1 Arthropod Ruler Attribute (Level 1) - 2 more to upgrade!
     * REGEX-TEST: +1 Arthropod Ruler Attribute (Level 2) - 3 more to upgrade!
     * REGEX-TEST: +2 Essence of Ice Attribute (Level 2) - 1 more to upgrade!
     * REGEX-TEST: +6 Ender Ruler Attribute (Level 3) - 3 more to upgrade!
     * REGEX-FAIL: +43 Essence of Ice Attribute (Level 10) MAXED
     */
    private val shardSyphonedPattern by patternGroup.pattern(
        "chat.syphoned.colorless",
        "\\+(?<amount>\\d+) (?<attributeName>.+) Attribute \\(Level (?<level>\\d+)\\) - (?<untilNext>\\d+) more to upgrade!",
    )

    /**
     * REGEX-TEST: +43 Essence of Ice Attribute (Level 10) MAXED
     * REGEX-FAIL: +2 Essence of Ice Attribute (Level 2) - 1 more to upgrade!
     */
    private val shardSyphonedMaxedPattern by patternGroup.pattern(
        "chat.syphoned.maxed.colorless",
        "\\+(?<amount>\\d+) (?<attributeName>.+) Attribute \\(Level (?<level>\\d+)\\) MAXED",
    )

    /**
     * REGEX-TEST: and 7 more...
     */
    private val andMoreMessagePattern by patternGroup.pattern(
        "chat.and.more.colorless",
        "and (?<amount>\\d+) more\\.\\.\\.",
    )

    private val advancedModeNotUnlocked by patternGroup.pattern(
        "advanced.mode.colorless",
        "Advanced Mode unlocked at 30",
    )

    /**
     * REGEX-TEST: Nature Elemental is now enabled!
     */
    private val attributeEnabledPattern by patternGroup.pattern(
        "chat.enabled.colorless",
        "(?<attributeName>.+) is now enabled!",
    )

    /**
     * REGEX-TEST: Nature Elemental is now disabled!
     */
    private val attributeDisabledPattern by patternGroup.pattern(
        "chat.disabled.colorless",
        "(?<attributeName>.+) is now disabled!",
    )

    /**
     * REGEX-TEST: You caught x2 Bal Shards!
     * REGEX-TEST: You caught a Birries Shard!
     * REGEX-TEST: You caught an Invisibug Shard!
     * REGEX-TEST: You caught x2 Hideonleaf Shards!
     * REGEX-TEST: You caught x2 Voracious Spider Shards!
     */
    private val caughtShardsPattern by patternGroup.pattern(
        "caught.shards.colorless",
        "You caught(?: [an]+)?(?: x(?<amount>\\d+))? (?<shardName>.+) Shards?!",
    )

    /**
     * REGEX-TEST: LOOT SHARE You received a Glacite Walker Shard for assisting Mealoan!
     * REGEX-TEST: LOOT SHARE You received 2 Mossybit Shards for assisting FallenYeti!
     */
    private val lootShareShardPattern by patternGroup.pattern(
        "loot.share.shard.colorless",
        "LOOT SHARE You received (?:an?|(?<amount>\\d+)) (?<shardName>.+) Shards? for assisting .+!",
    )

    /**
     * REGEX-TEST: FUSION! You obtained Bolt Shard x2!
     * REGEX-TEST: FUSION! You obtained Bolt Shard x2! NEW!
     * REGEX-TEST: FUSION! You obtained a Tadgang Shard!
     * REGEX-TEST: FUSION! You obtained a Tadgang Shard! NEW!
     */
    private val fusionShardPattern by patternGroup.pattern(
        "fusion.shard.colorless",
        "FUSION! You obtained(?: an?)? (?<shardName>.+) Shard(?: x(?<amount>\\d+))?!(?: NEW!)?",
    )

    /**
     * REGEX-TEST: SALT You charmed a Magma Slug and captured 3 Shards from it.
     * REGEX-TEST: SALT You charmed a Lapis Zombie and captured its Shard.
     * REGEX-TEST: CHARM You charmed a Lapis Zombie and captured its Shard.
     * REGEX-TEST: NAGA You charmed a Lapis Zombie and captured its Shard.
     */
    @Suppress("MaxLineLength")
    private val charmedShardPattern by patternGroup.pattern(
        "charmed.shard.colorless",
        "(?<charmType>CHARM|SALT|NAGA) You charmed an? (?<shardName>.+) and captured (?:(?<amount>\\d+) Shards from it|its Shard)\\.",
    )

    /**
     * REGEX-TEST: You sent an Invisibug Shard to your Hunting Box.
     * REGEX-TEST: You sent 6 Voracious Spider Shards to your Hunting Box.
     * REGEX-TEST: You sent a Voracious Spider Shard to your Hunting Box.
     * REGEX-TEST: You sent a Verdant Shard to your Hunting Box.
     */
    private val sentToHuntingBoxPattern by patternGroup.pattern(
        "sent.to.hunting.box.colorless",
        "You sent (?:an?|(?<amount>\\d+)) (?<shardName>.+) Shards? to your Hunting Box\\.",
    )

    private val shardGainChatPatterns = mapOf(
        caughtShardsPattern to ShardSource.HUNT,
        lootShareShardPattern to ShardSource.HUNT,
        charmedShardPattern to null,
        sentToHuntingBoxPattern to ShardSource.SENT_TO_HUNTING_BOX,
    )

    @HandleEvent(priority = HandleEvent.LOWEST)
    fun onNeuRepoReload(event: NeuRepositoryReloadEvent) {
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
    fun onChat(event: SkyHanniChatEvent.Allow) {
        val message = event.cleanMessage

        shardSyphonedPattern.matchMatcher(message) {
            val attributeName = group("attributeName")
            val level = group("level").toInt()
            val untilNext = group("untilNext").toInt()
            val shardName = abilityNameToShardName(attributeName) ?: return
            val shardInternalName = shardNameToInternalName(shardName) ?: return
            processShard(shardInternalName, level, untilNext)

            ShardEvent(shardInternalName, -group("amount").toInt(), ShardSource.SYPHON).post()

            lastSyphonedMessage = SimpleTimeMark.now()
            return
        }

        shardSyphonedMaxedPattern.matchMatcher(message) {
            val attributeName = group("attributeName")
            val shardName = abilityNameToShardName(attributeName) ?: return
            val shardInternalName = shardNameToInternalName(shardName) ?: return
            processShard(shardInternalName, 10, 0)

            ShardEvent(shardInternalName, -group("amount").toInt(), ShardSource.SYPHON).post()

            lastSyphonedMessage = SimpleTimeMark.now()
            return
        }

        andMoreMessagePattern.matchMatcher(message) {
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

        attributeEnabledPattern.matchMatcher(message) {
            val attributeName = group("attributeName")
            val shardName = abilityNameToShardName(attributeName) ?: return
            val shardInternalName = shardNameToInternalName(shardName) ?: return
            setAttributeState(shardInternalName, true)
        }

        attributeDisabledPattern.matchMatcher(message) {
            val attributeName = group("attributeName")
            val shardName = abilityNameToShardName(attributeName) ?: return
            val shardInternalName = shardNameToInternalName(shardName) ?: return
            setAttributeState(shardInternalName, false)
        }

        for ((pattern, source) in shardGainChatPatterns) {
            pattern.matchMatcher(message) {
                val shardName = group("shardName")
                val amount = groupOrNull("amount")?.toInt() ?: 1

                val shardInternalName = ItemResolutionQuery.attributeNameToInternalName(shardName) ?: run {
                    ItemUtils.addMissingRepoItem(
                        shardName,
                        "Could not find internal name for attribute shard: $shardName",
                    )
                    return
                }

                val newSource = if (source == null) {
                    val type = groupOrNull("charmType")
                    when (type) {
                        "CHARM" -> ShardSource.CHARM
                        "NAGA" -> ShardSource.NAGA
                        "SALT" -> ShardSource.SALT

                        else -> ShardSource.UNKNOWN
                    }
                } else {
                    source
                }

                if (source == ShardSource.SENT_TO_HUNTING_BOX) {
                    ShardEvent(shardInternalName, amount, newSource).post()
                } else {
                    ShardGainEvent(shardInternalName, amount, newSource).post()
                }
                return
            }
        }

        fusionShardPattern.matchMatcher(message) {
            val currentFusionData = FusionData.currentFusionData ?: return
            val amount = groupOrNull("amount")?.toInt() ?: 1
            ShardEvent(currentFusionData.outputShard, amount, ShardSource.FUSE).post()
            ShardEvent(
                currentFusionData.firstShard.internalName,
                -currentFusionData.firstShard.amount,
                ShardSource.FUSE,
            ).post()
            ShardEvent(
                currentFusionData.secondShard.internalName,
                -currentFusionData.secondShard.amount,
                ShardSource.FUSE,
            ).post()
        }
    }

    @HandleEvent
    fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Active Attribute Levels")
        event.addIrrelevant {
            for (shardName in attributeInfo.keys) {
                add("- $shardName: Level ${getActiveLevel(shardName)}/10")
            }
        }
    }

    private fun processAttributeMenuItems() {
        val items = InventoryUtils.getItemsInOpenChest().map { it.item }
        for (item in items) {
            val internalName = item.getInternalNameOrNull() ?: continue
            if (!isAttributeShard(internalName)) continue
            var tier = 0
            var toNextTier = 0
            attributeShardNamePattern.matchMatcher(item.cleanName()) {
                tier = groupOrNull("tier")?.romanToDecimal() ?: 0
            }
            val lore = item.getCleanLore()
            syphonAmountPattern.firstMatcher(lore) {
                toNextTier = group("amount").toInt()
            }
            processShard(internalName, tier, toNextTier)
            attributeStatePattern.firstMatcher(lore) {
                val enabled = group("state") == "Yes"
                setAttributeState(internalName, enabled)
            }
        }

        val advancedModeStack = InventoryUtils.getSlotAtIndex(52)?.item?.orNull()
        val advancedModeLore = advancedModeStack?.getCleanLore().orEmpty()
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
        val items = slots.map { it.item }
        for (item in items) {
            val internalName = item.getInternalNameOrNull() ?: continue
            if (!isAttributeShard(internalName)) continue
            var tier = 0
            var toNextTier = 0
            for (line in item.getCleanLore()) {
                attributeShardNameLorePattern.matchMatcher(line) {
                    tier = groupOrNull("tier")?.romanToDecimal() ?: 0
                }
                syphonAmountPattern.matchMatcher(line) {
                    toNextTier = group("amount").toInt()
                }
                amountOwnedPattern.matchMatcher(line) {
                    val amount = group("amount").formatInt()
                    val attributeName = shardInternalNameToShardName(internalName)
                    storage?.getOrPut(attributeName) {
                        ProfileSpecificStorage.AttributeShardData()
                    }?.amountInBox = amount
                }
            }
            processShard(internalName, tier, toNextTier)
        }
        HuntingBoxValue.processInventory(slots)
    }

    @HandleEvent(priority = HandleEvent.HIGHEST)
    fun onShardGain(event: ShardEvent) {
        val attributeName = shardInternalNameToShardName(event.shardInternalName)
        val existing = storage?.get(attributeName)?.amountInBox ?: 0
        val newAmount = (existing + event.amount).coerceAtLeast(0)
        storage?.getOrPut(attributeName) {
            ProfileSpecificStorage.AttributeShardData()
        }?.amountInBox = newAmount
    }

    private fun processShard(
        internalName: NeuInternalName,
        currentTier: Int,
        toNextTier: Int,
    ) {
        val attributeName = shardInternalNameToShardName(internalName)
        if (attributeName in unconsumableAttributes) return
        val rarity = attributeInfo[attributeName]?.rarity
            ?: ErrorManager.skyHanniError("Unknown attribute shard rarity for $attributeName")
        val totalAmount = findTotalAmount(currentTier, toNextTier, rarity)
        storage?.getOrPut(attributeName) {
            ProfileSpecificStorage.AttributeShardData()
        }?.amountSyphoned = totalAmount
    }

    private fun setAttributeState(
        internalName: NeuInternalName,
        enabled: Boolean,
    ) {
        val attributeName = shardInternalNameToShardName(internalName)
        if (attributeName in unconsumableAttributes) {
            ErrorManager.skyHanniError("Unconsumable attribute was toggled: $attributeName. This should never happen.")
        }
        storage?.getOrPut(attributeName) {
            ProfileSpecificStorage.AttributeShardData()
        }?.enabled = enabled
    }

    private fun findTotalAmount(currentTier: Int, toNextTier: Int, rarity: LorenzRarity): Int {
        val tierLevelling = attributeLevelling[rarity] ?: ErrorManager.skyHanniError("Unknown attribute rarity: $rarity")
        if (currentTier > tierLevelling.size) {
            ErrorManager.skyHanniError("Current attribute tier $currentTier exceeds the maximum tier")
        }
        val cumulativeAmount = tierLevelling.take(currentTier + 1).sum()
        return cumulativeAmount - toNextTier
    }

    fun findTierAndAmountUntilNext(shardName: String, totalAmount: Int): AttributeProgressData {
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
        return AttributeProgressData(tier, amountToNextTier, amountToMax)
    }

    fun shardInternalNameToShardName(internalName: NeuInternalName): String =
        internalNameToShard[internalName]
            ?: ErrorManager.skyHanniError("Unknown attribute shard internal name: $internalName")

    private fun abilityNameToShardName(ability: String): String? {
        val shardName = attributeAbilityNameToShard[ability]
        if (shardName == null) {
            ItemUtils.addMissingRepoItem(
                ability,
                "Could not find shard name for attribute ability: $ability",
            )
        }
        return shardName
    }

    fun shardNameToInternalName(shardName: String): NeuInternalName? {
        val internalName = attributeInfo[shardName]?.internalName
        if (internalName == null) {
            ItemUtils.addMissingRepoItem(
                shardName,
                "Could not find internal name for attribute shard: $shardName",
            )
        }
        return internalName
    }

    fun shardNameToAttributeInformation(shardName: String): NeuAttributeShardData? {
        val info = attributeInfo[shardName]
        if (info == null) {
            ItemUtils.addMissingRepoItem(
                shardName,
                "Could not find information for attribute shard: $shardName",
            )
        }
        return info
    }

    fun isAttributeShard(internalName: NeuInternalName): Boolean = internalName.asString().let {
        it.startsWith("ATTRIBUTE_SHARD_") && it.endsWith(";1")
    }

    private fun getLevel(shardName: String): Int = getSyphonedAmount(shardName).let {
        findTierAndAmountUntilNext(shardName, it).tier
    }

    private fun isEnabled(shardName: String): Boolean =
        storage?.get(shardName)?.enabled ?: false

    fun getActiveLevel(shardName: String) =
        if (isEnabled(shardName)) getLevel(shardName) else 0

    fun getSyphonedAmount(shardName: String): Int =
        storage?.get(shardName)?.amountSyphoned ?: 0

    fun getAmountInHuntingBox(shardName: String): Int =
        storage?.get(shardName)?.amountInBox ?: 0

    fun getAmountUntilMax(shardName: String): Int =
        findTierAndAmountUntilNext(shardName, getSyphonedAmount(shardName)).toMax

    fun isInFusionMachine(): Boolean =
        fusionBoxInventory.isInside() || shardFusionInventory.isInside() || confirmFusionInventory.isInside()

    fun resetHuntingBoxShards() {
        storage?.forEach { it.value.amountInBox = 0 }
        ChatUtils.clickableChat(
            "Reset hunting box shards data. Open the hunting box or click here to update the saved data.",
            { HypixelCommands.huntingBox() },
            "§eClick here to open the hunting box!",
        )
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shresethuntingbox") {
            description = "Resets stored hunting box shards"
            category = CommandCategory.USERS_RESET
            simpleCallback {
                resetHuntingBoxShards()
            }
        }
    }

    @ConsistentCopyVisibility
    data class AttributeProgressData internal constructor(
        val tier: Int,
        val toNextTier: Int,
        val toMax: Int,
    )
}
