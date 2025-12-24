package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.jsonobjects.repo.LividSolverJson
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.ServerBlockChangeEvent
import at.hannibal2.skyhanni.events.dungeon.DungeonBossRoomEnterEvent
import at.hannibal2.skyhanni.events.dungeon.DungeonCompleteEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.AllEntitiesGetter
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockStateAt
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils.onToggle
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.canBeSeen
import at.hannibal2.skyhanni.utils.EntityUtils.getSkinTexture
import at.hannibal2.skyhanni.utils.EntityUtils.isNpc
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzColor.Companion.toLorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RecalculatingValue
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.TimeUtils.ticks
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.add
import at.hannibal2.skyhanni.utils.compat.ColoredBlockCompat.Companion.getBlockColor
import at.hannibal2.skyhanni.utils.compat.ColoredBlockCompat.Companion.isWool
import at.hannibal2.skyhanni.utils.compat.EffectsCompat
import at.hannibal2.skyhanni.utils.compat.EffectsCompat.Companion.activePotionEffect
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawFilledBoundingBox
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawLineToEye
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.exactBoundingBox
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.exactLocation
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.player.RemotePlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand

// TODO replace all drawLineToEye with LineToMobHandler

@SkyHanniModule
object DungeonLividFinder {
    private val config get() = SkyHanniMod.feature.dungeon.lividFinder
    private val blockLocation = LorenzVec(6, 109, 43)

    private val isBlind by RecalculatingValue(2.ticks, ::isCurrentlyBlind)

    var livid: RemotePlayer? = null
        private set

    private var fakeLivids = mutableSetOf<RemotePlayer>()

    // This only happens when in f5/m5 bossfight, so the performance impact is minimal
    @OptIn(AllEntitiesGetter::class)
    private val lividEntities: List<RemotePlayer>
        get() = EntityUtils.getEntities<RemotePlayer>().filterTo(mutableListOf()) { it.isNpc() }

