package at.hannibal2.skyhanni.config.features.inventory.stacksize;

import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StackSizeMenuConfig {

    @Expose
    @ConfigOption(
        name = "§aAb§9ip§5ho§6ne§ds§7",
        desc = "Showing various pieces of information as a stack size for these menu items.\nSome values may be truncated percentages or §a✔§r§7s.\n§c§lWARNING§r§c: If you have any respect for your FPS, please §l§odon't§r§c enable §l§oeverything§r§c at once."
    )
    @ConfigEditorDraggableList
    public List<Abiphone> abiphone = new ArrayList<>(Arrays.asList(
        Abiphone.CONTACTS_DIRECTORY,
        Abiphone.DO_NOT_DISTURB,
        Abiphone.RELAYS_COMPLETED,
        Abiphone.SELECTED_RINGTONE,
        Abiphone.NAVIGATION
    ));

    public enum Abiphone {
        CONTACTS_DIRECTORY("§bContacts Directory"), // (#)
        DO_NOT_DISTURB("§bDND Indicator"), // (§c§l✖§b)
        RELAYS_COMPLETED("§bRelays Finished"), //(#)
        SELECTED_RINGTONE("§bSelected Ringtone"), // (Abbv)
        TIC_TAC_TOE("§bTic Tac Toe Stats"), // (§aW§eT§cL§b)
        SNAKE("§bSnake Highest Score"), // (#)
        NAVIGATION("§bSorting/Filtering Abbreviations"),
        ;

        final String str;
        Abiphone(String str) { this.str = str; }
        @Override
        public String toString() { return str; }
    }
}
