package at.hannibal2.skyhanni.features.minion

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils.formatInteger
import at.hannibal2.skyhanni.utils.LorenzUtils.matchRegex
import at.hannibal2.skyhanni.utils.RenderUtils.drawString
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
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

    var lastClickedEntity: LorenzVec? = null
    var lastMinion: LorenzVec? = null
    var lastMinionOpened = 0L
    var minionInventoryOpen = false

    var lastCoinsRecived = 0L
    var lastMinionPickedUp = 0L
    val minions = mutableMapOf<LorenzVec, Long>()
    var coinsPerDay = ""

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        for (minion in SkyHanniMod.feature.hidden.minions) {
            val vec = LorenzVec.decodeFromString(minion.key)
            minions[vec] = minion.value
        }
    }

    @SubscribeEvent
    fun onClick(event: InputEvent.MouseInputEvent) {
        if (!LorenzUtils.inSkyblock) return
        if (LorenzUtils.skyBlockIsland != "Private Island") return

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
        if (!LorenzUtils.inSkyblock) return
        if (LorenzUtils.skyBlockIsland != "Private Island") return
        if (!SkyHanniMod.feature.minions.lastClickedMinionDisplay) return

        val special = SkyHanniMod.feature.minions.lastOpenedMinionColor
        val color = Color(SpecialColour.specialToChromaRGB(special), true)

        val loc = lastMinion
        if (loc != null) {
            val time = SkyHanniMod.feature.minions.lastOpenedMinionTime * 1_000
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
        if (LorenzUtils.skyBlockIsland != "Private Island") return

        if (InventoryUtils.currentlyOpenInventory().contains("Minion")) {
            if (lastClickedEntity != null) {
                lastMinion = lastClickedEntity
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
                        minions[location] = System.currentTimeMillis()
                        saveConfig()
                    }
                    if (location !in minions) {
                        minions[location] = 0
                    }

                    if (System.currentTimeMillis() - lastMinionPickedUp < 2_000) {
                        minions.remove(location)
                        saveConfig()
                    }
                }
            }
        }

        if (SkyHanniMod.feature.minions.hopperProfitDisplay) {
            coinsPerDay = if (minionInventoryOpen) {
                updateCoinsPerDay()
            } else {
                ""
            }
        }
    }

    private fun updateCoinsPerDay(): String {
        val loc = lastMinion!!
        val slot = InventoryUtils.getItemsInOpenChest().find { it.slotNumber == 28 } ?: return ""

        val stack = slot.stack
        val line = stack.getLore().find { it.contains("Held Coins") } ?: return ""

        if (coinsPerDay != "") return coinsPerDay

        val lastClicked = minions.getOrDefault(loc, -1)
        if (lastClicked == -1L) {
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
        val minionConfig = SkyHanniMod.feature.hidden.minions

        minionConfig.clear()
        for (minion in minions) {
            minionConfig[minion.key.encodeToString()] = minion.value
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
        if (!LorenzUtils.inSkyblock) return
        if (LorenzUtils.skyBlockIsland != "Private Island") return

        if (event.message.matchRegex("§aYou received §r§6(.*) coins§r§a!")) {
            lastCoinsRecived = System.currentTimeMillis()
        }
        if (event.message.startsWith("§aYou picked up a minion!")) {
            lastMinionPickedUp = System.currentTimeMillis()
        }
    }

    @SubscribeEvent
    fun onRenderLastEmptied(event: RenderWorldLastEvent) {
        if (!LorenzUtils.inSkyblock) return
        if (!SkyHanniMod.feature.minions.emptiedTimeDisplay) return
        if (LorenzUtils.skyBlockIsland != "Private Island") return

        val playerLocation = LocationUtils.playerLocation()
        val playerEyeLocation = LocationUtils.playerEyeLocation()
        for (minion in minions) {
            val location = minion.key
            if (playerLocation.distance(location) < SkyHanniMod.feature.minions.emptiedTimeDistance) {
                val lastEmptied = minion.value
                if (lastEmptied == 0L) continue
                val duration = System.currentTimeMillis() - lastEmptied
                val format = StringUtils.formatDuration(duration / 1000) + " ago"
                if (LocationUtils.canSee(playerEyeLocation, location)) {
                    val text = "§eHopper Emptied: $format"
                    event.drawString(location.add(0.0, 2.0, 0.0), text, true)
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onRenderLiving(event: RenderLivingEvent.Specials.Pre<EntityLivingBase>) {
        if (!LorenzUtils.inSkyblock) return
        if (LorenzUtils.skyBlockIsland != "Private Island") return
        if (!SkyHanniMod.feature.minions.hideMobsNametagNearby) return

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
        if (SkyHanniMod.feature.minions.hopperProfitDisplay) {
            SkyHanniMod.feature.minions.hopperProfitPos.renderString(coinsPerDay)
        }
    }
}