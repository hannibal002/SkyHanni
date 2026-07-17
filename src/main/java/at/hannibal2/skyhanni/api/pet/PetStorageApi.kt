package at.hannibal2.skyhanni.api.pet

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.PetData
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuItemJson
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ComponentMatcher
import at.hannibal2.skyhanni.utils.ComponentMatcherUtils.matchStyledMatcher
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
import at.hannibal2.skyhanni.utils.ItemUtils.matchesItemName
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
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getPetInfo
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.StringUtils.removeResets
import at.hannibal2.skyhanni.utils.chat.TextHelper
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.indexOfFirstOrNull
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.orNull
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
import at.hannibal2.skyhanni.utils.compat.hover
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import java.util.UUID
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object PetStorageApi {

    private val config get() = SkyHanniMod.feature.misc.pets
    private val petStorage get() = ProfileStorageData.petProfiles
    private const val PET_MENU_CURRENT_PET_SLOT = 4
    private const val SB_MENU_CURRENT_PET_SLOT = 30
    private const val EQUIP_MENU_CURRENT_PET_SLOT = 47
    private val WIDGET_LOAD_GRACE = 3.seconds
    private var jsonNeedsSave: Boolean = false
    private var lastSaved: SimpleTimeMark = SimpleTimeMark.farPast()
    private var lastExactPetMenuClick: SimpleTimeMark = SimpleTimeMark.farPast()
    private var petWidgetState: PetWidgetState = PetWidgetState.NOT_READY

    private fun isPetWidgetUnavailable(): Boolean = IslandType.CATACOMBS.isInIsland()

    fun getPetWidgetWarning(showMissingOverflowXpWarning: Boolean): String? = when {
        isPetWidgetUnavailable() -> null
        !TabWidget.PET.isActive && SkyBlockUtils.lastWorldSwitch.passedSince() >= WIDGET_LOAD_GRACE -> {
            "§cInaccurate! Enable /tab → Pet Widget"
        }
        showMissingOverflowXpWarning && petWidgetState == PetWidgetState.MAXED_WITHOUT_OVERFLOW_XP -> {
            "§cInaccurate! Enable /tab → Pet Widget → Show Overflow Pet XP"
        }
        else -> null
    }

    internal val debugPetWidgetState: String get() = petWidgetState.name

    internal fun hasRecentExactPetMenuClick() = lastExactPetMenuClick.passedSince() < 5.seconds

    private enum class PetWidgetState {
        NOT_READY,
        READY,
        MAXED_WITHOUT_OVERFLOW_XP,
    }

    private fun Int.isPetStackLocation() = this in 10..43 &&
        this % 9 != 0 && (this + 1) % 9 != 0

    private fun Matcher.getPetSkinOrNull(petInternalName: NeuInternalName): NeuItemJson? {
        val skin = groupOrNull("skin") ?: groupOrNull("altskin") ?: return null
        return PetUtils.findPetSkinOrNull(petInternalName, skin)
    }

    private fun ComponentMatcher.getPetSkinOrNull(petInternalName: NeuInternalName): NeuItemJson? {
        val skin = getPetSkinColorTagOrNull() ?: return null
        return PetUtils.findPetSkinOrNull(petInternalName, skin)
    }

    private fun ComponentMatcher.getPetSkinTagOrNull(): String? =
        getPetSkinColorTagOrNull()?.replace(" ", "")

    private fun ComponentMatcher.getPetSkinColorTagOrNull(): String? =
        component("skin")?.formattedTextForItemLookup()
            ?: component("altskin")?.formattedTextForItemLookup()

    private fun ComponentMatcher.petNameAndRarityOrNull(): Pair<String, LorenzRarity>? {
        val petSpan = groupOrThrow("pet")
        val petName = petSpan.getText().trim()
        val rarity = petSpan.sampleStyleAtStart().toLorenzRarityOrNull() ?: return null
        return petName to rarity
    }

    private fun Style.toLorenzRarityOrNull(): LorenzRarity? = when (color?.name) {
        "dark_red" -> LorenzRarity.ULTIMATE
        "red" -> LorenzRarity.SPECIAL
        "aqua" -> LorenzRarity.DIVINE
        "light_purple" -> LorenzRarity.MYTHIC
        "gold" -> LorenzRarity.LEGENDARY
        "dark_purple" -> LorenzRarity.EPIC
        "blue" -> LorenzRarity.RARE
        "green" -> LorenzRarity.UNCOMMON
        "white" -> LorenzRarity.COMMON
        else -> null
    }

    private inline fun <T> Iterable<Component>.firstStyledMatcher(
        pattern: Pattern,
        consumer: ComponentMatcher.() -> T,
    ): T? {
        for (component in this) {
            pattern.matchStyledMatcher(component, consumer)?.let { return it }
        }
        return null
    }

    private fun Collection<Component>.toColorlessText(): List<String> =
        map { it.string.removeColor() }

    private fun Component.formattedTextForItemLookup(): String =
        formattedTextCompatLessResets().removeResets().trim()

    private fun Component.hoverTextComponents(): List<Component> = buildList {
        addHoverTextComponents(this@hoverTextComponents)
    }

    private fun MutableList<Component>.addHoverTextComponents(component: Component) {
        component.hover?.let { hover ->
            addAll(TextHelper.split(hover, "\n") ?: listOf(hover))
        }
        component.siblings.forEach { addHoverTextComponents(it) }
    }

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

    // Keeps both the numeric XP value and whether the menu text was exact or abbreviated.
    private data class PetExpRead(
        val value: Double,
        val exact: Boolean,
    )

    private val PetExpRead?.exactValue get() = this?.takeIf { it.exact }?.value

    private fun PetData.reconcileDisplayedExp(readExp: Double): Double {
        val storedExp = exp ?: return readExp
        if (readExp % 1.0 != 0.0) return readExp
        return storedExp.coerceIn(readExp, readExp + 0.999)
    }

    private fun String.isExactPetExpText() =
        !contains('k', ignoreCase = true) && !contains('m', ignoreCase = true)

    private fun PetData.hasMatchingSkinTag(skinTag: String?, skinTagKnown: Boolean): Boolean = when {
        skinTag == null -> !skinTagKnown || this.skinTag == null
        skinTagKnown -> this.skinTag == skinTag
        else -> this.skinTag?.removeColor() == skinTag.removeColor()
    }

    fun isMainPetMenuName(inventoryName: String?): Boolean =
        PetStoragePatterns.mainPetMenuNamePattern.matches(inventoryName)

    private fun SafeItemStack.toVisiblePetDataOrNull(): PetData? =
        PetStoragePatterns.petMenuPetStackNamePattern.matchStyledMatcher(hoverName) {
            val petInfo = getPetInfo()
            val level = matcher.group("level").formatInt()
            val petSpan = groupOrThrow("pet")
            val petName = petSpan.getText().trim()
            val itemInternalName = getInternalNameOrNull()?.takeIf { PetUtils.getPetRarity(it) != null }
            val petInternalName = itemInternalName ?: run {
                val rarity = petSpan.sampleStyleAtStart().toLorenzRarityOrNull() ?: return@matchStyledMatcher null
                PetUtils.petWithRarityToInternalName(petName, rarity)
            }
            val petSkin = getPetSkinOrNull(petInternalName)
            val lore = getLoreComponent().toColorlessText()
            val petExp = PetStoragePatterns.petMenuSelectedPetXpPattern.firstMatcher(lore) {
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
            val petData = petInfo?.let(::PetData) ?: PetData(petInternalName = petInternalName)
            petData.applyKnownData(
                skinInternalName = petInfo?.properSkinItem ?: petSkin?.internalName,
                heldItemInternalName = petInfo?.heldItem,
                exp = petInfoExp ?: petExp ?: PetUtils.levelToXp(level, petData.fauxInternalName) ?: 0.0,
            )
            petData
        }

    private fun SafeItemStack.toClickedPetDataOrNull(): PetData? =
        getPetInfo()?.let(::PetData) ?: toVisiblePetDataOrNull()

    private fun SafeItemStack.toExactPetDataOrNull(): PetData? =
        getPetInfo()?.let(::PetData)

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
        if (event.isClear()) {
            if (SkyBlockUtils.lastWorldSwitch.passedSince() < WIDGET_LOAD_GRACE) return
            petWidgetState = PetWidgetState.NOT_READY
            return
        }
        var foundUsableWidgetPet = false
        for (component in event.lines) {
            val lineWasUsable = readPetWidgetLine(component, event.lines) ?: continue
            foundUsableWidgetPet = foundUsableWidgetPet || lineWasUsable
        }
        if (!foundUsableWidgetPet) {
            petWidgetState = PetWidgetState.NOT_READY
        }
    }

    private fun readPetWidgetLine(component: Component, lines: List<Component>): Boolean? =
        PetStoragePatterns.petTabWidgetNamePattern.matchMatcher(component.string) {
            readPetWidgetData(component, lines)
        }

    private fun Matcher.readPetWidgetData(component: Component, lines: List<Component>): Boolean {
        val petName = groupOrNull("pet") ?: return false
        val level = group("level").toInt()
        val rarity = LorenzRarity.getByComponent(component, group("pet")) ?: return false
        val petInternalName = PetUtils.petWithRarityToInternalName(petName, rarity)
        val petSkin = getPetSkinOrNull(petInternalName)
        val petSkinTag = (groupOrNull("skin") ?: groupOrNull("altskin"))?.replace(" ", "")
        val petSkinTagKnown = petSkinTag == null
        val petHeldItem = readWidgetPetItem(lines)

        var maxedWithoutOverflowXp = false
        val petExp = readWidgetPetExp(lines, level, petInternalName) {
            maxedWithoutOverflowXp = true
        }
        val currentPetData = findWidgetPetData(
            petName,
            rarity,
            level,
            petSkinTag,
            petSkinTagKnown,
            petHeldItem,
            petExp,
            petInternalName,
            petSkin,
        )

        val previousExp = currentPetData.exp
        val exactPetExp = petExp.exactValue?.let { currentPetData.reconcileDisplayedExp(it) }
        currentPetData.applyKnownData(
            exp = exactPetExp,
            skinInternalName = petSkin?.internalName,
            heldItemInternalName = petHeldItem,
        )

        PetXpEstimateApi.recordPetDataRead(
            currentPetData,
            exact = exactPetExp != null,
            previousExp = previousExp,
            appliedExp = exactPetExp,
        )
        CurrentPetApi.assertFoundCurrentData(currentPetData, CurrentPetApi.PetDataAssertionSource.TAB)
        updatePetWidgetState(maxedWithoutOverflowXp, exactPetExp)
        jsonNeedsSave = true
        return maxedWithoutOverflowXp || exactPetExp != null
    }

    private fun readWidgetPetItem(lines: List<Component>): NeuInternalName? = lines.firstNotNullOfOrNull { line ->
        val trimmedLine = line.formattedTextCompat().trim().removeResets().takeIf { it.isNotBlank() }
            ?: return@firstNotNullOfOrNull null
        PetUtils.resolvePetItemOrNull(trimmedLine)
    }

    private fun readWidgetPetExp(
        lines: List<Component>,
        level: Int,
        petInternalName: NeuInternalName,
        onMaxLevel: () -> Unit,
    ): PetExpRead? = PetStoragePatterns.petTabWidgetXpPattern.firstMatcher(lines.map { it.string }) {
        // We don't know XP if it's just "MAX LEVEL".
        if (groupOrNull("max") != null) {
            onMaxLevel()
            return@firstMatcher null
        }
        val currentLevelXp = PetUtils.levelToXp(level, petInternalName) ?: return@firstMatcher null
        val current = groupOrNull("current") ?: "0"
        val readXpGroup = current.formatDoubleOrNull() ?: 0.0
        PetExpRead(currentLevelXp + readXpGroup, current.isExactPetExpText())
    }

    private fun findWidgetPetData(
        petName: String,
        rarity: LorenzRarity,
        level: Int,
        petSkinTag: String?,
        petSkinTagKnown: Boolean,
        petHeldItem: NeuInternalName?,
        petExp: PetExpRead?,
        petInternalName: NeuInternalName,
        petSkin: NeuItemJson?,
    ): PetData {
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
                currentPet.hasMatchingSkinTag(petSkinTag, petSkinTagKnown)
        }
        return resolvedPet ?: matchingCurrentPet ?: PetData(
            petInternalName = petInternalName,
            skinInternalName = petSkin?.internalName,
            heldItemInternalName = petHeldItem,
            exp = petExp?.value ?: PetUtils.levelToXp(level, petInternalName) ?: 0.0,
        )
    }

    private fun updatePetWidgetState(maxedWithoutOverflowXp: Boolean, exactPetExp: Double?) {
        when {
            maxedWithoutOverflowXp -> petWidgetState = PetWidgetState.MAXED_WITHOUT_OVERFLOW_XP
            exactPetExp != null -> petWidgetState = PetWidgetState.READY
        }
    }

    @HandleEvent(priority = HandleEvent.HIGHEST)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        PetStoragePatterns.petItemHeldMessagePattern.matchStyledMatcher(event.chatComponent) {
            val petHeldItemName = componentOrThrow("item").formattedTextForItemLookup()
            val petHeldItem = resolveAppliedPetItemOrNull(petHeldItemName) ?: return
            updateCurrentPetHeldItem(petHeldItem)
        }

        PetStoragePatterns.petItemRemovedMessagePattern.matchMatcher(event.cleanMessage) {
            updateCurrentPetHeldItem(null)
        }

        PetStoragePatterns.autoPetMessagePattern.matchMatcher(event.message.removeResets()) {
            if (config.hideAutopet) event.blockedReason = "autopet"

            val rarity = LorenzRarity.getByColorCode(group("rarity")[0]) ?: return@matchMatcher
            val petName = group("pet").trim()
            val level = group("level").toInt()
            val petInternalName = PetUtils.petWithRarityToInternalName(petName, rarity)
            val petSkinTag = groupOrNull("skin")?.replace(" ", "")
            val petSkin = petSkinTag?.let { PetUtils.findPetSkinOrNull(petInternalName, it) }

            val hoverInfoComponents = event.chatComponent.hoverTextComponents()
            val hoverInfo = hoverInfoComponents.toColorlessText()
            val petHeldItemName = hoverInfoComponents.firstStyledMatcher(PetStoragePatterns.autoPetHoverHeldItemPattern) {
                componentOrThrow("item").formattedTextForItemLookup()
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

            val previousPet = CurrentPetApi.currentPet
            CurrentPetApi.assertFoundCurrentData(resolvedPet, CurrentPetApi.PetDataAssertionSource.AUTOPET)
            PetXpEstimateApi.recordAutopetSwap(resolvedPet, previousPet, hoverInfo.autopetTriggerOrNull())
            jsonNeedsSave = true
        }
    }

    private fun resolveAppliedPetItemOrNull(itemName: String): NeuInternalName? {
        val heldItemName = InventoryUtils.getItemInHand()?.displayName?.formattedTextCompat()
            ?.removeResets()
            ?.trim()
        if (heldItemName?.removeColor() == itemName.removeColor()) {
            PetUtils.resolvePetItemOrNull(heldItemName)?.let { return it }
        }
        return PetUtils.resolvePetItemOrNull(itemName)
    }

    private fun updateCurrentPetHeldItem(heldItem: NeuInternalName?) {
        val currentPet = CurrentPetApi.currentPet ?: return
        if (currentPet.heldItemInternalName == heldItem) return
        currentPet.heldItemInternalName = heldItem
        if (currentPet.uuid != null) petStorage?.pets?.addOrReplace(currentPet)
        CurrentPetApi.assertFoundCurrentData(currentPet, CurrentPetApi.PetDataAssertionSource.CHAT)
        markDirty()
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
            ?.filter {
                it.uuid != null &&
                    it.cleanName == name &&
                    it.rarity == rarity &&
                    it.skinTag == skinTag
            }
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
        event.readSelectedPetData()
        event.readEquipmentPetData()
        PetStorageExpShare.readInventory(event.inventoryName, event.inventoryItems)
    }

    private fun InventoryFullyOpenedEvent.readExactPetMenuPets(): Set<UUID> {
        if (!isMainPetMenuName(inventoryName)) return emptySet()
        val petStorage = petStorage ?: return emptySet()
        val currentPetUuid = ProfileStorageData.profileSpecific?.currentPetUuid
        val exactPetUuids = mutableSetOf<UUID>()

        inventoryItems.filter { (slotNumber, stack) ->
            slotNumber.isPetStackLocation() && stack.getInternalNameOrNull() != null
        }.mapNotNull { (_, item) ->
            item.toExactPetDataOrNull()
        }.forEach { petData ->
            val previousExp = petStorage.pets.firstOrNull { storedPet -> storedPet.uuid == petData.uuid }?.exp
            petData.uuid?.let(exactPetUuids::add)
            if (petData.uuid == currentPetUuid || PetXpEstimateApi.shouldRecordPetMenuRead(petData.uuid)) {
                PetXpEstimateApi.recordPetDataRead(
                    petData,
                    exact = true,
                    previousExp = previousExp,
                    appliedExp = petData.exp,
                )
            }
            petStorage.pets.addOrReplace(petData)
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

    private fun InventoryFullyOpenedEvent.readSelectedPetData() {
        val isPetMenu = isMainPetMenuName(inventoryName)
        val exactMenuPetUuids = readExactPetMenuPets()
        if (isPetMenu && lastExactPetMenuClick.passedSince() < 5.seconds) return

        val petItemSlot = when {
            isPetMenu -> PET_MENU_CURRENT_PET_SLOT
            inventoryName == "SkyBlock Menu" -> SB_MENU_CURRENT_PET_SLOT
            else -> return
        }
        val currentPetItem = inventoryItems[petItemSlot] ?: return
        if (currentPetItem.readExactSelectedPetData()) return
        val currentPetItemLore = currentPetItem.getLoreComponent().takeIfNotEmpty() ?: return

        currentPetItemLore.firstStyledMatcher(PetStoragePatterns.petMenuSelectedPetNamePattern) {
            readSelectedPetDataFromLore(currentPetItemLore, exactMenuPetUuids)
        }
    }

    private fun ComponentMatcher.readSelectedPetDataFromLore(
        currentPetItemLore: List<Component>,
        exactMenuPetUuids: Set<UUID>,
    ): Boolean {
        val (petName, rarity) = petNameAndRarityOrNull() ?: return false
        val petInternalName = PetUtils.petWithRarityToInternalName(petName, rarity)
        val petSkin = getPetSkinOrNull(petInternalName)
        val petSkinTag = getPetSkinTagOrNull()
        val colorlessLore = currentPetItemLore.toColorlessText()
        val level = readSelectedPetLevel(colorlessLore, petInternalName) ?: return false
        val petExp = readSelectedPetExp(colorlessLore, level, petInternalName)
        val currentPetData = findSelectedPetData(petName, rarity, level, petSkinTag, petInternalName, petSkin, petExp)

        val hasExactPetMenuRead = currentPetData.uuid?.let { it in exactMenuPetUuids } == true
        val previousExp = currentPetData.exp
        val exactPetExp = petExp.exactValue.takeUnless { hasExactPetMenuRead }
            ?.let { currentPetData.reconcileDisplayedExp(it) }
        currentPetData.applyKnownData(exp = exactPetExp, skinInternalName = petSkin?.internalName)

        PetXpEstimateApi.recordPetDataRead(
            currentPetData,
            exact = exactPetExp != null,
            previousExp = previousExp,
            appliedExp = exactPetExp,
        )
        CurrentPetApi.assertFoundCurrentData(currentPetData, CurrentPetApi.PetDataAssertionSource.MENU)
        jsonNeedsSave = true
        return true
    }

    private fun readSelectedPetLevel(currentPetItemLore: List<String>, petInternalName: NeuInternalName): Int? =
        PetStoragePatterns.petMenuSelectedPetProgressPattern.firstMatcher(currentPetItemLore) {
            when (groupOrNull("next")) {
                null -> PetUtils.getMaxLevel(petInternalName)
                else -> (group("next").formatInt() - 1)
            }
        }

    private fun readSelectedPetExp(
        currentPetItemLore: List<String>,
        level: Int,
        petInternalName: NeuInternalName,
    ): PetExpRead? = PetStoragePatterns.petMenuSelectedPetXpPattern.firstMatcher(currentPetItemLore) {
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

    private fun findSelectedPetData(
        petName: String,
        rarity: LorenzRarity,
        level: Int,
        petSkinTag: String?,
        petInternalName: NeuInternalName,
        petSkin: NeuItemJson?,
        petExp: PetExpRead?,
    ): PetData {
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
        return resolvedPet ?: matchingCurrentPet ?: PetData(
            petInternalName = petInternalName,
            skinInternalName = petSkin?.internalName,
            exp = petExp?.value ?: PetUtils.levelToXp(level, petInternalName) ?: 0.0,
        )
    }

    private fun SafeItemStack.readExactSelectedPetData(): Boolean {
        val currentPetData = getPetInfo()?.let(::PetData) ?: return false
        val previousExp = currentPetData.uuid?.let { uuid ->
            petStorage?.pets?.firstOrNull { it.uuid == uuid }?.exp
        }
        if (currentPetData.uuid != null) petStorage?.pets?.addOrReplace(currentPetData)
        PetXpEstimateApi.recordPetDataRead(
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

    fun isAutopetMessage(message: String): Boolean =
        PetStoragePatterns.autoPetMessageColorlessPattern.matches(message.removeColor().removeResets())

    fun markDirty() {
        jsonNeedsSave = true
    }

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
        ?.filter {
            it.uuid != null &&
                it.cleanName == name &&
                (rarity == null || it.rarity == rarity) &&
                (heldItem == null || it.heldItemInternalName == heldItem) &&
                it.hasMatchingSkinTag(skinTag, skinTagKnown) &&
                (level == null || it.level == level)
        }
        ?.singleOrNull { it.hasMatchingExp(exp, expErrorFactor) }

    private fun PetData.hasMatchingExp(exp: Double?, expErrorFactor: Double): Boolean = exp?.let { readExp ->
        val allowedError = (readExp * expErrorFactor).coerceAtLeast(1.0)
        abs((this.exp ?: 0.0) - readExp) <= allowedError
    } ?: true
}
