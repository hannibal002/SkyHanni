package at.hannibal2.hanni.shader

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.data.NotificationManager
import at.hannibal2.hanni.data.HanniNotification
import at.hannibal2.hanni.events.IslandChangeEvent
import at.hannibal2.hanni.features.misc.ContributorManager
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.InventoryDetector
import at.hannibal2.hanni.utils.OSUtils
import at.hannibal2.hanni.utils.PlayerUtils
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.TimeUtils
import at.hannibal2.hanni.utils.system.PlatformUtils
import kotlin.random.Random
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * This class handles april jokes, in case people still play minecraft on april 1st.
 * Q: Why is this file in the shader package?
 * A: Why are you looking in the shader package right now? Go look at config or features package and be productive :P
 */
@Suppress("SpellCheckingInspection")
@HanniModule
object RobloxReminder {

    // Using now instead of farPast, so the message first shows up after 3 hours
    private var lastVersionReminder = SimpleTimeMark.now()
    private var lastInventoryError = SimpleTimeMark.farPast()
    private var lastSpecialPerson = SimpleTimeMark.farPast()

    init {
        InventoryDetector(
            onOpenInventory = { robuxError() },
            checkInventoryName = { it == "SkyBlock Menu" },
        )
    }

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        if (event.newIsland == IslandType.NONE) return
        if (!TimeUtils.isAprilFoolsDay) return

        versionReminder()
        specialPerson()
    }

    private fun specialPerson() {
        if (lastSpecialPerson.passedSince() < 24.hours) return
        // why not share love for every 1 in 200?
        val chance = if (isContributor()) 0.0051 else 0.005
        if (Random.nextDouble() >= chance) return

        lastSpecialPerson = SimpleTimeMark.now()
        ChatUtils.clickableChat(
            "If you're reading this, then you're §cspecial §d<3", prefix = false,
            onClick = {
                ChatUtils.chat("Not convinced yet? How about opening YouTube and playing your favorite song :)")
            },
            hover = "No really, this message only shows up for 1 in 200 of users.",
        )
    }

    private fun versionReminder() {
        if (lastVersionReminder.passedSince() < 3.hours) return

        // only 10% chance of happening on world switch
        val chance = if (isContributor()) 0.90 else 0.10
        if (Random.nextDouble() >= chance) return

        lastVersionReminder = SimpleTimeMark.now()

        val notification = HanniNotification(
            listOf(
                "§cHanni is no longer receiving updates for Minecraft.",
                "§cPlaying on a discontinued game is not recommended and may lead to issues.",
                "§cPlease update to Roblox.",
                "§fSadly, most, if not all, of your Minecraft mods might not work on Roblox.",
            ),
            5.minutes,
        )

        NotificationManager.queueNotification(notification)
    }

    // Tech-savvy ppl who have debug enabled should have fun as well :)
    private fun isContributor() = PlayerUtils.getName() in ContributorManager.contributorNames || SkyBlockUtils.debug

    private fun robuxError() {
        if (!TimeUtils.isAprilFoolsDay) return
        // why only contributors? Because we love a calm support channel :/
        if (!isContributor()) return
        if (lastInventoryError.passedSince() < 3.hours) return

        val chance = 0.50
        if (Random.nextDouble() >= chance) return

        lastInventoryError = SimpleTimeMark.now()

        // Lovingly taken from ErrorManager.kt
        val shVersion = HanniMod.VERSION
        val mcVersion = PlatformUtils.MC_VERSION
        val label = "Hanni $shVersion $mcVersion"
        val finalMessage = "Error while parsing Robux from SkyBlock Menu."

        ChatUtils.clickableChat(
            "§c[$label]: $finalMessage Click here to copy the error into the clipboard.",
            onClick = {
                val lru = "QcXgW9w4wQd=v?hctaw/moc.ebutuoy.www//:sptth".reversed()
                ChatUtils.chat("Error copied into the clipboard, please report it on the Hanni Discord!")
                OSUtils.copyToClipboard("I got rick rolled. For proof, click on $lru")
                OSUtils.openBrowser(lru)
            },
            hover = "§eClick to copy!",
            prefix = false,
        )
    }
}
