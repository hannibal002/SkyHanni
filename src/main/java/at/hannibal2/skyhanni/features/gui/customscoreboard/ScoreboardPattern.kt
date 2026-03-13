package at.hannibal2.skyhanni.features.gui.customscoreboard

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import java.util.regex.Pattern

@SkyHanniModule
object ScoreboardPattern {
    private val group = RepoPattern.group("features.gui.customscoreboard")

    // Lines from the scoreboard
    private val scoreboardGroup by group.exclusiveGroup("scoreboard")

    @HandleEvent(RepositoryReloadEvent::class)
    fun onRepoReload() {
        UnknownLinesHandler.invalidateRemoteOnlyPatterns()
    }

    internal fun computeRemoteOnlyPatterns(): Array<Pattern> =
        scoreboardGroup.getUnusedPatterns().toTypedArray()

    // Main scoreboard
    private val mainSB = scoreboardGroup.group("main")

    /**
     * @regexTest Motes: §5137,242
     */
    val motesPattern by mainSB.pattern(
        "motes",
        "(?:§.)*Motes: (?:§.)*(?<motes>[\\d,]+).*",
    )

    /**
     * @regexTest Copper: §c3,416
     */
    val copperPattern by mainSB.pattern(
        "copper",
        "(?:§.)*Copper: (?:§.)*(?<copper>[\\d,]+).*",
    )

    /**
     * @regexTest Sowdust: §230,210,307
     * @regexTestWrapped " Sowdust: §r§230,120,093"
     */
    val sowdustPattern by mainSB.pattern(
        "sowdust",
        "\\s?(?:§.)*Sowdust: (?:§.)*(?<sowdust>[\\d,]+)",
    )

    /**
     * @regexTest Sowdust: §26.5k §7(+912)
     * @regexTest Sowdust: §230.1M §7(+798)
     * @regexTest Sowdust: §22.7B §7(+12)
     * @regexTest Sowdust: §210.7k §7(+3.3k)
     * @regexTest Sowdust: §210.7k §7(+1.2M)
     */
    val sowdustGainedPattern by mainSB.pattern(
        "sowdust-gained",
        "^(?:§.)*Sowdust: (?:§.)*[\\d,.kKmMbB]+ §7\\(\\+[\\d.kKmMbB]+\\)",
    )

    /**
     * @regexTest Gems: §a350
     */
    val gemsPattern by mainSB.pattern(
        "gems",
        "(?:§.)*Gems: (?:§.)*(?<gems>[\\d,]+).*",
    )

    /**
     * @regexTestWrapped " §5ф §dWizard Tower"
     */
    val locationPattern by mainSB.pattern(
        "location",
        "\\s*(?<location>(?:§7⏣|§5ф) .*)",
    )

    /**
     * @regexTest §711/15/24 §8m10DH
     */
    val lobbyCodePattern by mainSB.pattern(
        "lobbycode",
        "\\s*§.(?:\\d{2}/?){3} §8(?<code>.*)",
    )

    /**
     * @regexTestWrapped " Early Spring 13th"
     */
    val datePattern by mainSB.pattern(
        "date",
        "\\s*(?:(?:Late|Early) )?(?:Spring|Summer|Autumn|Winter) \\d+(?:st|nd|rd|th)?.*",
    )

    /**
     * @regexTestWrapped " §78:50am"
     * @regexTestWrapped " §75:50am §b☽"
     */
    val timePattern by mainSB.pattern(
        "time",
        "\\s*§7\\d+:\\d+(?:am|pm)\\s*(?<symbol>§b☽|§e☀|§.⚡|§.☔)?.*",
    )

    /**
     * @regexTest §ewww.hypixel.net
     * @regexTest §ealpha.hypixel.net
     */
    val footerPattern by mainSB.pattern(
        "footer",
        "§e(?:www|alpha)\\.hypixel\\.net",
    )

    /**
     * @regexTest §6Year 384 Votes
     */
    val yearVotesPattern by mainSB.pattern(
        "yearvotes",
        "§6Year \\d+ Votes",
    )

