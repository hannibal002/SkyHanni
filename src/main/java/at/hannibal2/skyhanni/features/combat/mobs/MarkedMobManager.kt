package at.hannibal2.skyhanni.features.combat.mobs

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.data.mob.MobData
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils

@SkyHanniModule
object MarkedMobManager {

    private val config get() = SkyHanniMod.feature.combat.mobs.markedMobs
    private val entries get() = config.markedMobs
    private val allMobs get() = HashSet(MobData.skyblockMobs)
    private val markedMobs get() = allMobs.asSequence().filter { isMarked(it) }

    @HandleEvent
    private fun onMobSpawn(event: MobEvent.Spawn.SkyblockMob) {
        if (!isEnabled()) return
        if (isMarked(event.mob)) {
            event.mob.highlight(config.highlightColor.get())
        }
    }

    private fun isMarked(
        mob: Mob,
        entriesToCheck: Collection<MarkedMob> = entries,
    ): Boolean {
        return entriesToCheck.any {
            it.matches(
                name = mob.name,
                level = mob.levelOrTier,
            )
        }
    }

    fun toggle(markedVariant: MarkedMob) {
        val removedEntry = entries.firstOrNull {
            it.matches(markedVariant)
        }

        if (removedEntry != null) {
            entries.remove(removedEntry)
            val removedList = listOf(removedEntry)

            for (mob in allMobs) {
                if (isMarked(mob, removedList) &&
                    !isMarked(mob)
                ) {
                    mob.removeHighlight()
                }
            }
        } else {
            entries.add(markedVariant)

            for (mob in markedMobs) {
                mob.highlight(config.highlightColor.get())
            }
        }
    }

    fun forceApplyRules(glow: Boolean) {
        if (glow) {
            for (mob in allMobs) {
                mob.highlight(config.highlightColor.get())
            }
        } else {
            markedMobs.forEach { it.removeHighlight() }
        }
    }

    @HandleEvent
    private fun onCommand(event: CommandRegistrationEvent) {
        event.registerBrigadier("shtestmarkedmob") {
            description = "Test marked mob highlighting"
            category = CommandCategory.DEVELOPER_DEBUG
            literalCallback("get") {
                val all = entries.joinToString(", ")
                ChatUtils.chat("all: $all")
            }
            literal("clear") {
                literalCallback("all") {
                    forceApplyRules(glow = false)
                    entries.clear()
                }
            }
            literalCallback("apply") {
                forceApplyRules(glow = true)
            }
        }
    }

    private fun isEnabled() =
        config.enabled.get() && entries.isNotEmpty()
}
