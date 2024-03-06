package at.hannibal2.skyhanni

import at.hannibal2.skyhanni.api.CollectionAPI
import at.hannibal2.skyhanni.api.DataWatcherAPI
import at.hannibal2.skyhanni.api.GetFromSackAPI
import at.hannibal2.skyhanni.api.SkillAPI
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.Features
import at.hannibal2.skyhanni.config.SackData
import at.hannibal2.skyhanni.config.commands.Commands.init
import at.hannibal2.skyhanni.data.*
import at.hannibal2.skyhanni.data.jsonobjects.local.FriendsJson
import at.hannibal2.skyhanni.data.jsonobjects.local.JacobContestsJson
import at.hannibal2.skyhanni.data.jsonobjects.local.KnownFeaturesJson
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
import at.hannibal2.skyhanni.features.chat.*
import at.hannibal2.skyhanni.features.chat.playerchat.PlayerChatFilter
import at.hannibal2.skyhanni.features.chat.playerchat.PlayerChatModifier
import at.hannibal2.skyhanni.features.combat.BestiaryData
import at.hannibal2.skyhanni.features.combat.HideDamageSplash
import at.hannibal2.skyhanni.features.combat.damageindicator.DamageIndicatorManager
import at.hannibal2.skyhanni.features.combat.endernodetracker.EnderNodeTracker
import at.hannibal2.skyhanni.features.combat.ghostcounter.GhostCounter
import at.hannibal2.skyhanni.features.combat.mobs.AreaMiniBossFeatures
import at.hannibal2.skyhanni.features.combat.mobs.AshfangMinisNametagHider
import at.hannibal2.skyhanni.features.combat.mobs.MobHighlight
import at.hannibal2.skyhanni.features.combat.mobs.SpawnTimers
import at.hannibal2.skyhanni.features.commands.*
import at.hannibal2.skyhanni.features.commands.tabcomplete.GetFromSacksTabComplete
import at.hannibal2.skyhanni.features.commands.tabcomplete.PlayerTabComplete
import at.hannibal2.skyhanni.features.commands.tabcomplete.WarpTabComplete
import at.hannibal2.skyhanni.features.cosmetics.ArrowTrail
import at.hannibal2.skyhanni.features.cosmetics.CosmeticFollowingLine
import at.hannibal2.skyhanni.features.dungeon.*
import at.hannibal2.skyhanni.features.event.UniqueGiftingOpportunitiesFeatures
import at.hannibal2.skyhanni.features.event.diana.*
import at.hannibal2.skyhanni.features.event.jerry.HighlightJerries
import at.hannibal2.skyhanni.features.event.jerry.frozentreasure.FrozenTreasureTracker
import at.hannibal2.skyhanni.features.event.lobby.waypoints.christmas.PresentWaypoints
import at.hannibal2.skyhanni.features.event.lobby.waypoints.halloween.BasketWaypoints
import at.hannibal2.skyhanni.features.event.spook.TheGreatSpook
import at.hannibal2.skyhanni.features.event.winter.JyrreTimer
import at.hannibal2.skyhanni.features.event.winter.NewYearCakeReminder
import at.hannibal2.skyhanni.features.event.winter.UniqueGiftCounter
import at.hannibal2.skyhanni.features.fame.AccountUpgradeReminder
import at.hannibal2.skyhanni.features.fame.CityProjectFeatures
import at.hannibal2.skyhanni.features.fishing.*
import at.hannibal2.skyhanni.features.fishing.tracker.FishingProfitTracker
import at.hannibal2.skyhanni.features.fishing.tracker.SeaCreatureTracker
import at.hannibal2.skyhanni.features.fishing.trophy.*
import at.hannibal2.skyhanni.features.garden.*
import at.hannibal2.skyhanni.features.garden.composter.ComposterDisplay
import at.hannibal2.skyhanni.features.garden.composter.ComposterInventoryNumbers
import at.hannibal2.skyhanni.features.garden.composter.ComposterOverlay
import at.hannibal2.skyhanni.features.garden.composter.GardenComposterInventoryFeatures
import at.hannibal2.skyhanni.features.garden.contest.*
import at.hannibal2.skyhanni.features.garden.farming.*
import at.hannibal2.skyhanni.features.garden.fortuneguide.CaptureFarmingGear
import at.hannibal2.skyhanni.features.garden.inventory.*
import at.hannibal2.skyhanni.features.garden.pests.*
import at.hannibal2.skyhanni.features.garden.visitor.*
import at.hannibal2.skyhanni.features.inventory.*
import at.hannibal2.skyhanni.features.inventory.bazaar.*
import at.hannibal2.skyhanni.features.inventory.tiarelay.TiaRelayHelper
import at.hannibal2.skyhanni.features.inventory.tiarelay.TiaRelayWaypoints
import at.hannibal2.skyhanni.features.itemabilities.ChickenHeadTimer
import at.hannibal2.skyhanni.features.itemabilities.FireVeilWandParticles
import at.hannibal2.skyhanni.features.itemabilities.abilitycooldown.ItemAbilityCooldown
import at.hannibal2.skyhanni.features.mining.DeepCavernsParkour
import at.hannibal2.skyhanni.features.mining.HighlightMiningCommissionMobs
import at.hannibal2.skyhanni.features.mining.KingTalismanHelper
import at.hannibal2.skyhanni.features.mining.crystalhollows.CrystalHollowsNamesInCore
import at.hannibal2.skyhanni.features.mining.eventtracker.MiningEventTracker
import at.hannibal2.skyhanni.features.mining.powdertracker.PowderTracker
import at.hannibal2.skyhanni.features.minion.InfernoMinionFeatures
import at.hannibal2.skyhanni.features.minion.MinionCollectLogic
import at.hannibal2.skyhanni.features.minion.MinionFeatures
import at.hannibal2.skyhanni.features.minion.MinionXp
import at.hannibal2.skyhanni.features.misc.*
import at.hannibal2.skyhanni.features.misc.compacttablist.AdvancedPlayerList
import at.hannibal2.skyhanni.features.misc.compacttablist.TabListReader
import at.hannibal2.skyhanni.features.misc.compacttablist.TabListRenderer
import at.hannibal2.skyhanni.features.misc.discordrpc.DiscordRPCManager
import at.hannibal2.skyhanni.features.misc.items.AuctionHouseCopyUnderbidPrice
import at.hannibal2.skyhanni.features.misc.items.EstimatedItemValue
import at.hannibal2.skyhanni.features.misc.items.EstimatedWardrobePrice
import at.hannibal2.skyhanni.features.misc.items.GlowingDroppedItems
import at.hannibal2.skyhanni.features.misc.massconfiguration.DefaultConfigFeatures
import at.hannibal2.skyhanni.features.misc.teleportpad.TeleportPadCompactName
import at.hannibal2.skyhanni.features.misc.teleportpad.TeleportPadInventoryNumber
import at.hannibal2.skyhanni.features.misc.trevor.TrevorFeatures
import at.hannibal2.skyhanni.features.misc.trevor.TrevorSolver
import at.hannibal2.skyhanni.features.misc.trevor.TrevorTracker
import at.hannibal2.skyhanni.features.misc.update.UpdateManager
import at.hannibal2.skyhanni.features.misc.visualwords.ModifyVisualWords
import at.hannibal2.skyhanni.features.nether.PabloHelper
import at.hannibal2.skyhanni.features.nether.SulphurSkitterBox
import at.hannibal2.skyhanni.features.nether.VolcanoExplosivityDisplay
import at.hannibal2.skyhanni.features.nether.ashfang.*
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
import at.hannibal2.skyhanni.features.rift.area.westvillage.VerminTracker
import at.hannibal2.skyhanni.features.rift.area.westvillage.kloon.KloonHacking
import at.hannibal2.skyhanni.features.rift.area.wyldwoods.RiftLarva
import at.hannibal2.skyhanni.features.rift.area.wyldwoods.RiftOdonata
import at.hannibal2.skyhanni.features.rift.area.wyldwoods.ShyCruxWarnings
import at.hannibal2.skyhanni.features.rift.everywhere.*
import at.hannibal2.skyhanni.features.rift.everywhere.motes.RiftMotesOrb
import at.hannibal2.skyhanni.features.rift.everywhere.motes.ShowMotesNpcSellPrice
import at.hannibal2.skyhanni.features.skillprogress.SkillProgress
import at.hannibal2.skyhanni.features.skillprogress.SkillTooltip
import at.hannibal2.skyhanni.features.slayer.*
import at.hannibal2.skyhanni.features.slayer.blaze.BlazeSlayerClearView
import at.hannibal2.skyhanni.features.slayer.blaze.BlazeSlayerDaggerHelper
import at.hannibal2.skyhanni.features.slayer.blaze.BlazeSlayerFirePitsWarning
import at.hannibal2.skyhanni.features.slayer.blaze.HellionShieldHelper
import at.hannibal2.skyhanni.features.slayer.enderman.EndermanSlayerFeatures
import at.hannibal2.skyhanni.features.slayer.enderman.EndermanSlayerHideParticles
import at.hannibal2.skyhanni.features.stranded.HighlightPlaceableNpcs
import at.hannibal2.skyhanni.features.summonings.SummoningMobManager
import at.hannibal2.skyhanni.features.summonings.SummoningSoulsName
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.test.*
import at.hannibal2.skyhanni.test.command.CopyNearbyParticlesCommand
import at.hannibal2.skyhanni.test.hotswap.HotswapSupport
import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.MinecraftConsoleFilter.Companion.initLogging
import at.hannibal2.skyhanni.utils.NEUVersionCheck.checkIfNeuIsLoaded
import at.hannibal2.skyhanni.utils.repopatterns.RepoPatternManager
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
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
    version = "0.24.Beta.4",
)
class SkyHanniMod {

