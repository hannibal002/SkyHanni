package at.hannibal2.skyhanni

import at.hannibal2.skyhanni.api.CollectionAPI
import at.hannibal2.skyhanni.api.DataWatcherAPI
import at.hannibal2.skyhanni.api.GetFromSackAPI
import at.hannibal2.skyhanni.api.SkillAPI
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.Features
import at.hannibal2.skyhanni.config.SackData
import at.hannibal2.skyhanni.config.commands.Commands
import at.hannibal2.skyhanni.data.ActionBarData
import at.hannibal2.skyhanni.data.ActionBarStatsData
import at.hannibal2.skyhanni.data.BitsAPI
import at.hannibal2.skyhanni.data.BlockData
import at.hannibal2.skyhanni.data.BossbarData
import at.hannibal2.skyhanni.data.ChatManager
import at.hannibal2.skyhanni.data.CropAccessoryData
import at.hannibal2.skyhanni.data.EntityData
import at.hannibal2.skyhanni.data.EntityMovementData
import at.hannibal2.skyhanni.data.EventCounter
import at.hannibal2.skyhanni.data.FameRanks
import at.hannibal2.skyhanni.data.FixedRateTimerManager
import at.hannibal2.skyhanni.data.FriendAPI
import at.hannibal2.skyhanni.data.GardenComposterUpgradesData
import at.hannibal2.skyhanni.data.GardenCropMilestones
import at.hannibal2.skyhanni.data.GardenCropMilestonesCommunityFix
import at.hannibal2.skyhanni.data.GardenCropUpgrades
import at.hannibal2.skyhanni.data.GuiData
import at.hannibal2.skyhanni.data.GuiEditManager
import at.hannibal2.skyhanni.data.GuildAPI
import at.hannibal2.skyhanni.data.HighlightOnHoverSlot
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.ItemAddManager
import at.hannibal2.skyhanni.data.ItemClickData
import at.hannibal2.skyhanni.data.ItemTipHelper
import at.hannibal2.skyhanni.data.LocationFixData
import at.hannibal2.skyhanni.data.MaxwellAPI
import at.hannibal2.skyhanni.data.MayorAPI
import at.hannibal2.skyhanni.data.MinecraftData
import at.hannibal2.skyhanni.data.MiningAPI
import at.hannibal2.skyhanni.data.OtherInventoryData
import at.hannibal2.skyhanni.data.OwnInventoryData
import at.hannibal2.skyhanni.data.PartyAPI
import at.hannibal2.skyhanni.data.PetAPI
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.PurseAPI
import at.hannibal2.skyhanni.data.QuiverAPI
import at.hannibal2.skyhanni.data.RenderData
import at.hannibal2.skyhanni.data.SackAPI
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.data.ScreenData
import at.hannibal2.skyhanni.data.SkillExperience
import at.hannibal2.skyhanni.data.SlayerAPI
import at.hannibal2.skyhanni.data.TitleData
import at.hannibal2.skyhanni.data.TitleManager
import at.hannibal2.skyhanni.data.TrackerManager
import at.hannibal2.skyhanni.data.bazaar.HypixelBazaarFetcher
import at.hannibal2.skyhanni.data.hypixel.chat.PlayerChatManager
import at.hannibal2.skyhanni.data.hypixel.chat.PlayerNameFormatter
import at.hannibal2.skyhanni.data.jsonobjects.local.FriendsJson
import at.hannibal2.skyhanni.data.jsonobjects.local.JacobContestsJson
import at.hannibal2.skyhanni.data.jsonobjects.local.KnownFeaturesJson
import at.hannibal2.skyhanni.data.jsonobjects.local.VisualWordsJson
import at.hannibal2.skyhanni.data.mob.MobData
import at.hannibal2.skyhanni.data.mob.MobDebug
import at.hannibal2.skyhanni.data.mob.MobDetection
import at.hannibal2.skyhanni.data.repo.RepoManager
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.PreInitFinishedEvent
import at.hannibal2.skyhanni.features.anvil.AnvilCombineHelper
import at.hannibal2.skyhanni.features.bingo.BingoAPI
import at.hannibal2.skyhanni.features.bingo.CompactBingoChat
import at.hannibal2.skyhanni.features.bingo.MinionCraftHelper
import at.hannibal2.skyhanni.features.bingo.card.BingoCardDisplay
import at.hannibal2.skyhanni.features.bingo.card.BingoCardReader
import at.hannibal2.skyhanni.features.bingo.card.BingoCardTips
import at.hannibal2.skyhanni.features.bingo.card.nextstephelper.BingoNextStepHelper
import at.hannibal2.skyhanni.features.chat.ArachneChatMessageHider
import at.hannibal2.skyhanni.features.chat.ChatFilter
import at.hannibal2.skyhanni.features.chat.CompactBestiaryChatMessage
import at.hannibal2.skyhanni.features.chat.CompactSplashPotionMessage
import at.hannibal2.skyhanni.features.chat.PlayerDeathMessages
import at.hannibal2.skyhanni.features.chat.RareDropMessages
import at.hannibal2.skyhanni.features.chat.SkyblockXPInChat
import at.hannibal2.skyhanni.features.chat.Translator
import at.hannibal2.skyhanni.features.chat.WatchdogHider
import at.hannibal2.skyhanni.features.chat.playerchat.PlayerChatFilter
import at.hannibal2.skyhanni.features.chat.playerchat.PlayerChatModifier
import at.hannibal2.skyhanni.features.chroma.ChromaManager
import at.hannibal2.skyhanni.features.combat.BestiaryData
import at.hannibal2.skyhanni.features.combat.FerocityDisplay
import at.hannibal2.skyhanni.features.combat.FlareDisplay
import at.hannibal2.skyhanni.features.combat.HideDamageSplash
import at.hannibal2.skyhanni.features.combat.damageindicator.DamageIndicatorManager
import at.hannibal2.skyhanni.features.combat.endernodetracker.EnderNodeTracker
import at.hannibal2.skyhanni.features.combat.ghostcounter.GhostCounter
import at.hannibal2.skyhanni.features.combat.mobs.AreaMiniBossFeatures
import at.hannibal2.skyhanni.features.combat.mobs.AshfangMinisNametagHider
import at.hannibal2.skyhanni.features.combat.mobs.MobHighlight
import at.hannibal2.skyhanni.features.combat.mobs.SpawnTimers
import at.hannibal2.skyhanni.features.commands.PartyChatCommands
import at.hannibal2.skyhanni.features.commands.PartyCommands
import at.hannibal2.skyhanni.features.commands.SendCoordinatedCommand
import at.hannibal2.skyhanni.features.commands.ViewRecipeCommand
import at.hannibal2.skyhanni.features.commands.WarpIsCommand
import at.hannibal2.skyhanni.features.commands.WikiManager
import at.hannibal2.skyhanni.features.commands.tabcomplete.GetFromSacksTabComplete
import at.hannibal2.skyhanni.features.commands.tabcomplete.PlayerTabComplete
import at.hannibal2.skyhanni.features.commands.tabcomplete.TabComplete
import at.hannibal2.skyhanni.features.commands.tabcomplete.WarpTabComplete
import at.hannibal2.skyhanni.features.cosmetics.ArrowTrail
import at.hannibal2.skyhanni.features.cosmetics.CosmeticFollowingLine
import at.hannibal2.skyhanni.features.dungeon.CroesusChestTracker
import at.hannibal2.skyhanni.features.dungeon.DungeonAPI
import at.hannibal2.skyhanni.features.dungeon.DungeonArchitectFeatures
import at.hannibal2.skyhanni.features.dungeon.DungeonBossHideDamageSplash
import at.hannibal2.skyhanni.features.dungeon.DungeonBossMessages
import at.hannibal2.skyhanni.features.dungeon.DungeonChatFilter
import at.hannibal2.skyhanni.features.dungeon.DungeonCleanEnd
import at.hannibal2.skyhanni.features.dungeon.DungeonCopilot
import at.hannibal2.skyhanni.features.dungeon.DungeonDeathCounter
import at.hannibal2.skyhanni.features.dungeon.DungeonFinderFeatures
import at.hannibal2.skyhanni.features.dungeon.DungeonHideItems
import at.hannibal2.skyhanni.features.dungeon.DungeonHighlightClickedBlocks
import at.hannibal2.skyhanni.features.dungeon.DungeonLividFinder
import at.hannibal2.skyhanni.features.dungeon.DungeonMilestonesDisplay
import at.hannibal2.skyhanni.features.dungeon.DungeonRankTabListColor
import at.hannibal2.skyhanni.features.dungeon.DungeonShadowAssassinNotification
import at.hannibal2.skyhanni.features.dungeon.DungeonTeammateOutlines
import at.hannibal2.skyhanni.features.dungeon.DungeonsRaceGuide
import at.hannibal2.skyhanni.features.dungeon.HighlightDungeonDeathmite
import at.hannibal2.skyhanni.features.dungeon.TerracottaPhase
import at.hannibal2.skyhanni.features.event.UniqueGiftingOpportunitiesFeatures
import at.hannibal2.skyhanni.features.event.diana.AllBurrowsList
import at.hannibal2.skyhanni.features.event.diana.BurrowWarpHelper
import at.hannibal2.skyhanni.features.event.diana.DianaFixChat
import at.hannibal2.skyhanni.features.event.diana.DianaProfitTracker
import at.hannibal2.skyhanni.features.event.diana.GriffinBurrowHelper
import at.hannibal2.skyhanni.features.event.diana.GriffinBurrowParticleFinder
import at.hannibal2.skyhanni.features.event.diana.GriffinPetWarning
import at.hannibal2.skyhanni.features.event.diana.HighlightInquisitors
import at.hannibal2.skyhanni.features.event.diana.InquisitorWaypointShare
import at.hannibal2.skyhanni.features.event.diana.MythologicalCreatureTracker
import at.hannibal2.skyhanni.features.event.diana.SoopyGuessBurrow
import at.hannibal2.skyhanni.features.event.hoppity.HoppityCollectionStats
import at.hannibal2.skyhanni.features.event.hoppity.HoppityEggDisplayManager
import at.hannibal2.skyhanni.features.event.hoppity.HoppityEggLocator
import at.hannibal2.skyhanni.features.event.hoppity.HoppityEggsManager
import at.hannibal2.skyhanni.features.event.hoppity.HoppityEggsShared
import at.hannibal2.skyhanni.features.event.hoppity.HoppityNpc
import at.hannibal2.skyhanni.features.event.jerry.HighlightJerries
import at.hannibal2.skyhanni.features.event.jerry.frozentreasure.FrozenTreasureTracker
import at.hannibal2.skyhanni.features.event.lobby.waypoints.christmas.PresentWaypoints
import at.hannibal2.skyhanni.features.event.lobby.waypoints.easter.EasterEggWaypoints
import at.hannibal2.skyhanni.features.event.lobby.waypoints.halloween.BasketWaypoints
import at.hannibal2.skyhanni.features.event.spook.TheGreatSpook
import at.hannibal2.skyhanni.features.event.winter.JyrreTimer
import at.hannibal2.skyhanni.features.event.winter.NewYearCakeReminder
import at.hannibal2.skyhanni.features.event.winter.UniqueGiftCounter
import at.hannibal2.skyhanni.features.fame.AccountUpgradeReminder
import at.hannibal2.skyhanni.features.fame.CityProjectFeatures
import at.hannibal2.skyhanni.features.fishing.ChumBucketHider
import at.hannibal2.skyhanni.features.fishing.FishingAPI
import at.hannibal2.skyhanni.features.fishing.FishingBaitWarnings
import at.hannibal2.skyhanni.features.fishing.FishingHookDisplay
import at.hannibal2.skyhanni.features.fishing.FishingTimer
import at.hannibal2.skyhanni.features.fishing.IsFishingDetection
import at.hannibal2.skyhanni.features.fishing.SeaCreatureFeatures
import at.hannibal2.skyhanni.features.fishing.SeaCreatureManager
import at.hannibal2.skyhanni.features.fishing.SeaCreatureMessageShortener
import at.hannibal2.skyhanni.features.fishing.SharkFishCounter
import at.hannibal2.skyhanni.features.fishing.ShowFishingItemName
import at.hannibal2.skyhanni.features.fishing.ThunderSparksHighlight
import at.hannibal2.skyhanni.features.fishing.TotemOfCorruption
import at.hannibal2.skyhanni.features.fishing.tracker.FishingProfitTracker
import at.hannibal2.skyhanni.features.fishing.tracker.SeaCreatureTracker
import at.hannibal2.skyhanni.features.fishing.trophy.GeyserFishing
import at.hannibal2.skyhanni.features.fishing.trophy.OdgerWaypoint
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyFishFillet
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyFishManager
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyFishMessages
import at.hannibal2.skyhanni.features.garden.AnitaMedalProfit
import at.hannibal2.skyhanni.features.garden.AtmosphericFilterDisplay
import at.hannibal2.skyhanni.features.garden.FarmingFortuneDisplay
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.GardenCropMilestoneFix
import at.hannibal2.skyhanni.features.garden.GardenLevelDisplay
import at.hannibal2.skyhanni.features.garden.GardenNextJacobContest
import at.hannibal2.skyhanni.features.garden.GardenOptimalSpeed
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI
import at.hannibal2.skyhanni.features.garden.GardenPlotBorders
import at.hannibal2.skyhanni.features.garden.GardenWarpCommands
import at.hannibal2.skyhanni.features.garden.GardenYawAndPitch
import at.hannibal2.skyhanni.features.garden.SensitivityReducer
import at.hannibal2.skyhanni.features.garden.ToolTooltipTweaks
import at.hannibal2.skyhanni.features.garden.composter.ComposterDisplay
import at.hannibal2.skyhanni.features.garden.composter.ComposterInventoryNumbers
import at.hannibal2.skyhanni.features.garden.composter.ComposterOverlay
import at.hannibal2.skyhanni.features.garden.composter.GardenComposterInventoryFeatures
import at.hannibal2.skyhanni.features.garden.contest.FarmingContestAPI
import at.hannibal2.skyhanni.features.garden.contest.JacobContestFFNeededDisplay
import at.hannibal2.skyhanni.features.garden.contest.JacobContestStatsSummary
import at.hannibal2.skyhanni.features.garden.contest.JacobContestTimeNeeded
import at.hannibal2.skyhanni.features.garden.contest.JacobFarmingContestsInventory
import at.hannibal2.skyhanni.features.garden.farming.ArmorDropTracker
import at.hannibal2.skyhanni.features.garden.farming.CropMoneyDisplay
import at.hannibal2.skyhanni.features.garden.farming.CropSpeedMeter
import at.hannibal2.skyhanni.features.garden.farming.DicerRngDropTracker
import at.hannibal2.skyhanni.features.garden.farming.FarmingWeightDisplay
import at.hannibal2.skyhanni.features.garden.farming.GardenBestCropTime
import at.hannibal2.skyhanni.features.garden.farming.GardenBurrowingSporesNotifier
import at.hannibal2.skyhanni.features.garden.farming.GardenCropMilestoneDisplay
import at.hannibal2.skyhanni.features.garden.farming.GardenCropSpeed
import at.hannibal2.skyhanni.features.garden.farming.GardenCustomKeybinds
import at.hannibal2.skyhanni.features.garden.farming.GardenStartLocation
import at.hannibal2.skyhanni.features.garden.farming.WildStrawberryDyeNotification
import at.hannibal2.skyhanni.features.garden.farming.WrongFungiCutterWarning
import at.hannibal2.skyhanni.features.garden.farming.lane.FarmingLaneAPI
import at.hannibal2.skyhanni.features.garden.farming.lane.FarmingLaneCreator
import at.hannibal2.skyhanni.features.garden.farming.lane.FarmingLaneFeatures
import at.hannibal2.skyhanni.features.garden.fortuneguide.CaptureFarmingGear
import at.hannibal2.skyhanni.features.garden.inventory.AnitaExtraFarmingFortune
import at.hannibal2.skyhanni.features.garden.inventory.GardenCropMilestoneInventory
import at.hannibal2.skyhanni.features.garden.inventory.GardenInventoryNumbers
import at.hannibal2.skyhanni.features.garden.inventory.GardenInventoryTooltipOverflow
import at.hannibal2.skyhanni.features.garden.inventory.LogBookStats
import at.hannibal2.skyhanni.features.garden.inventory.SkyMartCopperPrice
import at.hannibal2.skyhanni.features.garden.inventory.plots.GardenNextPlotPrice
import at.hannibal2.skyhanni.features.garden.inventory.plots.GardenPlotIcon
import at.hannibal2.skyhanni.features.garden.inventory.plots.GardenPlotMenuHighlighting
import at.hannibal2.skyhanni.features.garden.pests.PestAPI
import at.hannibal2.skyhanni.features.garden.pests.PestFinder
import at.hannibal2.skyhanni.features.garden.pests.PestParticleLine
import at.hannibal2.skyhanni.features.garden.pests.PestParticleWaypoint
import at.hannibal2.skyhanni.features.garden.pests.PestProfitTracker
import at.hannibal2.skyhanni.features.garden.pests.PestSpawn
import at.hannibal2.skyhanni.features.garden.pests.PestSpawnTimer
import at.hannibal2.skyhanni.features.garden.pests.SprayDisplay
import at.hannibal2.skyhanni.features.garden.pests.SprayFeatures
import at.hannibal2.skyhanni.features.garden.pests.StereoHarmonyDisplay
import at.hannibal2.skyhanni.features.garden.visitor.GardenVisitorColorNames
import at.hannibal2.skyhanni.features.garden.visitor.GardenVisitorDropStatistics
import at.hannibal2.skyhanni.features.garden.visitor.GardenVisitorFeatures
import at.hannibal2.skyhanni.features.garden.visitor.GardenVisitorSupercraft
import at.hannibal2.skyhanni.features.garden.visitor.GardenVisitorTimer
import at.hannibal2.skyhanni.features.garden.visitor.HighlightVisitorsOutsideOfGarden
import at.hannibal2.skyhanni.features.garden.visitor.NPCVisitorFix
import at.hannibal2.skyhanni.features.garden.visitor.VisitorAPI
import at.hannibal2.skyhanni.features.garden.visitor.VisitorListener
import at.hannibal2.skyhanni.features.garden.visitor.VisitorRewardWarning
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.features.gui.quiver.QuiverDisplay
import at.hannibal2.skyhanni.features.gui.quiver.QuiverWarning
import at.hannibal2.skyhanni.features.inventory.AuctionsHighlighter
import at.hannibal2.skyhanni.features.inventory.ChestValue
import at.hannibal2.skyhanni.features.inventory.DojoRankDisplay
import at.hannibal2.skyhanni.features.inventory.HarpFeatures
import at.hannibal2.skyhanni.features.inventory.HideNotClickableItems
import at.hannibal2.skyhanni.features.inventory.HighlightBonzoMasks
import at.hannibal2.skyhanni.features.inventory.ItemDisplayOverlayFeatures
import at.hannibal2.skyhanni.features.inventory.ItemStars
import at.hannibal2.skyhanni.features.inventory.MaxPurseItems
import at.hannibal2.skyhanni.features.inventory.PowerStoneGuideFeatures
import at.hannibal2.skyhanni.features.inventory.QuickCraftFeatures
import at.hannibal2.skyhanni.features.inventory.RngMeterInventory
import at.hannibal2.skyhanni.features.inventory.SackDisplay
import at.hannibal2.skyhanni.features.inventory.ShiftClickBrewing
import at.hannibal2.skyhanni.features.inventory.ShiftClickEquipment
import at.hannibal2.skyhanni.features.inventory.ShiftClickNPCSell
import at.hannibal2.skyhanni.features.inventory.SkyblockGuideHighlightFeature
import at.hannibal2.skyhanni.features.inventory.StatsTuning
import at.hannibal2.skyhanni.features.inventory.SuperCraftFeatures
import at.hannibal2.skyhanni.features.inventory.auctionhouse.AuctionHouseCopyUnderbidPrice
import at.hannibal2.skyhanni.features.inventory.auctionhouse.AuctionHouseOpenPriceWebsite
import at.hannibal2.skyhanni.features.inventory.bazaar.BazaarApi
import at.hannibal2.skyhanni.features.inventory.bazaar.BazaarBestSellMethod
import at.hannibal2.skyhanni.features.inventory.bazaar.BazaarCancelledBuyOrderClipboard
import at.hannibal2.skyhanni.features.inventory.bazaar.BazaarOpenPriceWebsite
import at.hannibal2.skyhanni.features.inventory.bazaar.BazaarOrderHelper
import at.hannibal2.skyhanni.features.inventory.bazaar.CraftMaterialsFromBazaar
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.ChocolateFactoryAPI
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.ChocolateFactoryBarnManager
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.ChocolateFactoryCustomReminder
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.ChocolateFactoryDataLoader
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.ChocolateFactoryInventory
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.ChocolateFactoryKeybinds
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.ChocolateFactoryShortcut
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.ChocolateFactoryStats
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.ChocolateFactoryTimeTowerManager
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.ChocolateFactoryTooltip
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.ChocolateFactoryTooltipCompact
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.ChocolateFactoryUpgradeWarning
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.ChocolateShopPrice
import at.hannibal2.skyhanni.features.inventory.tiarelay.TiaRelayHelper
import at.hannibal2.skyhanni.features.inventory.tiarelay.TiaRelayWaypoints
import at.hannibal2.skyhanni.features.itemabilities.ChickenHeadTimer
import at.hannibal2.skyhanni.features.itemabilities.FireVeilWandParticles
import at.hannibal2.skyhanni.features.itemabilities.abilitycooldown.ItemAbilityCooldown
import at.hannibal2.skyhanni.features.mining.ColdOverlay
import at.hannibal2.skyhanni.features.mining.DeepCavernsGuide
import at.hannibal2.skyhanni.features.mining.GoldenGoblinHighlight
import at.hannibal2.skyhanni.features.mining.HighlightMiningCommissionMobs
import at.hannibal2.skyhanni.features.mining.KingTalismanHelper
import at.hannibal2.skyhanni.features.mining.MiningCommissionsBlocksColor
import at.hannibal2.skyhanni.features.mining.MiningNotifications
import at.hannibal2.skyhanni.features.mining.TunnelsMaps
import at.hannibal2.skyhanni.features.mining.crystalhollows.CrystalHollowsNamesInCore
import at.hannibal2.skyhanni.features.mining.crystalhollows.CrystalHollowsWalls
import at.hannibal2.skyhanni.features.mining.eventtracker.MiningEventDisplay
import at.hannibal2.skyhanni.features.mining.eventtracker.MiningEventTracker
import at.hannibal2.skyhanni.features.mining.fossilexcavator.ExcavatorProfitTracker
import at.hannibal2.skyhanni.features.mining.fossilexcavator.FossilExcavatorAPI
import at.hannibal2.skyhanni.features.mining.fossilexcavator.GlacitePowderFeatures
import at.hannibal2.skyhanni.features.mining.fossilexcavator.ProfitPerExcavation
import at.hannibal2.skyhanni.features.mining.fossilexcavator.solver.FossilSolverDisplay
import at.hannibal2.skyhanni.features.mining.mineshaft.CorpseAPI
import at.hannibal2.skyhanni.features.mining.mineshaft.MineshaftCorpseProfitPer
import at.hannibal2.skyhanni.features.mining.powdertracker.PowderTracker
import at.hannibal2.skyhanni.features.minion.InfernoMinionFeatures
import at.hannibal2.skyhanni.features.minion.MinionCollectLogic
import at.hannibal2.skyhanni.features.minion.MinionFeatures
import at.hannibal2.skyhanni.features.minion.MinionXp
import at.hannibal2.skyhanni.features.misc.AuctionHousePriceComparison
import at.hannibal2.skyhanni.features.misc.BetterSignEditing
import at.hannibal2.skyhanni.features.misc.BetterWikiFromMenus
import at.hannibal2.skyhanni.features.misc.BrewingStandOverlay
import at.hannibal2.skyhanni.features.misc.ButtonOnPause
import at.hannibal2.skyhanni.features.misc.CollectionTracker
import at.hannibal2.skyhanni.features.misc.ContributorManager
import at.hannibal2.skyhanni.features.misc.CopyPlaytime
import at.hannibal2.skyhanni.features.misc.CurrentPetDisplay
import at.hannibal2.skyhanni.features.misc.CustomTextBox
import at.hannibal2.skyhanni.features.misc.ExpOrbsOnGroundHider
import at.hannibal2.skyhanni.features.misc.FixGhostEntities
import at.hannibal2.skyhanni.features.misc.FixNEUHeavyPearls
import at.hannibal2.skyhanni.features.misc.HideArmor
import at.hannibal2.skyhanni.features.misc.HideFarEntities
import at.hannibal2.skyhanni.features.misc.InGameDateDisplay
import at.hannibal2.skyhanni.features.misc.JoinCrystalHollows
import at.hannibal2.skyhanni.features.misc.LesserOrbHider
import at.hannibal2.skyhanni.features.misc.LockMouseLook
import at.hannibal2.skyhanni.features.misc.MarkedPlayerManager
import at.hannibal2.skyhanni.features.misc.MiscFeatures
import at.hannibal2.skyhanni.features.misc.MovementSpeedDisplay
import at.hannibal2.skyhanni.features.misc.NoBitsWarning
import at.hannibal2.skyhanni.features.misc.NonGodPotEffectDisplay
import at.hannibal2.skyhanni.features.misc.ParticleHider
import at.hannibal2.skyhanni.features.misc.PartyMemberOutlines
import at.hannibal2.skyhanni.features.misc.PatcherSendCoordinates
import at.hannibal2.skyhanni.features.misc.PetCandyUsedDisplay
import at.hannibal2.skyhanni.features.misc.PetExpTooltip
import at.hannibal2.skyhanni.features.misc.PetItemDisplay
import at.hannibal2.skyhanni.features.misc.PocketSackInASackDisplay
import at.hannibal2.skyhanni.features.misc.PrivateIslandNoPickaxeAbility
import at.hannibal2.skyhanni.features.misc.QuickModMenuSwitch
import at.hannibal2.skyhanni.features.misc.ReplaceRomanNumerals
import at.hannibal2.skyhanni.features.misc.RestorePieceOfWizardPortalLore
import at.hannibal2.skyhanni.features.misc.ServerRestartTitle
import at.hannibal2.skyhanni.features.misc.SkyBlockKickDuration
import at.hannibal2.skyhanni.features.misc.SuperpairsClicksAlert
import at.hannibal2.skyhanni.features.misc.TabWidgetSettings
import at.hannibal2.skyhanni.features.misc.TimeFeatures
import at.hannibal2.skyhanni.features.misc.TpsCounter
import at.hannibal2.skyhanni.features.misc.compacttablist.AdvancedPlayerList
import at.hannibal2.skyhanni.features.misc.compacttablist.TabListReader
import at.hannibal2.skyhanni.features.misc.compacttablist.TabListRenderer
import at.hannibal2.skyhanni.features.misc.discordrpc.DiscordRPCManager
import at.hannibal2.skyhanni.features.misc.items.EstimatedItemValue
import at.hannibal2.skyhanni.features.misc.items.EstimatedWardrobePrice
import at.hannibal2.skyhanni.features.misc.items.GlowingDroppedItems
import at.hannibal2.skyhanni.features.misc.items.enchants.EnchantParser
import at.hannibal2.skyhanni.features.misc.limbo.LimboPlaytime
import at.hannibal2.skyhanni.features.misc.limbo.LimboTimeTracker
import at.hannibal2.skyhanni.features.misc.massconfiguration.DefaultConfigFeatures
import at.hannibal2.skyhanni.features.misc.teleportpad.TeleportPadCompactName
import at.hannibal2.skyhanni.features.misc.teleportpad.TeleportPadInventoryNumber
import at.hannibal2.skyhanni.features.misc.trevor.TrevorFeatures
import at.hannibal2.skyhanni.features.misc.trevor.TrevorSolver
import at.hannibal2.skyhanni.features.misc.trevor.TrevorTracker
import at.hannibal2.skyhanni.features.misc.update.UpdateManager
import at.hannibal2.skyhanni.features.misc.visualwords.ModifyVisualWords
import at.hannibal2.skyhanni.features.nether.MatriarchHelper
import at.hannibal2.skyhanni.features.nether.PabloHelper
import at.hannibal2.skyhanni.features.nether.SulphurSkitterBox
import at.hannibal2.skyhanni.features.nether.VolcanoExplosivityDisplay
import at.hannibal2.skyhanni.features.nether.ashfang.AshfangBlazes
import at.hannibal2.skyhanni.features.nether.ashfang.AshfangBlazingSouls
import at.hannibal2.skyhanni.features.nether.ashfang.AshfangFreezeCooldown
import at.hannibal2.skyhanni.features.nether.ashfang.AshfangGravityOrbs
import at.hannibal2.skyhanni.features.nether.ashfang.AshfangHideDamageIndicator
import at.hannibal2.skyhanni.features.nether.ashfang.AshfangHideParticles
import at.hannibal2.skyhanni.features.nether.ashfang.AshfangNextResetCooldown
import at.hannibal2.skyhanni.features.nether.kuudra.KuudraAPI
import at.hannibal2.skyhanni.features.nether.reputationhelper.CrimsonIsleReputationHelper
import at.hannibal2.skyhanni.features.rift.RiftAPI
import at.hannibal2.skyhanni.features.rift.area.colosseum.BlobbercystsHighlight
import at.hannibal2.skyhanni.features.rift.area.dreadfarm.RiftAgaricusCap
import at.hannibal2.skyhanni.features.rift.area.dreadfarm.RiftWiltedBerberisHelper
import at.hannibal2.skyhanni.features.rift.area.dreadfarm.VoltHighlighter
import at.hannibal2.skyhanni.features.rift.area.livingcave.LivingCaveDefenseBlocks
import at.hannibal2.skyhanni.features.rift.area.livingcave.LivingCaveLivingMetalHelper
import at.hannibal2.skyhanni.features.rift.area.livingcave.LivingMetalSuitProgress
import at.hannibal2.skyhanni.features.rift.area.mirrorverse.DanceRoomHelper
import at.hannibal2.skyhanni.features.rift.area.mirrorverse.RiftLavaMazeParkour
import at.hannibal2.skyhanni.features.rift.area.mirrorverse.RiftUpsideDownParkour
import at.hannibal2.skyhanni.features.rift.area.mirrorverse.TubulatorParkour
import at.hannibal2.skyhanni.features.rift.area.stillgorechateau.RiftBloodEffigies
import at.hannibal2.skyhanni.features.rift.area.westvillage.VerminHighlighter
import at.hannibal2.skyhanni.features.rift.area.westvillage.VerminTracker
import at.hannibal2.skyhanni.features.rift.area.westvillage.kloon.KloonHacking
import at.hannibal2.skyhanni.features.rift.area.wyldwoods.RiftLarva
import at.hannibal2.skyhanni.features.rift.area.wyldwoods.RiftOdonata
import at.hannibal2.skyhanni.features.rift.area.wyldwoods.ShyCruxWarnings
import at.hannibal2.skyhanni.features.rift.everywhere.CruxTalismanDisplay
import at.hannibal2.skyhanni.features.rift.everywhere.EnigmaSoulWaypoints
import at.hannibal2.skyhanni.features.rift.everywhere.HighlightRiftGuide
import at.hannibal2.skyhanni.features.rift.everywhere.RiftHorsezookaHider
import at.hannibal2.skyhanni.features.rift.everywhere.RiftTimer
import at.hannibal2.skyhanni.features.rift.everywhere.motes.RiftMotesOrb
import at.hannibal2.skyhanni.features.rift.everywhere.motes.ShowMotesNpcSellPrice
import at.hannibal2.skyhanni.features.skillprogress.SkillProgress
import at.hannibal2.skyhanni.features.skillprogress.SkillTooltip
import at.hannibal2.skyhanni.features.slayer.HideMobNames
import at.hannibal2.skyhanni.features.slayer.SlayerBossSpawnSoon
import at.hannibal2.skyhanni.features.slayer.SlayerItemsOnGround
import at.hannibal2.skyhanni.features.slayer.SlayerMiniBossFeatures
import at.hannibal2.skyhanni.features.slayer.SlayerProfitTracker
import at.hannibal2.skyhanni.features.slayer.SlayerQuestWarning
import at.hannibal2.skyhanni.features.slayer.SlayerRngMeterDisplay
import at.hannibal2.skyhanni.features.slayer.VampireSlayerFeatures
import at.hannibal2.skyhanni.features.slayer.blaze.BlazeSlayerClearView
import at.hannibal2.skyhanni.features.slayer.blaze.BlazeSlayerDaggerHelper
import at.hannibal2.skyhanni.features.slayer.blaze.BlazeSlayerFirePitsWarning
import at.hannibal2.skyhanni.features.slayer.blaze.FirePillarDisplay
import at.hannibal2.skyhanni.features.slayer.blaze.HellionShieldHelper
import at.hannibal2.skyhanni.features.slayer.enderman.EndermanSlayerFeatures
import at.hannibal2.skyhanni.features.slayer.enderman.EndermanSlayerHideParticles
import at.hannibal2.skyhanni.features.stranded.HighlightPlaceableNpcs
import at.hannibal2.skyhanni.features.summonings.SummoningMobManager
import at.hannibal2.skyhanni.features.summonings.SummoningSoulsName
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.test.HighlightMissingRepoItems
import at.hannibal2.skyhanni.test.PacketTest
import at.hannibal2.skyhanni.test.ParkourWaypointSaver
import at.hannibal2.skyhanni.test.ShowItemUuid
import at.hannibal2.skyhanni.test.SkyHanniDebugsAndTests
import at.hannibal2.skyhanni.test.TestBingo
import at.hannibal2.skyhanni.test.TestCopyBestiaryValues
import at.hannibal2.skyhanni.test.TestCopyRngMeterValues
import at.hannibal2.skyhanni.test.TestExportTools
import at.hannibal2.skyhanni.test.TestShowSlotNumber
import at.hannibal2.skyhanni.test.WorldEdit
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.test.command.TrackParticlesCommand
import at.hannibal2.skyhanni.test.command.TrackSoundsCommand
import at.hannibal2.skyhanni.test.hotswap.HotswapSupport
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.EntityOutlineRenderer
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.MinecraftConsoleFilter.Companion.initLogging
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NEUVersionCheck.checkIfNeuIsLoaded
import at.hannibal2.skyhanni.utils.TabListData
import at.hannibal2.skyhanni.utils.UtilsPatterns
import at.hannibal2.skyhanni.utils.renderables.RenderableTooltips
import at.hannibal2.skyhanni.utils.repopatterns.RepoPatternManager
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

