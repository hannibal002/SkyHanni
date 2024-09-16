package at.hannibal2.skyhanni.features.combat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ActionBarStatsData
import at.hannibal2.skyhanni.events.ActionBarUpdateEvent
import at.hannibal2.skyhanni.events.ActionBarValueUpdateEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.SkillExpGainEvent
import at.hannibal2.skyhanni.features.skillprogress.SkillType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.RegexUtils.findMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.TimeUnit
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import io.github.moulberry.notenoughupdates.miscgui.InventoryStorageSelector
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.event.entity.player.PlayerUseItemEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import tv.twitch.chat.Chat
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

@SkyHanniModule
object ArmorStackDisplay {
    private val config get() = SkyHanniMod.feature.combat.stackDisplayConfig
    private var stackCount = 0
    private var stackSymbol = ""
    private var armorPieceCount = 0

    private var stackDecayTime = Duration.ZERO

    private var stackDecayTimeCurrent: SimpleTimeMark = SimpleTimeMark.farPast()

    @SubscribeEvent
    fun onActionBar(event: ActionBarUpdateEvent) {
        if (!isEnabled()) return
        var actionBarText = event.actionBar
        val stackPattern = ActionBarStatsData.ARMOR_STACK.pattern

        stackSymbol = stackPattern.findMatcher(actionBarText) { group("symbol") } ?: ""
        stackCount = (stackPattern.findMatcher(actionBarText) { group("stack") } ?: "0").toInt()

        val updatedActionBarText = actionBarText.replace(Regex("\\$stackPattern?"), "").trim()
        event.changeActionBar(updatedActionBarText)
    }


    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled() || stackCount == 0) return

        val stackCountDisplay = "§6§l${stackCount}${stackSymbol}"
        config.position.renderStrings(listOf(stackCountDisplay, stackDecayTimeCurrent.timeUntil().toString()), posLabel = "Armor Stack Display")
//         config.position.renderStrings(listOf(stackCountDisplay, stackDecayTimeCurrent.timeUntil().toString(DurationUnit.SECONDS, 2),), posLabel = "Armor Stack Display")
    }

    @SubscribeEvent
    fun onSecond(event: SecondPassedEvent) {
        armorPieceCount = InventoryUtils.getArmor()
            .count { armor -> armor != null && armor.displayName.contains(getName(stackSymbol)) }

        stackDecayTime = when (armorPieceCount) {
            2 -> 4000
            3 -> 7000
            4 -> 10000
            else -> 0
        }.milliseconds
    }

    private val symbolMap = mapOf(
        "ᝐ" to "Crimson",
        "⁑" to "Terror",
        "Ѫ" to "Aurora",
        "҉" to "Fervor",
        "⚶" to "Hollow"
    )

    fun getName(symbol: String): String {
        return symbolMap[symbol] ?: ""
    }

    @SubscribeEvent
    fun onSoundPlay(event: PlaySoundEvent) {
        if (event.soundName != "tile.piston.out" && event.soundName != "tile.piston.in" ) return
        ChatUtils.chat(String.format("%s %s %.2f", event.soundName, event.volume, event.distanceToPlayer))
        stackDecayTimeCurrent = SimpleTimeMark.now() + stackDecayTime
    }


//     fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
    fun isEnabled() = config.enabled
}
