package at.hannibal2.skyhanni.features.inventory.loadout

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.inventory.customloadout.CustomLoadoutConfig
import at.hannibal2.skyhanni.events.MouseClickType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.AbstractCustomMenu
import at.hannibal2.skyhanni.utils.ColorUtils
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.ColorUtils.darker
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.ConditionalUtils.transformIf
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.FakePlayer
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.removeEnchants
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.compat.getTooltip
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.fakePlayer
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable.Companion.item
import at.hannibal2.skyhanni.utils.renderables.primitives.placeholder
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import com.google.gson.JsonPrimitive
import net.minecraft.client.Minecraft
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.Inventory
import java.awt.Color
import kotlin.math.min

@SkyHanniModule
@Suppress("TooManyFunctions")
object CustomLoadout : AbstractCustomMenu("Custom Loadout") {

    val config: CustomLoadoutConfig get() = SkyHanniMod.feature.inventory.customLoadout

    private const val LOADOUTS_PER_ROW = 6

    private var displayRenderable: Renderable? = null
    private var waitingForInventoryUpdate = false

    private val position: Position = Position().ignoreScale()

    private var activeScale: Int = 100
    private var currentMaxSize: Pair<Int, Int>? = null
    private var lastScreenSize: Pair<Int, Int>? = null

    override fun shouldReplace(inventoryName: String): Boolean {
        if (!isEnabled()) return false
        return LoadoutApi.matchesInventoryName(inventoryName)
    }

    override fun handleKeybinds() {
        CustomLoadoutKeybinds.handlePress()
    }

    override fun onScreenClosed() = reset()

    override fun renderOverlay(screenWidth: Int, screenHeight: Int) {
        val screenSize = screenWidth to screenHeight

        if (screenSize != lastScreenSize) {
            lastScreenSize = screenSize
            if (updateScreenSize(screenSize)) {
                return
            }
        }

        if (displayRenderable == null) update()
        val renderable = displayRenderable ?: return
        val left = (screenWidth - renderable.width) / 2
        val top = (screenHeight - renderable.height) / 2
        position.moveTo(left, top)
        position.renderRenderable(renderable, posLabel = guiName, addToGuiManager = false)
    }

    @HandleEvent(priority = HandleEvent.LOW, onlyOnSkyblock = true)
    private fun onInventoryUpdated() {
        DelayedRun.runOrNextTick {
            if (inCustomMenu) onInventoryUpdate()
        }
    }

    override fun onInventoryUpdate() {
        waitingForInventoryUpdate = false
        update()
    }

    private fun update() {
        displayRenderable = createRenderables()
    }

    override fun updateScreenSize(gui: Pair<Int, Int>): Boolean {
        val renderable = currentMaxSize ?: run {
            activeScale = config.spacing.globalScale
            update()
            return true
        }
        val previousActiveScale = activeScale
        val unscaledWidth = renderable.first / activeScale.toDouble()
        val unscaledHeight = renderable.second / activeScale.toDouble()
        val autoScaleWidth = 0.95 * gui.first / unscaledWidth
        val autoScaleHeight = 0.95 * gui.second / unscaledHeight
        val maxScale = min(autoScaleWidth, autoScaleHeight).toInt()

        activeScale = config.spacing.globalScale.coerceAtMost(maxScale)
        if (activeScale == previousActiveScale) return false
        update()
        return true
    }

