package at.hannibal2.skyhanni.utils.render.item;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ItemTexturePassPlanTest {
    @Test
    void atlasSlotsPreserveBottomOriginAndNeighbourBoundaries() {
        assertEquals(new ItemTexturePassPlan.Region(0, 240, 16, 16), ItemTexturePassPlan.atlasRegion(256, 0, 0, 16));
        assertEquals(new ItemTexturePassPlan.Region(16, 224, 16, 16), ItemTexturePassPlan.atlasRegion(256, 16, 16, 16));
        assertEquals(new ItemTexturePassPlan.Region(240, 0, 16, 16), ItemTexturePassPlan.atlasRegion(256, 240, 240, 16));
        assertThrows(IllegalArgumentException.class, () -> ItemTexturePassPlan.atlasRegion(256, 241, 0, 16));
    }

    @Test
    void realtimePassKeepsItsOwnAttachmentsAndDoesNotInheritAtlasScissor() {
        Object atlasColor = new Object(), atlasDepth = new Object(), realtimeColor = new Object(), realtimeDepth = new Object();
        var atlas = ItemTexturePassPlan.atlasSlot(atlasColor, atlasDepth, 256, 32, 64, 16);
        var realtime = ItemTexturePassPlan.realtimeSlot(realtimeColor, realtimeDepth);
        assertSame(atlasColor, atlas.colorAttachment());
        assertSame(atlasDepth, atlas.depthAttachment());
        assertSame(realtimeColor, realtime.colorAttachment());
        assertSame(realtimeDepth, realtime.depthAttachment());
        assertNull(realtime.scissor());
        assertEquals(new ItemTexturePassPlan.Region(32, 176, 16, 16), atlas.scissor());
    }
}
