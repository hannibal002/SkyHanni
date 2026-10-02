package at.hannibal2.skyhanni.utils.render.item;

import java.util.Objects;

/** Per-item attachments and atlas coordinates, without global render-target overrides. */
public record ItemTexturePassPlan<T>(T colorAttachment, T depthAttachment, Region scissor) {
    public ItemTexturePassPlan {
        Objects.requireNonNull(colorAttachment);
        Objects.requireNonNull(depthAttachment);
    }

    public record Region(int x, int y, int width, int height) {}

    public static Region atlasRegion(int textureSize, int slotX, int slotY, int pixelSize) {
        if (pixelSize <= 0 || slotX < 0 || slotY < 0 || slotX > textureSize - pixelSize || slotY > textureSize - pixelSize) {
            throw new IllegalArgumentException("Item slot must fit its atlas texture");
        }
        return new Region(slotX, textureSize - slotY - pixelSize, pixelSize, pixelSize);
    }

    public static <T> ItemTexturePassPlan<T> atlasSlot(T color, T depth, int textureSize, int x, int y, int size) {
        return new ItemTexturePassPlan<>(color, depth, atlasRegion(textureSize, x, y, size));
    }

    public static <T> ItemTexturePassPlan<T> realtimeSlot(T color, T depth) {
        return new ItemTexturePassPlan<>(color, depth, null);
    }
}
