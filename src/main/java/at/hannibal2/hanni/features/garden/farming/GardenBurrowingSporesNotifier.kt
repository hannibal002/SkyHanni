package at.hannibal2.hanni.features.garden.farming

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.config.features.garden.GardenConfig
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.data.title.TitleManager
import at.hannibal2.hanni.events.chat.HanniChatEvent
import at.hannibal2.hanni.features.garden.GardenApi
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ConfigUtils
import at.hannibal2.hanni.utils.ItemBlink
import at.hannibal2.hanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.hanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.hanni.utils.RegexUtils.matches
import at.hannibal2.hanni.utils.repopatterns.RepoPattern

@HanniModule
object GardenBurrowingSporesNotifier {

    private val config get() = GardenApi.config
    private val patternGroup = RepoPattern.group("garden.burrowingspores")
    private val sporeDropMessage by patternGroup.pattern(
        "drop",
        "§6§lVERY RARE CROP! §r§f§r§9Burrowing Spores\\.",
    )
    private val BURROWING_SPORES = "BURROWING_SPORES".toInternalName()

    private val titleSet = setOf(GardenConfig.BurrowingSporesNotificationType.TITLE, GardenConfig.BurrowingSporesNotificationType.BOTH)
    private val blinkSet = setOf(GardenConfig.BurrowingSporesNotificationType.BLINK, GardenConfig.BurrowingSporesNotificationType.BOTH)

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onChat(event: HanniChatEvent) {
        val selected = config.burrowingSporesNotificationType
        val titleEnabled = selected in titleSet
        val blinkEnabled = selected in blinkSet
        if (!titleEnabled && !blinkEnabled) return
        if (!sporeDropMessage.matches(event.message)) return

        if (titleEnabled) TitleManager.sendTitle("§9Burrowing Spores!")
        if (blinkEnabled) ItemBlink.setBlink(BURROWING_SPORES.getItemStackOrNull(), 5_000)
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(99, "garden.burrowingSporesNotification", "garden.burrowingSporesNotificationType") {
            ConfigUtils.migrateBooleanToEnum(
                it, GardenConfig.BurrowingSporesNotificationType.BOTH, GardenConfig.BurrowingSporesNotificationType.NONE,
            )
        }
    }
}
