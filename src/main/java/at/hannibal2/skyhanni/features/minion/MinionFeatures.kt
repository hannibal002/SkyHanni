package at.hannibal2.skyhanni.features.minion

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.Storage
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.MinionOpenEvent
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockStateAt
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.canBeSeen
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.editCopy
import at.hannibal2.skyhanni.utils.LorenzUtils.formatInteger
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNeeded
import at.hannibal2.skyhanni.utils.RenderUtils.drawString
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.SpecialColour
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matchRegex
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.init.Blocks
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import org.lwjgl.input.Mouse
import java.awt.Color

class MinionFeatures {
    private val config get() = SkyHanniMod.feature.minions
    private var lastClickedEntity: LorenzVec? = null
    private var lastMinion: LorenzVec? = null
    private var newMinion: LorenzVec? = null
    private var newMinionName: String? = null
    private var lastMinionOpened = 0L
    private var minionInventoryOpen = false

    private var lastInventoryClosed = 0L
    private var coinsPerDay = ""
    private val minionUpgradePattern = "§aYou have upgraded your Minion to Tier (?<tier>.*)".toPattern()

    @SubscribeEvent
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (LorenzUtils.skyBlockIsland != IslandType.PRIVATE_ISLAND) return
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) return

        val lookingAt = event.pos.offset(event.face).toLorenzVec()
        val equipped = InventoryUtils.getItemInHand() ?: return

        if (equipped.displayName.contains(" Minion ") && lookingAt.getBlockStateAt().block == Blocks.air) {
            newMinion = lookingAt.add(0.5, 0.0, 0.5)
            newMinionName = getMinionName(equipped.cleanName())
        } else {
            newMinion = null
            newMinionName = null
        }
    }

    @SubscribeEvent
    fun onClick(event: InputEvent.MouseInputEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (LorenzUtils.skyBlockIsland != IslandType.PRIVATE_ISLAND) return

        if (!Mouse.getEventButtonState()) return
        if (Mouse.getEventButton() != 1) return

        val minecraft = Minecraft.getMinecraft()
        val entity = minecraft.pointedEntity
        if (entity != null) {
            lastClickedEntity = entity.getLorenzVec()
        }
    }

    @SubscribeEvent
    fun onRenderLastClickedMinion(event: LorenzRenderWorldEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (LorenzUtils.skyBlockIsland != IslandType.PRIVATE_ISLAND) return
        if (!config.lastClickedMinion.display) return

        val special = config.lastClickedMinion.color
        val color = Color(SpecialColour.specialToChromaRGB(special), true)

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
                    extraSizeBottomY = 0.0
                )
            }
        }
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (LorenzUtils.skyBlockIsland != IslandType.PRIVATE_ISLAND) return
        if (!event.inventoryName.contains(" Minion ")) return

        event.inventoryItems[48]?.let {
            if ("§aCollect All" == it.name) {
                MinionOpenEvent(event.inventoryName, event.inventoryItems).postAndCatch()
            }
        }
    }

    @SubscribeEvent
    fun onMinionOpen(event: MinionOpenEvent) {
        val minions = minions ?: return
        val entity = lastClickedEntity ?: return

        val openInventory = event.inventoryName
        val name = getMinionName(openInventory)
        if (!minions.contains(entity)) {
            MinionFeatures.minions = minions.editCopy {
                this[entity] = Storage.ProfileSpecific.MinionConfig().apply {
                    displayName = name
                    lastClicked = 0
                }
            }
        } else {
            if (minions[entity]!!.displayName != name) {
                minions[entity]!!.displayName = name
            }
        }
        lastMinion = entity
        lastClickedEntity = null
        minionInventoryOpen = true
        lastMinionOpened = 0
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (!minionInventoryOpen) return
        val minions = minions ?: return

        minionInventoryOpen = false
        lastMinionOpened = System.currentTimeMillis()
        coinsPerDay = ""
        lastInventoryClosed = System.currentTimeMillis()

        val location = lastMinion ?: return

        if (location !in minions) {
            minions[location]!!.lastClicked = 0
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (LorenzUtils.skyBlockIsland != IslandType.PRIVATE_ISLAND) return
        if (coinsPerDay != "") return

        if (Minecraft.getMinecraft().currentScreen is GuiChest && config.hopperProfitDisplay) {
            coinsPerDay = if (minionInventoryOpen) updateCoinsPerDay() else ""
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
        val slot = InventoryUtils.getItemsInOpenChest().find { it.slotNumber == 28 } ?: return ""

        val stack = slot.stack
        val line = stack.getLore().find { it.contains("Held Coins") } ?: return ""

        val duration = minions?.get(loc)?.let {
            val lastClicked = it.lastClicked
            if (lastClicked == 0L) {
                return "§cCan't calculate coins/day: No time data available!"
            }
            System.currentTimeMillis() - lastClicked
        } ?: return "§cCan't calculate coins/day: No time data available!"

        //§7Held Coins: §b151,389
        val coins = line.split(": §b")[1].replace(",", "").toDouble()

        val coinsPerDay = (coins / (duration.toDouble())) * 1000 * 60 * 60 * 24

        val format = formatInteger(coinsPerDay.toInt())
        val hopperName = stack.name
        return "§7Coins/day with $hopperName§7: §6$format coins"
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        lastClickedEntity = null
        lastMinion = null
        lastMinionOpened = 0L
        minionInventoryOpen = false
    }

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (LorenzUtils.skyBlockIsland != IslandType.PRIVATE_ISLAND) return

        val message = event.message
        if (message.matchRegex("§aYou received §r§6(.*) coins§r§a!") && System.currentTimeMillis() - lastInventoryClosed < 2_000) {
            minions?.get(lastMinion)?.let {
                it.lastClicked = System.currentTimeMillis()
            }

        }
        if (message.startsWith("§aYou picked up a minion!") && lastMinion != null) {
            minions = minions?.editCopy { remove(lastMinion) }
            lastClickedEntity = null
            lastMinion = null
            lastMinionOpened = 0L
        }
        if (message.startsWith("§bYou placed a minion!") && newMinion != null) {
            minions = minions?.editCopy {
                this[newMinion!!] = Storage.ProfileSpecific.MinionConfig().apply {
                    displayName = newMinionName
                    lastClicked = 0
                }
            }
            newMinion = null
            newMinionName = null
        }

        minionUpgradePattern.matchMatcher(message) {
            val newTier = group("tier").romanToDecimalIfNeeded()
            minions?.get(lastMinion)?.let {
                val minionName = getMinionName(it.displayName, newTier)
                it.displayName = minionName
            }
        }
    }

    @SubscribeEvent
    fun onRenderLastEmptied(event: LorenzRenderWorldEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (LorenzUtils.skyBlockIsland != IslandType.PRIVATE_ISLAND) return

        val playerLocation = LocationUtils.playerLocation()
        val minions = minions ?: return
        for (minion in minions) {
            val location = minion.key.add(0.0, 1.0, 0.0)
            if (!location.canBeSeen()) continue

            val lastEmptied = minion.value.lastClicked
            if (playerLocation.distance(location) >= config.emptiedTime.distance) continue

            if (config.nameDisplay) {
                val displayName = minion.value.displayName
                val name = "§6" + if (config.nameOnlyTier) {
                    displayName.split(" ").last()
                } else displayName
                event.drawString(location.add(0.0, 0.65, 0.0), name, true)
            }

            if (config.emptiedTime.display && lastEmptied != 0L) {
                val duration = System.currentTimeMillis() - lastEmptied
                val format = TimeUtils.formatDuration(duration, longName = true) + " ago"
                val text = "§eHopper Emptied: $format"
                event.drawString(location.add(0.0, 1.15, 0.0), text, true)
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onRenderLiving(event: RenderLivingEvent.Specials.Pre<EntityLivingBase>) {
        if (!LorenzUtils.inSkyBlock) return
        if (LorenzUtils.skyBlockIsland != IslandType.PRIVATE_ISLAND) return
        if (!config.hideMobsNametagNearby) return

        val entity = event.entity
        if (entity !is EntityArmorStand) return
        if (!entity.hasCustomName()) return
        if (entity.isDead) return
        val minions = minions ?: return

        if (entity.customNameTag.contains("§c❤")) {
            val loc = entity.getLorenzVec()
            if (minions.any { it.key.distance(loc) < 5 }) {
                event.isCanceled = true
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun renderOverlay(event: GuiScreenEvent.BackgroundDrawnEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!minionInventoryOpen) return

        if (config.hopperProfitDisplay) {
            config.hopperProfitPos.renderString(coinsPerDay, posLabel = "Minion Coins Per Day")
        }
    }

    companion object {
        private var minions: Map<LorenzVec, Storage.ProfileSpecific.MinionConfig>?
            get() {
                return ProfileStorageData.profileSpecific?.minions
            }
            set(value) {
                ProfileStorageData.profileSpecific?.minions = value
            }

        fun clearMinionData() {
            minions = mutableMapOf()
            LorenzUtils.chat("Manually reset all private island minion location data!")
        }
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "minions.lastClickedMinionDisplay", "minions.lastClickedMinion.display")
        event.move(3, "minions.lastOpenedMinionColor", "minions.lastClickedMinion.color")
        event.move(3, "minions.lastOpenedMinionTime", "minions.lastClickedMinion.time")
        event.move(3, "minions.emptiedTimeDisplay", "minions.emptiedTime.display")
        event.move(3, "minions.distance", "minions.emptiedTime.distance")
    }
}
