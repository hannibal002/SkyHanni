package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

/**
 * TODO for dev rework
 *
 * move all usages of SkyHanniMod.feature.dev into the at.hannibal2.skyhanni.test package
 *
 * all config options under dev category should only be used in the test package.
 *
 * restructure all config toggles under dev into 4 clear categories:
 * 	- "main debug toggles" (stuff that disable whole backend logics, e.g. mob detection, damage indicator, island navigation)
 * 	- "toggle to show more info" (e.g. , show info x in item stacks, like internal names, item rarity, etc)
 * 	- "toggle to simulate/change behaviour" (e.g. repo auto update, always april fools, always hoppity, etc)
 * 	- "use tools/test stuff" (e.g. graph editor, debug mob, "enable debug" main boolean for test logic)
 *
 * 	fix all todo entries under package at.hannibal2.skyhanni.test
 *
 * rename test to dev or debug (unclear)
 */

@SkyHanniModule
object DevApi {

    val config get() = SkyHanniMod.feature.dev

    val mainToggles get() = config.mainToggles

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(127, "dev.damageIndicatorBackend", "dev.mainToggles.damageIndicator")
        event.move(127, "dev.mobDebug.enable", "dev.mainToggles.mobDetection")
        event.move(127, "dev.pingApi", "dev.mainToggles.pingApi")
    }
}
