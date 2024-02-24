package at.hannibal2.skyhanni.features.misc.compacttablist

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SkipTabListLineEvent
import at.hannibal2.skyhanni.mixins.transformers.AccessorGuiPlayerTabOverlay
import at.hannibal2.skyhanni.utils.CollectionUtils.filterToMutable
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.player.EnumPlayerModelParts
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object TabListRenderer {

    private val config get() = SkyHanniMod.feature.misc.compactTabList

    const val maxLines = 22
    private const val lineHeight = 8 + 1
    private const val padding = 3
    private const val columnSpacing = 6

    @SubscribeEvent
    fun onRenderOverlay(event: RenderGameOverlayEvent.Pre) {
        if (!LorenzUtils.inSkyBlock) return
        if (event.type != RenderGameOverlayEvent.ElementType.PLAYER_LIST) return
        if (!config.enabled) return
        event.isCanceled = true

        if (config.toggleTab) return

        drawTabList()
    }

    private var isPressed = false
    private var isTabToggled = false

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.enabled) return
        if (!config.toggleTab) return
        if (Minecraft.getMinecraft().currentScreen != null) return

        if (Minecraft.getMinecraft().gameSettings.keyBindPlayerList.isPressed) {
            isPressed = true
        } else {
            if (isPressed) {
                isPressed = false
                isTabToggled = !isTabToggled
            }
        }

        if (isTabToggled) {
            drawTabList()
        }
    }

    private fun drawTabList() {
        val columns = TabListReader.renderColumns

        if (columns.isEmpty()) return

        var maxLines = 0
        var totalWidth = 0 - columnSpacing

        for (column in columns) {
            maxLines = maxLines.coerceAtLeast(column.size())
            totalWidth += column.getMaxWidth() + columnSpacing
        }

        var totalHeight = maxLines * lineHeight
        val tabList = Minecraft.getMinecraft().ingameGUI.tabList as AccessorGuiPlayerTabOverlay

        var header = listOf<String>()
        if (tabList.header_skyhanni != null) {
            header = tabList.header_skyhanni.formattedText.split("\n").toMutableList()
            header.removeIf { line -> !line.contains(TabListReader.hypixelAdvertisingString) }
            if (config.hideAdverts) {
                header = listOf()
            } else {
                totalHeight += header.size * lineHeight + padding
            }
        }

        var footer = listOf<String>()
        if (tabList.footer_skyhanni != null) {
            footer = tabList.footer_skyhanni.formattedText.split("\n").toMutableList()
            footer.removeIf { line -> !line.contains(TabListReader.hypixelAdvertisingString) }
            if (config.hideAdverts) {
                footer = listOf()
            } else {
                totalHeight += footer.size * lineHeight + padding
            }
        }

        val minecraft = Minecraft.getMinecraft()
        val scaledResolution = ScaledResolution(minecraft)
        val screenWidth = scaledResolution.scaledWidth / 2
        val x = screenWidth - totalWidth / 2
        val y = 10

        Gui.drawRect(
            x - columnSpacing,
            y - padding,
            screenWidth + totalWidth / 2 + columnSpacing,
            10 + totalHeight + padding,
            -0x80000000
        )

        var headerY = y
        if (header.isNotEmpty()) {
            for (line in header) {
                minecraft.fontRendererObj.drawStringWithShadow(
                    line,
                    x + totalWidth / 2f - minecraft.fontRendererObj.getStringWidth(line) / 2f,
                    headerY.toFloat(),
                    0xFFFFFF
                )
                headerY += 8 + 1
            }
        }

        var middleX = x
        var lastTitle: TabLine? = null
        var lastSubTitle: TabLine? = null
        for (originalColumn in columns) {
            var middleY = if (config.hideAdverts) headerY else headerY + padding + 2

            val column = originalColumn.lines.filterToMutable { tabLine ->
                if (tabLine.type == TabStringType.TITLE) {
                    lastSubTitle = null
                    lastTitle = tabLine
                }
                if (tabLine.type == TabStringType.SUB_TITLE) {
                    lastSubTitle = tabLine
                }
                !SkipTabListLineEvent(tabLine, lastSubTitle, lastTitle).postAndCatch()
            }.let(::RenderColumn)

            Gui.drawRect(
                middleX - padding + 1,
                middleY - padding + 1,
                middleX + column.getMaxWidth() + padding - 2,
                middleY + column.size() * lineHeight + padding - 2,
                0x20AAAAAA
            )

            for (tabLine in column.lines) {
                val savedX = middleX

                val hideIcons = config.advancedPlayerList.hidePlayerIcons && !AdvancedPlayerList.ignoreCustomTabList()
                if (tabLine.type == TabStringType.PLAYER && !hideIcons) {
                    val playerInfo = tabLine.getInfo()
                    if (playerInfo != null) {
                        minecraft.textureManager.bindTexture(playerInfo.locationSkin)
                        GlStateManager.color(1f, 1f, 1f, 1f)
                        Gui.drawScaledCustomSizeModalRect(middleX, middleY, 8f, 8f, 8, 8, 8, 8, 64.0f, 64.0f)

                        val player = tabLine.getEntity(playerInfo)
                        if (player != null && player.isWearing(EnumPlayerModelParts.HAT)) {
                            Gui.drawScaledCustomSizeModalRect(middleX, middleY, 40.0f, 8f, 8, 8, 8, 8, 64.0f, 64.0f)
                        }
                    }
                    middleX += 8 + 2
                }

                val text = if (AdvancedPlayerList.ignoreCustomTabList()) tabLine.text else tabLine.customName
                if (tabLine.type == TabStringType.TITLE) {
                    minecraft.fontRendererObj.drawStringWithShadow(
                        text,
                        middleX + column.getMaxWidth() / 2f - tabLine.getWidth() / 2f,
                        middleY.toFloat(),
                        0xFFFFFF
                    )
                } else {
                    minecraft.fontRendererObj.drawStringWithShadow(
                        text,
                        middleX.toFloat(),
                        middleY.toFloat(),
                        0xFFFFFF
                    )
                }
                middleY += lineHeight
                middleX = savedX
            }
            middleX += column.getMaxWidth() + columnSpacing
        }

        if (footer.isNotEmpty()) {
            var footerY = y + totalHeight - footer.size * lineHeight + padding / 2 + 1
            for (line in footer) {
                minecraft.fontRendererObj.drawStringWithShadow(
                    line,
                    x + totalWidth / 2f - minecraft.fontRendererObj.getStringWidth(line) / 2f,
                    footerY.toFloat(),
                    -0x1
                )
                footerY += lineHeight
            }
        }
    }

    private val fireSalePattern by RepoPattern.pattern(
        "tablist.firesaletitle",
        "§b§lFire Sales: §r§f\\([0-9]+\\)"
    )

    @SubscribeEvent
    fun hideFireFromTheTabListBecauseWhoWantsThose(event: SkipTabListLineEvent) {
        if (config.hideFiresales && event.lastSubTitle != null && fireSalePattern.matches(event.lastSubTitle.text)) {
            event.cancel()
        }
    }
}
