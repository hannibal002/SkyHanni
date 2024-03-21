package at.hannibal2.skyhanni.features.gui.customscoreboard

import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

object ScoreboardPattern {
    val group = RepoPattern.group("features.gui.customscoreboard")

    // Stats from the scoreboard
    private val scoreboardGroup = group.group("scoreboard")

    // main scoreboard
    private val mainSb = scoreboardGroup.group("main")
    val motesPattern by mainSb.pattern(
        "motes",
        "^(§.)*Motes: (§.)*(?<motes>[\\d,]+).*$"
    )
    val heatPattern by mainSb.pattern(
        "heat",
        "^Heat: (?<heat>.*)$"
    ) // this line is weird (either text or number), ill leave it as is; it even has different colors?
    val copperPattern by mainSb.pattern(
        "copper",
        "^(§.)*Copper: (§.)*(?<copper>[\\d,]+).*$"
    )
    val locationPattern by mainSb.pattern(
        "location",
        "^\\s*(?<location>(§7⏣|§5ф) .*)$"
    )
    val lobbyCodePattern by mainSb.pattern(
        "lobbycode",
        "^\\s*§.((\\d{2}/\\d{2}/\\d{2})|Server closing: [\\d:]+) §8(?<code>.*)\$"
    )
    val datePattern by mainSb.pattern(
        "date",
        "^\\s*(Late |Early )?(Spring|Summer|Autumn|Winter) \\d{1,2}(st|nd|rd|th)?.*"
    )
    val timePattern by mainSb.pattern(
        "time",
        "^\\s*§7\\d{1,2}:\\d{2}(?:am|pm) (?<symbol>(§b☽|§e☀|§.⚡|§.☔)).*$"
    )
    val footerPattern by mainSb.pattern(
        "footer",
        "§e(www|alpha).hypixel.net\$"
    )
    val yearVotesPattern by mainSb.pattern(
        "yearvotes",
        "(?<yearvotes>^§6Year \\d+ Votes\$)"
    )
    val votesPattern by mainSb.pattern(
        "votes",
        "(?<votes>§[caebd]\\|+(§f)?\\|+ §(.+)$)"
    )
    val waitingForVotePattern by mainSb.pattern(
        "waitingforvote",
        "(§7Waiting for|§7your vote\\.\\.\\.)$"
    )
    val northstarsPattern by mainSb.pattern(
        "northstars",
        "North Stars: §d(?<northstars>[\\w,]+).*$"
    )
    val profileTypePattern by mainSb.pattern(
        "profiletype",
        "^\\s*(§7♲ §7Ironman|§a☀ §aStranded|§.Ⓑ §.Bingo).*$"
    )

    // multi use
    private val multiUseSb = scoreboardGroup.group("multiuse")
    val autoClosingPattern by multiUseSb.pattern(
        "autoclosing",
        "(§.)*Auto-closing in: §c(\\d{1,2}:)?\\d{1,2}$"
    )
    val startingInPattern by multiUseSb.pattern(
        "startingin",
        "(§.)*Starting in: §.(\\d{1,2}:)?\\d{1,2}$"
    )
    val timeElapsedPattern by multiUseSb.pattern(
        "timeelapsed",
        "(§.)*Time Elapsed: (§.)*(?<time>(\\w+[ydhms] ?)+)$"
    )
    val instanceShutdownPattern by multiUseSb.pattern(
        "instanceshutdown",
        "(§.)*Instance Shutdown In: (§.)*(?<time>(\\w+[ydhms] ?)+)$"
    )
    val timeLeftPattern by multiUseSb.pattern(
        "timeleft",
        "(§.)*Time Left: (§.)*[\\w:,.]*$"
    )

