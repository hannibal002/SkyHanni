package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.data.mob.MobData
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.ServerBlockChangeEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.dungeon.DungeonBossRoomEnterEvent
import at.hannibal2.skyhanni.events.dungeon.DungeonCompleteEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockStateAt
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils.onToggle
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzColor.Companion.toLorenzColor
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.MobUtils.mob
import at.hannibal2.skyhanni.utils.RecalculatingValue
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.RenderUtils.drawFilledBoundingBoxNea
import at.hannibal2.skyhanni.utils.RenderUtils.drawLineToEye
import at.hannibal2.skyhanni.utils.RenderUtils.exactBoundingBox
import at.hannibal2.skyhanni.utils.RenderUtils.exactLocation
import at.hannibal2.skyhanni.utils.TimeUtils.ticks
import at.hannibal2.skyhanni.utils.compat.EffectsCompat
import at.hannibal2.skyhanni.utils.compat.EffectsCompat.Companion.activePotionEffect
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.block.BlockStainedGlass
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.Entity
import net.minecraft.init.Blocks
import net.minecraft.item.EnumDyeColor

@SkyHanniModule
object DungeonLividFinder {
    private val config get() = SkyHanniMod.feature.dungeon.lividFinder
    private val blockLocation = LorenzVec(6, 109, 43)

    private val isBlind by RecalculatingValue(2.ticks, ::isCurrentlyBlind)

    var livid: Mob? = null
        private set
    private var lividArmorStandId: Int? = null

    val lividEntityOrArmorstand: Entity?
        get() = livid?.baseEntity ?: lividArmorStandId?.let { EntityUtils.getEntityByID(it) }

    private var fakeLivids = mutableSetOf<Mob>()

    private var color: LorenzColor? = null

    private val lividKillPattern by RepoPattern.pattern(
        "dungeon.f5.lividkill",
        "§c\\[BOSS](?:[\\w ]+)? Livid§r§f: Impossible! How did you figure out which one I was\\?!"
    )

    private val logger = LorenzLogger("livid_finder")

