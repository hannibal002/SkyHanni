package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ClipboardUtils
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.RenderUtils.drawFilledBoundingBox
import at.hannibal2.skyhanni.utils.RenderUtils.drawWireframeBoundingBox
import at.hannibal2.skyhanni.utils.RenderUtils.expandBlock
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getItemId
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import java.awt.Color

@SkyHanniModule
object WorldEdit {

    private var leftPos = null as BlockPos?
    private var rightPos = null as BlockPos?

    private fun funAABB(left: BlockPos, right: BlockPos) = AxisAlignedBB(
        minOf(left.x, left.x + 1, right.x, right.x + 1).toDouble(),
        minOf(left.y, left.y + 1, right.y, right.y + 1).toDouble(),
        minOf(left.z, left.z + 1, right.z, right.z + 1).toDouble(),
        maxOf(left.x, left.x + 1, right.x, right.x + 1).toDouble(),
        maxOf(left.y, left.y + 1, right.y, right.y + 1).toDouble(),
        maxOf(left.z, left.z + 1, right.z, right.z + 1).toDouble(),
    )

    private val aabb
        get() = leftPos?.let { l ->
            rightPos?.let { r ->
                funAABB(l, r)
            }
        }

    fun copyToClipboard() {
        ClipboardUtils.copyToClipboard(generateCodeSnippet())
    }

    private fun generateCodeSnippet(): String {
        var text = ""
        leftPos?.run { text += "val redLeft = net.minecraft.util.BlockPos($x, $y, $z)\n" }
        rightPos?.run { text += "val blueRight = net.minecraft.util.BlockPos($x, $y, $z)\n" }
        aabb?.run { text += "val aabb = net.minecraft.util.AxisAlignedBB($minX, $minY, $minZ, $maxX, $maxY, $maxZ)\n" }
        return text
    }

    @HandleEvent
    fun onBlockClick(event: BlockClickEvent) {
        if (!isEnabled()) return
        if (event.itemInHand?.getItemId() != "WOOD_AXE") return

        if (event.clickType == ClickType.LEFT_CLICK) {
            leftPos = event.position.toBlockPos()
        } else if (event.clickType == ClickType.RIGHT_CLICK) {
            rightPos = event.position.toBlockPos()
        }
    }

    @HandleEvent
    fun onWorldChange() {
        leftPos = null
        rightPos = null
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return

        leftPos?.let { l ->
            event.drawWireframeBoundingBox(
                funAABB(l, l).expandBlock(),
                Color.RED,
            )
        }
        rightPos?.let { r ->
            event.drawWireframeBoundingBox(
                funAABB(r, r).expandBlock(),
                Color.BLUE,
            )
        }
        aabb?.let {
            event.drawFilledBoundingBox(
                it.expandBlock(),
                Color.CYAN.addAlpha(60),
            )
        }
    }

    fun command(it: Array<String>) {
        if (!isEnabled()) {
            ChatUtils.userError("World Edit is disabled in the config. Enable it if you want to use it.")
            return
        }
        when (it.firstOrNull()) {
            null, "help" -> {
                ChatUtils.chat(
                    "Use a wood axe and left/right click to select a region in the world. " +
                        "Then use /shworldedit copy or /shworldedit reset.",
                )
            }

            "copy" -> {
                copyToClipboard()
                ChatUtils.chat("Copied text to clipboard.")
            }

            "reset" -> {
                leftPos = null
                rightPos = null
                ChatUtils.chat("Reset selected region")
            }

            "left", "pos1" -> {
                leftPos = LocationUtils.playerLocation().toBlockPos()
                ChatUtils.chat("Set left pos.")
            }

            "right", "pos2" -> {
                rightPos = LocationUtils.playerLocation().toBlockPos()
                ChatUtils.chat("Set right pos.")
            }

            else -> {
                ChatUtils.chat("Unknown subcommand")
            }
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shworldedit") {
            description = "Select regions in the world"
            category = CommandCategory.DEVELOPER_DEBUG
            callback { command(it) }
            autoComplete { listOf("copy", "reset", "help", "left", "right") }
        }
    }

    fun isEnabled() = SkyHanniMod.feature.dev.worldEdit
}
