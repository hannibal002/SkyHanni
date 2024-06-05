package at.hannibal2.skyhanni

import at.hannibal2.skyhanni.api.DataWatcherAPI
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.Features
import at.hannibal2.skyhanni.config.SackData
import at.hannibal2.skyhanni.config.commands.Commands
import at.hannibal2.skyhanni.data.ActionBarStatsData
import at.hannibal2.skyhanni.data.BlockData
import at.hannibal2.skyhanni.data.FixedRateTimerManager
import at.hannibal2.skyhanni.data.GardenComposterUpgradesData
import at.hannibal2.skyhanni.data.GuiEditManager
import at.hannibal2.skyhanni.data.HotmData
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.ItemAddManager
import at.hannibal2.skyhanni.data.ItemClickData
import at.hannibal2.skyhanni.data.ItemTipHelper
import at.hannibal2.skyhanni.data.OtherInventoryData
import at.hannibal2.skyhanni.data.OwnInventoryData
import at.hannibal2.skyhanni.data.RenderData
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.data.SkillExperience
import at.hannibal2.skyhanni.data.TitleData
import at.hannibal2.skyhanni.data.TitleManager
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
import at.hannibal2.skyhanni.features.combat.FerocityDisplay
import at.hannibal2.skyhanni.features.combat.HideDamageSplash
import at.hannibal2.skyhanni.features.combat.damageindicator.DamageIndicatorManager
import at.hannibal2.skyhanni.features.combat.mobs.AreaMiniBossFeatures
import at.hannibal2.skyhanni.features.combat.mobs.AshfangMinisNametagHider
import at.hannibal2.skyhanni.features.combat.mobs.MobHighlight
import at.hannibal2.skyhanni.features.combat.mobs.SpawnTimers
import at.hannibal2.skyhanni.features.commands.SendCoordinatedCommand
import at.hannibal2.skyhanni.features.commands.WarpIsCommand
import at.hannibal2.skyhanni.features.cosmetics.ArrowTrail
import at.hannibal2.skyhanni.features.cosmetics.CosmeticFollowingLine
import at.hannibal2.skyhanni.features.dungeon.CroesusChestTracker
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
import at.hannibal2.skyhanni.features.dungeon.DungeonRankTabListColor
import at.hannibal2.skyhanni.features.dungeon.DungeonShadowAssassinNotification
import at.hannibal2.skyhanni.features.dungeon.DungeonTeammateOutlines
import at.hannibal2.skyhanni.features.dungeon.DungeonsRaceGuide
import at.hannibal2.skyhanni.features.dungeon.HighlightDungeonDeathmite
import at.hannibal2.skyhanni.features.dungeon.TerracottaPhase
import at.hannibal2.skyhanni.features.event.diana.BurrowWarpHelper
import at.hannibal2.skyhanni.features.event.diana.DianaFixChat
import at.hannibal2.skyhanni.features.event.diana.GriffinPetWarning
import at.hannibal2.skyhanni.features.event.diana.HighlightInquisitors
import at.hannibal2.skyhanni.features.event.diana.SoopyGuessBurrow
import at.hannibal2.skyhanni.features.event.jerry.HighlightJerries
import at.hannibal2.skyhanni.features.event.lobby.waypoints.christmas.PresentWaypoints
import at.hannibal2.skyhanni.features.event.lobby.waypoints.easter.EasterEggWaypoints
import at.hannibal2.skyhanni.features.event.lobby.waypoints.halloween.BasketWaypoints
import at.hannibal2.skyhanni.features.event.spook.TheGreatSpook
import at.hannibal2.skyhanni.features.event.winter.JyrreTimer
import at.hannibal2.skyhanni.features.event.winter.NewYearCakeReminder
import at.hannibal2.skyhanni.features.fame.AccountUpgradeReminder
import at.hannibal2.skyhanni.features.fame.CityProjectFeatures
import at.hannibal2.skyhanni.features.fishing.ChumBucketHider
import at.hannibal2.skyhanni.features.fishing.FishingBaitWarnings
import at.hannibal2.skyhanni.features.fishing.FishingHookDisplay
import at.hannibal2.skyhanni.features.fishing.FishingTimer
import at.hannibal2.skyhanni.features.fishing.SeaCreatureFeatures
import at.hannibal2.skyhanni.features.fishing.SeaCreatureManager
import at.hannibal2.skyhanni.features.fishing.SeaCreatureMessageShortener
import at.hannibal2.skyhanni.features.fishing.SharkFishCounter
import at.hannibal2.skyhanni.features.fishing.ShowFishingItemName
import at.hannibal2.skyhanni.features.fishing.ThunderSparksHighlight
import at.hannibal2.skyhanni.features.fishing.TotemOfCorruption
import at.hannibal2.skyhanni.features.fishing.trophy.GeyserFishing
import at.hannibal2.skyhanni.features.fishing.trophy.OdgerWaypoint
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyFishFillet
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyFishMessages
import at.hannibal2.skyhanni.features.garden.AnitaMedalProfit
import at.hannibal2.skyhanni.features.garden.AtmosphericFilterDisplay
import at.hannibal2.skyhanni.features.garden.GardenCropMilestoneFix
import at.hannibal2.skyhanni.features.garden.GardenLevelDisplay
import at.hannibal2.skyhanni.features.garden.GardenOptimalSpeed
import at.hannibal2.skyhanni.features.garden.GardenWarpCommands
import at.hannibal2.skyhanni.features.garden.GardenYawAndPitch
import at.hannibal2.skyhanni.features.garden.ToolTooltipTweaks
import at.hannibal2.skyhanni.features.garden.composter.ComposterDisplay
import at.hannibal2.skyhanni.features.garden.composter.ComposterInventoryNumbers
import at.hannibal2.skyhanni.features.garden.composter.GardenComposterInventoryFeatures
import at.hannibal2.skyhanni.features.garden.contest.JacobContestFFNeededDisplay
import at.hannibal2.skyhanni.features.garden.contest.JacobContestStatsSummary
import at.hannibal2.skyhanni.features.garden.contest.JacobContestTimeNeeded
import at.hannibal2.skyhanni.features.garden.contest.JacobFarmingContestsInventory
import at.hannibal2.skyhanni.features.garden.farming.CropSpeedMeter
import at.hannibal2.skyhanni.features.garden.farming.FarmingWeightDisplay
import at.hannibal2.skyhanni.features.garden.farming.GardenBestCropTime
import at.hannibal2.skyhanni.features.garden.farming.GardenBurrowingSporesNotifier
import at.hannibal2.skyhanni.features.garden.farming.WildStrawberryDyeNotification
import at.hannibal2.skyhanni.features.garden.farming.WrongFungiCutterWarning
import at.hannibal2.skyhanni.features.garden.inventory.AnitaExtraFarmingFortune
import at.hannibal2.skyhanni.features.garden.inventory.GardenCropMilestoneInventory
import at.hannibal2.skyhanni.features.garden.inventory.GardenInventoryNumbers
import at.hannibal2.skyhanni.features.garden.inventory.GardenInventoryTooltipOverflow
import at.hannibal2.skyhanni.features.garden.inventory.LogBookStats
import at.hannibal2.skyhanni.features.garden.inventory.SkyMartCopperPrice
import at.hannibal2.skyhanni.features.garden.inventory.plots.GardenNextPlotPrice
import at.hannibal2.skyhanni.features.garden.inventory.plots.GardenPlotMenuHighlighting
import at.hannibal2.skyhanni.features.garden.pests.PestParticleLine
import at.hannibal2.skyhanni.features.garden.pests.PestParticleWaypoint
import at.hannibal2.skyhanni.features.garden.pests.PestSpawn
import at.hannibal2.skyhanni.features.garden.pests.SprayDisplay
import at.hannibal2.skyhanni.features.garden.pests.SprayFeatures
import at.hannibal2.skyhanni.features.garden.pests.StereoHarmonyDisplay
import at.hannibal2.skyhanni.features.garden.visitor.GardenVisitorSupercraft
import at.hannibal2.skyhanni.features.garden.visitor.GardenVisitorTimer
import at.hannibal2.skyhanni.features.garden.visitor.HighlightVisitorsOutsideOfGarden
import at.hannibal2.skyhanni.features.garden.visitor.VisitorListener
import at.hannibal2.skyhanni.features.garden.visitor.VisitorRewardWarning
import at.hannibal2.skyhanni.features.gui.MovableHotBar
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard
import at.hannibal2.skyhanni.features.gui.quiver.QuiverDisplay
import at.hannibal2.skyhanni.features.gui.quiver.QuiverWarning
import at.hannibal2.skyhanni.features.inventory.ChestValue
import at.hannibal2.skyhanni.features.inventory.DojoRankDisplay
import at.hannibal2.skyhanni.features.inventory.HideNotClickableItems
import at.hannibal2.skyhanni.features.inventory.ItemStars
import at.hannibal2.skyhanni.features.inventory.MaxPurseItems
import at.hannibal2.skyhanni.features.inventory.PowerStoneGuideFeatures
import at.hannibal2.skyhanni.features.inventory.QuickCraftFeatures
import at.hannibal2.skyhanni.features.inventory.RngMeterInventory
import at.hannibal2.skyhanni.features.inventory.ShiftClickBrewing
import at.hannibal2.skyhanni.features.inventory.ShiftClickEquipment
import at.hannibal2.skyhanni.features.inventory.SkyblockGuideHighlightFeature
import at.hannibal2.skyhanni.features.inventory.StatsTuning
import at.hannibal2.skyhanni.features.inventory.SuperpairsClicksAlert
import at.hannibal2.skyhanni.features.inventory.auctionhouse.AuctionHouseCopyUnderbidPrice
import at.hannibal2.skyhanni.features.inventory.auctionhouse.AuctionHouseOpenPriceWebsite
import at.hannibal2.skyhanni.features.inventory.bazaar.BazaarApi
import at.hannibal2.skyhanni.features.inventory.bazaar.BazaarBestSellMethod
import at.hannibal2.skyhanni.features.inventory.bazaar.BazaarCancelledBuyOrderClipboard
import at.hannibal2.skyhanni.features.inventory.bazaar.BazaarOpenPriceWebsite
import at.hannibal2.skyhanni.features.inventory.bazaar.BazaarOrderHelper
import at.hannibal2.skyhanni.features.inventory.bazaar.CraftMaterialsFromBazaar
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.ChocolateFactoryShortcut
import at.hannibal2.skyhanni.features.inventory.tiarelay.TiaRelayHelper
import at.hannibal2.skyhanni.features.inventory.tiarelay.TiaRelayWaypoints
import at.hannibal2.skyhanni.features.itemabilities.ChickenHeadTimer
import at.hannibal2.skyhanni.features.itemabilities.FireVeilWandParticles
import at.hannibal2.skyhanni.features.itemabilities.abilitycooldown.ItemAbilityCooldown
import at.hannibal2.skyhanni.features.mining.ColdOverlay
import at.hannibal2.skyhanni.features.mining.DeepCavernsGuide
import at.hannibal2.skyhanni.features.mining.GoldenGoblinHighlight
import at.hannibal2.skyhanni.features.mining.HighlightMiningCommissionMobs
import at.hannibal2.skyhanni.features.mining.HotmFeatures
import at.hannibal2.skyhanni.features.mining.KingTalismanHelper
import at.hannibal2.skyhanni.features.mining.TunnelsMaps
import at.hannibal2.skyhanni.features.mining.crystalhollows.CrystalHollowsNamesInCore
import at.hannibal2.skyhanni.features.mining.crystalhollows.CrystalHollowsWalls
import at.hannibal2.skyhanni.features.mining.eventtracker.MiningEventTracker
import at.hannibal2.skyhanni.features.mining.fossilexcavator.ExcavatorProfitTracker
import at.hannibal2.skyhanni.features.mining.fossilexcavator.GlacitePowderFeatures
import at.hannibal2.skyhanni.features.mining.fossilexcavator.ProfitPerExcavation
import at.hannibal2.skyhanni.features.mining.mineshaft.CorpseAPI
import at.hannibal2.skyhanni.features.mining.mineshaft.MineshaftCorpseProfitPer
import at.hannibal2.skyhanni.features.minion.InfernoMinionFeatures
import at.hannibal2.skyhanni.features.minion.MinionCollectLogic
import at.hannibal2.skyhanni.features.minion.MinionXp
import at.hannibal2.skyhanni.features.misc.AuctionHousePriceComparison
import at.hannibal2.skyhanni.features.misc.BetterSignEditing
import at.hannibal2.skyhanni.features.misc.BetterWikiFromMenus
import at.hannibal2.skyhanni.features.misc.BrewingStandOverlay
import at.hannibal2.skyhanni.features.misc.ButtonOnPause
import at.hannibal2.skyhanni.features.misc.CollectionTracker
import at.hannibal2.skyhanni.features.misc.CustomTextBox
import at.hannibal2.skyhanni.features.misc.ExpOrbsOnGroundHider
import at.hannibal2.skyhanni.features.misc.FixNEUHeavyPearls
import at.hannibal2.skyhanni.features.misc.HideArmor
import at.hannibal2.skyhanni.features.misc.HideFarEntities
import at.hannibal2.skyhanni.features.misc.InGameDateDisplay
import at.hannibal2.skyhanni.features.misc.JoinCrystalHollows
import at.hannibal2.skyhanni.features.misc.LesserOrbHider
import at.hannibal2.skyhanni.features.misc.MarkedPlayerManager
import at.hannibal2.skyhanni.features.misc.MovementSpeedDisplay
import at.hannibal2.skyhanni.features.misc.NonGodPotEffectDisplay
import at.hannibal2.skyhanni.features.misc.ParticleHider
import at.hannibal2.skyhanni.features.misc.PartyMemberOutlines
import at.hannibal2.skyhanni.features.misc.PatcherSendCoordinates
import at.hannibal2.skyhanni.features.misc.PocketSackInASackDisplay
import at.hannibal2.skyhanni.features.misc.PrivateIslandNoPickaxeAbility
import at.hannibal2.skyhanni.features.misc.ReplaceRomanNumerals
import at.hannibal2.skyhanni.features.misc.RestorePieceOfWizardPortalLore
import at.hannibal2.skyhanni.features.misc.SkyBlockKickDuration
import at.hannibal2.skyhanni.features.misc.TabWidgetSettings
import at.hannibal2.skyhanni.features.misc.TimeFeatures
import at.hannibal2.skyhanni.features.misc.TpsCounter
import at.hannibal2.skyhanni.features.misc.items.EstimatedWardrobePrice
import at.hannibal2.skyhanni.features.misc.items.GlowingDroppedItems
import at.hannibal2.skyhanni.features.misc.pets.CurrentPetDisplay
import at.hannibal2.skyhanni.features.misc.pets.PetCandyUsedDisplay
import at.hannibal2.skyhanni.features.misc.pets.PetExpTooltip
import at.hannibal2.skyhanni.features.misc.pets.PetItemDisplay
import at.hannibal2.skyhanni.features.misc.teleportpad.TeleportPadCompactName
import at.hannibal2.skyhanni.features.misc.teleportpad.TeleportPadInventoryNumber
import at.hannibal2.skyhanni.features.nether.MatriarchHelper
import at.hannibal2.skyhanni.features.nether.PabloHelper
import at.hannibal2.skyhanni.features.nether.SulphurSkitterBox
import at.hannibal2.skyhanni.features.nether.VolcanoExplosivityDisplay
import at.hannibal2.skyhanni.features.nether.ashfang.AshfangBlazes
import at.hannibal2.skyhanni.features.nether.ashfang.AshfangBlazingSouls
import at.hannibal2.skyhanni.features.nether.ashfang.AshfangGravityOrbs
import at.hannibal2.skyhanni.features.nether.ashfang.AshfangHideDamageIndicator
import at.hannibal2.skyhanni.features.nether.ashfang.AshfangHideParticles
import at.hannibal2.skyhanni.features.nether.ashfang.AshfangNextResetCooldown
import at.hannibal2.skyhanni.features.nether.reputationhelper.CrimsonIsleReputationHelper
import at.hannibal2.skyhanni.features.rift.area.colosseum.BlobbercystsHighlight
import at.hannibal2.skyhanni.features.rift.area.dreadfarm.RiftAgaricusCap
import at.hannibal2.skyhanni.features.rift.area.dreadfarm.RiftWiltedBerberisHelper
import at.hannibal2.skyhanni.features.rift.area.dreadfarm.VoltHighlighter
import at.hannibal2.skyhanni.features.rift.area.livingcave.LivingCaveDefenseBlocks
import at.hannibal2.skyhanni.features.rift.area.livingcave.LivingCaveLivingMetalHelper
import at.hannibal2.skyhanni.features.rift.area.livingcave.LivingMetalSuitProgress
import at.hannibal2.skyhanni.features.rift.area.mirrorverse.RiftLavaMazeParkour
import at.hannibal2.skyhanni.features.rift.area.mirrorverse.RiftUpsideDownParkour
import at.hannibal2.skyhanni.features.rift.area.mirrorverse.TubulatorParkour
import at.hannibal2.skyhanni.features.rift.area.westvillage.VerminHighlighter
import at.hannibal2.skyhanni.features.rift.area.westvillage.kloon.KloonHacking
import at.hannibal2.skyhanni.features.rift.area.wyldwoods.RiftLarva
import at.hannibal2.skyhanni.features.rift.area.wyldwoods.RiftOdonata
import at.hannibal2.skyhanni.features.rift.area.wyldwoods.ShyCruxWarnings
import at.hannibal2.skyhanni.features.rift.everywhere.HighlightRiftGuide
import at.hannibal2.skyhanni.features.rift.everywhere.RiftHorsezookaHider
import at.hannibal2.skyhanni.features.rift.everywhere.RiftTimer
import at.hannibal2.skyhanni.features.rift.everywhere.motes.RiftMotesOrb
import at.hannibal2.skyhanni.features.rift.everywhere.motes.ShowMotesNpcSellPrice
import at.hannibal2.skyhanni.features.skillprogress.SkillTooltip
import at.hannibal2.skyhanni.features.slayer.HideMobNames
import at.hannibal2.skyhanni.features.slayer.SlayerBossSpawnSoon
import at.hannibal2.skyhanni.features.slayer.SlayerItemsOnGround
import at.hannibal2.skyhanni.features.slayer.SlayerMiniBossFeatures
import at.hannibal2.skyhanni.features.slayer.SlayerQuestWarning
import at.hannibal2.skyhanni.features.slayer.SlayerRngMeterDisplay
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
import at.hannibal2.skyhanni.skyhannimodule.LoadedModules
import at.hannibal2.skyhanni.test.HighlightMissingRepoItems
import at.hannibal2.skyhanni.test.ParkourWaypointSaver
import at.hannibal2.skyhanni.test.ShowItemUuid
import at.hannibal2.skyhanni.test.SkyHanniDebugsAndTests
import at.hannibal2.skyhanni.test.TestShowSlotNumber
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.test.hotswap.HotswapSupport
import at.hannibal2.skyhanni.utils.MinecraftConsoleFilter.Companion.initLogging
import at.hannibal2.skyhanni.utils.NEUVersionCheck.checkIfNeuIsLoaded
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
    version = "0.26.Beta.7",
)
class SkyHanniMod {

