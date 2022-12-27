package at.hannibal2.skyhanni;

import at.hannibal2.skyhanni.config.ConfigManager;
import at.hannibal2.skyhanni.config.Features;
import at.hannibal2.skyhanni.config.commands.Commands;
import at.hannibal2.skyhanni.data.*;
import at.hannibal2.skyhanni.data.repo.RepoManager;
import at.hannibal2.skyhanni.features.*;
import at.hannibal2.skyhanni.features.anvil.AnvilCombineHelper;
import at.hannibal2.skyhanni.features.bazaar.BazaarApi;
import at.hannibal2.skyhanni.features.bazaar.BazaarBestSellMethod;
import at.hannibal2.skyhanni.features.bazaar.BazaarOrderHelper;
import at.hannibal2.skyhanni.features.chat.ChatFilter;
import at.hannibal2.skyhanni.features.chat.PlayerDeathMessages;
import at.hannibal2.skyhanni.features.chat.playerchat.PlayerChatFilter;
import at.hannibal2.skyhanni.features.chat.playerchat.PlayerChatModifier;
import at.hannibal2.skyhanni.features.commands.WikiCommand;
import at.hannibal2.skyhanni.features.damageindicator.DamageIndicatorManager;
import at.hannibal2.skyhanni.features.dungeon.*;
import at.hannibal2.skyhanni.features.end.VoidlingExtremistColor;
import at.hannibal2.skyhanni.features.event.diana.BurrowWarpHelper;
import at.hannibal2.skyhanni.features.event.diana.GriffinBurrowHelper;
import at.hannibal2.skyhanni.features.event.diana.GriffinBurrowParticleFinder;
import at.hannibal2.skyhanni.features.event.diana.SoopyGuessBurrow;
import at.hannibal2.skyhanni.features.fishing.SeaCreatureManager;
import at.hannibal2.skyhanni.features.fishing.SeaCreatureMessageShortener;
import at.hannibal2.skyhanni.features.fishing.TrophyFishMessages;
import at.hannibal2.skyhanni.features.inventory.*;
import at.hannibal2.skyhanni.features.itemabilities.FireVeilWandParticles;
import at.hannibal2.skyhanni.features.itemabilities.abilitycooldown.ItemAbilityCooldown;
import at.hannibal2.skyhanni.features.minion.MinionFeatures;
import at.hannibal2.skyhanni.features.nether.MilleniaAgedBlazeColor;
import at.hannibal2.skyhanni.features.nether.ashfang.*;
import at.hannibal2.skyhanni.features.slayer.EndermanSlayerBeacon;
import at.hannibal2.skyhanni.features.slayer.HideMobNames;
import at.hannibal2.skyhanni.features.slayer.HighlightSlayerMiniboss;
import at.hannibal2.skyhanni.features.slayer.SlayerQuestWarning;
import at.hannibal2.skyhanni.features.slayer.blaze.*;
import at.hannibal2.skyhanni.features.summonings.SummoningMobManager;
import at.hannibal2.skyhanni.features.summonings.SummoningSoulsName;
import at.hannibal2.skyhanni.test.LorenzTest;
import at.hannibal2.skyhanni.utils.MinecraftConsoleFilter;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.MinecraftForge;
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

@Mod(modid = SkyHanniMod.MODID, version = SkyHanniMod.VERSION, clientSideOnly = true, useMetadata = true, guiFactory = "at.hannibal2.skyhanni.config.ConfigGuiForgeInterop")
public class SkyHanniMod {

    public static final String MODID = "skyhanni";
    public static final String VERSION = "0.13";

    public static Features feature;

    public static RepoManager repo;
    public static ConfigManager configManager;
    private static Logger logger;

    public static List<Object> listenerClasses = new ArrayList<>();
    public static Job globalJob = JobKt.Job(null);
    public static CoroutineScope coroutineScope =
            CoroutineScopeKt.CoroutineScope(
                    EmptyCoroutineContext.INSTANCE
                            .plus(new CoroutineName("SkyHanni")) // I love calling KotLin from JaVa
                            .plus(SupervisorKt.SupervisorJob(globalJob)));

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = LogManager.getLogger("SkyHanni");

        //API and utils
        new BazaarApi();
        registerEvent(this);
        registerEvent(new ChatManager());
        registerEvent(new HypixelData());
        registerEvent(new DungeonData());
        registerEvent(new ScoreboardData());
        registerEvent(new ApiKeyGrabber());
        registerEvent(new SeaCreatureManager());
        registerEvent(new ItemRenderBackground());
        registerEvent(new EntityData());
        registerEvent(new EntityMovementData());
        registerEvent(new ItemClickData());
        registerEvent(new MinecraftData());
        registerEvent(new SendTitleHelper());
        registerEvent(new ItemTipHelper());