    /**
     * @regexTest §f||||||||||||||| §aFoxy
     * @regexTest §d|§f|||||||||||||| §dDiaz
     */
    val votesPattern by mainSB.pattern(
        "votes",
        "§.\\|+(?:§f)?\\|+ §.+",
    )

    /**
     * @regexTest §7Waiting for
     * @regexTest §7your vote...
     */
    val waitingForVotePattern by mainSB.pattern(
        "waitingforvote",
        "§7Waiting for|§7your vote\\.\\.\\.",
    )

    /**
     * @regexTest North Stars: §d1,539
     */
    val northstarsPattern by mainSB.pattern(
        "northstars",
        "North Stars: §d(?<northstars>[\\w,]+).*",
    )

    /**
     * @regexTestWrapped " §7♲ §7Ironman"
     * @regexTestWrapped " §a☀ §aStranded"
     * @regexTestWrapped " §9Ⓑ §9Bingo"
     */
    val profileTypePattern by mainSB.pattern(
        "profiletype",
        "\\s*(?:§7♲ §7Ironman|§a☀ §aStranded|§.Ⓑ §.Bingo).*",
    )

    // multi use
    private val multiUseSB = scoreboardGroup.group("multiuse")

    /**
     * @regexTest Auto-closing in: §c1:58
     */
    val autoClosingPattern by multiUseSB.pattern(
        "autoclosing",
        "(?:§.)*Auto-closing in: §c(?:\\d+:)?\\d+",
    )

    /**
     * @regexTest Starting in: §a0:02
     */
    val startingInPattern by multiUseSB.pattern(
        "startingin",
        "(?:§.)*Starting in: §.(?:\\d+:)?\\d+",
    )

    /**
     * @regexTest Time Elapsed: §a48s
     */
    val timeElapsedPattern by multiUseSB.pattern(
        "timeelapsed",
        "(?:§.)*Time Elapsed: (?:§.)*(?<time>(?:\\w+[ydhms] ?)+)",
    )

    /**
     * @regexTest Instance Shutdown In: §a01m 59s
     */
    val instanceShutdownPattern by multiUseSB.pattern(
        "instanceshutdown",
        "(?:§.)*Instance Shutdown In: (?:§.)*(?<time>(?:\\w+[ydhms] ?)+)",
    )

    /**
     * @regexTest Time Left: §b11
     */
    val timeLeftPattern by multiUseSB.pattern(
        "timeleft",
        "(?:§.)*Time Left: (?:§.)*[\\w:,.\\s]+",
    )

    // dungeon scoreboard
    private val dungeonSB = scoreboardGroup.group("dungeon")

    /**
     * @regexTest §8- §c§4Power Dragon§a 497.3M§c❤
     * @regexTest §8- §c§4Power Dragon§a 497.3M
     */
    val m7dragonsPattern by dungeonSB.pattern(
        "m7dragons",
        "§cNo Alive Dragons|§8- (?:§.)+[\\w\\s]+Dragon§a [\\w,.]+(?:§.❤)?",
    )
    val keysPattern by dungeonSB.pattern(
        "keys",
        "Keys: §.■ §.[✗✓] §.■ §a.x",
    )
    val clearedPattern by dungeonSB.pattern(
        "cleared",
        "(?:§.)*Cleared: (?:§.)*(?<percent>[\\w,.]+)% (?:§.)*\\((?:§.)*(?<score>[\\w,.]+)(?:§.)*\\)",
    )
    val soloPattern by dungeonSB.pattern(
        "solo",
        "§3§lSolo",
    )

    /**
     * @regexTest §a[H] §6Eisengolem §7[Lv48]
     * @regexTest §e[M] §b04032006 §a7,361§c❤
     */
    @Suppress("MaxLineLength")
    val teammatesPattern by dungeonSB.pattern(
        "teammates",
        "(?:§.)*(?<classAbbv>\\[\\w]) (?:§.)*(?<username>\\w{2,16}) (?:(?:§.)*(?<classLevel>\\[Lvl?(?<level>[\\w,.]+)?]?)|(?:§(?<color>.))*(?<health>[\\w,.]+)(?:§.)*.?)",
    )

