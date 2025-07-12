package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.ServerBlockChangeEvent
import at.hannibal2.skyhanni.events.dungeon.DungeonBossRoomEnterEvent
import at.hannibal2.skyhanni.events.dungeon.DungeonCompleteEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockStateAt
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils.onToggle
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.canBeSeen
import at.hannibal2.skyhanni.utils.EntityUtils.isNpc
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzColor.Companion.toLorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RecalculatingValue
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.RenderUtils.drawFilledBoundingBox
import at.hannibal2.skyhanni.utils.RenderUtils.drawLineToEye
import at.hannibal2.skyhanni.utils.RenderUtils.exactBoundingBox
import at.hannibal2.skyhanni.utils.RenderUtils.exactLocation
import at.hannibal2.skyhanni.utils.TimeUtils.ticks
import at.hannibal2.skyhanni.utils.compat.ColoredBlockCompat.Companion.getBlockColor
import at.hannibal2.skyhanni.utils.compat.ColoredBlockCompat.Companion.isWool
import at.hannibal2.skyhanni.utils.compat.EffectsCompat
import at.hannibal2.skyhanni.utils.compat.EffectsCompat.Companion.activePotionEffect
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityArmorStand

// TODO replace all drawLineToEye with LineToMobHandler

@SkyHanniModule
object DungeonLividFinder {
    private val config get() = SkyHanniMod.feature.dungeon.lividFinder
    private val blockLocation = LorenzVec(6, 109, 43)

    private val isBlind by RecalculatingValue(2.ticks, ::isCurrentlyBlind)

    var livid: EntityOtherPlayerMP? = null
        private set

    private var fakeLivids = mutableSetOf<EntityOtherPlayerMP>()
    private val lividEntities
        get() = EntityUtils.getEntities<EntityOtherPlayerMP>().filter { it.isNpc() && lividNamePattern.matches(it.name) }.toList()

    private var color: LorenzColor? = null
    private val lividNameColor = mapOf(
        "Vendetta" to LorenzColor.WHITE,
        "Doctor" to LorenzColor.GRAY,
        "Crossed" to LorenzColor.LIGHT_PURPLE,
        "Purple" to LorenzColor.DARK_PURPLE,
        "Scream" to LorenzColor.BLUE,
        "Hockey" to LorenzColor.RED,
        "Arcade" to LorenzColor.YELLOW,
        "Smile" to LorenzColor.GREEN,
        "Frog" to LorenzColor.DARK_GREEN,
    )

    /**
     * REGEX-TEST: Doctor Livid
     */
    private val lividNamePattern by RepoPattern.pattern(
        "dungeon.f5.livid.name",
        "^(?<type>\\w+) Livid$",
    )

    /**
     * REGEX-TEST: §2﴾ §2§lLivid§r§r §a7M§c❤ §2﴿
     * REGEX-TEST: §5﴾ §5§lLivid§r§r §a7M§c❤ §5﴿
     */
    private val lividArmorStandNamePattern by RepoPattern.pattern(
        "dungeon.f5.livid.armorstand",
        "^§(?<colorCode>.)﴾ §.§lLivid.*$",
    )

    @HandleEvent(SecondPassedEvent::class)
    fun onSecondPassed() {
        if (!config.enabled.get()) return
        if (!inLividBossRoom()) return
        if (color == null) return

        for (entity in lividEntities) {
            val lividColor = entity.getLividColor() ?: run {
                ErrorManager.logErrorStateWithData(
                    "Unknown Livid found",
                    "No color matches for name",
                    "Livid Name" to entity.name,
                )
                continue
            }
            if (lividColor == color) {
                livid = entity
                entity.highlight(color)
            } else {
                if (entity !in fakeLivids) fakeLivids += entity
            }
        }
    }

    @HandleEvent
    fun onBlockChange(event: ServerBlockChangeEvent) {
        if (!inLividBossRoom()) return
        if (event.location != blockLocation) return
        if (!event.newState.isWool()) return

        val newColor = event.newState.getBlockColor()
        color = newColor
        ChatUtils.debug("newColor! $newColor")

        livid = null
        fakeLivids.clear()

        for (mob in lividEntities) {
            if (mob.isLividColor(LorenzColor.RED) && newColor != LorenzColor.RED) {
                if (mob == livid) {
                    livid = null
                }
                mob.highlight(null)
                fakeLivids += mob
                continue
            }

            if (mob.isLividColor(newColor)) {
                livid = mob
                ChatUtils.debug("Livid found: $newColor§7")
                if (config.enabled.get()) mob.highlight(newColor)
                fakeLivids -= mob
                continue
            }
        }
    }