    private fun createRenderables(): Renderable {
        val scale = activeScale / 100.0
        val containerWidth = (config.spacing.slotWidth * scale).toInt()
        val containerHeight = (config.spacing.slotHeight * scale).toInt()
        val playerScale = containerWidth * (config.spacing.playerScale / 100.0)
        val horizontalSpacing = (config.spacing.horizontalSpacing * scale).toInt()
        val verticalSpacing = (config.spacing.verticalSpacing * scale).toInt()
        val backgroundPadding = (config.spacing.backgroundPadding * scale).toInt()
        val buttonSlotsVerticalSpacing = (config.spacing.buttonSlotsVerticalSpacing * scale).toInt()

        val pageButtons = addPageButtons()
        val pageButtonsSpacing = horizontalSpacing + (containerWidth / 5)

        val maxRows = ((LoadoutApi.slots.count { it.page == 1 } - 1) / LOADOUTS_PER_ROW) + 1
        var maxWidth = LOADOUTS_PER_ROW * containerWidth + (LOADOUTS_PER_ROW - 1) * horizontalSpacing +
            pageButtonsSpacing + pageButtons.width
        var maxHeight = maxRows * containerHeight + (maxRows - 1) * verticalSpacing

        val bottomButtons = addBottomButtons()
        if (bottomButtons.width > maxWidth) maxWidth = bottomButtons.width
        maxHeight += bottomButtons.height + buttonSlotsVerticalSpacing
        maxWidth += 2 * backgroundPadding
        maxHeight += 2 * backgroundPadding
        currentMaxSize = maxWidth to maxHeight

        val pageSlots = displayedSlots()

        val grid = if (pageSlots.isEmpty()) {
            centerString("§cNo favorite loadouts on this page", scale = scale)
        } else {
            val rows = pageSlots.chunked(LOADOUTS_PER_ROW).map { row ->
                val slotRenderables =
                    row.map { slot -> createSlotRenderable(slot, playerScale, containerWidth, containerHeight) }
                Renderable.horizontal(slotRenderables, horizontalSpacing, verticalAlign = VerticalAlignment.CENTER)
            }
            Renderable.vertical(rows, verticalSpacing, horizontalAlign = HorizontalAlignment.CENTER)
        }

        val gridWithNav = Renderable.horizontal(
            pageButtonsSpacing,
            verticalAlign = VerticalAlignment.CENTER,
        ) {
            add(grid)
            add(pageButtons)
        }

        val total = Renderable.vertical(
            buttonSlotsVerticalSpacing,
            horizontalAlign = HorizontalAlignment.CENTER,
        ) {
            add(gridWithNav)
            add(bottomButtons)
        }

        return addGuiBackground(total, backgroundPadding)
    }

    private fun createSlotRenderable(
        slot: LoadoutSlot,
        playerScale: Double,
        containerWidth: Int,
        containerHeight: Int,
    ): Renderable {
        val (topOutline, bottomOutline) = slot.getOutlineColor()
        val background = createHoverableRenderable(
            Renderable.placeholder(containerWidth, containerHeight),
            topLayerRenderable = addSlotHoverableButtons(slot),
            hoveredColor = slot.getSlotColor(),
            borderOutlineThickness = config.spacing.outlineThickness,
            borderOutlineBlur = config.spacing.outlineBlur,
            onClick = { clickSlot(slot) },
            onRightClick = { rightClickSlot(slot) },
            topOutlineColor = topOutline,
            bottomOutlineColor = bottomOutline,
        )
        val player = createFakePlayerRenderable(slot, playerScale * 0.75, containerWidth, containerHeight)
        var layered =
            Renderable.doubleLayered(background, player, blockBottomHover = false, forceBottomRenderFirst = true)

        addSlotName(slot, containerWidth)?.let { nameOverlay ->
            layered =
                Renderable.doubleLayered(layered, nameOverlay, blockBottomHover = false, forceBottomRenderFirst = true)
        }

        addPetOverlay(slot, containerWidth, containerHeight)?.let { petOverlay ->
            layered =
                Renderable.doubleLayered(layered, petOverlay, blockBottomHover = false, forceBottomRenderFirst = true)
        }
        return addTooltip(slot, layered)
    }

    private fun addTooltip(slot: LoadoutSlot, content: Renderable): Renderable {
        val iconTooltip = slot.getUncachedIcon()?.let { getIconTooltip(it, slot) }
            ?: return Renderable.hoverTips(content, tips = buildLoadoutTooltip(slot))
        return Renderable.hoverTips(content, tips = iconTooltip)
    }

    private fun getIconTooltip(icon: SafeItemStack, slot: LoadoutSlot): List<Component>? {
        try {
            return icon.getTooltip(Minecraft.getInstance().options.advancedItemTooltips)
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(
                e,
                "Failed to get tooltip for loadout icon in CustomLoadout",
                "Icon" to icon,
                "Slot" to slot.id,
            )
            return null
        }
    }

    private fun addSlotName(slot: LoadoutSlot, containerWidth: Int): Renderable? {
        if (slot.locked) return null
        val name = slot.name ?: return null
        val baseScale = activeScale / 100.0
        val rawTextWidth = Minecraft.getInstance().font.width(name).toDouble().coerceAtLeast(1.0)
        val maxTextWidth = containerWidth * 0.66
        val scale = minOf(baseScale, maxTextWidth / rawTextWidth)
        return Renderable.vertical {
            add(Renderable.placeholder(0, 4))
            add(
                Renderable.horizontal {
                    add(Renderable.placeholder(4, 0))
                    add(centerString(name, scale = scale))
                }
            )
        }
    }

