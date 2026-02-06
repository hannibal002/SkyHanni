package at.hannibal2.skyhanni.features.event.diana

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.data.ElectionCandidate
import at.hannibal2.skyhanni.data.EntityMovementData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.diana.BurrowDetectEvent
import at.hannibal2.skyhanni.events.diana.BurrowDugEvent
import at.hannibal2.skyhanni.events.diana.BurrowGuessEvent
import at.hannibal2.skyhanni.events.entity.EntityMoveEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.features.event.diana.DianaApi.isDianaSpade
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.BlockUtils.isInLoadedChunk
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.collection.TimeLimitedSet
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.compat.addDoublePlant
import at.hannibal2.skyhanni.utils.compat.addLeaves
import at.hannibal2.skyhanni.utils.compat.addLeaves2
import at.hannibal2.skyhanni.utils.compat.addRedFlower
import at.hannibal2.skyhanni.utils.compat.addTallGrass
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawColor
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawLineToEye
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.toLorenzVec
import io.github.notenoughupdates.moulconfig.ChromaColour
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.level.block.Blocks
import java.awt.Color
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.acos
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object GriffinBurrowHelper {

    private val config get() = SkyHanniMod.feature.event.diana

    val allowedBlocksAboveGround = buildList {
        add(Blocks.AIR)
        add(Blocks.DANDELION)
        add(Blocks.SPRUCE_FENCE)
        addLeaves()
        addLeaves2()
        addTallGrass()
        addDoublePlant()
        addRedFlower()
    }

    private val patternGroup = RepoPattern.group("event.diana.mythological.burrows")

    /**
     * REGEX-TEST: §eYou finished the Griffin burrow chain! §r§7(8/8)
     * REGEX-TEST: §eYou dug out a Griffin Burrow! §r§7(4/8)
     */
    private val burrowDugPattern by patternGroup.pattern(
        "burrow-dug-capture",
        "§eYou (?<type>finished the Griffin burrow chain!|dug out a Griffin Burrow!) §r§7\\((?<current>\\d+)/(?<max>\\d+)\\)",
    )

    /**
     * REGEX-TEST: §c§lUh oh! §r§eYou dug out a §r§2Gaia Construct§r§e!
     * REGEX-TEST: §c§lOi! §r§eYou dug out a §r§2Minos Inquisitor§r§e!
     * REGEX-TEST: §c§lOi! §r§eYou dug out §r§2Siamese Lynxes§r§e!
     * REGEX-TEST: §c§lWoah! §r§eYou dug out a §r§2Cretan Bull§r§e!
     * REGEX-TEST: §c§lDanger! §r§eYou dug out a §r§2Cretan Bull§r§e!
     */
    val genericMythologicalSpawnPattern by patternGroup.pattern(
        "generic-spawn",
        "§c§l(?:Oh|Uh oh|Yikes|Oi|Good Grief|Danger|Woah)! §r§eYou dug out (?:a )?(?:§[a-f0-9r])*(?<creatureType>[\\w\\s]+)§r§e!",
    )

    /**
     * REGEX-TEST: §6§lRARE DROP! §r§eYou dug out a §r§9Mythos Fragment§r§e!
     * REGEX-TEST: §6§lWow! §r§eYou dug out §r§6120,000 coins§r§e!
     * REGEX-TEST: §6§lRARE DROP! §r§eYou dug out a §r§9Griffin Feather§r§e!
     * braided griffin feather may be crazy rare or smth
     */
    val treasureDugPattern by patternGroup.pattern(
        "treasure-dug",
        "§6§l(?:RARE DROP!|Wow!) §r§eYou dug out(?: a)? §r§?.+§r§e!",
    )

    var targetLocation: LorenzVec? = null

    private val allGuessesTimers = mutableMapOf<GuessEntry, SimpleTimeMark>() // hypixel itself removes burrows after 30m
    private val allGuesses = ConcurrentLinkedQueue<GuessEntry>()
    private val recentGuessesRemoved = TimeLimitedSet<LorenzVec>(5.seconds)
    fun getTimer(guessEntry: GuessEntry): SimpleTimeMark? {
        return allGuessesTimers[guessEntry]
    }

    private val recentActionsDebug = ConcurrentLinkedDeque<String>()
    fun addDebug(action: String) {
        recentActionsDebug.addFirst(action)
        if (recentActionsDebug.size > 80) {
            recentActionsDebug.pollLast()
        }
    }

    // used because insta-breaking a block makes it invalid would be better to store valid blocks in repo
    private val recentClickedBlocks = TimeLimitedSet<LorenzVec>(1.seconds)

    private var shouldFocusOnRareMob = false
    var mobAlive = false

    fun removeGuess(location: LorenzVec, reason: String) {
        val toRemove = allGuesses.filter { it.contains(location) }
        for (item in toRemove) {
            removeGuess(item, reason)
        }
    }

    fun removeGuess(guess: GuessEntry, reason: String, logAsPossibleBurrow: Boolean = true) {
        if (allGuesses.contains(guess)) addDebug("removed guess: $guess because $reason")
        allGuesses.remove(guess)
        allGuessesTimers.remove(guess)
        if (logAsPossibleBurrow) recentGuessesRemoved.addAll(guess.guesses)
    }

    fun removeGuess(set: Set<GuessEntry>, reason: String) {
        if (allGuesses.any { set.contains(it) }) addDebug("removed guesses: $set because $reason")
        allGuesses.removeAll(set)
        allGuessesTimers.keys.removeAll(set)
        recentGuessesRemoved.addAll(set.flatMap { it.guesses })
    }

    fun addGuess(guess: GuessEntry, reason: String) {
        getGuess(guess.getCurrent())?.let {
            val existingType = it.burrowType
            if (existingType != BurrowType.UNKNOWN) {
                addDebug("didnt add guess because already exists with type: $guess")
                return
            } else removeGuess(it, "added guess replacing unknown type")
        }

        allGuesses.add(guess)
        allGuessesTimers[guess] = SimpleTimeMark.now()
        addDebug("added guess: $guess because $reason")
    }

    fun getGuess(location: LorenzVec?): GuessEntry? {
        if (location == null) return null
        return allGuesses.toList().firstOrNull { it.contains(location) }
    }

    fun removeInaccurateIfLooking() {
        // remove any inaccurate guesses that the player is looking at
        val inaccurate = allGuesses.filter { it.spadeGuess }.toSet()
        val toDelete = mutableSetOf<GuessEntry>()
        for (item in inaccurate) {
            val player = MinecraftCompat.localPlayer
            val eyePos = player.eyePosition.toLorenzVec()
            val lookAngle = player.lookAngle.toLorenzVec()
            val toTarget = item.getCurrent().minus(eyePos)

            val angle = Math.toDegrees(acos(lookAngle.dotProduct(toTarget.normalize())))
            if (angle < 2.0 && toTarget.length() < 80) toDelete.add(item)
        }
        removeGuess(toDelete, "clicked inaccurate guess")
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Griffin Burrow Helper")

        if (!DianaApi.isDoingDiana()) {
            event.addIrrelevant("not doing diana")
            return
        }

        event.addData {
            add("targetLocation: ${targetLocation?.printWithAccuracy(1)}")
            add("allGuesses: ${allGuesses.size}")
            for (guess in allGuesses) {
                add("  ${guess.getCurrent().printWithAccuracy(1)} (size=${guess.guesses.size}) (type=${guess.burrowType})")
            }
            add("recent actions:")
            for (string in recentActionsDebug) {
                add("  $string")
            }
        }
    }

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return
        update()
    }

    fun update() {
        val newLocation = calculateNewTarget()
        if (targetLocation != newLocation) {
            targetLocation = newLocation
            // TODO: add island graphs here some day when the hub is fully added in the graph
//             newLocation?.let {
//                 IslandGraphs.find(it)
//             }
        }

        if (config.burrowNearestWarp) {
            targetLocation?.let {
                BurrowWarpHelper.shouldUseWarps(it)
            }
        }

        // attempt to move all guesses
        val sb = StringBuilder()
        val toDelete = allGuesses.filter { it.checkRemove(sb) }.toSet()
        removeGuess(toDelete, sb.toString())

        if (!toDelete.isEmpty()) update()
    }

    // TODO add option to only focus on last guess - highly requested method that is less optimal for money per hour. users choice
    // TODO pathfind alg / check closest to any warp point
    private fun calculateNewTarget(): LorenzVec? {
        val locations = mutableListOf<LorenzVec>()

        if (config.rareMobsSharing.enabled) {
            for (waypoint in RareMobWaypointShare.waypoints) {
                locations.add(waypoint.value.location)
            }
        }
        shouldFocusOnRareMob = config.rareMobsSharing.focus && locations.isNotEmpty()
        if (!shouldFocusOnRareMob) {
            allGuesses.forEach { locations.add(it.getCurrent()) }
            locations.addAll(RareMobWaypointShare.waypoints.values.map { it.location })
        }
        val newLocation = locations.minByOrNull { it.distanceToPlayer() }
        return newLocation
    }

    fun showUseSpadeTitle() {
        addDebug("showing use spade title")
        TitleManager.sendTitle("§eUse Spade")
    }

    @HandleEvent
    fun onBurrowGuess(event: BurrowGuessEvent) {
        EntityMovementData.addToTrack(MinecraftCompat.localPlayer)

        val newLocation = event.guess.getCurrent()
        val playerLocation = LocationUtils.playerLocation()

        if (newLocation.distance(playerLocation) < 6) return
        if (!IslandType.HUB.isInBounds(newLocation)) return

        addGuess(event.guess, "burrow guess from ${event.source}")

        update()
    }

    @HandleEvent
    fun onBurrowDetect(event: BurrowDetectEvent) {
        EntityMovementData.addToTrack(MinecraftCompat.localPlayer)
        val burrowLocation = event.burrowLocation
        val currentEntry = getGuess(burrowLocation)

        if (currentEntry != null) removeGuess(currentEntry, "type detected")
        addGuess(GuessEntry(listOf(burrowLocation), event.type, ignoreInvalidBlock = true), "type detected")

        val toDelete = mutableSetOf<GuessEntry>()
        allGuesses.filter { it.spadeGuess }.forEach {
            if (it.getCurrent().distanceSq(burrowLocation) < 2000) {
                toDelete.add(it)
            }
        }
        removeGuess(toDelete, "inaccurate burrow near detected burrow")

        update()
    }

    @Suppress("MaxLineLength")
    @HandleEvent
    fun onBurrowDug(event: BurrowDugEvent) {
        val location = event.burrowLocation
        mobAlive = false
        addDebug("Burrow dug event [${location.x}, ${location.y}, ${location.z}] recently removed burrows size: ${recentGuessesRemoved.size}")
        removeGuess(location, "burrow dug event")

        // finished chain
        if (event.current == event.max && config.warnOnChainComp) {
            val nearby = allGuesses.filter { it.getCurrent().distanceSq(location) < 10 }.toSet()
            removeGuess(nearby, "chain finished with leftover burrow within 3 blocks")
            if (config.warnOnChainComp) {
                val playerLoc = MinecraftCompat.localPlayer.position().toLorenzVec()
                val anyClose = allGuesses.filter { it.getCurrent().distanceSq(playerLoc) < 8100 }
                if (anyClose.isEmpty()) showUseSpadeTitle()
            }
        }

        if (config.guessFromArrow && config.warnOnFail && event.current != event.max) {
            DelayedRun.runDelayed(
                1.seconds,
            ) {
                if (ArrowGuessBurrow.lastArrowTime.passedSince() > 2.seconds) {
                    showUseSpadeTitle()
                }
            }
        }

        update()
    }

    @HandleEvent
    fun onPlayerMove(event: EntityMoveEvent<LocalPlayer>) {
        if (!isEnabled()) return
        if (event.distance > 10 && event.isLocalPlayer) {
            update()
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.HUB)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!isEnabled()) return

        BurrowApi.lastBurrowInteracted?.let {
            if (event.message.startsWith("§c ☠ §r§7You were killed by §r")) {
                DelayedRun.runOrNextTick {
                    mobAlive = false
                    removeGuess(it, "you died L bozo")
                }
            }
            if (it.distanceToPlayer() > 9) {
                addDebug("burrow dug event received at to far away location [${it.x}, ${it.y}, ${it.z}]")
                return
            }

            val burrowDugMatcher = burrowDugPattern.matcher(event.message)
            if (burrowDugMatcher.find()) {
                val current = burrowDugMatcher.group("current").toInt()
                val max = burrowDugMatcher.group("max").toInt()
                DelayedRun.runOrNextTick { BurrowDugEvent(it, current, max).post() }
            } else if (genericMythologicalSpawnPattern.matches(event.message)) {
                DelayedRun.runOrNextTick {
                    mobAlive = true
                    removeGuess(it, "chat mob spawn replacing old")
                    addGuess(GuessEntry(listOf(it), BurrowType.MOB, ignoreInvalidBlock = true), "chat mob spawn")
                }
            } else if (treasureDugPattern.matches(event.message)) {
                DelayedRun.runOrNextTick {
                    removeGuess(it, "chat treasure dug replacing old")
                    addGuess(
                        GuessEntry(
                            listOf(it),
                            BurrowType.START,
                            ignoreParticleCheckUntil = SimpleTimeMark.now() + 3.seconds,
                            ignoreInvalidBlock = true,
                        ),
                        "chat treasure dug",
                    )
                }
            }
        }

        // talking to Diana NPC
        if (event.message == "§6Poof! §r§eYou have cleared your griffin burrows!") {
            DelayedRun.runOrNextTick { resetAllData() }
        }
    }

    private fun resetAllData() {
        addDebug("reset all data")
        allGuesses.clear()
        allGuessesTimers.clear()
        targetLocation = null
        GriffinBurrowParticleFinder.reset()
        mobAlive = false

        BurrowWarpHelper.currentWarp = null
        if (isEnabled()) {
            update()
        }
    }

    @HandleEvent
    fun onWorldChange() {
        DelayedRun.runOrNextTick {
            if (mobAlive) {
                BurrowApi.lastBurrowInteracted?.let { removeGuess(it, "changed worlds while mob was alive") }
                mobAlive = false
            }
            if (config.clearOnWorldChange) resetAllData()
        }
    }

    @HandleEvent
    fun onProfileChange(event: ProfileJoinEvent) {
        DelayedRun.runOrNextTick { resetAllData() }
    }

    fun isBlockValid(pos: LorenzVec): Boolean {
        if (!pos.isInLoadedChunk()) {
            return true
        }
        val isGround = recentClickedBlocks.contains(pos) || pos.getBlockAt() == Blocks.GRASS_BLOCK
        val isValidBlockAbove = pos.up().getBlockAt() in allowedBlocksAboveGround
        return isGround && isValidBlockAbove
    }

    fun shouldBurrowParticlesBeVisible(timeInPast: Duration = 2.seconds): Boolean {
        val spade = InventoryUtils.getItemInHand()?.isDianaSpade == true
        val time = InventoryUtils.lastItemChangeTime.passedSince()
        return spade && time > timeInPast
    }

    fun removeSpadeWarnTitle() {
        if (config.warnOnFail || config.warnOnChainComp) {
            TitleManager.conditionallyStopTitle { currentTitle ->
                currentTitle == "§eUse Spade"
            }
        }
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return

        val playerLocation = LocationUtils.playerLocation()
        if (config.rareMobsSharing.enabled) {
            renderRareMobs(event, playerLocation)
        }

        val currentWarp = BurrowWarpHelper.currentWarp
        if (config.lineToNext) {
            var color: ChromaColour?
            val renderLocation = if (currentWarp != null) {
                color = LorenzColor.AQUA.toChromaColor()
                currentWarp.location
            } else {
                color = if (shouldFocusOnRareMob) LorenzColor.LIGHT_PURPLE.toChromaColor() else LorenzColor.WHITE.toChromaColor()
                targetLocation?.blockCenter() ?: return
            }

            val targetType = getGuess(targetLocation)?.burrowType
            val lineWidth = if (targetType != null && targetType != BurrowType.UNKNOWN) {
                color = targetType.color
                3
            } else 2
            if (currentWarp == null) {
                event.drawLineToEye(renderLocation, color, lineWidth, false)
            }
        }

        if (RareMobWaypointShare.waypoints.isNotEmpty() && config.rareMobsSharing.focus) {
            return
        }

        if (config.multiGuesses) {
            renderAllGuesses(event, playerLocation)
        } else {
            val target = getGuess(targetLocation) ?: return
            val location = target.getCurrent()
            val distance = location.distance(playerLocation)
            val text = when (target.burrowType) {
                BurrowType.UNKNOWN -> "${if (currentWarp != null) "§b" else "§f"}Guess"
                else -> target.burrowType.text
            }

            event.drawColor(location, target.burrowType.color, config.beaconDistance != -1.0F && distance > config.beaconDistance)
            event.drawDynamicText(location.up(), text, 1.5 * config.textScale)
        }

    }

    private fun renderRareMobs(event: SkyHanniRenderWorldEvent, playerLocation: LorenzVec) {
        for (rareMob in RareMobWaypointShare.waypoints.values) {
            val location = rareMob.location
            // TODO add chroma color support via config
            event.drawColor(location, LorenzColor.LIGHT_PURPLE.toChromaColor())
            val distance = location.distance(playerLocation)
            if (distance > 10) {
                // TODO use round(1)
                val formattedDistance = distance.toInt().addSeparators()
                event.drawDynamicText(location.up(), "§d§l${rareMob.mobName} §e${formattedDistance}m", 1.7 * config.textScale)
            } else {
                event.drawDynamicText(location.up(), "§d§l${rareMob.mobName}", 1.7 * config.textScale)
            }
            if (distance < 5) {
                RareMobWaypointShare.maybeRemove(rareMob)
            }
            event.drawDynamicText(location.up(), "§eFrom §b${rareMob.playerDisplayName}", 1.6 * config.textScale, yOff = 9f)

            if (config.rareMobsSharing.showDespawnTime) {
                val spawnTime = rareMob.spawnTime
                val format = (75.seconds - spawnTime.passedSince()).format()
                event.drawDynamicText(location.up(), "§eDespawns in §b$format", 1.6 * config.textScale, yOff = 18f)
            }
        }
    }

    private fun renderAllGuesses(event: SkyHanniRenderWorldEvent, playerLocation: LorenzVec) {
        for (guess in allGuesses.toList()) {
            val location = guess.getCurrent()
            val distance = location.distance(playerLocation)
            val burrowType = guess.burrowType
            var text = burrowType.text

            if (!config.burrowsNearbyDetection) {
                if (burrowType != BurrowType.UNKNOWN) return
            }

            if (burrowType == BurrowType.UNKNOWN) {
                if (!config.guess) return
                else {
                    val textColor = if (BurrowWarpHelper.currentWarp != null && targetLocation == location) "§b" else "§f"
                    text = "${textColor}Guess"
                    if (distance > 5) {
                        val formattedDistance = distance.toInt().addSeparators()
                        event.drawDynamicText(location.up(), "§e${formattedDistance}m", 1.7 * config.textScale, yOff = 10f)
                    }
                }
            }

            if (config.renderSubGuesses) {
                var lineStart = location
                for (subGuess in guess.guesses.drop(guess.currentIndex + 1)) {
                    event.drawColor(subGuess, Color.LIGHT_GRAY.toChromaColor(), false)
                    event.draw3DLine(lineStart, subGuess, Color.LIGHT_GRAY.toChromaColor(), 1, false)
                    lineStart = subGuess
                }
            }

            // TODO add chroma color support via config
            event.drawColor(location, burrowType.color, config.beaconDistance != -1.0F && distance > config.beaconDistance)
            event.drawDynamicText(location.up(), text, 1.5 * config.textScale)
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "diana", "event.diana")
    }

    @HandleEvent(onlyOnIsland = IslandType.HUB)
    fun onBlockClick(event: BlockClickEvent) {
        if (!isEnabled()) return

        val location = event.position

        getGuess(location)?.let {
            if (event.itemInHand?.isDianaSpade == true && it.burrowType == BurrowType.UNKNOWN && it.getCurrent() == location) {
                DelayedRun.runDelayed(
                    200.milliseconds,
                    {
                        if (BurrowApi.lastBurrowRelatedChatMessage.passedSince() > 400.milliseconds) it.attemptMove()
                    },
                )
            }
        }

        val burrows = allGuesses.toList().flatMap { it.guesses }.union(recentGuessesRemoved)
        if (burrows.contains(location)) BurrowApi.setBurrowInteracted(location)
    }

    private fun isEnabled() = DianaApi.isDoingDiana()

    private fun setTestBurrow(arg: String) {
        if (!IslandType.HUB.isCurrent()) {
            ChatUtils.userError("You can only create test burrows on the hub island!")
            return
        }

        if (!isEnabled()) {
            if (!ElectionCandidate.DIANA.isActive()) {
                ChatUtils.chatAndOpenConfig(
                    "§cSelect Diana as mayor overwrite!",
                    SkyHanniMod.feature.dev.debug::assumeMayor,
                )

            } else {
                ChatUtils.userError("Have an Ancestral Spade in the inventory!")
            }
            return
        }

        val type: BurrowType = when (arg) {
            "reset" -> {
                DelayedRun.runOrNextTick { resetAllData() }
                ChatUtils.chat("Manually reset all burrow data.")
                return
            }

            "1", "start" -> BurrowType.START
            "2", "mob" -> BurrowType.MOB
            "3", "treasure" -> BurrowType.TREASURE
            else -> {
                ChatUtils.userError("Unknown burrow type! Try 1-3 instead.")
                return
            }
        }

        EntityMovementData.addToTrack(MinecraftCompat.localPlayer)
        val location = LocationUtils.playerLocation().roundLocation()
        addGuess(GuessEntry(listOf(location), burrowType = type), "added test burrow from command")
        update()
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shresetburrows") {
            description = "Resets all saved griffin burrow locations"
            category = CommandCategory.USERS_RESET
            callback {
                resetAllData()
                ChatUtils.chat("Manually reset all burrow data.")
            }
        }

        event.registerBrigadier("shtestburrow") {
            description = "Sets a test burrow waypoint at your location"
            category = CommandCategory.DEVELOPER_TEST
            arg("type", BrigadierArguments.string()) { type ->
                callback { DelayedRun.runOrNextTick { setTestBurrow(getArg(type)) } }
            }
        }
        event.registerBrigadier("shtestburrowchain") {
            category = CommandCategory.DEVELOPER_TEST
            simpleCallback {
                DelayedRun.runOrNextTick {
                    addGuess(
                        GuessEntry(
                            listOf(
                                LorenzVec(-143, 69, 62),
                                LorenzVec(-137, 69, 68),
                                LorenzVec(-132, 69, 73),
                                LorenzVec(-125, 69, 80),
                                LorenzVec(-117, 69, 88),
                                LorenzVec(-107, 69, 98),
                                LorenzVec(-92, 69, 113),
                                LorenzVec(-88, 69, 123),
                                LorenzVec(-78, 69, 136),
                            ),
                        ),
                        "added test burrow chain from command",
                    )
                }
            }

        }
        event.registerBrigadier("shtestburrowchainenddetect") {
            category = CommandCategory.DEVELOPER_TEST
            simpleCallback {
                DelayedRun.runOrNextTick {
                    BurrowDetectEvent(LorenzVec(-88, 69, 123), BurrowType.TREASURE).post()
                }
            }
        }
    }
}