    @Mod.EventHandler
    fun preInit(event: FMLPreInitializationEvent?) {
        checkIfNeuIsLoaded()

        HotswapSupport.load()

        // data
        loadModule(this)
        loadModule(ChatManager)
        loadModule(HypixelData())
        loadModule(LocationFixData)
        loadModule(DungeonAPI())
        loadModule(ScoreboardData())
        loadModule(SeaCreatureFeatures())
        loadModule(SeaCreatureManager())
        loadModule(ItemRenderBackground())
        loadModule(EntityData())
        loadModule(EntityMovementData())
        loadModule(TestExportTools)
        loadModule(ItemClickData())
//        loadModule(Year300RaffleEvent)
        loadModule(MinecraftData)
        loadModule(TitleManager())
        loadModule(ItemTipHelper())
        loadModule(RenderLivingEntityHelper())
        loadModule(SkillExperience())
        loadModule(OtherInventoryData)
        loadModule(TabListData())
        loadModule(RenderData())
        loadModule(GardenCropMilestones)
        loadModule(GardenCropMilestonesCommunityFix)
        loadModule(GardenCropUpgrades())
        loadModule(VisitorListener())
        loadModule(OwnInventoryData())
        loadModule(ToolTipData())
        loadModule(HighlightVisitorsOutsideOfGarden())
        loadModule(GuiEditManager())
        loadModule(GetFromSackAPI)
        loadModule(UpdateManager)
        loadModule(CropAccessoryData())
        loadModule(MayorElection())
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
        loadModule(DeepCavernsParkour())
        loadModule(GardenBestCropTime())
        loadModule(ActionBarData)
        loadModule(TrackerManager)
        loadModule(UtilsPatterns)
        loadModule(PetAPI)
        loadModule(BossbarData)
        loadModule(ChatUtils)

        // APIs
        loadModule(BazaarApi())
        loadModule(GardenAPI)
        loadModule(GardenPlotAPI)
        loadModule(DataWatcherAPI())
        loadModule(CollectionAPI)
        loadModule(FarmingContestAPI)
        loadModule(FriendAPI)
        loadModule(PartyAPI)
        loadModule(GuildAPI)
        loadModule(SlayerAPI)
        loadModule(PurseAPI)
        loadModule(RiftAPI)
        loadModule(SackAPI)
        loadModule(BingoAPI)
        loadModule(FishingAPI)
        loadModule(SkillAPI)
        loadModule(IsFishingDetection)
        loadModule(LorenzUtils)
        loadModule(NEUItems)

        // features
        loadModule(BazaarOrderHelper())
        loadModule(AuctionsHighlighter())
        loadModule(ChatFilter())
        loadModule(PlayerChatModifier())
        loadModule(DungeonChatFilter())
        loadModule(HideNotClickableItems())
        loadModule(ItemDisplayOverlayFeatures)
        loadModule(CurrentPetDisplay())
        loadModule(ExpOrbsOnGroundHider())
        loadModule(BetterWikiFromMenus())
        loadModule(DamageIndicatorManager())
        loadModule(ItemAbilityCooldown())
        loadModule(DungeonHighlightClickedBlocks())
        loadModule(DungeonMilestonesDisplay())
        loadModule(DungeonDeathCounter())
        loadModule(DungeonCleanEnd())
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
        loadModule(AnvilCombineHelper())
        loadModule(SeaCreatureMessageShortener())
        loadModule(AshfangFreezeCooldown)
        loadModule(AshfangNextResetCooldown())
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
        loadModule(SummoningMobManager())
        loadModule(SkyblockXPInChat())
        loadModule(AreaMiniBossFeatures())
        loadModule(MobHighlight())
        loadModule(SpawnTimers())
        loadModule(MarkedPlayerManager())
        loadModule(SlayerMiniBossFeatures())
        loadModule(PlayerDeathMessages())
        loadModule(HighlightDungeonDeathmite())
        loadModule(DungeonHideItems())
        loadModule(DungeonCopilot())
        loadModule(EndermanSlayerFeatures())
        loadModule(FireVeilWandParticles())
        loadModule(HideMobNames())
        loadModule(HideDamageSplash())
        loadModule(InGameDateDisplay())
        loadModule(ThunderSparksHighlight())
        loadModule(BlazeSlayerDaggerHelper())
        loadModule(HellionShieldHelper())
        loadModule(BlazeSlayerFirePitsWarning())
        loadModule(BlazeSlayerClearView())
        loadModule(EndermanSlayerHideParticles())
        loadModule(PlayerChatFilter())
        loadModule(HideArmor())
        loadModule(SlayerQuestWarning())
        loadModule(StatsTuning())
        loadModule(NonGodPotEffectDisplay())
        loadModule(SoopyGuessBurrow())
        loadModule(DianaProfitTracker)
        loadModule(MythologicalCreatureTracker)
        loadModule(ShiftClickNPCSell)
        loadModule(HighlightJerries())
        loadModule(TheGreatSpook())
        loadModule(GriffinBurrowHelper)
        loadModule(AllBurrowsList)
        loadModule(GriffinBurrowParticleFinder)
        loadModule(BurrowWarpHelper())
        loadModule(CollectionTracker())
        loadModule(HighlightBonzoMasks())
        loadModule(BazaarCancelledBuyOrderClipboard())
        loadModule(CompactSplashPotionMessage())
        loadModule(CroesusUnopenedChestTracker())
        loadModule(CompactBingoChat())
        loadModule(BrewingStandOverlay())
        loadModule(FishingTimer())
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
        loadModule(BingoCardDisplay())
        loadModule(BingoNextStepHelper())
        loadModule(MinionCraftHelper())
        loadModule(TpsCounter())
        loadModule(ParticleHider())
        loadModule(MiscFeatures())
        loadModule(SkyMartCopperPrice())
        loadModule(GardenVisitorFeatures())
        loadModule(NPCVisitorFix)
        loadModule(GardenInventoryNumbers())
        loadModule(GardenVisitorTimer())
        loadModule(MinionXp())
        loadModule(GardenNextPlotPrice())
        loadModule(GardenCropMilestoneDisplay)
        loadModule(GardenCustomKeybinds)
        loadModule(ChickenHeadTimer())
        loadModule(GardenOptimalSpeed())
        loadModule(GardenDeskInSBMenu())
        loadModule(GardenLevelDisplay())
        loadModule(FarmingWeightDisplay())
        loadModule(DicerRngDropTracker)
        loadModule(PrivateIslandNoPickaxeAbility())
        loadModule(CropMoneyDisplay)
        loadModule(JacobFarmingContestsInventory())
        loadModule(GardenNextJacobContest)
        loadModule(WrongFungiCutterWarning())
        loadModule(ArmorDropTracker)
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
        loadModule(ServerRestartTitle())
        loadModule(CityProjectFeatures())
        loadModule(GardenPlotIcon)
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
        loadModule(RiftBloodEffigies())
        loadModule(RiftWiltedBerberisHelper())
        loadModule(RiftHorsezookaHider())
        loadModule(GriffinPetWarning())
        loadModule(BestiaryData)
        loadModule(KingTalismanHelper())
        loadModule(HarpFeatures)
        loadModule(EnderNodeTracker)
        loadModule(CompactBestiaryChatMessage())
        loadModule(WatchdogHider())
        loadModule(AccountUpgradeReminder())
        loadModule(PetExpTooltip())
        loadModule(Translator())
        loadModule(GardenPlotBorders)
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
        loadModule(PlayerChatSymbols())
        loadModule(FixNEUHeavyPearls())
        loadModule(QuickCraftFeatures())
        loadModule(SkyBlockKickDuration())
        loadModule(LimboTimeTracker())
        loadModule(PartyMemberOutlines())
        loadModule(ArrowTrail())
        loadModule(ShiftClickEquipment())
        loadModule(LockMouseLook)
        loadModule(SensitivityReducer)
        loadModule(DungeonFinderFeatures())
        loadModule(PabloHelper())
        loadModule(FishingBaitWarnings())
        loadModule(RepoPatternManager)
        loadModule(PestSpawn())
        loadModule(PestSpawnTimer)
        loadModule(PestFinder())
        loadModule(SprayFeatures())
        loadModule(DojoRankDisplay())
        loadModule(SprayDisplay())
        loadModule(HighlightPlaceableNpcs())
        loadModule(PresentWaypoints())
        loadModule(MiningEventTracker())
        loadModule(JyrreTimer())
        loadModule(NewYearCakeReminder())
        loadModule(SulphurSkitterBox())
        loadModule(HighlightInquisitors())
        loadModule(VerminTracker)
        loadModule(SkillProgress)
        loadModule(SkillTooltip())
        loadModule(QuiverNotification)
        loadModule(MaxPurseItems())
        loadModule(SuperCraftFeatures())
        loadModule(InfernoMinionFeatures())

        init()

        // test stuff
        loadModule(SkyHanniDebugsAndTests())
        loadModule(FixGhostEntities)
        loadModule(CopyNearbyParticlesCommand)
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
    }
}