        //features
        registerEvent(new BazaarOrderHelper());
        registerEvent(new ChatFilter());
        registerEvent(new PlayerChatModifier());
        registerEvent(new DungeonChatFilter());
        registerEvent(new HideNotClickableItems());
        registerEvent(new ItemDisplayOverlayFeatures());
        registerEvent(new CurrentPetDisplay());
        registerEvent(new ExpBottleOnGroundHider());
        registerEvent(new DamageIndicatorManager());
        registerEvent(new ItemAbilityCooldown());
        registerEvent(new DungeonHighlightClickedBlocks());
        registerEvent(new DungeonMilestonesDisplay());
        registerEvent(new DungeonDeathCounter());
        registerEvent(new DungeonCleanEnd());
        registerEvent(new DungeonBossMessages());
        registerEvent(new DungeonBossHideDamageSplash());
        registerEvent(new TrophyFishMessages());
        registerEvent(new BazaarBestSellMethod());
        registerEvent(new AnvilCombineHelper());
        registerEvent(new SeaCreatureMessageShortener());
//        registerEvent(new GriffinBurrowFinder());
        registerEvent(new AshfangFreezeCooldown());
        registerEvent(new AshfangNextResetCooldown());
        registerEvent(new SummoningSoulsName());
        registerEvent(new AshfangGravityOrbs());
        registerEvent(new AshfangBlazingSouls());
        registerEvent(new AshfangBlazes());
        registerEvent(new AshfangHideParticles());
        registerEvent(new AshfangHideDamageIndicator());
        registerEvent(new ItemStars());
        registerEvent(new MinionFeatures());
        registerEvent(new RealTime());
        registerEvent(new RngMeterInventory());
        registerEvent(new WikiCommand());
        registerEvent(new SummoningMobManager());
        registerEvent(new VoidlingExtremistColor());
        registerEvent(new MilleniaAgedBlazeColor());
        registerEvent(new CorruptedMobHighlight());
        registerEvent(new MarkedPlayerManager());
        registerEvent(new HighlightSlayerMiniboss());
        registerEvent(new PlayerDeathMessages());
        registerEvent(new HighlightDungeonDeathmite());
        registerEvent(new DungeonHideItems());
        registerEvent(new DungeonCopilot());
        registerEvent(new EndermanSlayerBeacon());
        registerEvent(new FireVeilWandParticles());
        registerEvent(new HideMobNames());
        registerEvent(new HideDamageSplash());
        registerEvent(new ThunderSparksHighlight());
        registerEvent(new BlazeSlayerPillar());
        registerEvent(new BlazeSlayerDaggerHelper());
        registerEvent(new HellionShieldHelper());
        registerEvent(new BlazeSlayerFirePitsWarning());
        registerEvent(new BlazeSlayerClearView());
        registerEvent(new PlayerChatFilter());
        registerEvent(new HideArmor());
        registerEvent(new SlayerQuestWarning());
        registerEvent(new StatsTuning());
        registerEvent(new NonGodPotEffectDisplay());
        registerEvent(new HideBlazeParticles());
        registerEvent(new SoopyGuessBurrow());
        registerEvent(new GriffinBurrowHelper());
        registerEvent(new GriffinBurrowParticleFinder());
        registerEvent(new BurrowWarpHelper());
        registerEvent(new HighlightBonzoMasks());
        registerEvent(new DungeonLevelColor());

        Commands.INSTANCE.init();

        registerEvent(new LorenzTest());
        registerEvent(new ButtonOnPause());

        configManager = new ConfigManager();
        configManager.firstLoad();
        MinecraftConsoleFilter.Companion.initLogging();

        Runtime.getRuntime().addShutdownHook(new Thread(configManager::saveConfig));

        repo = new RepoManager(configManager.getConfigDirectory());
        repo.loadRepoInformation();
    }

    private void registerEvent(Object object) {
        listenerClasses.add(object);
        String simpleName = object.getClass().getSimpleName();
        consoleLog("SkyHanni registering '" + simpleName + "'");
        long start = System.currentTimeMillis();
        MinecraftForge.EVENT_BUS.register(object);
        long duration = System.currentTimeMillis() - start;
        consoleLog("Done after " + duration + " ms!");
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