    /**
     * @regexTest §8 - §cChaos§a 1
     */
    val floor3GuardiansPattern by dungeonSB.pattern(
        "floor3guardians",
        "§. - §.(?:Healthy|Reinforced|Laser|Chaos)§a [\\w,.]*(?:§c❤)?",
    )

    // kuudra
    private val kuudraSB = scoreboardGroup.group("kuudra")

    /**
     * @regexTest §f§lWave: §c§l2 §8- §a0:09
     */
    val wavePattern by kuudraSB.pattern(
        "wave",
        "(?:§.)*Wave: (?:§.)*\\d+(?:§.)*(?: §.- §.\\d+:\\d+)?",
    )

    /**
     * @regexTest §fTokens: §565
     */
    val tokensPattern by kuudraSB.pattern(
        "tokens",
        "(?:§.)*Tokens: §.[\\w,]+",
    )

    /**
     * @regexTest Submerges In: §e01m 00s
     * @regexTest Submerges In: §e???
     */
    val submergesPattern by kuudraSB.pattern(
        "submerges",
        "(?:§.)*Submerges In: (?:§.)*[\\w\\s?]+",
    )

    // farming
    private val farmingSB = scoreboardGroup.group("farming")

    /**
     * @regexTest §6§lGOLD §fmedals: §6111
     * @regexTest §f§lSILVER §fmedals: §f1,154
     * @regexTest §c§lBRONZE §fmedals: §c268
     */
    val medalsPattern by farmingSB.pattern(
        "medals",
        "§[6fc]§l(?:GOLD|SILVER|BRONZE) §fmedals: §[6fc][\\d.,]+",
    )

    /**
     * @regexTestWrapped "   §cLocked"
     */
    val lockedPattern by farmingSB.pattern(
        "locked",
        "\\s*§cLocked.*",
    )

    /**
     * @regexTestWrapped "   §fCleanup§7: §e0.3%"
     * @regexTestWrapped "   §fCleanup§7: §b2 §4§lൠ§7 x1"
     */
    val cleanUpPattern by farmingSB.pattern(
        "cleanup",
        "\\s*(?:§.)*Cleanup(?:§.)*: (?:§.)*.*",
    )

    /**
     * @regexTestWrapped "   §fPasting§7: §e41.9%"
     * @regexTestWrapped "   §fBarn Pasting§7: §e10.2%"
     */
    val pastingPattern by farmingSB.pattern(
        "pasting",
        "\\s*(?:§.)*(?:Barn )?Pasting§7: (?:§.)*[\\d,.]+%?",
    )

    /**
     * @regexTest Pelts: §5160
     */
    val peltsPattern by farmingSB.pattern(
        "pelts",
        "(?:§.)*Pelts: (?:§.)*[\\d,]+.*",
    )

    /**
     * @regexTest Tracker Mob Location:
     */
    val mobLocationPattern by farmingSB.pattern(
        "moblocation",
        "(?:§.)*Tracker Mob Location:",
    )
    val jacobsContestPattern by farmingSB.pattern(
        "jacobscontest",
        "§eJacob's Contest",
    )

    /**
     * @regexTestWrapped "   §aPlot §7- §b3 §4§lൠ§7 x8"
     */
    val plotPattern by farmingSB.pattern(
        "plot",
        "\\s*§aPlot §7-.*",
    )

    // mining
    private val miningSB = scoreboardGroup.group("mining")

    /**
     * @regexTest §2᠅ §fMithril§f: §235,448
     * @regexTest §d᠅ §fGemstone§f: §d36,758
     * @regexTest §b᠅ §fGlacite§f: §b29,537
     * @regexTest §2᠅ §fMithril Powder§f: §235,448
     * @regexTest §d᠅ §fGemstone Powder§f: §d36,758
     * @regexTest §b᠅ §fGlacite Powder§f: §b29,537
     */
    val powderPattern by miningSB.pattern(
        "powder",
        "(?:§.)*᠅ §.(?<type>Gemstone|Mithril|Glacite)(?: Powder)?(?:§.)*:? (?:§.)*(?<amount>[\\d,.]*)",
    )

    val windCompassPattern by miningSB.pattern(
        "windcompass",
        "§9Wind Compass",
    )