    // dungeon scoreboard
    private val dungeonSb = scoreboardGroup.group("dungeon")
    val m7dragonsPattern by dungeonSb.pattern(
        "m7dragons",
        "^(§cNo Alive Dragons|§8- (§.)+[\\w\\s]+Dragon§a \\w+§.❤)$"
    )
    val keysPattern by dungeonSb.pattern(
        "keys",
        "Keys: §.■ §.[✗✓] §.■ §a.x$"
    )
    val clearedPattern by dungeonSb.pattern(
        "cleared",
        "(§.)*Cleared: (§.)*(?<percent>[\\w,.]+)% (§.)*\\((§.)*(?<score>[\\w,.]+)(§.)*\\)$"
    )
    val soloPattern by dungeonSb.pattern(
        "solo",
        "§3§lSolo$"
    )
    val teammatesPattern by dungeonSb.pattern(
        "teammates",
        "(§.)*(?<classAbbv>\\[\\w]) (§.)*(?<username>[a-zA-Z0-9_]{2,16}) ((§.)*(?<classLevel>\\[Lvl?(?<level>[\\w,.]+)?]?)|(§.)*(?<health>[\\w,.]+)(§.)*.?)$"
    )
    val floor3GuardiansPattern by dungeonSb.pattern(
        "floor3guardians",
        "§. - §.(?:Healthy|Reinforced|Laser|Chaos)§a ([\\\\w,.]?)+(?:§c❤)?"
    )

    // kuudra
    private val kuudraSb = scoreboardGroup.group("kuudra")
    val wavePattern by kuudraSb.pattern(
        "wave",
        "^(§.)*Wave: (§.)*\\d+(§.)*( §.- §.\\d+:\\d+)?$"
    )
    val tokensPattern by kuudraSb.pattern(
        "tokens",
        "^(§.)*Tokens: §.[\\w,]+$"
    )
    val submergesPattern by kuudraSb.pattern(
        "submerges",
        "^(§.)*Submerges In: (§.)*[\\w,?:]+$"
    )

    // farming
    private val farmingSb = scoreboardGroup.group("farming")
    val medalsPattern by farmingSb.pattern(
        "medals",
        "§[6fc]§l(GOLD|SILVER|BRONZE) §fmedals: §[6fc]\\d+$"
    )
    val lockedPattern by farmingSb.pattern(
        "locked",
        "^\\s*§cLocked$"
    )
    val cleanUpPattern by farmingSb.pattern(
        "cleanup",
        "^\\s*(§.)*Cleanup(§.)*: (§.)*[\\d,.]+%$"
    )
    val pastingPattern by farmingSb.pattern(
        "pasting",
        "^\\s*§f(Barn )?Pasting§7: (§.)*[\\d,.]+%$"
    )
    val peltsPattern by farmingSb.pattern(
        "pelts",
        "^(§.)*Pelts: (§.)*([\\d,]+).*$"
    )
    val mobLocationPattern by farmingSb.pattern(
        "moblocation",
        "^(§.)*Tracker Mob Location:"
    )
    val jacobsContestPattern by farmingSb.pattern(
        "jacobscontest",
        "^§eJacob's Contest$"
    )
    val plotPattern by farmingSb.pattern(
        "plot",
        "\\s*§aPlot §7-.*"
    )

    // mining
    private val miningSb = scoreboardGroup.group("mining")
    val powderPattern by miningSb.pattern(
        "powder",
        "(§.)*᠅ §f(Gemstone|Mithril)( Powder)?(§.)*:?.*$"
    )
    val windCompassPattern by miningSb.pattern(
        "windcompass",
        "§9Wind Compass$"
    )
    val windCompassArrowPattern by miningSb.pattern(
        "windcompassarrow",
        "( )*((§[a-zA-Z0-9]|[⋖⋗≈])+)( )*((§[a-zA-Z0-9]|[⋖⋗≈])+)?( )*"
    )
    val miningEventPattern by miningSb.pattern(
        "miningevent",
        "^Event: §.§L.*$"
    )
    val miningEventZonePattern by miningSb.pattern(
        "miningeventzone",
        "^Zone: §.*$"
    )
    val raffleUselessPattern by miningSb.pattern(
        "raffleuseless",
        "^(Find tickets on the|ground and bring them|to the raffle box)$"
    )
    val raffleTicketsPattern by miningSb.pattern(
        "raffletickets",
        "^Tickets: §a\\d+ §7\\(\\d{1,3}\\.\\d%\\)$"
    )
    val rafflePoolPattern by miningSb.pattern(
        "rafflepool",
        "^Pool: §6\\d+§8/500$"
    )
    val mithrilUselessPattern by miningSb.pattern(
        "mithriluseless",
        "^§7Give Tasty Mithril to Don!$"
    )
    val mithrilRemainingPattern by miningSb.pattern(
        "mithrilremaining",
        "^Remaining: §a(\\d+ Tasty Mithril|FULL)$"
    )
    val mithrilYourMithrilPattern by miningSb.pattern(
        "mithrilyourmithril",
        "^Your Tasty Mithril: §c\\d+.*$"
    )
    val nearbyPlayersPattern by miningSb.pattern(
        "nearbyplayers",
        "^Nearby Players: §.(\\d+|N/A)$"
    )
    val uselessGoblinPattern by miningSb.pattern(
        "uselessgoblin",
        "^§7Kill goblins!$"
    )
    val remainingGoblinPattern by miningSb.pattern(
        "remaininggoblin", "^Remaining: §a\\d+ goblins$"
    )
    val yourGoblinKillsPattern by miningSb.pattern(
        "yourgoblin", "^Your kills: §c\\d+ ☠( §a\\(\\+\\d+\\))?$"
    )

