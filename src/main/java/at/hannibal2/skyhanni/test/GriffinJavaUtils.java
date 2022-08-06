package at.hannibal2.skyhanni.test;

import at.hannibal2.skyhanni.utils.LorenzVec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.function.Function;

public class GriffinJavaUtils {
    public static <T> void permute(ArrayList<ArrayList<T>> result, T[] a, int k) {
        if (k == a.length) {
            ArrayList<T> subResult = new ArrayList<>();
            result.add(subResult);
            Collections.addAll(subResult, a);
        } else {
            for (int i = k; i < a.length; i++) {
                T temp = a[k];
                a[k] = a[i];
                a[i] = temp;

                permute(result, a, k + 1);

                temp = a[k];
                a[k] = a[i];
                a[i] = temp;
            }
        }
    }

//    public static <T> ArrayList<T> sortLocationListC(LorenzVec start, Map<T, LorenzVec> map, boolean brokenMath) {
//        Map<T, Double> fastestWithout = new HashMap<>();
//        for (T without : map.keySet()) {
//
//            ArrayList<ArrayList<T>> variants = new ArrayList<>();
//            ArrayList<T> values = new ArrayList<>(map.keySet());
//
//            values.remove(without);
//            T[] array = (T[]) values.toArray();
//
//            permute(variants, array, 0);
//
//            LinkedHashMap<ArrayList<T>, Double> distances = new LinkedHashMap<>();
//
//            for (ArrayList<T> list : variants) {
//
//                double distance = 0;
//                LorenzVec last = start;
//
//                for (T t : list) {
//                    LorenzVec location = map.get(t);
//                    distance += last.distanceSq(location);
//                    last = location;
//                }
//
//                distances.put(list, distance);
//            }
//
//            Map<ArrayList<T>, Double> sort;
//            if (brokenMath) {
//                sort = sortByValue(distances);
//            } else {
//                sort = sortByValueAsc(distances);
//            }
//
//            double fastestDistance = sort.values().iterator().next();
//            fastestWithout.put(without, fastestDistance);
//        }
//
//        T skip = sortByValueAsc(fastestWithout).keySet().iterator().next();
//
//
//        map.remove(skip);
//        ArrayList<T> result = sortLocationListB(start, map, brokenMath, false, T -> false, 0);
//        result.add(skip);
//
//        return result;
//    }

    public static <T> ArrayList<LorenzVec> sortLocationListB(LorenzVec start, Map<T, LorenzVec> map, boolean brokenMath,
                                                             boolean skipWorst, Function<T, Boolean> shouldAddToHostile, int addToHostileLastValue) {

//        if (skipWorst) {
//            return sortLocationListC(start, map, brokenMath);
//        }
        ArrayList<ArrayList<T>> variants = new ArrayList<>();
        Set<T> values = map.keySet();
        T[] array = (T[]) values.toArray();

        permute(variants, array, 0);

        LinkedHashMap<ArrayList<T>, Double> distances = new LinkedHashMap<>();

        int with = 0;
        int without = 0;

        for (ArrayList<T> list : variants) {

            double distance = 0;
            LorenzVec last = start;
            T lastT = null;

            for (T t : list) {
                LorenzVec location = map.get(t);
                distance += last.distanceSq(location);
                last = location;
                lastT = t;
            }
            if (shouldAddToHostile.apply(lastT)) {
                distance += addToHostileLastValue;
                with++;
            } else {
                without++;
            }

            distances.put(list, distance);
        }
//        LorenzUtils.Companion.chat("with: " + with);
//        LorenzUtils.Companion.chat("without: " + without);

        Map<ArrayList<T>, Double> sort;
        if (brokenMath) {
            sort = sortByValue(distances);
        } else {
            sort = sortByValueAsc(distances);
        }
        ArrayList<T> result = sort.keySet().iterator().next();
        ArrayList<LorenzVec> resultList = new ArrayList<>();
        for (T t : result) {
            resultList.add(map.get(t));
        }

        return resultList;
    }

