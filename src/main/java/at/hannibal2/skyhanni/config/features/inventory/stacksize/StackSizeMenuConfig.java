package at.hannibal2.skyhanni.config.features.inventory.stacksize;

import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDraggableList;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StackSizeMenuConfig {

    public enum Bingo {
        SECRET_BINGO_DISCOVERY("§bSecret Bingo Goal Discovery (#, caps at 10k)"),
        SECRET_BINGO_HINT_COUNTDOWN(""),
        ROW_COLUMN_DIAGONAL_PROGRESS("§bRow/Diagonal/Column Progress"),
        TOP_BLANK_PERCENT_COMMUNITY_GOAL_CONTRIB("§bCommunity Goals Top Percent Contribution (#)"),
        ;

        final String str;

        Bingo(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }
}