    private var color: LorenzColor? = null

//     @Suppress("LineTooLong")
//     private var lividTextureToColor = mapOf(
//         "ewogICJ0aW1lc3RhbXAiIDogMTU5ODk3NzMyNzkxMiwKICAicHJvZmlsZUlkIiA6ICIzZmM3ZmRmOTM5NjM0YzQxOTExOTliYTNmN2NjM2ZlZCIsCiAgInByb2ZpbGVOYW1lIiA6ICJZZWxlaGEiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmEwNGM4Yzg4N2UzOThkMzMyMGQzOTUwNTdjODdiMWUwMmI3OTViMTBiYmIzOGY3ZTJhOGNmYmZjMDc4YTE2OCIKICAgIH0KICB9Cn0=" to LorenzColor.WHITE,
//         "ewogICJ0aW1lc3RhbXAiIDogMTU5ODk3NzQzNjUwMSwKICAicHJvZmlsZUlkIiA6ICI3ZGEyYWIzYTkzY2E0OGVlODMwNDhhZmMzYjgwZTY4ZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJHb2xkYXBmZWwiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDQ4ZTQzZjg0MmYzMTY2NTFlNTFhNTc5N2NhYTMyYmZhOWRlODFhOGMyMzg0YmQ2YzBkMWM0N2M0NDgwM2M5MSIKICAgIH0KICB9Cn0=" to LorenzColor.GRAY,
//         "ewogICJ0aW1lc3RhbXAiIDogMTU5ODk3NzM0ODg4MSwKICAicHJvZmlsZUlkIiA6ICIxNzhmMTJkYWMzNTQ0ZjRhYjExNzkyZDc1MDkzY2JmYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJzaWxlbnRkZXRydWN0aW9uIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzdmMWFiZmQwNzE3NTExMTVmYTEwMDBjOWQ3NmQxMDk3M2ZmMzI3NzMxNDZjZDE0MDY4NjRiYWFmMzc4MTZlOWEiCiAgICB9CiAgfQp9" to LorenzColor.LIGHT_PURPLE,
//         "ewogICJ0aW1lc3RhbXAiIDogMTU5ODk3NzQxNTQyNSwKICAicHJvZmlsZUlkIiA6ICJmYThiNGRmYWMxZTg0Mzg5YmFkZTIzYTE0Zjk1ZTRkNyIsCiAgInByb2ZpbGVOYW1lIiA6ICJkZXZ2YXJhcmdzIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzJmMDVmYjRiZGI1NzgwNzc4ZmU0NDYxMjgzZWRkZmFiNzI3M2I5NmQ0Njc1NDdlOGJjYTdlYzEwMTM1N2U2NmYiCiAgICB9CiAgfQp9" to LorenzColor.DARK_PURPLE,
//         "ewogICJ0aW1lc3RhbXAiIDogMTU5ODk3NzM2ODIxMiwKICAicHJvZmlsZUlkIiA6ICJmNWQwYjFhZTQxNmU0YTE5ODEyMTRmZGQzMWU3MzA1YiIsCiAgInByb2ZpbGVOYW1lIiA6ICJDYXRjaFRoZVdhdmUxMCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS82MTA4ODU0Mzk0YzgwZmVkNDE4OTU4Mjg3ZGU1ODEyMDlmZDY5ZmZmM2U2M2NiM2M4ODFjMzRiZmE4MThjOWUiCiAgICB9CiAgfQp9" to LorenzColor.BLUE,
//         "ewogICJ0aW1lc3RhbXAiIDogMTU5ODk3NzI4MzQ1NiwKICAicHJvZmlsZUlkIiA6ICI5MWYwNGZlOTBmMzY0M2I1OGYyMGUzMzc1Zjg2ZDM5ZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJTdG9ybVN0b3JteSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9hMTE2ZGJhYmQ3Njk1N2E1MDBkYjhmMzQ2NDcwZDc5NjQ3M2YyNDU1N2Y3ZjlkM2Y0ZTJhYzNmN2M4NDM5ZWEzIgogICAgfQogIH0KfQ==" to LorenzColor.RED,
//         "ewogICJ0aW1lc3RhbXAiIDogMTU5ODk3NzMwMjU4MSwKICAicHJvZmlsZUlkIiA6ICJiYWE1Yjg0YzA2NGM0NTBlYjU2NTU4ZDQxOWVmYTkzMSIsCiAgInByb2ZpbGVOYW1lIiA6ICJDYW1lbGxpYWFkYW1zIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzMxY2M2NDA4ZTVhMjY4ZTZjZWIyZjhiOWFmYjZlZWZkNGE5NGI3ZWI0Nzg4MzgyNmJkNmMzNTRmYzNkY2E5NzMiCiAgICB9CiAgfQp9" to LorenzColor.YELLOW,
//         "ewogICJ0aW1lc3RhbXAiIDogMTU5ODk3NzM5MTM5NCwKICAicHJvZmlsZUlkIiA6ICI2ZmQyNGJlNDk4ZjA0MDJlOTZhYWQ2MWUzY2VmYjZmMCIsCiAgInByb2ZpbGVOYW1lIiA6ICJBbmdlbGFsbHhfIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzRjM2MwMjQ4OGU2M2I1ZTY3NTg0YWE5Nzc2ZDVlYTU2YmFhNjk2NWE3MzNhNjhmNzAwY2E4YjA4ODkxMWEyYjciCiAgICB9CiAgfQp9" to LorenzColor.GREEN,
//         "ewogICJ0aW1lc3RhbXAiIDogMTU5ODk3NzIzNjA1MiwKICAicHJvZmlsZUlkIiA6ICIyNmM1MmQzZjgxMzQ0ZjUzYmNhYzA0Mjc4ODBiZDVjNCIsCiAgInByb2ZpbGVOYW1lIiA6ICJBbWJpZ3VvdXNCaXZhbHZlIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzliZmY0ZDY1OWQ5ODVlNTFmNDIxOTU1YWM4NzcwNGE5YjYxMjJjYjZhMTY5ZDliMDQ4Y2RkNmFiMWUxYjBiNTciCiAgICB9CiAgfQp9" to LorenzColor.DARK_GREEN,
//     )