    private fun addPetOverlay(slot: LoadoutSlot, containerWidth: Int, containerHeight: Int): Renderable? {
        if (slot.locked) return null
        val pet = slot.getData()?.pet ?: return null
        val petItem = Renderable.item(pet) {
            scale = containerWidth / 50.0
            horizontalAlign = HorizontalAlignment.LEFT
            verticalAlign = VerticalAlignment.TOP
        }
        val besideHead = Renderable.horizontal(
            listOf(Renderable.placeholder((containerWidth * 0.66).toInt(), 0), petItem),
            horizontalAlign = HorizontalAlignment.LEFT,
            verticalAlign = VerticalAlignment.TOP,
        )
        return Renderable.vertical(
            listOf(Renderable.placeholder(0, containerHeight / 8), besideHead),
            horizontalAlign = HorizontalAlignment.LEFT,
            verticalAlign = VerticalAlignment.TOP,
        )
    }

    private fun addSlotHoverableButtons(slot: LoadoutSlot): Renderable {
        val textScale = 1.5 * (activeScale / 100.0)
        val canFavorite = !slot.isEmpty() && !slot.locked
        if (!canFavorite && !slot.favorite) return Renderable.placeholder(0, 0)
        val heart = Renderable.clickable(
            Renderable.hoverable(
                centerString((if (slot.favorite) "§c" else "§7") + "❤", scale = textScale),
                centerString((if (slot.favorite) "§4" else "§8") + "❤", scale = textScale),
            ),
            onLeftClick = {
                slot.favorite = !slot.favorite
                update()
            },
        )
        return Renderable.vertical(
            horizontalAlign = HorizontalAlignment.RIGHT,
        ) {
            add(Renderable.placeholder(0, 4))
            add(
                Renderable.horizontal {
                    add(heart)
                    add(Renderable.placeholder(4, 0))
                }
            )
        }
    }

    private fun createFakePlayerRenderable(
        slot: LoadoutSlot,
        playerScale: Double,
        containerWidth: Int,
        containerHeight: Int,
    ): Renderable {
        val fakePlayer = FakePlayer.fromLocalPlayerOrThrow()
        val armor = slot.getDisplayedArmor()

        for (equipment in Inventory.EQUIPMENT_SLOT_MAPPING.values) {
            val armorOrdinal = equipment.ordinal - 2
            if (armorOrdinal !in 0..3) continue
            val stack = armor.reversed()[armorOrdinal]?.copy()?.removeEnchants() ?: SafeItemStack.EMPTY
            fakePlayer.equipment.set(equipment, stack)
        }

        return Renderable.fakePlayer(
            fakePlayer,
            followMouse = config.eyesFollowMouse,
            width = containerWidth,
            height = containerHeight,
            entityScale = playerScale.toInt(),
            padding = 0,
        )
    }

    private fun LoadoutSlot.getDisplayedArmor(): List<SafeItemStack?> {
        val armorPiece = getUncachedArmorPiece() ?: return getData()?.armor ?: LoadoutApi.emptyArmor()
        val armorIndex = when (armorPiece.get(DataComponents.EQUIPPABLE)?.slot()) {
            EquipmentSlot.CHEST -> 1
            EquipmentSlot.LEGS -> 2
            EquipmentSlot.FEET -> 3
            else -> 0
        }
        return List(LoadoutApi.emptyArmor().size) { index -> armorPiece.takeIf { index == armorIndex } }
    }

    private fun buildLoadoutTooltip(slot: LoadoutSlot): List<String> {
        val data = slot.getData()
        return buildList {
            add("§9Loadout ${slot.id + 1}")
            when {
                slot.locked -> add("§cLocked")
                data == null || slot.isEmpty() -> add("§7Select this loadout to load its gear.")
                else -> {
                    addItems("§7Equipment:", data.equipment)
                    data.pet?.let { add(" §7Pet: ${it.hoverName.formattedTextCompatLeadingWhiteLessResets()}") }
                    data.powerstone?.let { add(" §7Power Stone: §a$it") }
                    data.tunings?.let { tunings ->
                        add(" §7Tuning:")
                        tunings.forEach { add("   $it") }
                    }
                    data.hotm?.let { add(" §7HotM: §a$it") }
                    data.hotf?.let { add(" §7HotF: §a$it") }
                }
            }
        }
    }

    private fun MutableList<String>.addItems(label: String, items: List<SafeItemStack?>) {
        val names = items.mapNotNull { it?.hoverName?.formattedTextCompatLeadingWhiteLessResets() }
        if (names.isEmpty()) return
        add(label)
        names.forEach { add("   §7- $it") }
    }

