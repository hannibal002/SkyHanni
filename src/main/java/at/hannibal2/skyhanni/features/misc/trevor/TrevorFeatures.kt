package at.hannibal2.skyhanni.features.misc.trevor

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.title.TitleContext
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.entity.EntityEnterWorldEvent
import at.hannibal2.skyhanni.events.entity.EntityLeaveWorldEvent
import at.hannibal2.skyhanni.events.minecraft.KeyPressEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.skyblock.GraphAreaChangeEvent
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.EntityUtils.getSkinTexture
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RegexUtils.findMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.fromNow
import at.hannibal2.skyhanni.utils.SkullTextureHolder
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.compat.command
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawString
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.roundedUpSeconds
import net.minecraft.client.Minecraft
import net.minecraft.client.player.RemotePlayer
import net.minecraft.world.entity.decoration.ArmorStand
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object TrevorFeatures {

    private val patternGroup = RepoPattern.group("misc.trevor")

    // <editor-fold desc="Patterns">
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

    /**
     * REGEX-TEST: Location: Mushroom Gorge
     */
    private val locationPattern by patternGroup.pattern(
        "zone",
        "Location: (?<zone>.*)",
    )
    private val mobDiedPattern by patternGroup.pattern(
        "mob.died.colorless",
        "Return to the Trapper soon to get a new animal to hunt!",
    )
    private val outOfTimePattern by patternGroup.pattern(
        "outoftime",
        "You ran out of time and the animal disappeared!",
    )
    private val clickOptionPattern by patternGroup.pattern(
        "clickoption.colorless",
        "Click an option: \\[YES] - \\[NO]",
    )
    private val areaTrappersDenPattern by patternGroup.pattern(
        "area.trappersden",
        "Trapper's Den",
    )
    private val clickArmorStandPattern by patternGroup.pattern(
        "click.armorstand",
        "CLICK",
    )
    // </editor-fold>

    private val config get() = SkyHanniMod.feature.misc.trevorTheTrapper

    // TODO form to data class, use Resettable
    private var nextReadyTime = SimpleTimeMark.farPast()
    private val timeUntilNextReady get() = nextReadyTime.timeUntil().roundedUpSeconds.coerceAtLeast(0)
    private var trapperReady: Boolean = true
    private var currentStatus = TrapperStatus.READY
    private var currentLabel = "§2Ready"
    private var timeLastWarped = SimpleTimeMark.farPast()
    private var lastChatPrompt = ""
    private var lastChatPromptTime = SimpleTimeMark.farPast()

    private val trevorTexture by SkullTextureHolder.texture("TREVOR")
    private var trevorEntity: RemotePlayer? = null

    var questActive = false
    var inBetweenQuests = false
    var inTrapperDen = false
    var lastTitle: TitleContext? = null

    @HandleEvent(onlyOnIsland = IslandType.THE_FARMING_ISLANDS)
    fun onTick() {
        updateTrapper()
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_FARMING_ISLANDS)
    fun onSecondPassed() {
        TrevorTracker.update()
        TrevorTracker.calculatePeltsPerHour()
        if (config.solver && questActive) {
            TrevorSolver.findMob()
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_FARMING_ISLANDS)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        val formattedMessage = event.cleanMessage

        mobDiedPattern.matchMatcher(formattedMessage) {
            TrevorSolver.resetLocation()
            TalbotCircles.resetCircles()
            if (config.mobDiedMessage) {
                lastTitle?.stop()
                lastTitle = TitleManager.sendTitle("§2Mob Died")
                SoundUtils.playBeepSound()
            }
            trapperReady = true
            TrevorSolver.mobLocation = TrapperMobArea.NONE
            if (nextReadyTime.isInPast()) {
                currentStatus = TrapperStatus.READY
                currentLabel = "§2Ready"
            } else {
                currentStatus = TrapperStatus.WAITING
                currentLabel = if (timeUntilNextReady == 1) "§31 second left" else "§3$timeUntilNextReady seconds left"
            }
            TrevorSolver.mobLocation = TrapperMobArea.NONE
        }

        trapperPattern.matchMatcher(formattedMessage) {
            nextReadyTime = 20.seconds.fromNow()
            currentStatus = TrapperStatus.ACTIVE
            currentLabel = "§cActive Quest"
            trapperReady = false
            TrevorTracker.startQuest(this)
            lastChatPromptTime = SimpleTimeMark.farPast()
        }

        talbotPatternAbove.matchMatcher(formattedMessage) {
            val height = group("height").toInt()
            val angle = group("angle").toInt()
            TrevorSolver.findMobHeight(height, true)
            TalbotCircles.addResult(height, angle)
        }
        talbotPatternBelow.matchMatcher(formattedMessage) {
            val height = group("height").toInt()
            val angle = group("angle").toInt()
            TrevorSolver.findMobHeight(height, false)
            TalbotCircles.addResult(-height, angle)
        }
        talbotPatternAt.matchMatcher(formattedMessage) {
            TrevorSolver.averageHeight = LocationUtils.playerLocation().y
        }

        outOfTimePattern.matchMatcher(formattedMessage) {
            resetTrapper()
        }

        clickOptionPattern.findMatcher(formattedMessage) {
            for (sibling in event.chatComponent.siblings) {
                val clickEvent = sibling.command ?: continue

                if (clickEvent.contains("YES")) {
                    lastChatPromptTime = SimpleTimeMark.now()
                    lastChatPrompt = clickEvent.substringAfter(" ")
                }
            }
        }
    }

    @HandleEvent
    fun onTabListUpdate(event: TabListUpdateEvent) {
        var found = false
        var active = false
        val previousLocation = TrevorSolver.mobLocation
        // TODO work with trapper widget, widget api, repo patterns, when not found, warn in chat and don't update
        event.tabList.forEach { line ->
            val formattedLine = line.string.drop(1)
            if (formattedLine.startsWith("Time Left: ")) {
                trapperReady = false
                currentStatus = TrapperStatus.ACTIVE
                currentLabel = "§cActive Quest"
                active = true
            }

            TrapperMobArea.entries.firstOrNull { it.location == formattedLine }?.let {
                TrevorSolver.mobLocation = it
                found = true
            }
            locationPattern.matchMatcher(formattedLine) {
                val zone = group("zone")
                TrevorSolver.mobLocation = TrapperMobArea.entries.firstOrNull { it.location == zone } ?: TrapperMobArea.NONE
                found = true
            }
        }
        if (!found) TrevorSolver.mobLocation = TrapperMobArea.NONE

        if (!active) trapperReady = true
        else inBetweenQuests = true

        if (TrevorSolver.mobCoordinates != LorenzVec(0.0, 0.0, 0.0) && active) {
            TrevorSolver.mobLocation = previousLocation
        }
        questActive = active
    }

    @HandleEvent(GuiRenderEvent.GuiOverlayRenderEvent::class, priority = HandleEvent.LOWEST, onlyOnIsland = IslandType.THE_FARMING_ISLANDS)
    fun onGuiRenderOverlay() {
        if (!config.cooldownGui) return

        val cooldownMessage = if (nextReadyTime.isInPast()) "Trapper Ready"
        else if (timeUntilNextReady == 1) "1 second left"
        else "$timeUntilNextReady seconds left"

        val display = Renderable.text("${currentStatus.colorCode}Trapper Cooldown: $cooldownMessage")
        config.cooldownGuiPosition.renderRenderable(display, posLabel = "Trapper Cooldown GUI")
    }

    private fun updateTrapper() {
        if (trapperReady && nextReadyTime.isInFuture()) {
            currentStatus = TrapperStatus.WAITING
            currentLabel = if (timeUntilNextReady == 1) "§31 second left" else "§3$timeUntilNextReady seconds left"
        }

        if (nextReadyTime.isInPast() && trapperReady) {
            if (!nextReadyTime.isFarPast()) {
                nextReadyTime = SimpleTimeMark.farPast()
                if (config.readyTitle) {
                    lastTitle?.stop()
                    lastTitle = TitleManager.sendTitle("§2Trapper Ready")
                    SoundUtils.playBeepSound()
                }
            }
            currentStatus = TrapperStatus.READY
            currentLabel = "§2Ready"
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_FARMING_ISLANDS)
    fun onEntityEnterWorld(event: EntityEnterWorldEvent<RemotePlayer>) {
        if (trevorTexture != null && event.entity.getSkinTexture() == trevorTexture) trevorEntity = event.entity
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_FARMING_ISLANDS)
    fun onEntityLeaveWorld(event: EntityLeaveWorldEvent<RemotePlayer>) {
        if (event.entity == trevorEntity) trevorEntity = null
    }

    private fun renderCooldown(event: SkyHanniRenderWorldEvent) {
        val entity = trevorEntity ?: return

        RenderLivingEntityHelper.setEntityColor(entity, currentStatus.color) {
            config.cooldown
        }
        entity.getLorenzVec().let {
            if (it.distanceToPlayer() < 15) {
                event.drawString(it.up(2.23), currentLabel)
            }
        }
    }

    private fun findMob(event: SkyHanniRenderWorldEvent): Boolean {
        if (!config.solver) return false
        if (TrevorSolver.mobLocation == TrapperMobArea.NONE) return false

        var location = TrevorSolver.mobLocation.coordinates
        if (TrevorSolver.averageHeight != 0.0) {
            location = LorenzVec(location.x, TrevorSolver.averageHeight, location.z)
        }

        val found = TrevorSolver.mobLocation == TrapperMobArea.FOUND
        if (found) {
            val displayName = TrevorSolver.currentMob?.mobName ?: "Mob Location"
            location = TrevorSolver.mobCoordinates
            event.drawWaypointFilled(location.down(2), LorenzColor.GREEN.toColor(), seeThroughBlocks = true, beacon = true)
            event.drawDynamicText(location.up(), displayName, 1.5)
        } else {
            event.drawWaypointFilled(location, LorenzColor.GOLD.toColor(), seeThroughBlocks = true, beacon = true)
            event.drawDynamicText(location.up(), TrevorSolver.mobLocation.location, 1.5)
        }

        return found
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_FARMING_ISLANDS)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (config.cooldown) renderCooldown(event)
        val mobFound = findMob(event)
        if (config.talbotCircles && !mobFound) TalbotCircles.drawCircles(event)
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_FARMING_ISLANDS)
    fun onKeyPress(event: KeyPressEvent) {
        if (Minecraft.getInstance().screen != null) return

        if (event.keyCode != config.keyBind) return

        if (config.acceptQuest) {
            val timeSince = lastChatPromptTime.passedSince()
            if (timeSince > 200.milliseconds && timeSince < 5.seconds) {
                lastChatPromptTime = SimpleTimeMark.farPast()
                HypixelCommands.chatPrompt(lastChatPrompt)
                lastChatPrompt = ""
                timeLastWarped = SimpleTimeMark.now()
                return
            }
        }

        if (config.warpToTrapper && timeLastWarped.passedSince() > 3.seconds) {
            HypixelCommands.warp("trapper")
            timeLastWarped = SimpleTimeMark.now()
        }
    }

    @HandleEvent(priority = HandleEvent.HIGHEST, onlyOnIsland = IslandType.THE_FARMING_ISLANDS)
    fun onCheckRender(event: CheckRenderEntityEvent<ArmorStand>) {
        if (!inTrapperDen || !config.cooldown) return
        if (clickArmorStandPattern.matches(event.entity.name.string)) event.cancel()
    }

    private fun resetTrapper() {
        TrevorSolver.resetLocation()
        TalbotCircles.resetCircles()
        currentStatus = TrapperStatus.READY
        currentLabel = "§2Ready"
        questActive = false
        inBetweenQuests = false
        trevorEntity = null
    }

    @HandleEvent
    fun onWorldChange() {
        resetTrapper()
    }

    @HandleEvent
    fun onGraphAreaChange(event: GraphAreaChangeEvent) {
        inTrapperDen = areaTrappersDenPattern.matches(event.area)
    }

    enum class TrapperStatus(baseColor: LorenzColor) {
        READY(LorenzColor.DARK_GREEN),
        WAITING(LorenzColor.DARK_AQUA),
        ACTIVE(LorenzColor.DARK_RED),
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

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shcleartalbotcircles") {
            description = "Clears Talbot circles"
            category = CommandCategory.USERS_RESET
            simpleCallback { TalbotCircles.resetCircles() }
        }
    }
}
