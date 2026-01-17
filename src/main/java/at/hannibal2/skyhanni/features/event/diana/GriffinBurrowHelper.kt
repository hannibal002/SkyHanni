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
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
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
    private val allGuesses = mutableListOf<GuessEntry>()

    // used because insta-breaking a block makes it invalid would be better to store valid blocks in repo
    private val recentClickedBlocks = TimeLimitedSet<LorenzVec>(1.seconds)

    private var shouldFocusOnRareMob = false
    private var mobAlive = false

    data class GuessEntry(
        val guesses: List<LorenzVec>,
        var burrowType: BurrowType = BurrowType.UNKNOWN,
        var currentIndex: Int = 0,
        var ignoreParticleCheckUntil: SimpleTimeMark = SimpleTimeMark.now(),
    ) {
        fun getCurrent(): LorenzVec = guesses[currentIndex]
        fun contains(vec: LorenzVec): Boolean {
            return guesses.contains(vec)
        }

        fun checkRemove(): Boolean {
            // remove guesses older than 30 minutes
            allGuessesTimers[this]?.passedSince()?.let {
                if (it > 30.minutes) {
                    return true
                }
            }

            if (shouldKeepGuess()) return false

            var shouldMove = false
            if (!isBlockValid(this.getCurrent())) shouldMove = true

            val now = SimpleTimeMark.now()
            val shouldBeLoaded = InventoryUtils.getItemInHandDuringTimeframe(now - 0.3.seconds, now - 0.8.seconds)?.isDianaSpade
            if (shouldBeLoaded == true &&
                !GriffinBurrowParticleFinder.containsBurrow(this.getCurrent()) && // burrow is not found
                this.getCurrent().distanceSq(MinecraftCompat.localPlayer.position().toLorenzVec()) < 900 // within 30 blocks
            ) {
                shouldMove = true
            }

            if (shouldMove) {
                val nextIndex = currentIndex + 1
                if (nextIndex in guesses.indices) {
                    currentIndex = nextIndex
                    BurrowGuessEvent(this).post()
                    return false
                } else return true // remove if it should have moved but cant
            }
            return false
        }

        private fun shouldKeepGuess(): Boolean {

            // burrows that are known from the previous dug even if particles don't update
            if (ignoreParticleCheckUntil.passedSince() < 0.milliseconds) return true

            // don't attempt to move mob burrows if a mob is alive
            if (mobAlive && this.burrowType == BurrowType.MOB) return true

            return false
        }
    }

    fun removeGuess(location: LorenzVec) {
        val toRemove = allGuesses.filter { it.contains(location) }
        for (item in toRemove) {
            removeGuess(item)
        }
    }

    fun removeGuess(guess: GuessEntry) {
        allGuesses.remove(guess)
        allGuessesTimers.remove(guess)
    }

    fun addGuess(guess: GuessEntry) {
        allGuesses.add(guess)
        allGuessesTimers[guess] = SimpleTimeMark.now()
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
        val toDelete = allGuesses.filter { it.checkRemove() }.toSet()
        allGuesses.removeAll(toDelete)
        allGuessesTimers.keys.removeAll(toDelete)

        if (!toDelete.isEmpty()) update()
    }

    // TODO add option to only focus on last guess - highly requested method that is less optimal for money per hour. users choice
    // TODO pathfind alg / check closest to any warp point
    private fun calculateNewTarget(): LorenzVec? {
        val locations = mutableListOf<LorenzVec>()

        if (config.inquisitorSharing.enabled) {
            for (waypoint in RareMobWaypointShare.waypoints) {
                locations.add(waypoint.value.location)
            }
        }
        shouldFocusOnRareMob = config.inquisitorSharing.focusInquisitor && locations.isNotEmpty()
        if (!shouldFocusOnRareMob) {
            allGuesses.forEach { locations.add(it.getCurrent()) }
            locations.addAll(RareMobWaypointShare.waypoints.values.map { it.location })
        }
        val newLocation = locations.minByOrNull { it.distanceToPlayer() }
        return newLocation
    }

    @HandleEvent
    fun onBurrowGuess(event: BurrowGuessEvent) {
        EntityMovementData.addToTrack(MinecraftCompat.localPlayer)

        if (allGuesses.flatMap { it.guesses }.any { event.guess.contains(it) }) {
            return
        }

        val newLocation = event.guess.getCurrent()
        val playerLocation = LocationUtils.playerLocation()

        if (newLocation.distance(playerLocation) < 6) return
        if (!IslandType.HUB.isInBounds(newLocation)) return

        addGuess(event.guess)

        update()
    }

    @HandleEvent
    fun onBurrowDetect(event: BurrowDetectEvent) {
        EntityMovementData.addToTrack(MinecraftCompat.localPlayer)
        val burrowLocation = event.burrowLocation
        val currentEntry = allGuesses.firstOrNull { it.contains(burrowLocation) }

        if (currentEntry == null) addGuess(GuessEntry(listOf(burrowLocation), event.type))
        else {
            val correctIndex = currentEntry.guesses.indices // safe because of the .contains and null checks above
                .first { index -> currentEntry.guesses[index] == burrowLocation }
            currentEntry.burrowType = event.type
            currentEntry.currentIndex = correctIndex
        }

        update()
    }

    @HandleEvent
    fun onBurrowDug(event: BurrowDugEvent) {
        val location = event.burrowLocation
        mobAlive = false
        removeGuess(location)

        // finished chain
        if (event.current == event.max && config.warnOnChainComp) {
            // finished chain
            if (config.warnOnChainComp) {
                val playerLoc = MinecraftCompat.localPlayer.position().toLorenzVec()
                val anyClose = allGuesses.filter { it.getCurrent().distanceSq(playerLoc) < 8100 }
                if (anyClose.isEmpty()) TitleManager.sendTitle("§eUse Spade")
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
    fun onChat(event: SkyHanniChatEvent) {
        if (!isEnabled()) return

        if (event.message.startsWith("§c ☠ §r§7You were killed by §r")) {
            mobAlive = false
            BurrowApi.lastBurrowInteracted?.let { removeGuess(it) }
        }

        BurrowApi.lastBurrowInteracted?.let {
            val burrowDugMatcher = burrowDugPattern.matcher(event.message)
            if (burrowDugMatcher.find()) {
                val current = burrowDugMatcher.group("current").toInt()
                val max = burrowDugMatcher.group("max").toInt()
                BurrowDugEvent(it, current, max).post()
            } else if (genericMythologicalSpawnPattern.matches(event.message)) {
                mobAlive = true
                removeGuess(it)
                addGuess(GuessEntry(listOf(it), BurrowType.MOB, ignoreParticleCheckUntil = SimpleTimeMark.now() + 2.seconds))
            } else if (treasureDugPattern.matches(event.message)) {
                removeGuess(it)
                addGuess(GuessEntry(listOf(it), BurrowType.START, ignoreParticleCheckUntil = SimpleTimeMark.now() + 2.seconds))
            }
        }

        // talking to Diana NPC
        if (event.message == "§6Poof! §r§eYou have cleared your griffin burrows!") {
            resetAllData()
        }
    }

    private fun resetAllData() {
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
        mobAlive = false
        if (config.clearOnWorldChange) resetAllData()
    }

    @HandleEvent
    fun onProfileChange(event: ProfileJoinEvent) {
        resetAllData()
    }

    fun isBlockValid(pos: LorenzVec): Boolean {
        if (!pos.isInLoadedChunk()) {
            return true
        }
        val isGround = recentClickedBlocks.contains(pos) || pos.getBlockAt() == Blocks.GRASS_BLOCK
        val isValidBlockAbove = pos.up().getBlockAt() in allowedBlocksAboveGround
        return isGround && isValidBlockAbove
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
        if (config.inquisitorSharing.enabled) {
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

            val targetType = allGuesses.firstOrNull { it.getCurrent() == targetLocation }?.burrowType
            val lineWidth = if (targetType != null && targetType != BurrowType.UNKNOWN) {
                color = targetType.color
                3
            } else 2
            if (currentWarp == null) {
                event.drawLineToEye(renderLocation, color, lineWidth, false)
            }
        }

        if (RareMobWaypointShare.waypoints.isNotEmpty() && config.inquisitorSharing.focusInquisitor) {
            return
        }

        if (config.multiGuesses) {
            renderAllGuesses(event, playerLocation)
        } else {
            val target = allGuesses.firstOrNull { it.getCurrent() == targetLocation }
            if (target == null) return
            val location = target.getCurrent()
            val distance = location.distance(playerLocation)
            val text = when (target.burrowType) {
                BurrowType.UNKNOWN -> "${if (currentWarp != null) "§b" else "§f"}Guess"
                else -> target.burrowType.text
            }

            event.drawColor(location, target.burrowType.color, distance > 10)
            event.drawDynamicText(location.up(), text, 1.5)
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
                event.drawDynamicText(location.up(), "§d§l${rareMob.mobName} §e${formattedDistance}m", 1.7)
            } else {
                event.drawDynamicText(location.up(), "§d§l${rareMob.mobName}", 1.7)
            }
            if (distance < 5) {
                RareMobWaypointShare.maybeRemove(rareMob)
            }
            event.drawDynamicText(location.up(), "§eFrom §b${rareMob.playerDisplayName}", 1.6, yOff = 9f)

            if (config.inquisitorSharing.showDespawnTime) {
                val spawnTime = rareMob.spawnTime
                val format = (75.seconds - spawnTime.passedSince()).format()
                event.drawDynamicText(location.up(), "§eDespawns in §b$format", 1.6, yOff = 18f)
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
                        event.drawDynamicText(location.up(), "§e${formattedDistance}m", 1.7, yOff = 10f)
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
            event.drawColor(location, burrowType.color, distance > 10)
            event.drawDynamicText(location.up(), text, 1.5)
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
        if (event.itemInHand?.isDianaSpade != true || location.getBlockAt() !== Blocks.GRASS_BLOCK) return

        val burrows = allGuesses.flatMap { it.guesses }
        if (burrows.contains(location)) {
            BurrowApi.lastBurrowInteracted = location
        }
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
                resetAllData()
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
        allGuesses.add(GuessEntry(listOf(location), burrowType = type))
        update()
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shtestburrow") {
            description = "Sets a test burrow waypoint at your location"
            category = CommandCategory.DEVELOPER_TEST
            arg("type", BrigadierArguments.string()) { type ->
                callback { setTestBurrow(getArg(type)) }
            }
        }
    }
}
