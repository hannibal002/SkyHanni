package at.hannibal2.skyhanni

import at.hannibal2.skyhanni.api.CollectionAPI
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.Features
import at.hannibal2.skyhanni.config.commands.Commands.init
import at.hannibal2.skyhanni.data.*
import at.hannibal2.skyhanni.data.repo.RepoManager
import at.hannibal2.skyhanni.features.anvil.AnvilCombineHelper
import at.hannibal2.skyhanni.features.bazaar.BazaarApi
import at.hannibal2.skyhanni.features.bazaar.BazaarBestSellMethod
import at.hannibal2.skyhanni.features.bazaar.BazaarCancelledBuyOrderClipboard
import at.hannibal2.skyhanni.features.bazaar.BazaarOrderHelper
import at.hannibal2.skyhanni.features.bingo.BingoCardDisplay
import at.hannibal2.skyhanni.features.bingo.BingoNextStepHelper
import at.hannibal2.skyhanni.features.bingo.CompactBingoChat
import at.hannibal2.skyhanni.features.bingo.MinionCraftHelper
import at.hannibal2.skyhanni.features.chat.ChatFilter
import at.hannibal2.skyhanni.features.chat.PlayerDeathMessages
import at.hannibal2.skyhanni.features.chat.playerchat.PlayerChatFilter
import at.hannibal2.skyhanni.features.chat.playerchat.PlayerChatModifier
import at.hannibal2.skyhanni.features.commands.WikiCommand
import at.hannibal2.skyhanni.features.damageindicator.DamageIndicatorManager
import at.hannibal2.skyhanni.features.dungeon.*
import at.hannibal2.skyhanni.features.event.diana.BurrowWarpHelper
import at.hannibal2.skyhanni.features.event.diana.GriffinBurrowHelper
import at.hannibal2.skyhanni.features.event.diana.GriffinBurrowParticleFinder
import at.hannibal2.skyhanni.features.event.diana.SoopyGuessBurrow
import at.hannibal2.skyhanni.features.fishing.*
import at.hannibal2.skyhanni.features.garden.*
import at.hannibal2.skyhanni.features.garden.composter.ComposterDisplay
import at.hannibal2.skyhanni.features.garden.composter.ComposterInventoryNumbers
import at.hannibal2.skyhanni.features.garden.composter.ComposterOverlay
import at.hannibal2.skyhanni.features.garden.composter.GardenComposterInventoryFeatures
import at.hannibal2.skyhanni.features.garden.contest.FarmingContestAPI
import at.hannibal2.skyhanni.features.garden.contest.JacobContestFFNeededDisplay
import at.hannibal2.skyhanni.features.garden.contest.JacobFarmingContestsInventory
import at.hannibal2.skyhanni.features.garden.farming.*
import at.hannibal2.skyhanni.features.garden.inventory.*
import at.hannibal2.skyhanni.features.garden.visitor.GardenVisitorColorNames
import at.hannibal2.skyhanni.features.garden.visitor.GardenVisitorFeatures
import at.hannibal2.skyhanni.features.garden.visitor.GardenVisitorTimer
import at.hannibal2.skyhanni.features.inventory.*
import at.hannibal2.skyhanni.features.itemabilities.FireVeilWandParticles
import at.hannibal2.skyhanni.features.itemabilities.abilitycooldown.ItemAbilityCooldown
import at.hannibal2.skyhanni.features.minion.MinionCollectLogic
import at.hannibal2.skyhanni.features.minion.MinionFeatures
import at.hannibal2.skyhanni.features.misc.*
import at.hannibal2.skyhanni.features.misc.discordrpc.DiscordRPCManager
import at.hannibal2.skyhanni.features.misc.tiarelay.TiaRelayHelper
import at.hannibal2.skyhanni.features.misc.tiarelay.TiaRelayWaypoints
import at.hannibal2.skyhanni.features.misc.update.UpdateManager
import at.hannibal2.skyhanni.features.mobs.AreaMiniBossFeatures
import at.hannibal2.skyhanni.features.mobs.AshfangMinisNametagHider
import at.hannibal2.skyhanni.features.mobs.MobHighlight
import at.hannibal2.skyhanni.features.nether.ashfang.*
import at.hannibal2.skyhanni.features.nether.reputationhelper.CrimsonIsleReputationHelper
import at.hannibal2.skyhanni.features.slayer.EndermanSlayerBeacon
import at.hannibal2.skyhanni.features.slayer.HideMobNames
import at.hannibal2.skyhanni.features.slayer.HighlightSlayerMiniBoss
import at.hannibal2.skyhanni.features.slayer.SlayerQuestWarning
import at.hannibal2.skyhanni.features.slayer.blaze.BlazeSlayerClearView
import at.hannibal2.skyhanni.features.slayer.blaze.BlazeSlayerDaggerHelper
import at.hannibal2.skyhanni.features.slayer.blaze.BlazeSlayerFirePitsWarning
import at.hannibal2.skyhanni.features.slayer.blaze.HellionShieldHelper
import at.hannibal2.skyhanni.features.summonings.SummoningMobManager
import at.hannibal2.skyhanni.features.summonings.SummoningSoulsName
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.test.PacketTest
import at.hannibal2.skyhanni.test.SkyHanniTestCommand
import at.hannibal2.skyhanni.test.TestBingo
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
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