    /**
     * @regexTestWrapped "  ≈"
     */
    val windCompassArrowPattern by miningSB.pattern(
        "windcompassarrow",
        "\\s*(?:§.|[⋖⋗≈])+\\s*(?:§.|[⋖⋗≈])*\\s*",
    )

    /**
     * @regexTest Event: §6§LRAFFLE
     * @regexTest Event: §C§LGOBLIN RAID
     * @regexTest Event: §B§LMITHRIL GOURMAND
     */
    val miningEventPattern by miningSB.pattern(
        "miningevent",
        "Event: §.§[lL].*",
    )

    /**
     * @regexTest Zone: §bGoblin Burrows
     */
    val miningEventZonePattern by miningSB.pattern(
        "miningeventzone",
        "Zone: §.*",
    )

    /**
     * @regexTest Find tickets on the
     * @regexTest ground and bring them
     * @regexTest to the raffle box
     */
    val raffleUselessPattern by miningSB.pattern(
        "raffleuseless",
        "Find tickets on the|ground and bring them|to the raffle box",
    )

    /**
     * @regexTest Tickets: §a8 §7(17.4%)
     */
    val raffleTicketsPattern by miningSB.pattern(
        "raffletickets",
        "Tickets: §a\\d+ §7\\(\\d+(?:\\.\\d)?%\\)",
    )

    /**
     * @regexTest Pool: §646
     */
    val rafflePoolPattern by miningSB.pattern(
        "rafflepool",
        "Pool: §6\\d+",
    )
    val mithrilUselessPattern by miningSB.pattern(
        "mithriluseless",
        "§7Give Tasty Mithril to Don!",
    )

    /**
     * @regexTest Remaining: §a80 Tasty Mithril
     * @regexTest Remaining: §aFULL
     */
    val mithrilRemainingPattern by miningSB.pattern(
        "mithrilremaining",
        "Remaining: §a(?:\\d+ Tasty Mithril|FULL)",
    )

    /**
     * @regexTest Your Tasty Mithril: §c70 §a(+70)
     */
    val mithrilYourMithrilPattern by miningSB.pattern(
        "mithrilyourmithril",
        "Your Tasty Mithril: §c\\d+.*",
    )

    /**
     * @regexTest Nearby Players: §a0
     * @regexTest Nearby Players: §a1
     * @regexTest Nearby Players: §a5 §cMAX
     * @regexTest Nearby Players: §cN/A
     */
    val nearbyPlayersPattern by miningSB.pattern(
        "nearbyplayers",
        "Nearby Players: §.(?:\\d+|N/A)(?: §cMAX)?",
    )
    val goblinUselessPattern by miningSB.pattern(
        "goblinguseless",
        "§7Kill goblins!",
    )

    /**
     * @regexTest Remaining: §a1 goblin
     * @regexTest Remaining: §a2 goblins
     */
    val remainingGoblinPattern by miningSB.pattern(
        "remaininggoblin",
        "Remaining: §a\\d+ goblins?",
    )

    /**
     * @regexTest Your kills: §c85 ☠
     */
    val yourGoblinKillsPattern by miningSB.pattern(
        "yourgoblin",
        "Your kills: §c\\d+ ☠(?: §a\\(\\+\\d+\\))?",
    )

    /**
     * @regexTest §7§lNot started.§7§l..
     */
    val mineshaftNotStartedPattern by miningSB.pattern(
        "mineshaft.notstarted",
        "(?:§.)*Not started.*",
    )

    /**
     * @regexTest Event Bonus: §6+4☘
     */
    val fortunateFreezingBonusPattern by miningSB.pattern(
        "fortunatefreezing.bonus",
        "Event Bonus: §6\\+\\d+☘",
    )

    /**
     * @regexTest Fossil Dust: §f3,281 §e(+1)
     * @regexTest Fossil Dust: 405 §e(+1)
     */
    val fossilDustPattern by miningSB.pattern(
        "fossildust",
        "Fossil Dust: (?:§f)*[\\d.,]+.*",
    )

    // combat
    private val combatSB = scoreboardGroup.group("combat")
    val magmaChamberPattern by combatSB.pattern(
        "magmachamber",
        "Magma Chamber",
    )

