package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.ItemClickEvent
import at.hannibal2.skyhanni.events.entity.EntityHealthUpdateEvent
import at.hannibal2.skyhanni.events.slayer.SlayerStateChangeEvent
import at.hannibal2.skyhanni.features.event.diana.DianaApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalNames
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.world.entity.LivingEntity
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object SlayerQuestWarning {

    private val config get() = SlayerApi.config

    private var lastWeaponUse = SimpleTimeMark.farPast()
    private val teleportItems = setOf("ASPECT_OF_THE_END", "ASPECT_OF_THE_VOID").toInternalNames()

    @HandleEvent(onlyOnSkyblock = true)
    fun onSlayerStateChange(event: SlayerStateChangeEvent) {
        if (event.state == SlayerApi.ActiveQuestState.GRINDING) {
            needSlayerQuest = false
        }
        if (event.state == SlayerApi.ActiveQuestState.FAILED) {
            needNewQuest("The old slayer quest has failed!")
        }
        if (event.state == SlayerApi.ActiveQuestState.SLAIN) {
            DelayedRun.runDelayed(5.seconds) {
                if (SlayerApi.state == SlayerApi.ActiveQuestState.SLAIN) {
                    needNewQuest("You have no Auto-Slayer active!")
                }
            }
        }
    }

    private var needSlayerQuest = false
    private var lastWarning = SimpleTimeMark.farPast()
    private var currentReason = ""

    private fun needNewQuest(reason: String) {
        currentReason = reason
        needSlayerQuest = true
    }

    private fun tryWarn() {
        if (!needSlayerQuest) return
        warn("New Slayer Quest!", "Start a new slayer quest! $currentReason")
    }

    private fun warn(titleMessage: String, chatMessage: String) {
        if (!config.questWarning) return
        if (lastWarning.passedSince() < 10.seconds) return

        if (DianaApi.isDoingDiana()) return
        // prevent warnings when mobs are hit by other players
        if (lastWeaponUse.passedSince() > 500.milliseconds) return

        lastWarning = SimpleTimeMark.now()
        ChatUtils.chat(chatMessage)

        if (config.questWarningTitle) {
            TitleManager.sendTitle("§e$titleMessage", duration = 2.seconds)
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onEntityHealthUpdate(event: EntityHealthUpdateEvent) {
        val entity = event.entity
        if (entity.getLorenzVec().distanceToPlayer() < 6 && isSlayerMob(entity)) {
            tryWarn()
        }
    }

    private fun isSlayerMob(entity: LivingEntity): Boolean {
        val slayerType = SlayerApi.currentAreaType ?: return false

        // workaround for rift mob that is unrelated to slayer
        if (entity.name.string == "Oubliette Guard") return false
        // workaround for Bladesoul in Crimson Isle
        if (SkyBlockUtils.scoreboardArea == "Stronghold" && entity.name.string == "Skeleton") return false

        val isSlayer = slayerType.clazz.isInstance(entity)
        if (!isSlayer) return false

        SlayerApi.activeType?.let {
            if (slayerType != it) {
                val activeSlayerName = it.displayName
                val slayerName = slayerType.displayName
                SlayerApi.latestWrongAreaWarning = SimpleTimeMark.now()
                warn(
                    "Wrong Slayer!",
                    "Wrong slayer selected! You have $activeSlayerName selected and you are in an $slayerName area!",
                )
            }
        }

        return SlayerApi.activeType == slayerType
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onItemClick(event: ItemClickEvent) {
        val internalName = event.itemInHand?.getInternalNameOrNull()

        if (event.clickType == ClickType.RIGHT_CLICK) {
            if (internalName in teleportItems) {
                // ignore harmless teleportation
                return
            }
            if (internalName == null) {
                // ignore harmless right click
                return
            }
        }
        lastWeaponUse = SimpleTimeMark.now()
    }
}
