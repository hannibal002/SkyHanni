package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.storage.ResettableStorageSet
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ColorUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.enumMapOf
import at.hannibal2.skyhanni.utils.compat.GuiScreenUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXYAligned
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.opengl.GL11
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object TitleManager {

    private val titleLocationQueues: MutableMap<TitleLocation, CollectionUtils.OrderedQueue<TitleData>> = enumMapOf()
    private val currentTitles: MutableMap<TitleLocation, TitleData?> = enumMapOf()

    private data class TitleData(
        var titleText: String = "",
        var subtitleText: String? = null,
        var duration: Duration = 0.seconds,
        var height: Double = 1.8,
        var fontSize: Float = 4f,
        val weight: Double = 1.0,
    ) : ResettableStorageSet() {
        var endTime: SimpleTimeMark = SimpleTimeMark.now() + duration

        fun stop() {
            endTime = SimpleTimeMark.now()
        }
    }

    enum class TitleLocation(
        private val displayName: String,
        val activationRequirement: () -> Boolean = { true },
    ) {
        GLOBAL("Global"),
        INVENTORY("Inventory", activationRequirement = { InventoryUtils.inInventory() }),
        ;

        override fun toString() = displayName
    }

    enum class TitleAddType(private val displayName: String) {
        FORCE_FIRST("Force First"),
        QUEUE("Queue"),
        ;

        override fun toString() = displayName
    }

    fun sendTitle(
        titleText: String,
        subtitleText: String? = null,
        duration: Duration = 3.seconds,
        height: Double = 1.8,
        fontSize: Float = 4f,
        location: TitleLocation = TitleLocation.GLOBAL,
        addType: TitleAddType = TitleAddType.QUEUE,
        weight: Double = 1.0,
    ) {
        val newTitle = TitleData(titleText, subtitleText, duration, height, fontSize, weight)
        val targetQueue = titleLocationQueues.getOrPut(location) { CollectionUtils.OrderedQueue() }

        if (addType == TitleAddType.QUEUE) {
            targetQueue.add(newTitle, weight)
        } else {
            val currentTitle = currentTitles[location]
            if (currentTitle != null && !currentTitle.endTime.isInPast()) {
                // Push back into the queue
                targetQueue.add(currentTitle, currentTitle.weight)
                currentTitle.applyFromOther(newTitle)
            } else {
                currentTitles[location] = newTitle
            }
        }
    }

    fun optionalResetTitle(
        location: TitleLocation? = TitleLocation.GLOBAL,
        condition: (String) -> Boolean,
    ) {
        when (location) {
            null -> {
                currentTitles.values.filterNotNull()
                    .filter { condition(it.titleText) }
                    .forEach { it.stop() }
            }

            else -> currentTitles[location]?.let { title ->
                if (condition(title.titleText)) {
                    title.stop()
                }
            }
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shsendtitle") {
            description = "Display a title on the screen with the specified settings."
            category = CommandCategory.DEVELOPER_TEST
            callback { command(it, this.name) }
        }
        event.register("shsendinventorytitle") {
            description = "Display a title on the inventory screen with the specified settings."
            category = CommandCategory.DEVELOPER_TEST
            callback { command(it, this.name, TitleLocation.INVENTORY) }
        }
    }

    private fun command(args: Array<String>, command: String, location: TitleLocation = TitleLocation.GLOBAL) {
        if (args.size < 4) {
            ChatUtils.userError("Usage: /$command <duration> <height> <fontSize> <text ..>")
            return
        }

        val duration = TimeUtils.getDurationOrNull(args[0]) ?: run {
            ChatUtils.userError("Invalid duration format `${args[0]}`! Use e.g. 10s, or 20m or 30h")
            return
        }
        val height = args[1].toDouble()
        val fontSize = args[2].toFloat()
        val title = "§6" + args.drop(3).joinToString(" ").replace("&", "§")

        sendTitle(title, subtitleText = null, duration, height, fontSize, location)
    }

    @HandleEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        stop()
    }

    @HandleEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        stop(TitleLocation.INVENTORY)
    }

    private fun stop(location: TitleLocation? = null) {
        when (location) {
            null -> currentTitles.values.filterNotNull().forEach { it.stop() }
            else -> currentTitles[location]?.stop()
        }
    }

    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
        TitleLocation.entries.filter {
            it.activationRequirement.invoke()
        }.forEach { location ->
            when (val currentTitle = currentTitles[location]) {
                null -> dequeueNextTitle(location)
                else -> {
                    if (currentTitle.endTime.isInFuture()) return@forEach
                    dequeueNextTitle(location)
                }
            }
        }
    }

    private fun dequeueNextTitle(location: TitleLocation) {
        val titleQueue = titleLocationQueues[location]
        val title = titleQueue?.pollOrNull()
        currentTitles[location] = title
    }

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (InventoryUtils.inInventory()) return
        val globalTitle = currentTitles[TitleLocation.GLOBAL] ?: return
        globalTitle.tryRenderGlobalTitle()
    }

    private fun TitleData.tryRenderGlobalTitle() {
        val guiWidth = GuiScreenUtils.scaledWindowWidth
        val guiHeight = GuiScreenUtils.scaledWindowHeight

        val globalTitleWidth = 200
        val stringWidth = Minecraft.getMinecraft().fontRendererObj.getStringWidth(titleText)
        var factor = globalTitleWidth / stringWidth.toDouble()
        factor = min(factor, 1.0)

        val mainScalar = factor * fontSize
        val subScalar = mainScalar * 0.75f

        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0)
        GlStateManager.pushMatrix()

        val mainTextRenderable = Renderable.string(
            titleText,
            scale = mainScalar,
            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
        )

        val subtitleRenderable: Renderable? = subtitleText?.let {
            Renderable.string(
                it,
                scale = subScalar,
                horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
            )
        }

        val targetRenderable = if (subtitleRenderable == null) mainTextRenderable
        else Renderable.verticalContainer(
            listOf(mainTextRenderable, subtitleRenderable),
            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
            verticalAlign = RenderUtils.VerticalAlignment.CENTER,
        )

        val renderableWidth = targetRenderable.width
        val renderableHeight = targetRenderable.height

        val posX = (guiWidth - renderableWidth) / 2
        val posY = (guiHeight - (renderableHeight * 4)) / 2

        GlStateManager.translate(posX.toFloat(), posY.toFloat(), 0f)
        targetRenderable.renderXYAligned(0, 0, renderableWidth, renderableHeight)
        GlStateManager.popMatrix()
    }


    @HandleEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!InventoryUtils.inInventory()) return
        val inventoryTitle = currentTitles[TitleLocation.INVENTORY] ?: return
        inventoryTitle.tryRenderInventoryTitle()
    }

    private fun TitleData.tryRenderInventoryTitle() {
        val gui = Minecraft.getMinecraft().currentScreen as? GuiContainer ?: return

        val baseStringRenderable = Renderable.string(titleText, 1.5)
        val stringRenderable = when (subtitleText) {
            null -> baseStringRenderable
            else -> {
                val displaySubText = subtitleText ?: return
                Renderable.verticalContainer(
                    listOf(
                        baseStringRenderable,
                        Renderable.string(
                            displaySubText,
                            scale = 1.0,
                            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                        ),
                    ),
                    horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                )
            }
        }

        val heightTranslation = when (subtitleText) {
            null -> 100f
            else -> 150f
        }

        GlStateManager.pushMatrix()
        GlStateManager.translate(0f, -(heightTranslation), 500f)
        Renderable.drawInsideRoundedRect(
            stringRenderable,
            ColorUtils.TRANSPARENT_COLOR,
            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
            verticalAlign = RenderUtils.VerticalAlignment.CENTER,
        ).renderXYAligned(0, 0, gui.width, gui.height)

        GlStateManager.translate(0f, heightTranslation, -500f)
        GlStateManager.popMatrix()
    }
}
