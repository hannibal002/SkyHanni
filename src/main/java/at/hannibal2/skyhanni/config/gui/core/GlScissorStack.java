package at.hannibal2.skyhanni.config.gui.core;

import java.util.LinkedList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;

public class GlScissorStack {

    private static class Bounds {

        int left;
        int top;
        int right;
        int bottom;

        public Bounds(int left, int top, int right, int bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }

        public Bounds createSubBound(int left, int top, int right, int bottom) {
            left = Math.max(left, this.left);
            top = Math.max(top, this.top);
            right = Math.min(right, this.right);
            bottom = Math.min(bottom, this.bottom);

            if (top > bottom) {
                top = bottom;
            }
            if (left > right) {
                left = right;
            }

            return new Bounds(left, top, right, bottom);
        }

        public void set(ScaledResolution scaledResolution) {
            int height = Minecraft.getMinecraft().displayHeight;
            int scale = scaledResolution.getScaleFactor();
            GL11.glScissor(left * scale, height - bottom * scale, (right - left) * scale, (bottom - top) * scale);
        }
    }

    private static final LinkedList<Bounds> boundsStack = new LinkedList<>();

    public static void push(int left, int top, int right, int bottom, ScaledResolution scaledResolution) {
        if (right < left) {
            int temp = right;
            right = left;
            left = temp;
        }
        if (bottom < top) {
            int temp = bottom;
            bottom = top;
            top = temp;
        }
        if (boundsStack.isEmpty()) {
            boundsStack.push(new Bounds(left, top, right, bottom));
        } else {
            boundsStack.push(boundsStack.peek().createSubBound(left, top, right, bottom));
        }
        if (!boundsStack.isEmpty()) {
            boundsStack.peek().set(scaledResolution);
        }
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
    }

    public static void pop(ScaledResolution scaledResolution) {
        if (!boundsStack.isEmpty()) {
            boundsStack.pop();
        }
        if (boundsStack.isEmpty()) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        } else {
            boundsStack.peek().set(scaledResolution);
        }
    }

    public static void clear() {
        boundsStack.clear();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
}
