package at.hannibal2.skyhanni.features.event.anniversary

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.RenderUtils.renderSingleLineWithItems
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import at.hannibal2.skyhanni.utils.TimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.renderables.Renderable
import io.github.moulberry.notenoughupdates.util.SkyBlockTime
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.time.Instant
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object Year300RaffleEvent {
    private val config get() = SkyHanniMod.feature.event.century
    val displayItem by lazy { NEUItems.getItemStackOrNull("EPOCH_CAKE_ORANGE") ?: ItemStack(Items.clock) }

    private var lastTimerReceived = TimeMark.never()
    private var lastTimeAlerted = TimeMark.never()

    private var overlay: List<Any>? = null

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (event.message == "§6§lACTIVE PLAYER! §eYou gained §b+1 Raffle Ticket§e!") {
            lastTimerReceived = TimeMark.now()
        }
    }

    fun isEnabled() = config.enableActiveTimer &&
            Instant.now().isBefore(SkyBlockTime(301).toInstant()) &&
            LorenzUtils.inSkyBlock


    @SubscribeEvent
    fun onRender(event: GuiRenderEvent.GameOverlayRenderEvent) {
        config.activeTimerPosition.renderSingleLineWithItems(
            overlay ?: return,
            posLabel = "300þ Anniversary Active Timer"
        )
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) {
            overlay = null
            return
        }
        val p = lastTimerReceived.passedTime()
        val timeLeft = if (p > 20.minutes) {
            0.seconds
        } else {
            20.minutes - p
        }
        if (p.isFinite() && timeLeft < 1.seconds && lastTimeAlerted.passedTime() > 5.minutes && config.enableActiveAlert) {
            SoundUtils.centuryActiveTimerAlert.playSound()
            lastTimeAlerted = TimeMark.now()
        }
        overlay = listOf(
            Renderable.itemStack(displayItem),
            Renderable.string("§eTime Left: ${timeLeft.format()}")
        )
    }


}