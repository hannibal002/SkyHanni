package at.hannibal2.skyhanni.api.pet

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.PetData
import at.hannibal2.skyhanni.data.PetDataStorage
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuItemJson
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatDouble
import at.hannibal2.skyhanni.utils.NumberUtil.formatDoubleOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.PetUtils
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getPetInfo
import at.hannibal2.skyhanni.utils.StringUtils.removeResets
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.firstUniqueByOrNull
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.indexOfFirstOrNull
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
import at.hannibal2.skyhanni.utils.compat.hover
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import java.util.regex.Matcher
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object PetStorageApi {

    private val config get() = SkyHanniMod.feature.misc.pets
    private val petStorage get() = ProfileStorageData.petProfiles
    private val patternGroup = RepoPattern.group("misc.pet.storage")
    private const val PET_MENU_CURRENT_PET_SLOT = 4
    private const val SB_MENU_CURRENT_PET_SLOT = 30
    private const val EQUIP_MENU_CURRENT_PET_SLOT = 47
    private val EXP_SHARE_SLOTS = listOf(30, 31, 32)
    private var jsonNeedsSave: Boolean = false
    private var lastSaved: SimpleTimeMark = SimpleTimeMark.farPast()

    // <editor-fold desc="Patterns">
    /**
     * REGEX-TEST: Pets
     * REGEX-TEST: Pets (1/3)
     * REGEX-TEST: Pets: "a"
     * REGEX-TEST: Pets: "e" (1/2)
     */
    val mainPetMenuNamePattern by patternGroup.pattern(
        "menu.gui.name",
        "Pets(?:: \"(?<search>.*)\")?(?: \\((?<currentpage>\\d+)\\/(?<maxpage>\\d+)\\))? ?",
    )

    /**
     * REGEX-TEST:  §r§7[Lvl 100] §r§6Hedgehog
     * REGEX-TEST:  §r§7[Lvl 68] §r§6Blaze
     * REGEX-TEST:  §r§7[Lvl 51] §r§fKuudra
     * REGEX-TEST:  §r§7[Lvl 100] §r§dFlying Fish
     * REGEX-TEST:  §r§7[Lvl 100] §r§6Chicken§r§5 ✦
     * REGEX-TEST:  §r§7[Lvl 200] §r§8[§r§6122§4✦] §r§6Golden Dragon
     * REGEX-FAIL:  §r§7No pet selected
     */
    @Suppress("MaxLineLength")
    private val petTabWidgetNamePattern by patternGroup.pattern(
        "tab.name",
        " (?:§.)+\\[Lvl (?<level>[\\d,]+)] (?:(?:§.)+\\[(?:§.)+\\d+(?<altskin>§.✦)\\] )?(?:§.)+§(?<rarity>.)?(?<pet>[\\w ]+)(?:§r(?<skin>§. ✦))?",
    )

    /**
     * REGEX-TEST:  §r§6+§r§e163,119,730.2 XP
     * REGEX-TEST:  §r§e33,915§r§6/§r§e179.7k XP §r§6(18.9%)
     * REGEX-TEST:  §r§e2,877.5§r§6/§r§e9.7k XP §r§6(29.7%)
     * REGEX-TEST:  §r§e931,886.2§r§6/§r§e1.4M XP §r§6(67.2%)
     * REGEX-TEST:  §r§e251,016.4§r§6/§r§e561.7k XP §r§6(44.7%)
     * REGEX-TEST:  §r§e3,138.4§r§6/§r§e9.7k XP §r§6(32.4%)
     * REGEX-TEST:  §r§b§lMAX LEVEL
     */
    @Suppress("MaxLineLength")
    private val petTabWidgetXpPattern by patternGroup.pattern(
        "tab.xp",
        " (?:§.)+(?:(?<max>MAX LEVEL)|(?:\\+(?:§.)+)?(?<current>[\\d,.kM]+)(?:(?:§.|\\/)*(?<next>[\\d,.kM]+))? XP(?: (?:§.)+\\((?<percentage>[\\d.]+)%\\))?)",
    )

    /**
     * REGEX-TEST: §7§7Selected pet: §6Chicken§5 ✦
     * REGEX-TEST: §7§7Selected pet: §5Rift Ferret
     * REGEX-TEST: §7§7Selected pet: §dEndermite
     * REGEX-FAIL: §7§7Selected pet: §cNone
     */
    private val petMenuSelectedPetNamePattern by patternGroup.pattern(
        "menu.selected.name",
        "(?:§.)+Selected pet: §(?<rarity>[^c])(?<pet>[\\w ]+)(?<skin>§. ✦)?",
    )

    /**
     * REGEX-TEST: §7Progress to Level 52: §e29.7%
     * REGEX-TEST: §7Progress to Level 2: §e0%
     * REGEX-TEST: §7Progress to Level 69: §e18.9%
     * REGEX-TEST: §b§lMAX LEVEL
     */
    private val petMenuSelectedPetProgressPattern by patternGroup.pattern(
        "menu.selected.progress",
        "(?:§.)+(?:MAX LEVEL|Progress to Level (?<next>\\d+): (?:§.)+(?<percentage>[\\d.]+)%)",
    )

    /**
     * REGEX-TEST: §2§l§m        §f§l§m                 §r §e2,877.5§6/§e9.7k
     * REGEX-TEST: §2§l§m     §f§l§m                    §r §e33,915§6/§e179.7k
     * REGEX-TEST: §2§l§m                 §f§l§m        §r §e931,886.2§6/§e1.4M
     * REGEX-TEST: §f§l§m                         §r §e0§6/§e660
     * REGEX-TEST: §8▸ 25,353,248 XP
     */
    private val petMenuSelectedPetXpPattern by patternGroup.pattern(
        "menu.selected.xp",
        "(?:§.|▸| )+(?<current>[\\d,.kM]+)(?: XP|(?:§.|\\/)+(?<next>[\\d,.kM]+))",
    )

    /**
     * REGEX-TEST: §cAutopet §eequipped your §7[Lvl 100] §dEnderman§e! §a§lVIEW RULE
     * REGEX-TEST: §cAutopet §eequipped your §7[Lvl 200] §6Golden Dragon§e! §a§lVIEW RULE
     * REGEX-TEST: §cAutopet §eequipped your §7[Lvl 100] §dRabbit§9 ✦§e! §a§lVIEW RULE
     * REGEX-TEST: §cAutopet §eequipped your §7[Lvl 200] §r§8[§r§6122§4✦] §r§6Golden Dragon§e! §a§lVIEW RULE
     * REGEX-TEST: §cAutopet §eequipped your §7[Lvl 200] §8[§634§8§4✦§8] §6Golden Dragon§e! §a§lVIEW RULE
     */
    @Suppress("MaxLineLength")
    private val autoPetMessagePattern by patternGroup.pattern(
        "autopet.message",
        "§cAutopet §eequipped your §7\\[Lvl (?<level>\\d+)] (?:(?:§.)+\\[(?:§.)*\\d+(?:§.)*(?<altskin>§.✦)(?:§.)*\\] )?(?:§.)*§(?<rarity>.)(?<pet>[^§]+)(?<skin>§. ✦)?§e! §a§lVIEW RULE",
    )

    /**
     * REGEX-TEST: §aHeld Item: §9Farming Exp Boost
     */
    private val autoPetHoverHeldItemPattern by patternGroup.pattern(
        "autopet.hover.helditem",
        "§aHeld Item: (?<item>.*)",
    )
    // </editor-fold>

    private fun Int.isPetStackLocation() = this in 10..43 &&
        this % 9 != 0 && (this + 1) % 9 != 0

    private fun Matcher.getPetSkinOrNull(petInternalName: NeuInternalName): NeuItemJson? {
        val skin = groupOrNull("skin") ?: groupOrNull("altskin") ?: return null
        return PetUtils.findPetSkinOrNull(petInternalName, skin)
    }

    private fun Matcher.getRarityOrNull() = LorenzRarity.getByColorCode(group("rarity")[0])

    @HandleEvent
    fun onSecondPassed() {
        if (!jsonNeedsSave || lastSaved.passedSince() < 30.seconds) return
        SkyHanniMod.configManager.saveConfig(ConfigFileType.PETS, "saving-data")
        jsonNeedsSave = false
        lastSaved = SimpleTimeMark.now()
    }

    @HandleEvent(onlyOnSkyblock = true, priority = HandleEvent.HIGHEST)
    fun onWidgetUpdate(event: WidgetUpdateEvent) {
        if (!event.isWidget(TabWidget.PET)) return
        petTabWidgetNamePattern.firstMatcher(event.lines) {
            val petName = groupOrNull("pet") ?: return@firstMatcher false
            val level = group("level").toInt()
            val rarity = getRarityOrNull() ?: return@firstMatcher false
            val petHeldItem = event.lines.firstNotNullOfOrNull { line ->
                PetUtils.resolvePetItemOrNull(line.trim().removeResets())
            }

            val petExp = petTabWidgetXpPattern.firstMatcher(event.lines) expFirstMatcher@{
                // We don't know XP if it's just "MAX LEVEL"
                if (groupOrNull("max") != null) return@expFirstMatcher null
                val petInternalName = PetUtils.petWithRarityToInternalName(petName, rarity)
                val currentLevelXp = PetUtils.levelToXp(level, petInternalName) ?: return@expFirstMatcher null
                val readXpGroup = groupOrNull("current")?.formatDoubleOrNull() ?: 0.0
                currentLevelXp + readXpGroup
            }

            val resolvedPet = resolvePetDataOrNull(
                name = petName,
                rarity = rarity,
                level = level,
                heldItem = petHeldItem,
                exp = petExp,
            ) ?: return@firstMatcher false

            // Apply all the data we know for sure to the pet
            resolvedPet.apply {
                exp = petExp?.takeIf { it > (exp ?: 0.0) } ?: exp
                skinInternalName = getPetSkinOrNull(fauxInternalName)?.internalName ?: skinInternalName
                heldItemInternalName = petHeldItem ?: heldItemInternalName
            }

            CurrentPetApi.assertFoundCurrentData(resolvedPet, CurrentPetApi.PetDataAssertionSource.TAB)
            jsonNeedsSave = true
        }
    }

    @HandleEvent(priority = HandleEvent.HIGHEST)
    fun onChat(event: SkyHanniChatEvent) {
        autoPetMessagePattern.matchMatcher(event.message) {
            if (config.hideAutopet) event.blockedReason = "autopet"

            val petName = groupOrNull("pet") ?: return
            val level = group("level").toInt()
            val rarity = getRarityOrNull() ?: return
            val petInternalName = PetUtils.petWithRarityToInternalName(petName, rarity)
            val petSkin = getPetSkinOrNull(petInternalName)
            val petSkinTag = groupOrNull("skin")

            val hoverInfo = event.chatComponent.hover?.siblings?.joinToString {
                it.formattedTextCompat()
            }?.split("\n") ?: return

            val petHeldItem = autoPetHoverHeldItemPattern.firstMatcher(hoverInfo) {
                PetUtils.resolvePetItemOrNull(group("item").removeResets())
            }

            val resolvedPet = resolvePetDataOrNull(
                name = petName,
                rarity = rarity,
                heldItem = petHeldItem,
                skinTag = petSkinTag,
                level = level,
            ) ?: return

            // Apply all the data we know for sure to the pet
            resolvedPet.apply {
                skinInternalName = petSkin?.internalName ?: skinInternalName
                heldItemInternalName = petHeldItem ?: heldItemInternalName
            }

            CurrentPetApi.assertFoundCurrentData(resolvedPet, CurrentPetApi.PetDataAssertionSource.AUTOPET)
            jsonNeedsSave = true
        }
    }

    @HandleEvent(onlyOnSkyblock = true, priority = HandleEvent.HIGHEST)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!mainPetMenuNamePattern.matches(InventoryUtils.openInventoryName())) return
        val clickedItem = event.item ?: return
        val petInfo = clickedItem.getPetInfo() ?: return
        val currentPetUuid = ProfileStorageData.profileSpecific?.currentPetUuid
        when (event.clickedButton) {
            1 -> { // Right click - remove pet from menu
                petStorage?.pets?.removeIf { it.uuid == petInfo.uniqueId }
                if (currentPetUuid == petInfo.uniqueId) {
                    ProfileStorageData.profileSpecific?.currentPetUuid = null
                }
            }

            0 -> { // Left click - if not a shift click, summon/un-summon pet
                if (KeyboardManager.isShiftKeyDown()) return
                ProfileStorageData.profileSpecific?.currentPetUuid = when (currentPetUuid) {
                    petInfo.uniqueId -> null // Un-summon
                    else -> petInfo.uniqueId // Summon
                }
            }

            else -> return
        }
        jsonNeedsSave = true
    }

    @HandleEvent(onlyOnSkyblock = true, priority = HandleEvent.HIGHEST)
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        event.readPetsMenuItems()
        event.readEquipmentPetData()
        event.readSelectedPetData()
        event.readExpSharePets()
    }

    private fun InventoryFullyOpenedEvent.readPetsMenuItems() {
        if (!mainPetMenuNamePattern.matches(inventoryName)) return
        val petStorage = petStorage ?: return

        inventoryItems.filter { (slotNumber, stack) ->
            slotNumber.isPetStackLocation() && stack.getInternalNameOrNull() != null
        }.mapNotNull { (_, item) ->
            val petInfo = item.getPetInfo() ?: return@mapNotNull null
            PetData(
                petInternalName = item.getInternalName(),
                skinInternalName = petInfo.properSkinItem,
                heldItemInternalName = petInfo.heldItem,
                exp = petInfo.exp,
                uuid = petInfo.uniqueId,
                skinVariantIndex = petInfo.getSkinVariantIndex() ?: -1,
            )
        }.forEach { data ->
            // Because this inventory is the "source of truth", if we come across the same UUID
            // we should always replace the data in-place
            petStorage.pets.indexOfFirstOrNull { it.uuid == data.uuid }?.let {
                petStorage.pets[it] = data
            } ?: petStorage.pets.add(data)
        }
        jsonNeedsSave = true
    }

    private fun InventoryFullyOpenedEvent.readEquipmentPetData() {
        if (inventoryName != "Your Equipment and Stats") return
        val petStorage = petStorage ?: return
        val currentPetItem = inventoryItems[EQUIP_MENU_CURRENT_PET_SLOT]?.takeIf {
            it.displayName != "§7Empty Pet Slot"
        } ?: return
        val petInfo = currentPetItem.getPetInfo() ?: return

        val data = PetData(
            petInternalName = currentPetItem.getInternalName(),
            skinInternalName = petInfo.properSkinItem,
            heldItemInternalName = petInfo.heldItem,
            exp = petInfo.exp,
            uuid = petInfo.uniqueId,
            skinVariantIndex = petInfo.getSkinVariantIndex() ?: -1,
        )

        petStorage.pets.indexOfFirstOrNull { it.uuid == petInfo.uniqueId }?.let {
            petStorage.pets[it] = data
        } ?: petStorage.pets.add(data)

        CurrentPetApi.assertFoundCurrentData(data, CurrentPetApi.PetDataAssertionSource.MENU)
        jsonNeedsSave = true
    }

    private fun InventoryFullyOpenedEvent.readSelectedPetData() {
        val petItemSlot = when {
            mainPetMenuNamePattern.matches(inventoryName) -> PET_MENU_CURRENT_PET_SLOT
            inventoryName == "SkyBlock Menu" -> SB_MENU_CURRENT_PET_SLOT
            else -> return
        }
        val currentPetItemLore = inventoryItems[petItemSlot]?.getLore()?.takeIfNotEmpty() ?: return

        petMenuSelectedPetNamePattern.firstMatcher(currentPetItemLore) {
            val petName = groupOrNull("pet") ?: return@firstMatcher false
            val rarity = getRarityOrNull() ?: return@firstMatcher false
            val petInternalname = PetUtils.petWithRarityToInternalName(petName, rarity)
            val petSkin = getPetSkinOrNull(petInternalname)
            val petSkinTag = groupOrNull("skin")

            val level = petMenuSelectedPetProgressPattern.firstMatcher(currentPetItemLore) {
                when (groupOrNull("next")) {
                    null -> PetUtils.getMaxLevel(petInternalname)
                    else -> (group("next").formatInt() - 1)
                }
            } ?: return@firstMatcher false

            val petExp = petMenuSelectedPetXpPattern.firstMatcher(currentPetItemLore) {
                val currentValue = group("current").formatDouble()
                when (groupOrNull("next")) {
                    null -> currentValue
                    else -> {
                        val currentLevelXp = PetUtils.levelToXp(level, petInternalname) ?: 0.0
                        currentLevelXp + currentValue
                    }
                }
            }

            val resolvedPet = resolvePetDataOrNull(
                name = petName,
                skinTag = petSkinTag,
                rarity = rarity,
                level = level,
                exp = petExp,
            ) ?: return

            // Apply all the data we know for sure to the pet
            resolvedPet.apply {
                exp = petExp?.takeIf { it > (exp ?: 0.0) } ?: exp
                skinInternalName = petSkin?.internalName ?: skinInternalName
            }

            CurrentPetApi.assertFoundCurrentData(resolvedPet, CurrentPetApi.PetDataAssertionSource.MENU)
            jsonNeedsSave = true
        }
    }

    private fun InventoryFullyOpenedEvent.readExpSharePets() {
        if (inventoryName != "Exp Sharing") return
        val petStorage = petStorage ?: return
        petStorage.expSharePets.clear()
        petStorage.expSharePets.addAll(
            EXP_SHARE_SLOTS.map { expShareSlot ->
                val slotItem = inventoryItems[expShareSlot]?.takeIf {
                    it.displayName != "§7No pet in slot"
                } ?: return@map null
                slotItem.getPetInfo()?.uniqueId
            },
        )
    }

    fun resolvePetDataOrNull(
        name: String,
        rarity: LorenzRarity? = null,
        heldItem: NeuInternalName? = null,
        skinTag: String? = null,
        level: Int? = null,
        exp: Double? = null,
        expErrorFactor: Double = 0.01,
    ): PetData? = petStorage?.pets?.filter {
        it.uuid != null
    }?.takeIfNotEmpty()?.firstUniqueByOrNull(
        { it.cleanName == name },
        { rarity == null || it.rarity == rarity },
        { heldItem == null || it.heldItemInternalName == heldItem },
        { skinTag == null || it.skinTag == skinTag },
        { level == null || it.level == level },
        { exp == null || abs((it.exp ?: 0.0) - exp) < (exp * expErrorFactor) },
    )

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shresetpetstorage") {
            description = "Removes all pets from SkyHanni's storage"
            category = CommandCategory.USERS_RESET
            callback {
                ProfileStorageData.petProfiles = PetDataStorage.ProfileSpecific()
                ChatUtils.clickableChat(
                    "Cleared all pets from storage. Re-open the §b/pet §emenu to re-populate it.",
                    onClick = { HypixelCommands.pet() },
                    hover = "Click to re-open the pet menu",
                )
            }
        }
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        fun PetData.formatForDebug() = fauxInternalName.asString() + ":<lvl$level>:" + uuid.toString()
        event.title("Pet Storage API")
        event.addIrrelevant {
            val petStorage = petStorage ?: run {
                add("petStorage is null")
                return@addIrrelevant
            }
            LorenzRarity.entries.reversed().forEach { rarity ->
                val pets = petStorage.pets.filter { it.rarity == rarity }.takeIfNotEmpty() ?: return@forEach
                add("pets (${rarity.name}):\n" + pets.joinToString(", ", transform = PetData::formatForDebug))
                add("")
            }
            add("expSharePets: " + petStorage.expSharePets.joinToString(", ") { it?.toString() ?: "<empty>" })
        }
    }
}
