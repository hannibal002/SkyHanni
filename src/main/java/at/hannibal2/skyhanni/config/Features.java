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
                Social.forLink("Join our Discord", DISCORD, "https://discord.gg/8DXVN4BJz3"),
                Social.forLink("Look at the code", GITHUB, "https://github.com/hannibal002/SkyHanni")
        );
    }

    @Override
    public String getTitle() {
        return "SkyHanni " + SkyHanniMod.getVersion() + " by §channibal2§r, config by §5Moulberry §rand §5nea89";
    }

    @Category(name = "About", desc = "Information about SkyHanni and updates")
    public About about = new About();

    @Expose
    @Category(name = "GUI Locations", desc = "Change the locations of GUI elements. (§e/sh gui§7)")
    public GUI gui = new GUI();

    @Expose
    @Category(name = "Chat", desc = "Change how the chat looks.")
    public Chat chat = new Chat();

    @Expose
    @Category(name = "Dungeon", desc = "Features that change the dungeon experience in catacombs.")
    public Dungeon dungeon = new Dungeon();

    @Expose
    @Category(name = "Inventory", desc = "Changing the behavior around items and the inventory.")
    public Inventory inventory = new Inventory();

    @Expose
    @Category(name = "Item Abilities", desc = "Stuff about item abilities.")
    public ItemAbilities itemAbilities = new ItemAbilities();

    @Expose
    @Category(name = "Summonings", desc = "Mobs you revive.")
    public Summonings summonings = new Summonings();

    @Expose
    @Category(name = "Ashfang", desc = "Ashfang fight in Crimson Isle.")
    public Ashfang ashfang = new Ashfang();

    @Expose
    @Category(name = "Minion", desc = "The minions at your private island.")
    public Minions minions = new Minions();

    @Expose
    @Category(name = "Bazaar", desc = "Bazaar settings.")
    public Bazaar bazaar = new Bazaar();

    @Expose
    @Category(name = "Fishing", desc = "Fishing stuff.")
    public Fishing fishing = new Fishing();

    @Expose
    @Category(name = "Damage Indicator", desc = "Better damage overview in combat with bosses of all sorts.")
    public DamageIndicator damageIndicator = new DamageIndicator();

    @Expose
    @Category(name = "Slayer", desc = "Slayer features.")
    public Slayer slayer = new Slayer();

    @Expose
    @Category(name = "Diana", desc = "Diana's mythological event.")
    public Diana diana = new Diana();

    @Expose
    @Category(name = "Commands", desc = "Enable or disable commands.")
    public CommandsFeatures commands = new CommandsFeatures();

    @Expose
    @Category(name = "Marked Players", desc = "Players that got marked with /shmarkplayer.")
    public MarkedPlayers markedPlayers = new MarkedPlayers();

    @Expose
    @Category(name = "Bingo", desc = "Features for the Bingo mode.")
    public Bingo bingo = new Bingo();

    @Expose
    @Category(name = "Mobs", desc = "Visual help for Mobs")
    public Mobs mobs = new Mobs();

    @Expose
    @Category(name = "Garden", desc = "Features on the Garden island.")
    public Garden garden = new Garden();

    @Expose
    @Category(name = "Misc", desc = "Settings without a category.")
    public Misc misc = new Misc();

    @Expose
    @Category(name = "Dev", desc = "Debug and test stuff. Developers are cool.")
    public DevData dev = new DevData();

    @Expose
    public Hidden hidden = new Hidden();
}
