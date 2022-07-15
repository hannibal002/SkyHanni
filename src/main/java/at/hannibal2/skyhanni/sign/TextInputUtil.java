package at.hannibal2.skyhanni.sign;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class TextInputUtil {
    private final FontRenderer fontRenderer;
    private final Supplier<String> stringSupplier;
    private final Consumer<String> stringConsumer;
    private final int maxLength;
    private int selectionStart;
    private int selectionEnd;

    public TextInputUtil(FontRenderer fontRenderer, Supplier<String> stringSupplier, Consumer<String> stringConsumer, int maxLength) {
        this.fontRenderer = fontRenderer;
        this.stringSupplier = stringSupplier;
        this.stringConsumer = stringConsumer;
        this.maxLength = maxLength;
        this.moveCaretToEnd();
    }

    public boolean insert(char typedChar) {
        if (ChatAllowedCharacters.isAllowedCharacter(typedChar)) {
            this.insert(Character.toString(typedChar));
        }
        return true;
    }

    private void insert(String typedChar) {
        if (this.selectionEnd != this.selectionStart) {
            this.deleteSelectedText();
        }

        String s = this.stringSupplier.get();
        this.selectionStart = MathHelper.clamp_int(this.selectionStart, 0, s.length());
        String s1 = new StringBuilder(s).insert(this.selectionStart, typedChar).toString();

        if (this.fontRenderer.getStringWidth(s1) <= this.maxLength) {
            this.stringConsumer.accept(s1);
            this.selectionEnd = this.selectionStart = Math.min(s1.length(), this.selectionStart + typedChar.length());
        }
    }

    public boolean handleSpecialKey(int keyCode) {
        String s = this.stringSupplier.get();

        if (GuiScreen.isKeyComboCtrlA(keyCode)) {
            this.selectionEnd = 0;
            this.selectionStart = s.length();
            return true;
        } else if (GuiScreen.isKeyComboCtrlC(keyCode)) {
            GuiScreen.setClipboardString(this.getSelectedText());
            return true;
        } else if (GuiScreen.isKeyComboCtrlV(keyCode)) {
            this.insert(ChatAllowedCharacters.filterAllowedCharacters(EnumChatFormatting.getTextWithoutFormattingCodes(GuiScreen.getClipboardString().replaceAll("\\r", ""))));
            this.selectionEnd = this.selectionStart;
            return true;
        } else if (GuiScreen.isKeyComboCtrlX(keyCode)) {
            GuiScreen.setClipboardString(this.getSelectedText());
            this.deleteSelectedText();
            return true;
        } else {
            switch (keyCode) {
                case Keyboard.KEY_BACK:
                    if (!s.isEmpty()) {
                        if (this.selectionEnd != this.selectionStart) {
                            this.deleteSelectedText();
                        } else if (this.selectionStart > 0) {
                            s = new StringBuilder(s).deleteCharAt(Math.max(0, this.selectionStart - 1)).toString();
                            this.selectionEnd = this.selectionStart = Math.max(0, this.selectionStart - 1);
                            this.stringConsumer.accept(s);
                        }
                    }
                    return true;
                case Keyboard.KEY_DELETE:
                    if (!s.isEmpty()) {
                        if (this.selectionEnd != this.selectionStart) {
                            this.deleteSelectedText();
                        } else if (this.selectionStart < s.length()) {
                            s = new StringBuilder(s).deleteCharAt(Math.max(0, this.selectionStart)).toString();
                            this.stringConsumer.accept(s);
                        }
                    }
                    return true;
                case Keyboard.KEY_LEFT:
                    int j = this.fontRenderer.getBidiFlag() ? 1 : -1;
                    if (GuiScreen.isCtrlKeyDown()) {
                        this.selectionStart = this.findWordEdge(s, j, this.selectionStart);
                    } else {
                        this.selectionStart = Math.max(0, Math.min(s.length(), this.selectionStart + j));
                    }
                    if (!GuiScreen.isShiftKeyDown()) {
                        this.selectionEnd = this.selectionStart;
                    }
                    return true;
                case Keyboard.KEY_RIGHT:
                    int i = this.fontRenderer.getBidiFlag() ? -1 : 1;
                    if (GuiScreen.isCtrlKeyDown()) {
                        this.selectionStart = this.findWordEdge(s, i, this.selectionStart);
                    } else {
                        this.selectionStart = Math.max(0, Math.min(s.length(), this.selectionStart + i));
                    }
                    if (!GuiScreen.isShiftKeyDown()) {
                        this.selectionEnd = this.selectionStart;
                    }
                    return true;
                case Keyboard.KEY_HOME:
                    this.selectionStart = 0;
                    if (!GuiScreen.isShiftKeyDown()) {
                        this.selectionEnd = this.selectionStart;
                    }
                    return true;
                case Keyboard.KEY_END:
                    this.selectionStart = this.stringSupplier.get().length();
                    if (!GuiScreen.isShiftKeyDown()) {
                        this.selectionEnd = this.selectionStart;
                    }
                    return true;
                default:
                    return false;
            }
        }
    }

    private String getSelectedText() {
        String s = this.stringSupplier.get();
        int i = Math.min(this.selectionStart, this.selectionEnd);
        int j = Math.max(this.selectionStart, this.selectionEnd);
        return s.substring(i, j);
    }

    private void deleteSelectedText() {
        if (this.selectionEnd != this.selectionStart) {
            String s = this.stringSupplier.get();
            int i = Math.min(this.selectionStart, this.selectionEnd);
            int j = Math.max(this.selectionStart, this.selectionEnd);
            String s1 = s.substring(0, i) + s.substring(j);
            this.selectionStart = i;
            this.selectionEnd = this.selectionStart;
            this.stringConsumer.accept(s1);
        }
    }

    public void moveCaretToEnd() {
        this.selectionEnd = this.selectionStart = this.stringSupplier.get().length();
    }

    public int getSelectionStart() {
        return this.selectionStart;
    }

    public int getSelectionEnd() {
        return this.selectionEnd;
    }

    private int findWordEdge(String text, int bidiFlag, int selectionStart) {
        int i = selectionStart;
        boolean flag = bidiFlag < 0;
        int j = Math.abs(bidiFlag);

        for (int k = 0; k < j; ++k) {
            if (flag) {
                while (i > 0 && (text.charAt(i - 1) == ' ' || text.charAt(i - 1) == '\n')) {
                    --i;
                }
                while (i > 0 && text.charAt(i - 1) != ' ' && text.charAt(i - 1) != '\n') {
                    --i;
                }
            } else {
                int l = text.length();
                int i1 = text.indexOf(32, i);
                int j1 = text.indexOf(10, i);

                if (i1 == -1 && j1 == -1) {
                    i = -1;
                } else if (i1 != -1 && j1 != -1) {
                    i = Math.min(i1, j1);
                } else if (i1 != -1) {
                    i = i1;
                } else {
                    i = j1;
                }

                if (i == -1) {
                    i = l;
                } else {
                    while (i < l && (text.charAt(i) == ' ' || text.charAt(i) == '\n')) {
                        ++i;
                    }
                }
            }
        }
        return i;
    }
}