package at.hannibal2.skyhanni;

import at.hannibal2.skyhanni.api.CollectionAPI;
import at.hannibal2.skyhanni.config.ConfigManager;
import at.hannibal2.skyhanni.config.Features;
import at.hannibal2.skyhanni.config.commands.Commands;
import at.hannibal2.skyhanni.data.*;
import at.hannibal2.skyhanni.data.repo.RepoManager;
import at.hannibal2.skyhanni.features.anvil.AnvilCombineHelper;
import at.hannibal2.skyhanni.features.bazaar.BazaarApi;
import at.hannibal2.skyhanni.features.bazaar.BazaarBestSellMethod;
import at.hannibal2.skyhanni.features.bazaar.BazaarCancelledBuyOrderClipboard;
import at.hannibal2.skyhanni.features.bazaar.BazaarOrderHelper;
import at.hannibal2.skyhanni.features.bingo.BingoCardDisplay;
import at.hannibal2.skyhanni.features.bingo.BingoNextStepHelper;
import at.hannibal2.skyhanni.features.bingo.CompactBingoChat;
import at.hannibal2.skyhanni.features.bingo.MinionCraftHelper;
import at.hannibal2.skyhanni.features.chat.ChatFilter;
import at.hannibal2.skyhanni.features.chat.PlayerDeathMessages;
import at.hannibal2.skyhanni.features.chat.playerchat.PlayerChatFilter;
import at.hannibal2.skyhanni.features.chat.playerchat.PlayerChatModifier;
import at.hannibal2.skyhanni.features.commands.WikiCommand;
import at.hannibal2.skyhanni.features.damageindicator.DamageIndicatorManager;
import at.hannibal2.skyhanni.features.dungeon.*;
import at.hannibal2.skyhanni.features.event.diana.BurrowWarpHelper;
import at.hannibal2.skyhanni.features.event.diana.GriffinBurrowHelper;
import at.hannibal2.skyhanni.features.event.diana.GriffinBurrowParticleFinder;
import at.hannibal2.skyhanni.features.event.diana.SoopyGuessBurrow;
import at.hannibal2.skyhanni.features.fishing.*;
import at.hannibal2.skyhanni.features.garden.*;
import at.hannibal2.skyhanni.features.garden.composter.ComposterDisplay;
import at.hannibal2.skyhanni.features.garden.composter.ComposterInventoryNumbers;
import at.hannibal2.skyhanni.features.garden.composter.ComposterOverlay;
import at.hannibal2.skyhanni.features.garden.composter.GardenComposterInventoryFeatures;
import at.hannibal2.skyhanni.features.garden.farming.*;
import at.hannibal2.skyhanni.features.garden.inventory.*;
import at.hannibal2.skyhanni.features.garden.visitor.GardenVisitorColorNames;
import at.hannibal2.skyhanni.features.garden.visitor.GardenVisitorFeatures;
import at.hannibal2.skyhanni.features.garden.visitor.GardenVisitorTimer;
import at.hannibal2.skyhanni.features.inventory.*;
import at.hannibal2.skyhanni.features.itemabilities.FireVeilWandParticles;
import at.hannibal2.skyhanni.features.itemabilities.abilitycooldown.ItemAbilityCooldown;
import at.hannibal2.skyhanni.features.minion.MinionCollectLogic;
import at.hannibal2.skyhanni.features.minion.MinionFeatures;
import at.hannibal2.skyhanni.features.misc.*;
import at.hannibal2.skyhanni.features.misc.discordrpc.DiscordRPCManager;
import at.hannibal2.skyhanni.features.misc.tiarelay.TiaRelayHelper;
import at.hannibal2.skyhanni.features.misc.tiarelay.TiaRelayWaypoints;
import at.hannibal2.skyhanni.features.misc.update.UpdateManager;
import at.hannibal2.skyhanni.features.mobs.AreaMiniBossFeatures;
import at.hannibal2.skyhanni.features.mobs.AshfangMinisNametagHider;
import at.hannibal2.skyhanni.features.mobs.MobHighlight;
import at.hannibal2.skyhanni.features.nether.ashfang.*;
import at.hannibal2.skyhanni.features.nether.reputationhelper.CrimsonIsleReputationHelper;
import at.hannibal2.skyhanni.features.slayer.EndermanSlayerBeacon;
import at.hannibal2.skyhanni.features.slayer.HideMobNames;
import at.hannibal2.skyhanni.features.slayer.HighlightSlayerMiniBoss;
import at.hannibal2.skyhanni.features.slayer.SlayerQuestWarning;
import at.hannibal2.skyhanni.features.slayer.blaze.BlazeSlayerClearView;
import at.hannibal2.skyhanni.features.slayer.blaze.BlazeSlayerDaggerHelper;
import at.hannibal2.skyhanni.features.slayer.blaze.BlazeSlayerFirePitsWarning;
import at.hannibal2.skyhanni.features.slayer.blaze.HellionShieldHelper;
import at.hannibal2.skyhanni.features.summonings.SummoningMobManager;
import at.hannibal2.skyhanni.features.summonings.SummoningSoulsName;
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper;
import at.hannibal2.skyhanni.test.LorenzTest;
import at.hannibal2.skyhanni.test.PacketTest;
import at.hannibal2.skyhanni.utils.MinecraftConsoleFilter;
import at.hannibal2.skyhanni.utils.TabListData;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

