package at.hannibal2.skyhanni

import at.hannibal2.skyhanni.api.CollectionAPI
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.Features
import at.hannibal2.skyhanni.config.SackData
import at.hannibal2.skyhanni.config.commands.Commands.init
import at.hannibal2.skyhanni.data.*
import at.hannibal2.skyhanni.data.repo.RepoManager
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.features.anvil.AnvilCombineHelper
import at.hannibal2.skyhanni.features.bazaar.*
import at.hannibal2.skyhanni.features.bingo.*
import at.hannibal2.skyhanni.features.chat.*
import at.hannibal2.skyhanni.features.chat.playerchat.PlayerChatFilter
import at.hannibal2.skyhanni.features.chat.playerchat.PlayerChatModifier
import at.hannibal2.skyhanni.features.commands.PartyTransferCommand
import at.hannibal2.skyhanni.features.commands.SendCoordinatedCommand
import at.hannibal2.skyhanni.features.commands.WarpIsCommand
import at.hannibal2.skyhanni.features.commands.WikiCommand
import at.hannibal2.skyhanni.features.cosmetics.CosmeticFollowingLine
import at.hannibal2.skyhanni.features.damageindicator.DamageIndicatorManager
import at.hannibal2.skyhanni.features.dungeon.*
import at.hannibal2.skyhanni.features.event.diana.*
import at.hannibal2.skyhanni.features.fame.AccountUpgradeReminder
import at.hannibal2.skyhanni.features.fame.CityProjectFeatures
import at.hannibal2.skyhanni.features.fishing.*
import at.hannibal2.skyhanni.features.fishing.trophy.OdgerWaypoint
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyFishFillet
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyFishManager
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyFishMessages
import at.hannibal2.skyhanni.features.garden.*
import at.hannibal2.skyhanni.features.garden.composter.ComposterDisplay
import at.hannibal2.skyhanni.features.garden.composter.ComposterInventoryNumbers
import at.hannibal2.skyhanni.features.garden.composter.ComposterOverlay
import at.hannibal2.skyhanni.features.garden.composter.GardenComposterInventoryFeatures
import at.hannibal2.skyhanni.features.garden.contest.*
import at.hannibal2.skyhanni.features.garden.farming.*
import at.hannibal2.skyhanni.features.garden.fortuneguide.CaptureFarmingGear
import at.hannibal2.skyhanni.features.garden.inventory.*
import at.hannibal2.skyhanni.features.garden.visitor.GardenVisitorColorNames
import at.hannibal2.skyhanni.features.garden.visitor.GardenVisitorDropStatistics
import at.hannibal2.skyhanni.features.garden.visitor.GardenVisitorFeatures
import at.hannibal2.skyhanni.features.garden.visitor.GardenVisitorTimer
import at.hannibal2.skyhanni.features.inventory.*
import at.hannibal2.skyhanni.features.itemabilities.FireVeilWandParticles
import at.hannibal2.skyhanni.features.itemabilities.abilitycooldown.ItemAbilityCooldown
import at.hannibal2.skyhanni.features.mining.HighlightMiningCommissionMobs
import at.hannibal2.skyhanni.features.mining.KingTalismanHelper
import at.hannibal2.skyhanni.features.mining.crystalhollows.CrystalHollowsNamesInCore
import at.hannibal2.skyhanni.features.minion.MinionCollectLogic
import at.hannibal2.skyhanni.features.minion.MinionFeatures
import at.hannibal2.skyhanni.features.misc.*
import at.hannibal2.skyhanni.features.misc.discordrpc.DiscordRPCManager
import at.hannibal2.skyhanni.features.misc.ghostcounter.GhostCounter
import at.hannibal2.skyhanni.features.misc.items.EstimatedItemValue
import at.hannibal2.skyhanni.features.misc.items.EstimatedWardrobePrice
import at.hannibal2.skyhanni.features.misc.items.GlowingDroppedItems
import at.hannibal2.skyhanni.features.misc.massconfiguration.DefaultConfigFeatures
import at.hannibal2.skyhanni.features.misc.powdertracker.PowderTracker
import at.hannibal2.skyhanni.features.misc.tabcomplete.GetFromSacksTabComplete
import at.hannibal2.skyhanni.features.misc.tabcomplete.PlayerTabComplete
import at.hannibal2.skyhanni.features.misc.tabcomplete.WarpTabComplete
import at.hannibal2.skyhanni.features.misc.teleportpad.TeleportPadCompactName
import at.hannibal2.skyhanni.features.misc.teleportpad.TeleportPadInventoryNumber
import at.hannibal2.skyhanni.features.misc.tiarelay.TiaRelayHelper
import at.hannibal2.skyhanni.features.misc.tiarelay.TiaRelayWaypoints
import at.hannibal2.skyhanni.features.misc.trevor.TrevorFeatures
import at.hannibal2.skyhanni.features.misc.trevor.TrevorSolver
import at.hannibal2.skyhanni.features.misc.trevor.TrevorTracker
import at.hannibal2.skyhanni.features.misc.update.UpdateManager
import at.hannibal2.skyhanni.features.mobs.AreaMiniBossFeatures
import at.hannibal2.skyhanni.features.mobs.AshfangMinisNametagHider
import at.hannibal2.skyhanni.features.mobs.MobHighlight
import at.hannibal2.skyhanni.features.mobs.SpawnTimers
import at.hannibal2.skyhanni.features.nether.ashfang.*
import at.hannibal2.skyhanni.features.nether.reputationhelper.CrimsonIsleReputationHelper
import at.hannibal2.skyhanni.features.rift.RiftAPI
import at.hannibal2.skyhanni.features.rift.area.RiftLarva
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
import at.hannibal2.skyhanni.features.rift.area.westvillage.KloonHacking
import at.hannibal2.skyhanni.features.rift.area.wyldwoods.RiftOdonata
import at.hannibal2.skyhanni.features.rift.area.wyldwoods.ShyCruxWarnings
import at.hannibal2.skyhanni.features.rift.everywhere.*
import at.hannibal2.skyhanni.features.rift.everywhere.motes.RiftMotesOrb
import at.hannibal2.skyhanni.features.rift.everywhere.motes.ShowMotesNpcSellPrice
import at.hannibal2.skyhanni.features.slayer.*
import at.hannibal2.skyhanni.features.slayer.blaze.BlazeSlayerClearView
import at.hannibal2.skyhanni.features.slayer.blaze.BlazeSlayerDaggerHelper
import at.hannibal2.skyhanni.features.slayer.blaze.BlazeSlayerFirePitsWarning
import at.hannibal2.skyhanni.features.slayer.blaze.HellionShieldHelper
import at.hannibal2.skyhanni.features.slayer.enderman.EndermanSlayerFeatures
import at.hannibal2.skyhanni.features.slayer.enderman.EndermanSlayerHideParticles
import at.hannibal2.skyhanni.features.summonings.SummoningMobManager
import at.hannibal2.skyhanni.features.summonings.SummoningSoulsName
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.test.*
import at.hannibal2.skyhanni.test.command.CopyNearbyParticlesCommand
import at.hannibal2.skyhanni.utils.EntityOutlineRenderer
import at.hannibal2.skyhanni.utils.MinecraftConsoleFilter.Companion.initLogging
import at.hannibal2.skyhanni.utils.NEUVersionCheck.checkIfNeuIsLoaded
import at.hannibal2.skyhanni.utils.TabListData
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
    version = "0.20.Beta.23",
)
class SkyHanniMod {
    @Mod.EventHandler
    fun preInit(event: FMLPreInitializationEvent?) {
        checkIfNeuIsLoaded()

        // utils
        loadModule(this)
        loadModule(ChatManager)
        loadModule(HypixelData())
        loadModule(DungeonData())
        loadModule(ScoreboardData())
        loadModule(SeaCreatureFeatures())
        loadModule(SeaCreatureManager())
        loadModule(ItemRenderBackground())
        loadModule(EntityData())
        loadModule(EntityMovementData())
        loadModule(TestExportTools)
        loadModule(ItemClickData())
//        loadModule(Year300RaffleEvent)
        loadModule(MinecraftData())
        loadModule(TitleUtils())
        loadModule(ItemTipHelper())
        loadModule(RenderLivingEntityHelper())
        loadModule(SkillExperience())
        loadModule(OtherInventoryData)
        loadModule(TabListData())
        loadModule(RenderGuiData())
        loadModule(GardenCropMilestones)
        loadModule(GardenCropUpgrades())
        loadModule(OwnInventoryData())
        loadModule(ToolTipData())
        loadModule(GuiEditManager())
        loadModule(UpdateManager)
        loadModule(CropAccessoryData())
        loadModule(MayorElection())
        loadModule(GardenComposterUpgradesData())
        loadModule(ActionBarStatsData)
        loadModule(GardenCropMilestoneInventory())
        loadModule(GardenCropSpeed)
        loadModule(ProfileStorageData)
        loadModule(TitleData())
        loadModule(BlockData())
        loadModule(DefaultConfigFeatures)
        loadModule(EntityOutlineRenderer)

        // APIs
        loadModule(BazaarApi())
        loadModule(GardenAPI)
        loadModule(CollectionAPI())
        loadModule(FarmingContestAPI)
        loadModule(FriendAPI())
        loadModule(PartyAPI())
        loadModule(SlayerAPI)
        loadModule(PurseAPI())
        loadModule(RiftAPI)
        loadModule(SackAPI)

        // features
        loadModule(BazaarOrderHelper())
        loadModule(AuctionsHighlighter())
        loadModule(ChatFilter())
        loadModule(PlayerChatModifier())
        loadModule(DungeonChatFilter())
        loadModule(HideNotClickableItems())
        loadModule(ItemDisplayOverlayFeatures())
        loadModule(CurrentPetDisplay())
        loadModule(ExpOrbsOnGroundHider())
        loadModule(FandomWikiIsSoMuchBetter())
        loadModule(DamageIndicatorManager())
        loadModule(ItemAbilityCooldown())
        loadModule(DungeonHighlightClickedBlocks())
        loadModule(DungeonMilestonesDisplay())
        loadModule(DungeonDeathCounter())
        loadModule(DungeonCleanEnd())
        loadModule(DungeonBossMessages())
        loadModule(DungeonBossHideDamageSplash())
        loadModule(TrophyFishManager())
        loadModule(TrophyFishFillet())
        loadModule(TrophyFishMessages())
        loadModule(BazaarBestSellMethod())
        loadModule(BazaarOpenPriceWebsite())
        loadModule(AnvilCombineHelper())
        loadModule(SeaCreatureMessageShortener())
        //        registerEvent(new GriffinBurrowFinder());
        loadModule(AshfangFreezeCooldown())
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
        loadModule(WikiCommand())
        loadModule(SendCoordinatedCommand())
        loadModule(WarpIsCommand())
        loadModule(PartyTransferCommand())
        loadModule(SummoningMobManager())
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
        loadModule(GriffinBurrowHelper)
        loadModule(GriffinBurrowParticleFinder())
        loadModule(BurrowWarpHelper())
        loadModule(CollectionTracker())
        loadModule(HighlightBonzoMasks())
        loadModule(DungeonLevelColor())
        loadModule(BazaarCancelledBuyOrderClipboard())
        loadModule(CompactSplashPotionMessage())
        loadModule(CroesusUnopenedChestTracker())
        loadModule(CompactBingoChat())
        loadModule(BrewingStandOverlay())
        loadModule(FishingTimer())
        loadModule(FishingHookDisplay())
        loadModule(CrimsonIsleReputationHelper(this))
        loadModule(SharkFishCounter())
        loadModule(SkyBlockLevelGuideHelper())
        loadModule(OdgerWaypoint())
        loadModule(TiaRelayHelper())
        loadModule(TiaRelayWaypoints())
        loadModule(BingoCardDisplay())
        loadModule(BingoNextStepHelper())
        loadModule(MinionCraftHelper())
        loadModule(TpsCounter())
        loadModule(ParticleHider())
        loadModule(MiscFeatures())
        loadModule(SkyMartCopperPrice())
        loadModule(GardenVisitorFeatures())
        loadModule(GardenInventoryNumbers())
        loadModule(GardenVisitorTimer())
        loadModule(GardenNextPlotPrice())
        loadModule(GardenCropMilestoneDisplay)
        loadModule(GardenCustomKeybinds)
        loadModule(ChickenHeadTimer())
        loadModule(GardenOptimalSpeed())
        loadModule(GardenDeskInSBMenu())
        loadModule(GardenLevelDisplay())
        loadModule(FarmingWeightDisplay())
        loadModule(DicerRngDropCounter())
        loadModule(CropMoneyDisplay)
        loadModule(JacobFarmingContestsInventory())
        loadModule(GardenNextJacobContest)
        loadModule(WrongFungiCutterWarning())
        loadModule(FarmingArmorDrops())
        loadModule(JoinCrystalHollows())
        loadModule(CrystalHollowsNamesInCore())
        loadModule(GardenVisitorColorNames())
        loadModule(TeleportPadCompactName())
        loadModule(AnitaMedalProfit())
        loadModule(AnitaExtraFarmingFortune())
        loadModule(ComposterDisplay())
        loadModule(GardenComposterInventoryFeatures())
        loadModule(MinionCollectLogic())
        loadModule(PasteIntoSigns())
        loadModule(PatcherSendCoordinates())
        loadModule(EstimatedItemValue)
        loadModule(EstimatedWardrobePrice())
        loadModule(ComposterInventoryNumbers())
        loadModule(FarmingFortuneDisplay())
        loadModule(ToolTooltipTweaks())
        loadModule(CropSpeedMeter())
        loadModule(AshfangMinisNametagHider())
        loadModule(TeleportPadInventoryNumber())
        loadModule(ComposterOverlay())
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
        loadModule(CaptureFarmingGear())
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
        loadModule(SlayerItemProfitTracker)
        loadModule(SlayerItemsOnGround())
        loadModule(RestorePieceOfWizardPortalLore())
        loadModule(QuickModMenuSwitch)
        loadModule(ArachneChatMessageHider())
        loadModule(ShowItemUuid())
        loadModule(FrozenTreasureTracker())
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
        loadModule(HarpFeatures())
        loadModule(EnderNodeTracker())
        loadModule(CompactBestiaryChatMessage())
        loadModule(WatchdogHider())
        loadModule(AccountUpgradeReminder())
        loadModule(PetExpTooltip())
        loadModule(Translator())
        loadModule(GardenPlotBorders())
        loadModule(CosmeticFollowingLine())
        loadModule(SuperpairsClicksAlert())
        loadModule(PowderTracker())
        loadModule(GlowingDroppedItems())
        loadModule(DungeonTeammateOutlines())

        init()

        // test stuff
        loadModule(SkyHanniDebugsAndTests())
        loadModule(CopyNearbyParticlesCommand)
        loadModule(ButtonOnPause())
        loadModule(PacketTest())
        loadModule(TestBingo)
        loadModule(TestCopyRngMeterValues)
        loadModule(TestCopyBestiaryValues)
        loadModule(HighlightMissingRepoItems())
        loadModule(ParkourWaypointSaver())
        loadModule(TestShowSlotNumber())
    }

    @Mod.EventHandler
    fun init(event: FMLInitializationEvent?) {
        configManager = ConfigManager()
        configManager.firstLoad()
        initLogging()
        Runtime.getRuntime().addShutdownHook(Thread {
            configManager.saveConfig("shutdown-hook")
        })
        repo = RepoManager(configManager.configDirectory)
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
        lateinit var repo: RepoManager
        lateinit var configManager: ConfigManager
        val logger: Logger = LogManager.getLogger("SkyHanni")
        fun getLogger(name: String): Logger {
            return LogManager.getLogger("SkyHanni.$name")
        }

        val modules: MutableList<Any> = ArrayList()
        val globalJob: Job = Job(null)
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