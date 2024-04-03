package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.dungeon.DragPrioConfig
import at.hannibal2.skyhanni.data.MayorAPI
import at.hannibal2.skyhanni.data.Perk
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.DungeonCompleteEvent
import at.hannibal2.skyhanni.events.DungeonStartEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzKeyPressEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.LocationUtils.isInside
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.client.Minecraft
import net.minecraft.entity.boss.EntityDragon
import net.minecraft.network.Packet
import net.minecraft.network.play.server.S2APacketParticles
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.EnumParticleTypes
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import net.minecraftforge.event.entity.living.LivingDeathEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class DungeonDragonPriority {

    private val config get() = SkyHanniMod.feature.dungeon.dragPrio

    private val startPattern by RepoPattern.pattern(
        "dungeons.startphase5",
        "(.+)§r§a picked the §r§cCorrupted Blue Relic§r§a!"
    )

    enum class DragonInfo(
        val color: String,
        var status: SpawnedStatus,
        val isEasy: Boolean,
        val priority: IntArray,
        val spawnBox: AxisAlignedBB,
        val deathBox: AxisAlignedBB,
        var id: Int,
        val colorCode: Char
    ) {
        POWER("Red", SpawnedStatus.UNDEFEATED, false, intArrayOf(1, 3), AxisAlignedBB(24.0, 15.0, 30.0, 56.0, 22.0, 62.0), AxisAlignedBB(14.5, 13.0, 45.5, 39.5, 28.0, 70.5), -1, LorenzColor.RED.chatColorCode),
        FLAME("Orange", SpawnedStatus.UNDEFEATED, true, intArrayOf(2, 1), AxisAlignedBB(82.0, 15.0, 88.0, 56.0, 22.0, 62.0), AxisAlignedBB(70.0, 8.0, 47.0, 102.0, 28.0, 77.0), -1, LorenzColor.GOLD.chatColorCode),
        APEX("Green", SpawnedStatus.UNDEFEATED, true, intArrayOf(5, 2), AxisAlignedBB(24.0, 15.0, 30.0, 91.0, 22.0, 97.0), AxisAlignedBB(7.0, 8.0, 80.0, 37.0, 28.0, 110.0), -1, LorenzColor.GREEN.chatColorCode),
        ICE("Blue", SpawnedStatus.UNDEFEATED, false, intArrayOf(3, 4), AxisAlignedBB(82.0, 15.0, 88.0, 91.0, 22.0, 97.0), AxisAlignedBB(71.5, 16.0, 82.5, 96.5, 26.0, 107.5), -1, LorenzColor.AQUA.chatColorCode),
        SOUL("Purple", SpawnedStatus.UNDEFEATED, true, intArrayOf(4, 5), AxisAlignedBB(53.0, 15.0, 59.0, 122.0, 22.0, 128.0), AxisAlignedBB(45.5, 13.0, 113.5, 68.5, 23.0, 136.5), -1, LorenzColor.LIGHT_PURPLE.chatColorCode),
        NONE("None", SpawnedStatus.UNDEFEATED, false, intArrayOf(0, 0), AxisAlignedBB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0), AxisAlignedBB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0), -1, LorenzColor.CHROMA.chatColorCode);

        companion object {
            fun clearSpawned() {
                entries.forEach { it.status = SpawnedStatus.UNDEFEATED }
            }
        }
    }

    enum class SpawnedStatus {
        UNDEFEATED,
        SPAWNING,
        ALIVE,
        DEFEATED;
    }

    private var inBerserkTeam = false
    private var inArcherTeam = false
    private var isHealer = false
    private var isTank = false

    private var isSearching = false
    private val particleList = mutableListOf<String>()
    private var titleString = ""

    private var shouldSplit = true
    private var keyBindCoolDown = SimpleTimeMark.now()

    private var dragonOrder = arrayOf(DragonInfo.NONE, DragonInfo.NONE)

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!config.enabled) return
        if (!DungeonAPI.inDungeon()) return
        if (DungeonAPI.dungeonFloor != "M7") return
        if (!startPattern.matches(event.message)) return
        reset()
        ChatUtils.debug("starting p5")
        startP5()
    }

    private fun startP5() {
        if (!config.saySplit) return
        val currentClass = DungeonAPI.playerClass
        if (config.forceClass == DragPrioConfig.ForceClass.NONE) when (currentClass) {
            DungeonAPI.DungeonClass.MAGE -> inBerserkTeam = true
            DungeonAPI.DungeonClass.BERSERK -> inBerserkTeam = true
            DungeonAPI.DungeonClass.ARCHER -> inArcherTeam = true
            DungeonAPI.DungeonClass.TANK -> {
                inArcherTeam = true
                isTank = true
            }

            DungeonAPI.DungeonClass.HEALER -> isHealer = true
            else -> return
        } else {
            when (config.forceClass) {
                DragPrioConfig.ForceClass.ARCHER -> inArcherTeam = true
                DragPrioConfig.ForceClass.BERSERK -> inBerserkTeam = true
                DragPrioConfig.ForceClass.MAGE -> inBerserkTeam = true
                DragPrioConfig.ForceClass.TANK -> {
                    inArcherTeam = true
                    isTank = true
                }
                DragPrioConfig.ForceClass.HEALER -> isHealer = true
                else -> return
            }
        }
        DelayedRun.runDelayed(2000.milliseconds) {
            val currentPower = getPower()
            when {
                currentPower >= config.splitPower -> ChatUtils.chat("Power: $currentPower | Split on all drags!")
                currentPower >= config.easyPower -> ChatUtils.chat("Power: $currentPower | Split on easy drags!")
                else -> ChatUtils.chat("Power: $currentPower | No split!")
            }
        }
        isSearching = true
    }

    private fun reset() {
        DragonInfo.clearSpawned()
        dragonOrder = arrayOf(DragonInfo.NONE, DragonInfo.NONE)
        inArcherTeam = false
        inBerserkTeam = false
        isHealer = false
        isTank = false
        particleList.clear()
    }

    private fun checkCoordinates(particle: Packet<*>) {
        if (particle !is S2APacketParticles) return
        val vec = particle.toLorenzVec()
        particleList.add("Position: $vec | Type: ${particle.particleType} | Count: ${particle.particleCount} | Speed: ${particle.particleSpeed} | Offset: ${particle.xOffset} ${particle.yOffset} ${particle.zOffset} | LongDistance: ${particle.isLongDistance}")
        if (particle.particleType != EnumParticleTypes.FLAME) return
        DragonInfo.entries.forEach {
            if (it.status == SpawnedStatus.UNDEFEATED && it.spawnBox.isInside(vec)) {
                particleList.add("matched ${it.name}")
                assignDragon(it)
            }
        }
    }

    private fun assignDragon(dragon: DragonInfo) {
        ChatUtils.debug("try spawning ${dragon.name}")
        dragon.status = SpawnedStatus.SPAWNING
        when (DragonInfo.NONE) {
            dragonOrder[0] -> {
                ChatUtils.debug("${dragon.name} is now dragon0")
                dragonOrder[0] = dragon
            }

            dragonOrder[1] -> {
                ChatUtils.debug("${dragon.name} is now dragon1")
                dragonOrder[1] = dragon
                determinePriority()
            }

            else -> {
                ChatUtils.debug("dragonOrder was full")
                if (config.showSingleDragons) sendTitle("§${dragon.colorCode}${dragon.color} is Spawning!")
            }
        }
    }

    private fun determinePriority() {
        val power = getPower()
        val isEasy: Boolean = dragonOrder[0].isEasy && dragonOrder[1].isEasy
        val trySplit = if ((!isEasy && power >= config.splitPower) || (isEasy && power >= config.easyPower)) 1 else 0

        ChatUtils.debug("isEasy: $isEasy")
        ChatUtils.debug("trySplit = $trySplit")
        ChatUtils.debug("shouldSplit = $shouldSplit")
        val split = if (shouldSplit) trySplit else 0
        val (berserkDragon, archerDragon) = if (dragonOrder[0].priority[split] < dragonOrder[1].priority[split]) {
            dragonOrder[0] to dragonOrder[1]
        } else {
            dragonOrder[1] to dragonOrder[0]
        }
        displayDragons(berserkDragon, archerDragon, split)
    }

    private fun displayDragons( //TODO: cleanup this
        berserkDragon: DragonInfo,
        archerDragon: DragonInfo,
        split: Int
    ) {
        val purple = DragonInfo.SOUL in dragonOrder
        ChatUtils.debug("berserkDragon: ${berserkDragon.name} | archerDragon: ${archerDragon.name}")
        if (split == 1 && config.sendMessage) {
            ChatUtils.chat("Berserk Team: ${berserkDragon.color} (send in pc)")
            ChatUtils.chat("Archer Team: ${archerDragon.color} (send in pc)")
        }

        if (inBerserkTeam || (purple && (
                    (isHealer && config.healerPurple == DragPrioConfig.HealerPurpleValue.BERSERK)
                            || (isTank && config.tankPurple == DragPrioConfig.TankPurpleValue.BERSERK)))
        ) sendTitle("§${berserkDragon.colorCode}${berserkDragon.color.uppercase()} is Spawning!")
        else sendTitle("§${archerDragon.colorCode}${archerDragon.color.uppercase()} is Spawning!")
    }

    @SubscribeEvent
    fun onParticle(event: PacketEvent.ReceiveEvent) {
        if (!config.enabled) return
        if (!DungeonAPI.inDungeon()) return
        if (DungeonAPI.dungeonFloor != "M7") return
        if (!isSearching) return
        if (event.packet !is S2APacketParticles) return
        checkCoordinates(event.packet)
    }

    @SubscribeEvent
    fun onDungeonStart(event: DungeonStartEvent) {
        if (DungeonAPI.dungeonFloor != "F1") return
        if (isSearching) return
        isSearching = true
    }

    @SubscribeEvent
    fun onDungeonEnd(event: DungeonCompleteEvent) {
        if (particleList.isNotEmpty()) {
            val stringList = particleList.joinToString("\n")
            particleList.clear()
            ChatUtils.clickableChat("i want particles again sorry",
                { OSUtils.copyToClipboard(stringList); ChatUtils.chat("copied") })
        }
        if (!config.enabled) return
        if (!isSearching) return
        isSearching = false
    }

    @SubscribeEvent
    fun onDebugCollect(event: DebugDataCollectEvent) {
        event.title("Dragon Priority")
        if (!DungeonAPI.inDungeon() || DungeonAPI.dungeonFloor != "M7") {
            event.addIrrelevant("not in m7")
            return
        }

        event.addData {
            add("Power: ${getPower()}")
            add("inArchTeam: $inArcherTeam")
            add("inBersTeam: $inBerserkTeam")
            add("isHealer: $isHealer")
            add("isTank: $isTank")
            add("isSearching: $isSearching")
            add("dragon0: ${dragonOrder[0].name}")
            add("dragon1: ${dragonOrder[1].name}")
        }
    }

    private fun sendTitle(input: String) {
        titleString = input
        val duration: Duration = config.titleDuration.toDouble().seconds
        DelayedRun.runDelayed(duration) {
            titleString = ""
        }
    }

    @SubscribeEvent
    fun onRender(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.enabled) return
        if (!DungeonAPI.inDungeon()) return
        if (DungeonAPI.dungeonFloor != "M7") return
        if (titleString == "") return
        config.dragonSpawnedPosition.renderString(titleString, posLabel = "Dragon Priority Title")
    }

    @SubscribeEvent
    fun onDragonSpawn(event: EntityJoinWorldEvent) {
        if (!config.enabled) return
        if (!isSearching) return
        if (event.entity !is EntityDragon) return
        ChatUtils.debug("Dragon spawned at ${event.entity.position.toLorenzVec()} with id ${event.entity.entityId}")
        DragonInfo.entries.filter { it.status == SpawnedStatus.SPAWNING }.forEach {
            if (it.spawnBox.isInside(event.entity.position.toLorenzVec())) {
                it.status = SpawnedStatus.ALIVE
                it.id = event.entity.entityId
            }
        }
    }

    @SubscribeEvent
    fun onDragonDeath(event: LivingDeathEvent) {
        if (!config.enabled) return
        if (!isSearching) return
        if (event.entity !is EntityDragon) return
        ChatUtils.debug("Dragon died at ${event.entity.position.toLorenzVec()} with id ${event.entity.entityId}")
        DragonInfo.entries.filter { it.id == event.entity.entityId }.forEach {
            if (it.deathBox.isInside(event.entity.position.toLorenzVec())) {
                ChatUtils.debug("${it.color} died inside box")
                it.status = SpawnedStatus.DEFEATED
            } else {
                ChatUtils.debug("${it.color} died outside box")
                it.status = SpawnedStatus.UNDEFEATED
            }
            it.id = -1
        }
    }

    @SubscribeEvent
    fun onKeyClick(event: LorenzKeyPressEvent) {
        if (!config.enabled) return
        if (event.keyCode != config.keyBindToggleModes) return
        if (Minecraft.getMinecraft().currentScreen != null) return
        if (NEUItems.neuHasFocus()) return
        if (!DungeonAPI.inDungeon()) return
        if (DungeonAPI.dungeonFloor != "M7") return
        if (keyBindCoolDown.passedSince() < 1.seconds) return
        keyBindCoolDown = SimpleTimeMark.now()

        if (shouldSplit) {
            shouldSplit = false
            ChatUtils.chat("Now in Normal Priority mode!")
        } else {
            shouldSplit = true
            ChatUtils.chat("Now in Split Priority mode!")
        }
        SoundUtils.playPlingSound()
    }

    private fun getPower(): Double {
        val paulBoost = if (MayorAPI.currentMayor?.activePerks?.contains(Perk.BENEDICTION) == true) 1.25 else 1.0
        return (DungeonAPI.DungeonBlessings.valueOf("POWER").power
                + (DungeonAPI.DungeonBlessings.valueOf("TIME").power.toDouble() / 2)) * paulBoost
    }
}