    private var lividTextureToColor = mutableMapOf<String, LorenzColor>()
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
     * REGEX-TEST: §2﴾ §2§lLivid§r§r §a7M§c❤ §2﴿
     * REGEX-TEST: §5﴾ §5§lLivid§r§r §a7M§c❤ §5﴿
     */
    private val lividArmorStandNamePattern by RepoPattern.pattern(
        "dungeon.f5.livid.armorstand",
        "^§(?<colorCode>.)﴾ §.§lLivid.*$",
    )


    /**
     * REGEX-TEST: Doctor Livid
     */
    private val lividNamePattern by RepoPattern.pattern(
        "dungeon.f5.livid.name",
        "^(?<type>\\w+) Livid$",
    )

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<LividSolverJson>("dungeons/LividSolver")
        val map = data.lividSkins
        for ((color, skin) in map) {
            if (!lividTextureToColor.containsKey(skin)) {
                val repoColor = LorenzColor.entries.firstOrNull { it.name == color } ?: continue
                lividTextureToColor.add(Pair(skin, repoColor))
            }
        }
    }


    @HandleEvent(SecondPassedEvent::class)
    fun onSecondPassed() {
        if (!config.enabled.get()) return
        if (!inLividBossRoom()) return
        if (color == null) return

        for (entity in lividEntities) {
            val lividColor = entity.getLividColor() ?: run {
                lividNamePattern.matchMatcher(entity.name.toString()) {
                    val namecolor = lividNameColor[group("name")] ?: continue
                    ErrorManager.logErrorStateWithData(
                        "Unknown Livid found",
                        "No color matches for texture",
                        "Livid Texture & Livid Name with associated Color" to "${entity.getSkinTexture()} $namecolor ${group("name")}",
                    )

                    continue
                }
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
        if (event.entity is RemotePlayer && event.entity in fakeLivids) event.cancel()
        if (event.entity is ArmorStand) {
            lividArmorStandNamePattern.matchMatcher(event.entity.name.formattedTextCompatLessResets()) {
                val colorChar = group("colorCode")[0]

                if (colorChar.toLorenzColor() != color) event.cancel()
            }
        }
    }

    private fun isCurrentlyBlind() = (MinecraftCompat.localPlayerOrNull?.activePotionEffect(EffectsCompat.BLINDNESS)?.duration ?: 0) > 10

    private fun RemotePlayer.isLividColor(color: LorenzColor): Boolean {
        val chatColor = color.getChatColor()
        return name.formattedTextCompatLessResets().startsWith("$chatColor﴾ $chatColor§lLivid")
    }

    private fun RemotePlayer.getLividColor(): LorenzColor? {
        val texture = this.getSkinTexture() ?: return null
        return lividTextureToColor.getOrElse(texture) { return null }
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!inLividBossRoom() || !config.enabled.get()) return
        if (isBlind) return

        val entity = livid ?: return
        val lorenzColor =
            if (config.colorOverride != LividColorHighlight.DEFAULT) config.colorOverride.color as LorenzColor else color ?: return

        if (!entity.canBeSeen(150, 0.5, true)) return
        val location = event.exactLocation(entity)
        val boundingBox = event.exactBoundingBox(entity)

        event.drawDynamicText(location, lorenzColor.getChatColor() + "Livid", 1.5)

        val color = lorenzColor.toChromaColor()
        event.drawFilledBoundingBox(boundingBox, color, 0.5f)
        event.drawLineToEye(location.add(x = 0.5, z = 0.5), color, 3, true)
    }

    private fun inLividBossRoom() = DungeonApi.inBossRoom && DungeonApi.getCurrentBoss() == DungeonFloor.F5

    private fun RemotePlayer.highlight(color: LorenzColor?) {
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
            add("livid: '${livid?.name.formattedTextCompatLessResets()}'")
            add("color: ${color?.name}")
        }
    }
}
