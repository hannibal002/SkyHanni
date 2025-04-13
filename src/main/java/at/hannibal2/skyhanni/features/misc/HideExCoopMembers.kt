package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RegexUtils.anyMatches
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.init.Items

@SkyHanniModule
object HideExCoopMembers {

    private val config get() = SkyHanniMod.feature.misc
    private val storage get() = ProfileStorageData.profileSpecific?.exCoopMembers

    private const val usage = "§c/shedithiddenexcoopmembers <add|remove> <name>"

    private val patternGroup = RepoPattern.group("data.exmembers")

    /**
     * REGEX-TEST: oxsss
     * REGEX-TEST: Gillsplash
     */
    private val validNamePattern by patternGroup.pattern(
        "name",
        "[a-zA-Z0-9_]{2,16}",
    )

    /**
     * REGEX-TEST: Historic Members
     */
    private val inventoryPattern by patternGroup.pattern(
        "inventory.historic",
        "Historic Members",
    )

    /**
     * REGEX-TEST: §c[§fYouTube§c] oxsss
     * REGEX-TEST: §b[MVP§f+§b] oxsss
     * REGEX-TEST: §a[VIP] oxsss
     */
    private val namePattern by patternGroup.pattern(
        "inventory.historic.name",
        "(?:§.\\[[^]]+(?:§\\++§b)?] |§7)(?<name>\\S{2,16})",
    )

    /**
     * REGEX-TEST: Farming Collections
     * REGEX-TEST: Carrot Collection
     */
    private val collectionInventoryPattern by patternGroup.pattern(
        "inventory.collections",
        ".+ Collections?",
    )

    /**
     * REGEX-TEST: §b[MVP§f+§b] oxsss§7: §e1.9M
     * REGEX-TEST: §a[VIP] oxsss§7: §e0
     * REGEX-TEST: §7oxsss§7: §e0
     */
    private val collectedPattern by patternGroup.pattern(
        "inventory.collections.collected",
        "(?:§.\\[[^]]+(?:§\\++§b)?] |§7)(?<name>[^§]{2,16})§7: §e(?<amount>.+)",
    )

    /**
     * REGEX-TEST: §7Progress to Raw Chicken IX: §e25.7§6%
     * REGEX-TEST: §7Total Collected: §e1,917,287
     */
    private val dontDisplayMaxedPattern by patternGroup.pattern(
        "inventory.collections.displaymaxed",
        "§7(?:Progress to .+|Total Collected: .+)",
    )

    @HandleEvent
    fun onTooltip(event: ToolTipEvent) {
        if (!config.hideExCoopMembers || !collectionInventoryPattern.matches(InventoryUtils.openInventoryName())) return
        val storage = storage ?: return
        if (storage.isEmpty()) return

        event.toolTip = event.toolTipRemovedPrefix().handleTooltip(storage)
    }

    private fun List<String>.handleTooltip(storage: MutableSet<String>): MutableList<String> = this.toMutableList().apply {
        val coopIndex = indexOf("§7Co-op Contributions:")
        if (coopIndex == -1) return@apply

        var remainingPlayers = 0
        var totalCollected = 0
        val itemsToRemove = mutableListOf<Int>()

        for (i in (coopIndex + 1) until size) {
            if (this[i].isBlank()) break

            collectedPattern.matchMatcher(this[i]) {
                if (group("name") in storage) {
                    itemsToRemove.add(i)
                } else {
                    remainingPlayers++
                }

                totalCollected += group("amount").formatInt()
            }
        }

        itemsToRemove.sortedDescending().forEach { removeAt(it) }

        if (remainingPlayers <= 1) {
            if (!dontDisplayMaxedPattern.anyMatches(this)) {
                if (coopIndex + 1 < size) this[coopIndex + 1] = "§7Total collected: §e${totalCollected.addSeparators()}"
                if (coopIndex < size) this[coopIndex] = "§a§lCOLLECTION MAXED OUT!"
            } else {
                if (coopIndex + 1 < size) removeAt(coopIndex + 1)
                if (coopIndex < size) removeAt(coopIndex)
                if (coopIndex - 1 < size) removeAt(coopIndex - 1)
            }
        }
    }

    @HandleEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!config.hideExCoopMembers || !inventoryPattern.matches(event.inventoryName)) return

        event.inventoryItems.values
            .filter { it.item == Items.skull }
            .forEach { item ->
                namePattern.matchMatcher(item.displayName) {
                    addExMember(group("name"))
                }
            }
    }

    private fun editExCoopMembers(args: Array<String>) {
        if (args.isEmpty()) return ChatUtils.userError(usage)

        val action = args.firstOrNull()?.takeIf { it in setOf("add", "remove") }
            ?: return ChatUtils.userError(usage)

        val name = args.getOrNull(1)?.takeIf { validNamePattern.matches(it) } ?: run {
            return ChatUtils.userError("Invalid username! Did you enter it correctly?")
        }

        val new = when (action) {
            "add" -> addExMember(name)
            "remove" -> removeExMember(name)
            else -> return ChatUtils.userError(usage)
        }

        if (new.isEmpty()) return ChatUtils.userError(
            when (action) {
                "add" -> "That username is already in the list!"
                "remove" -> "That username wasn't in the list!"
                else -> ""
            },
        )

        ChatUtils.hoverableChat(
            "${action.successString()} $name (Hover to see current list).",
            new,
        )
    }

    private fun String.successString(): String = when (this) {
        "add" -> "Added"
        "remove" -> "Removed"
        else -> ""
    }

    private fun addExMember(name: String): List<String> {
        val storage = storage ?: return listOf()
        if (!storage.add(name)) return listOf()
        return storage.toList()
    }

    private fun removeExMember(name: String): List<String> {
        val storage = storage ?: return listOf()
        if (!storage.remove(name)) return listOf()
        return storage.toList()
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shedithiddenexcoopmembers") {
            description = "Manually edit the list of ex co-op members you want to hide."
            category = CommandCategory.USERS_ACTIVE
            callback { editExCoopMembers(it) }
        }
    }
}
