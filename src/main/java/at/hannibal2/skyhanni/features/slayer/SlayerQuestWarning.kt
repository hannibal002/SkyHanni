package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.data.SendTitleHelper
import at.hannibal2.skyhanni.events.EntityHealthUpdateEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.matchRegex
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.entity.EntityLivingBase
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class SlayerQuestWarning {

    private var needSlayerQuest = false
    private var lastWarning = 0L
    private var currentReason = ""
    private var dirtySidebar = false
    private var tick = 0
    private var activeSlayer: SlayerType? = null

    //TODO add check if player has clicked on an item, before mobs around you gets damage

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!isEnabled()) return

        val message = event.message

        //died
        if (message == "  §r§c§lSLAYER QUEST FAILED!") {
            needNewQuest("The old slayer quest has failed!")
        }
        if (message == "§eYour unsuccessful quest has been cleared out!") {
            needSlayerQuest = false
        }

        //no auto slayer
        if (message.matchRegex("   §r§5§l» §r§7Talk to Maddox to claim your (.+) Slayer XP!")) {
            needNewQuest("You have no Auto-Slayer active!")
        }
        if (message == "  §r§a§lSLAYER QUEST COMPLETE!") {
            needSlayerQuest = false
        }

        if (message == "§aYour Slayer Quest has been cancelled!") {
            activeSlayer = null
            needSlayerQuest = false
        }

        //TODO hyp does no damage anymore

        //TODO auto slayer disabled bc of no more money in bank or purse
    }

    private fun needNewQuest(reason: String) {
        currentReason = reason
        needSlayerQuest = true
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!isEnabled()) return

        if (dirtySidebar) {
            if (tick++ % 60 == 0) {
                checkSidebar()
            }
        }
    }

    private fun checkSidebar() {
        var loaded = false

        var slayerQuest = false
        var bossSlain = false
        var slayBoss = false
        var slayerTypeName = ""
        var nextIsType = false
        for (line in ScoreboardData.sidebarLinesFormatted()) {
            if (nextIsType) {
                slayerTypeName = line.removeColor()
                nextIsType = false
            }
            if (line == "Slayer Quest") {
                slayerQuest = true
                nextIsType = true
            }
            if (line == "§aBoss slain!") {
                bossSlain = true
            }
            if (line == "§eSlay the boss!") {
                slayBoss = true
            }
            if (line == "§ewww.hypixel.net" || line == "§ewww.alpha.hypixel.net") {
                loaded = true
            }
        }

        activeSlayer = SlayerType.getByDisplayName(slayerTypeName)

        if (loaded) {
            dirtySidebar = false
            if (slayerQuest && !needSlayerQuest) {
                if (bossSlain) {
                    needNewQuest("You have no Auto-Slayer active!")
                } else if (slayBoss) {
                    needNewQuest("You probably switched the server during an active boss and now hypixel doesn't know what to do.")
                }
            }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        if (!SkyHanniMod.feature.slayer.questWarning) return

        if (!needSlayerQuest) {
            dirtySidebar = true
        }
    }

    private fun tryWarn() {
        if (!needSlayerQuest) return
        warn("New Slayer Quest!", "Start a new slayer quest! $currentReason")
    }

    private fun warn(titleMessage: String, chatMessage: String) {
        if (lastWarning + 10_000 > System.currentTimeMillis()) return

        lastWarning = System.currentTimeMillis()
        LorenzUtils.chat("§e[SkyHanni] $chatMessage")
        SendTitleHelper.sendTitle("§e$titleMessage", 2_000)
    }

    @SubscribeEvent
    fun onEntityHealthUpdate(event: EntityHealthUpdateEvent) {
        if (!isEnabled()) return

        val entity = event.entity
        if (entity.getLorenzVec().distance(LocationUtils.playerLocation()) < 5) {
            if (isSlayerMob(entity)) {
                tryWarn()
            }
        }
    }

    private fun isSlayerMob(entity: EntityLivingBase): Boolean {
        val area = LorenzUtils.skyBlockArea
        val slayerType = SlayerType.getByArea(area) ?: return false

        if (activeSlayer != null) {
            val activeSlayer = activeSlayer!!
            if (slayerType != activeSlayer) {
                val activeSlayerName = activeSlayer.displayName
                val slayerName = slayerType.displayName
                warn(
                    "Wrong Slayer!",
                    "Wrong slayer selected! You have $activeSlayerName selected and are in the $slayerName area!"
                )
            }
        }

        return slayerType.clazz.isInstance(entity)
    }

    private fun isEnabled(): Boolean {
        return LorenzUtils.inSkyBlock && SkyHanniMod.feature.slayer.questWarning
    }
}