    // combat
    private val combatSb = scoreboardGroup.group("combat")
    val magmaChamberPattern by combatSb.pattern(
        "magmachamber",
        "^Magma Chamber$"
    )
    val magmaBossPattern by combatSb.pattern(
        "magmaboss",
        "^§7Boss: §[c6e]\\d{1,3}%$"
    )
    val damageSoakedPattern by combatSb.pattern(
        "damagesoaked",
        "^§7Damage Soaked:"
    )
    val killMagmasPattern by combatSb.pattern(
        "killmagmas",
        "^§6Kill the Magmas:$"
    )
    val killMagmasDamagedSoakedBarPattern by combatSb.pattern(
        "killmagmasbar",
        "^((§.)*▎+)+.*$"
    )
    val reformingPattern by combatSb.pattern(
        "magmareforming",
        "^§cThe boss is (?:re)?forming!$"
    )
    val bossHealthPattern by combatSb.pattern(
        "magmabosshealth",
        "^§7Boss Health:$"
    )
    val bossHealthBarPattern by combatSb.pattern(
        "magmabosshealthbar",
        "^§.(\\d{1,2}(\\.\\d)?M|\\d{1,3}k)§f/§a10M§c❤$"
    )
    val broodmotherPattern by combatSb.pattern(
        "broodmother",
        "^§4Broodmother§7: §[e64](Slain|Dormant|Soon|Awakening|Imminent|Alive!)$"
    )
    val bossHPPattern by combatSb.pattern(
        "bosshp",
        "^(Protector|Dragon) HP: §a(,?\\d{1,3})* §c❤$"
    )
    val bossDamagePattern by combatSb.pattern(
        "bossdamage",
        "^Your Damage: §c(,?\\d{1,3}(\\.\\d)?)*$"
    )
    val slayerQuestPattern by combatSb.pattern(
        "slayerquest",
        "^Slayer Quest$"
    )

    // misc
    private val miscSb = scoreboardGroup.group("misc")
    val essencePattern by miscSb.pattern(
        "essence",
        "^\\s*(.*)?Essence: §.(?<essence>-?\\d+(:?,\\d{3})*(?:\\.\\d+)?)$"
    )
    val brokenRedstonePattern by miscSb.pattern(
        "brokenredstone",
        "\\s*(?:(?:§.)*⚡ (§.)*Redston|e: (?:§.)*\\d+%)\\s*"
    )
    val redstonePattern by miscSb.pattern(
        "redstone",
        "\\s*(§.)*⚡ §cRedstone: (§.)*\\d{1,3}%$"
    )
    val visitingPattern by miscSb.pattern(
        "visiting",
        "^\\s*§a✌ §7\\(§.\\d+(§.)?/\\d+(§.)?\\)$"
    )
    val flightDurationPattern by miscSb.pattern(
        "flightduration",
        "^\\s*Flight Duration: §a(:?\\d{1,3})*$"
    )
    val dojoChallengePattern by miscSb.pattern(
        "dojochallenge",
        "^(§.)*Challenge: (§.)*(?<challenge>[\\w ]+)$"
    )
    val dojoDifficultyPattern by miscSb.pattern(
        "dojodifficulty",
        "^(§.)*Difficulty: (§.)*(?<difficulty>[\\w ]+)$"
    )
    val dojoPointsPattern by miscSb.pattern(
        "dojopoints",
        "^(§.)*Points: (§.)*[\\w.]+ ?(§7\\(§.*§7\\))?\$"
    )
    val dojoTimePattern by miscSb.pattern(
        "dojotime",
        "^(§.)*Time: (§.)*[\\w.]+( §7\\(§.*§7\\))?$"
    )
    val objectivePattern by miscSb.pattern(
        "objective",
        "^(§.)*(Objective|Quest).*"
    )

