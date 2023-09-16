package at.hannibal2.skyhanni.config;

import at.hannibal2.skyhanni.SkyHanniMod;
import at.hannibal2.skyhanni.config.features.*;
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
    @Category(name = "GUI Locations", desc = "Change the locations of GUI elements. (§e/sh gui§7)")
    public GUIConfig gui = new GUIConfig();

    @Expose
    @Category(name = "Chat", desc = "Change how the chat looks.")
    public ChatConfig chat = new ChatConfig();

    @Expose
    @Category(name = "Dungeon", desc = "Features that change the dungeon experience in catacombs.")
    public DungeonConfig dungeon = new DungeonConfig();

    @Expose
    @Category(name = "Inventory", desc = "Changing the behavior around items and the inventory.")
    public InventoryConfig inventory = new InventoryConfig();

    @Expose
    @Category(name = "Item Abilities", desc = "Stuff about item abilities.")
    public ItemAbilityConfig itemAbilities = new ItemAbilityConfig();

    @Expose
    @Category(name = "Summonings", desc = "Mobs you revive.")
    public SummoningsConfig summonings = new SummoningsConfig();

    @Expose
    @Category(name = "Ashfang", desc = "Ashfang fight in Crimson Isle.")
    public AshfangConfig ashfang = new AshfangConfig();

    @Expose
    @Category(name = "Minion", desc = "The minions at your private island.")
    public MinionsConfig minions = new MinionsConfig();

    @Expose
    @Category(name = "Bazaar", desc = "Bazaar settings.")
    public BazaarConfig bazaar = new BazaarConfig();

    @Expose
    @Category(name = "Fishing", desc = "Fishing stuff.")
    public FishingConfig fishing = new FishingConfig();

    @Expose
    @Category(name = "Damage Indicator", desc = "Better damage overview in combat with bosses of all sorts.")
    public DamageIndicatorConfig damageIndicator = new DamageIndicatorConfig();

    @Expose
    @Category(name = "Slayer", desc = "Slayer features.")
    public SlayerConfig slayer = new SlayerConfig();

    @Expose
    @Category(name = "Diana", desc = "Diana's mythological event.")
    public DianaConfig diana = new DianaConfig();

    @Expose
    @Category(name = "Commands", desc = "Enable or disable commands.")
    public CommandsConfig commands = new CommandsConfig();

    @Expose
    @Category(name = "Marked Players", desc = "Players that got marked with /shmarkplayer.")
    public MarkedPlayerConfig markedPlayers = new MarkedPlayerConfig();

    @Expose
    @Category(name = "Bingo", desc = "Features for the Bingo mode.")
    public BingoConfig bingo = new BingoConfig();

    @Expose
    @Category(name = "Mobs", desc = "Visual help for Mobs")
    public MobsConfig mobs = new MobsConfig();

    @Expose
    @Category(name = "Garden", desc = "Features on the Garden island.")
    public GardenConfig garden = new GardenConfig();

    @Expose
    @Category(name = "The Rift", desc = "Features for The Rift dimension.")
    public RiftConfig rift = new RiftConfig();

    @Expose
    @Category(name = "Ghost Counter", desc = "Ghost Counter settings.")
    public GhostCounterConfig ghostCounter = new GhostCounterConfig();

    @Expose
    @Category(name = "Misc", desc = "Settings without a category.")
    public MiscConfig misc = new MiscConfig();

    @Expose
    @Category(name = "Dev", desc = "Debug and test stuff. Developers are cool.")
    public DevConfig dev = new DevConfig();

    @Expose
    public OldHidden hidden = new OldHidden();

    @Expose
    public Storage storage = new Storage();
}
