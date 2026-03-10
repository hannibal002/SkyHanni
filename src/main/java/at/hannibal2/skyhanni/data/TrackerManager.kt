package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.features.misc.tracker.GenericIndividualTrackerConfig.TrackerSync.configSet
import at.hannibal2.skyhanni.config.features.misc.tracker.GenericIndividualTrackerConfig.TrackerSync.syncAllTrackers
import at.hannibal2.skyhanni.config.features.misc.tracker.ItemTrackerGenericConfig
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.ItemAddEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatIntOrUserError

@SkyHanniModule
object TrackerManager {

    private var hasChanged = false
    var dirty = false
    var commandEditTrackerSuccess = false
    private var shouldSyncTrackers = false // used for config migration


    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        with(SkyHanniMod.feature.misc.tracker) {
            ConditionalUtils.onToggle(
                this.itemTracker.textOrder,
                this.itemTracker.showTable,
                this.itemTracker.itemsShown,
                showUptime,
                onlyShowSession,
                this.itemTracker.profitPerHour
            ) {
                hasChanged = true
            }
        }
        for (config in configSet) {
            with(config.trackerConfig) {
                ConditionalUtils.onToggle(
                    showUptime,
                    onlyShowSession,
                ) {
                    hasChanged = true
                }
            }

            if (config.trackerConfig is ItemTrackerGenericConfig) {
                with(config.trackerConfig.itemTracker) {
                    ConditionalUtils.onToggle(
                        textOrder,
                        showTable,
                        itemsShown,
                        profitPerHour
                    ) {
                        hasChanged = true
                    }
                }
            }
        }

        if (shouldSyncTrackers) {
            syncAllTrackers()
            shouldSyncTrackers = false
        }
    }

    @HandleEvent(priority = HandleEvent.HIGHEST)
    fun onRenderOverlayFirst(event: GuiRenderEvent) {
        if (hasChanged) {
            dirty = true
        }
    }

    @HandleEvent(priority = HandleEvent.LOWEST)
    fun onRenderOverlayLast(event: GuiRenderEvent) {
        if (hasChanged) {
            dirty = false
            hasChanged = false
        }
    }

    fun commandEditTracker(args: Array<String>) {
        if (args.size < 2) {
            ChatUtils.userError("Usage: /shedittracker <item name> <amount>")
            return
        }

        val amount = args.last().formatIntOrUserError() ?: return
        if (amount == 0) {
            ChatUtils.userError("Amount can not be zero!")
            return
        }

        val rawName = args.dropLast(1).joinToString(" ")
        val internalName = NeuInternalName.fromItemNameOrInternalName(rawName)
        if (!internalName.isKnownItem()) {
            ChatUtils.chat("No item found for '$rawName'!")
            return
        }

        commandEditTrackerSuccess = false
        ItemAddEvent(internalName, amount, ItemAddManager.Source.COMMAND).post()
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shedittracker") {
            description = "Changes the tracked item amount for Diana, Fishing, Pest, Excavator, and Slayer Item Trackers."
            category = CommandCategory.USERS_BUG_FIX
            legacyCallbackArgs { commandEditTracker(it) }
        }
    }

    @HandleEvent(priority = HandleEvent.LOWEST)
    fun onItemAdd(event: ItemAddEvent) {
        if (event.source != ItemAddManager.Source.COMMAND || event.isCancelled) return
        if (!commandEditTrackerSuccess) {
            ChatUtils.userError("Could not edit the Item Tracker! Does this item belong to this tracker? Is the tracker active right now?")
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        val oldBase = "misc.tracker"
        val newBase = "misc.tracker.itemTracker"
        val movedList: List<String> = listOf(
            "showRecentDrops",
            "excludeHiddenItemsInPrice",
            "showTable",
            "itemsShown",
            "profitPerHour",
            "hideInEstimatedItemValue",
            "hideOutsideInventory",
            "textOrder"
        )
        for (entry in movedList) {
            event.move(121, "$oldBase.$entry", "$newBase.$entry")
        }
        // if we don't include the transformation it bricks the config
        event.move(121, "$oldBase.warnings", "$newBase.warnings") { entry ->
            entry
        }
        if (event.oldVersion < 121) shouldSyncTrackers = true

        event.move(121, "misc.tracker.hideItemTrackersOutsideInventory", "misc.tracker.hideOutsideInventory")

    }
}
