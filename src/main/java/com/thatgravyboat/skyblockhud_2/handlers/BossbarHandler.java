//package com.thatgravyboat.skyblockhud.handlers;
//
//import at.lorenz.mod.LorenzMod;
//import com.thatgravyboat.skyblockhud.location.LocationHandler;
//import com.thatgravyboat.skyblockhud.location.Locations;
//import com.thatgravyboat.skyblockhud.utils.Utils;
//import net.minecraft.entity.boss.BossStatus;
//import net.minecraftforge.client.event.RenderGameOverlayEvent;
//import net.minecraftforge.fml.common.eventhandler.EventPriority;
//import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
//
//public class BossbarHandler {
//
//    public static boolean bossBarRendered = true;
//
//    @SubscribeEvent(priority = EventPriority.LOWEST)
//    public void onBossbarRender(RenderGameOverlayEvent.Pre event) {
//        if (event.type == RenderGameOverlayEvent.ElementType.BOSSHEALTH && BossStatus.bossName != null) {
//            bossBarRendered = !event.isCanceled();
//            if (!LorenzMod.config.main.bossShiftHud) {
//                bossBarRendered = false;
//            }
//            String bossName = Utils.removeColor(BossStatus.bossName);
//            if (LorenzMod.config.renderer.hideBossBar && !LocationHandler.getCurrentLocation().equals(Locations.CATACOMBS)) {
//                if (bossName.equalsIgnoreCase("wither")) {
//                    event.setCanceled(true);
//                    bossBarRendered = false;
//                }
//                if (bossName.toLowerCase().startsWith("objective:")) {
//                    event.setCanceled(true);
//                    bossBarRendered = false;
//                }
//            }
//        }
//    }
//}