    /**
     * @regexTest §7Boss: §c45%
     */
    val magmaBossPattern by combatSB.pattern(
        "magmaboss",
        "§7Boss: §[c6e]\\d+%",
    )
    val damageSoakedPattern by combatSB.pattern(
        "damagesoaked",
        "§7Damage Soaked:",
    )
    val killMagmasPattern by combatSB.pattern(
        "killmagmas",
        "§6Kill the Magmas:",
    )

    /**
     * @regexTest §a▎▎▎▎▎§7▎▎▎▎▎▎▎▎▎▎▎▎▎▎▎▎▎▎▎▎
     * @regexTest §a§7▎▎▎▎▎▎▎▎▎▎▎▎▎▎▎▎▎▎▎▎▎▎▎▎▎
     */
    val killMagmasDamagedSoakedBarPattern by combatSB.pattern(
        "killmagmasbar",
        "(?:(?:§.)*▎+)+.*",
    )

    /**
     * @regexTest §cThe boss is reforming!
     * @regexTest §cThe boss is forming!
     */
    val reformingPattern by combatSB.pattern(
        "magmareforming",
        "§cThe boss is (?:re)?forming!",
    )
    val bossHealthPattern by combatSB.pattern(
        "magmabosshealth",
        "§7Boss Health:",
    )

    /**
     * @regexTest §e389.6k§f/§a10M§c❤
     */
    val bossHealthBarPattern by combatSB.pattern(
        "magmabosshealthbar",
        "§.[\\w,.]+§f/§a10M§c❤",
    )

    /**
     * @regexTest Dragon HP: §a2,317,156 §c❤
     * @regexTest Dragon HP: §a8,612,684 §c❤
     */
    val bossHPPattern by combatSB.pattern(
        "bosshp",
        "(?:Protector|Dragon) HP: §a[\\d,.]* §c❤",
    )

    /**
     * @regexTest Your Damage: §c0
     * @regexTest Your Damage: §c439,753.6
     */
    val bossDamagePattern by combatSB.pattern(
        "bossdamage",
        "Your Damage: §c[\\d,.]+",
    )
    val slayerQuestPattern by combatSB.pattern(
        "slayerquest",
        "Slayer Quest",
    )

    // misc
    private val miscSB = scoreboardGroup.group("misc")

    /**
     * @regexTest Dragon Essence: §d2,442
     */
    val essencePattern by miscSB.pattern(
        "essence",
        "\\s*.*Essence: §.(?<essence>-?\\d+(?::?,\\d{3})*(?:\\.\\d+)?)",
    )

    /**
     * @regexTestWrapped " §e§l⚡ §cRedstone: §e§b4%"
     */
    val redstonePattern by miscSB.pattern(
        "redstone",
        "\\s*(?:§.)*⚡ §cRedstone: (?:§.)*\\d+%",
    )

    /**
     * @regexTestWrapped " §a✌ §7(§a9§7/20)"
     */
    val visitingPattern by miscSB.pattern(
        "visiting",
        "\\s*§a✌ §7\\(§.\\d+(?:§.)?/\\d+(?:§.)?\\)",
    )

    /**
     * @regexTest Flight Duration: §a202:46:12
     */
    val flightDurationPattern by miscSB.pattern(
        "flightduration",
        "\\s*Flight Duration: §a(?::?\\d{1,3})*",
    )

    /**
     * @regexTest Challenge: §6Force
     */
    val dojoChallengePattern by miscSB.pattern(
        "dojochallenge",
        "(?:§.)*Challenge: (?:§.)*(?<challenge>.+)",
    )

    /**
     * @regexTest Difficulty: §aEasy
     */
    val dojoDifficultyPattern by miscSB.pattern(
        "dojodifficulty",
        "(?:§.)*Difficulty: (?:§.)*(?<difficulty>.+)",
    )

    /**
     * @regexTest Points: §a0
     * @regexTest Points: §a10 §7(§a+§a10§7)
     */
    val dojoPointsPattern by miscSB.pattern(
        "dojopoints",
        "(?:§.)*Points: (?:§.)*[\\w.]+.*",
    )

