package at.hannibal2.skyhanni.events.render.gui

import at.hannibal2.skyhanni.api.event.RenderingSkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import net.minecraft.client.gui.GuiGraphicsExtractor

/**
 * Fired twice per rendered frame during GUI extraction.
 *
 * Fired on the main client thread via a Mixin into `GameRenderer`. Only fired when a local player is present.
 *
 * Use [startPhase] to distinguish the two firing points within a frame:
 * - `true`: fired before the GUI render state is extracted (start of the frame)
 * - `false`: fired before the saving indicator is rendered (end of the frame)
 *
 * The [GuiGraphicsExtractor] is accessible via the `context` property inherited from `RenderingSkyHanniEvent`.
 *
 * @param context the graphics context for the current frame
 * @param startPhase true at the start of the frame, false at the end
 */
@PrimaryFunction("onRenderingTick")
class RenderingTickEvent(
    context: GuiGraphicsExtractor,
    val startPhase: Boolean,
) : RenderingSkyHanniEvent(context)