    @HandleEvent(DungeonBossRoomEnterEvent::class)
    fun onBossStart() {
        if (DungeonApi.getCurrentBoss() != DungeonFloor.F5) return
        color = LorenzColor.RED
    }

    @HandleEvent(DungeonCompleteEvent::class)
    fun onBossEnd() {
        color = null
        livid = null
        fakeLivids.clear()
    }

    @HandleEvent
    fun onWorldChange() {
        color = null
        livid = null
    }

    @HandleEvent(onlyOnIsland = IslandType.CATACOMBS)
    fun onCheckRender(event: CheckRenderEntityEvent<Entity>) {
        if (!inLividBossRoom() || !config.hideWrong) return
        if (livid == null) return // in case livid detection fails, don't hide anything
        if (event.entity is EntityOtherPlayerMP && event.entity in fakeLivids) event.cancel()
        if (event.entity is EntityArmorStand) {
            lividArmorStandNamePattern.matchMatcher(event.entity.name) {
                val colorChar = group("colorCode")[0]

                if (colorChar.toLorenzColor() != color) event.cancel()
            }
        }
    }

    private fun isCurrentlyBlind() = (MinecraftCompat.localPlayerOrNull?.activePotionEffect(EffectsCompat.BLINDNESS)?.duration ?: 0) > 10

    private fun EntityOtherPlayerMP.isLividColor(color: LorenzColor): Boolean {
        val chatColor = color.getChatColor()
        return name.startsWith("$chatColor﴾ $chatColor§lLivid")
    }

    private fun EntityOtherPlayerMP.getLividColor(): LorenzColor? {
        lividNamePattern.matchMatcher(this.name) {
            val type = groupOrNull("type") ?: return null

            return lividNameColor.getOrElse(type) { null }
        }
        return null
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!inLividBossRoom() || !config.enabled.get()) return
        if (isBlind) return

        val entity = livid ?: return
        val lorenzColor =
            if (config.colorOverride != LividColorHighlight.DEFAULT) config.colorOverride.color as LorenzColor else color ?: return

        if (!entity.canBeSeen()) return
        val location = event.exactLocation(entity)
        val boundingBox = event.exactBoundingBox(entity)

        event.drawDynamicText(location, lorenzColor.getChatColor() + "Livid", 1.5)

        val color = lorenzColor.toColor()
        event.drawFilledBoundingBox(boundingBox, color, 0.5f)
        event.drawLineToEye(location.add(x = 0.5, z = 0.5), color, 3, true)
    }

    private fun inLividBossRoom() = DungeonApi.inBossRoom && DungeonApi.getCurrentBoss() == DungeonFloor.F5

    private fun EntityOtherPlayerMP.highlight(color: LorenzColor?) {
        if (color == null) {
            RenderLivingEntityHelper.removeEntityColor(this)
            RenderLivingEntityHelper.removeNoHurtTime(this)
            return
        }

        val newColor = if (config.colorOverride != LividColorHighlight.DEFAULT) config.colorOverride.color as LorenzColor else color

        RenderLivingEntityHelper.setEntityColorWithNoHurtTime(
            entity = this,
            color = newColor.toColor(),
            condition = { this.isLividColor(newColor) },
        )
    }

    @HandleEvent(ConfigLoadEvent::class)
    fun onConfigLoad() {
        config.enabled.onToggle {
            reloadHighlight()
        }
    }

    private fun reloadHighlight() {
        val enabled = config.enabled.get()

        if (enabled) {
            val newLivid = livid ?: return
            val newColor = color ?: return

            RenderLivingEntityHelper.setEntityColorWithNoHurtTime(
                entity = newLivid,
                color = newColor.toColor(),
                condition = { newLivid.isLividColor(newColor) },
            )
        } else {
            RenderLivingEntityHelper.removeEntityColor(livid ?: return)
            RenderLivingEntityHelper.removeNoHurtTime(livid ?: return)
        }
    }

    enum class LividColorHighlight(val color: LorenzColor?, private val prettyName: String = color?.toString() ?: "Disabled") {
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

        event.addData {
            add("isEnabled: ${config.enabled.get()}")
            add("inBoss: ${inLividBossRoom()}")
            add("isBlind: $isBlind")
            add("blockColor: ${blockLocation.getBlockStateAt()}")
            add("livid: '${livid?.name}'")
            add("color: ${color?.name}")
        }
    }
}
