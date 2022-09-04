package at.hannibal2.skyhanni;

import at.hannibal2.skyhanni.config.ConfigManager;
import at.hannibal2.skyhanni.config.Features;
import at.hannibal2.skyhanni.config.gui.commands.Commands;
import at.hannibal2.skyhanni.data.ApiKeyGrabber;
import at.hannibal2.skyhanni.data.HypixelData;
import at.hannibal2.skyhanni.data.ItemRenderBackground;
import at.hannibal2.skyhanni.data.ScoreboardData;
import at.hannibal2.skyhanni.data.repo.RepoManager;
import at.hannibal2.skyhanni.features.*;
import at.hannibal2.skyhanni.features.anvil.AnvilCombineHelper;
import at.hannibal2.skyhanni.features.bazaar.BazaarApi;
import at.hannibal2.skyhanni.features.bazaar.BazaarBestSellMethod;
import at.hannibal2.skyhanni.features.bazaar.BazaarOrderHelper;
import at.hannibal2.skyhanni.features.chat.ChatFilter;
import at.hannibal2.skyhanni.features.chat.ChatManager;
import at.hannibal2.skyhanni.features.chat.PlayerChatFilter;
import at.hannibal2.skyhanni.features.commands.WikiCommand;
import at.hannibal2.skyhanni.features.damageindicator.DamageIndicatorManager;
import at.hannibal2.skyhanni.features.dungeon.*;
import at.hannibal2.skyhanni.features.fishing.SeaCreatureManager;
import at.hannibal2.skyhanni.features.fishing.SeaCreatureMessageShortener;
import at.hannibal2.skyhanni.features.fishing.TrophyFishMessages;
import at.hannibal2.skyhanni.features.items.HideNotClickableItems;
import at.hannibal2.skyhanni.features.items.ItemDisplayOverlayFeatures;
import at.hannibal2.skyhanni.features.items.ItemStars;
import at.hannibal2.skyhanni.features.items.RngMeterInventory;
import at.hannibal2.skyhanni.features.items.abilitycooldown.ItemAbilityCooldown;
import at.hannibal2.skyhanni.features.minion.MinionFeatures;
import at.hannibal2.skyhanni.features.nether.ashfang.*;
import at.hannibal2.skyhanni.test.LorenzTest;
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
    public static final String VERSION = "0.6";

    public static Features feature;

    public static RepoManager repo;
    public static ConfigManager configManager;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        new BazaarApi();
        registerEvent(this);
        registerEvent(new ChatManager());
        registerEvent(new HypixelData());
        registerEvent(new DungeonData());
        registerEvent(new ScoreboardData());
        registerEvent(new ApiKeyGrabber());
        registerEvent(new SeaCreatureManager());
        registerEvent(new ItemRenderBackground());

        registerEvent(new BazaarOrderHelper());
        registerEvent(new ChatFilter());
        registerEvent(new PlayerChatFilter());
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

        Commands.init();

        registerEvent(new LorenzTest());
        registerEvent(new ButtonOnPause());

        configManager = new ConfigManager(this);
        configManager.firstLoad();

        Runtime.getRuntime().addShutdownHook(new Thread(configManager::saveConfig));

        repo = new RepoManager(configManager.getConfigDirectory());
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
