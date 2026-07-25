package at.hannibal2.skyhanni.features.chat.filter

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.chat.filter.ChatFilterManager.block
import at.hannibal2.skyhanni.features.chat.filter.PowderMiningChatFilter.genericMiningRewardMessage
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrEmpty
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent

@SkyHanniModule
object ChatFilterManager {

    // DO NOT MAKE THIS PUBLIC
    // To avoid circular dependencies (The groups need chatFilterGroup, and ChatFilterManager needs the groups)
    private val config get() = SkyHanniMod.feature.chat.filterType
    private val activeFilters = mutableSetOf<ChatFilter>()

    // RepoPattern must be initialized at pre-init time
    private val groups = setOf(
        DungeonChatFilter,
        SlayerChatFilter,
        EventChatFilter,
        MiscChatFilter,
        ForagingChatFilter,
        HuntingChatFilter,
        FarmingChatFilter,
        GardenChatFilter,
        WinterChatFilter,
    )

    // Forces all the filters to be initialized at pre-init time
    private val knownFilters: Set<ChatFilter> =
        groups.flatMap { it.filters }.toSet()

    fun register(filter: ChatFilter) {
        require(filter in knownFilters) { "Filter $filter is not registered in any group" }

        activeFilters += filter
    }

    fun unregister(filter: ChatFilter) {
        activeFilters -= filter
    }

    fun register(filters: Set<ChatFilter>) {
        filters.forEach(::register)
    }

    fun unregister(filters: Set<ChatFilter>) {
        filters.forEach(::unregister)
    }

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        groups.forEach { group ->
            group.activation.bind { groupActive ->
                if (groupActive) {
                    group.filters.forEach { filter ->
                        if (filter is ActivatedChatFilter) {
                            filter.activation.bind { active ->
                                if (active) register(filter)
                                else unregister(filter)
                            }
                        } else {
                            register(filter)
                        }
                    }
                } else {
                    group.filters.forEach { filter ->
                        if (filter is ActivatedChatFilter) {
                            filter.activation.unbind()
                        }
                        unregister(filter)
                    }
                }
            }
        }
    }

    @HandleEvent
    fun onSystemMessage(event: SkyHanniChatEvent.Allow) {
        var blockReason = block(event.cleanMessage)
        if (blockReason == null && config.powderMining.enabled) blockReason = powderMiningBlock(event)
        if (blockReason == null && config.crystalNucleus.enabled) blockReason = crystalNucleusBlock(event)

        event.blockedReason = blockReason ?: return
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent.Modify) {
        if (config.powderMining.enabled) powderMiningBlock(event)
        if (config.crystalNucleus.enabled) crystalNucleusBlock(event)
    }

    /**
     * Checks if the message should be blocked
     * @param message The message to check
     * @return The reason why the message was blocked, empty if not blocked
     */
    private fun block(message: String): String? {
        for (filter in activeFilters) {
            val blockReason = filter.block(message)
            if (blockReason != null) {
                return blockReason
            }
        }
        return null
    }

    /**
     * Checks if the message is a blocked powder mining message, as defined in PowderMiningChatFilter.
     * Will return a resultant blocking code
     * @param event The event to check
     * @return Block reason if applicable
     * @see block
     */
    private fun powderMiningBlock(event: SkyHanniChatEvent.Allow): String? {
        val powderMiningMatchResult = PowderMiningChatFilter.block(event.message)
        if (powderMiningMatchResult == "no_filter") {
            return null
        }
        return powderMiningMatchResult
    }

    /**
     * Checks if the message is a blocked powder mining message, as defined in PowderMiningChatFilter.
     * Will modify un-filtered Mining reward
     * @param event The event to check
     * @see block
     */
    private fun powderMiningBlock(event: SkyHanniChatEvent.Modify) {
        val powderMiningMatchResult = PowderMiningChatFilter.block(event.message)
        if (powderMiningMatchResult == "no_filter") {
            genericMiningRewardMessage.matchMatcher(event.message) {
                val reward = groupOrEmpty("reward")
                val amountFormat = groupOrNull("amount")?.let {
                    "§a+ §b$it§r"
                } ?: "§a+§r"
                event.replaceComponent("$amountFormat $reward".asComponent(), "powder_gain")
            }
        }
    }

    /**
     * Checks if the message is a blocked Crystal Nucleus Run message, as defined in CrystalNucleusChatFilter.
     * Will conditionally return a blocking code
     * @param event The event to check
     * @return Block reason if applicable
     * @see block
     */
    private fun crystalNucleusBlock(event: SkyHanniChatEvent.Allow): String? {
        val blockCode = CrystalNucleusChatFilter.block(event.message)?.getPair()?.first
        blockCode?.let { return it }
        return null
    }

    /**
     * Checks if the message is a blocked Crystal Nucleus Run message, as defined in CrystalNucleusChatFilter.
     * Will conditionally modify/compact messages in some cases
     * @param event The event to check
     * @see block
     */
    private fun crystalNucleusBlock(event: SkyHanniChatEvent.Modify) {
        val newMessage = CrystalNucleusChatFilter.block(event.message)?.getPair()?.second
        newMessage?.let {
            event.replaceComponent(it.asComponent(), "nuc_run")
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "chat.hypixelHub", "chat.filterType.hypixelHub")
        event.move(3, "chat.empty", "chat.filterType.empty")
        event.move(3, "chat.warping", "chat.filterType.warping")
        event.move(3, "chat.guildExp", "chat.filterType.guildExp")
        event.move(3, "chat.friendJoinLeft", "chat.filterType.friendJoinLeft")
        event.move(3, "chat.winterGift", "chat.filterType.winterGift")
        event.move(3, "chat.powderMining", "chat.filterType.powderMining")
        event.move(3, "chat.killCombo", "chat.filterType.killCombo")
        event.move(3, "chat.profileJoin", "chat.filterType.profileJoin")
        event.move(3, "chat.others", "chat.filterType.others")
        event.move(52, "chat.filterType.powderMining", "chat.filterType.powderMiningFilter.enabled")
        event.transform(53, "chat.filterType.powderMiningFilter.gemstoneFilterConfig") { element ->
            element.asJsonObject.apply {
                entrySet().forEach { (key, value) ->
                    if (value.asString == "FINE_ONLY") addProperty(key, "FINE_UP")
                }
            }
        }
        event.move(61, "chat.filterType.powderMiningFilter", "chat.filterType.powderMining")
        event.move(61, "chat.filterType.gemstoneFilterConfig", "chat.filterType.powderMining.gemstone")
        event.move(107, "chat.filterType.guildExp", "chat.filterType.guildEventExp")
    }
}
