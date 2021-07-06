package com.thatgravyboat.skyblockhud.mixins;

import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.S3EPacketTeams;
import net.minecraft.scoreboard.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(NetHandlerPlayClient.class)
public class MixinNetHandlerPlayClient {

  /* DISABLE UNTIL NEW SYSTEM
    @Inject(method = "handleSetSlot", at = @At("HEAD"))
    public void onHandleSetSlot(S2FPacketSetSlot packetIn, CallbackInfo ci){
        if (SkyblockHud.hasSkyblockScoreboard()) {
            Minecraft mc = Minecraft.getMinecraft();
            PacketThreadUtil.checkThreadAndEnqueue(packetIn, mc.getNetHandler(), mc);
            if (packetIn.func_149175_c() == 0) {
                ItemStack stack = packetIn.func_149174_e();

                if (stack != null && stack.hasTagCompound()) {
                    if (stack.getTagCompound().hasKey("ExtraAttributes")) {
                        NBTTagCompound extraAttributes = stack.getTagCompound().getCompoundTag("ExtraAttributes");
                        String id = extraAttributes.getString("id");
                        ItemStack slotStack = Minecraft.getMinecraft().thePlayer.inventoryContainer.getSlot(packetIn.func_149173_d()).getStack();
                        int changeAmount = stack.stackSize - (slotStack == null ? 0 : slotStack.stackSize);
                        String eId = null;
                        int eLvl = -1;
                        if (extraAttributes.hasKey("enchantments")) {
                            NBTTagCompound enchantments = extraAttributes.getCompoundTag("enchantments");
                            if (enchantments.getKeySet().size() == 1) {
                                for (String e : enchantments.getKeySet()) { eId = e; break; }
                                if (eId != null) eLvl = enchantments.getInteger(eId);
                            }
                        }
                        TrackerHandler.onItemAdded(id, changeAmount, eId, eLvl);
                    }
                }
            }
        }
    }
     */

  @Inject(
    method = "handleTeams",
    locals = LocalCapture.CAPTURE_FAILHARD,
    at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/network/play/server/S3EPacketTeams;getAction()I",
      ordinal = 0,
      shift = At.Shift.BEFORE
    ),
    cancellable = true
  )
  public void handleTeams(
    S3EPacketTeams packetIn,
    CallbackInfo ci,
    Scoreboard scoreboard
  ) {
    //This stops Hypixel from being stupid and spamming our logs because they dont have different ids for things.
    if (
      scoreboard.getTeam(packetIn.getName()) != null &&
      packetIn.getAction() == 0
    ) ci.cancel();
  }
}
