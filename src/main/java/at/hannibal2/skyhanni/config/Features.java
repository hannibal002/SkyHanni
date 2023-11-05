package at.hannibal2.skyhanni.config;

import at.hannibal2.skyhanni.SkyHanniMod;
import at.hannibal2.skyhanni.config.features.About;
import at.hannibal2.skyhanni.config.features.BazaarConfig;
import at.hannibal2.skyhanni.config.features.ChatConfig;
import at.hannibal2.skyhanni.config.features.ChromaConfig;
import at.hannibal2.skyhanni.config.features.CombatConfig;
import at.hannibal2.skyhanni.config.features.CommandsConfig;
import at.hannibal2.skyhanni.config.features.CrimsonIsleConfig;
import at.hannibal2.skyhanni.config.features.DevConfig;
import at.hannibal2.skyhanni.config.features.DungeonConfig;
import at.hannibal2.skyhanni.config.features.EventConfig;
import at.hannibal2.skyhanni.config.features.FishingConfig;
import at.hannibal2.skyhanni.config.features.GUIConfig;
import at.hannibal2.skyhanni.config.features.GardenConfig;
import at.hannibal2.skyhanni.config.features.InventoryConfig;
import at.hannibal2.skyhanni.config.features.ItemAbilityConfig;
import at.hannibal2.skyhanni.config.features.MarkedPlayerConfig;
import at.hannibal2.skyhanni.config.features.MiningConfig;
import at.hannibal2.skyhanni.config.features.MinionsConfig;
import at.hannibal2.skyhanni.config.features.MiscConfig;
import at.hannibal2.skyhanni.config.features.RiftConfig;
import at.hannibal2.skyhanni.config.features.SlayerConfig;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.Config;
import io.github.moulberry.moulconfig.Social;
import io.github.moulberry.moulconfig.annotations.Category;
import net.minecraft.util.ResourceLocation;

import java.util.Arrays;
import java.util.List;


public class Features extends Config {
    public static final ResourceLocation DISCORD = new ResourceLocation("notenoughupdates:social/discord.png");
    public static final ResourceLocation GITHUB = new ResourceLocation("notenoughupdates:social/github.png");

    @Override
    public boolean shouldAutoFocusSearchbar() {
        return true;
    }

    @Override
    public List<Social> getSocials() {
        return Arrays.asList(
                Social.forLink("Join our Discord", DISCORD, "https://discord.com/invite/skyhanni-997079228510117908"),
                Social.forLink("Look at the code", GITHUB, "https://github.com/hannibal002/SkyHanni")
        );
    }

    @Override
    public void saveNow() {
        SkyHanniMod.configManager.saveConfig("close-gui");
    }

    @Override
    public String getTitle() {
        return "SkyHanni " + SkyHanniMod.getVersion() + " by §channibal2§r, config by §5Moulberry §rand §5nea89";
    }

    @Expose
    @Category(name = "About", desc = "Information about SkyHanni and updates")
    public About about = new About();

    @Expose
    @Category(name = "GUI", desc = "Change the locations of GUI elements. (§e/sh gui§7)")
    public GUIConfig gui = new GUIConfig();

    @Expose
    @Category(name = "Chroma", desc = "Settings for Chroma text. (Credit to SBA)")
    public ChromaConfig chroma = new ChromaConfig();

    @Expose
    @Category(name = "Chat", desc = "Change how the chat looks.")
    public ChatConfig chat = new ChatConfig();

    @Expose
    @Category(name = "Dungeon", desc = "Features that change the Dungeons experience in The Catacombs.")
    public DungeonConfig dungeon = new DungeonConfig();

    @Expose
    @Category(name = "Inventory", desc = "Change the behavior of items and the inventory.")
    public InventoryConfig inventory = new InventoryConfig();

    @Expose
    @Category(name = "Item Abilities", desc = "Stuff about item abilities.")
    public ItemAbilityConfig itemAbilities = new ItemAbilityConfig();

    @Expose
    @Category(name = "Crimson Isle", desc = "Things to do on the Crimson Isle/Nether island.")
    public CrimsonIsleConfig crimsonIsle = new CrimsonIsleConfig();

    @Expose
    @Category(name = "Minion", desc = "The minions on your private island.")
    public MinionsConfig minions = new MinionsConfig();

    @Expose
    @Category(name = "Bazaar", desc = "Bazaar settings.")
    public BazaarConfig bazaar = new BazaarConfig();

    @Expose
    @Category(name = "Fishing", desc = "Fishing stuff.")
    public FishingConfig fishing = new FishingConfig();

    @Expose
    @Category(name = "Combat", desc = "Everything combat and PvE related.")
    public CombatConfig combat = new CombatConfig();

    @Expose
    @Category(name = "Slayer", desc = "Slayer features.")
    public SlayerConfig slayer = new SlayerConfig();

    @Expose
    @Category(name = "Mining", desc = "Features that help you break blocks.")
    public MiningConfig mining = new MiningConfig();

    @Expose
    @Category(name = "Commands", desc = "Enable or disable commands.")
    public CommandsConfig commands = new CommandsConfig();

    @Expose
    @Category(name = "Marked Players", desc = "Players that got marked with §e/shmarkplayer§7.")
    public MarkedPlayerConfig markedPlayers = new MarkedPlayerConfig();

    @Expose
    @Category(name = "Events", desc = "Stuff that is not always available.")
    public EventConfig event = new EventConfig();

    @Expose
    @Category(name = "Garden", desc = "Features for the Garden island.")
    public GardenConfig garden = new GardenConfig();

    @Expose
    @Category(name = "The Rift", desc = "Features for The Rift dimension.")
    public RiftConfig rift = new RiftConfig();

    @Expose
    @Category(name = "Misc", desc = "Settings without a category.")
    public MiscConfig misc = new MiscConfig();

    @Expose
    @Category(name = "Dev", desc = "Debug and test stuff. Developers are cool.")
    public DevConfig dev = new DevConfig();

    @Expose
    public Storage storage = new Storage();

    @Expose
    public int lastVersion = ConfigUpdaterMigrator.INSTANCE.getConfigVersion();

}
