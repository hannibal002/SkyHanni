package at.hannibal2.skyhanni.features.minion

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils.formatInteger
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.RenderUtils.drawString
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.StringUtils.matchRegex
import net.minecraft.client.Minecraft
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import org.lwjgl.input.Mouse
import java.awt.Color

class MinionFeatures {
    private val config get() = SkyHanniMod.feature.minions
    private var lastClickedEntity: LorenzVec? = null
    private var lastMinion: LorenzVec? = null
    private var lastMinionOpened = 0L
    private var minionInventoryOpen = false

    private var lastCoinsRecived = 0L
    private var lastMinionPickedUp = 0L
    private val minions = mutableMapOf<LorenzVec, MinionData>()
    private var coinsPerDay = ""

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        for (minion in SkyHanniMod.feature.hidden.minionLastClick) {
            val key = minion.key
            val vec = LorenzVec.decodeFromString(key)
            val name = SkyHanniMod.feature.hidden.minionName[key] ?: "§cNo name saved!"
            val data = MinionData(name, minion.value)
            minions[vec] = data
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
    fun onRenderLastClickedMinion(event: RenderWorldLastEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (LorenzUtils.skyBlockIsland != IslandType.PRIVATE_ISLAND) return
        if (!config.lastClickedMinionDisplay) return

        val special = config.lastOpenedMinionColor
        val color = Color(SpecialColour.specialToChromaRGB(special), true)

        val loc = lastMinion
        if (loc != null) {
            val time = config.lastOpenedMinionTime * 1_000
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
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (LorenzUtils.skyBlockIsland != IslandType.PRIVATE_ISLAND) return

        val openInventory = InventoryUtils.currentlyOpenInventory()
        if (openInventory.contains("Minion")) {
            lastClickedEntity?.let {
                val name = getMinionName(openInventory)
                if (!minions.contains(it)) {
                    minions[it] = MinionData(name, 0)
                    saveConfig()
                } else {
                    if (minions[it]!!.name != name) {
                        minions[it]!!.name = name
                        saveConfig()
                    }
                }
                lastMinion = it
                lastClickedEntity = null
                minionInventoryOpen = true
                lastMinionOpened = 0
            }
        } else {
            if (minionInventoryOpen) {
                minionInventoryOpen = false
                lastMinionOpened = System.currentTimeMillis()

                val location = lastMinion
                if (location != null) {

                    if (System.currentTimeMillis() - lastCoinsRecived < 2_000) {
                        minions[location]!!.lastClicked = System.currentTimeMillis()
                        saveConfig()
                    }
                    if (location !in minions) {
                        minions[location]!!.lastClicked = 0
                    }

                    if (System.currentTimeMillis() - lastMinionPickedUp < 2_000) {
                        minions.remove(location)
                        saveConfig()
                    }
                }
            }
        }

        if (config.hopperProfitDisplay) {
            coinsPerDay = if (minionInventoryOpen) {
                updateCoinsPerDay()
            } else {
                ""
            }
        }
    }

    private fun getMinionName(inventoryName: String): String {
        var list = inventoryName.split(" ").toList()
        val last = list.last()
        val number = last.romanToDecimal()
        list = list.dropLast(1)

        return list.joinToString(" ") + " $number"
    }

    private fun updateCoinsPerDay(): String {
        val loc = lastMinion!!
        val slot = InventoryUtils.getItemsInOpenChest().find { it.slotNumber == 28 } ?: return ""

        val stack = slot.stack
        val line = stack.getLore().find { it.contains("Held Coins") } ?: return ""

        if (coinsPerDay != "") return coinsPerDay

        val lastClicked = minions[loc]!!.lastClicked
        if (lastClicked == 0L) {
            return "Can't calculate coins/day: No time data available!"
        }
        val duration = System.currentTimeMillis() - lastClicked

        //§7Held Coins: §b151,389
        val coins = line.split(": §b")[1].replace(",", "").toDouble()

        val coinsPerDay = (coins / (duration.toDouble())) * 1000 * 60 * 60 * 24

        val format = formatInteger(coinsPerDay.toInt())
        val hopperName = stack.name
        return "§7Coins/day with $hopperName§7: §6$format coins"
    }

    private fun saveConfig() {
        val minionConfig = SkyHanniMod.feature.hidden.minionLastClick
        val minionName = SkyHanniMod.feature.hidden.minionName

        minionConfig.clear()
        minionName.clear()
        for (minion in minions) {
            val coordinates = minion.key.encodeToString()
            val data = minion.value
            minionConfig[coordinates] = data.lastClicked
            minionName[coordinates] = data.name
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        lastClickedEntity = null
        lastMinion = null
        lastMinionOpened = 0L
        minionInventoryOpen = false
    }

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (LorenzUtils.skyBlockIsland != IslandType.PRIVATE_ISLAND) return

        if (event.message.matchRegex("§aYou received §r§6(.*) coins§r§a!")) {
            lastCoinsRecived = System.currentTimeMillis()
        }
        if (event.message.startsWith("§aYou picked up a minion!")) {
            lastMinionPickedUp = System.currentTimeMillis()
        }
    }

    @SubscribeEvent
    fun onRenderLastEmptied(event: RenderWorldLastEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (LorenzUtils.skyBlockIsland != IslandType.PRIVATE_ISLAND) return

        val playerLocation = LocationUtils.playerLocation()
        val playerEyeLocation = LocationUtils.playerEyeLocation()
        for (minion in minions) {
            val location = minion.key.add(0.0, 1.0, 0.0)
            if (!LocationUtils.canSee(playerEyeLocation, location)) continue

            val lastEmptied = minion.value.lastClicked
            if (playerLocation.distance(location) >= config.distance) continue

            if (config.nameDisplay) {
                val displayName = minion.value.name
                val name = "§6" + if (config.nameOnlyTier) {
                    displayName.split(" ").last()
                } else displayName
                event.drawString(location.add(0.0, 0.65, 0.0), name, true)
            }

            if (config.emptiedTimeDisplay) {
                if (lastEmptied != 0L) {
                    val duration = System.currentTimeMillis() - lastEmptied
                    val format = TimeUtils.formatDuration(duration, longName = true) + " ago"
                    val text = "§eHopper Emptied: $format"
                    event.drawString(location.add(0.0, 1.15, 0.0), text, true)
                }
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

        if (entity.customNameTag.contains("§c❤")) {
            val loc = entity.getLorenzVec()
            if (minions.any { it.key.distance(loc) < 5 }) {
                event.isCanceled = true
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun renderOverlay(event: GuiScreenEvent.BackgroundDrawnEvent) {
        if (config.hopperProfitDisplay) {
            config.hopperProfitPos.renderString(coinsPerDay, posLabel = "Minion Coins Per Day")
        }
    }
}