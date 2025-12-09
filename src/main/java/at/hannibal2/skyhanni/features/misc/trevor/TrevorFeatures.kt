package at.hannibal2.skyhanni.features.misc.trevor

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.Perk
import at.hannibal2.skyhanni.data.mob.MobData
import at.hannibal2.skyhanni.data.title.TitleContext
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.minecraft.KeyPressEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.skyblock.GraphAreaChangeEvent
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.RegexUtils.findMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.compat.command
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawString
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object TrevorFeatures {
    private val patternGroup = RepoPattern.group("misc.trevor")

    /**
     * REGEX-TEST: [NPC] Trevor: You can find your TRACKABLE animal near the §eDesert Mountain.
     */
    private val trapperPattern by patternGroup.pattern(
        "trapper",
        "\\[NPC] Trevor: You can find your (?<rarity>.*) animal near the (?<location>.*)\\.",
    )

    /**
     * REGEX-TEST: The target is around 40 blocks above, at a 45 degrees angle!
     */
    private val talbotPatternAbove by patternGroup.pattern(
        "above",
        "The target is around (?<height>.*) blocks above, at a (?<angle>.*) degrees angle!",
    )

    /**
     * REGEX-TEST: The target is around 15 blocks below, at a 30 degrees angle!
     */
    private val talbotPatternBelow by patternGroup.pattern(
        "below",
        "The target is around (?<height>.*) blocks below, at a (?<angle>.*) degrees angle!",
    )
    private val talbotPatternAt by patternGroup.pattern(
        "at",
        "You are at the exact height!",
    )

    private val mobDiedPattern by patternGroup.pattern(
        "mob.died",
        "§aReturn to the Trapper soon to get a new animal to hunt!",
    )
    private val outOfTimePattern by patternGroup.pattern(
        "outoftime",
        "You ran out of time and the animal disappeared!",
    )
    private val clickOptionPattern by patternGroup.pattern(
        "clickoption",
        "Click an option: §r§a§l\\[YES]§r§7 - §r§c§l\\[NO]",
    )
    private val areaTrappersDenPattern by patternGroup.pattern(
        "area.trappersden",
        "Trapper's Den",
    )

    var state = TrapperState.READY
    var lastTitle: TitleContext? = null
    var inTrapperDen = false
    private var lastChatPromptTime = SimpleTimeMark.farPast()
    private var lastChatPrompt = ""
    private var lastQuestStart = SimpleTimeMark.farPast()
    private var timeLastWarped = SimpleTimeMark.farPast()

    private val totalCooldown get() = if (Perk.PELT_POCALYPSE.isActive) 16.seconds else 21.seconds
    private val WARP_DELAY: Duration = 700.milliseconds
    private const val TRAPPER_ID: Int = 56
    private const val BACKUP_TRAPPER_ID: Int = 17

    private val config get() = SkyHanniMod.feature.misc.trevorTheTrapper

    private fun getDisplayLabel(): String {
        return when (state) {
            TrapperState.COOLDOWN -> {
                val remaining = totalCooldown - lastQuestStart.passedSince()
                val seconds = remaining.inWholeSeconds.coerceAtLeast(0)
                "${TrapperState.COOLDOWN.colorCode}${if (seconds == 1L) "1 second left" else "$seconds seconds left"}"
            }

            else -> state.colorCode + state.label
        }
    }

    @HandleEvent(SecondPassedEvent::class, onlyOnIsland = IslandType.THE_FARMING_ISLANDS)
    fun onSecondPassed() {
        if (state == TrapperState.PENDING && lastChatPromptTime.passedSince() > 5.seconds) {
            state = TrapperState.READY
        }

        if (state == TrapperState.COOLDOWN && lastQuestStart.passedSince() >= totalCooldown) {
            state = TrapperState.READY
            if (config.readyTitle) {
                lastTitle?.stop()
                lastTitle = TitleManager.sendTitle("§2Trapper Ready")
                SoundUtils.playBeepSound()
            }
        }

        TrevorTracker.update()
        TrevorTracker.calculatePeltsPerHour()

        if (config.solver && state == TrapperState.ACTIVE) {
            TrevorSolver.findMob()
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_FARMING_ISLANDS)
    fun onChat(event: SkyHanniChatEvent) {
        val formattedMessage = event.message.removeColor()

        mobDiedPattern.matchMatcher(event.message) {
            TrevorSolver.resetLocation()
            if (config.mobDiedMessage) {
                lastTitle?.stop()
                lastTitle = TitleManager.sendTitle("§2Mob Died")
                SoundUtils.playBeepSound()
            }
            TrevorSolver.mobLocation = TrapperMobArea.NONE
            state = if (lastQuestStart.passedSince() >= totalCooldown) TrapperState.READY else TrapperState.COOLDOWN
        }

        trapperPattern.matchMatcher(formattedMessage) {
            state = TrapperState.ACTIVE
            lastQuestStart = SimpleTimeMark.now()
            TrevorTracker.startQuest(this)
            lastChatPromptTime = SimpleTimeMark.farPast()
            val chatLocation = group("location").removeColor()
            TrevorSolver.mobLocation = TrapperMobArea.entries.firstOrNull { it.location == chatLocation } ?: TrapperMobArea.NONE
        }

        talbotPatternAbove.matchMatcher(formattedMessage) {
            val height = group("height").toInt()
            TrevorSolver.findMobHeight(height, true)
        }
        talbotPatternBelow.matchMatcher(formattedMessage) {
            val height = group("height").toInt()
            TrevorSolver.findMobHeight(height, false)
        }
        talbotPatternAt.matchMatcher(formattedMessage) {
            TrevorSolver.averageHeight = LocationUtils.playerLocation().y
        }

        outOfTimePattern.matchMatcher(formattedMessage) {
            resetTrapper()
        }

        clickOptionPattern.findMatcher(event.message) {
            for (sibling in event.chatComponent.siblings) {
                val clickEvent = sibling.command ?: continue

                if (clickEvent.contains("YES")) {
                    lastChatPromptTime = SimpleTimeMark.now()
                    lastChatPrompt = clickEvent.substringAfter(" ")
                    state = TrapperState.PENDING
                }
            }
        }
    }

    @HandleEvent(GuiRenderEvent.GuiOverlayRenderEvent::class, priority = HandleEvent.LOWEST, onlyOnIsland = IslandType.THE_FARMING_ISLANDS)
    fun onRenderOverlay() {
        if (!config.cooldownGui) return

        val message = when (state) {
            TrapperState.READY -> "Trapper Ready"
            TrapperState.ACTIVE -> "Active Quest"
            TrapperState.COOLDOWN -> {
                val remaining = totalCooldown - lastQuestStart.passedSince()
                val seconds = remaining.inWholeSeconds.coerceAtLeast(0)
                if (seconds == 1L) "1 second left" else "$seconds seconds left"
            }

            TrapperState.PENDING -> "Starting Quest"
        }

        config.cooldownGuiPosition.renderRenderables(
            buildList { addString("${state.colorCode}Trapper: $message") },
            posLabel = "Trapper Cooldown GUI",
        )
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_FARMING_ISLANDS)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        var entityTrapper = EntityUtils.getEntityByID(TRAPPER_ID)
        if (entityTrapper !is EntityLivingBase) entityTrapper = EntityUtils.getEntityByID(BACKUP_TRAPPER_ID)
        if (entityTrapper is EntityLivingBase && config.cooldown) {
            // Solve for the fact that Moby also has the same ID as the Trapper
            val entityMob = MobData.entityToMob[entityTrapper] ?: return
            if (entityMob.name == "Moby") return
            RenderLivingEntityHelper.setEntityColorWithNoHurtTime(entityTrapper, state.color) {
                config.cooldown
            }
            entityTrapper.getLorenzVec().let {
                if (it.distanceToPlayer() < 15) {
                    event.drawString(it.up(2.23), getDisplayLabel())
                }
            }
        }

        if (config.solver) {
            var location = TrevorSolver.mobLocation.coordinates
            if (TrevorSolver.mobLocation == TrapperMobArea.NONE) return
            if (TrevorSolver.averageHeight != 0.0) {
                location = LorenzVec(location.x, TrevorSolver.averageHeight, location.z)
            }
            if (TrevorSolver.mobLocation == TrapperMobArea.FOUND) {
                val displayName = TrevorSolver.currentMob?.mobName ?: "Mob Location"
                location = TrevorSolver.mobCoordinates
                event.drawWaypointFilled(location.down(2), LorenzColor.GREEN.toColor(), seeThroughBlocks = true, beacon = true)
                event.drawDynamicText(location.up(), displayName, 1.5)
            } else {
                event.drawWaypointFilled(location, LorenzColor.GOLD.toColor(), seeThroughBlocks = true, beacon = true)
                event.drawDynamicText(location.up(), TrevorSolver.mobLocation.location, 1.5)
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_FARMING_ISLANDS)
    fun onKeyPress(event: KeyPressEvent) {
        if (Minecraft.getMinecraft().currentScreen != null) return
        if (NeuItems.neuHasFocus()) return

        if (event.keyCode != config.keyBind) return

        if (config.acceptQuest) {
            val timeSince = lastChatPromptTime.passedSince()
            if (timeSince > 200.milliseconds && timeSince < 5.seconds) {
                HypixelCommands.chatPrompt(lastChatPrompt)
                lastChatPrompt = ""
                state = TrapperState.PENDING
                return
            }
        }

        if (timeLastWarped.passedSince() < WARP_DELAY) return

        if (state == TrapperState.ACTIVE && config.warpToClosest) {
            val mobLocation = TrevorSolver.mobLocation
            if (mobLocation == TrapperMobArea.NONE) return
            val mobCoordinates = mobLocation.coordinates

            // Not actual /warp trapper coords, but his cave entrance (better estimation)
            val trapperCoordinates = LorenzVec(287.5, 102.0, -571.5)
            val desertCoordinates = LorenzVec(160.5, 77.0, -370.5)

            val currentDistanceSqToMob = mobCoordinates.distanceSqToPlayer()
            var warpTarget: String? = null
            var bestDistanceSq = currentDistanceSqToMob

            // filter mobLocation to GORGE for safer estimation
            if (mobLocation == TrapperMobArea.GORGE) {
                val distSqFromTrapper = trapperCoordinates.distanceSq(mobCoordinates)
                if (distSqFromTrapper < bestDistanceSq) {
                    bestDistanceSq = distSqFromTrapper
                    warpTarget = "trapper"
                }
            }

            // filter mobLocation to SETTLEMENT and OASIS for safer estimation
            if (mobLocation == TrapperMobArea.SETTLEMENT || mobLocation == TrapperMobArea.OASIS) {
                val distSqFromDesert = desertCoordinates.distanceSq(mobCoordinates)
                if (distSqFromDesert < bestDistanceSq) {
                    bestDistanceSq = distSqFromDesert
                    warpTarget = "desert"
                }
            }

            if (warpTarget != null) {
                HypixelCommands.warp(warpTarget)
                timeLastWarped = SimpleTimeMark.now()
            }
        } else if (config.warpToTrapper && state != TrapperState.PENDING) {
            HypixelCommands.warp("trapper")
            timeLastWarped = SimpleTimeMark.now()
        }
    }

    @HandleEvent(priority = HandleEvent.HIGHEST, onlyOnIsland = IslandType.THE_FARMING_ISLANDS)
    fun onCheckRender(event: CheckRenderEntityEvent<EntityArmorStand>) {
        if (!inTrapperDen || !config.cooldown) return
        if (event.entity.name == "§e§lCLICK") event.cancel()
    }

    private fun resetTrapper() {
        TrevorSolver.resetLocation()
        state = TrapperState.READY
    }

    @HandleEvent
    fun onWorldChange() {
        resetTrapper()
    }

    @HandleEvent(GraphAreaChangeEvent::class)
    fun onGraphAreaChange(event: GraphAreaChangeEvent) {
        inTrapperDen = areaTrappersDenPattern.matches(event.area)
    }

    enum class TrapperState(val label: String, baseColor: LorenzColor) {
        // Ready to be started. Trapper is ready when we first come to him
        READY("Ready", LorenzColor.DARK_GREEN),

        // Quest is initiated, but not started - clicked on trapper, he started dialogue, but [YES] is not clicked yet
        PENDING("Pending", LorenzColor.DARK_AQUA),

        // Quest is active - go kill the mob
        ACTIVE("Active Quest", LorenzColor.DARK_RED),

        // Mob is killed, now waiting for the cooldown (if killed too quickly)
        COOLDOWN("", LorenzColor.DARK_AQUA),
        ;

        val color = baseColor.toColor().addAlpha(75)
        val colorCode = baseColor.getChatColor()
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        val base = "misc.trevorTheTrapper"
        event.move(95, "$base.trapperSolver", "$base.solver")
        event.move(95, "$base.trapperMobDiedMessage", "$base.mobDiedMessage")
        event.move(95, "$base.keyBindWarpTrapper", "$base.keyBind")
        event.move(95, "$base.trapperTalkCooldown", "$base.cooldown")
        event.move(95, "$base.trapperReadyTitle", "$base.readyTitle")
        event.move(95, "$base.trapperCooldownGui", "$base.cooldownGui")
        event.move(95, "$base.trapperCooldownGuiPosition", "$base.cooldownGuiPosition")
    }
}