@Mod(
    modid = SkyHanniMod.MODID,
    clientSideOnly = true,
    useMetadata = true,
    guiFactory = "at.hannibal2.skyhanni.config.ConfigGuiForgeInterop",
    version = "0.17.Beta.41.1",
)
class SkyHanniMod {
    @Mod.EventHandler
    fun preInit(event: FMLPreInitializationEvent?) {
        checkIfNeuIsLoaded()

        // utils
        loadModule(this)
        loadModule(ChatManager())
        loadModule(HypixelData())
        loadModule(DungeonData())
        loadModule(ScoreboardData())
        loadModule(ApiDataLoader())
        loadModule(SeaCreatureManager())
        loadModule(ItemRenderBackground())
        loadModule(EntityData())
        loadModule(EntityMovementData())
        loadModule(ItemClickData())
        loadModule(MinecraftData())
        loadModule(TitleUtils())
        loadModule(ItemTipHelper())
        loadModule(RenderLivingEntityHelper())
        loadModule(SkillExperience())
        loadModule(OtherInventoryData())
        loadModule(TabListData())
        loadModule(RenderGuiData())
        loadModule(GardenCropMilestones())
        loadModule(GardenCropUpgrades())
        loadModule(OwnInventoryData())
        loadModule(ToolTipData())
        loadModule(GuiEditManager())
        loadModule(UpdateManager)
        loadModule(CropAccessoryData())
        loadModule(MayorElection())
        loadModule(GardenComposterUpgradesData())
        loadModule(ActionBarStatsData())
        loadModule(GardenCropMilestoneAverage())
        loadModule(GardenCropSpeed)

        // APIs
        loadModule(BazaarApi())
        loadModule(GardenAPI)
        loadModule(CollectionAPI())
        loadModule(FarmingContestAPI)

        // features
        loadModule(BazaarOrderHelper())
        loadModule(AuctionsHighlighter())
        loadModule(ChatFilter())
        loadModule(PlayerChatModifier())
        loadModule(DungeonChatFilter())
        loadModule(HideNotClickableItems())
        loadModule(ItemDisplayOverlayFeatures())
        loadModule(CurrentPetDisplay())
        loadModule(ExpBottleOnGroundHider())
        loadModule(DamageIndicatorManager())
        loadModule(ItemAbilityCooldown())
        loadModule(DungeonHighlightClickedBlocks())
        loadModule(DungeonMilestonesDisplay())
        loadModule(DungeonDeathCounter())
        loadModule(DungeonCleanEnd())
        loadModule(DungeonBossMessages())
        loadModule(DungeonBossHideDamageSplash())
        loadModule(TrophyFishMessages())
        loadModule(BazaarBestSellMethod())
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
        loadModule(RealTime())
        loadModule(RngMeterInventory())
        loadModule(WikiCommand())
        loadModule(SummoningMobManager())
        loadModule(AreaMiniBossFeatures())
        loadModule(MobHighlight())
        loadModule(MarkedPlayerManager())
        loadModule(HighlightSlayerMiniBoss())
        loadModule(PlayerDeathMessages())
        loadModule(HighlightDungeonDeathmite())
        loadModule(DungeonHideItems())
        loadModule(DungeonCopilot())
        loadModule(EndermanSlayerBeacon())
        loadModule(FireVeilWandParticles())
        loadModule(HideMobNames())
        loadModule(HideDamageSplash())
        loadModule(ThunderSparksHighlight())
        loadModule(BlazeSlayerDaggerHelper())
        loadModule(HellionShieldHelper())
        loadModule(BlazeSlayerFirePitsWarning())
        loadModule(BlazeSlayerClearView())
        loadModule(PlayerChatFilter())
        loadModule(HideArmor())
        loadModule(SlayerQuestWarning())
        loadModule(StatsTuning())
        loadModule(NonGodPotEffectDisplay())
        loadModule(SoopyGuessBurrow())
        loadModule(GriffinBurrowHelper())
        loadModule(GriffinBurrowParticleFinder())
        loadModule(BurrowWarpHelper())
        loadModule(CollectionCounter())
        loadModule(HighlightBonzoMasks())
        loadModule(DungeonLevelColor())
        loadModule(BazaarCancelledBuyOrderClipboard())
        loadModule(CompactSplashPotionMessage())
        loadModule(CroesusUnopenedChestTracker())
        loadModule(CompactBingoChat())
        loadModule(BrewingStandOverlay())
        loadModule(BarnFishingTimer())
        loadModule(CrimsonIsleReputationHelper(this))
        loadModule(SharkFishCounter())
        loadModule(SkyBLockLevelGuideHelper())
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
        loadModule(EliteFarmingWeight())
        loadModule(DicerRngDropCounter())
        loadModule(CropMoneyDisplay())
        loadModule(JacobFarmingContestsInventory())
        loadModule(GardenNextJacobContest())
        loadModule(WrongFungiCutterWarning())
        loadModule(FarmingArmorDrops())
        loadModule(JoinCrystalHollows())
        loadModule(GardenVisitorColorNames())
        loadModule(GardenTeleportPadCompactName())
        loadModule(AnitaMedalProfit())
        loadModule(ComposterDisplay())
        loadModule(GardenComposterInventoryFeatures())
        loadModule(MinionCollectLogic())
        loadModule(PasteIntoSigns())
        loadModule(EstimatedItemValue())
        loadModule(ComposterInventoryNumbers())
        loadModule(FarmingFortuneDisplay())
        loadModule(ToolTooltipTweaks())
        loadModule(CropSpeedMeter())
        loadModule(AshfangMinisNametagHider())
        loadModule(GardenTeleportPadInventoryNumber())
        loadModule(ComposterOverlay())
        loadModule(DiscordRPCManager())
        loadModule(GardenCropMilestoneFix())
        loadModule(GardenBurrowingSporesNotifier())
        loadModule(WildStrawberryDyeNotification())
        loadModule(JacobContestFFNeededDisplay())
        loadModule(GardenYawAndPitch())
        loadModule(MovementSpeedDisplay())
        loadModule(ChumBucketHider())
        loadModule(GardenRecentTeleportPadsDisplay())

        init()

        loadModule(SkyHanniTestCommand())
        loadModule(ButtonOnPause())
        loadModule(PacketTest())
        loadModule(TestBingo)
    }

    @Mod.EventHandler
    fun init(event: FMLInitializationEvent?) {
        configManager = ConfigManager()
        configManager.firstLoad()
        initLogging()
        Runtime.getRuntime().addShutdownHook(Thread { configManager.saveConfig("shutdown-hook") })
        repo = RepoManager(configManager.configDirectory)
        repo.loadRepoInformation()
    }

    fun loadModule(obj: Any) {
        modules.add(obj)
        MinecraftForge.EVENT_BUS.register(obj)
    }

    @SubscribeEvent
    fun onClientTick(event: ClientTickEvent?) {
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