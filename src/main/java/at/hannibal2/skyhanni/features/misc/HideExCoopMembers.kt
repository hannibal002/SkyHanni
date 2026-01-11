package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.CollectionApi
import at.hannibal2.skyhanni.api.CollectionApi.getCorrectedName
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RegexUtils.anyMatches
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.cleanPlayerName
import at.hannibal2.skyhanni.utils.StringUtils.isPlayerName
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

@SkyHanniModule
object HideExCoopMembers {

    private val config get() = SkyHanniMod.feature.misc
    private val storage get() = ProfileStorageData.profileSpecific

    private val historicMembersInventory = InventoryDetector { name -> inventoryPattern.matches(name) }

    private var changedSlotNumber: Int? = null

    private val patternGroup = RepoPattern.group("data.hiddenmembers")

    /**
     * REGEX-TEST: Historic Members
     */
    private val inventoryPattern by patternGroup.pattern(
        "inventory.historic",
        "Historic Members",
    )

    @HandleEvent
    fun onTooltip(event: ToolTipEvent) {
        if (!config.hideExCoopMembers || !CollectionApi.collectionInventory.isInside()) return
        val hiddenMembers = storage?.hiddenCoopMembers.takeIf { !it.isNullOrEmpty() } ?: return

        event.toolTip = event.toolTipRemovedPrefix().handleTooltip(hiddenMembers, event.itemStack)
        changedSlotNumber = event.slot.index
    }

    private fun List<String>.handleTooltip(storage: MutableSet<String>, item: ItemStack): MutableList<String> = this.toMutableList().apply {
        val coopIndex = indexOf("§7Co-op Contributions:")
        if (coopIndex == -1) return@apply

        val internalName = item.getInternalName().getCorrectedName()
        val totalCollected = CollectionApi.getCollectionCounter(internalName) ?: 0L

        var remainingPlayers = 0
        val linesToRemove = mutableListOf<Int>()

        drop(coopIndex).forEachIndexed { index, line ->
            if (line.isBlank()) return@forEachIndexed

            CollectionApi.playerCounterPattern.matchMatcher(line) {
                if (group("name") in storage) {
                    linesToRemove.add(coopIndex + index)
                } else {
                    remainingPlayers++
                }
            }
        }

        linesToRemove.sortedDescending().forEach { removeAt(it) }

        if (remainingPlayers >= 2) return this

        val notMaxed = CollectionApi.collectionNotMaxedPattern.anyMatches(this)

        if (!notMaxed) {
            if (coopIndex + 1 < size) this[coopIndex + 1] = "§7Total collected: §e${totalCollected.addSeparators()}"
            if (coopIndex < size) this[coopIndex] = "§a§lCOLLECTION MAXED OUT!"
        } else {
            for (i in (coopIndex + 1) downTo (coopIndex - 1)) {
                if (i < size) removeAt(i)
            }
        }
    }

    @HandleEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!config.hideExCoopMembers || !historicMembersInventory.isInside()) return

        event.inventoryItems.values
            .filter { it.item == Items.PLAYER_HEAD }
            .forEach { item ->
                addHiddenMember(item.hoverName.formattedTextCompatLeadingWhiteLessResets().cleanPlayerName())
            }
    }

    private const val usage = "§c/shedithiddencoopmembers <add|remove> <name>"
    private fun sendUsage() = ChatUtils.userError(usage)

    private fun addHiddenMember(name: String): List<String>? {
        val exMembers = storage?.hiddenCoopMembers ?: return null
        if (!exMembers.add(name)) return null
        return exMembers.toList()
    }

    private fun removeHiddenMember(name: String): List<String>? {
        val exMembers = storage?.hiddenCoopMembers ?: return null
        if (!exMembers.remove(name)) return null
        return exMembers.toList()
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shedithiddencoopmembers") {
            description = "Manually edit the list of ex co-op members you want to hide."
            category = CommandCategory.USERS_ACTIVE
            literal("add") {
                argCallback("name", BrigadierArguments.string()) { name ->
                    if (!name.isPlayerName()) {
                        ChatUtils.userError("Invalid username! Did you enter it correctly?")
                        return@argCallback
                    }
                    val new = addHiddenMember(name)
                    if (new == null) {
                        ChatUtils.userError("That username is already in the list!")
                        return@argCallback
                    }
                    ChatUtils.hoverableChat("Added $name (Hover to see current list).", hover = new)
                }
            }
            literal("remove") {
                argCallback("name", BrigadierArguments.string()) { name ->
                    if (!name.isPlayerName()) {
                        ChatUtils.userError("Invalid username! Did you enter it correctly?")
                        return@argCallback
                    }
                    val new = removeHiddenMember(name)
                    if (new == null) {
                        ChatUtils.userError("That username wasn't in the list!")
                        return@argCallback
                    }
                    ChatUtils.hoverableChat("Removed $name (Hover to see current list).", hover = new)
                }
            }
            simpleCallback { sendUsage() }
        }
    }
}
