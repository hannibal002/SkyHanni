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
import at.hannibal2.skyhanni.utils.ItemUtils.itemNameWithoutColor
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatDouble
import at.hannibal2.skyhanni.utils.NumberUtil.formatDoubleOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.PetUtils
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.PetInfo
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getPetInfo
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.StringUtils.removeResets
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.indexOfFirstOrNull
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
import at.hannibal2.skyhanni.utils.compat.hover
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.orNull
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.network.chat.Component
import java.util.UUID
import java.util.regex.Matcher
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
    private var lastExactPetMenuClick: SimpleTimeMark = SimpleTimeMark.farPast()

    // <editor-fold desc="Patterns">
    /**
     * REGEX-TEST: Pets
     * REGEX-TEST: Pets (1/3)
     * REGEX-TEST: (6/6) Pets
     * REGEX-TEST: Pets: "a"
     * REGEX-TEST: Pets: "e" (1/2)
     * REGEX-TEST: (6/6) Pets: "e"
     */
    private val mainPetMenuNamePattern by patternGroup.pattern(
        "menu.gui.title",
        "(?:\\(\\d+\\/\\d+\\) )?Pets(?:: \"(?<search>.*)\")?(?: \\(\\d+\\/\\d+\\))? ?",
    )

    /**
     * REGEX-TEST: §7[Lvl 8] §6Squid
     * REGEX-TEST: §7[Lvl 100] §dHermit Crab
     * REGEX-TEST: §7[Lvl 200] §8[§6122§4✦§8] §6Golden Dragon
     */
    @Suppress("MaxLineLength")
    private val petMenuPetStackNamePattern by patternGroup.pattern(
        "menu.petstack.name",
        "(?:§.)*\\[Lvl (?<level>[\\d,]+)] (?:(?:§.)+\\[(?:§.)*\\d+(?:§.)*(?<altskin>§.✦)(?:§.)*] )?(?:§.)*§(?<rarity>.)(?<pet>[^§]+?)(?<skin>§. ✦)?",
    )

    /**
     * REGEX-TEST:  [Lvl 100] Hedgehog
     * REGEX-TEST:  [Lvl 68] Blaze
     * REGEX-TEST:  [Lvl 51] Kuudra
     * REGEX-TEST:  [Lvl 100] Flying Fish
     * REGEX-TEST:  [Lvl 100] Chicken ✦
     * REGEX-TEST:  [Lvl 200] [122✦] Golden Dragon
     * REGEX-FAIL:  No pet selected
     */
    @Suppress("MaxLineLength")
    private val petTabWidgetNamePattern by patternGroup.pattern(
        "tab.name",
        " \\[Lvl (?<level>[\\d,]+)] (?:\\[\\d+(?<altskin>✦)\\] )?(?<pet>[\\w ]+?)(?:(?<skin> ✦))?$",
    )

    /**
     * REGEX-TEST:  +163,119,730.2 XP
     * REGEX-TEST:  33,915/179.7k XP (18.9%)
     * REGEX-TEST:  2,877.5/9.7k XP (29.7%)
     * REGEX-TEST:  931,886.2/1.4M XP (67.2%)
     * REGEX-TEST:  251,016.4/561.7k XP (44.7%)
     * REGEX-TEST:  3,138.4/9.7k XP (32.4%)
     * REGEX-TEST:  MAX LEVEL
     */
    @Suppress("MaxLineLength")
    private val petTabWidgetXpPattern by patternGroup.pattern(
        "tab.xp",
        " (?:(?<max>MAX LEVEL)|(?:\\+)?(?<current>[\\d,.kM]+)(?:(?:|\\/)*(?<next>[\\d,.kM]+))? XP(?: \\((?<percentage>[\\d.]+)%\\))?)",
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
     * REGEX-TEST: Held Item: Poignant Lucky Clover
     * REGEX-TEST: Equip: [Lvl 200] Rose Dragon Held Item: Poignant Lucky Clover
     */
    private val autoPetHoverHeldItemPattern by patternGroup.pattern(
        "autopet.hover.helditem.clean",
        ".*Held Item: (?<item>.*)",
    )
    // </editor-fold>

    private fun Int.isPetStackLocation() = this in 10..43 &&
        this % 9 != 0 && (this + 1) % 9 != 0

    private fun Matcher.getPetSkinOrNull(petInternalName: NeuInternalName): NeuItemJson? {
        val skin = groupOrNull("skin") ?: groupOrNull("altskin") ?: return null
        return PetUtils.findPetSkinOrNull(petInternalName, skin)
    }

    private fun Matcher.getRarityOrNull() = LorenzRarity.getByColorCode(group("rarity")[0])

    private val PetInfo.ownedUuid: UUID? get() = uniqueId ?: uuid

    private fun MutableList<PetData>.addOrReplace(petData: PetData) {
        indexOfFirstOrNull { it.uuid == petData.uuid }?.let {
            this[it] = petData
        } ?: add(petData)
    }

    private fun PetData.applyKnownData(
        exp: Double? = null,
        skinInternalName: NeuInternalName? = null,
        heldItemInternalName: NeuInternalName? = null,
    ) {
        this.exp = exp ?: this.exp
        this.skinInternalName = skinInternalName ?: this.skinInternalName
        this.heldItemInternalName = heldItemInternalName ?: this.heldItemInternalName
    }

    private val PetExpRead?.exactValue get() = this?.takeIf { it.exact }?.value

    private fun PetData.reconcileDisplayedExp(readExp: Double): Double {
        val storedExp = exp ?: return readExp
        if (readExp % 1.0 != 0.0) return readExp
        return storedExp.coerceIn(readExp, readExp + 0.999)
    }

    private fun String.isExactPetExpText() =
        !contains('k', ignoreCase = true) && !contains('m', ignoreCase = true)

    fun isMainPetMenuName(inventoryName: String?) = mainPetMenuNamePattern.matches(inventoryName)

    private data class PetExpRead(
        val value: Double,
        val exact: Boolean,
    )

    private fun SafeItemStack.toVisiblePetDataOrNull(petInfo: PetInfo? = getPetInfo()): PetData? =
        petMenuPetStackNamePattern.matchMatcher(hoverName.formattedTextCompat()) {
            val level = group("level").formatInt()
            val petName = groupOrNull("pet")?.trim() ?: return@matchMatcher null
            val itemInternalName = getInternalNameOrNull()?.takeIf { PetUtils.getPetRarity(it) != null }
            val petInternalName = itemInternalName ?: run {
                val rarity = getRarityOrNull() ?: return@matchMatcher null
                PetUtils.petWithRarityToInternalName(petName, rarity)
            }
            val petSkin = getPetSkinOrNull(petInternalName)
            val lore = getLore()
            val petExp = petMenuSelectedPetXpPattern.firstMatcher(lore) {
                val currentValue = group("current").formatDouble()
                when (groupOrNull("next")) {
                    null -> currentValue
                    else -> {
                        val currentLevelXp = PetUtils.levelToXp(level, petInternalName) ?: 0.0
                        currentLevelXp + currentValue
                    }
                }
            }
            val petInfoExp = petInfo?.exp?.takeIf { it > 0.0 || level <= 1 }
            PetData(
                petInternalName = petInternalName,
                skinInternalName = petInfo?.properSkinItem ?: petSkin?.internalName,
                heldItemInternalName = petInfo?.heldItem,
                exp = petInfoExp ?: petExp ?: PetUtils.levelToXp(level, petInternalName) ?: 0.0,
                uuid = petInfo?.ownedUuid,
                skinVariantIndex = petInfo?.getSkinVariantIndex(),
            )
        }

    private fun SafeItemStack.toClickedPetDataOrNull(petInfo: PetInfo? = getPetInfo()): PetData? =
        petInfo?.let(::PetData) ?: toVisiblePetDataOrNull(null)

    private fun SafeItemStack.toExactPetDataOrNull(): PetData? =
        getPetInfo()?.let { petInfo ->
            PetData(
                petInternalName = getInternalName(),
                skinInternalName = petInfo.properSkinItem,
                heldItemInternalName = petInfo.heldItem,
                exp = petInfo.exp,
                uuid = petInfo.ownedUuid,
                skinVariantIndex = petInfo.getSkinVariantIndex(),
            )
        }

    private fun SafeItemStack.isCurrentPetStack() = getLore().any { it.contains("Click to despawn") }

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
        for (component in event.lines) {
            petTabWidgetNamePattern.matchMatcher(component.string) {
                val petName = groupOrNull("pet") ?: return@matchMatcher false
                val level = group("level").toInt()
                val rarity: LorenzRarity =
                    LorenzRarity.getByComponent(component, group("pet")) ?: return@matchMatcher false
                val petInternalName = PetUtils.petWithRarityToInternalName(petName, rarity)
                val petSkin = getPetSkinOrNull(petInternalName)
                val petSkinTag = (groupOrNull("skin") ?: groupOrNull("altskin"))?.replace(" ", "")
                val petSkinTagKnown = petSkinTag == null
                val petHeldItem = event.lines.firstNotNullOfOrNull { line ->
                    val trimmedLine =
                        line.formattedTextCompat().trim().removeResets().takeIf { it.isNotBlank() }
                            ?: return@firstNotNullOfOrNull null
                    PetUtils.resolvePetItemOrNull(trimmedLine)
                }

                val petExp = petTabWidgetXpPattern.firstMatcher(event.lines.map { it.string }) expFirstMatcher@{
                    // We don't know XP if it's just "MAX LEVEL"
                    if (groupOrNull("max") != null) return@expFirstMatcher null
                    val currentLevelXp = PetUtils.levelToXp(level, petInternalName) ?: return@expFirstMatcher null
                    val current = groupOrNull("current") ?: "0"
                    val readXpGroup = current.formatDoubleOrNull() ?: 0.0
                    PetExpRead(currentLevelXp + readXpGroup, current.isExactPetExpText())
                }

                val resolvedPet = resolvePetDataOrNull(
                    name = petName,
                    rarity = rarity,
                    level = level,
                    heldItem = petHeldItem,
                    skinTagKnown = petSkinTagKnown,
                    exp = petExp?.value,
                )
                val matchingCurrentPet = CurrentPetApi.currentPet?.takeIf { currentPet ->
                    currentPet.cleanName == petName &&
                        currentPet.rarity == rarity &&
                        currentPet.level == level &&
                        (petSkinTag == null || currentPet.skinTag == petSkinTag)
                }
                val currentPetData = resolvedPet ?: matchingCurrentPet ?: PetData(
                    petInternalName = petInternalName,
                    skinInternalName = petSkin?.internalName,
                    heldItemInternalName = petHeldItem,
                    exp = petExp?.value ?: PetUtils.levelToXp(level, petInternalName) ?: 0.0,
                )

                currentPetData.applyKnownData(
                    exp = petExp.exactValue?.let { currentPetData.reconcileDisplayedExp(it) },
                    skinInternalName = petSkin?.internalName,
                    heldItemInternalName = petHeldItem,
                )

                CurrentPetApi.assertFoundCurrentData(currentPetData, CurrentPetApi.PetDataAssertionSource.TAB)
                jsonNeedsSave = true
            }
        }
    }

    @HandleEvent(priority = HandleEvent.HIGHEST)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        autoPetMessagePattern.matchMatcher(event.message) {
            if (config.hideAutopet) event.blockedReason = "autopet"

            val petName = groupOrNull("pet") ?: return
            val level = group("level").toInt()
            val rarity = getRarityOrNull() ?: return
            val petInternalName = PetUtils.petWithRarityToInternalName(petName, rarity)
            val petSkin = getPetSkinOrNull(petInternalName)
            val petSkinTag = (groupOrNull("skin") ?: groupOrNull("altskin"))?.replace(" ", "")

            val hoverInfo = event.chatComponent.hoverTextLines()
            val petHeldItemName = autoPetHoverHeldItemPattern.firstMatcher(hoverInfo.map { it.removeResets() }) {
                group("item")
            }?.trim()
            val petHeldItem = petHeldItemName?.let(PetUtils::resolvePetItemOrNull)

            val resolvedPet = resolveAutopetPetDataOrNull(
                name = petName,
                rarity = rarity,
                heldItemName = petHeldItemName,
                heldItemKnown = hoverInfo.isNotEmpty(),
                skinTag = petSkinTag,
                level = level,
            ) ?: PetData(
                petInternalName = petInternalName,
                exp = PetUtils.levelToXp(level, petInternalName),
            )

            resolvedPet.applyKnownData(skinInternalName = petSkin?.internalName)
            when {
                petHeldItem != null -> resolvedPet.heldItemInternalName = petHeldItem
                hoverInfo.isNotEmpty() && petHeldItemName == null -> resolvedPet.heldItemInternalName = null
            }
            PetUtils.levelToXp(level, resolvedPet.fauxInternalName)?.let { minimumExp ->
                if ((resolvedPet.exp ?: 0.0) < minimumExp) resolvedPet.exp = minimumExp
            }

            CurrentPetApi.assertFoundCurrentData(resolvedPet, CurrentPetApi.PetDataAssertionSource.AUTOPET)
            PetXpEstimateApi.recordAutopetSwap(resolvedPet.uuid, hoverInfo.autopetTriggerOrNull())
            jsonNeedsSave = true
        }
    }

    private fun List<String>.autopetTriggerOrNull(): String? =
        map { it.removeColor().removeResets().trim() }
            .zipWithNext()
            .firstOrNull { (line, trigger) -> line == "When:" && trigger.isNotBlank() }
            ?.second

    private fun resolveAutopetPetDataOrNull(
        name: String,
        rarity: LorenzRarity,
        heldItemName: String?,
        heldItemKnown: Boolean,
        skinTag: String?,
        level: Int,
    ): PetData? {
        val candidates = petStorage?.pets
            ?.asSequence()
            ?.filter { it.uuid != null }
            ?.filter { it.cleanName == name }
            ?.filter { it.rarity == rarity }
            ?.filter { it.skinTag == skinTag }
            ?.toList()
            ?.takeIfNotEmpty()
            ?: return null

        val levelCandidates = candidates.filter { it.level == level }.takeIfNotEmpty()
            ?: candidates.filter { it.level <= level }

        val heldItemCandidates = when {
            !heldItemKnown -> levelCandidates
            heldItemName == null -> levelCandidates.filter { it.heldItemInternalName == null }
            else -> levelCandidates.filter { it.heldItemInternalName?.matchesItemName(heldItemName) == true }
        }
        return heldItemCandidates.singleOrNull()
            ?: levelCandidates.singleOrNull()
            ?: candidates.singleOrNull()
    }

    private fun Component.hoverTextLines(): List<String> = buildList {
        addHoverTextLines(this@hoverTextLines)
    }

    private fun MutableList<String>.addHoverTextLines(component: Component) {
        component.hover?.formattedTextCompat()?.split("\n")?.let(::addAll)
        component.siblings.forEach { addHoverTextLines(it) }
    }

    private fun NeuInternalName.matchesItemName(itemName: String): Boolean = when {
        itemName.contains('§') -> repoItemName == itemName
        else ->
            itemNameWithoutColor == itemName ||
                asString().replace("_", " ").equals(itemName, ignoreCase = true)
    }

    @HandleEvent(onlyOnSkyblock = true, priority = HandleEvent.HIGHEST)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        val inventoryName = InventoryUtils.openInventoryName()
        if (!isMainPetMenuName(inventoryName)) return
        if (!event.slotId.isPetStackLocation()) return
        val clickedItem = event.slot?.item.orNull() ?: event.item.orNull() ?: return
        val clickedPetData = clickedItem.toClickedPetDataOrNull() ?: return
        val clickedPetUuid = clickedPetData.uuid
        val currentPetUuid = ProfileStorageData.profileSpecific?.currentPetUuid
        when (event.clickedButton) {
            1 -> { // Right click - remove pet from menu
                clickedPetUuid ?: return
                petStorage?.pets?.removeIf { it.uuid == clickedPetUuid }
                if (currentPetUuid == clickedPetUuid) {
                    CurrentPetApi.clearCurrentPet()
                }
            }

            0 -> { // Left click - if not a shift click, summon/un-summon pet
                if (KeyboardManager.isShiftKeyDown()) return
                lastExactPetMenuClick = SimpleTimeMark.now()
                if (clickedItem.isCurrentPetStack() || currentPetUuid == clickedPetUuid) {
                    CurrentPetApi.clearCurrentPet()
                } else {
                    if (clickedPetUuid != null) petStorage?.pets?.addOrReplace(clickedPetData)
                    CurrentPetApi.assertFoundCurrentData(clickedPetData, CurrentPetApi.PetDataAssertionSource.MENU)
                }
            }

            else -> return
        }
        jsonNeedsSave = true
    }

    @HandleEvent(onlyOnSkyblock = true, priority = HandleEvent.HIGHEST)
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        val exactPetMenuUuids = event.readPetsMenuItems()
        event.readEquipmentPetData()
        event.readSelectedPetData(exactPetMenuUuids)
        event.readExpSharePets()
    }

    private fun InventoryFullyOpenedEvent.readPetsMenuItems(): Set<UUID> {
        if (!isMainPetMenuName(inventoryName)) return emptySet()
        val petStorage = petStorage ?: return emptySet()
        val currentPetUuid = ProfileStorageData.profileSpecific?.currentPetUuid
        val exactPetUuids = mutableSetOf<UUID>()

        inventoryItems.filter { (slotNumber, stack) ->
            slotNumber.isPetStackLocation() && stack.getInternalNameOrNull() != null
        }.mapNotNull { (_, item) ->
            item.toExactPetDataOrNull()
        }.forEach {
            val previousExp = petStorage.pets.firstOrNull { petData -> petData.uuid == it.uuid }?.exp
            it.uuid?.let(exactPetUuids::add)
            if (it.uuid == currentPetUuid || PetXpEstimateApi.shouldRecordPetMenuRead(it.uuid)) {
                PetXpEstimateApi.recordPetDataRead(
                    "pet-menu-grid",
                    it,
                    exact = true,
                    previousExp = previousExp,
                    appliedExp = it.exp,
                )
            }
            petStorage.pets.addOrReplace(it)
        }
        jsonNeedsSave = true
        return exactPetUuids
    }

    private fun InventoryFullyOpenedEvent.readEquipmentPetData() {
        if (inventoryName != "Your Equipment and Stats") return
        val petStorage = petStorage ?: return
        val currentPetItem = inventoryItems[EQUIP_MENU_CURRENT_PET_SLOT]?.takeIf {
            it.hoverName.string != "Empty Pet Slot"
        } ?: return
        val data = currentPetItem.toExactPetDataOrNull() ?: return

        petStorage.pets.addOrReplace(data)

        CurrentPetApi.assertFoundCurrentData(data, CurrentPetApi.PetDataAssertionSource.MENU)
        jsonNeedsSave = true
    }

    private fun InventoryFullyOpenedEvent.readSelectedPetData(exactPetMenuUuids: Set<UUID>) {
        val isPetMenu = isMainPetMenuName(inventoryName)
        if (isPetMenu && lastExactPetMenuClick.passedSince() < 5.seconds) return

        val petItemSlot = when {
            isPetMenu -> PET_MENU_CURRENT_PET_SLOT
            inventoryName == "SkyBlock Menu" -> SB_MENU_CURRENT_PET_SLOT
            else -> return
        }
        val currentPetItem = inventoryItems[petItemSlot] ?: return
        if (currentPetItem.readExactSelectedPetData()) return
        val currentPetItemLore = currentPetItem.getLore().takeIfNotEmpty() ?: return

        petMenuSelectedPetNamePattern.firstMatcher(currentPetItemLore) {
            val petName = groupOrNull("pet") ?: return@firstMatcher false
            val rarity = getRarityOrNull() ?: return@firstMatcher false
            val petInternalName = PetUtils.petWithRarityToInternalName(petName, rarity)
            val petSkin = getPetSkinOrNull(petInternalName)
            val petSkinTag = groupOrNull("skin")?.replace(" ", "")

            val level = petMenuSelectedPetProgressPattern.firstMatcher(currentPetItemLore) {
                when (groupOrNull("next")) {
                    null -> PetUtils.getMaxLevel(petInternalName)
                    else -> (group("next").formatInt() - 1)
                }
            } ?: return@firstMatcher false

            val petExp = petMenuSelectedPetXpPattern.firstMatcher(currentPetItemLore) {
                val current = group("current")
                val currentValue = current.formatDouble()
                val exact = current.isExactPetExpText()
                when (groupOrNull("next")) {
                    null -> PetExpRead(currentValue, exact)
                    else -> {
                        val currentLevelXp = PetUtils.levelToXp(level, petInternalName) ?: 0.0
                        PetExpRead(currentLevelXp + currentValue, exact)
                    }
                }
            }

            val resolvedPet = resolvePetDataOrNull(
                name = petName,
                skinTag = petSkinTag,
                skinTagKnown = true,
                rarity = rarity,
                level = level,
                exp = petExp?.value,
            )
            val matchingCurrentPet = CurrentPetApi.currentPet?.takeIf { currentPet ->
                currentPet.matchesSelectedPet(petName, rarity, level, petSkinTag)
            }
            val currentPetData = resolvedPet ?: matchingCurrentPet ?: PetData(
                petInternalName = petInternalName,
                skinInternalName = petSkin?.internalName,
                exp = petExp?.value ?: PetUtils.levelToXp(level, petInternalName) ?: 0.0,
            )

            val hasExactPetMenuRead = currentPetData.uuid?.let { it in exactPetMenuUuids } == true
            val previousExp = currentPetData.exp
            val exactPetExp = petExp.exactValue.takeUnless { hasExactPetMenuRead }
                ?.let { currentPetData.reconcileDisplayedExp(it) }
            currentPetData.applyKnownData(exp = exactPetExp, skinInternalName = petSkin?.internalName)

            PetXpEstimateApi.recordPetDataRead(
                "selected-pet-lore",
                currentPetData,
                exact = exactPetExp != null,
                previousExp = previousExp,
                appliedExp = exactPetExp,
            )
            CurrentPetApi.assertFoundCurrentData(currentPetData, CurrentPetApi.PetDataAssertionSource.MENU)
            jsonNeedsSave = true
        }
    }

    private fun SafeItemStack.readExactSelectedPetData(): Boolean {
        val currentPetData = getPetInfo()?.let(::PetData) ?: return false
        val previousExp = currentPetData.uuid?.let { uuid ->
            petStorage?.pets?.firstOrNull { it.uuid == uuid }?.exp
        }
        if (currentPetData.uuid != null) petStorage?.pets?.addOrReplace(currentPetData)
        PetXpEstimateApi.recordPetDataRead(
            "selected-pet-nbt",
            currentPetData,
            exact = true,
            previousExp = previousExp,
            appliedExp = currentPetData.exp,
        )
        CurrentPetApi.assertFoundCurrentData(currentPetData, CurrentPetApi.PetDataAssertionSource.MENU)
        jsonNeedsSave = true
        return true
    }

    private fun PetData.matchesSelectedPet(
        petName: String,
        rarity: LorenzRarity,
        level: Int,
        skinTag: String?,
    ) = cleanName == petName &&
        this.rarity == rarity &&
        this.level <= level &&
        this.skinTag == skinTag

    private fun InventoryFullyOpenedEvent.readExpSharePets() {
        if (inventoryName != "Exp Sharing") return
        val petStorage = petStorage ?: return
        petStorage.expSharePets.clear()
        petStorage.expSharePets.addAll(
            EXP_SHARE_SLOTS.map { expShareSlot ->
                val slotItem = inventoryItems[expShareSlot]?.takeIf {
                    it.hoverName.string != "No pet in slot"
                } ?: return@map null
                slotItem.getPetInfo()?.ownedUuid
            },
        )
    }

    fun isAutopetMessage(message: String): Boolean = autoPetMessagePattern.matches(message)

    fun markDirty() {
        jsonNeedsSave = true
    }

    fun getExpSharePets(): List<PetData> = petStorage?.let { petStorage ->
        petStorage.expSharePets.mapNotNull { uuid ->
            uuid?.let { petUuid -> petStorage.pets.firstOrNull { it.uuid == petUuid } }
        }
    } ?: emptyList()

    fun resolvePetDataOrNull(
        name: String,
        rarity: LorenzRarity? = null,
        heldItem: NeuInternalName? = null,
        skinTag: String? = null,
        skinTagKnown: Boolean = false,
        level: Int? = null,
        exp: Double? = null,
        expErrorFactor: Double = 0.01,
    ): PetData? = petStorage?.pets
        ?.filter { it.uuid != null }
        ?.filter { it.cleanName == name }
        ?.filter { rarity == null || it.rarity == rarity }
        ?.filter { heldItem == null || it.heldItemInternalName == heldItem }
        ?.filter { if (skinTagKnown || skinTag != null) it.skinTag == skinTag else true }
        ?.filter { level == null || it.level == level }
        ?.singleOrNull { it.hasMatchingExp(exp, expErrorFactor) }

    private fun PetData.hasMatchingExp(exp: Double?, expErrorFactor: Double): Boolean = exp?.let { readExp ->
        val allowedError = (readExp * expErrorFactor).coerceAtLeast(1.0)
        abs((this.exp ?: 0.0) - readExp) <= allowedError
    } ?: true

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shresetpetstorage") {
            description = "Removes all pets from SkyHanni's storage"
            category = CommandCategory.USERS_RESET
            simpleCallback {
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
    fun onDebugDataCollect(event: DebugDataCollectEvent) {
        fun PetData.formatForDebug(currentPetUuid: UUID?) = buildString {
            append(if (uuid == currentPetUuid) "* " else "- ")
            append(fauxInternalName.asString())
            append(" lvl=$level")
            append(" rarity=$rarity")
            append(" uuid=$uuid")
            append(" exp=${exp?.toLong() ?: 0L}")
            append(" held=${heldItemInternalName?.asString() ?: "<none>"}")
            append(" skin=${skinInternalName?.asString() ?: "<none>"}")
            append(" skinTag=${skinTag ?: "<none>"}")
            append(" variant=${skinVariantIndex ?: "<none>"}")
        }
        event.title("Pet Storage API")
        event.addIrrelevant {
            val petStorage = petStorage ?: run {
                add("petStorage is null")
                return@addIrrelevant
            }
            val currentPetUuid = ProfileStorageData.profileSpecific?.currentPetUuid
            add("currentPetUuid: $currentPetUuid")
            add("recentExactPetMenuClick: ${lastExactPetMenuClick.passedSince() < 5.seconds}")
            add("petCount: ${petStorage.pets.size}")
            LorenzRarity.entries.reversed().forEach { rarity ->
                val pets = petStorage.pets.filter { it.rarity == rarity }.takeIfNotEmpty() ?: return@forEach
                add("pets (${rarity.name}):")
                pets.forEach { add(it.formatForDebug(currentPetUuid)) }
                add("")
            }
            add("expSharePets: " + petStorage.expSharePets.joinToString(", ") { it?.toString() ?: "<empty>" })
        }
    }
}
