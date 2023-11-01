package at.hannibal2.skyhanni.features.summonings

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.RenderMobColoredEvent
import at.hannibal2.skyhanni.events.ResetEntityHurtEvent
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.entity.EntityLiving
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class SummoningMobManager {
    private val config get() = SkyHanniMod.feature.combat.summonings


    private val summoningMobs = mutableMapOf<EntityLiving, SummoningMob>()
    private val summoningMobNametags = mutableListOf<EntityArmorStand>()
    private var summoningsSpawned = 0
    private var searchArmorStands = false
    private var searchMobs = false

    //§aYou have spawned your Tank Zombie §r§asoul! §r§d(249 Mana)
    private val spawnPattern = "§aYou have spawned your (.+) §r§asoul! §r§d\\((\\d+) Mana\\)".toPattern()
    private val despawnPattern = "§cYou have despawned your (monster|monsters)!".toPattern()

    //§a§ohannibal2's Tank Zombie§r §a160k§c❤
    private val healthPattern = "§a§o(.+)'s (.+)§r §[ae]([\\dkm]+)§c❤".toPattern()

    //§cThe Seraph recalled your 3 summoned allies!
    private val seraphRecallPattern = "§cThe Seraph recalled your (\\d) summoned allies!".toPattern()

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock) return

        val message = event.message
        spawnPattern.matchMatcher(message) {
            if (config.summoningMobDisplay) {
                event.blockedReason = "summoning_soul"
            }
            summoningsSpawned++
            searchArmorStands = true
            searchMobs = true
        }

        if (despawnPattern.matcher(message).matches() || message.startsWith("§c ☠ §r§7You ")) {
            despawned()
            if (config.summoningMobDisplay && !message.contains("☠")) {
                event.blockedReason = "summoning_soul"
            }
        }
        if (message == "§cThe Seraph recalled your summoned ally!" || seraphRecallPattern.matcher(message).matches()) {
            despawned()
            if (config.summoningMobDisplay) {
                event.blockedReason = "summoning_soul"
            }
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return

        if (config.summoningMobDisplay && event.repeatSeconds(1)) {
            updateData()
        }

        if (searchArmorStands) {
            EntityUtils.getEntities<EntityArmorStand>().filter { it !in summoningMobNametags }
                .forEach {
                    val name = it.displayName.unformattedText
                    healthPattern.matchMatcher(name) {
                        val playerName = LorenzUtils.getPlayerName()
                        if (name.contains(playerName)) {
                            summoningMobNametags.add(it)
                            if (summoningMobNametags.size == summoningsSpawned) {
                                searchArmorStands = false
                            }
                        }
                    }
                }
        }

        if (searchMobs) {
            val playerLocation = LocationUtils.playerLocation()
            EntityUtils.getEntities<EntityLiving>().filter {
                it !in summoningMobs.keys && it.getLorenzVec()
                    .distance(playerLocation) < 10 && it.ticksExisted < 2
            }.forEach {
                summoningMobs[it] = SummoningMob(System.currentTimeMillis(), name = "Mob")
                updateData()
                if (summoningMobs.size == summoningsSpawned) {
                    searchMobs = false
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
                LorenzUtils.chat("§e[SkyHanni] Your Summoning Mob just §cdied!")
                continue
            }

            val maxHealth = entityLiving.baseMaxHealth
            val color = NumberUtil.percentageColor(currentHealth.toLong(), maxHealth.toLong()).getChatColor()

            val currentFormat = NumberUtil.format(currentHealth)
            val maxFormat = NumberUtil.format(maxHealth)
            summoningMob.lastDisplayName = "§a$name $color$currentFormat/$maxFormat"
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.summoningMobDisplay) return
        if (summoningMobs.isEmpty()) return

        val list = mutableListOf<String>()
        list.add("Summoning mobs: " + summoningMobs.size)
        var id = 1
        for (mob in summoningMobs) {
            val name = mob.value.lastDisplayName
            list.add("#$id $name")
            id++
        }

        config.summoningMobDisplayPos.renderStrings(list, posLabel = "Summoning Mob Display")
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        despawned()
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onRenderLiving(event: RenderLivingEvent.Specials.Pre<EntityLivingBase>) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.summoningMobHideNametag) return

        val entity = event.entity
        if (entity !is EntityArmorStand) return
        if (!entity.hasCustomName()) return
        if (entity.isDead) return

        event.isCanceled = entity in summoningMobNametags
    }

    @SubscribeEvent
    fun onRenderMobColored(event: RenderMobColoredEvent) {
        if (config.summoningMobColored) {
            val entity = event.entity
            if (entity is EntityLiving && entity in summoningMobs.keys) {
                event.color = LorenzColor.GREEN.toColor().withAlpha(127)
            }
        }
    }

    @SubscribeEvent
    fun onResetEntityHurtTime(event: ResetEntityHurtEvent) {
        val entity = event.entity
        if (config.summoningMobColored && entity in summoningMobs.keys) {
            event.shouldReset = true
        }
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "summonings", "combat.summonings")
    }

    private fun despawned() {
        summoningMobs.clear()
        summoningMobNametags.clear()
        summoningsSpawned = 0
        searchArmorStands = false
        searchMobs = false
    }

    private fun isEnabled(): Boolean {
        return LorenzUtils.inSkyBlock && (config.summoningMobDisplay || config.summoningMobHideNametag)
    }

    class SummoningMob(
        val spawnTime: Long,
        var name: String = "",
        var lastDisplayName: String = "",
    )
}
