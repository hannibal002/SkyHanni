package at.hannibal2.skyhanni.features.minion

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.achievements.Achievement
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.MinionCloseEvent
import at.hannibal2.skyhanni.events.MinionOpenEvent
import at.hannibal2.skyhanni.events.MinionStorageOpenEvent
import at.hannibal2.skyhanni.events.achievements.AchievementRegistrationEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.entity.EntityClickEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.player.ClickAction
import at.hannibal2.skyhanni.events.player.PlayerInteractionEvent
import at.hannibal2.skyhanni.features.achievements.AchievementManager
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockStateAt
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.EntityUtils.getEntitiesNearby
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatDouble
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNecessary
import at.hannibal2.skyhanni.utils.RegexUtils.find
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.compat.deceased
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawString
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.block.Blocks

@SkyHanniModule
object MinionFeatures {

    private val config get() = SkyHanniMod.feature.misc.minions
    private var lastClickedEntity: LorenzVec? = null
    private var newMinion: LorenzVec? = null
    private var newMinionName: String? = null
    private var lastMinionOpened = 0L

    private var lastInventoryClosed = 0L
    private var display: Renderable? = null

    private val patternGroup = RepoPattern.group("minion")

    /**
     * REGEX-TEST: You have upgraded your Minion to Tier V
     */
    private val minionUpgradePattern by patternGroup.pattern(
        "chat.upgrade-colorless",
        "You have upgraded your Minion to Tier (?<tier>.*)",
    )

    /**
     * REGEX-TEST: You received 3,520,690 coins!
     */
    private val minionCoinPattern by patternGroup.pattern(
        "chat.coin-colorless",
        "You received (?<coins>[,\\d.]+) coins!",
    )

    /**
     * REGEX-TEST: Redstone Minion IV
     * REGEX-TEST: Chicken Minion XI
     */
    private val minionTitlePattern by patternGroup.pattern(
        "title",
        "Minion [^➜]",
    )
    private val minionCollectItemPattern by patternGroup.pattern(
        "item.collect",
        "^§aCollect All$",
    )

    var lastMinion: LorenzVec? = null
    private var lastStorage: LorenzVec? = null
    var minionInventoryOpen = false
    var minionStorageInventoryOpen = false