    private fun addPageButtons(): Renderable {
        val current = LoadoutApi.currentPage ?: 1
        val verticalSpacing = (config.spacing.buttonVerticalSpacing * (activeScale / 100.0)).toInt()
        val upButton = createLabeledButton("§a▲", onClick = { changePage(-1) })
        val downButton = createLabeledButton("§a▼", onClick = { changePage(1) })
        val pageIndicator = centerString("§7$current/${LoadoutApi.maxPage}", scale = activeScale / 100.0)
        return Renderable.vertical(
            listOf(upButton, pageIndicator, downButton),
            verticalSpacing,
            horizontalAlign = HorizontalAlignment.CENTER,
            verticalAlign = VerticalAlignment.CENTER,
        )
    }

    private fun addBottomButtons(): Renderable {
        val horizontalSpacing = (config.spacing.buttonHorizontalSpacing * (activeScale / 100.0)).toInt()
        val verticalSpacing = (config.spacing.buttonVerticalSpacing * (activeScale / 100.0)).toInt()
        val backButton = createLabeledButton("§aBack", onClick = { exit(48) })
        val closeButton = createLabeledButton("§cClose", onClick = { exit(49) })

        val greenColor = Color(85, 255, 85, 200)
        val redColor = Color(255, 85, 85, 200)
        val favoriteButton = createLabeledButton(
            "§eFavorite",
            hoveredColor = if (config.onlyFavorites) greenColor else redColor,
            onClick = {
                config.onlyFavorites = !config.onlyFavorites
                update()
            },
        )
        val editButton = createLabeledButton(
            "§bEdit",
            onClick = {
                DelayedRun.runNextTick {
                    reset()
                    enterEditMode()
                }
            },
        )

        val row = Renderable.horizontal(
            listOf(backButton, closeButton, favoriteButton),
            horizontalSpacing,
            horizontalAlign = HorizontalAlignment.CENTER,
        )
        return Renderable.vertical(
            listOf(row, editButton),
            verticalSpacing,
            horizontalAlign = HorizontalAlignment.CENTER,
            verticalAlign = VerticalAlignment.CENTER,
        )
    }

    private fun exit(inventorySlot: Int) {
        InventoryUtils.clickSlot(inventorySlot)
        reset()
    }

    private fun changePage(delta: Int) {
        if (waitingForInventoryUpdate) return
        val current = LoadoutApi.currentPage ?: 1
        val target = (current + delta).coerceIn(1, LoadoutApi.maxPage)
        if (target == current) return
        LoadoutApi.currentPage = target
        waitingForInventoryUpdate = true
        InventoryUtils.clickSlot(if (delta < 0) LoadoutApi.PREVIOUS_PAGE_SLOT else LoadoutApi.NEXT_PAGE_SLOT)
        update()
    }

    fun clickSlot(slot: LoadoutSlot) {
        if (waitingForInventoryUpdate) return
        LoadoutApi.clickSlot(slot)
        update()
    }

    private fun rightClickSlot(slot: LoadoutSlot) {
        if (!slot.isInCurrentPage() || slot.locked || waitingForInventoryUpdate) return
        InventoryUtils.clickSlot(slot.inventorySlot, button = MouseClickType.RIGHT_CLICK)
    }

    fun displayedSlots(): List<LoadoutSlot> {
        val pageSlots = LoadoutApi.slots.filter { it.isInCurrentPage() }
        return if (config.onlyFavorites) pageSlots.filter { it.favorite || it.isCurrentSlot() } else pageSlots
    }

    internal fun reset() {
        displayRenderable = null
        waitingForInventoryUpdate = false
        currentMaxSize = null
        lastScreenSize = null
    }

    private fun LoadoutSlot.getOutlineColor(): Pair<Color, Color> {
        val (top, bottom) = config.color.topBorderColor.toColor() to config.color.bottomBorderColor.toColor()
        return when {
            isEmpty() || locked -> ColorUtils.TRANSPARENT_COLOR to ColorUtils.TRANSPARENT_COLOR
            else -> top to bottom
        }
    }

    private fun LoadoutSlot.getSlotColor(): Color = with(config.color) {
        when {
            isCurrentSlot() -> equippedColor.toColor()
            favorite && !config.onlyFavorites -> favoriteColor.toColor()
            else -> samePageColor.toColor().transformIf({ locked || isEmpty() }) { darker(0.2) }.addAlpha(100)
        }
    }

    private fun addGuiBackground(renderable: Renderable, borderPadding: Int) = Renderable.drawInsideRoundedRect(
        renderable,
        config.color.backgroundColor.toColor(),
        padding = borderPadding,
    )

