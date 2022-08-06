package at.hannibal2.skyhanni;

import at.hannibal2.skyhanni.bazaar.BazaarApi;
import at.hannibal2.skyhanni.bazaar.BazaarBestSellMethod;
import at.hannibal2.skyhanni.bazaar.BazaarOrderHelper;
import at.hannibal2.skyhanni.chat.ChatFilter;
import at.hannibal2.skyhanni.chat.ChatManager;
import at.hannibal2.skyhanni.chat.NewChatFilter;
import at.hannibal2.skyhanni.chat.PlayerChatFilter;
import at.hannibal2.skyhanni.config.Features;
import at.hannibal2.skyhanni.config.gui.commands.Commands;
import at.hannibal2.skyhanni.diana.GriffinBurrowFinder;
import at.hannibal2.skyhanni.dungeon.*;
import at.hannibal2.skyhanni.dungeon.damageindicator.DungeonBossDamageIndicator;
import at.hannibal2.skyhanni.features.abilities.AshfangFreezeCooldown;
import at.hannibal2.skyhanni.fishing.SeaCreatureManager;
import at.hannibal2.skyhanni.fishing.SeaCreatureMessageShortener;
import at.hannibal2.skyhanni.fishing.TrophyFishMessages;
import at.hannibal2.skyhanni.inventory.anvil.AnvilCombineHelper;
import at.hannibal2.skyhanni.items.HideNotClickableItems;
import at.hannibal2.skyhanni.items.ItemDisplayOverlayFeatures;
import at.hannibal2.skyhanni.items.abilitycooldown.ItemAbilityCooldown;
import at.hannibal2.skyhanni.misc.*;
import at.hannibal2.skyhanni.repo.RepoManager;
import at.hannibal2.skyhanni.test.LorenzTest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@Mod(modid = SkyHanniMod.MODID, version = SkyHanniMod.VERSION)
public class SkyHanniMod {

    public static final String MODID = "skyhanni";
    public static final String VERSION = "0.1";

    public static Features feature;
    private File configFile;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();

    public static File configDirectory;
    public static RepoManager repo;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        new BazaarApi();
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new ChatManager());
        MinecraftForge.EVENT_BUS.register(new HypixelData());
        MinecraftForge.EVENT_BUS.register(new DungeonData());
        MinecraftForge.EVENT_BUS.register(new ScoreboardData());
        MinecraftForge.EVENT_BUS.register(new ApiData());
        MinecraftForge.EVENT_BUS.register(new SeaCreatureManager());

        MinecraftForge.EVENT_BUS.register(new BazaarOrderHelper());
        MinecraftForge.EVENT_BUS.register(new ChatFilter());
        MinecraftForge.EVENT_BUS.register(new NewChatFilter());
        MinecraftForge.EVENT_BUS.register(new PlayerChatFilter());
        MinecraftForge.EVENT_BUS.register(new DungeonChatFilter());
        MinecraftForge.EVENT_BUS.register(new HideNotClickableItems());
        MinecraftForge.EVENT_BUS.register(new DungeonHighlightClickedBlocks());
        MinecraftForge.EVENT_BUS.register(new ItemDisplayOverlayFeatures());
        MinecraftForge.EVENT_BUS.register(new CurrentPetDisplay());
        MinecraftForge.EVENT_BUS.register(new ExpBottleOnGroundHider());
        MinecraftForge.EVENT_BUS.register(new DungeonBossDamageIndicator());
        MinecraftForge.EVENT_BUS.register(new ItemAbilityCooldown());
        MinecraftForge.EVENT_BUS.register(new DungeonMilestoneDisplay());
        MinecraftForge.EVENT_BUS.register(new DungeonDeathCounter());
        MinecraftForge.EVENT_BUS.register(new DungeonCleanEnd());
        MinecraftForge.EVENT_BUS.register(new DungeonBossMessages());
        MinecraftForge.EVENT_BUS.register(new TrophyFishMessages());
        MinecraftForge.EVENT_BUS.register(new BazaarBestSellMethod());
        MinecraftForge.EVENT_BUS.register(new AnvilCombineHelper());
        MinecraftForge.EVENT_BUS.register(new SeaCreatureMessageShortener());
//        MinecraftForge.EVENT_BUS.register(new GriffinBurrowFinder());
        MinecraftForge.EVENT_BUS.register(new AshfangFreezeCooldown());

        Commands.init();

        MinecraftForge.EVENT_BUS.register(new LorenzTest());
        MinecraftForge.EVENT_BUS.register(new ButtonOnPause());

        configDirectory = new File("config/skyhanni");
        try {
            //noinspection ResultOfMethodCallIgnored
            configDirectory.mkdir();
        } catch (Exception ignored) {}

        configFile = new File(configDirectory, "config.json");

        if (configFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8))) {
                feature = gson.fromJson(reader, Features.class);
            } catch (Exception ignored) {}
        }

        if (feature == null) {
            feature = new Features();
            saveConfig();
        }
        Runtime.getRuntime().addShutdownHook(new Thread(this::saveConfig));

        repo = new RepoManager(configDirectory);
        repo.loadRepoInformation();
    }

    public void saveConfig() {
        try {
            //noinspection ResultOfMethodCallIgnored
            configFile.createNewFile();

            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8))) {
                writer.write(gson.toJson(feature));
            }
        } catch (IOException ignored) {}
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