@Mod(modid = SkyHanniMod.MODID, clientSideOnly = true, useMetadata = true,
        guiFactory = "at.hannibal2.skyhanni.config.ConfigGuiForgeInterop",
        dependencies = SkyHanniMod.DEPENDENCIES)
public class SkyHanniMod {

    public static final String MODID = "skyhanni";

    public static String getVersion() {
        return Loader.instance().getIndexedModList().get(MODID).getVersion();
    }

    public static final String DEPENDENCIES = "after:notenoughupdates@[2.1.1,);";

    public static Features feature;

    public static RepoManager repo;
    public static ConfigManager configManager;
    private static Logger logger;
    public static org.slf4j.Logger getLogger(String name) {
        return org.slf4j.LoggerFactory.getLogger("SkyHanni." + name);
    }

    public static List<Object> modules = new ArrayList<>();
    public static Job globalJob = JobKt.Job(null);
    public static CoroutineScope coroutineScope =
            CoroutineScopeKt.CoroutineScope(
                    EmptyCoroutineContext.INSTANCE
                            .plus(new CoroutineName("SkyHanni")) // I love calling KotLin from JaVa
                            .plus(SupervisorKt.SupervisorJob(globalJob)));

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = LogManager.getLogger("SkyHanni");

        // utils
        loadModule(this);
        loadModule(new ChatManager());
        loadModule(new HypixelData());
        loadModule(new DungeonData());
        loadModule(new ScoreboardData());
        loadModule(new ApiDataLoader());
        loadModule(new SeaCreatureManager());
        loadModule(new ItemRenderBackground());
        loadModule(new EntityData());
        loadModule(new EntityMovementData());
        loadModule(new ItemClickData());
        loadModule(new MinecraftData());
        loadModule(new TitleUtils());
        loadModule(new ItemTipHelper());
        loadModule(new RenderLivingEntityHelper());
        loadModule(new SkillExperience());
        loadModule(new OtherInventoryData());
        loadModule(new TabListData());
        loadModule(new RenderGuiData());
        loadModule(new GardenCropMilestones());
        loadModule(new GardenCropUpgrades());
        loadModule(new OwnInventoryData());
        loadModule(new ToolTipData());
        loadModule(new GuiEditManager());
        loadModule(UpdateManager.INSTANCE);
        loadModule(new CropAccessoryData());
        loadModule(new MayorElectionData());
        loadModule(new GardenComposterUpgradesData());

        // APIs
        loadModule(new BazaarApi());
        loadModule(GardenAPI.INSTANCE);
        loadModule(new CollectionAPI());

