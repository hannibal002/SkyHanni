package at.hannibal2.skyhanni;

import at.hannibal2.skyhanni.config.Features;
import at.hannibal2.skyhanni.config.gui.commands.Commands;
import at.hannibal2.skyhanni.data.ApiData;
import at.hannibal2.skyhanni.data.HypixelData;
import at.hannibal2.skyhanni.data.ItemRenderBackground;
import at.hannibal2.skyhanni.data.ScoreboardData;
import at.hannibal2.skyhanni.data.repo.RepoManager;
import at.hannibal2.skyhanni.features.ButtonOnPause;
import at.hannibal2.skyhanni.features.CurrentPetDisplay;
import at.hannibal2.skyhanni.features.ExpBottleOnGroundHider;
import at.hannibal2.skyhanni.features.SummoningSoulsName;
import at.hannibal2.skyhanni.features.anvil.AnvilCombineHelper;
import at.hannibal2.skyhanni.features.bazaar.BazaarApi;
import at.hannibal2.skyhanni.features.bazaar.BazaarBestSellMethod;
import at.hannibal2.skyhanni.features.bazaar.BazaarOrderHelper;
import at.hannibal2.skyhanni.features.chat.ChatFilter;
import at.hannibal2.skyhanni.features.chat.ChatManager;
import at.hannibal2.skyhanni.features.chat.NewChatFilter;
import at.hannibal2.skyhanni.features.chat.PlayerChatFilter;
import at.hannibal2.skyhanni.features.damageindicator.DamageIndicatorManager;
import at.hannibal2.skyhanni.features.dungeon.*;
import at.hannibal2.skyhanni.features.fishing.SeaCreatureManager;
import at.hannibal2.skyhanni.features.fishing.SeaCreatureMessageShortener;
import at.hannibal2.skyhanni.features.fishing.TrophyFishMessages;
import at.hannibal2.skyhanni.features.items.HideNotClickableItems;
import at.hannibal2.skyhanni.features.items.ItemDisplayOverlayFeatures;
import at.hannibal2.skyhanni.features.items.abilitycooldown.ItemAbilityCooldown;
import at.hannibal2.skyhanni.features.nether.ashfang.AshfangFreezeCooldown;
import at.hannibal2.skyhanni.features.nether.ashfang.AshfangGravityOrbs;
import at.hannibal2.skyhanni.features.nether.ashfang.AshfangNextResetCooldown;
import at.hannibal2.skyhanni.test.LorenzTest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.*;
import java.nio.charset.StandardCharsets;

@Mod(modid = SkyHanniMod.MODID, version = SkyHanniMod.VERSION)
public class SkyHanniMod {

    public static final String MODID = "skyhanni";
    public static final String VERSION = "0.4.1";

    public static Features feature;
    private File configFile;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();

    public static File configDirectory;
    public static RepoManager repo;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        new BazaarApi();
        registerEvent(this);
        registerEvent(new ChatManager());
        registerEvent(new HypixelData());
        registerEvent(new DungeonData());
        registerEvent(new ScoreboardData());
        registerEvent(new ApiData());
        registerEvent(new SeaCreatureManager());
        registerEvent(new ItemRenderBackground());

        registerEvent(new BazaarOrderHelper());
        registerEvent(new ChatFilter());
        registerEvent(new NewChatFilter());
        registerEvent(new PlayerChatFilter());
        registerEvent(new DungeonChatFilter());
        registerEvent(new HideNotClickableItems());
        registerEvent(new DungeonHighlightClickedBlocks());
        registerEvent(new ItemDisplayOverlayFeatures());
        registerEvent(new CurrentPetDisplay());
        registerEvent(new ExpBottleOnGroundHider());
        registerEvent(new DamageIndicatorManager());
        registerEvent(new ItemAbilityCooldown());
        registerEvent(new DungeonMilestoneDisplay());
        registerEvent(new DungeonDeathCounter());
        registerEvent(new DungeonCleanEnd());
        registerEvent(new DungeonBossMessages());
        registerEvent(new TrophyFishMessages());
        registerEvent(new BazaarBestSellMethod());
        registerEvent(new AnvilCombineHelper());
        registerEvent(new SeaCreatureMessageShortener());
//        registerEvent(new GriffinBurrowFinder());
        registerEvent(new AshfangFreezeCooldown());
        registerEvent(new AshfangNextResetCooldown());
        registerEvent(new SummoningSoulsName());
        registerEvent(new AshfangGravityOrbs());

        Commands.init();

        registerEvent(new LorenzTest());
        registerEvent(new ButtonOnPause());

        configDirectory = new File("config/skyhanni");
        try {
            //noinspection ResultOfMethodCallIgnored
            configDirectory.mkdir();
        } catch (Exception ignored) {
        }

        configFile = new File(configDirectory, "config.json");

        if (configFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8))) {
                feature = gson.fromJson(reader, Features.class);
            } catch (Exception ignored) {
            }
        }

        if (feature == null) {
            feature = new Features();
            saveConfig();
        }
        Runtime.getRuntime().addShutdownHook(new Thread(this::saveConfig));

        repo = new RepoManager(configDirectory);
        repo.loadRepoInformation();
    }

    private void registerEvent(Object object) {
        String simpleName = object.getClass().getSimpleName();
        System.out.println("SkyHanni registering '" + simpleName + "'");
        long start = System.currentTimeMillis();
        MinecraftForge.EVENT_BUS.register(object);
        long duration = System.currentTimeMillis() - start;
        System.out.println("Done after " + duration + " ms!");
    }

    public void saveConfig() {
        try {
            //noinspection ResultOfMethodCallIgnored
            configFile.createNewFile();

            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8))) {
                writer.write(gson.toJson(feature));
            }
        } catch (IOException ignored) {
        }
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
}