    /**
     * @regexTest Time: §a20s
     * @regexTest Time: §a7s §7(§a0s§7)
     */
    val dojoTimePattern by miscSB.pattern(
        "dojotime",
        "(?:§.)*Time: (?:§.)*[\\w.]+.*",
    )

    /**
     * @regexTest Objective
     * @regexTest Objective §a§l⬇
     */
    val objectivePattern by miscSB.pattern(
        "objective",
        "(?:§.)*(?:Objective|Quest).*",
    )

    /**
     * @regexTest Queued: §aThe Catacombs
     */
    val queuePattern by miscSB.pattern(
        "queued",
        "Queued:.*",
    )

    /**
     * @regexTest Tier: §eFloor VI
     */
    val queueTierPattern by miscSB.pattern(
        "queuetier",
        "Tier: §e.*",
    )

    /**
     * @regexTest Position: §b#2 §fSince: §a00:01
     */
    val queuePositionPattern by miscSB.pattern(
        "queueposition",
        "Position: (?:§.)*#\\d+ (?:§.)*Since: .*",
    )

    val queueWaitingForLeaderPattern by miscSB.pattern(
        "queuewaitingforleader",
        "§aWaiting on party leader!",
    )

    /**
     * @regexTest §d5th Anniversary§f 167:59:54
     * @regexTest §bCentury Raffle§f 124:00:00
     */
    val anniversaryPattern by miscSB.pattern(
        "anniversary",
        "(?:§d\\d+(?:st|nd|rd|th) Anniversary|§bCentury Raffle)§f (?:\\d|:)+",
    )

    // this thirdObjectiveLinePattern includes all those weird objective lines that go into a third (and fourth) scoreboard line
    /**
     * @regexTest §eProtect Elle §7(§a98%§7)
     * @regexTest §fFish 1 Flyfish §c✖
     * @regexTest §fFish 1 Skeleton Fish §c✖
     * @regexTestWrapped "  §7(§e1§7/§a100§7)"
     */
    @Suppress("MaxLineLength")
    val thirdObjectiveLinePattern by miscSB.pattern(
        "thirdobjectiveline",
        "§eProtect Elle §7\\(§.\\d+%§7\\)|\\s*§.\\(§.[\\w,.]+§.\\/§.[\\w,.]+§.\\)|§f Mages.*|§f Barbarians.*|§edefeat Kuudra|§eand stun him|§.Fish \\d .*[fF]ish §.[✖✔]",
    )

    /**
     * collection of lines that just randomly exist and I have no clue how on earth to effectively remove them
     * @regexTest §eKill 100 Automatons
     * @regexTest §eMine 10 Rubies
     * @regexTest §eFind a Jungle Key
     * @regexTest §eFind the 4 Missing Pieces
     * @regexTest §eTalk to the Goblin King
     * @regexTest §eBring items to Moby
     * @regexTestWrapped " Glowing Mushroom §8x8"
     */
    @Suppress("MaxLineLength")
    val wtfAreThoseLinesPattern by miscSB.pattern(
        "wtfarethoselines",
        "§eMine \\d+ .*|§eKill 100 Automatons|§eFind a Jungle Key|§eFind the \\d+ Missing Pieces?|§eTalk to the Goblin King|§eBring items to Moby| Glowing Mushroom §8x\\d",
    )
    val darkAuctionCurrentItemPattern by miscSB.pattern(
        "darkauction.currentitem",
        "Current Item:",
    )

    // events
    private val eventsSB = scoreboardGroup.group("events")

    /**
     * @regexTest §aTraveling Zoo§f 43:41
     */
    val travelingZooPattern by eventsSB.pattern(
        "travelingzoo",
        "§aTraveling Zoo§f \\d*:\\d+",
    )

    /**
     * @regexTest §dNew Year Event!§f 17:53
     */
    val newYearPattern by eventsSB.pattern(
        "newyear",
        "§dNew Year Event!§f \\d*:?\\d+",
    )

    /**
     * @regexTest §6Spooky Festival§f 50:54
     */
    val spookyPattern by eventsSB.pattern(
        "spooky",
        "§6Spooky Festival§f \\d*:?\\d+",
    )