        // features
        loadModule(new BazaarOrderHelper());
        loadModule(new AuctionsHighlighter());
        loadModule(new ChatFilter());
        loadModule(new PlayerChatModifier());
        loadModule(new DungeonChatFilter());
        loadModule(new HideNotClickableItems());
        loadModule(new ItemDisplayOverlayFeatures());
        loadModule(new CurrentPetDisplay());
        loadModule(new ExpBottleOnGroundHider());
        loadModule(new DamageIndicatorManager());
        loadModule(new ItemAbilityCooldown());
        loadModule(new DungeonHighlightClickedBlocks());
        loadModule(new DungeonMilestonesDisplay());
        loadModule(new DungeonDeathCounter());
        loadModule(new DungeonCleanEnd());
        loadModule(new DungeonBossMessages());
        loadModule(new DungeonBossHideDamageSplash());
        loadModule(new TrophyFishMessages());
        loadModule(new BazaarBestSellMethod());
        loadModule(new AnvilCombineHelper());
        loadModule(new SeaCreatureMessageShortener());
//        registerEvent(new GriffinBurrowFinder());
        loadModule(new AshfangFreezeCooldown());
        loadModule(new AshfangNextResetCooldown());
        loadModule(new SummoningSoulsName());
        loadModule(new AshfangGravityOrbs());
        loadModule(new AshfangBlazingSouls());
        loadModule(new AshfangBlazes());
        loadModule(new AshfangHideParticles());
        loadModule(new AshfangHideDamageIndicator());
        loadModule(new ItemStars());
        loadModule(new MinionFeatures());
        loadModule(new RealTime());
        loadModule(new RngMeterInventory());
        loadModule(new WikiCommand());
        loadModule(new SummoningMobManager());
        loadModule(new AreaMiniBossFeatures());
        loadModule(new MobHighlight());
        loadModule(new MarkedPlayerManager());
        loadModule(new HighlightSlayerMiniBoss());
        loadModule(new PlayerDeathMessages());
        loadModule(new HighlightDungeonDeathmite());
        loadModule(new DungeonHideItems());
        loadModule(new DungeonCopilot());
        loadModule(new EndermanSlayerBeacon());
        loadModule(new FireVeilWandParticles());
        loadModule(new HideMobNames());
        loadModule(new HideDamageSplash());
        loadModule(new ThunderSparksHighlight());
        loadModule(new BlazeSlayerDaggerHelper());
        loadModule(new HellionShieldHelper());
        loadModule(new BlazeSlayerFirePitsWarning());
        loadModule(new BlazeSlayerClearView());
        loadModule(new PlayerChatFilter());
        loadModule(new HideArmor());
        loadModule(new SlayerQuestWarning());
        loadModule(new StatsTuning());
        loadModule(new NonGodPotEffectDisplay());
        loadModule(new SoopyGuessBurrow());
        loadModule(new GriffinBurrowHelper());
        loadModule(new GriffinBurrowParticleFinder());
        loadModule(new BurrowWarpHelper());
        loadModule(new CollectionCounter());
        loadModule(new HighlightBonzoMasks());
        loadModule(new DungeonLevelColor());
        loadModule(new BazaarCancelledBuyOrderClipboard());
        loadModule(new CompactSplashPotionMessage());
        loadModule(new CroesusUnopenedChestTracker());
        loadModule(new CompactBingoChat());
        loadModule(new BrewingStandOverlay());
        loadModule(new BarnFishingTimer());
        loadModule(new CrimsonIsleReputationHelper(this));
        loadModule(new SharkFishCounter());
        loadModule(new SkyBLockLevelGuideHelper());
        loadModule(new OdgerWaypoint());
        loadModule(new TiaRelayHelper());
        loadModule(new TiaRelayWaypoints());
        loadModule(new BingoCardDisplay());
        loadModule(new BingoNextStepHelper());
        loadModule(new MinionCraftHelper());
        loadModule(new TpsCounter());
        loadModule(new ParticleHider());
        loadModule(new MiscFeatures());
        loadModule(new SkyMartCopperPrice());
        loadModule(new GardenVisitorFeatures());
        loadModule(new GardenInventoryNumbers());
        loadModule(new GardenVisitorTimer());
        loadModule(new GardenNextPlotPrice());
        loadModule(new GardenCropMilestoneDisplay());
        loadModule(new GardenCustomKeybinds());
        loadModule(new ChickenHeadTimer());
        loadModule(new GardenOptimalSpeed());
        loadModule(new GardenDeskInSBMenu());
        loadModule(new GardenLevelDisplay());
        loadModule(new EliteFarmingWeight());
        loadModule(new DicerRngDropCounter());
        loadModule(new CropMoneyDisplay());
        loadModule(new JacobFarmingContestsInventory());
        loadModule(new GardenNextJacobContest());
        loadModule(new WrongFungiCutterWarning());
        loadModule(new FarmingArmorDrops());
        loadModule(new JoinCrystalHollows());
        loadModule(new GardenVisitorColorNames());
        loadModule(new GardenTeleportPadCompactName());
        loadModule(new AnitaMedalProfit());
        loadModule(new ComposterDisplay());
        loadModule(new GardenComposterInventoryFeatures());
        loadModule(new MinionCollectLogic());
        loadModule(new PasteIntoSigns());
        loadModule(new EstimatedItemValue());
        loadModule(new ComposterInventoryNumbers());
        loadModule(new FarmingFortuneDisplay());
        loadModule(new ToolTooltipTweaks());
        loadModule(new CropSpeedMeter());
        loadModule(new AshfangMinisNametagHider());
        loadModule(new GardenTeleportPadInventoryNumber());
        loadModule(new ComposterOverlay());
        loadModule(new DiscordRPCManager());

        Commands.INSTANCE.init();

        loadModule(new LorenzTest());
        loadModule(new ButtonOnPause());
        loadModule(new PacketTest());

        configManager = new ConfigManager();
        configManager.firstLoad();

        MinecraftConsoleFilter.Companion.initLogging();

        Runtime.getRuntime().addShutdownHook(new Thread(configManager::saveConfig));

        repo = new RepoManager(configManager.getConfigDirectory());
        repo.loadRepoInformation();
    }

    public void loadModule(Object object) {
        modules.add(object);
        MinecraftForge.EVENT_BUS.register(object);
    }

    public static GuiScreen screenToOpen = null;
    private static int screenTicks = 0;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (screenToOpen != null) {
            screenTicks++;
            if (screenTicks == 5) {
                Minecraft.getMinecraft().displayGuiScreen(screenToOpen);
                screenTicks = 0;
                screenToOpen = null;
            }
        }
    }

    public static void consoleLog(String message) {
        if (logger != null) {
            logger.log(Level.INFO, message);
        } else {
            System.out.println("consoleLog: (" + message + ")");
        }
    }
}