    @Mod.EventHandler
    fun preInit(event: FMLPreInitializationEvent?) {
        checkIfNeuIsLoaded()

        HotswapSupport.load()

        loadModule(this)
        LoadedModules.modules.forEach { loadModule(it) }

        // data
        loadModule(PlayerChatManager())
        loadModule(PlayerNameFormatter())
        loadModule(HypixelData())
        loadModule(ScoreboardData())
        loadModule(SeaCreatureFeatures())
        loadModule(SeaCreatureManager())
        loadModule(MobData())
        loadModule(MobDetection())
        loadModule(ItemClickData())
//        loadModule(Year300RaffleEvent)
        loadModule(TitleManager())
        loadModule(ItemTipHelper())
        loadModule(RenderLivingEntityHelper())
        loadModule(SkillExperience())
        loadModule(RenderData())
        loadModule(MovableHotBar())
        loadModule(VisitorListener())
        loadModule(VisitorRewardWarning())
        loadModule(OwnInventoryData())
        loadModule(HighlightVisitorsOutsideOfGarden())
        loadModule(GuiEditManager())
        loadModule(GardenComposterUpgradesData())
        loadModule(ActionBarStatsData)
        loadModule(GardenCropMilestoneInventory())
        loadModule(GardenWarpCommands())
        loadModule(TitleData())
        loadModule(BlockData())
        loadModule(ItemAddManager())
        loadModule(BingoCardReader())
        loadModule(DeepCavernsGuide())
        loadModule(DungeonsRaceGuide())
        loadModule(GardenBestCropTime())
        loadModule(FixedRateTimerManager())
        loadModule(HotmData)

        // APIs
        loadModule(BazaarApi())
        loadModule(DataWatcherAPI())

        // features
        loadModule(BazaarOrderHelper())
        loadModule(ChatFilter())
        loadModule(PlayerChatModifier())
        loadModule(DungeonChatFilter())
        loadModule(HideNotClickableItems())
        loadModule(CurrentPetDisplay())
        loadModule(HideFarEntities())
        loadModule(ExpOrbsOnGroundHider())
        loadModule(BetterWikiFromMenus())
        loadModule(DamageIndicatorManager())
        loadModule(ItemAbilityCooldown())
        loadModule(DungeonHighlightClickedBlocks())
        loadModule(DungeonDeathCounter())
        loadModule(DungeonCleanEnd())
        loadModule(TunnelsMaps())
        loadModule(DungeonBossMessages())
        loadModule(DungeonBossHideDamageSplash())
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
        loadModule(AshfangNextResetCooldown())
        loadModule(CrystalHollowsWalls())
        loadModule(SummoningSoulsName())
        loadModule(AshfangGravityOrbs())
        loadModule(AshfangBlazingSouls())
        loadModule(AshfangBlazes())
        loadModule(AshfangHideParticles())
        loadModule(AshfangHideDamageIndicator())
        loadModule(ItemStars())
        loadModule(TimeFeatures())
        loadModule(RngMeterInventory())
        loadModule(SendCoordinatedCommand())
        loadModule(WarpIsCommand())
        loadModule(SummoningMobManager())
        loadModule(SkyblockXPInChat())
        loadModule(AreaMiniBossFeatures())
        loadModule(MobHighlight())
        loadModule(ChocolateFactoryShortcut())
        loadModule(SpawnTimers())
        loadModule(MarkedPlayerManager)
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
        loadModule(HotmFeatures())
        loadModule(DianaFixChat())
        loadModule(HighlightJerries())
        loadModule(TheGreatSpook())
        loadModule(BurrowWarpHelper())
        loadModule(CollectionTracker())
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
        loadModule(ReplaceRomanNumerals())
        loadModule(GardenPlotMenuHighlighting())
        loadModule(SkyMartCopperPrice())
        loadModule(GardenVisitorSupercraft())
        loadModule(GardenInventoryNumbers())
        loadModule(GardenVisitorTimer())
        loadModule(MinionXp())
        loadModule(GardenNextPlotPrice())
        loadModule(ChickenHeadTimer())
        loadModule(ExcavatorProfitTracker())
        loadModule(ProfitPerExcavation())
        loadModule(GlacitePowderFeatures())
        loadModule(MineshaftCorpseProfitPer())
        loadModule(CorpseAPI())
        loadModule(GardenOptimalSpeed())
        loadModule(GardenLevelDisplay())
        loadModule(FarmingWeightDisplay())
        loadModule(PrivateIslandNoPickaxeAbility())
        loadModule(JacobFarmingContestsInventory())
        loadModule(WrongFungiCutterWarning())
        loadModule(JoinCrystalHollows())
        loadModule(CrystalHollowsNamesInCore())
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
        loadModule(EstimatedWardrobePrice())
        loadModule(ComposterInventoryNumbers())
        loadModule(ToolTooltipTweaks())
        loadModule(CropSpeedMeter())
        loadModule(AshfangMinisNametagHider())
        loadModule(TeleportPadInventoryNumber())
        loadModule(GardenCropMilestoneFix())
        loadModule(GardenBurrowingSporesNotifier())
        loadModule(WildStrawberryDyeNotification())
        loadModule(JacobContestFFNeededDisplay())
        loadModule(JacobContestTimeNeeded())
        loadModule(JacobContestStatsSummary())
        loadModule(GardenYawAndPitch())
        loadModule(MovementSpeedDisplay())
        loadModule(ChumBucketHider())
        loadModule(BingoCardTips())
        loadModule(PetCandyUsedDisplay())
        loadModule(CityProjectFeatures())
        loadModule(LogBookStats())
        loadModule(PocketSackInASackDisplay())
        loadModule(ShowFishingItemName())
        loadModule(SlayerItemsOnGround())
        loadModule(RestorePieceOfWizardPortalLore())
        loadModule(ArachneChatMessageHider())
        loadModule(ShowItemUuid())
        loadModule(SlayerRngMeterDisplay())
        loadModule(RiftTimer())
        loadModule(HighlightRiftGuide())
        loadModule(ShyCruxWarnings())
        loadModule(RiftLarva())
        loadModule(VoltHighlighter())
        loadModule(RiftOdonata())
        loadModule(RiftAgaricusCap())
        loadModule(KloonHacking())
        loadModule(TubulatorParkour())
        loadModule(CustomTextBox())
        loadModule(RiftUpsideDownParkour())
        loadModule(RiftLavaMazeParkour())
        loadModule(HighlightMiningCommissionMobs())
        loadModule(ShowMotesNpcSellPrice())
        loadModule(LivingMetalSuitProgress())
        loadModule(BlobbercystsHighlight())
        loadModule(LivingCaveDefenseBlocks())
        loadModule(LivingCaveLivingMetalHelper())
        loadModule(RiftMotesOrb())
        loadModule(ChestValue())
        loadModule(SlayerBossSpawnSoon())
        loadModule(RiftWiltedBerberisHelper())
        loadModule(RiftHorsezookaHider())
        loadModule(GriffinPetWarning())
        loadModule(KingTalismanHelper())
        loadModule(CompactBestiaryChatMessage())
        loadModule(WatchdogHider())
        loadModule(AuctionHousePriceComparison())
        loadModule(AccountUpgradeReminder())
        loadModule(PetExpTooltip())
        loadModule(Translator())
        loadModule(CosmeticFollowingLine())
        loadModule(SuperpairsClicksAlert())
        loadModule(GlowingDroppedItems())
        loadModule(DungeonTeammateOutlines())
        loadModule(DungeonRankTabListColor())
        loadModule(TerracottaPhase())
        loadModule(VolcanoExplosivityDisplay())
        loadModule(FixNEUHeavyPearls())
        loadModule(QuickCraftFeatures())
        loadModule(SkyBlockKickDuration())
        loadModule(PartyMemberOutlines())
        loadModule(ArrowTrail())
        loadModule(ShiftClickEquipment())
        loadModule(DungeonFinderFeatures())
        loadModule(GoldenGoblinHighlight())
        loadModule(TabWidgetSettings())
        loadModule(PabloHelper())
        loadModule(FishingBaitWarnings())
        loadModule(CustomScoreboard())
        loadModule(PestSpawn())
        loadModule(PestParticleWaypoint())
        loadModule(StereoHarmonyDisplay())
        loadModule(PestParticleLine())
        loadModule(SprayFeatures())
        loadModule(DojoRankDisplay())
        loadModule(SprayDisplay())
        loadModule(HighlightPlaceableNpcs())
        loadModule(PresentWaypoints())
        loadModule(MiningEventTracker())
        loadModule(JyrreTimer())
        loadModule(TotemOfCorruption())
        loadModule(NewYearCakeReminder())
        loadModule(SulphurSkitterBox())
        loadModule(HighlightInquisitors())
        loadModule(VerminHighlighter())
        loadModule(GardenInventoryTooltipOverflow())
        loadModule(SkillTooltip())
        loadModule(MaxPurseItems())
        loadModule(InfernoMinionFeatures())
        loadModule(RareDropMessages())
        loadModule(CraftMaterialsFromBazaar())
        loadModule(DungeonShadowAssassinNotification())
        loadModule(ColdOverlay())
        loadModule(QuiverDisplay())
        loadModule(QuiverWarning())

        // test stuff
        loadModule(SkyHanniDebugsAndTests())
        loadModule(ButtonOnPause())
        loadModule(HighlightMissingRepoItems())
        loadModule(ParkourWaypointSaver())
        loadModule(TestShowSlotNumber())
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
        loadedClasses.clear()
    }

    private val loadedClasses = mutableSetOf<String>()

    fun loadModule(obj: Any) {
        if (!loadedClasses.add(obj.javaClass.name)) throw IllegalStateException("Module ${obj.javaClass.name} is already loaded")
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
