package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.jsonobjects.repo.HideNotClickableItemsJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.SalvageFilter
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.item.ItemNotClickableEvent
import at.hannibal2.skyhanni.features.garden.composter.ComposterOverlay
import at.hannibal2.skyhanni.features.garden.visitor.VisitorApi
import at.hannibal2.skyhanni.features.inventory.bazaar.BazaarApi
import at.hannibal2.skyhanni.features.mining.fossilexcavator.FossilExcavatorApi
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.features.rift.RiftApi.motesNpcPrice
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getNpcPriceOrNull
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.isAnySoulbound
import at.hannibal2.skyhanni.utils.ItemUtils.isSoulbound
import at.hannibal2.skyhanni.utils.ItemUtils.isVanilla
import at.hannibal2.skyhanni.utils.MultiFilter
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.anyMatches
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getItemId
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.hasAttributes
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.isMuseumDonated
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.isRiftExportable
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.isRiftTransferable
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.equalsOneOf
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object HideNotClickableItemsFeature {

    private val config get() = SkyHanniMod.feature.inventory.hideNotClickable

    private val hideNpcSellFilter = MultiFilter()
    private val hideInStorageFilter = MultiFilter()
    private val itemsToSalvage = mutableListOf<String>()
    private val hidePlayerTradeFilter = MultiFilter()
    private val notAuctionableFilter = MultiFilter()

    private val netherWart = "NETHER_STALK".toInternalName()

    private val birdFood = setOf(
        "BAG_OF_SEEDS".toInternalName(), // for Bluebirds
        "WRIGGLEWORM".toInternalName(), // for Parakeets
        "YOGI_BERRY".toInternalName(), // for Macaws
    )

    // TODO add more special named fossils (hypixel why)
    private val specialFossils = setOf(
        "HELIX".toInternalName(),
    )

    private val patternGroup = RepoPattern.group("inventory.hidenotclickable")

    private val clickToSellPattern by patternGroup.pattern(
        "clicktosell",
        "§eClick to sell!",
    )

    /**
     * REGEX-TEST: SEEDS
     * REGEX-TEST: CARROT_ITEM
     * REGEX-TEST: POTATO_ITEM
     * REGEX-TEST: PUMPKIN_SEEDS
     * REGEX-TEST: SUGAR_CANE
     * REGEX-TEST: MELON_SEEDS
     * REGEX-TEST: CACTUS
     * REGEX-TEST: INK_SACK-3
     */
    private val seedsPattern by patternGroup.pattern(
        "inventory.hidenotclickable.seeds",
        "SEEDS|CARROT_ITEM|POTATO_ITEM|PUMPKIN_SEEDS|SUGAR_CANE|MELON_SEEDS|CACTUS|INK_SACK-3|DOUBLE_PLANT|MOONFLOWER|WILD_ROSE",
    )

    @HandleEvent
    private fun onRepoReload(event: RepositoryReloadEvent) {
        val hideNotClickable = event.getConstant<HideNotClickableItemsJson>("HideNotClickableItems")
        hideNpcSellFilter.load(hideNotClickable.hideNpcSell)
        hideInStorageFilter.load(hideNotClickable.hideInStorage)
        hidePlayerTradeFilter.load(hideNotClickable.hidePlayerTrade)
        notAuctionableFilter.load(hideNotClickable.notAuctionable)
        updateSalvageList(hideNotClickable.salvage)
    }

    private fun updateSalvageList(data: SalvageFilter) {
        itemsToSalvage.clear()

        itemsToSalvage.addAll(data.items)
        for (armor in data.armor) {
            itemsToSalvage.add("$armor Helmet")
            itemsToSalvage.add("$armor Chestplate")
            itemsToSalvage.add("$armor Leggings")
            itemsToSalvage.add("$armor Boots")
        }
    }

    @Suppress("CyclomaticComplexMethod")
    @HandleEvent
    private fun onItemNotClickable(event: ItemNotClickableEvent) {
        if (!config.enabled) return

        with(event) {
            if (hideNpcSell()) return
            if (hideInStorage()) return
            if (hideSalvage()) return
            if (hidePlayerTrade()) return
            if (hideBazaarOrAH()) return
            if (hideAccessoryBag()) return
            if (hideBasketOfSeeds()) return
            if (hideNetherWartPouch()) return
            if (hideTrickOrTreatBag()) return
            if (hideSackOfSacks()) return
            if (hideFishingBag()) return
            if (hidePotionBag()) return
            if (hidePrivateIslandChest()) return
            if (hideAttributeFusion()) return
            if (hideYourEquipment()) return
            if (hideComposter()) return
            if (hideRiftMotesGrubber()) return
            if (hideRiftTransferChest()) return
            if (hideFossilExcavator()) return
            if (hideResearchCenter()) return
            if (hideBirdFeeder()) return
        }
    }

    private fun ItemNotClickableEvent.hideFossilExcavator(): Boolean {
        if (!FossilExcavatorApi.inExcavatorMenu) return false

        showGreenLine = true

        val internalName = stack.getInternalNameOrNull() ?: return true
        if (internalName == FossilExcavatorApi.scrapItem) {
            return false
        }

        val category = stack.getItemCategoryOrNull() ?: return true
        if (category == ItemCategory.CHISEL) {
            return false
        }

        hideReason = "Not a chisel or scrap!"
        return true
    }

    private fun ItemNotClickableEvent.hideResearchCenter(): Boolean {
        if (chestName != "Research Center") return false

        showGreenLine = true

        val internalName = stack.getInternalNameOrNull() ?: return false

        if (internalName in specialFossils) {
            return false
        }
        if (internalName.endsWith("_FOSSIL")) {
            return false
        }

        hideReason = "Not a fossil!"
        return true
    }

    private fun ItemNotClickableEvent.hideBirdFeeder(): Boolean {
        if (chestName != "Birdfeeder") return false

        showGreenLine = true

        val internalName = stack.getInternalNameOrNull() ?: return false

        if (internalName in birdFood) {
            return false
        }

        hideReason = "Not bird food!"
        return true
    }

    private fun ItemNotClickableEvent.hideRiftTransferChest(): Boolean {
        if (chestName != "Rift Transfer Chest") return false

        showGreenLine = true

        if (stack.isRiftTransferable() || stack.isRiftExportable()) return false

        hideReason = "Not Rift-Transferable or Rift-Exportable!"
        return true
    }

    private fun ItemNotClickableEvent.hideRiftMotesGrubber(): Boolean {
        if (!RiftApi.inRift()) return false
        if (chestName != "Motes Grubber" && !ShiftClickNpcSell.inInventory) return false

        showGreenLine = true

        if (stack.motesNpcPrice() != null) return false

        hideReason = "Not sellable for Motes!"
        return true
    }

    private fun ItemNotClickableEvent.hideComposter(): Boolean {
        if (!ComposterOverlay.isEnabled() || !ComposterOverlay.inInventory) return false

        showGreenLine = true

        val internalName = stack.getInternalName()
        if (internalName == ComposterOverlay.currentOrganicMatterItem) {
            return false
        }
        if (internalName == ComposterOverlay.currentFuelItem) {
            return false
        }

        hideReason = "Only insert the selected items!"
        return true
    }

    private fun ItemNotClickableEvent.hideYourEquipment(): Boolean {
        if (!CurrentEquipmentApi.inventory.isInside()) return false


        if (stack.getItemCategoryOrNull() in ItemCategory.armorAndEquipmentAndMasks) {
            if (stack.getLore().any { it.contains("§l") }) {
                showGreenLine = true
                return false
            }
        }

        if (ItemUtils.isSkyBlockMenuItem(stack)) {
            hideReason = "The SkyBlock Menu cannot be put into your equipment!"
            allowBypass = false
            return true
        }

        hideReason = "This item cannot be put into your equipment!"
        return true
    }

    private fun ItemNotClickableEvent.hideAttributeFusion(): Boolean {
        if (!chestName.startsWith("Attribute Fusion")) return false

        showGreenLine = true

        if (stack.hasAttributes()) return false

        hideReason = "This item has no attributes!"
        return true
    }

    private fun ItemNotClickableEvent.hidePrivateIslandChest(): Boolean {
        if (!InventoryUtils.isInNormalChest()) return false
        if (!IslandType.PRIVATE_ISLAND.isInIsland()) return false
        if (!stack.isSoulbound()) return false

        hideReason = "This item cannot be stored into a chest!"
        return true
    }

    private fun ItemNotClickableEvent.hidePotionBag(): Boolean {
        if (!chestName.startsWith("Potion Bag")) return false

        if (ItemUtils.isSkyBlockMenuItem(stack)) {
            hideReason = "The SkyBlock Menu cannot be put into the potion bag!"
            allowBypass = false
            return true
        }

        showGreenLine = true
        if (stack.cleanName.endsWith(" Potion") || stack.cleanName == "Water Bottle") return false

        hideReason = "This item is not a potion!"
        return true
    }

    private fun ItemNotClickableEvent.hideFishingBag(): Boolean {
        if (!chestName.startsWith("Fishing Bag")) return false

        if (ItemUtils.isSkyBlockMenuItem(stack)) {
            hideReason = "The SkyBlock Menu cannot be put into the fishing bag!"
            allowBypass = false
            return true
        }

        showGreenLine = true
        if (stack.getLore().any { it.removeColor() == "Fishing Bait" }) {
            return false
        }
        hideReason = "This item is not a fishing bait!"
        return true
    }

    private fun ItemNotClickableEvent.hideSackOfSacks(): Boolean {
        if (!chestName.startsWith("Sack of Sacks")) return false
        if (ItemUtils.isSkyBlockMenuItem(stack)) return false

        showGreenLine = true
        if (ItemUtils.isSack(stack)) return false


        hideReason = "This item is not a sack!"
        return true
    }

    private fun ItemNotClickableEvent.hideAccessoryBag(): Boolean {
        if (!chestName.startsWith("Accessory Bag") && !chestName.startsWith("Accessory Bag (")) return false
        if (ItemUtils.isSkyBlockMenuItem(stack)) return false

        showGreenLine = true
        if (stack.getLore().any { it.contains("ACCESSORY") || it.contains("HATCESSORY") }) return false

        hideReason = "This item is not an accessory!"
        return true
    }

    private fun ItemNotClickableEvent.hideBasketOfSeeds(): Boolean {
        if (!chestName.startsWith("Basket of Seeds")) return false

        if (ItemUtils.isSkyBlockMenuItem(stack)) {
            hideReason = "The SkyBlock Menu cannot be put into the basket of seeds!"
            allowBypass = false
            return true
        }

        seedsPattern.matchMatcher(stack.getInternalName().asString()) {
            return false
        }

        hideReason = "This item is not a seed!"
        return true
    }

    private fun ItemNotClickableEvent.hideNetherWartPouch(): Boolean {
        if (!chestName.startsWith("Nether Wart Pouch")) return false

        if (ItemUtils.isSkyBlockMenuItem(stack)) {
            hideReason = "The SkyBlock Menu cannot be put into the nether wart pouch!"
            allowBypass = false
            return true
        }

        if (stack.getInternalName() == netherWart) return false

        hideReason = "This item is not a nether wart!"
        return true
    }

    private fun ItemNotClickableEvent.hideTrickOrTreatBag(): Boolean {
        if (!chestName.startsWith("Trick or Treat Bag")) return false

        if (ItemUtils.isSkyBlockMenuItem(stack)) {
            hideReason = "The SkyBlock Menu cannot be put into the trick or treat bag!"
            allowBypass = false
            return true
        }

        if (stack.cleanName == "Green Candy" || stack.cleanName == "Purple Candy" || stack.cleanName == "Dark Candy") return false

        hideReason = "This item is not a spooky candy!"
        return true
    }

    private fun ItemNotClickableEvent.hidePlayerTrade(): Boolean {
        if (!isTradeMenu(chestName)) return false

        val isUntradableSoulbound = if (HypixelData.noTrade) stack.isSoulbound() else stack.isAnySoulbound()
        if (isUntradableSoulbound) {
            hideReason = "Soulbound items cannot be traded!"
            return true
        }

        if (ItemUtils.isSkyBlockMenuItem(stack)) {
            hideReason = "The SkyBlock Menu cannot be traded!"
            allowBypass = false
            return true
        }

        val name = stack.cleanName

        if (ItemUtils.isSack(stack)) {
            hideReason = "Sacks cannot be traded!"
            return true
        }

        val result = hidePlayerTradeFilter.match(name)

        if (result) hideReason = "This item cannot be traded!"
        return result
    }

    fun isTradeMenu(chestName: String): Boolean = chestName.startsWith("You    ")

    @Suppress("ReturnCount")
    private fun ItemNotClickableEvent.hideNpcSell(): Boolean {
        if (RiftApi.inRift()) return false
        if (!ShiftClickNpcSell.inInventory) return false
        if (VisitorApi.inInventory) return false

        showGreenLine = true

        var name = stack.cleanName
        val size = stack.count
        val amountText = " x$size"
        if (name.endsWith(amountText)) {
            name = name.substring(0, name.length - amountText.length)
        }

        val sellable = npcSellable(stack)
        if (!sellable) {
            hideReason = "This item cannot be sold at the NPC!"
            return true
        }

        if (stack.isMuseumDonated()) {
            hideReason = "This item cannot be sold at the NPC! (Donated to Museum)"
            return true
        }

        if (ItemUtils.isRecombobulated(stack)) {
            hideReason = "This item should not be sold at the NPC! (Recombobulated)"
            return true
        }

        if (!config.protectRarelySoldItems) return false
        if (stack.isVanilla() && !stack.isEnchanted) return false
        if (SkyBlockUtils.noTradeMode && BazaarApi.isBazaarItem(stack)) return false
        if (hideNpcSellFilter.match(name)) return false

        hideReason = "This item should not be sold at the NPC!"
        return true
    }

    fun npcSellable(stack: SafeItemStack): Boolean {
        return clickToSellPattern.anyMatches(stack.getLore()) ||
            (stack.getItemId() != "PET" && (stack.getInternalNameOrNull()?.getNpcPriceOrNull() ?: 0.0) > 0)
    }

    private fun ItemNotClickableEvent.hideInStorage(): Boolean {
        if (!chestName.contains("Ender Chest") && !chestName.contains("Backpack") && chestName != "Storage") return false

        if (ItemUtils.isSkyBlockMenuItem(stack)) {
            hideReason = "The SkyBlock Menu cannot be put into the storage!"
            allowBypass = false
            return true
        }

        val name = stack.cleanName

        val result = hideInStorageFilter.match(name)

        if (result) hideReason = "Bags cannot be put into the storage!"
        return result
    }

    @Suppress("ReturnCount")
    private fun ItemNotClickableEvent.hideSalvage(): Boolean {
        if (!chestName.equalsOneOf("Salvage Item", "Salvage Items")) return false
        showGreenLine = true

        if (ItemUtils.isRecombobulated(stack)) {
            hideReason = "This item should not be salvaged! (Recombobulated)"
            return true
        }
        // TODO replace with rarity check
        for (line in stack.getLore()) {
            if (line.contains("LEGENDARY DUNGEON")) {
                hideReason = "This item should not be salvaged! (Legendary)"
                return true
            }
        }

        if (stack.isMuseumDonated()) {
            hideReason = "This item cannot be salvaged! (Donated to Museum)"
            return true
        }

        if (ItemUtils.isSkyBlockMenuItem(stack)) {
            hideReason = "The SkyBlock Menu cannot be salvaged!"
            allowBypass = false
            return true
        }

        val name = stack.cleanName
        for (item in itemsToSalvage) {
            if (name.endsWith(item)) {
                return false
            }
        }

        hideReason = "This item cannot be salvaged!"
        return true
    }

    private fun ItemNotClickableEvent.hideBazaarOrAH(): Boolean {
        val bazaarInventory = BazaarApi.inBazaarInventory
        val auctionHouseInventory = isAuctionHouse(chestName)
        if (!bazaarInventory && !auctionHouseInventory) return false
        showGreenLine = true


        if (ItemUtils.isSkyBlockMenuItem(stack)) {
            if (bazaarInventory) hideReason = "The SkyBlock Menu is not a Bazaar Product!"
            if (auctionHouseInventory) hideReason = "The SkyBlock Menu cannot be auctioned!"
            allowBypass = false
            return true
        }

        if (bazaarInventory != BazaarApi.isBazaarItem(stack)) {
            if (bazaarInventory) hideReason = "This item is not a Bazaar Product!"
            if (auctionHouseInventory) hideReason = "Bazaar Products cannot be auctioned!"

            return true
        }

        if (isNotAuctionable()) return true

        return false
    }

    fun isAuctionHouse(chestName: String): Boolean {
        val auctionHouseInventory =
            chestName == "Co-op Auction House" || chestName == "Auction House" ||
                chestName == "Create BIN Auction" || chestName == "Create Auction"
        return auctionHouseInventory
    }

    private fun ItemNotClickableEvent.isNotAuctionable(): Boolean {
        if (stack.isAnySoulbound()) {
            hideReason = "Soulbound items cannot be auctioned!"
            return true
        }

        val name = stack.cleanName

        if (ItemUtils.isSack(stack)) {
            hideReason = "Sacks cannot be auctioned!"
            return true
        }

        val result = notAuctionableFilter.match(name)
        if (result) hideReason = "This item cannot be auctioned!"
        return result
    }

    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "inventory.hideNotClickableItems", "inventory.hideNotClickable.items")
        event.move(3, "inventory.hideNotClickableItemsBlockClicks", "inventory.hideNotClickable.itemsBlockClicks")
        event.move(3, "inventory.hideNotClickableOpacity", "inventory.hideNotClickable.opacity")
        event.move(3, "inventory.notClickableItemsBypass", "inventory.hideNotClickable.itemsBypass")
        event.move(3, "inventory.hideNotClickableItemsGreenLine", "inventory.hideNotClickable.itemsGreenLine")
        event.move(108, "inventory.hideNotClickable.items", "inventory.hideNotClickable.enabled")
    }
}
