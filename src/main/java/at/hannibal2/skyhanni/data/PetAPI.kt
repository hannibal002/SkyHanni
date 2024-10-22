package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.jsonobjects.repo.NEUPetsJson
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.NeuRepositoryReloadEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.events.skyblock.PetChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatDouble
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatches
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.hasGroup
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getExtraAttributes
import at.hannibal2.skyhanni.utils.StringUtils.convertToUnformatted
import at.hannibal2.skyhanni.utils.chat.Text.hover
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.gson.Gson
import com.google.gson.JsonObject
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object PetAPI {
    private val patternGroup = RepoPattern.group("misc.pet")
    private val petMenuPattern by patternGroup.pattern(
        "menu.title",
        "Pets(?: \\(\\d+/\\d+\\) )?",
    )

    var pet: PetData? = null
        private set
    var inPetMenu = false
        private set

    private var xpLeveling: List<Int> = listOf()
    private var customXpLeveling: JsonObject? = null
    private var petRarityOffset = mapOf<LorenzRarity, Int>()
    private var customDisplayToInternalName = mapOf<NEUInternalName, String>()

    /**
     * REGEX-TEST: §e⭐ §7[Lvl 200] §6Golden Dragon§d ✦
     * REGEX-TEST: ⭐ [Lvl 100] Black Cat ✦
     */
    private val petItemNamePattern by patternGroup.pattern(
        "item.name",
        "(?<favorite>(?:§.)*⭐ )?(?:§.)*\\[Lvl (?<level>\\d+)] (?<name>.*)",
    )
    private val neuRepoPetItemNamePattern by patternGroup.pattern(
        "item.name.neu.format",
        "(?:§f§f)?§7\\[Lvl (?:1➡(?:100|200)|\\{LVL})] (?<name>.*)",
    )

    /**
     * REGEX-TEST: §e⭐ §7[Lvl 100] §6Ender Dragon
     * REGEX-TEST: §e⭐ §7[Lvl 100] §dBlack Cat§d ✦
     * REGEX-TEST: §7[Lvl 100] §6Mole
     */
    private val petNameMenuPattern by patternGroup.pattern(
        "menu.pet.name",
        "^(?:§e(?<favorite>⭐) )?(?:§.)*\\[Lvl (?<level>\\d+)] §(?<rarity>.)(?<name>[\\w ]+)(?<skin>§. ✦)?\$"
    )

    /**
     * REGEX-TEST: §6Held Item: §9Mining Exp Boost
     * REGEX-TEST: §6Held Item: §fAll Skills Exp Boost
     * REGEX-TEST: §6Held Item: §9Dwarf Turtle Shelmet
     */
    private val petItemMenuPattern by patternGroup.pattern(
        "menu.pet.item",
        "^§6Held Item: (?<item>§.[\\w -]+)\$"
    )

    /**
     * REGEX-TEST: §7Progress to Level 45: §e94.4%
     * REGEX-TEST: §8▸ 25,396,280 XP
     * REGEX-TEST: §7Progress to Level 58: §e3.3%
     */
    private val petXPMenuPattern by patternGroup.pattern(
        "menu.pet.xp",
        "§.(?:Progress to Level (?<level>\\d+): §e(?<percentage>[\\d.]+)%|▸ (?<totalXP>[\\d,.]+) XP)\$"
    )

    /**
     * REGEX-TEST: §7§cClick to despawn!
     */
    private val petDespawnMenuPattern by patternGroup.pattern(
        "menu.pet.despawn",
        "§7§cClick to despawn!"
    )

    /**
     * REGEX-TEST: §7To Select Process (Slot #2)
     * REGEX-TEST: §7To Select Process (Slot #4)
     * REGEX-TEST: §7To Select Process (Slot #7)
     */
    private val forgeBackMenuPattern by patternGroup.pattern(
        "menu.forge.goback",
        "§7To Select Process \\(Slot #\\d\\)"
    )

    /**
     * REGEX-TEST:  §r§7[Lvl 100] §r§dEndermite
     * REGEX-TEST:  §r§7[Lvl 200] §r§8[§r§6108§r§8§r§4✦§r§8] §r§6Golden Dragon
     * REGEX-TEST:  §r§7[Lvl 100] §r§dBlack Cat§r§d ✦
     */
    private val petWidgetPattern by patternGroup.pattern(
        "widget.pet",
        "^ §r§7\\[Lvl (?<level>\\d+)](?: (?:§.)+\\[(?:§.)+(?<overflow>\\d+)(?:§.)+✦(?:§.)+])? §r§(?<rarity>.)(?<name>[\\w ]+)(?:§r(?<skin>§. ✦))?\$",
    )

    /**
     * REGEX-TEST:  §r§7No pet selected
     * REGEX-TEST:  §r§6Washed-up Souvenir
     * REGEX-TEST:  §r§9Dwarf Turtle Shelmet
     */
    private val widgetStringPattern by patternGroup.pattern(
        "widget.string",
        "^ §r(?<string>§.[\\w -]+)\$",
    )

    /**
     * REGEX-TEST:  §r§b§lMAX LEVEL
     * REGEX-TEST:  §r§6+§r§e21,248,020.7 XP
     * REGEX-TEST:  §r§e15,986.6§r§6/§r§e29k XP §r§6(53.6%)
     */
    private val xpWidgetPattern by patternGroup.pattern(
        "widget.xp",
        "^ §r§.(?:§l(?<max>MAX LEVEL)|\\+§r§e(?<overflow>[\\d,.]+) XP|(?<currentXP>[\\d,.]+)§r§6\\/§r§e(?<maxXP>[\\d.km]+) XP §r§6\\((?<percentage>[\\d.%]+)\\))$",
    )

    /**
     * REGEX-TEST: §cAutopet §eequipped your §7[Lvl 100] §6Scatha§e! §a§lVIEW RULE
     * REGEX-TEST: §cAutopet §eequipped your §7[Lvl 99] §6Flying Fish§e! §a§lVIEW RULE
     * REGEX-TEST: §cAutopet §eequipped your §7[Lvl 100] §dBlack Cat§d ✦§e! §a§lVIEW RULE
     */
    private val autopetMessagePattern by patternGroup.pattern(
        "chat.autopet",
        "^§cAutopet §eequipped your §7(?<pet>\\[Lvl \\d{1,3}] §.[\\w ]+)(?:§. ✦)?§e! §a§lVIEW RULE\$"
    )

    /**
     * REGEX-TEST: §r, §aEquip: §r, §7[Lvl 99] §r, §6Flying Fish
     * REGEX-TEST: §r, §aEquip: §r, §e⭐ §r, §7[Lvl 100] §r, §dBlack Cat§r, §d ✦
     * REGEX-TEST: §r, §aEquip: §r, §7[Lvl 47] §r, §5Lion
     */
    private val autopetHoverPetPattern by patternGroup.pattern(
        "chat.autopet.hover.pet",
        "^§r, §aEquip: §r,(?: §e⭐ §r,)? §7\\[Lvl (?<level>\\d+)] §r, §(?<rarity>.)(?<pet>[\\w ]+)(?:§r, (?<skin>§. ✦))?\$"
    )

    /**
     * REGEX-TEST: §r, §aHeld Item: §r, §9Mining Exp Boost§r]
     * REGEX-TEST: §r, §aHeld Item: §r, §5Lucky Clover§r]
     * REGEX-TEST: §r, §aHeld Item: §r, §5Fishing Exp Boost§r]
     */
    private val autopetHoverPetItemPattern by patternGroup.pattern(
        "chat.autopet.hover.item",
        "^§r, §aHeld Item: §r, (?<item>§.[\\w -]+)§r]\$"
    )

    /**
     * REGEX-TEST: §aYour pet is now holding §r§9Bejeweled Collar§r§a.
     */
    private val petItemMessagePattern by patternGroup.pattern(
        "chat.pet.item.equip",
        "^§aYour pet is now holding §r(?<petItem>§.[\\w -]+)§r§a\\.\$"
    )

    private val ignoredPetStrings = listOf(
        "Archer",
        "Berserk",
        "Mage",
        "Tank",
        "Healer",
        "➡",
    )

    fun isPetMenu(inventoryTitle: String, inventoryItems: Map<Int, ItemStack>): Boolean {
        if (!petMenuPattern.matches(inventoryTitle)) return false

        val goBackLore = inventoryItems[48]?.getLore() ?: emptyList()
        return !goBackLore.any { forgeBackMenuPattern.matches(it) }
    }

    var currentPet: String?
        get() = ProfileStorageData.profileSpecific?.currentPet?.takeIf { it.isNotEmpty() }
        set(value) {
            ProfileStorageData.profileSpecific?.currentPet = value
        }

    fun isCurrentPet(petName: String): Boolean = pet?.cleanName?.contains(petName) ?: false

    fun getCleanName(nameWithLevel: String): String? {
        petItemNamePattern.matchMatcher(nameWithLevel) {
            return group("name")
        }
        neuRepoPetItemNamePattern.matchMatcher(nameWithLevel) {
            return group("name")
        }

        return null
    }

    @Deprecated(message = "use PetAPI.pet.level")
    fun getPetLevel(nameWithLevel: String): Int? = petItemNamePattern.matchMatcher(nameWithLevel) {
        group("level").toInt()
    }

    @Deprecated(message = "use PetAPI.pet.name")
    fun hasPetName(name: String): Boolean = petItemNamePattern.matches(name) && !ignoredPetStrings.any { name.contains(it) }

    @SubscribeEvent
    fun onWidgetUpdate(event: WidgetUpdateEvent) {
        if (!event.isWidget(TabWidget.PET)) return

        val newPetLine = petWidgetPattern.firstMatches(event.lines)?.trim() ?: return
        if (newPetLine == pet?.rawPetName) return

        var internalName: NEUInternalName? = null
        var cleanName: String? = null
        var rarity: LorenzRarity? = null
        var level: Int? = null
        var xp: Double? = null
        var rawPetName: String? = null
        var petItem: NEUInternalName? = null
        var overflowXP = 0.0
        for (line in event.lines) {
            val tempPetItem = handleWidgetStringLine(line)
            if (tempPetItem != null) {
                petItem = tempPetItem
                continue
            }

            val tempPetXP = handleWidgetXPLine(line)
            if (tempPetXP != null) {
                overflowXP = tempPetXP
                continue
            }

            val tempPetMisc = handleWidgetPetLine(line, newPetLine)
            if (tempPetMisc != null) {
                internalName = tempPetMisc.internalName
                cleanName = tempPetMisc.cleanName
                rarity = tempPetMisc.rarity
                level = tempPetMisc.level
                xp = tempPetMisc.xp
                rawPetName = tempPetMisc.rawPetName
                continue
            }
        }
        val newPetData = PetData(
            internalName ?: return,
            cleanName ?: return,
            rarity ?: return,
            petItem,
            level ?: return,
            xp ?: return,
            rawPetName ?: return,
        )
        updatePet(newPetData.copy(xp = newPetData.xp + overflowXP))
    }

    private fun handleWidgetPetLine(line: String, newPetLine: String): PetData? {
        return petWidgetPattern.matchMatcher(line) {
            val rarity = LorenzRarity.getByColorCode(groupOrNull("rarity")?.get(0) ?: run {
                throwUnknownRarity(group("rarity"))
            }) ?: throwUnknownRarity(group("rarity"))
            val petName = groupOrNull("name") ?: ""
            val level = groupOrNull("level")?.toInt() ?: 0
            val xp = levelToXP(level, rarity, petName) ?: return null

            return PetData(
                petNameToInternalName(petName, rarity),
                petName,
                rarity,
                null,
                level,
                xp,
                newPetLine,
            )
        }
    }

    private fun handleWidgetStringLine(line: String): NEUInternalName? {
        return widgetStringPattern.matchMatcher(line) {
            val string = group("string")
            if (string == "No pet selected") {
                PetChangeEvent(pet, null).post()
                pet = null
                return null
            }
            return NEUInternalName.fromItemNameOrNull(string)
        }
    }

    private fun handleWidgetXPLine(line: String): Double? {
        xpWidgetPattern.matchMatcher(line) {
            if (hasGroup("max")) return null

            return group("overflow")?.formatDouble() ?: group("currentXP")?.formatDouble()
        }
        return null
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (autopetMessagePattern.matches(event.message)) {
            val hoverMessage = event.chatComponent.hover?.siblings?.joinToString("")?.split("\n") ?: return

            var internalName: NEUInternalName? = null
            var cleanName: String? = null
            var rarity: LorenzRarity? = null
            var level: Int? = null
            var xp: Double? = null
            var rawPetName: String? = null
            var petItem: NEUInternalName? = null
            for (line in hoverMessage) {
                val item = readAutopetItemMessage(line)
                if (item != null) {
                    petItem = item
                    continue
                }

                val data = readAutopetMessage(line)
                if (data != null) {
                    internalName = data.internalName
                    cleanName = data.cleanName
                    rarity = data.rarity
                    level = data.level
                    xp = data.xp
                    rawPetName = data.rawPetName
                    continue
                }
            }
            val petData = PetData(
                internalName ?: return,
                cleanName ?: return,
                rarity ?: return,
                petItem,
                level ?: return,
                xp ?: return,
                rawPetName ?: return,
            )
            updatePet(petData.copy(petItem = petItem))
            return
        }
        petItemMessagePattern.matchMatcher(event.message) {
            val item = NEUInternalName.fromItemNameOrNull(group("petItem")) ?: ErrorManager.skyHanniError(
                "Couldn't parse pet item name.",
                Pair("message", event.message),
                Pair("item", group("petItem"))
            )
            val newPet = pet?.copy(petItem = item) ?: return
            updatePet(newPet)
        }
    }

    private fun readAutopetMessage(string: String): PetData? {
        autopetHoverPetPattern.matchMatcher(string) {
            val level = group("level").toInt()
            val rarity = LorenzRarity.getByColorCode(group("rarity")[0]) ?: throwUnknownRarity(group("rarity"))
            val petName = group("pet")
            val hasSkin = group("skin") != null

            val fakePetLine = "§r§7[Lvl $level] §r${rarity.chatColorCode}$petName${if (hasSkin) "§r${group("skin")}" else ""}"

            return PetData(
                internalName = petNameToInternalName(petName, rarity),
                cleanName = petName,
                rarity = rarity,
                level = level,
                xp = levelToXP(level, rarity, petName) ?: 0.0,
                rawPetName = fakePetLine,
            )
        }
        return null
    }

    private fun readAutopetItemMessage(string: String): NEUInternalName? {
        return autopetHoverPetItemPattern.matchMatcher(string) {
            NEUInternalName.fromItemNameOrNull(group("item"))
        }
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        inPetMenu = isPetMenu(event.inventoryName, event.inventoryItems)
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inPetMenu = false
    }

    @SubscribeEvent
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!inPetMenu) return
        if (event.clickTypeEnum != GuiContainerEvent.ClickType.NORMAL) return
        val category = event.item?.getItemCategoryOrNull() ?: return
        if (category != ItemCategory.PET) return

        parsePetAsItem(event.item)
    }

    private fun parsePetAsItem(item: ItemStack) {
        val lore = item.getLore()

        if (lore.any { petDespawnMenuPattern.matches(it) }) {
            updatePet(null)
            return
        }

        getPetDataFromItem(item)
    }

    private fun getPetDataFromItem(item: ItemStack) {
        val (_, _, rarity, petItem, _, petXP, _) = parsePetNBT(item)
        val (internalName, name, _, _, level, _, skin) = parsePetName(item.displayName) ?: return

        val newPet = PetData(
            internalName,
            name,
            rarity,
            petItem,
            level,
            petXP,
            "§r§7[Lvl $level] §r${rarity.chatColorCode}$name${if (skin != "") "§r${skin}" else ""}",
        )
        updatePet(newPet)
    }

    private fun parsePetNBT(item: ItemStack): PetData {
        val petInfo = Gson().fromJson(item.getExtraAttributes()?.getString("petInfo"), PetNBT::class.java)

        println(petInfo)
        val rarity = LorenzRarity.getByName(petInfo.tier) ?: ErrorManager.skyHanniError(
            "Couldn't parse pet rarity.",
            Pair("petNBT", petInfo),
            Pair("rarity", petInfo.tier)
        )

        return PetData(
            internalName = NEUInternalName.NONE,
            cleanName = "",
            level = 0,
            rarity = rarity,
            petItem = petInfo.heldItem?.asInternalName(),
            xp = petInfo.exp,
            rawPetName = "",
        )
    }

    private fun parsePetName(displayName: String): PetData? {
        petNameMenuPattern.matchMatcher(displayName) {
            val name = group("name") ?: ""
            val rarity = LorenzRarity.getByColorCode(group("rarity")[0]) ?: ErrorManager.skyHanniError(
                "Couldn't parse pet rarity.",
                Pair("displayName", displayName),
                Pair("rarity", group("rarity"))
            )
            val level = group("level").toInt()
            val skin = group("skin") ?: ""

            return PetData(
                internalName = petNameToInternalName(name, rarity),
                cleanName = name,
                rarity = rarity,
                petItem = null,
                level = level,
                xp = 0.0,
                rawPetName = skin,
            )
        }
        return null
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shpetxp") {
            description = "Calculates the pet xp from a given level and rarity."
            category = CommandCategory.DEVELOPER_TEST
            callback { levelToXPCommand(it) }
        }
    }

    private fun levelToXPCommand(input: Array<String>) {
        if (input.size < 3) {
            ChatUtils.userError("Usage: /shcalcpetxp <level> <rarity> <pet>")
            return
        }

        val level = input[0].toIntOrNull()
        if (level == null) {
            ChatUtils.userError("Invalid level '${input[0]}'.")
            return
        }
        val rarity = LorenzRarity.getByName(input[1])
        if (rarity == null) {
            ChatUtils.userError("Invalid rarity '${input[1]}'.")
            return
        }

        val petName = input.slice(2..<input.size).joinToString(" ")
        val xp: Double = levelToXP(level, rarity, petName) ?: run {
            ChatUtils.userError("Invalid level or rarity.")
            return
        }
        ChatUtils.chat(xp.addSeparators())
        return
    }

    private fun levelToXP(level: Int, rarity: LorenzRarity, petName: String): Double? {
        val newPetName = petNameToFakeInternalName(petName)
        val petObject = customXpLeveling?.getAsJsonObject(newPetName)

        val rarityOffset = getRarityOffset(rarity, petObject?.getAsJsonObject("rarity_offset")) ?: return null
        if (!isValidLevel(level, petObject)) return null

        val xpList = xpLeveling + getCustomLeveling(petObject)

        return xpList.slice(0 + rarityOffset..<level + rarityOffset - 1).sum().toDouble()
    }

    private fun isValidLevel(level: Int, petObject: JsonObject?): Boolean {
        val maxLevel = petObject?.get("max_level")?.asInt ?: 100

        return maxLevel >= level
    }

    private fun getCustomLeveling(petObject: JsonObject?): List<Int> {
        return petObject?.getAsJsonArray("pet_levels")?.map { it.asInt } ?: listOf()
    }

    private fun getRarityOffset(rarity: LorenzRarity, petObject: JsonObject?): Int? {
        return petObject?.entrySet()?.associate { (rarity, offset) ->
            (LorenzRarity.getByName(rarity) ?: run {
                ChatUtils.userError("Invalid Rarity '${rarity}''")
                return null
            }) to offset.asInt
        }?.let { it[rarity] }
            ?: when (rarity) {
                LorenzRarity.COMMON -> 0
                LorenzRarity.UNCOMMON -> 6
                LorenzRarity.RARE -> 11
                LorenzRarity.EPIC -> 16
                LorenzRarity.LEGENDARY -> 20
                LorenzRarity.MYTHIC -> 20
                else -> {
                    ChatUtils.userError("Invalid Rarity \"${rarity.name}\"")
                    null
                }
            }
    }

    private fun updatePet(newPet: PetData?) {
        if (newPet == pet) return
        val oldPet = pet
        pet = newPet
        if (SkyHanniMod.feature.dev.debug.petEventMessages) {
            ChatUtils.debug("oldPet: " + oldPet.toString().convertToUnformatted())
            ChatUtils.debug("newPet: " + newPet.toString().convertToUnformatted())
        }
        currentPet = if (newPet == null) null else "§${newPet.rarity.chatColorCode}${newPet.cleanName}"
        PetChangeEvent(oldPet, newPet).post()
    }

    @SubscribeEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("PetAPI")
        if (pet == null) {
            event.addIrrelevant("no pet equipped")
            return
        }
        event.addIrrelevant {
            add("petName: '${pet?.internalName}'")
            add("petRarity: '${pet?.rarity}'")
            add("petItem: '${pet?.petItem}'")
            add("petLevel: '${pet?.level}'")
            add("petXP: '${pet?.xp}'")
            add("rawPetLine: '${pet?.rawPetName}'")
        }
    }

    @SubscribeEvent
    fun onNEURepoReload(event: NeuRepositoryReloadEvent) {
        val data = event.getConstant<NEUPetsJson>("pets")
        xpLeveling = data.petLevels
        val xpLevelingCustomJson = data.customPetLeveling.getAsJsonObject()

        customXpLeveling = xpLevelingCustomJson

        petRarityOffset = data.petRarityOffset.getAsJsonObject().entrySet().associate { (rarity, offset) ->
            (LorenzRarity.getByName(rarity) ?: throwUnknownRarity(rarity)) to offset.asInt
        }
        customDisplayToInternalName = data.internalToDisplayName
    }

    private fun petNameToFakeInternalName(petName: String): String {
        return customDisplayToInternalName.entries.find { it.value == petName }?.key?.asString()
            ?: petName.uppercase().replace(" ", "_")
    }

    private fun petNameToInternalName(petName: String, rarity: LorenzRarity): NEUInternalName {
        return "${petNameToFakeInternalName(petName)};${rarity.id}".asInternalName()
    }

    private fun throwUnknownRarity(badRarity: String): Nothing {
        ErrorManager.skyHanniError("Unknown rarity",
            Pair("rarity", badRarity)
        )
    }
}

data class PetNBT(
    val type: String,
    val active: Boolean,
    val exp: Double,
    val tier: String,
    val hideInfo: Boolean,
    val heldItem: String?,
    val candyUsed: Int,
    val skin: String?,
    val uuid: String,
    val uniqueId: String,
    val hideRightClick: Boolean,
    val noMove: Boolean
)
