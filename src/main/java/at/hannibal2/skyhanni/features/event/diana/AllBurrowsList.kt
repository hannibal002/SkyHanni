package at.hannibal2.hanni.features.event.diana

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.commands.CommandCategory
import at.hannibal2.hanni.config.commands.CommandRegistrationEvent
import at.hannibal2.hanni.events.SecondPassedEvent
import at.hannibal2.hanni.events.diana.BurrowDetectEvent
import at.hannibal2.hanni.events.minecraft.HanniRenderWorldEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.hanni.utils.LorenzColor
import at.hannibal2.hanni.utils.LorenzVec
import at.hannibal2.hanni.utils.OSUtils
import at.hannibal2.hanni.utils.collection.CollectionUtils.editCopy
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawColor

@HanniModule
object AllBurrowsList {
    private var list = listOf<LorenzVec>()
    private val config get() = HanniMod.feature.event.diana.allBurrowsList
    private var burrowLocations
        get() = HanniMod.feature.storage.foundDianaBurrowLocations
        set(value) {
            HanniMod.feature.storage.foundDianaBurrowLocations = value
        }

    @HandleEvent
    fun onBurrowDetect(event: BurrowDetectEvent) {
        if (!isEnabled()) return
        burrowLocations = burrowLocations.editCopy {
            add(event.burrowLocation)
        }
    }

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return

        val range = 5..70
        list = burrowLocations.asSequence().map { it to it.distanceToPlayer() }
            .filter { it.second.toInt() in range }
            .sortedBy { it.second }
            .map { it.first }
            .take(25).toList()
    }

    private fun copyToClipboard() {
        val list = burrowLocations.map { it.printWithAccuracy(0, ":") }
        OSUtils.copyToClipboard(list.joinToString(";"))
        ChatUtils.chat("Saved all ${list.size} burrow locations to clipboard.")
    }

    private fun addFromClipboard() {
        HanniMod.launchIOCoroutine("diana burrows all addFromClipboard") {
            val text = OSUtils.readFromClipboard() ?: return@launchIOCoroutine

            var new = 0
            var duplicate = 0
            val newEntries = mutableListOf<LorenzVec>()
            for (raw in text.split(";")) {
                val location = LorenzVec.decodeFromString(raw)
                if (location !in burrowLocations) {
                    newEntries.add(location)
                    new++
                } else {
                    duplicate++
                }
            }
            burrowLocations = burrowLocations.editCopy {
                addAll(newEntries)
            }

            ChatUtils.chat("Added $new new burrow locations, $duplicate are duplicate.")
        }
    }

    @HandleEvent
    fun onRenderWorld(event: HanniRenderWorldEvent) {
        if (!isEnabled()) return
        if (!config.showAll) return

        for (location in list) {
            event.drawColor(location, LorenzColor.DARK_AQUA.toChromaColor())
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shaddfoundburrowlocationsfromclipboard") {
            description = "Add all ever found burrow locations from clipboard"
            category = CommandCategory.DEVELOPER_TEST
            callback { addFromClipboard() }
        }
        event.register("shcopyfoundburrowlocations") {
            description = "Copy all ever found burrow locations to clipboard"
            category = CommandCategory.DEVELOPER_DEBUG
            callback { copyToClipboard() }
        }
    }

    fun isEnabled() = DianaApi.isDoingDiana() && config.save
}
