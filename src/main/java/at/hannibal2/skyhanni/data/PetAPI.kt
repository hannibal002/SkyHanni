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
import at.hannibal2.skyhanni.utils.ItemUtils.extraAttributes
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatDoubleOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatches
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.convertToUnformatted
import at.hannibal2.skyhanni.utils.chat.Text.hover
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.gson.Gson
import com.google.gson.JsonObject
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
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

    @Deprecated(message = "use PetAPI.inPetMenu")
    fun isPetMenu(inventoryTitle: String): Boolean = petMenuPattern.matches(inventoryTitle)

    // Contains color code + name and for older SkyHanni users maybe also the pet level
    @Deprecated(message = "use PetAPI.pet.name")
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

// ---
    @SubscribeEvent
    fun onWidgetUpdate(event: WidgetUpdateEvent) {
        if (!event.isWidget(TabWidget.PET)) return

        val newPetLine = petWidgetPattern.firstMatches(event.lines)?.trim() ?: return
        if (newPetLine == pet?.rawPetName) return

        var newPetData = PetData()
        var overflowXP = 0.0
        event.lines.forEach { line ->
            val tempPetItem = handleWidgetStringLine(line)
            if (tempPetItem != null) {
                newPetData = newPetData.copy(petItem = tempPetItem)
                return@forEach
            }

            val tempPetXP = handleWidgetXPLine(line)
            if (tempPetXP != null) {
                overflowXP = tempPetXP
                return@forEach
            }

            val tempPetMisc = handleWidgetPetLine(line, newPetLine)
            if (tempPetMisc != null) {
                newPetData = newPetData.copy(
                    internalName = tempPetMisc.internalName,
                    cleanName = tempPetMisc.cleanName,
                    rarity = tempPetMisc.rarity,
                    level = tempPetMisc.level,
                    xp = tempPetMisc.xp,
                    rawPetName = tempPetMisc.rawPetName,
                )
                return@forEach
            }
        }
        fireEvent(newPetData.copy(xp = newPetData.xp + overflowXP))
    }

    private fun handleWidgetPetLine(line: String, newPetLine: String): PetData? {
        petWidgetPattern.matchMatcher(line) {
            val rarity = LorenzRarity.getByColorCode(group("rarity")[0]) ?: LorenzRarity.ULTIMATE
            val petName = group("name")
            val level = group("level").toInt()
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
        return null
    }

    private fun handleWidgetStringLine(line: String): NEUInternalName? {
        widgetStringPattern.matchMatcher(line) {
            val string = group("string")
            if (string == "No pet selected") {
                PetChangeEvent(pet, null).post()
                pet = null
                return null
            }
            return NEUInternalName.fromItemNameOrNull(string)
        }
        return null
    }

    private fun handleWidgetXPLine(line: String): Double? {
        xpWidgetPattern.matchMatcher(line) {
            if (group("max") != null) return null

            val overflow = group("overflow")?.formatDoubleOrNull() ?: 0.0
            val currentXP = group("currentXP")?.formatDoubleOrNull() ?: 0.0

            return overflow + currentXP
        }
        return null
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (autopetMessagePattern.matches(event.message)) {
            val hoverMessage = event.chatComponent.hover?.siblings?.joinToString("")?.split("\n") ?: return

            var petItem = NEUInternalName.NONE
            for (it in hoverMessage) {
                val item = handleAutopetItemMessage(it)
                if (item != null) {
                    petItem = item
                    break
                }
            }
            hoverMessage.forEach {
                if (handleAutopetMessage(it, petItem)) return
            }
            return
        }
        petItemMessagePattern.matchMatcher(event.message) {
            val item = NEUInternalName.fromItemNameOrNull(group("petItem")) ?: ErrorManager.skyHanniError(
                "Couldn't parse pet item name.",
                Pair("message", event.message),
                Pair("item", group("petItem"))
            )
            val newPet = pet?.copy(petItem = item) ?: return
            fireEvent(newPet)
        }
    }

    private fun handleAutopetMessage(string: String, petItem: NEUInternalName): Boolean {
        autopetHoverPetPattern.matchMatcher(string) {
            val level = group("level").toInt()
            val rarity = LorenzRarity.getByColorCode(group("rarity")[0]) ?: LorenzRarity.ULTIMATE
            val petName = group("pet")
            val hasSkin = group("skin") != null

            val fakePetLine = "§r§7[Lvl $level] §r${rarity.chatColorCode}$petName${if (hasSkin) "§r${group("skin")}" else ""}"

            val newPet = PetData(
                petNameToInternalName(petName, rarity),
                petName,
                rarity,
                petItem,
                level,
                levelToXP(level, rarity, petName) ?: 0.0,
                fakePetLine,
            )
            fireEvent(newPet)
            return true
        }
        return false
    }

    private fun handleAutopetItemMessage(string: String): NEUInternalName? {
        return autopetHoverPetItemPattern.matchMatcher(string) {
            NEUInternalName.fromItemNameOrNull(group("item"))
        }
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!petMenuPattern.matches(event.inventoryName)) {
            inPetMenu = false
            return
        }
        val goBackLore = event.inventoryItems[48]?.getLore() ?: emptyList()
        if (goBackLore.any { forgeBackMenuPattern.matches(it) }) {
            inPetMenu = false
            return
        }

        inPetMenu = true
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
            fireEvent(null)
            return
        }

        getPetDataFromItem(item)
    }

    private fun getPetDataFromItem(item: ItemStack) {
        val (_, _, rarity, petItem, _, petXP, _) = parsePetNBT(item.extraAttributes)
        val (internalName, name, _, _, level, _, skin) = parsePetName(item.displayName)

        val newPet = PetData(
            internalName,
            name,
            rarity,
            petItem,
            level,
            petXP,
            "§r§7[Lvl $level] §r${rarity.chatColorCode}$name${if (skin != "") "§r${skin}" else ""}",
        )
        fireEvent(newPet)
    }

    private fun parsePetNBT(nbt: NBTTagCompound): PetData {
        val jsonString = nbt.getTag("petInfo").toString()
            .replace("\\", "")
            .removePrefix("\"")
            .removeSuffix("\"")
        val petInfo = Gson().fromJson(jsonString, PetNBT::class.java)

        return PetData(
            NEUInternalName.NONE,
            "",
            LorenzRarity.getByName(petInfo.tier) ?: LorenzRarity.ULTIMATE,
            petInfo.heldItem?.asInternalName(),
            0,
            petInfo.exp,
            ""
        )
    }

    private fun parsePetName(displayName: String): PetData {
        var name = ""
        var rarity = LorenzRarity.ULTIMATE
        var level = 0
        var skin = ""

        petNameMenuPattern.matchMatcher(displayName) {
            name = group("name") ?: ""
            rarity = LorenzRarity.getByColorCode(group("rarity")[0]) ?: LorenzRarity.ULTIMATE
            level = group("level").toInt()
            skin = group("skin") ?: ""
        }

        return PetData(
            petNameToInternalName(name, rarity),
            name,
            rarity,
            null,
            level,
            0.0,
            skin,
        )
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shcalcpetxp") {
            description = "Gets the pet xp from a given level and rarity."
            category = CommandCategory.DEVELOPER_TEST
            callback { testLevelToXP(it) }
        }
    }

    private fun testLevelToXP(input: Array<String>) {
        if (input.size >= 3) {
            val level = input[0].toIntOrNull()
            val rarity = LorenzRarity.getByName(input[1])
            val petName = input.slice(2..<input.size).joinToString(" ")
            if (level != null && rarity != null) {
                val xp: Double = levelToXP(level, rarity, petName) ?: run {
                    ChatUtils.userError("Invalid level or rarity.")
                    return
                }
                ChatUtils.chat(xp.addSeparators())
                return
            }
        }
        ChatUtils.userError("Invalid Usage. /shcalcpetxp <level> <rarity> <pet>")
    }

    private fun levelToXP(level: Int, rarity: LorenzRarity, petName: String = ""): Double? {
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
        return if (petObject == null) {
            when (rarity) {
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
        } else {
            petObject.entrySet().associate { (rarity, offset) ->
                (LorenzRarity.getByName(rarity) ?: LorenzRarity.ULTIMATE) to offset.asInt
            }[rarity]
        }
    }

    private fun fireEvent(newPet: PetData?) {
        val oldPet = pet
        pet = newPet
        if (newPet == oldPet) return
        if (SkyHanniMod.feature.dev.debug.petEventMessages) {
            ChatUtils.debug("oldPet: " + oldPet.toString().convertToUnformatted())
            ChatUtils.debug("newPet: " + newPet.toString().convertToUnformatted())
        }
        PetChangeEvent(oldPet, newPet).post()
    }

    @SubscribeEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("PetAPI")
        if (pet != null) {
            event.addIrrelevant {
                add("petName: '${pet?.internalName}'")
                add("petRarity: '${pet?.rarity}'")
                add("petItem: '${pet?.petItem}'")
                add("petLevel: '${pet?.level}'")
                add("petXP: '${pet?.xp}'")
                add("rawPetLine: '${pet?.rawPetName}'")
            }
        } else {
            event.addData("no pet equipped")
        }
    }

    @SubscribeEvent
    fun onNEURepoReload(event: NeuRepositoryReloadEvent) {
        val data = event.getConstant<NEUPetsJson>("pets")
        xpLeveling = data.petLevels
        val xpLevelingCustomJson = data.customPetLeveling.getAsJsonObject()

        customXpLeveling = xpLevelingCustomJson

        petRarityOffset = data.petRarityOffset.getAsJsonObject().entrySet().associate { (rarity, offset) ->
            (LorenzRarity.getByName(rarity) ?: LorenzRarity.ULTIMATE) to offset.asInt
        }
        customDisplayToInternalName = data.displayToInternalName
    }

    private fun petNameToFakeInternalName(petName: String): String {
        return customDisplayToInternalName.entries.find { it.value == petName }?.key?.asString()
            ?: petName.uppercase().replace(" ", "_")
    }

    private fun petNameToInternalName(petName: String, rarity: LorenzRarity): NEUInternalName {
        return "${petNameToFakeInternalName(petName)};${rarity.id}".asInternalName()
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
