package at.hannibal2.skyhanni.features

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.client.Minecraft
import net.minecraft.entity.EntityLiving
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.util.regex.Pattern

class SummoningMobManager {

    private val summoningMobs = mutableMapOf<EntityLiving, SummoningMob>()
    private val summoningMobNametags = mutableListOf<EntityArmorStand>()
    private var summoningsSpawned = 0
    private var searchArmorStands = false
    private var searchMobs = false

    //§aYou have spawned your Tank Zombie §r§asoul! §r§d(249 Mana)
    private val spawnPatter = Pattern.compile("§aYou have spawned your (.+) §r§asoul! §r§d\\((\\d+) Mana\\)")
    private val despawnPatter = Pattern.compile("§cYou have despawned your (monster|monsters)!")

    //§a§ohannibal2's Tank Zombie§r §a160k§c❤
    private val healthPattern = Pattern.compile("§a§o(.+)'s (.+)§r §[ae]([\\dkm]+)§c❤")

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!LorenzUtils.isOnHypixel) return

        val message = event.message
        val matcher = spawnPatter.matcher(message)
        if (matcher.matches()) {
            if (SkyHanniMod.feature.abilities.summoningMobDisplay) {
                event.blockedReason = "summoning_soul"
            }
            summoningsSpawned++
            searchArmorStands = true
            searchMobs = true
        }

        if (despawnPatter.matcher(message).matches() || message.startsWith("§c ☠ §r§7You ")) {
            despawned()
            if (SkyHanniMod.feature.abilities.summoningMobDisplay && !message.contains("☠")) {
                event.blockedReason = "summoning_soul"
            }
        }
    }

    var tick = 0

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!isEnabled()) return

        if (SkyHanniMod.feature.abilities.summoningMobDisplay) {
            if (tick++ % 20 == 0) {
                updateData()
            }
        }

        if (searchArmorStands) {
            Minecraft.getMinecraft().theWorld.loadedEntityList
                .filter { it is EntityArmorStand && it !in summoningMobNametags }
                .forEach {
                    val name = it.displayName.unformattedText
                    val matcher = healthPattern.matcher(name)
                    if (matcher.matches()) {
                        val playerName = Minecraft.getMinecraft().thePlayer.name
                        if (name.contains(playerName)) {
                            summoningMobNametags.add(it as EntityArmorStand)
                            if (summoningMobNametags.size == summoningsSpawned) {
                                searchArmorStands = false
                            }
                        }
                    }
                }
        }
        if (searchMobs) {

            val playerLocation = LocationUtils.playerLocation()
            Minecraft.getMinecraft().theWorld.loadedEntityList
                .filter {
                    it is EntityLiving && it !in summoningMobs.keys && it.getLorenzVec().distance(playerLocation) < 3
                }
                .forEach {
                    if (it.ticksExisted == 0) {
                        summoningMobs[it as EntityLiving] = SummoningMob(System.currentTimeMillis(), name = "Mob")
                        updateData()
                        if (summoningMobs.size == summoningsSpawned) {
                            searchMobs = false
                        }
                    }
                }
        }
    }

    private fun updateData() {
        if (summoningMobs.isEmpty()) return

        for (entry in HashMap(summoningMobs)) {
            val entityLiving = entry.key
            val summoningMob = entry.value

            val currentHealth = entityLiving.health.toInt()
            val name = summoningMob.name
            if (currentHealth == 0) {
                summoningMobs.remove(entityLiving)
                LorenzUtils.chat("§e[SkyHanni] your Summoning Mob just §cdied!")
                continue
            }

            val maxHealth = entityLiving.baseMaxHealth.toInt()
            val color = NumberUtil.percentageColor(currentHealth, maxHealth).getChatColor()

            val currentFormat = NumberUtil.format(currentHealth)
            val maxFormat = NumberUtil.format(maxHealth)
            summoningMob.lastDisplayName = "§a$name $color$currentFormat/$maxFormat"
        }
    }

    @SubscribeEvent
    fun renderOverlay(event: RenderGameOverlayEvent.Post) {
        if (!SkyHanniMod.feature.abilities.summoningMobDisplay) return
        if (summoningMobs.isEmpty()) return

        val list = mutableListOf<String>()
        list.add("Summoning mobs: " + summoningMobs.size)
        var id = 1
        for (mob in summoningMobs) {
            val name = mob.value.lastDisplayName
            list.add("#$id $name")
            id++
        }

        SkyHanniMod.feature.abilities.summoningMobDisplayPos.renderStrings(list)
    }

    @SubscribeEvent
    fun renderOverlay(event: WorldEvent.Load) {
        despawned()
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onRenderLiving(event: RenderLivingEvent.Specials.Pre<EntityLivingBase>) {
        if (!LorenzUtils.inSkyblock) return
        if (!SkyHanniMod.feature.abilities.summoningMobHideNametag) return

        val entity = event.entity
        if (entity !is EntityArmorStand) return
        if (!entity.hasCustomName()) return
        if (entity.isDead) return

        event.isCanceled = entity in summoningMobNametags
    }

    private fun despawned() {
        summoningMobs.clear()
        summoningMobNametags.clear()
        summoningsSpawned = 0
        searchArmorStands = false
        searchMobs = false
        println("despawning")
    }

    private fun isEnabled(): Boolean {
        return LorenzUtils.inSkyblock && (SkyHanniMod.feature.abilities.summoningMobDisplay || SkyHanniMod.feature.abilities.summoningMobHideNametag)
    }

    class SummoningMob(
        val spawnTime: Long,
        var name: String = "",
        var lastDisplayName: String = "",
    )
}