    //descending
    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Map.Entry.comparingByValue());
        Collections.reverse(list);

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    //ascending
    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValueAsc(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Map.Entry.comparingByValue());

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    public static String formatInteger(int i) {
        return new DecimalFormat("#,##0").format(i).replace(',', '.');
    }

    public static List<ItemStack> getItemsInInventory() {
        return getItemsInInventory(false);
    }

    public static List<ItemStack> getItemsInInventory(boolean withCursorItem) {
        List<ItemStack> list = new ArrayList<>();

//        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
        if (player == null) {
            System.err.println("loadCurrentInventory: player is null!");
            return list;
        }

        InventoryPlayer inventory = player.inventory;
        ItemStack[] mainInventory = inventory.mainInventory;

        ArrayList<ItemStack> helpList = new ArrayList<>();
        helpList.addAll(Arrays.asList(mainInventory));

        if (withCursorItem) {
            helpList.add(inventory.getItemStack());
        }

        for (ItemStack item : helpList) {
            if (item == null) continue;
            String name = item.getDisplayName();
            if (name.equalsIgnoreCase("air")) continue;
            if (name.equalsIgnoreCase("luft")) continue;
            list.add(item);
        }

        return list;
    }

    public static void drawWaypoint(LorenzVec pos, float partialTicks, Color color, boolean beacon) {
        drawWaypoint(pos, partialTicks, color, beacon, false);
    }

    public static void drawWaypoint(LorenzVec pos, float partialTicks, Color color, boolean beacon, boolean forceBeacon) {
        Entity viewer = Minecraft.getMinecraft().getRenderViewEntity();
        double viewerX = viewer.lastTickPosX + (viewer.posX - viewer.lastTickPosX) * partialTicks;
        double viewerY = viewer.lastTickPosY + (viewer.posY - viewer.lastTickPosY) * partialTicks;
        double viewerZ = viewer.lastTickPosZ + (viewer.posZ - viewer.lastTickPosZ) * partialTicks;

        double x = pos.getX() - viewerX;
        double y = pos.getY() - viewerY;
        double z = pos.getZ() - viewerZ;


        GlStateManager.disableDepth();
        GlStateManager.disableCull();
        GlStateManager.disableTexture2D();
        if (beacon) {
            double distSq = x * x + y * y + z * z;
            if (distSq > 5 * 5 || forceBeacon) {
                //TODO add beacon
//                GriffinUtils.renderBeaconBeam(x, y, z, color.getRGB(), 1.0f, partialTicks);
            }
        }
//        BlockPos a = pos.toBlocPos();
//        BlockPos b = pos.add(1, 1, 1).toBlocPos();
//        draw3DBox(new AxisAlignedBB(a, b), color, partialTicks);

        AxisAlignedBB aabb = new AxisAlignedBB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);

        draw3DBox(aabb, color, partialTicks);
        GlStateManager.disableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.enableDepth();

    }

    public static void draw3DLine(LorenzVec p1, LorenzVec p2, Color color, int lineWidth, boolean depth, float partialTicks) {
        GlStateManager.disableDepth();
        GlStateManager.disableCull();

//        Vec3 pos1 = new Vec3(p1.getX(), p1.getY(), p1.getZ());
//        Vec3 pos2 = new Vec3(p2.getX(), p2.getY(), p2.getZ());

        Entity render = Minecraft.getMinecraft().getRenderViewEntity();
        WorldRenderer worldRenderer = Tessellator.getInstance().getWorldRenderer();

        double realX = render.lastTickPosX + (render.posX - render.lastTickPosX) * partialTicks;
        double realY = render.lastTickPosY + (render.posY - render.lastTickPosY) * partialTicks;
        double realZ = render.lastTickPosZ + (render.posZ - render.lastTickPosZ) * partialTicks;

        GlStateManager.pushMatrix();
        GlStateManager.translate(-realX, -realY, -realZ);
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glLineWidth(lineWidth);


        if (!depth) {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GlStateManager.depthMask(false);
        }
        GlStateManager.color(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
        worldRenderer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION);

        worldRenderer.pos(p1.getX(), p1.getY(), p1.getZ()).endVertex();
        worldRenderer.pos(p2.getX(), p2.getY(), p2.getZ()).endVertex();
        Tessellator.getInstance().draw();

        GlStateManager.translate(realX, realY, realZ);
        if (!depth) {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GlStateManager.depthMask(true);
        }


        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();


        GlStateManager.disableLighting();
        GlStateManager.enableDepth();
    }

    public static void draw3DBox(AxisAlignedBB aabb, Color colour, float partialTicks) {
        Entity render = Minecraft.getMinecraft().getRenderViewEntity();
        WorldRenderer worldRenderer = Tessellator.getInstance().getWorldRenderer();

        double realX = render.lastTickPosX + (render.posX - render.lastTickPosX) * partialTicks;
        double realY = render.lastTickPosY + (render.posY - render.lastTickPosY) * partialTicks;
        double realZ = render.lastTickPosZ + (render.posZ - render.lastTickPosZ) * partialTicks;

        GlStateManager.pushMatrix();
        GlStateManager.translate(-realX, -realY, -realZ);
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glLineWidth(2);
        GlStateManager.color(colour.getRed() / 255f, colour.getGreen() / 255f, colour.getBlue() / 255f, colour.getAlpha() / 255f);
        worldRenderer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION);

        worldRenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex();
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.minZ).endVertex();
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.maxZ).endVertex();
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.maxZ).endVertex();
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex();
        Tessellator.getInstance().draw();
        worldRenderer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION);
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex();
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.minZ).endVertex();
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).endVertex();
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.maxZ).endVertex();
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex();
        Tessellator.getInstance().draw();
        worldRenderer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION);
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.minZ).endVertex();
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.minZ).endVertex();
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.minZ).endVertex();
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.minZ).endVertex();
        worldRenderer.pos(aabb.maxX, aabb.minY, aabb.maxZ).endVertex();
        worldRenderer.pos(aabb.maxX, aabb.maxY, aabb.maxZ).endVertex();
        worldRenderer.pos(aabb.minX, aabb.minY, aabb.maxZ).endVertex();
        worldRenderer.pos(aabb.minX, aabb.maxY, aabb.maxZ).endVertex();
        Tessellator.getInstance().draw();

        GlStateManager.translate(realX, realY, realZ);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }
}
