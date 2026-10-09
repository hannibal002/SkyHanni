package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.garden.GardenConfig
import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConfigUtils
import at.hannibal2.skyhanni.utils.ItemBlink
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object GardenBurrowingSporesNotifier {

    private val config get() = GardenApi.config
    private val patternGroup = RepoPattern.group("garden.burrowingspores")

    /**
     * REGEX-TEST: VERY RARE CROP! Burrowing Spores
     */
    private val sporeDropMessage by patternGroup.pattern(
        "drop.colorless",
        "VERY RARE CROP! Burrowing Spores",
    )
    private val BURROWING_SPORES = "BURROWING_SPORES".toInternalName()

    private val titleSet = setOf(GardenConfig.BurrowingSporesNotificationType.TITLE, GardenConfig.BurrowingSporesNotificationType.BOTH)
    private val blinkSet = setOf(GardenConfig.BurrowingSporesNotificationType.BLINK, GardenConfig.BurrowingSporesNotificationType.BOTH)

    @HandleEvent(onlyOnIsland = GARDEN)
    private fun onSystemMessage(event: SystemMessageEvent.Allow) {
        val selected = config.burrowingSporesNotificationType
        val titleEnabled = selected in titleSet
        val blinkEnabled = selected in blinkSet
        if (!titleEnabled && !blinkEnabled) return
        if (!sporeDropMessage.matches(event.cleanMessage)) return

        if (titleEnabled) TitleManager.sendTitle("§9Burrowing Spores!")
        if (blinkEnabled) ItemBlink.setBlink(BURROWING_SPORES.getItemStackOrNull(), 5_000)
    }

    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(99, "garden.burrowingSporesNotification", "garden.burrowingSporesNotificationType") {
            ConfigUtils.migrateBooleanToEnum(
                it, GardenConfig.BurrowingSporesNotificationType.BOTH, GardenConfig.BurrowingSporesNotificationType.NONE,
            )
        }
    }
}
