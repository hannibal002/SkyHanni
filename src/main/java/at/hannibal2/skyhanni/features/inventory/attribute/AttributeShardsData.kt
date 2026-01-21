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
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.orNull
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object AttributeShardsData {

    val config get(): AttributeShardsConfig = SkyHanniMod.feature.inventory.attributeShards
    private val storage get() = ProfileStorageData.profileSpecific?.attributeShards

    private var attributeLevelling = mapOf<LorenzRarity, List<Int>>()
    var unconsumableAttributes = listOf<String>()
    private var attributeInfo = mapOf<String, NeuAttributeShardData>()
    private var internalNameToShard = mapOf<NeuInternalName, String>()
    private var attributeAbilityNameToShard = mapOf<String, String>()

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
     * REGEX-TEST: §7Enabled: §aYes
     * REGEX-TEST: §7Enabled: §cNo
     */
    private val attributeStatePattern by patternGroup.pattern(
        "state",
        "§7Enabled: §.(?<state>.+)",
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
     * REGEX-TEST: §7Required to fuse: §b5
     */
    val requiredToFusePattern by patternGroup.pattern(
        "fuse.required",
        "§7Required to fuse: §b(?<amount>\\d)",
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
        "§a\\+(?<amount>\\d+) (?<attributeName>.+) Attribute §r§7\\(Level (?<level>\\d+)\\) - (?<untilNext>\\d+) more to upgrade!",
    )

    /**
     * REGEX-TEST: §a+43 Essence of Ice Attribute §r§7(Level 10) §r§a§lMAXED
     * REGEX-FAIL: §a+2 Essence of Ice Attribute §r§7(Level 2) - 1 more to upgrade!
     */
    private val shardSyphonedMaxedPattern by patternGroup.pattern(
        "chat.syphoned.maxed",
        "§a\\+(?<amount>\\d+) (?<attributeName>.+) Attribute §r§7\\(Level (?<level>\\d+)\\) §r§a§lMAXED",
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

    /**
     * REGEX-TEST: §6Nature Elemental §r§ais now enabled!
     */
    private val attributeEnabledPattern by patternGroup.pattern(
        "chat.enabled",
        "§6(?<attributeName>.+) §r§ais now enabled!",
    )

    /**
     * REGEX-TEST: §6Nature Elemental §r§cis now disabled!
     */
    private val attributeDisabledPattern by patternGroup.pattern(
        "chat.disabled",
        "§6(?<attributeName>.+) §r§cis now disabled!",
    )

    /**
     * REGEX-TEST: §aYou caught §7x2 §5Bal §aShards§a!
     * REGEX-TEST: §aYou caught a §fBirries §aShard!
     * REGEX-TEST: §aYou caught an §9Invisibug §aShard!
     * REGEX-TEST: §aYou caught an §9Invisibug §aShard!
     * REGEX-TEST: §aYou caught §7x2 §fHideonleaf §aShards§a!
     * REGEX-TEST: §aYou caught §7x2 §fVoracious Spider §aShards§a!
     */
    private val caughtShardsPattern by patternGroup.pattern(
        "caught.shards",
        "§aYou caught(?: [an]+)?(?: §7x(?<amount>\\d+))? §.(?<shardName>.+) §aShard(?:s§a)?!",
    )

    /**
     * REGEX-TEST: §e§lLOOT SHARE §fYou received a §9Glacite Walker §fShard for assisting §bMealoan§f!
     * REGEX-TEST: §e§lLOOT SHARE §fYou received §b2 §aMossybit §fShards for assisting §bFallenYeti§f!
     */
    private val lootShareShardPattern by patternGroup.pattern(
        "loot.share.shard",
        "§e§lLOOT SHARE §fYou received (?:an?|§.(?<amount>\\d+)) §.(?<shardName>.+) §fShards? for assisting .*§f!",
    )

    /**
     * REGEX-TEST: §5§lFUSION! §7You obtained §9Bolt Shard §8x2§7!
     * REGEX-TEST: §5§lFUSION! §7You obtained §9Bolt Shard §8x2§7! §d§lNEW!
     * REGEX-TEST: §5§lFUSION! §7You obtained a §fTadgang Shard§7!
     * REGEX-TEST: §5§lFUSION! §7You obtained a §fTadgang Shard§7! §d§lNEW!
     */
    private val fusionShardPattern by patternGroup.pattern(
        "fusion.shard",
        "§5§lFUSION! §7You obtained(?: an?)? (?:§.)+(?<shardName>.+) Shard(?: §8x(?<amount>\\d+))?§7!(?: §d§lNEW!)?",
    )

    /**
     * REGEX-TEST: §d§lSALT§7 You charmed a §aMagma Slug§7 and captured §93 Shards §7from it.§r
     * REGEX-TEST: §d§lSALT§7 You charmed a §fLapis Zombie§7 and captured its §9Shard§7.
     * REGEX-TEST: §5§lCHARM§7 You charmed a §fLapis Zombie§7 and captured its §9Shard§7.
     * REGEX-TEST: §6§lNAGA§7 You charmed a §fLapis Zombie§7 and captured its §9Shard§7.
     */
    @Suppress("MaxLineLength")
    private val charmedShardPattern by patternGroup.pattern(
        "charmed.shard",
        "§.§l(?<charmType>CHARM|SALT|NAGA)§7 You charmed an? §.(?<shardName>.+)§7 and captured (?:§.(?<amount>\\d+) Shards §7from it|its §9Shard§7)\\.(?:§.)*",
    )

    /**
     * REGEX-TEST: §7You sent §aan §9Invisibug Shard §7to your §aHunting Box§7.
     * REGEX-TEST: §7You sent §a6 §fVoracious Spider Shards §7to your §aHunting Box§7.
     * REGEX-TEST: §7You sent §aa §fVoracious Spider Shard §7to your §aHunting Box§7.
     * REGEX-TEST: §7You sent a §fVerdant Shard §7to your §aHunting Box§7.
     */
    private val sentToHuntingBoxPattern by patternGroup.pattern(
        "sent.to.hunting.box",
        "§7You sent (?:§a)?(?:an?|(?<amount>\\d+)) §.(?<shardName>.+) Shards? §7to your §aHunting Box§7.",
    )

    // the boolean is if it should post the shard gain event
    private val shardGainChatPatterns = mapOf(
        caughtShardsPattern to (true to ShardSource.HUNT),
        lootShareShardPattern to (true to ShardSource.HUNT),
        charmedShardPattern to (true to null),
        sentToHuntingBoxPattern to (false to ShardSource.SENT_TO_HUNTING_BOX),
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
    fun onChat(event: SkyHanniChatEvent) {
        shardSyphonedPattern.matchMatcher(event.message) {
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

        shardSyphonedMaxedPattern.matchMatcher(event.message) {
            val attributeName = group("attributeName")
            val shardName = abilityNameToShardName(attributeName) ?: return
            val shardInternalName = shardNameToInternalName(shardName) ?: return
            processShard(shardInternalName, 10, 0)

            ShardEvent(shardInternalName, -group("amount").toInt(), ShardSource.SYPHON).post()

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

        attributeEnabledPattern.matchMatcher(event.message) {
            val attributeName = group("attributeName")
            val shardName = abilityNameToShardName(attributeName) ?: return
            val shardInternalName = shardNameToInternalName(shardName) ?: return
            setAttributeState(shardInternalName, true)
        }

        attributeDisabledPattern.matchMatcher(event.message) {
            val attributeName = group("attributeName")
            val shardName = abilityNameToShardName(attributeName) ?: return
            val shardInternalName = shardNameToInternalName(shardName) ?: return
            setAttributeState(shardInternalName, false)
        }

        for ((pattern, shouldPostGainEvent) in shardGainChatPatterns) {
            pattern.matchMatcher(event.message) {
                val shardName = group("shardName")
                val amount = groupOrNull("amount")?.toInt() ?: 1

                val shardInternalName = ItemResolutionQuery.attributeNameToInternalName(shardName)?.toInternalName()
                if (shardInternalName == null) {
                    ItemUtils.addMissingRepoItem(shardName, "Could not find internal name for attribute shard: $shardName")
                    return
                }

                val source = shouldPostGainEvent.second
                val newSource = if (source == null) {
                    val type = groupOrNull("charmType")
                    if (type == "CHARM") {
                        ShardSource.CHARM
                    } else if (type == "NAGA") {
                        ShardSource.NAGA
                    } else if (type == "SALT") {
                        ShardSource.SALT
                    } else {
                        ShardSource.UNKNOWN
                    }
                } else {
                    source
                }

                if (shouldPostGainEvent.first) {
                    ShardGainEvent(shardInternalName, amount, newSource).post()
                } else {
                    ShardEvent(shardInternalName, amount, newSource).post()
                }
                return
            }
        }

        fusionShardPattern.matchMatcher(event.message) {
            val currentFusionData = FusionData.currentFusionData ?: return
            val amount = groupOrNull("amount")?.toInt() ?: 1
            ShardEvent(currentFusionData.outputShard, amount, ShardSource.FUSE).post()
            ShardEvent(currentFusionData.firstShard.internalName, -currentFusionData.firstShard.amount, ShardSource.FUSE).post()
            ShardEvent(currentFusionData.secondShard.internalName, -currentFusionData.secondShard.amount, ShardSource.FUSE).post()
        }
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
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
            attributeShardNamePattern.matchMatcher(item.hoverName.formattedTextCompatLeadingWhiteLessResets()) {
                tier = groupOrNull("tier")?.romanToDecimal() ?: 0
            }
            val lore = item.getLore()
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
        val items = slots.map { it.item }
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

    fun shardInternalNameToShardName(internalName: NeuInternalName): String {
        return internalNameToShard[internalName]
            ?: ErrorManager.skyHanniError("Unknown attribute shard internal name: $internalName")
    }

    private fun abilityNameToShardName(ability: String): String? {
        val shardName = attributeAbilityNameToShard[ability]
        if (shardName == null) {
            ItemUtils.addMissingRepoItem(ability, "Could not find shard name for attribute ability: $ability")
        }
        return shardName
    }

    fun shardNameToInternalName(shardName: String): NeuInternalName? {
        val internalName = attributeInfo[shardName]?.internalName
        if (internalName == null) {
            ItemUtils.addMissingRepoItem(shardName, "Could not find internal name for attribute shard: $shardName")
        }
        return internalName
    }

    fun shardNameToAttributeInformation(shardName: String): NeuAttributeShardData? {
        val info = attributeInfo[shardName]
        if (info == null) {
            ItemUtils.addMissingRepoItem(shardName, "Could not find information for attribute shard: $shardName")
        }
        return info
    }

    fun isAttributeShard(internalName: NeuInternalName): Boolean =
        internalName.asString().let {
            it.startsWith("ATTRIBUTE_SHARD_") && it.endsWith(";1")
        }

    private fun getLevel(shardName: String): Int =
        getSyphonedAmount(shardName).let {
            findTierAndAmountUntilNext(shardName, it).first
        }

    private fun isEnabled(shardName: String): Boolean =
        storage?.get(shardName)?.enabled ?: false

    fun getActiveLevel(shardName: String) =
        if (isEnabled(shardName)) getLevel(shardName) else 0

    fun getSyphonedAmount(shardName: String): Int {
        return storage?.get(shardName)?.amountSyphoned ?: 0
    }

    fun getAmountInHuntingBox(shardName: String): Int {
        return storage?.get(shardName)?.amountInBox ?: 0
    }

    fun getAmountUntilMax(shardName: String): Int {
        return findTierAndAmountUntilNext(shardName, getSyphonedAmount(shardName)).third
    }

    fun isInFusionMachine(): Boolean {
        return fusionBoxInventory.isInside() || shardFusionInventory.isInside() || confirmFusionInventory.isInside()
    }

    fun resetHuntingBoxShards() {
        storage?.forEach { it.value.amountInBox = 0 }
        ChatUtils.clickableChat(
            "Reset hunting box shards data. Open the hunting box or click here to update the saved data.",
            { HypixelCommands.huntingBox() },
            "§eClick here to open the hunting box!"
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
}
