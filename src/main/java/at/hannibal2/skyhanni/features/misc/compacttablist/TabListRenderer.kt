package at.hannibal2.skyhanni.features.misc.compacttablist

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.mixins.transformers.AccessorGuiPlayerTabOverlay
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
        if (event.type != RenderGameOverlayEvent.ElementType.PLAYER_LIST) return
        if (!config.enabled) return
        if (TabListReader.renderColumns.isEmpty()) return
        event.isCanceled = true

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
        for (column in columns) {
            var middleY = if (config.hideAdverts) headerY else headerY + padding + 2

            Gui.drawRect(
                middleX - padding + 1,
                middleY - padding + 1,
                middleX + column.getMaxWidth() + padding - 2,
                middleY + column.size() * lineHeight + padding - 2,
                0x20AAAAAA
            )

            for (tabLine in column.lines) {
                val savedX = middleX

                if (tabLine.type == TabStringType.PLAYER) {
                    val pLayerInfo = minecraft.netHandler.getPlayerInfo(TabStringType.usernameFromLine(tabLine.text))
                    if (pLayerInfo != null) {
                        val player = minecraft.theWorld.getPlayerEntityByUUID(pLayerInfo.gameProfile.id)

                        minecraft.textureManager.bindTexture(pLayerInfo.locationSkin)
                        GlStateManager.color(1f, 1f, 1f, 1f)
                        Gui.drawScaledCustomSizeModalRect(middleX, middleY, 8f, 8f, 8, 8, 8, 8, 64.0f, 64.0f)
                        if (player != null && player.isWearing(EnumPlayerModelParts.HAT)) {
                            Gui.drawScaledCustomSizeModalRect(middleX, middleY, 40.0f, 8f, 8, 8, 8, 8, 64.0f, 64.0f)
                        }
                    }
                    middleX += 8 + 2
                }

                if (tabLine.type == TabStringType.TITLE) {
                    minecraft.fontRendererObj.drawStringWithShadow(
                        tabLine.text,
                        middleX + column.getMaxWidth() / 2f - tabLine.getWidth() / 2f,
                        middleY.toFloat(),
                        0xFFFFFF
                    )
                } else {
                    minecraft.fontRendererObj.drawStringWithShadow(
                        tabLine.text,
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
}