@Mod(
    modid = SkyHanniMod.MODID,
    clientSideOnly = true,
    useMetadata = true,
    guiFactory = "at.hannibal2.skyhanni.config.ConfigGuiForgeInterop",
    version = "0.26.Beta.1",
)
class SkyHanniMod {

    @Mod.EventHandler
    fun preInit(event: FMLPreInitializationEvent?) {
        checkIfNeuIsLoaded()

        HotswapSupport.load()

        // data
        loadModule(this)
        loadModule(ChatManager)
        loadModule(PlayerChatManager())
        loadModule(PlayerNameFormatter())
        loadModule(HypixelData())
        loadModule(LocationFixData)
        loadModule(DungeonAPI)
        loadModule(ScoreboardData())
        loadModule(SeaCreatureFeatures())
        loadModule(SeaCreatureManager())
        loadModule(EntityData())
        loadModule(MobData())
        loadModule(MobDetection())
        loadModule(EntityMovementData)
        loadModule(TestExportTools)
        loadModule(ItemClickData())
//        loadModule(Year300RaffleEvent)
        loadModule(MinecraftData)
        loadModule(TitleManager())
        loadModule(ItemTipHelper())
        loadModule(RenderLivingEntityHelper())
        loadModule(SkillExperience())
        loadModule(OtherInventoryData)
        loadModule(TabListData)
        loadModule(RenderData())
        loadModule(GardenCropMilestones)
        loadModule(GardenCropMilestonesCommunityFix)
        loadModule(GardenCropUpgrades)
        loadModule(VisitorListener())
        loadModule(VisitorRewardWarning())
        loadModule(OwnInventoryData())
        loadModule(ScreenData)
        loadModule(HighlightVisitorsOutsideOfGarden())
        loadModule(GuiEditManager())
        loadModule(GetFromSackAPI)
        loadModule(UpdateManager)
        loadModule(CropAccessoryData)
        loadModule(GardenComposterUpgradesData())
        loadModule(ActionBarStatsData)
        loadModule(GardenCropMilestoneInventory())
        loadModule(GardenCropSpeed)
        loadModule(GardenWarpCommands())
        loadModule(ProfileStorageData)
        loadModule(TitleData())
        loadModule(BlockData())
        loadModule(DefaultConfigFeatures)
        loadModule(EntityOutlineRenderer)
        loadModule(KeyboardManager)
        loadModule(AdvancedPlayerList)
        loadModule(ItemAddManager())
        loadModule(BingoCardReader())
        loadModule(DeepCavernsGuide())
        loadModule(DungeonsRaceGuide())
        loadModule(GardenBestCropTime())
        loadModule(ActionBarData)
        loadModule(TrackerManager)
        loadModule(ScoreboardPattern)
        loadModule(UtilsPatterns)
        loadModule(GuiData)
        loadModule(BossbarData)
        loadModule(EntityUtils)
        loadModule(ChatUtils)
        loadModule(FixedRateTimerManager())
        loadModule(ChromaManager)
        loadModule(ContributorManager)
        loadModule(TabComplete)
        loadModule(HypixelBazaarFetcher)
        loadModule(EventCounter)

        // APIs
        loadModule(BazaarApi())
        loadModule(GardenAPI)
        loadModule(GardenPlotAPI)
        loadModule(DataWatcherAPI())
        loadModule(CollectionAPI)
        loadModule(FarmingContestAPI)
        loadModule(HighlightOnHoverSlot)
        loadModule(FriendAPI)
        loadModule(PartyAPI)
        loadModule(GuildAPI)
        loadModule(SlayerAPI)
        loadModule(PurseAPI)
        loadModule(RiftAPI)
        loadModule(SackAPI)
        loadModule(BingoAPI)
        loadModule(FameRanks)
        loadModule(FishingAPI)
        loadModule(MaxwellAPI)
        loadModule(QuiverAPI)
        loadModule(BitsAPI)
        loadModule(MayorAPI)
        loadModule(SkillAPI)
        loadModule(VisitorAPI)
        loadModule(KuudraAPI)
        loadModule(PetAPI)
        loadModule(IsFishingDetection)
        loadModule(LorenzUtils)
        loadModule(NEUItems)
        loadModule(PestAPI)
        loadModule(MiningAPI)
        loadModule(FossilExcavatorAPI)
        loadModule(ChocolateFactoryAPI)
        loadModule(RenderableTooltips)

        // features
        loadModule(BazaarOrderHelper())
        loadModule(AuctionsHighlighter)
        loadModule(ChatFilter())
        loadModule(PlayerChatModifier())
        loadModule(DungeonChatFilter())
        loadModule(HideNotClickableItems())
        loadModule(ItemDisplayOverlayFeatures)
        loadModule(CurrentPetDisplay())
        loadModule(HideFarEntities())
        loadModule(ExpOrbsOnGroundHider())
        loadModule(BetterWikiFromMenus())
        loadModule(DamageIndicatorManager())
        loadModule(ItemAbilityCooldown())
        loadModule(DungeonHighlightClickedBlocks())
        loadModule(DungeonMilestonesDisplay)
        loadModule(DungeonDeathCounter())
        loadModule(DungeonCleanEnd())
        loadModule(TunnelsMaps())
        loadModule(DungeonBossMessages())
        loadModule(DungeonBossHideDamageSplash())
        loadModule(UniqueGiftingOpportunitiesFeatures)
        loadModule(UniqueGiftCounter)
        loadModule(TrophyFishManager)
        loadModule(TrophyFishFillet())
        loadModule(TrophyFishMessages())
        loadModule(GeyserFishing())
        loadModule(BazaarBestSellMethod())
        loadModule(ShiftClickBrewing())
        loadModule(BazaarOpenPriceWebsite())
        loadModule(AuctionHouseCopyUnderbidPrice())
        loadModule(AuctionHouseOpenPriceWebsite())
        loadModule(AnvilCombineHelper())
        loadModule(SeaCreatureMessageShortener())
        loadModule(AshfangFreezeCooldown)
        loadModule(AshfangNextResetCooldown())
        loadModule(CrystalHollowsWalls())
        loadModule(SummoningSoulsName())
        loadModule(AshfangGravityOrbs())
        loadModule(AshfangBlazingSouls())
        loadModule(AshfangBlazes())
        loadModule(AshfangHideParticles())
        loadModule(AshfangHideDamageIndicator())
        loadModule(ItemStars())
        loadModule(MinionFeatures())
        loadModule(TimeFeatures())
        loadModule(RngMeterInventory())
        loadModule(WikiManager)
        loadModule(SendCoordinatedCommand())
        loadModule(WarpIsCommand())
        loadModule(ViewRecipeCommand)
        loadModule(PartyCommands)
        loadModule(PartyChatCommands)
        loadModule(SummoningMobManager())
        loadModule(SkyblockXPInChat())
        loadModule(AreaMiniBossFeatures())
        loadModule(MobHighlight())
        loadModule(ChocolateFactoryDataLoader)
        loadModule(ChocolateFactoryBarnManager)
        loadModule(ChocolateFactoryShortcut())
        loadModule(ChocolateFactoryInventory)
        loadModule(ChocolateFactoryStats)
        loadModule(ChocolateFactoryTooltipCompact)
        loadModule(ChocolateFactoryTimeTowerManager)
        loadModule(ChocolateFactoryTooltip)
        loadModule(ChocolateFactoryKeybinds)
        loadModule(ChocolateShopPrice)
        loadModule(ChocolateFactoryUpgradeWarning)
        loadModule(ChocolateFactoryCustomReminder)
        loadModule(HoppityNpc)
        loadModule(HoppityEggsManager)
        loadModule(HoppityEggLocator)
        loadModule(HoppityEggsShared)
        loadModule(HoppityEggDisplayManager)
        loadModule(HoppityCollectionStats)
        loadModule(SpawnTimers())
        loadModule(MarkedPlayerManager())
        loadModule(SlayerMiniBossFeatures())
        loadModule(PlayerDeathMessages())
        loadModule(HighlightDungeonDeathmite())
        loadModule(DungeonHideItems())
        loadModule(DungeonCopilot())
        loadModule(DungeonArchitectFeatures())
        loadModule(EndermanSlayerFeatures())
        loadModule(FireVeilWandParticles())
        loadModule(HideMobNames())
        loadModule(HideDamageSplash())
        loadModule(FerocityDisplay())
        loadModule(FlareDisplay)
        loadModule(InGameDateDisplay())
        loadModule(ThunderSparksHighlight())
        loadModule(BlazeSlayerDaggerHelper())
        loadModule(HellionShieldHelper())
        loadModule(BlazeSlayerFirePitsWarning())
        loadModule(BlazeSlayerClearView())
        loadModule(FirePillarDisplay())
        loadModule(EndermanSlayerHideParticles())
        loadModule(PlayerChatFilter())
        loadModule(HideArmor())
        loadModule(SlayerQuestWarning())
        loadModule(StatsTuning())
        loadModule(NonGodPotEffectDisplay())
        loadModule(SoopyGuessBurrow())
        loadModule(DianaProfitTracker)
        loadModule(DianaFixChat())
        loadModule(MythologicalCreatureTracker)
        loadModule(ShiftClickNPCSell)
        loadModule(HighlightJerries())
        loadModule(TheGreatSpook())
        loadModule(GriffinBurrowHelper)
        loadModule(AllBurrowsList)
        loadModule(GriffinBurrowParticleFinder)
        loadModule(BurrowWarpHelper())
        loadModule(CollectionTracker())
        loadModule(HighlightBonzoMasks)
        loadModule(BazaarCancelledBuyOrderClipboard())
        loadModule(CompactSplashPotionMessage())
        loadModule(CroesusChestTracker())
        loadModule(CompactBingoChat())
        loadModule(BrewingStandOverlay())
        loadModule(FishingTimer())
        loadModule(MatriarchHelper())
        loadModule(LesserOrbHider())
        loadModule(FishingHookDisplay())
        loadModule(CrimsonIsleReputationHelper(this))
        loadModule(SkyblockGuideHighlightFeature)
        loadModule(SharkFishCounter())
        loadModule(PowerStoneGuideFeatures())
        loadModule(OdgerWaypoint())
        loadModule(TiaRelayHelper())
        loadModule(TiaRelayWaypoints())
        loadModule(BasketWaypoints())
        loadModule(EasterEggWaypoints())
        loadModule(BingoCardDisplay())
        loadModule(BingoNextStepHelper())
        loadModule(MinionCraftHelper())
        loadModule(TpsCounter())
        loadModule(ParticleHider())
        loadModule(MiscFeatures)
        loadModule(ReplaceRomanNumerals())
        loadModule(GardenPlotMenuHighlighting())
        loadModule(SkyMartCopperPrice())
        loadModule(GardenVisitorFeatures)
        loadModule(GardenVisitorSupercraft())
        loadModule(NPCVisitorFix)
        loadModule(GardenInventoryNumbers())
        loadModule(GardenVisitorTimer())
        loadModule(MinionXp())
        loadModule(GardenNextPlotPrice())
        loadModule(GardenCropMilestoneDisplay)
        loadModule(GardenCustomKeybinds)
        loadModule(ChickenHeadTimer())
        loadModule(FossilSolverDisplay)
        loadModule(ExcavatorProfitTracker())
        loadModule(ProfitPerExcavation())
        loadModule(GlacitePowderFeatures())
        loadModule(MineshaftCorpseProfitPer())
        loadModule(CorpseAPI())
        loadModule(GardenOptimalSpeed())
        loadModule(GardenLevelDisplay())
        loadModule(FarmingWeightDisplay())
        loadModule(DicerRngDropTracker)
        loadModule(PrivateIslandNoPickaxeAbility())
        loadModule(CropMoneyDisplay)
        loadModule(JacobFarmingContestsInventory())
        loadModule(GardenNextJacobContest)
        loadModule(WrongFungiCutterWarning())
        loadModule(ArmorDropTracker)
        loadModule(FarmingLaneAPI)
        loadModule(FarmingLaneFeatures)
        loadModule(FarmingLaneCreator)
        loadModule(JoinCrystalHollows())
        loadModule(CrystalHollowsNamesInCore())
        loadModule(GardenVisitorColorNames)
        loadModule(TeleportPadCompactName())
        loadModule(AnitaMedalProfit())
        loadModule(AtmosphericFilterDisplay())
        loadModule(AnitaExtraFarmingFortune())
        loadModule(ComposterDisplay())
        loadModule(GardenComposterInventoryFeatures())
        loadModule(MinionCollectLogic())
        loadModule(BetterSignEditing())
        loadModule(PatcherSendCoordinates())
        loadModule(PetItemDisplay())
        loadModule(EstimatedItemValue)
        loadModule(EstimatedWardrobePrice())
        loadModule(ComposterInventoryNumbers())
        loadModule(FarmingFortuneDisplay)
        loadModule(ToolTooltipTweaks())
        loadModule(CropSpeedMeter())
        loadModule(AshfangMinisNametagHider())
        loadModule(TeleportPadInventoryNumber())
        loadModule(ComposterOverlay)
        loadModule(DiscordRPCManager)
        loadModule(GardenCropMilestoneFix())
        loadModule(GardenBurrowingSporesNotifier())
        loadModule(WildStrawberryDyeNotification())
        loadModule(JacobContestFFNeededDisplay())
        loadModule(JacobContestTimeNeeded())
        loadModule(JacobContestStatsSummary())
        loadModule(GardenYawAndPitch())
        loadModule(MovementSpeedDisplay())
        loadModule(ChumBucketHider())
        loadModule(InquisitorWaypointShare)
        loadModule(TrevorFeatures)
        loadModule(TrevorSolver)
        loadModule(TrevorTracker)
        loadModule(BingoCardTips())
        loadModule(GardenVisitorDropStatistics)
        loadModule(CaptureFarmingGear)
        loadModule(SackDisplay)
        loadModule(GardenStartLocation)
        loadModule(PetCandyUsedDisplay())
        loadModule(ServerRestartTitle)
        loadModule(CityProjectFeatures())
        loadModule(GardenPlotIcon)
        loadModule(GardenPlotBorders)
        loadModule(LogBookStats())
        loadModule(PocketSackInASackDisplay())
        loadModule(ShowFishingItemName())
        loadModule(WarpTabComplete)
        loadModule(PlayerTabComplete)
        loadModule(GetFromSacksTabComplete)
        loadModule(SlayerProfitTracker)
        loadModule(FishingProfitTracker)
        loadModule(SeaCreatureTracker)
        loadModule(SlayerItemsOnGround())
        loadModule(RestorePieceOfWizardPortalLore())
        loadModule(QuickModMenuSwitch)
        loadModule(ArachneChatMessageHider())
        loadModule(ShowItemUuid())
        loadModule(FrozenTreasureTracker)
        loadModule(MiningEventDisplay)
        loadModule(MiningCommissionsBlocksColor)
        loadModule(SlayerRngMeterDisplay())
        loadModule(GhostCounter)
        loadModule(RiftTimer())
        loadModule(HighlightRiftGuide())
        loadModule(ShyCruxWarnings())
        loadModule(RiftLarva())
        loadModule(VoltHighlighter())
        loadModule(RiftOdonata())
        loadModule(RiftAgaricusCap())
        loadModule(KloonHacking())
        loadModule(EnigmaSoulWaypoints)
        loadModule(DungeonLividFinder)
        loadModule(CruxTalismanDisplay)
        loadModule(DanceRoomHelper)
        loadModule(TubulatorParkour())
        loadModule(CustomTextBox())
        loadModule(RiftUpsideDownParkour())
        loadModule(RiftLavaMazeParkour())
        loadModule(HighlightMiningCommissionMobs())
        loadModule(ShowMotesNpcSellPrice())
        loadModule(LivingMetalSuitProgress())
        loadModule(VampireSlayerFeatures)
        loadModule(BlobbercystsHighlight())
        loadModule(LivingCaveDefenseBlocks())
        loadModule(LivingCaveLivingMetalHelper())
        loadModule(RiftMotesOrb())
        loadModule(ChestValue())
        loadModule(SlayerBossSpawnSoon())
        loadModule(RiftBloodEffigies)
        loadModule(RiftWiltedBerberisHelper())
        loadModule(RiftHorsezookaHider())
        loadModule(GriffinPetWarning())
        loadModule(BestiaryData)
        loadModule(KingTalismanHelper())
        loadModule(HarpFeatures)
        loadModule(EnderNodeTracker)
        loadModule(CompactBestiaryChatMessage())
        loadModule(WatchdogHider())
        loadModule(AuctionHousePriceComparison())
        loadModule(AccountUpgradeReminder())
        loadModule(PetExpTooltip())
        loadModule(Translator())
        loadModule(CosmeticFollowingLine())
        loadModule(SuperpairsClicksAlert())
        loadModule(PowderTracker)
        loadModule(ModifyVisualWords)
        loadModule(TabListReader)
        loadModule(TabListRenderer)
        loadModule(GlowingDroppedItems())
        loadModule(DungeonTeammateOutlines())
        loadModule(DungeonRankTabListColor())
        loadModule(TerracottaPhase())
        loadModule(VolcanoExplosivityDisplay())
        loadModule(FixNEUHeavyPearls())
        loadModule(QuickCraftFeatures())
        loadModule(SkyBlockKickDuration())
        loadModule(LimboTimeTracker)
        loadModule(PartyMemberOutlines())
        loadModule(ArrowTrail())
        loadModule(ShiftClickEquipment())
        loadModule(LockMouseLook)
        loadModule(SensitivityReducer)
        loadModule(DungeonFinderFeatures())
        loadModule(GoldenGoblinHighlight())
        loadModule(TabWidgetSettings())
        loadModule(EnchantParser)
        loadModule(PabloHelper())
        loadModule(FishingBaitWarnings())
        loadModule(CustomScoreboard())
        loadModule(RepoPatternManager)
        loadModule(PestSpawn())
        loadModule(PestSpawnTimer)
        loadModule(PestFinder)
        loadModule(PestParticleWaypoint())
        loadModule(StereoHarmonyDisplay())
        loadModule(PestParticleLine())
        loadModule(SprayFeatures())
        loadModule(DojoRankDisplay())
        loadModule(SprayDisplay())
        loadModule(HighlightPlaceableNpcs())
        loadModule(PresentWaypoints())
        loadModule(MiningEventTracker())
        loadModule(MiningNotifications)
        loadModule(JyrreTimer())
        loadModule(TotemOfCorruption())
        loadModule(NewYearCakeReminder())
        loadModule(SulphurSkitterBox())
        loadModule(HighlightInquisitors())
        loadModule(VerminTracker)
        loadModule(VerminHighlighter())
        loadModule(SkillProgress)
        loadModule(GardenInventoryTooltipOverflow())
        loadModule(SkillTooltip())
        loadModule(MaxPurseItems())
        loadModule(SuperCraftFeatures)
        loadModule(InfernoMinionFeatures())
        loadModule(LimboPlaytime)
        loadModule(CopyPlaytime)
        loadModule(RareDropMessages())
        loadModule(CraftMaterialsFromBazaar())
        loadModule(DungeonShadowAssassinNotification())
        loadModule(PestProfitTracker)
        loadModule(NoBitsWarning)
        loadModule(ColdOverlay())
        loadModule(QuiverDisplay())
        loadModule(QuiverWarning())

        // test stuff
        loadModule(SkyHanniDebugsAndTests())
        loadModule(FixGhostEntities)
        loadModule(TrackSoundsCommand)
        loadModule(TrackParticlesCommand)
        loadModule(ButtonOnPause())
        loadModule(PacketTest)
        loadModule(TestBingo)
        loadModule(TestCopyRngMeterValues)
        loadModule(TestCopyBestiaryValues)
        loadModule(HighlightMissingRepoItems())
        loadModule(ParkourWaypointSaver())
        loadModule(TestShowSlotNumber())
        loadModule(SkyHanniDebugsAndTests)
        loadModule(WorldEdit)
        loadModule(MobDebug())

        Commands.init()

        PreInitFinishedEvent().postAndCatch()
    }

