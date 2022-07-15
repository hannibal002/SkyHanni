package at.hannibal2.skyhanni.sign;

import net.minecraft.util.IChatComponent;

public interface IModifiedSign {
    IChatComponent getText(int line);

    void setText(int line, IChatComponent component);

    void setSelectionState(int currentRow, int selectionStart, int selectionEnd, boolean caretVisible);

    void resetSelectionState();

    boolean getCaretVisible();

    int getSelectionStart();

    int getSelectionEnd();
}
