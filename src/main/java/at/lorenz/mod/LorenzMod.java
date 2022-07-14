package at.lorenz.mod;

import at.lorenz.mod.bazaar.BazaarApi;
import at.lorenz.mod.bazaar.BazaarOrderHelper;
import at.lorenz.mod.chat.ChatFilter;
import at.lorenz.mod.chat.ChatManager;
import at.lorenz.mod.chat.PlayerChatFilter;
import at.lorenz.mod.config.Features;
import at.lorenz.mod.dungeon.*;
import at.lorenz.mod.dungeon.damageindicator.DungeonBossDamageIndicator;
import at.lorenz.mod.items.HideNotClickableItems;
import at.lorenz.mod.items.ItemDisplayOverlayFeatures;
import at.lorenz.mod.items.abilitycooldown.ItemAbilityCooldown;
import at.lorenz.mod.misc.*;
import at.lorenz.mod.test.LorenzTest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import at.lorenz.mod.config.commands.Commands;
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

@Mod(modid = LorenzMod.MODID, version = LorenzMod.VERSION)
public class LorenzMod {

    public static final String MODID = "lorenzmod";
    public static final String VERSION = "0.6";

    public static Features feature;
    private File configFile;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();

    public static File configDirectory;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        new BazaarApi();
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new ChatManager());
        MinecraftForge.EVENT_BUS.register(new HypixelData());
        MinecraftForge.EVENT_BUS.register(new DungeonData());
        MinecraftForge.EVENT_BUS.register(new ScoreboardData());

        MinecraftForge.EVENT_BUS.register(new BazaarOrderHelper());
        MinecraftForge.EVENT_BUS.register(new ChatFilter());
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

        Commands.init();

        MinecraftForge.EVENT_BUS.register(new LorenzTest());
        MinecraftForge.EVENT_BUS.register(new ButtonOnPause());

        configDirectory = new File("mods/LorenzMod/config");
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
