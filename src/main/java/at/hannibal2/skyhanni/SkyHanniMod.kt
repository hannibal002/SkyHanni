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
import at.hannibal2.skyhanni.test.command.CopyNearbyParticlesCommand
import at.hannibal2.skyhanni.test.command.ErrorManager
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
        loadModule(ActionBarData)
        loadModule(ActionBarStatsData)
        loadModule(AdvancedPlayerList)
        loadModule(BingoCardReader())
        loadModule(BlockData())
        loadModule(BossbarData)
        loadModule(ChatManager)
        loadModule(ChatUtils)
        loadModule(ChromaManager)
        loadModule(ContributorManager)
        loadModule(CropAccessoryData)
        loadModule(DefaultConfigFeatures)
        loadModule(DungeonAPI)
        loadModule(EntityData())
        loadModule(EntityMovementData)
        loadModule(EntityOutlineRenderer)
        loadModule(EntityUtils)
        loadModule(EventCounter)
        loadModule(FixedRateTimerManager())
        loadModule(GardenBestCropTime())
        loadModule(GardenComposterUpgradesData())
        loadModule(GardenCropMilestoneInventory())
        loadModule(GardenCropMilestones)
        loadModule(GardenCropMilestonesCommunityFix)
        loadModule(GardenCropSpeed)
        loadModule(GardenCropUpgrades)
        loadModule(GardenWarpCommands())
        loadModule(GetFromSackAPI)
        loadModule(GuiData)
        loadModule(GuiEditManager())
        loadModule(HighlightVisitorsOutsideOfGarden())
        loadModule(HypixelBazaarFetcher)
        loadModule(HypixelData())
        loadModule(ItemAddManager())
        loadModule(ItemClickData())
        loadModule(ItemTipHelper())
        loadModule(KeyboardManager)
        loadModule(LocationFixData)
        loadModule(MinecraftData)
        loadModule(MobData())
        loadModule(MobDetection())
        loadModule(OtherInventoryData)
        loadModule(OwnInventoryData())
        loadModule(PlayerChatManager())
        loadModule(PlayerNameFormatter())
        loadModule(ProfileStorageData)
        loadModule(RenderData())
        loadModule(RenderLivingEntityHelper())
        loadModule(ScoreboardData())
        loadModule(ScoreboardPattern)
        loadModule(ScreenData)
        loadModule(SeaCreatureFeatures())
        loadModule(SeaCreatureManager())
        loadModule(SkillExperience())
        loadModule(TabComplete)
        loadModule(TabListData)
        loadModule(TestExportTools)
        loadModule(TitleData())
        loadModule(TitleManager())
        loadModule(TrackerManager)
        loadModule(UpdateManager)
        loadModule(UtilsPatterns)
        loadModule(VisitorListener())
        loadModule(VisitorRewardWarning())
