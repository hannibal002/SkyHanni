package at.hannibal2.skyhanni.mixins.transformers.tileentity;

import at.hannibal2.skyhanni.sign.IModifiedSign;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TileEntitySign.class)
public class TileEntitySignMixin implements IModifiedSign {

    private final TileEntitySign that = (TileEntitySign) (Object) this;
    private int selectionStart = -1;
    private int selectionEnd = -1;
    private boolean caretVisible;

    @Override
    public IChatComponent getText(int line) {
        return this.that.signText[line];
    }

    @Override
    public void setText(int line, IChatComponent component) {
        this.that.signText[line] = component;
    }

    @Override
    public void setSelectionState(int currentRow, int selectionStart, int selectionEnd, boolean caretVisible) {
        this.that.lineBeingEdited = currentRow;
        this.selectionStart = selectionStart;
        this.selectionEnd = selectionEnd;
        this.caretVisible = caretVisible;
    }

    @Override
    public void resetSelectionState() {
        this.that.lineBeingEdited = -1;
        this.selectionStart = -1;
        this.selectionEnd = -1;
        this.caretVisible = false;
    }

    @Override
    public boolean getCaretVisible() {
        return this.caretVisible;
    }

    @Override
    public int getSelectionStart() {
        return this.selectionStart;
    }

    @Override
    public int getSelectionEnd() {
        return this.selectionEnd;
    }
}