    /**
     * @regexTest Event Start: §a2:38
     */
    val winterEventStartPattern by eventsSB.pattern(
        "wintereventstart",
        "(?:§.)*Event Start: §.[\\d:]+$",
    )

    /**
     * @regexTest Next Wave: §a§aSoon!
     */
    val winterNextWavePattern by eventsSB.pattern(
        "wintereventnextwave",
        "(?:§.)*Next Wave: (?:§.)*(?:[\\d:]+|Soon!)",
    )

    /**
     * @regexTest §cWave 5
     */
    val winterWavePattern by eventsSB.pattern(
        "wintereventwave",
        "(?:§.)*Wave \\d+",
    )

    /**
     * @regexTest Magma Cubes Left: §c-4
     * @regexTest Magma Cubes Left: §c3
     */
    val winterMagmaLeftPattern by eventsSB.pattern(
        "wintereventmagmaleft",
        "(?:§.)*Magma Cubes Left: §.-?\\d+",
    )

    /**
     * @regexTest Your Total Damage: §c13,804 §e(#
     */
    val winterTotalDmgPattern by eventsSB.pattern(
        "wintereventtotaldmg",
        "(?:§.)*Your Total Damage: §.[\\d+,.]+.*$",
    )

    /**
     * @regexTest Your Cube Damage: §c303
     */
    val winterCubeDmgPattern by eventsSB.pattern(
        "wintereventcubedmg",
        "(?:§.)*Your Cube Damage: §.[\\d+,.]+$",
    )

    // rift
    private val riftSB = scoreboardGroup.group("rift")

    /**
     * @regexTestWrapped " §fRift Dimension"
     */
    val riftDimensionPattern by riftSB.pattern(
        "dimension",
        "\\s*(?:§f)?Rift Dimension",
    )
    val riftHotdogTitlePattern by riftSB.pattern(
        "hotdogtitle",
        "§6Hot Dog Contest",
    )

    /**
     * @regexTest Eaten: §c2/50
     */
    val riftHotdogEatenPattern by riftSB.pattern(
        "hotdogeaten",
        "Eaten: §.\\d+/\\d+",
    )

    /**
     * @regexTest Time spent sitting
     * @regexTest with Ävaeìkx: §a32m15s
     */
    val riftAveikxPattern by riftSB.pattern(
        "aveikx",
        "Time spent sitting|with Ävaeìkx: .*",
    )

    /**
     * @regexTest Hay Eaten: §e2,477/3,000
     */
    val riftHayEatenPattern by riftSB.pattern(
        "hayeaten",
        "Hay Eaten: §.[\\d,.]+/[\\d,.]+",
    )

    /**
     * @regexTest Clues: §a0/8
     */
    val cluesPattern by riftSB.pattern(
        "clues",
        "Clues: §.\\d+/\\d+",
    )

    /**
     * @regexTest §eFirst Up
     * @regexTest Find and talk with Barry
     */
    val barryProtestorsQuestlinePattern by riftSB.pattern(
        "protestors.quest",
        "§eFirst Up|Find and talk with Barry",
    )

    /**
     * @regexTest Protestors handled: §b5/7
     */
    val barryProtestorsHandledPattern by riftSB.pattern(
        "protestors.handled",
        "Protestors handled: §b\\d+\\/\\d+",
    )

    val timeSlicedPattern by riftSB.pattern(
        "timesliced",
        "§c§lTIME SLICED!",
    )

    /**
     * @regexTestWrapped " Big damage in: §d2m 59s"
     */
    val bigDamagePattern by riftSB.pattern(
        "bigdamage",
        "\\s*Big damage in: §d[\\w\\s]+",
    )

    private val carnivalSB = scoreboardGroup.group("carnival")

    /**
     * @regexTest §eCarnival§f 85:33:57
     * @regexTest §eCarnival§f 118:41:05
     */
    val carnivalPattern by carnivalSB.pattern(
        "carnival",
        "§eCarnival§f \\d+(?::\\d+)*",
    )

    /**
     * @regexTest §3§lCatch a Fish
     * @regexTest §6§lFruit Digging
     * @regexTest §c§lZombie Shootout
     */
    val carnivalTasksPattern by carnivalSB.pattern(
        "tasks",
        "§.§l(?:Catch a Fish|Fruit Digging|Zombie Shootout)",
    )