    private val minions: MutableMap<LorenzVec, ProfileSpecificStorage.MinionConfig>?
        get() = ProfileStorageData.profileSpecific?.minions

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onPlayerInteraction(event: PlayerInteractionEvent) {
        if (event.action != ClickAction.RIGHT_CLICK_BLOCK) return

        val vec = event.face?.unitVec3i ?: return
        val lookingAt = event.pos?.offset(vec)?.toLorenzVec() ?: return
        val equipped = InventoryUtils.getItemInHand() ?: return

        if (equipped.hoverName.string.contains(" Minion ") && lookingAt.getBlockStateAt().block == Blocks.AIR) {
            newMinion = lookingAt.add(0.5, 0.0, 0.5)
            newMinionName = getMinionName(equipped.cleanName())
        } else {
            newMinion = null
            newMinionName = null
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onEntityClick(event: EntityClickEvent) {
        if (!enableWithHub()) return
        if (event.clickType != ClickType.RIGHT_CLICK) return

        lastClickedEntity = event.clickedEntity.getLorenzVec()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onBlockClick(event: BlockClickEvent) {
        if (!enableWithHub()) return
        if (event.clickType != ClickType.RIGHT_CLICK) return

        lastStorage = event.position
    }

    @HandleEvent
    fun onRenderLastClickedMinion(event: SkyHanniRenderWorldEvent) {
        if (!enableWithHub()) return
        if (!config.lastClickedMinion.display) return

        val color = config.lastClickedMinion.color.toColor()

        val loc = lastMinion
        if (loc != null) {
            val time = config.lastClickedMinion.time * 1_000
            if (lastMinionOpened + time > System.currentTimeMillis()) {
                event.drawWaypointFilled(
                    loc.add(-0.5, 0.0, -0.5),
                    color,
                    true,
                    extraSize = -0.25,
                    extraSizeTopY = 0.2,
                    extraSizeBottomY = 0.0,
                )
            }
        }
    }

    @HandleEvent
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!enableWithHub()) return
        val inventoryName = event.inventoryName
        if (!minionTitlePattern.find(inventoryName)) return

        event.inventoryItems[48]?.let {
            if (minionCollectItemPattern.matches(it.hoverName.formattedTextCompatLeadingWhiteLessResets())) {
                MinionOpenEvent(inventoryName, event.inventoryItems).post()
                return
            }
        }

        MinionStorageOpenEvent(lastStorage, event.inventoryItems).post()
        minionStorageInventoryOpen = true
    }

    @HandleEvent
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!enableWithHub()) return
        if (minionInventoryOpen) {
            MinionOpenEvent(event.inventoryName, event.inventoryItems).post()
        }
    }

    @HandleEvent
    fun onMinionOpen(event: MinionOpenEvent) {
        removeBuggedMinions()
        val minions = minions ?: return
        val entity = lastClickedEntity ?: return

        val openInventory = event.inventoryName
        val name = getMinionName(openInventory)
        val inHub = SkyBlockUtils.currentIsland == IslandType.HUB
        val inStorage = minions.contains(entity)

        if (!inStorage && !inHub) minions[entity] = ProfileSpecificStorage.MinionConfig().apply {
            displayName = name
            lastClicked = SimpleTimeMark.farPast()
        } else minions[entity]?.apply { displayName = name }

        lastMinion = entity
        lastClickedEntity = null
        minionInventoryOpen = true
        lastMinionOpened = 0
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shfixminions") {
            description = "Removed bugged minion locations from your private island"
            category = CommandCategory.USERS_BUG_FIX
            simpleCallback { removeBuggedMinions(isCommand = true) }
        }
    }

    private fun removeBuggedMinions(isCommand: Boolean = false) {
        if (!IslandType.PRIVATE_ISLAND.isCurrent()) return
        val minions = minions ?: return

        val removedEntities = mutableListOf<LorenzVec>()
        for (location in minions.keys) {
            if (location.distanceToPlayer() > 30) continue
            val entitiesNearby = location.getEntitiesNearby<ArmorStand>(5.0).map { it.distanceTo(location) }
            if (!entitiesNearby.any { it == 0.0 }) {
                removedEntities.add(location)
            }
        }

        val size = removedEntities.size
        if (size == 0) {
            if (isCommand) {
                ChatUtils.chat("No bugged minions found nearby.")
            }
            return
        }
        for (removedEntity in removedEntities) {
            minions.remove(removedEntity)
        }
        ChatUtils.chat("Removed $size wrong/bugged minion locations from your island.")
    }

    @HandleEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (event.reopenSameName) return

        minionStorageInventoryOpen = false
        if (!minionInventoryOpen) return
        val minions = minions ?: return

        minionInventoryOpen = false
        lastMinionOpened = System.currentTimeMillis()
        display = null
        lastInventoryClosed = System.currentTimeMillis()

        MinionCloseEvent().post()
        if (IslandType.PRIVATE_ISLAND.isCurrent()) {
            val location = lastMinion ?: return

            if (location !in minions) {
                minions[location]?.lastClicked = SimpleTimeMark.farPast()
            }
        }
    }

    // Todo this calculation should not happen invariably when null.
    //  Use a "dirty" flag or something similar, and handle state management.
    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onTick() {
        if (display != null) return

        if (Minecraft.getInstance().screen is ContainerScreen && config.hopperProfitDisplay) {
            display = if (minionInventoryOpen) Renderable.text(updateCoinsPerDay()) else null
        }
    }

    private fun getMinionName(oldName: String, newTier: Int = 0): String {
        var list = oldName.split(" ").toList()
        val last = list.last()
        val number = if (newTier != 0) newTier else last.romanToDecimal()
        list = list.dropLast(1)

        return list.joinToString(" ") + " $number"
    }

    private fun updateCoinsPerDay(): String {
        val loc = lastMinion ?: return "§cNo last minion found! Try reopening the minion view."
        val slot = InventoryUtils.getItemsInOpenChest().find { it.index == 28 } ?: return ""

        val stack = slot.item
        val line = stack.getLore().find { it.contains("Held Coins") } ?: return ""

        val duration = minions?.get(loc)?.let {
            val lastClicked = it.lastClicked
            if (lastClicked.isFarPast()) {
                return "§cCan't calculate coins/day: No time data available!"
            }
            SimpleTimeMark.now() - lastClicked
        } ?: return "§cCan't calculate coins/day: No time data available!"

        // §7Held Coins: §b151,389
        // TODO use regex
        val coins = line.split(": §b")[1].formatDouble()

        val coinsPerDay = (coins / (duration.inWholeMilliseconds)) * 1000 * 60 * 60 * 24

        val format = coinsPerDay.toInt().addSeparators()
        return "§7Coins/day with ${stack.hoverName.formattedTextCompatLeadingWhiteLessResets()}§7: §6$format coins"
    }

    // TODO reshape to data class, use Resettable
    @HandleEvent
    fun onWorldChange() {
        lastClickedEntity = null
        lastMinion = null
        lastMinionOpened = 0L
        minionInventoryOpen = false
        minionStorageInventoryOpen = false
    }

    private const val MINION_COIN_ACHIEVEMENT = "minion hopper"

    @HandleEvent
    fun onAchievementRegistration(event: AchievementRegistrationEvent) {
        val achievement = Achievement(
            "Inflation Contributor".asComponent(),
            "Gain Coins from a single Minion Hopper".asComponent(),
            10f,
            false,
            listOf(1_000_000, 5_000_000, 10_000_000),
        )
        event.register(achievement, MINION_COIN_ACHIEVEMENT)
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        // TODO use repo patterns
        val message = event.cleanMessage
        minionCoinPattern.matchMatcher(message) {
            if (System.currentTimeMillis() - lastInventoryClosed < 2_000) {
                minions?.get(lastMinion)?.let {
                    it.lastClicked = SimpleTimeMark.now()
                }
            }
            val coins = group("coins").formatInt()
            val achievement = AchievementManager.getAchievement(MINION_COIN_ACHIEVEMENT)
            if (coins > achievement.data.progress) {
                AchievementManager.updateTieredAchievement(MINION_COIN_ACHIEVEMENT, coins)
            }
        }
        if (message.startsWith("You picked up a minion!") && lastMinion != null) {
            minions?.remove(lastMinion)
            lastClickedEntity = null
            lastMinion = null
            lastMinionOpened = 0L
        }

        if (message.startsWith("You placed a minion!")) newMinion?.let {
            minions?.put(
                it,
                ProfileSpecificStorage.MinionConfig().apply {
                    displayName = newMinionName.orEmpty()
                    lastClicked = SimpleTimeMark.farPast()
                },
            )
            newMinion = null
            newMinionName = null
        }

        minionUpgradePattern.matchMatcher(message) {
            val newTier = group("tier").romanToDecimalIfNecessary()
            minions?.get(lastMinion)?.let {
                val minionName = getMinionName(it.displayName, newTier)
                it.displayName = minionName
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onRenderLastEmptied(event: SkyHanniRenderWorldEvent) {
        val playerLocation = LocationUtils.playerLocation()
        val minions = minions ?: return
        for (minion in minions) {
            val location = minion.key.up()
            if (location.distanceToPlayer() > 50) continue

            val lastEmptied = minion.value.lastClicked
            if (playerLocation.distance(location) >= config.emptiedTime.distance) continue

            if (config.nameDisplay) {
                val displayName = minion.value.displayName
                val name = "§6" + if (config.nameOnlyTier) {
                    displayName.split(" ").last()
                } else displayName
                event.drawString(location.up(0.65), name, true)
            }

            if (config.emptiedTime.display && !lastEmptied.isFarPast()) {
                val format = lastEmptied.passedSince().format(longName = true) + " ago"
                val text = "§eHopper Emptied: $format"
                event.drawString(location.up(1.15), text, true)
            }
        }
    }

    @HandleEvent(priority = HandleEvent.HIGH, onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onRenderLiving(event: CheckRenderEntityEvent<ArmorStand>) {
        if (!config.hideMobsNametagNearby) return

        val entity = event.entity.takeIf {
            val nameMatch = it.customName?.string?.contains("❤") ?: false
            it.hasCustomName() && !it.deceased && nameMatch
        } ?: return
        val minions = minions ?: return

        val loc = entity.getLorenzVec()
        if (minions.any { it.key.distance(loc) < 5 }) {
            event.cancel()
        }
    }

    private fun enableWithHub() = IslandType.PRIVATE_ISLAND.isCurrent() || IslandType.HUB.isCurrent()

    @HandleEvent(onlyOnSkyblock = true)
    fun onChestGuiRender(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!minionInventoryOpen || !config.hopperProfitDisplay) return

        val display = display ?: return
        config.hopperProfitPos.renderRenderable(display, posLabel = "Minion Coins Per Day")
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "minions.lastClickedMinionDisplay", "minions.lastClickedMinion.display")
        event.move(3, "minions.lastOpenedMinionColor", "minions.lastClickedMinion.color")
        event.move(3, "minions.lastOpenedMinionTime", "minions.lastClickedMinion.time")
        event.move(3, "minions.emptiedTimeDisplay", "minions.emptiedTime.display")
        event.move(3, "minions.distance", "minions.emptiedTime.distance")

        event.move(31, "minions", "misc.minions")
    }
}
