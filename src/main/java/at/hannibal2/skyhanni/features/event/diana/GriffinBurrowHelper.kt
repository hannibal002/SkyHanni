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
import at.hannibal2.skyhanni.events.ItemClickEvent
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
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.compat.addDoublePlant
import at.hannibal2.skyhanni.utils.compat.addLeaves
import at.hannibal2.skyhanni.utils.compat.addLeaves2
import at.hannibal2.skyhanni.utils.compat.addRedFlower
import at.hannibal2.skyhanni.utils.compat.addTallGrass
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawColor
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawLineToEye
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.toLorenzVec
import io.github.notenoughupdates.moulconfig.ChromaColour
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.init.Blocks
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object GriffinBurrowHelper {

    private val config get() = SkyHanniMod.feature.event.diana

    val allowedBlocksAboveGround = buildList {
        add(Blocks.air)
        add(Blocks.yellow_flower)
        add(Blocks.spruce_fence)
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
        "§eYou (?<type>finished the Griffin burrow chain!|dug out a Griffin Burrow!) §r§7\\((?<current>\\d+)/(?<max>\\d+)\\)"
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
    private val recentBlocksClicked = mutableListOf<LorenzVec>()
    private var lastBurrowInteracted: LorenzVec? = null

    private var shouldFocusOnRareMob = false

    data class GuessEntry(
        val guesses: List<LorenzVec>,
        var burrowType: BurrowType = BurrowType.UNKNOWN,
        var range: Int = 0, //TODO
        var currentIndex: Int = 0
    ) {
        fun getCurrent(): LorenzVec = guesses[currentIndex]
        fun contains(vec: LorenzVec): Boolean {
            // exact match
            if (guesses.contains(vec)) return true

            // check if within range
            if (range > 0) {
                val withinRange = guesses.any { guess ->
                    val distanceSq = guess.distanceSq(vec)
                    distanceSq <= range * range
                }
                if (withinRange) return true
            }

            return false
        }
        fun moveToNext(): Boolean {
            val nextIndex = currentIndex + 1
            if (nextIndex in guesses.indices) {
                currentIndex = nextIndex
                if (!isBlockValid(guesses[nextIndex])) {
                    return moveToNext()
                }
                BurrowGuessEvent(this).post()
                checkMoveGuess()
                return true
            } else return false
        }
    }

    fun removeGuess(location: LorenzVec) {
        val toRemove = allGuesses.filter { it.contains(location) }
        for (item in toRemove) {
            allGuesses.remove(item)
            allGuessesTimers.remove(item)
        }
    }

    fun addGuess(guess: GuessEntry) {
        allGuesses.add(guess)
        allGuessesTimers[guess] = SimpleTimeMark.now()
    }

    fun getKnownBurrows(): List<GuessEntry> {
       return allGuesses.filter { it.burrowType != BurrowType.UNKNOWN }
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
            add("additionalGuesses: ${allGuesses.size}")
            for (guess in allGuesses) {
                add("  ${guess.getCurrent().printWithAccuracy(1)} (size=${guess.guesses.size}) (type=${guess.burrowType})")
            }
        }
    }

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return
        update()
        checkMoveGuess()
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
    }

    fun checkMoveGuess() { //TODO or delete burrow (already does kinda)
        val burrows = getKnownBurrows().flatMap { it.guesses }
        val toDelete = mutableSetOf<GuessEntry>()
        for (guessEntry in allGuesses) {

            allGuessesTimers[guessEntry]?.passedSince()?.let {
                if (it > 30.minutes) {
                    toDelete.add(guessEntry)
                    continue
                }
            }

            var shouldMove = false
            if (!isBlockValid(guessEntry.getCurrent())) shouldMove = true

            val shouldBeLoaded = InventoryUtils.getItemInHandAtTime(SimpleTimeMark.now() - 0.5.seconds)?.isDianaSpade //TODO better
            if (shouldBeLoaded == true &&
                !burrows.contains(guessEntry.getCurrent()) && // burrow is not found
                guessEntry.getCurrent().distanceSq(MinecraftCompat.localPlayer.position.toLorenzVec()) < 900 // within 30 blocks
            ) { shouldMove = true }

            if (shouldMove) {
                if (!guessEntry.moveToNext()) toDelete.add(guessEntry)
            }
        }
        allGuesses.removeAll(toDelete)
        allGuessesTimers.keys.removeAll(toDelete)
    }

    // TODO add option to only focus on last guess - highly requersted method that is less optimal for money per hour. users choice
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
            println("already guessed")
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
        println("burrow detected")
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
        println("burrow dug at: $location")
        removeGuess(location)

        // finished chain
        if (event.current == event.max && config.warnOnChainComp) {
            // finished chain
            if (config.warnOnChainComp) {
                val playerLoc = MinecraftCompat.localPlayer.position.toLorenzVec()
                val anyClose = allGuesses.filter { it.getCurrent().distanceSq(playerLoc) < 8100 }
                if (anyClose.isEmpty()) TitleManager.sendTitle("§eUse Spade")
            }
        }

        update()
    }

    @HandleEvent
    fun onPlayerMove(event: EntityMoveEvent<EntityPlayerSP>) {
        if (!isEnabled()) return
        if (event.distance > 10 && event.isLocalPlayer) {
            update()
            checkMoveGuess()
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.HUB)
    fun onChat(event: SkyHanniChatEvent) {
        if (!isEnabled()) return
        if (event.message.startsWith("§c ☠ §r§7You were killed by §r")) {
            // TODO remove based on last blocks clicked
        }

        // send burrow dug event
        val burrowDugMatcher = burrowDugPattern.matcher(event.message)
        if (burrowDugMatcher.find()) {
            val current = burrowDugMatcher.group("current").toInt()
            val max = burrowDugMatcher.group("max").toInt()

            val burrows = allGuesses.flatMap { it.guesses }
            for (block in recentBlocksClicked.asReversed()) {
                if (burrows.contains(block)) {
                    lastBurrowInteracted == block
                    BurrowDugEvent(block, current, max).post()
                    return
                }
            }
            recentBlocksClicked.clear()

        } else if (genericMythologicalSpawnPattern.matches(event.message)) {
            println("adding mob burrow from mob")
            lastBurrowInteracted?.let {
                addGuess(GuessEntry(listOf(it), BurrowType.MOB))
            }
        }else if (treasureDugPattern.matches(event.message)) {
            println("adding start burrow from treasure")
            lastBurrowInteracted?.let {
                addGuess(GuessEntry(listOf(it), BurrowType.START))
            }
        }

        // talking to Diana NPC
        if (event.message == "§6Poof! §r§eYou have cleared your griffin burrows!") {
            resetAllData()
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.HUB)
    fun onUseAbility(event: ItemClickEvent) {
        if (!isEnabled()) return
        val item = event.itemInHand ?: return
        if (!item.isDianaSpade) return

        if (config.warnOnFail || config.warnOnChainComp) {
            TitleManager.conditionallyStopTitle { currentTitle ->
                currentTitle == "§eUse Spade"
            }
        }
    }

    private fun resetAllData() {
        allGuesses.clear()
        allGuessesTimers.clear()
        recentBlocksClicked.clear()
        targetLocation = null
        GriffinBurrowParticleFinder.reset()

        BurrowWarpHelper.currentWarp = null
        if (isEnabled()) {
            update()
        }
    }

    @HandleEvent
    fun onWorldChange() {
        resetAllData()
    }

    fun isBlockValid(pos: LorenzVec): Boolean {
        if (!pos.isInLoadedChunk()) {
            return true
        }
        val isGround = pos.getBlockAt() == Blocks.grass
        val isValidBlockAbove = pos.up().getBlockAt() in allowedBlocksAboveGround
        return isGround && isValidBlockAbove
    }

    private fun findBlock(point: LorenzVec): LorenzVec {
        if (!point.isInLoadedChunk()) {
            return point.copy(y = LocationUtils.playerLocation().y)
        }
        findGround(point)?.let {
            return it
        }

        return findBlockBelowAir(point)
    }

    private fun findGround(point: LorenzVec): LorenzVec? {
        fun isValidGround(y: Double): Boolean {
            val isGround = point.copy(y = y).getBlockAt() == Blocks.grass
            val isValidBlockAbove = point.copy(y = y + 1).getBlockAt() in allowedBlocksAboveGround
            return isGround && isValidBlockAbove
        }

        var gY = 140.0
        while (!isValidGround(gY)) {
            gY--
            if (gY < 65) {
                // no ground detected, find the lowest block below air
                return null
            }
        }
        return point.copy(y = gY)
    }

    private fun findBlockBelowAir(point: LorenzVec): LorenzVec {
        val start = 65.0
        var gY = start
        while (point.copy(y = gY).getBlockAt() != Blocks.air) {
            gY++
            if (gY > 140) {
                // no blocks at this spot, assuming outside of island
                return point.copy(y = LocationUtils.playerLocation().y)
            }
        }

        if (gY == start) {
            return point.copy(y = LocationUtils.playerLocation().y)
        }
        return point.copy(y = gY - 1)
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return

        val playerLocation = LocationUtils.playerLocation()
        if (config.inquisitorSharing.enabled) {
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

        if (!config.multiGuesses) {
            val target = allGuesses.firstOrNull { it.getCurrent() == targetLocation }
            if (target == null) return
            val location = target.getCurrent()
            val distance = location.distance(playerLocation)

            event.drawColor(location, target.burrowType.color, distance > 10)
            event.drawDynamicText(location.up(), target.burrowType.text, 1.5)

            return
        }

        for (guess in allGuesses) {
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
                    val textColor = if (currentWarp != null && targetLocation == location) "§b" else "§f"
                    text = "${textColor}Guess"
                    if (distance > 5) {
                        val formattedDistance = distance.toInt().addSeparators()
                        event.drawDynamicText(location.up(), "§e${formattedDistance}m", 1.7, yOff = 10f)
                    }
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
        if (event.itemInHand?.isDianaSpade != true || location.getBlockAt() !== Blocks.grass) return
        recentBlocksClicked.add(location)
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
