package at.hannibal2.skyhanni.config.features.combat.ghostcounter.textformatting;

import at.hannibal2.skyhanni.features.combat.ghostcounter.GhostFormatting;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorInfoText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class TextFormattingConfig {

    @ConfigOption(name = "§eText Formatting Info", desc = "§e%session% §7is §e§lalways §7replaced with the count for your current session.\n" +
        "§7It is reset when the game is restarted.\n" +
        "§7You can use §e&Z §7color code to use SBA chroma.")
    @ConfigEditorInfoText
    public boolean formatInfo = false;

    @ConfigOption(name = "Reset Formatting", desc = "Reset formatting to default text.")
    @ConfigEditorButton(buttonText = "Reset")
    public Runnable resetFormatting = GhostFormatting.INSTANCE::reset;

    @ConfigOption(name = "Export Formatting", desc = "Export current formatting to clipboard.")
    @ConfigEditorButton(buttonText = "Export")
    public Runnable exportFormatting = GhostFormatting.INSTANCE::export;

    @ConfigOption(name = "Import Formatting", desc = "Import formatting from clipboard.")
    @ConfigEditorButton(buttonText = "Import")
    public Runnable importFormatting = GhostFormatting.INSTANCE::importFormat;

    @Expose
    @ConfigOption(name = "Title", desc = "Title line.")
    @ConfigEditorText
    public String titleFormat = "&6Ghost Counter";

    @Expose
    @ConfigOption(name = "Ghosts killed", desc = "Ghosts killed line.\n" +
        "§e%value% §7is replaced with Ghosts killed.\n" +
        "§e%session% §7is replaced with Ghosts killed in this session.")
    @ConfigEditorText
    public String ghostKilledFormat = "  &6Ghosts killed: &b%value% &7(%session%)";

    @Expose
    @ConfigOption(name = "Sorrows dropped", desc = "Sorrows dropped line.\n" +
        "§e%value% §7is replaced with Sorrows dropped.")
    @ConfigEditorText
    public String sorrowsFormat = "  &6Sorrows: &b%value% &7(%session%)";

    @Expose
    @ConfigOption(name = "Ghosts since Sorrow", desc = "Ghosts since Sorrow line.\n" +
        "§e%value% §7is replaced with Ghosts since last Sorrow drop.")
    @ConfigEditorText
    public String ghostSinceSorrowFormat = "  &6Ghosts since Sorrow: &b%value%";

    @Expose
    @ConfigOption(name = "Ghost kills per Sorrow", desc = "Ghost kills per Sorrow line.\n" +
        "§e%value% §7is replaced with average Ghost kills per Sorrow drop.")
    @ConfigEditorText
    public String ghostKillPerSorrowFormat = "  &6Ghosts/Sorrow: &b%value%";

    @Expose
    @ConfigOption(name = "Voltas dropped", desc = "Voltas dropped line.\n" +
        "§e%value% §7is replaced with Voltas dropped.")
    @ConfigEditorText
    public String voltasFormat = "  &6Voltas: &b%value% &7(%session%)";

    @Expose
    @ConfigOption(name = "Plasmas dropped", desc = "Plasmas dropped line.\n" +
        "§e%value% §7is replaced with Plasmas dropped.")
    @ConfigEditorText
    public String plasmasFormat = "  &6Plasmas: &b%value% &7(%session%)";

    @Expose
    @ConfigOption(name = "Ghostly Boots", desc = "Ghostly Boots dropped line.\n" +
        "§e%value% §7is replaced with Ghostly Boots dropped.")
    @ConfigEditorText
    public String ghostlyBootsFormat = "  &6Ghostly Boots: &b%value% &7(%session%)";

    @Expose
    @ConfigOption(name = "Bag Of Cash", desc = "Bag Of Cash dropped line.\n" +
        "§e%value% §7is replaced with Bag Of Cash dropped.")
    @ConfigEditorText
    public String bagOfCashFormat = "  &6Bag Of Cash: &b%value% &7(%session%)";

    @Expose
    @ConfigOption(name = "Average Magic Find", desc = "Average Magic Find line.\n" +
        "§e%value% §7is replaced with Average Magic Find.")
    @ConfigEditorText
    public String avgMagicFindFormat = "  &6Avg Magic Find: &b%value%";

    @Expose
    @ConfigOption(name = "Scavenger Coins", desc = "Scavenger Coins line.\n" +
        "§e%value% §7is replaced with Coins earned from kill ghosts.\n" +
        "Includes: Scavenger Enchant, Scavenger Talismans, Kill Combo")
    @ConfigEditorText
    public String scavengerCoinsFormat = "  &6Scavenger Coins: &b%value% &7(%session%)";

    @Expose
    @ConfigOption(name = "Kill Combo", desc = "Kill Combo line.\n" +
        "§e%value% §7is replaced with your current kill combo.")
    @ConfigEditorText
    public String killComboFormat = "  &6Kill Combo: &b%value%";

    @Expose
    @ConfigOption(name = "Highest Kill Combo", desc = "Highest Kill Combo line.\n" +
        "§e%value% §7is replaced with your highest kill combo.")
    @ConfigEditorText
    public String highestKillComboFormat = "  &6Highest Kill Combo: &b%value% &7(%session%)";

    @Expose
    @ConfigOption(name = "Skill XP Gained", desc = "Skill XP Gained line.\n" +
        "§e%value% §7is replaced with Skill XP Gained from killing Ghosts.")
    @ConfigEditorText
    public String skillXPGainFormat = "  &6Skill XP Gained: &b%value% &7(%session%)";

    @ConfigOption(name = "Bestiary Formatting", desc = "")
    @Accordion
    @Expose
    public BestiaryFormattingConfig bestiaryFormatting = new BestiaryFormattingConfig();

    @ConfigOption(name = "XP per Hour Formatting", desc = "")
    @Accordion
    @Expose
    public XPHourFormattingConfig xpHourFormatting = new XPHourFormattingConfig();

    @ConfigOption(name = "ETA Formatting", desc = "")
    @Accordion
    @Expose
    public ETAFormattingConfig etaFormatting = new ETAFormattingConfig();

    @ConfigOption(name = "Kills per Hour Formatting", desc = "")
    @Expose
    @Accordion
    public KillHourFormattingConfig killHourFormatting = new KillHourFormattingConfig();

    @Expose
    @ConfigOption(name = "Money per Hour", desc = "Money per Hour.\n" +
        "§e%value% §7is replaced with estimated money earned per hour.\n" +
        "Calculated with your kills per hour and your average magic find.")
    @ConfigEditorText
    public String moneyHourFormat = "  &6$/h: &b%value%";

    @Expose
    @ConfigOption(name = "Money made", desc = "Calculate the money you made.\n" +
        "Includes §eSorrow§7, §ePlasma§7, §eVolta§7, §e1m coins drops§7, §eGhostly Boots§7, §eScavenger coins§7.\n" +
        "§eUses current Sell Offer value.")
    @ConfigEditorText
    public String moneyMadeFormat = "  &6Money made: &b%value%";
}