    /**
     * @regexTest §fCarnival Tokens: §e129
     * @regexTest §fCarnival Tokens: §e1,031
     */
    val carnivalTokensPattern by carnivalSB.pattern(
        "tokens",
        "(?:§f)*Carnival Tokens: §e[\\d,]+",
    )

    /**
     * @regexTest §fFruits: §a2§7/§c10
     */
    val carnivalFruitsPattern by carnivalSB.pattern(
        "fruits",
        "(?:§f)?Fruits: §.\\d+§./§.\\d+",
    )

    /**
     * @regexTest §fScore: §e600 §6(+300)
     * @regexTest §fScore: §e600
     */
    val carnivalScorePattern by carnivalSB.pattern(
        "score",
        "(?:§f)?Score: §.\\d+.*",
    )

    /**
     * @regexTest §fCatch Streak: §a0
     */
    val carnivalCatchStreakPattern by carnivalSB.pattern(
        "catchstreak",
        "(?:§f)?Catch Streak: §.\\d+",
    )

    /**
     * @regexTest §fAccuracy: §a81.82%
     * @regexTest §fAccuracy: §a81%
     */
    val carnivalAccuracyPattern by carnivalSB.pattern(
        "accuracy",
        "(?:§f)?Accuracy: §.\\d+(?:\\.\\d+)?%",
    )

    /**
     * @regexTest §fKills: §a8
     */
    val carnivalKillsPattern by carnivalSB.pattern(
        "kills",
        "(?:§f)?Kills: §.\\d+",
    )

    // Galatea
    private val galateaSB = scoreboardGroup.group("galatea")

    /**
     * @regexTest Whispers: §3141§b (+1)
     * @regexTest Whispers: §317.5k§b (+50)
     * @regexTest §fWhispers: §317k§b (+40)
     */
    val whispersPattern by galateaSB.pattern(
        "whispers",
        "(?:§f)?Whispers: §3[\\w,.]+.*",
    )

    /**
     * @regexTestWrapped "    §aHOTF§f: §a28k§3.7k§b (+35)"
     * @regexTestWrapped "    §aHOTF§f: §a28k§9 (+29 Exp)"
     */
    val hotfPattern by galateaSB.pattern(
        "hotf",
        "\\s*§aHOTF§f: §a[\\w,.]+.*",
    )

    /**
     * @regexTest §eAgatha's Contest §a5m28s
     */
    val agathasContestPattern by galateaSB.pattern(
        "agathas-contest",
        "§eAgatha's Contest §a.*",
    )

    /**
     * Sometimes when the scoreboard updates, it only updates half way,
     * causing some lines to become mixed with other lines -> broken.
     * This should already get handled fine but sometimes these errors still occur with some lines way too often.
     * This pattern is to catch those lines.
     */
    /**
     * @regexTestWrapped " §e§l⚡ §cRedston"
     * @regexTestWrapped "      §ce: §e§b0%"
     * @regexTest Starting in: §a0 §c1:55
     * @regexTest §2᠅ §fMithril§f:§695
     * @regexTest §d᠅ §fGemstone§f
     * @regexTest §d᠅ §fGemstone§f§e(+1)
     */
    val brokenPatterns by group.list(
        "broken",
        "\\s*§.§l⚡ §cRedston",
        "\\s*§ce: §e§b\\d+%",
        "\\s*Starting in: §a0 §c[\\d:]+",
        "(?:§.)*᠅ §.(?<type>Gemstone|Mithril|Glacite)(?: Powder)?.*",
    )

    // Lines from the tablist
    private val tablistGroup = group.group("tablist-no-color")

    /**
     * @regexTestWrapped " Ends In: 27h"
     */
    val eventTimeEndsPattern by tablistGroup.pattern(
        "eventtime",
        "\\s+Ends In: (?<time>.*)",
    )

    /**
     * @regexTestWrapped " Starts In: 7h"
     */
    val eventTimeStartsPattern by tablistGroup.pattern(
        "eventtimestarts",
        "\\s+Starts In: (?<time>.*)",
    )
}