//        loadModule(Year300RaffleEvent)

        // APIs
        loadModule(BazaarApi())
        loadModule(BingoAPI)
        loadModule(BitsAPI)
        loadModule(ChocolateFactoryAPI)
        loadModule(CollectionAPI)
        loadModule(DataWatcherAPI())
        loadModule(FameRanks)
        loadModule(FarmingContestAPI)
        loadModule(FishingAPI)
        loadModule(FossilExcavatorAPI)
        loadModule(FriendAPI)
        loadModule(GardenAPI)
        loadModule(GardenPlotAPI)
        loadModule(GuildAPI)
        loadModule(HighlightOnHoverSlot)
        loadModule(IsFishingDetection)
        loadModule(KuudraAPI)
        loadModule(LorenzUtils)
        loadModule(MaxwellAPI)
        loadModule(MayorAPI)
        loadModule(MiningAPI)
        loadModule(NEUItems)
        loadModule(PartyAPI)
        loadModule(PestAPI)
        loadModule(PetAPI)
        loadModule(PurseAPI)
        loadModule(QuiverAPI)
        loadModule(RenderableTooltips)
        loadModule(RiftAPI)
        loadModule(SackAPI)
        loadModule(SkillAPI)
        loadModule(SlayerAPI)
        loadModule(VisitorAPI)

        // features
        loadModule(AccountUpgradeReminder())
        loadModule(AllBurrowsList)
        loadModule(AnitaExtraFarmingFortune())
        loadModule(AnitaMedalProfit())
        loadModule(AnvilCombineHelper())
        loadModule(ArachneChatMessageHider())
        loadModule(AreaMiniBossFeatures())
        loadModule(ArmorDropTracker)
        loadModule(ArrowTrail())
        loadModule(AshfangBlazes())
        loadModule(AshfangBlazingSouls())
        loadModule(AshfangFreezeCooldown)
        loadModule(AshfangGravityOrbs())
        loadModule(AshfangHideDamageIndicator())
        loadModule(AshfangHideParticles())
        loadModule(AshfangMinisNametagHider())
        loadModule(AshfangNextResetCooldown())
        loadModule(AtmosphericFilterDisplay())
        loadModule(AuctionHouseCopyUnderbidPrice())
        loadModule(AuctionHouseOpenPriceWebsite())
        loadModule(AuctionHousePriceComparison())
        loadModule(AuctionsHighlighter)
        loadModule(BasketWaypoints())
        loadModule(BazaarBestSellMethod())
        loadModule(BazaarCancelledBuyOrderClipboard())
        loadModule(BazaarOpenPriceWebsite())
        loadModule(BazaarOrderHelper())
        loadModule(BestiaryData)
        loadModule(BetterSignEditing())
        loadModule(BetterWikiFromMenus())
        loadModule(BingoCardDisplay())
        loadModule(BingoCardTips())
        loadModule(BingoNextStepHelper())
        loadModule(BlazeSlayerClearView())
        loadModule(BlazeSlayerDaggerHelper())
        loadModule(BlazeSlayerFirePitsWarning())
        loadModule(BlobbercystsHighlight())
        loadModule(BrewingStandOverlay())
        loadModule(BurrowWarpHelper())
        loadModule(CaptureFarmingGear)
        loadModule(ChatFilter())
        loadModule(ChestValue())
        loadModule(ChickenHeadTimer())
        loadModule(ChocolateFactoryBarnManager)
        loadModule(ChocolateFactoryCustomReminder)
        loadModule(ChocolateFactoryDataLoader)
        loadModule(ChocolateFactoryInventory)
        loadModule(ChocolateFactoryKeybinds)
        loadModule(ChocolateFactoryShortcut())
        loadModule(ChocolateFactoryStats)
        loadModule(ChocolateFactoryTimeTowerManager)
        loadModule(ChocolateFactoryTooltip)
        loadModule(ChocolateFactoryTooltipCompact)
        loadModule(ChocolateFactoryUpgradeWarning)
        loadModule(ChocolateShopPrice)
        loadModule(ChumBucketHider())
        loadModule(CityProjectFeatures())
        loadModule(ColdOverlay())
        loadModule(CollectionTracker())
        loadModule(CompactBestiaryChatMessage())
        loadModule(CompactBingoChat())
        loadModule(CompactSplashPotionMessage())
        loadModule(ComposterDisplay())
        loadModule(ComposterInventoryNumbers())
        loadModule(ComposterOverlay)
        loadModule(CopyPlaytime)
        loadModule(CorpseAPI())
        loadModule(CosmeticFollowingLine())
        loadModule(CraftMaterialsFromBazaar())
        loadModule(CrimsonIsleReputationHelper(this))
        loadModule(CroesusChestTracker())
        loadModule(CropMoneyDisplay)
        loadModule(CropSpeedMeter())
        loadModule(CruxTalismanDisplay)
        loadModule(CrystalHollowsNamesInCore())
        loadModule(CrystalHollowsWalls())
        loadModule(CurrentPetDisplay())
        loadModule(CustomScoreboard())
        loadModule(CustomTextBox())
        loadModule(DamageIndicatorManager())
        loadModule(DanceRoomHelper)
        loadModule(DeepCavernsGuide())
        loadModule(DianaFixChat())
        loadModule(DianaProfitTracker)
        loadModule(DicerRngDropTracker)
        loadModule(DiscordRPCManager)
        loadModule(DojoRankDisplay())
        loadModule(DungeonArchitectFeatures())
        loadModule(DungeonBossHideDamageSplash())
        loadModule(DungeonBossMessages())
        loadModule(DungeonChatFilter())
        loadModule(DungeonCleanEnd())
        loadModule(DungeonCopilot())
        loadModule(DungeonDeathCounter())
        loadModule(DungeonFinderFeatures())
        loadModule(DungeonHideItems())
        loadModule(DungeonHighlightClickedBlocks())
        loadModule(DungeonLividFinder)
        loadModule(DungeonMilestonesDisplay)
        loadModule(DungeonRankTabListColor())
        loadModule(DungeonShadowAssassinNotification())
        loadModule(DungeonTeammateOutlines())
        loadModule(DungeonsRaceGuide())
        loadModule(EasterEggWaypoints())
        loadModule(EnchantParser)
        loadModule(EnderNodeTracker)
        loadModule(EndermanSlayerFeatures())
        loadModule(EndermanSlayerHideParticles())
        loadModule(EnigmaSoulWaypoints)
        loadModule(EstimatedItemValue)
        loadModule(EstimatedWardrobePrice())
        loadModule(ExcavatorProfitTracker())
        loadModule(ExpOrbsOnGroundHider())
        loadModule(FarmingFortuneDisplay)
        loadModule(FarmingLaneAPI)
        loadModule(FarmingLaneCreator)
        loadModule(FarmingLaneFeatures)
        loadModule(FarmingWeightDisplay())
        loadModule(FerocityDisplay())
        loadModule(FirePillarDisplay())
        loadModule(FireVeilWandParticles())
        loadModule(FishingBaitWarnings())
        loadModule(FishingHookDisplay())
        loadModule(FishingProfitTracker)
        loadModule(FishingTimer())
        loadModule(FixNEUHeavyPearls())
        loadModule(FlareDisplay)
        loadModule(FossilSolverDisplay)
        loadModule(FrozenTreasureTracker)
        loadModule(GardenBurrowingSporesNotifier())
        loadModule(GardenComposterInventoryFeatures())
        loadModule(GardenCropMilestoneDisplay)
        loadModule(GardenCropMilestoneFix())
        loadModule(GardenCustomKeybinds)
        loadModule(GardenInventoryNumbers())
        loadModule(GardenInventoryTooltipOverflow())
        loadModule(GardenLevelDisplay())
        loadModule(GardenNextJacobContest)
        loadModule(GardenNextPlotPrice())
        loadModule(GardenOptimalSpeed())
        loadModule(GardenPlotBorders)
        loadModule(GardenPlotIcon)
        loadModule(GardenPlotMenuHighlighting())
        loadModule(GardenStartLocation)
        loadModule(GardenVisitorColorNames)
        loadModule(GardenVisitorDropStatistics)
        loadModule(GardenVisitorFeatures)
        loadModule(GardenVisitorSupercraft())
        loadModule(GardenVisitorTimer())
        loadModule(GardenYawAndPitch())
        loadModule(GetFromSacksTabComplete)
        loadModule(GeyserFishing())
        loadModule(GhostCounter)
        loadModule(GlacitePowderFeatures())
        loadModule(GlowingDroppedItems())
        loadModule(GoldenGoblinHighlight())
        loadModule(GriffinBurrowHelper)
        loadModule(GriffinBurrowParticleFinder)
        loadModule(GriffinPetWarning())
        loadModule(HarpFeatures)
        loadModule(HellionShieldHelper())
        loadModule(HideArmor())
        loadModule(HideDamageSplash())
        loadModule(HideFarEntities())
        loadModule(HideMobNames())
        loadModule(HideNotClickableItems())
        loadModule(HighlightBonzoMasks)
        loadModule(HighlightDungeonDeathmite())
        loadModule(HighlightInquisitors())
        loadModule(HighlightJerries())
        loadModule(HighlightMiningCommissionMobs())
        loadModule(HighlightPlaceableNpcs())
        loadModule(HighlightRiftGuide())
        loadModule(HoppityCollectionStats)
        loadModule(HoppityEggDisplayManager)
        loadModule(HoppityEggLocator)
        loadModule(HoppityEggsManager)
        loadModule(HoppityEggsShared)
        loadModule(HoppityNpc)
        loadModule(InGameDateDisplay())
        loadModule(InfernoMinionFeatures())
        loadModule(InquisitorWaypointShare)
        loadModule(ItemAbilityCooldown())
        loadModule(ItemDisplayOverlayFeatures)
        loadModule(ItemStars())
        loadModule(JacobContestFFNeededDisplay())
        loadModule(JacobContestStatsSummary())
        loadModule(JacobContestTimeNeeded())
        loadModule(JacobFarmingContestsInventory())
        loadModule(JoinCrystalHollows())
        loadModule(JyrreTimer())
        loadModule(KingTalismanHelper())
        loadModule(KloonHacking())
        loadModule(LesserOrbHider())
        loadModule(LimboPlaytime)
        loadModule(LimboTimeTracker)
        loadModule(LivingCaveDefenseBlocks())
        loadModule(LivingCaveLivingMetalHelper())
        loadModule(LivingMetalSuitProgress())
        loadModule(LockMouseLook)
        loadModule(LogBookStats())
        loadModule(MarkedPlayerManager())
        loadModule(MatriarchHelper())
        loadModule(MaxPurseItems())
        loadModule(MineshaftCorpseProfitPer())
        loadModule(MiningCommissionsBlocksColor)
        loadModule(MiningEventDisplay)
        loadModule(MiningEventTracker())
        loadModule(MiningNotifications)
        loadModule(MinionCollectLogic())
        loadModule(MinionCraftHelper())
        loadModule(MinionFeatures())
        loadModule(MinionXp())
        loadModule(MiscFeatures)
        loadModule(MobHighlight())
        loadModule(ModifyVisualWords)
        loadModule(MovementSpeedDisplay())
        loadModule(MythologicalCreatureTracker)
        loadModule(NPCVisitorFix)
        loadModule(NewYearCakeReminder())
        loadModule(NoBitsWarning)
        loadModule(NonGodPotEffectDisplay())
        loadModule(OdgerWaypoint())
        loadModule(PabloHelper())
        loadModule(ParticleHider())
        loadModule(PartyChatCommands)
        loadModule(PartyCommands)
        loadModule(PartyMemberOutlines())
        loadModule(PatcherSendCoordinates())
        loadModule(PestFinder)
        loadModule(PestParticleLine())
        loadModule(PestParticleWaypoint())
        loadModule(PestProfitTracker)
        loadModule(PestSpawn())
        loadModule(PestSpawnTimer)
        loadModule(PetCandyUsedDisplay())
        loadModule(PetExpTooltip())
        loadModule(PetItemDisplay())
        loadModule(PlayerChatFilter())
        loadModule(PlayerChatModifier())
        loadModule(PlayerDeathMessages())
        loadModule(PlayerTabComplete)
        loadModule(PocketSackInASackDisplay())
        loadModule(PowderTracker)
        loadModule(PowerStoneGuideFeatures())
        loadModule(PresentWaypoints())
        loadModule(PrivateIslandNoPickaxeAbility())
        loadModule(ProfitPerExcavation())
        loadModule(QuickCraftFeatures())
        loadModule(QuickModMenuSwitch)
        loadModule(QuiverDisplay())
        loadModule(QuiverWarning())
        loadModule(RareDropMessages())
        loadModule(ReplaceRomanNumerals())
        loadModule(RepoPatternManager)
        loadModule(RestorePieceOfWizardPortalLore())
        loadModule(RiftAgaricusCap())
        loadModule(RiftBloodEffigies)
        loadModule(RiftHorsezookaHider())
        loadModule(RiftLarva())
        loadModule(RiftLavaMazeParkour())
        loadModule(RiftMotesOrb())
        loadModule(RiftOdonata())
        loadModule(RiftTimer())
        loadModule(RiftUpsideDownParkour())
        loadModule(RiftWiltedBerberisHelper())
        loadModule(RngMeterInventory())
        loadModule(SackDisplay)
        loadModule(SeaCreatureMessageShortener())
        loadModule(SeaCreatureTracker)
        loadModule(SendCoordinatedCommand())
        loadModule(SensitivityReducer)
        loadModule(ServerRestartTitle)
        loadModule(SharkFishCounter())
        loadModule(ShiftClickBrewing())
        loadModule(ShiftClickEquipment())
        loadModule(ShiftClickNPCSell)
        loadModule(ShowFishingItemName())
        loadModule(ShowItemUuid())
        loadModule(ShowMotesNpcSellPrice())
        loadModule(ShyCruxWarnings())
        loadModule(SkillProgress)
        loadModule(SkillTooltip())
        loadModule(SkyBlockKickDuration())
        loadModule(SkyMartCopperPrice())
        loadModule(SkyblockGuideHighlightFeature)
        loadModule(SkyblockXPInChat())
        loadModule(SlayerBossSpawnSoon())
        loadModule(SlayerItemsOnGround())
        loadModule(SlayerMiniBossFeatures())
        loadModule(SlayerProfitTracker)
        loadModule(SlayerQuestWarning())
        loadModule(SlayerRngMeterDisplay())
        loadModule(SoopyGuessBurrow())
        loadModule(SpawnTimers())
        loadModule(SprayDisplay())
        loadModule(SprayFeatures())
        loadModule(StatsTuning())
        loadModule(StereoHarmonyDisplay())
        loadModule(SulphurSkitterBox())
        loadModule(SummoningMobManager())
        loadModule(SummoningSoulsName())
        loadModule(SuperCraftFeatures)
        loadModule(SuperpairsClicksAlert())
        loadModule(TabListReader)
        loadModule(TabListRenderer)
        loadModule(TabWidgetSettings())
        loadModule(TeleportPadCompactName())
        loadModule(TeleportPadInventoryNumber())
        loadModule(TerracottaPhase())
        loadModule(TheGreatSpook())
        loadModule(ThunderSparksHighlight())
        loadModule(TiaRelayHelper())
        loadModule(TiaRelayWaypoints())
        loadModule(TimeFeatures())
        loadModule(ToolTooltipTweaks())
        loadModule(TotemOfCorruption())
        loadModule(TpsCounter())
        loadModule(Translator())
        loadModule(TrevorFeatures)
        loadModule(TrevorSolver)
        loadModule(TrevorTracker)
        loadModule(TrophyFishFillet())
        loadModule(TrophyFishManager)
        loadModule(TrophyFishMessages())
        loadModule(TubulatorParkour())
        loadModule(TunnelsMaps())
        loadModule(UniqueGiftCounter)
        loadModule(UniqueGiftingOpportunitiesFeatures)
        loadModule(VampireSlayerFeatures)
        loadModule(VerminHighlighter())
        loadModule(VerminTracker)
        loadModule(ViewRecipeCommand)
        loadModule(VolcanoExplosivityDisplay())
        loadModule(VoltHighlighter())
        loadModule(WarpIsCommand())
        loadModule(WarpTabComplete)
        loadModule(WatchdogHider())
        loadModule(WikiManager)
        loadModule(WildStrawberryDyeNotification())
        loadModule(WrongFungiCutterWarning())

        // test stuff
        loadModule(ButtonOnPause())
        loadModule(CopyNearbyParticlesCommand)
        loadModule(FixGhostEntities)
        loadModule(HighlightMissingRepoItems())
        loadModule(MobDebug())
        loadModule(PacketTest)
        loadModule(ParkourWaypointSaver())
        loadModule(SkyHanniDebugsAndTests())
        loadModule(SkyHanniDebugsAndTests)
        loadModule(TestBingo)
        loadModule(TestCopyBestiaryValues)
        loadModule(TestCopyRngMeterValues)
        loadModule(TestShowSlotNumber())
        loadModule(TrackSoundsCommand)
        loadModule(WorldEdit)

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
