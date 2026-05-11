package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.pet.CurrentPetApi
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.IslandTypeTag
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object FrogPetHopTimer {

    private val config get() = SkyHanniMod.feature.foraging.frogPetHop

    private val buffDuration = 20.seconds
    private val warnThreshold get() = config.warningThreshold.seconds

    // Hop ability only exists on Epic rarity and above (;3 = Epic)
    private val epicFrog = "FROG;3".toInternalName()

    private var lastJumpTime = SimpleTimeMark.farPast()
    private var wasInAir = false
    private var hasFrogPet = false
    private var expiringSent = false
    private var expiredSent = false

    // Park + Galatea + Hub only
    private fun isInIsland() = IslandTypeTag.FORAGING.isInIsland() || IslandType.HUB.isInIsland()

    @HandleEvent(onlyOnSkyblock = true)
    fun onTick(event: SkyHanniTickEvent) {
        if (!config.enabled) return
        if (!isInIsland()) return
        if (hasFrogPet) detectJump()
        if (!event.isMod(10)) return

        hasFrogPet = CurrentPetApi.isCurrentPetOrHigherRarity(epicFrog)
        if (!hasFrogPet) return
        checkNotifications()
    }

    private fun detectJump() {
        val inAir = PlayerUtils.inAir()
        if (inAir && !wasInAir) {
            lastJumpTime = SimpleTimeMark.now()
            expiringSent = false
            expiredSent = false
        }
        wasInAir = inAir
    }

    private fun checkNotifications() {
        if (lastJumpTime.isFarPast()) return

        val remaining = buffDuration - lastJumpTime.passedSince()

        // Buff about to expire
        if (!expiringSent && !remaining.isNegative() && remaining <= warnThreshold) {
            expiringSent = true
            if (config.warningTitle) TitleManager.sendTitle("§aJump! §7(Hop buff expiring)", duration = 2.seconds)
            if (config.warningChat) ChatUtils.chat("§6Hop§7 buff expiring, §ajump now§7 to maintain your §2Foraging Fortune§7!")
            if (config.warningSound) SoundUtils.playPlingSound()
        }

        // Buff has expired
        if (!expiredSent && remaining.isNegative()) {
            expiredSent = true
            if (config.warningTitle) TitleManager.sendTitle("§cHop expired, Jump!", duration = 2.seconds)
            if (config.expiredChat) ChatUtils.chat("§6Hop§7 buff has §cexpired§7, §ajump§7 to reactivate your §2Foraging Fortune§7!")
            if (config.warningSound) SoundUtils.playPlingSound()
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onGuiRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.enabled || !hasFrogPet) return
        if (!isInIsland()) return
        config.position.renderRenderable(buildDisplay(), posLabel = "Frog Pet Hop Timer")
    }

    private fun buildDisplay(): Renderable {
        if (lastJumpTime.isFarPast()) {
            return Renderable.text { append("§6Hop§7: §8Jump to activate") }
        }
        val remaining = buffDuration - lastJumpTime.passedSince()
        return if (remaining.isNegative()) {
            Renderable.text { append("§6Hop§7: §cExpired, Jump!") }
        } else {
            val color = when {
                remaining > 10.seconds -> "§a"
                remaining > warnThreshold -> "§e"
                else -> "§c"
            }
            Renderable.text { append("§6Hop§7: $color${remaining.format()}") }
        }
    }

    @HandleEvent
    fun onWorldChange() {
        lastJumpTime = SimpleTimeMark.farPast()
        wasInAir = false
        expiringSent = false
        expiredSent = false
    }
}