    @Mod.EventHandler
    fun init(event: FMLInitializationEvent?) {
        configManager = ConfigManager()
        configManager.firstLoad()
        initLogging()
        Runtime.getRuntime().addShutdownHook(Thread {
            configManager.saveConfig(ConfigFileType.FEATURES, "shutdown-hook")
        })
        repo = RepoManager(ConfigManager.configDirectory)
        loadModule(repo)
        try {
            repo.loadRepoInformation()
        } catch (e: Exception) {
            Exception("Error reading repo data", e).printStackTrace()
        }
    }

    fun loadModule(obj: Any) {
        modules.add(obj)
        MinecraftForge.EVENT_BUS.register(obj)
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (screenToOpen != null) {
            screenTicks++
            if (screenTicks == 5) {
                Minecraft.getMinecraft().thePlayer.closeScreen()
                OtherInventoryData.close()
                Minecraft.getMinecraft().displayGuiScreen(screenToOpen)
                screenTicks = 0
                screenToOpen = null
            }
        }
    }

    companion object {

        const val MODID = "skyhanni"

        @JvmStatic
        val version: String
            get() = Loader.instance().indexedModList[MODID]!!.version

        @JvmStatic
        val feature: Features get() = configManager.features
        val sackData: SackData get() = configManager.sackData
        val friendsData: FriendsJson get() = configManager.friendsData
        val knownFeaturesData: KnownFeaturesJson get() = configManager.knownFeaturesData
        val jacobContestsData: JacobContestsJson get() = configManager.jacobContestData
        val visualWordsData: VisualWordsJson get() = configManager.visualWordsData

        lateinit var repo: RepoManager
        lateinit var configManager: ConfigManager
        val logger: Logger = LogManager.getLogger("SkyHanni")
        fun getLogger(name: String): Logger {
            return LogManager.getLogger("SkyHanni.$name")
        }

        val modules: MutableList<Any> = ArrayList()
        private val globalJob: Job = Job(null)
        val coroutineScope = CoroutineScope(
            CoroutineName("SkyHanni") + SupervisorJob(globalJob)
        )
        var screenToOpen: GuiScreen? = null
        private var screenTicks = 0
        fun consoleLog(message: String) {
            logger.log(Level.INFO, message)
        }

        fun launchCoroutine(function: suspend () -> Unit) {
            coroutineScope.launch {
                try {
                    function()
                } catch (ex: Exception) {
                    ErrorManager.logErrorWithData(ex, "Asynchronous exception caught")
                }
            }
        }
    }
}
