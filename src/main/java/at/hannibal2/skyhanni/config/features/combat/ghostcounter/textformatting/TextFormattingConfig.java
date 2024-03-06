package at.hannibal2.skyhanni.config.features.combat.ghostcounter.textformatting;

import at.hannibal2.skyhanni.features.combat.ghostcounter.GhostFormatting;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorButton;
import io.github.moulberry.moulconfig.annotations.ConfigEditorInfoText;
import io.github.moulberry.moulconfig.annotations.ConfigEditorText;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class TextFormattingConfig {

    @ConfigOption(name = "§eText Formatting Info", desc = "§e%session% §ris §e§lalways §rreplaced with\n" +
        "§7the count for your current session.\n" +
        "§7Reset when restarting the game.\n" +
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
    @ConfigOption(name = "Title", desc = "Title Line.")
    @ConfigEditorText
    public String titleFormat = "&6Ghost Counter";

    @Expose
    @ConfigOption(name = "Ghosts Killed", desc = "Ghosts Killed line.\n§e%value% §ris replaced with\n" +
        "Ghosts Killed.\n" +
        "§e%session% §7is replaced with Ghosts killed")
    @ConfigEditorText
    public String ghostKilledFormat = "  &6Ghosts Killed: &b%value% &7(%session%)";

    @Expose
    @ConfigOption(name = "Sorrows", desc = "Sorrows drop line.\n" +
        "§e%value% §7is replaced with\nsorrows dropped.")
    @ConfigEditorText
    public String sorrowsFormat = "  &6Sorrow: &b%value% &7(%session%)";

    @Expose
    @ConfigOption(name = "Ghost Since Sorrow", desc = "Ghost Since Sorrow line.\n" +
        "§e%value% §7is replaced with\nGhost since last sorrow drop.")
    @ConfigEditorText
    public String ghostSinceSorrowFormat = "  &6Ghost since Sorrow: &b%value%";

    @Expose
    @ConfigOption(name = "Ghost Kill Per Sorrow", desc = "Ghost Kill Per Sorrow line.\n" +
        "§e%value% §7is replaced with\naverage ghost kill per sorrow drop.")
    @ConfigEditorText
    public String ghostKillPerSorrowFormat = "  &6Ghosts/Sorrow: &b%value%";

    @Expose
    @ConfigOption(name = "Voltas", desc = "Voltas drop line.\n" +
        "§e%value% §7is replaced with\nvoltas dropped.")
    @ConfigEditorText
    public String voltasFormat = "  &6Voltas: &b%value% &7(%session%)";

    @Expose
    @ConfigOption(name = "Plasmas", desc = "Plasmas drop line.\n" +
        "§e%value% §7is replaced with\nplasmas dropped.")
    @ConfigEditorText
    public String plasmasFormat = "  &6Plasmas: &b%value% &7(%session%)";

    @Expose
    @ConfigOption(name = "Ghostly Boots", desc = "Ghostly Boots drop line.\n" +
        "§e%value% §7is replaced with\nGhostly Boots dropped.")
    @ConfigEditorText
    public String ghostlyBootsFormat = "  &6Ghostly Boots: &b%value% &7(%session%)";

    @Expose
    @ConfigOption(name = "Bag Of Cash", desc = "Bag Of Cash drop line.\n" +
        "§e%value% §7is replaced with\nBag Of Cash dropped.")
    @ConfigEditorText
    public String bagOfCashFormat = "  &6Bag Of Cash: &b%value% &7(%session%)";

    @Expose
    @ConfigOption(name = "Average Magic Find", desc = "Average Magic Find line.\n" +
        "§e%value% §7is replaced with\nAverage Magic Find.")
    @ConfigEditorText
    public String avgMagicFindFormat = "  &6Avg Magic Find: &b%value%";

    @Expose
    @ConfigOption(name = "Scavenger Coins", desc = "Scavenger Coins line.\n" +
        "§e%value% §7is replaced with\nCoins earned from kill ghosts.\nInclude: Scavenger Enchant, Scavenger Talismans, Kill Combo.")
    @ConfigEditorText
    public String scavengerCoinsFormat = "  &6Scavenger Coins: &b%value% &7(%session%)";

    @Expose
    @ConfigOption(name = "Kill Combo", desc = "Kill Combo line.\n" +
        "§e%value% §7is replaced with\nYour current kill combo.")
    @ConfigEditorText
    public String killComboFormat = "  &6Kill Combo: &b%value%";

    @Expose
    @ConfigOption(name = "Highest Kill Combo", desc = "Highest Kill Combo line.\n" +
        "§e%value% §7is replaced with\nYour current highest kill combo.")
    @ConfigEditorText
    public String highestKillComboFormat = "  &6Highest Kill Combo: &b%value% &7(%session%)";

    @Expose
    @ConfigOption(name = "Skill XP Gained", desc = "Skill XP Gained line.\n" +
        "§e%value% §7is replaced with\nSkill XP Gained from killing Ghosts.")
    @ConfigEditorText
    public String skillXPGainFormat = "  &6Skill XP Gained: &b%value% &7(%session%)";

    @ConfigOption(name = "Bestiary Formatting", desc = "")
    @Accordion
    @Expose
    public BestiaryFormattingConfig bestiaryFormatting = new BestiaryFormattingConfig();

    @ConfigOption(name = "XP Per Hour Formatting", desc = "")
    @Accordion
    @Expose
    public XPHourFormattingConfig xpHourFormatting = new XPHourFormattingConfig();

    @ConfigOption(name = "ETA Formatting", desc = "")
    @Accordion
    @Expose
    public ETAFormattingConfig etaFormatting = new ETAFormattingConfig();

    @ConfigOption(name = "Kill Per Hour Formatting", desc = "")
    @Expose
    @Accordion
    public KillHourFormattingConfig killHourFormatting = new KillHourFormattingConfig();

    @Expose
    @ConfigOption(name = "Money Per Hour", desc = "Money Per Hour.\n§e%value% §7is replaced with\nEstimated money you get per hour\n" +
        "Calculated with your kill per hour and your average magic find.")
    @ConfigEditorText
    public String moneyHourFormat = "  &6$/h: &b%value%";

    @Expose
    @ConfigOption(name = "Money made", desc = "Calculate the money you made.\nInclude §eSorrow§7, §ePlasma§7, §eVolta§7, §e1M coins drop\n" +
        "§eGhostly Boots§7, §eScavenger coins.\n" +
        "§cUsing current Sell Offer value.")
    @ConfigEditorText
    public String moneyMadeFormat = "  &6Money made: &b%value%";
}
