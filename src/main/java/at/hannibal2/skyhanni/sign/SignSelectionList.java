package at.hannibal2.skyhanni.sign;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiListExtended;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.network.play.client.C12PacketUpdateSign;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class SignSelectionList extends GuiListExtended {

    public static final List<Entry> AUCTION_STARTING_BID_PRICES = new ArrayList<>();
    public static final List<Entry> AUCTION_BID_PRICES = new ArrayList<>();
    public static final List<Entry> AUCTION_QUERIES = new ArrayList<>();
    public static final List<Entry> BANK_WITHDRAW = new ArrayList<>();
    public static final List<Entry> BANK_DEPOSIT = new ArrayList<>();
    public static final List<Entry> BAZAAR_ORDER = new ArrayList<>();
    public static final List<Entry> BAZAAR_PRICE = new ArrayList<>();
    private int selectedSlotIndex = -1;
    private final List<SignSelectionList.Entry> list;
    private final String title;

    public SignSelectionList(Minecraft mc, int width, int height, int top, int bottom, List<SignSelectionList.Entry> list, String title) {
        super(mc, width, height, top, bottom, 16);
        this.list = list;
        this.title = title;

        if (this.getSize() > 5) {
            this.list.remove(0);
        }
        Collections.reverse(this.list);
    }

    @Override
    protected void elementClicked(int slotIndex, boolean isDoubleClick, int mouseX, int mouseY) {
        this.selectedSlotIndex = slotIndex;
    }

    @Override
    public IGuiListEntry getListEntry(int index) {
        return this.list.stream().distinct().collect(Collectors.toList()).get(index);
    }

    @Override
    protected int getSize() {
        return this.list.stream().distinct().collect(Collectors.toList()).size();
    }

    @Override
    protected boolean isSelected(int index) {
        return index == this.selectedSlotIndex;
    }

    @Override
    protected void drawContainerBackground(Tessellator tessellator) {}

    @Override
    protected void overlayBackground(int startY, int endY, int startAlpha, int endAlpha) {}

    @Override
    public int getListWidth() {
        return 100;
    }

    @Override
    public int getSlotHeight() {
        return 10;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (this.field_178041_q) {
            this.mouseX = mouseX;
            this.mouseY = mouseY;
            this.drawBackground();
            this.bindAmountScrolled();
            GlStateManager.disableLighting();
            GlStateManager.disableFog();
            Tessellator tessellator = Tessellator.getInstance();
            this.drawContainerBackground(tessellator);
            int k = this.left + this.width / 2 - this.getListWidth() / 2 + 2;
            int l = this.top + 4 - (int) this.amountScrolled;

            if (this.hasListHeader) {
                this.drawListHeader(k, l, tessellator);
            }

            this.drawSelectionBox(k, l, mouseX, mouseY);
            this.mc.fontRendererObj.drawString(this.title + ":", k, l - 12, 16777215);
            GlStateManager.disableDepth();
            this.overlayBackground(0, this.top, 255, 255);
            this.overlayBackground(this.bottom, this.height, 255, 255);
            this.func_148142_b(mouseX, mouseY);
        }
        GlStateManager.enableDepth();
    }

    public void add(String value) {
        this.list.add(new Entry(value));
    }

    public static void clearAll() {
        SignSelectionList.AUCTION_STARTING_BID_PRICES.clear();
        SignSelectionList.AUCTION_BID_PRICES.clear();
        SignSelectionList.AUCTION_QUERIES.clear();
        SignSelectionList.BANK_WITHDRAW.clear();
        SignSelectionList.BANK_DEPOSIT.clear();
        SignSelectionList.BAZAAR_ORDER.clear();
        SignSelectionList.BAZAAR_PRICE.clear();
    }

    public static class Entry implements GuiListExtended.IGuiListEntry {

        private final Minecraft mc;
        private final String value;
        private long lastClicked;

        public Entry(String value) {
            this.mc = Minecraft.getMinecraft();
            this.value = value;
        }

        @Override
        public void setSelected(int p_178011_1_, int p_178011_2_, int p_178011_3_) {}

        @Override
        public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected) {
            this.mc.fontRendererObj.drawString(this.value, x + 2, y + 2, 16777215);
        }

        @Override
        public boolean mousePressed(int slotIndex, int mouseX, int mouseY, int mouseEvent, int relativeX, int relativeY) {
            //            TileEntitySign sign = ((GuiEditSign)this.mc.currentScreen).tileSign;
            //            TileEntitySign sign = ().tileSign;
            TileEntitySign sign = LorenzSignUtils.getTileSign((GuiEditSign) this.mc.currentScreen);
            sign.markDirty();

            if (Minecraft.getSystemTime() - this.lastClicked < 250L) {
                //                if (SkyBlockcatiaSettings.INSTANCE.auctionBidConfirm && NumberUtils.isNumeric(this.value))
                //                {
                //                    int price = Integer.parseInt(this.value);
                //
                //                    if (price >= SkyBlockcatiaSettings.INSTANCE.auctionBidConfirmValue)
                //                    {
                //                        this.mc.displayGuiScreen(new GuiYesNo(this.mc.currentScreen, LangUtils.translate("message.bid_confirm_title"), LangUtils.translate("message.bid_confirm"), 201));
                //                    }
                //                    else
                //                    {
                //                        SignSelectionList.processSignData(sign);
                //                        this.mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
                //                        this.mc.displayGuiScreen(null);
                //                    }
                //                }
                //                else
                //                {
                SignSelectionList.processSignData(sign);
                this.mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
                this.mc.displayGuiScreen(null);
                //                }
            }

            sign.signText[0] = new ChatComponentText(this.value);

            if (this.mc.currentScreen instanceof IEditSign) {
                ((IEditSign) this.mc.currentScreen).getTextInputUtil().moveCaretToEnd();
            }
            this.lastClicked = Minecraft.getSystemTime();
            return false;
        }

        @Override
        public void mouseReleased(int slotIndex, int x, int y, int mouseEvent, int relativeX, int relativeY) {}

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Entry)) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            Entry other = (Entry) obj;
            return new EqualsBuilder().append(this.value, other.value).isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder().append(this.value).toHashCode();
        }

        public String getValue() {
            return this.value;
        }
    }

    public static void processSignData(TileEntitySign sign) {
        NetHandlerPlayClient nethandlerplayclient = Minecraft.getMinecraft().getNetHandler();

        if (nethandlerplayclient != null) {
            nethandlerplayclient.addToSendQueue(new C12PacketUpdateSign(sign.getPos(), sign.signText));
        }
        sign.setEditable(true);
    }
}
