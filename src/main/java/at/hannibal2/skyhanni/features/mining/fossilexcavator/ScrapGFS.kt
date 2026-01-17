package at.hannibal2.skyhanni.features.mining.fossilexcavator

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.GetFromSackApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.SackApi
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.OwnInventoryItemUpdateEvent
import at.hannibal2.skyhanni.events.SackChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils.afterChange
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.KeyboardManager.LEFT_MOUSE
import at.hannibal2.skyhanni.utils.KeyboardManager.RIGHT_MOUSE
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.NeuItemStackProvider
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.animated.AnimatedItemStackRenderable.Companion.animatedItemStack
import at.hannibal2.skyhanni.utils.renderables.animated.ItemStackAnimationFrame
import at.hannibal2.skyhanni.utils.renderables.animated.ItemStackRotationDefinition
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.system.LazyVar
import net.minecraft.core.Direction.Axis
import net.minecraft.world.phys.Vec3
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object ScrapGFS {

    private val config get() = SkyHanniMod.feature.mining.fossilExcavator.scrapGFS
    private val enabled get() = config.enabled && FossilExcavatorApi.inExcavatorMenu
    private val currentFetchAmount get() = config.fetchAmount.get()
    private val scrapProvider = NeuItemStackProvider(FossilExcavatorApi.scrapItem)
    private val scrapFrames = listOf(ItemStackAnimationFrame(scrapProvider, ticks = 0))
    private val scrapRotationDefinition = ItemStackRotationDefinition(
        axis = Axis.Y,
        rotationSpeed = 50.0,
    )
    private val validRange = 1..2048

    private val darkGray = LorenzColor.DARK_GRAY.toColor()
    private val darkerGray = darkGray.darker()
    private val darkestGray = darkerGray.darker()
    private val lighterGray = darkGray.brighter()
    private val lightestGray = lighterGray.brighter()

    private var uiDirty: Boolean = true
    private var susScrapInInventory by LazyVar { getSusScrapCurrentlyInInventory() }
    private var renderable: Renderable? = null
    private var currentScrapRotation: Vec3 = Vec3(0.0, 0.0, 0.0)
    private var lastScrollSound: SimpleTimeMark = SimpleTimeMark.farPast()
    private var lastScrollErrorSound: SimpleTimeMark = SimpleTimeMark.farPast()

    private fun getSusScrapCurrentlyInInventory() = InventoryUtils.getItemsInOwnInventory().filter {
        it.getInternalNameOrNull() == FossilExcavatorApi.scrapItem
    }.sumOf { it.count }

    @HandleEvent
    fun onConfigLoad() {
        config.fetchAmount.afterChange {
            uiDirty = true
            if (currentFetchAmount !in validRange) {
                config.fetchAmount.set(validRange.first)
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.DWARVEN_MINES)
    fun onOwnInventoryItemUpdate(event: OwnInventoryItemUpdateEvent) {
        if (!enabled) return
        susScrapInInventory = getSusScrapCurrentlyInInventory()
        uiDirty = true
    }

    @HandleEvent(onlyOnIsland = IslandType.DWARVEN_MINES)
    fun onSackChange(event: SackChangeEvent) {
        if (!enabled || event.sackChanges.none { it.internalName == FossilExcavatorApi.scrapItem }) return
        uiDirty = true
    }

    @HandleEvent(GuiRenderEvent.ChestGuiOverlayRenderEvent::class, onlyOnIsland = IslandType.DWARVEN_MINES)
    fun onBackgroundDraw() {
        if (!enabled) return
        if (config.onlyIfNoScrap && susScrapInInventory > 0) return
        if (uiDirty) {
            renderable = buildFinalRenderable()
            uiDirty = false
        }
        val renderable = renderable ?: return
        config.position.renderRenderable(renderable, posLabel = "Scrap GFS")
    }

    private fun buildFinalRenderable() = Renderable.drawInsideRoundedRectWithOutline(
        Renderable.vertical(
            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
            verticalAlign = RenderUtils.VerticalAlignment.CENTER,
            spacing = 5,
        ) {
            addFetchButton()
            addCenterDisplay()
            addSackInfoFooter()
        },
        color = darkGray,
        topOutlineColor = lighterGray.rgb,
        bottomOutlineColor = lighterGray.rgb,
        borderOutlineThickness = 3,
        horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
        verticalAlign = RenderUtils.VerticalAlignment.CENTER,
        padding = 5,
    )

    private fun MutableList<Renderable>.addCenterDisplay() = Renderable.horizontal(
        spacing = 10,
        horizontalAlign = RenderUtils.HorizontalAlignment.CENTER
    ) {
        addStaticSetButton("Minimum", LorenzColor.RED, validRange.first)
        addCurrentFetchContainer()
        addStaticSetButton("Maximum", LorenzColor.GREEN, validRange.last)
    }.let { add(it) }


    private fun MutableList<Renderable>.addSackInfoFooter() = with(SackApi) {
        FossilExcavatorApi.scrapItem.getAmountInSacksOrNull()?.takeIf { it > 0 }?.let { scrapInSacks ->
            Renderable.link(
                Renderable.hoverTips(
                    Renderable.text(
                        "Scrap In Sacks: §f$scrapInSacks",
                        scale = 0.9,
                        color = lighterGray,
                        horizontalAlign = RenderUtils.HorizontalAlignment.CENTER
                    ),
                    tips = listOf("§7Click to set fetch amount to §f$scrapInSacks§7!")
                ),
                onLeftClick = {
                    SoundUtils.playClickSound()
                    config.fetchAmount.set(scrapInSacks)
                },
                underlineColor = lightestGray,
            ).let { add(it) }
        } ?: run {
            val baseNoScrapRenderable = Renderable.text(
                "No Scrap in Sacks!",
                scale = 0.9,
                color = lighterGray,
                horizontalAlign = RenderUtils.HorizontalAlignment.CENTER
            )
            when (config.bzIfSacksEmpty && !HypixelData.noTrade) {
                true -> Renderable.vertical {
                    add(baseNoScrapRenderable)
                    Renderable.link(
                        Renderable.text(
                            "§eClick to buy from Bazaar",
                            scale = 0.9,
                            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER
                        ),
                        onLeftClick = { HypixelCommands.bazaar("Suspicious Scrap") },
                        underlineColor = lightestGray,
                    ).let { add(it) }
                }
                else -> baseNoScrapRenderable
            }.let { add(it) }
        }
    }

    private fun MutableList<Renderable>.addFetchButton() = Renderable.darkRectButton(
        Renderable.text("Get Scrap from Sacks", horizontalAlign = RenderUtils.HorizontalAlignment.CENTER, scale = 1.2),
        onClick = {
            SoundUtils.playClickSound()
            GetFromSackApi.getFromSack(FossilExcavatorApi.scrapItem, currentFetchAmount)
            uiDirty = true
        },
        padding = 4,
    ).let { add(it) }

    private fun MutableList<Renderable>.addCurrentFetchContainer() = Renderable.clickableAndScrollable(
        Renderable.drawInsideRoundedRectWithOutline(
            Renderable.vertical(
                listOf(
                    Renderable.animatedItemStack(
                        scrapFrames,
                        scrapRotationDefinition,
                        scale = 1.2,
                        horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                        initialRotation = currentScrapRotation,
                    ) { currentScrapRotation = it },
                    Renderable.text(currentFetchAmount.toString(), scale = 0.9, horizontalAlign = RenderUtils.HorizontalAlignment.CENTER),
                ),
                horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                verticalAlign = RenderUtils.VerticalAlignment.CENTER,
                spacing = 3,
            ),
            padding = 4,
            color = darkestGray,
            topOutlineColor = darkerGray.rgb,
            bottomOutlineColor = darkerGray.rgb,
            borderOutlineThickness = 2,
        ),
        onAnyClick = mapOf(
            LEFT_MOUSE to { scrollWithStaggerSound(-1) },
            RIGHT_MOUSE to { scrollWithStaggerSound(1) },
        )
    ).let { add(it) }

    private fun scrollWithStaggerSound(offset: Int) {
        if (!offset.incrementValid()) {
            SoundUtils.playErrorSound()
            return
        }
        config.fetchAmount.set(currentFetchAmount + offset)
        if (lastScrollSound.passedSince() < 100.milliseconds) return
        lastScrollSound = SimpleTimeMark.now()
        SoundUtils.playClickSound()
    }

    private fun Int.incrementValid() = currentFetchAmount + this in validRange
    private fun Int.getIncrementText() = if (this > 0) "+$this" else this.toString()

    private fun buildIncrementText(increment: Int) = Renderable.text(
        when {
            !increment.incrementValid() -> "§7"
            increment > 0 -> "§a"
            else -> "§c"
        } + increment.getIncrementText(),
        horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
    )

    private fun buildIncrementButton(increment: Int): Renderable = Renderable.darkRectButton(
        buildIncrementText(increment),
        onClick = {
            if (!increment.incrementValid()) {
                if (lastScrollErrorSound.passedSince() > 100.milliseconds) {
                    SoundUtils.playErrorSound()
                    lastScrollErrorSound = SimpleTimeMark.now()
                }
                return@darkRectButton
            }
            SoundUtils.playClickSound()
            config.fetchAmount.set(currentFetchAmount + increment)
        },
        condition = { increment.incrementValid() },
        padding = 4,
    )

    private fun MutableList<Renderable>.addStaticSetButton(
        label: String,
        color: LorenzColor,
        value: Int
    ) = Renderable.darkRectButton(
        Renderable.vertical(
            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
            verticalAlign = RenderUtils.VerticalAlignment.CENTER,
        ) {
            val pre = color.getChatColor()
            Renderable.text(
                "$pre$label",
                horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                scale = 0.9
            ).let { add(it) }
            Renderable.text(
                "$pre($value)",
                horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                scale = 0.8
            ).let { add(it) }
        },
        onClick = {
            SoundUtils.playClickSound()
            config.fetchAmount.set(value)
        },
        padding = 4,
    ).let {
        add(Renderable.vertical(verticalAlign = RenderUtils.VerticalAlignment.CENTER) { add(it) })
    }
}