    // this thirdObjectiveLinePattern includes all those weird objective lines that go into a third scoreboard line
    val thirdObjectiveLinePattern by miscSb.pattern(
        "thirdobjectiveline",
        "(\\s*§.\\(§.\\w+§./§.\\w+§.\\)|§f Mages.*|§f Barbarians.*|§edefeat Kuudra|§eand stun him)"
    )

    // collection of lines that just randomly exist and I have no clue how on earth to effectively remove them
    val wtfAreThoseLinesPattern by miscSb.pattern(
        "wtfarethoselines",
        "^§eMine 10 Rubies$"
    )
    val darkAuctionCurrentItemPattern by miscSb.pattern(
        "darkauction.currentitem",
        "^Current Item:$"
    )

    // events
    private val eventsSb = scoreboardGroup.group("events")
    val travelingZooPattern by eventsSb.pattern(
        "travelingzoo",
        "§aTraveling Zoo§f \\d{0,2}:\\d{2}$"
    )
    val newYearPattern by eventsSb.pattern(
        "newyear",
        "§dNew Year Event!§f \\d{0,2}?:?\\d{2}$"
    )
    val spookyPattern by eventsSb.pattern(
        "spooky",
        "§6Spooky Festival§f \\d{0,2}?:?\\d{2}$"
    )
    val winterEventStartPattern by eventsSb.pattern(
        "wintereventstart",
        "(§.)*Event Start: §.\\d+:\\d+$"
    )
    val winterNextWavePattern by eventsSb.pattern(
        "wintereventnextwave",
        "(§.)*Next Wave: (§.)*(\\d+:\\d+|Soon!)$"
    )
    val winterWavePattern by eventsSb.pattern(
        "wintereventwave",
        "(§.)*Wave \\d+$"
    )
    val winterMagmaLeftPattern by eventsSb.pattern(
        "wintereventmagmaleft",
        "(§.)*Magma Cubes Left: §.\\d+$"
    )
    val winterTotalDmgPattern by eventsSb.pattern(
        "wintereventtotaldmg",
        "(§.)*Your Total Damage: §.\\d+( §e\\(#\\d+\\)?)?$"
    )
    val winterCubeDmgPattern by eventsSb.pattern(
        "wintereventcubedmg",
        "(§.)*Your Cube Damage: §.\\d+$"
    )

    // rift
    private val riftSb = scoreboardGroup.group("rift")
    val riftDimensionPattern by riftSb.pattern(
        "dimension",
        "^\\s*§fRift Dimension$"
    )


    // Stats from the tablist
    private val tablistGroup = group.group("tablist")
    val gemsPattern by tablistGroup.pattern(
        "gems",
        "^\\s*Gems: §a(?<gems>\\d*,?(\\.\\d+)?[a-zA-Z]?)$"
    )
    val bankPattern by tablistGroup.pattern(
        "bank",
        "^\\s*Bank: §6(?<bank>[\\w.,]+(?:§7 \\/ §6(?<coop>[\\w.,]+))?)$"
    )
    val mithrilPowderPattern by tablistGroup.pattern(
        "mithrilpowder",
        "^\\s*Mithril Powder: (?:§.)+(?<mithrilpowder>[\\d,\\.]+)$"
    )
    val gemstonePowderPattern by tablistGroup.pattern(
        "gemstonepowder",
        "^\\s*Gemstone Powder: (?:§.)+(?<gemstonepowder>[\\d,\\.]+)$"
    )
    val eventNamePattern by tablistGroup.pattern(
        "event",
        "^\\s*§e§lEvent: §r(?<name>§.*)$"
    )
    val eventTimeEndsPattern by tablistGroup.pattern(
        "eventtime",
        "^\\s+Ends In: §r§e(?<time>.*)$"
    )
}