    @HandleEvent
    fun onMobSpawn(event: MobEvent.Spawn.SkyblockMob) {
        if (!inLividBossRoom()) return
        val mob = event.mob
        if (!mob.name.contains("Livid")) return
        if (mob.baseEntity !is EntityOtherPlayerMP) return

        val lividColor = color
        val isCorrectLivid = lividColor != null && mob.isLividColor(lividColor)

        if (lividColor == null) {
            fakeLivids += mob
            return
        }

        if (isCorrectLivid) {
            livid = mob
            lividArmorStandId = mob.armorStand?.entityId
            // When the real livid dies at the same time as a fake livid, Hypixel despawns the player entity,
            // and makes it impossible to get the mob of the real livid again.

            val message = "Livid found: $lividColor§7 | $lividArmorStandId (direct spawn)"
            logger.log(message)
            ChatUtils.debug(message)

            if (config.enabled.get()) mob.highlight(lividColor.toColor())
        } else fakeLivids += mob
    }

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        config.enabled.onToggle {
            reloadHighlight()
        }
    }

    private fun reloadHighlight() {
        val enabled = config.enabled.get()

        if (enabled) {
            livid?.highlight(color?.toColor())
        } else {
            livid?.highlight(null)
        }
    }

    @HandleEvent
    fun onBlockChange(event: ServerBlockChangeEvent) {
        if (!inLividBossRoom()) return
        if (event.location != blockLocation) return
        if (event.location.getBlockAt() != Blocks.wool) return

        val newColor = event.newState.getValue(BlockStainedGlass.COLOR).getColor() ?: run {
            val message = "bad color found! ${event.newState.getValue(BlockStainedGlass.COLOR)}"
            ChatUtils.userError(message)
            logger.log(message)
            event.newState.getValue(BlockStainedGlass.COLOR).toLorenzColor()
        }
        color = newColor

        val colorMessage = "newColor! $newColor"
        ChatUtils.debug(colorMessage)
        logger.log(colorMessage)

        fakeLivids.clear()
        livid = null
        lividArmorStandId = null

        for (mob in MobData.currentMobs) {
            if (!mob.name.contains("Livid")) continue

            mob.highlight(null)
            if (mob.isLividColor(newColor)) {
                livid = mob
                lividArmorStandId = mob.armorStand?.entityId

                val message = "Livid found: $newColor§7 | $lividArmorStandId (color switch)"
                ChatUtils.debug(message)
                logger.log(message)

                if (config.enabled.get()) mob.highlight(newColor.toColor())
                continue
            }

            fakeLivids.add(mob)
        }

        if (livid == null || lividArmorStandId == null) {
            logger.log(MobData.currentMobs.filter { it.name == "Livid" || it.name == "Real Livid" }.joinToString(" | "))
        }
    }

    var bossCount = 1

    @HandleEvent
    fun onBossStart(event: DungeonBossRoomEnterEvent) {
        if (DungeonApi.getCurrentBoss() != DungeonFloor.F5) return
        color = LorenzColor.RED
        logger.log("-----")
        logger.log("start boss $bossCount\n")
        bossCount += 1
    }

    @HandleEvent
    fun onBossEnd(event: DungeonCompleteEvent) {
        reset()
    }

    @HandleEvent
    fun onMobDespawn(event: MobEvent.DeSpawn.SkyblockMob) {
        when (event.mob) {
            livid -> livid = null
            in fakeLivids -> fakeLivids -= event.mob
        }
    }

    @HandleEvent
    fun onWorldChange(event: WorldChangeEvent) {
        color = null
        lividArmorStandId = null
    }

    @HandleEvent(onlyOnIsland = IslandType.CATACOMBS)
    fun onCheckRender(event: CheckRenderEntityEvent<Entity>) {
        if (!inLividBossRoom() || !config.hideWrong) return
        if (livid == null && lividArmorStandId == null) return // in case livid detection fails, don't hide anything
        if (event.entity.mob in fakeLivids) event.cancel()
    }

    private fun isCurrentlyBlind() = (Minecraft.getMinecraft().thePlayer?.activePotionEffect(EffectsCompat.BLINDNESS)?.duration ?: 0) > 10

    private fun Mob.isLividColor(color: LorenzColor): Boolean {
        logger.log("Checking Livid ${this.armorStand?.name} against $color")
        val chatColor = color.getChatColor()
        return armorStand?.name?.startsWith("$chatColor﴾ $chatColor§lLivid") == true
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!inLividBossRoom() || !config.enabled.get()) return
        if (isBlind) return

        val colorOverride = config.colorOverride
        val entity = lividEntityOrArmorstand ?: return
        val lorenzColor =
            if (colorOverride != LividColorHighlight.DEFAULT)
                colorOverride.color ?: return
            else
                color ?: return

        val location = event.exactLocation(entity)
        val boundingBox = event.exactBoundingBox(entity)

        event.drawDynamicText(location, lorenzColor.getChatColor() + "Livid", 1.5)

        val color = lorenzColor.toColor()
        event.drawFilledBoundingBoxNea(boundingBox, color, 0.5f)

        if (location.distanceSqToPlayer() > 50) {
            event.drawLineToEye(location.add(x = 0.5, z = 0.5), color, 3, true)
        }
    }

    private fun inLividBossRoom() = DungeonApi.inBossRoom && DungeonApi.getCurrentBoss() == DungeonFloor.F5

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Livid Finder")

        if (!inLividBossRoom()) {
            event.addIrrelevant {
                add("Not in Livid Boss")
                add("currentBoss: ${DungeonApi.getCurrentBoss()}")
                add("inBossRoom: ${DungeonApi.inBossRoom}")
            }
            return
        }

        // TODO either hide if setting is disabled, or include the info if setting is enabled
        event.addData {
            add("inBoss: ${inLividBossRoom()}")
            add("isBlind: $isBlind")
            add("blockColor: ${blockLocation.getBlockStateAt()}")
            add("livid: '${livid?.armorStand?.name}'")
            add("lividArmorStandID: $lividArmorStandId")
            add("color: ${color?.name}")
        }

        fakeLivids.clear()
        livid = null
        lividArmorStandId = null
        val color = color ?: return
        logger.log("reloading livids")

        for (mob in MobData.currentMobs) {
            if (!mob.name.contains("Livid")) continue

            mob.highlight(null)
            if (mob.isLividColor(color)) {
                livid = mob
                lividArmorStandId = mob.armorStand?.entityId

                val message = "Livid found: ${color}§7 | $lividArmorStandId (color switch)"
                ChatUtils.debug(message)
                logger.log(message)

                if (config.enabled.get()) mob.highlight(color.toColor())
                continue
            }

            fakeLivids.add(mob)
        }
        ChatUtils.chat("reloaded livids!")
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent) {
        if (!inLividBossRoom()) return
        if (!lividKillPattern.matches(event.message)) return

        reset()
    }

    private fun reset() {
        color = null
        livid = null
        lividArmorStandId = null
        fakeLivids.clear()
    }

    private fun EnumDyeColor.getColor(): LorenzColor? =
        when (this) {
            EnumDyeColor.WHITE -> LorenzColor.WHITE

            EnumDyeColor.GRAY -> LorenzColor.GRAY

            EnumDyeColor.MAGENTA -> LorenzColor.LIGHT_PURPLE
            EnumDyeColor.PURPLE -> LorenzColor.DARK_PURPLE

            EnumDyeColor.BLUE -> LorenzColor.BLUE
            EnumDyeColor.RED -> LorenzColor.RED
            EnumDyeColor.YELLOW -> LorenzColor.YELLOW

            EnumDyeColor.LIME -> LorenzColor.GREEN
            EnumDyeColor.GREEN -> LorenzColor.DARK_GREEN

            else -> null
            // these don't exist (for now?)
//             EnumDyeColor.ORANGE
//             EnumDyeColor.LIGHT_BLUE
//             EnumDyeColor.PINK
//             EnumDyeColor.SILVER
//             EnumDyeColor.CYAN
//             EnumDyeColor.BROWN
//             EnumDyeColor.BLACK
        }

    enum class LividColorHighlight(val color: LorenzColor?, val prettyName: String = color?.toString() ?: "Disabled") {
        DEFAULT(null),
        BLACK(LorenzColor.BLACK),
        DARK_BLUE(LorenzColor.DARK_BLUE),
        DARK_GREEN(LorenzColor.DARK_GREEN),
        DARK_AQUA(LorenzColor.DARK_AQUA),
        DARK_RED(LorenzColor.DARK_RED),
        DARK_PURPLE(LorenzColor.DARK_PURPLE),
        GOLD(LorenzColor.GOLD),
        GRAY(LorenzColor.GRAY),
        DARK_GRAY(LorenzColor.DARK_GRAY),
        BLUE(LorenzColor.BLUE),
        GREEN(LorenzColor.GREEN),
        AQUA(LorenzColor.AQUA),
        RED(LorenzColor.RED),
        LIGHT_PURPLE(LorenzColor.LIGHT_PURPLE),
        YELLOW(LorenzColor.YELLOW),
        WHITE(LorenzColor.WHITE),
        ;

        override fun toString() = prettyName
    }
}