    override fun createLabeledButton(
        text: String,
        hoveredColor: Color,
        unhoveredColor: Color,
        onClick: () -> Unit,
    ): Renderable {
        val buttonWidth = (config.spacing.buttonWidth * (activeScale / 100.0)).toInt()
        val buttonHeight = (config.spacing.buttonHeight * (activeScale / 100.0)).toInt()
        val textScale = activeScale / 100.0

        return Renderable.hoverable(
            Renderable.drawInsideRoundedRectWithOutline(
                Renderable.doubleLayered(
                    Renderable.clickable(Renderable.placeholder(buttonWidth, buttonHeight), onClick),
                    centerString(text, scale = textScale),
                    false,
                ),
                hoveredColor,
                padding = 0,
                topOutlineColor = config.color.topBorderColor.toColor().rgb,
                bottomOutlineColor = config.color.bottomBorderColor.toColor().rgb,
                borderOutlineThickness = 2,
                horizontalAlign = HorizontalAlignment.CENTER,
            ),
            Renderable.drawInsideRoundedRect(
                Renderable.doubleLayered(
                    Renderable.placeholder(buttonWidth, buttonHeight),
                    centerString(text, scale = textScale),
                ),
                unhoveredColor.darker(0.57),
                padding = 0,
                horizontalAlign = HorizontalAlignment.CENTER,
            ),
        )
    }

    private fun createHoverableRenderable(
        hoveredRenderable: Renderable,
        topLayerRenderable: Renderable = Renderable.placeholder(0, 0),
        hoveredColor: Color,
        borderOutlineThickness: Int,
        borderOutlineBlur: Float = 0.5f,
        onClick: () -> Unit,
        onRightClick: () -> Unit = {},
        topOutlineColor: Color,
        bottomOutlineColor: Color,
    ): Renderable = Renderable.hoverable(
        Renderable.drawInsideRoundedRectWithOutline(
            Renderable.doubleLayered(
                Renderable.clickable(
                    hoveredRenderable,
                    mapOf(KeyboardManager.LEFT_MOUSE to onClick, KeyboardManager.RIGHT_MOUSE to onRightClick),
                ),
                topLayerRenderable,
            ),
            hoveredColor,
            padding = 0,
            topOutlineColor = topOutlineColor.rgb,
            bottomOutlineColor = bottomOutlineColor.rgb,
            borderOutlineThickness = borderOutlineThickness,
            blur = borderOutlineBlur,
            horizontalAlign = HorizontalAlignment.CENTER,
            verticalAlign = VerticalAlignment.CENTER,
        ),
        Renderable.drawInsideRoundedRect(
            Renderable.placeholder(hoveredRenderable.width, hoveredRenderable.height),
            hoveredColor,
            padding = 0,
            horizontalAlign = HorizontalAlignment.CENTER,
            verticalAlign = VerticalAlignment.CENTER,
        ),
    )

    private fun centerString(
        text: String,
        scale: Double = 1.0,
        color: Color = Color.WHITE,
    ) = Renderable.text(text, scale, color, horizontalAlign = HorizontalAlignment.CENTER)

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.enabled

    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        val wardrobe = "inventory.customWardrobe"
        val loadout = "inventory.customLoadout"

        // The custom loadout gui is off by default, but gets enabled for everyone who used the custom wardrobe
        var customWardrobeEnabled = true
        event.move(147, "$wardrobe.enabled", "$loadout.wardrobe.enabled") { element ->
            customWardrobeEnabled = element.asBoolean
            element
        }
        event.add(147, "$loadout.enabled") {
            JsonPrimitive(customWardrobeEnabled)
        }

        // These are now shared between the loadout, wardrobe and equipment guis
        event.move(147, "$wardrobe.eyesFollowMouse", "$loadout.eyesFollowMouse")
        event.move(147, "$wardrobe.color", "$loadout.color")
        event.move(147, "$wardrobe.spacing.maxPlayersPerRow", "$loadout.wardrobe.maxPlayersPerRow")
        event.move(147, "$wardrobe.spacing", "$loadout.spacing")

        val wardrobeOptions = listOf(
            "hideEmptySlots", "hideLockedSlots", "onlyFavorites", "estimatedValue", "loadingText",
            "showTooltipOnlyKeybind", "tooltipKeybind", "showReiItems", "keybinds",
        )
        for (option in wardrobeOptions) {
            event.move(147, "$wardrobe.$option", "$loadout.wardrobe.$option")
        }
    }
}
