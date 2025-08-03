package at.hannibal2.skyhanni.features.event.anniversary

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable.Companion.item
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

// @SkyHanniModule
@Suppress("SKyHanniModuleInspection", "unused")
object Year300RaffleEvent {

    val ORANGE_CAKE = "EPOCH_CAKE_ORANGE".toInternalName()

    private val config get() = SkyHanniMod.feature.event.century
    private val displayItem by lazy { ORANGE_CAKE.getItemStackOrNull() ?: ItemStack(Items.clock) }

    private var lastTimerReceived = SimpleTimeMark.farPast()
    private var lastTimeAlerted = SimpleTimeMark.farPast()

    private var overlay: Renderable? = null

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent) {
        if (event.message == "§6§lACTIVE PLAYER! §eYou gained §b+1 Raffle Ticket§e!") {
            lastTimerReceived = SimpleTimeMark.now()
        }
    }

    fun isEnabled() = SkyBlockUtils.inSkyBlock && config.enableActiveTimer && SkyBlockTime(301).toTimeMark().isInPast()

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        config.activeTimerPosition.renderRenderable(
            overlay ?: return,
            posLabel = "300þ Anniversary Active Timer",
        )
    }

    @HandleEvent
    fun onTick() {
        if (!isEnabled()) {
            overlay = null
            return
        }
        val p = lastTimerReceived.passedSince()
        val timeLeft = if (p > 20.minutes) {
            0.seconds
        } else {
            20.minutes - p
        }
        if (p.isFinite() && timeLeft < 1.seconds && lastTimeAlerted.passedSince() > 5.minutes && config.enableActiveAlert) {
            SoundUtils.centuryActiveTimerAlert.playSound()
            lastTimeAlerted = SimpleTimeMark.now()
        }
        overlay = Renderable.horizontal(
            Renderable.item(displayItem),
            Renderable.text("§eTime Left: ${timeLeft.format()}"),
        )
    }
}
