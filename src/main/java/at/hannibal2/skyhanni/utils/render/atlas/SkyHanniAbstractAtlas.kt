package at.hannibal2.skyhanni.utils.render.atlas

import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.render.item.SkyHanniAbstractItemTexture
import com.mojang.blaze3d.platform.TextureUtil
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.GpuTexture
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.Dumpable
import net.minecraft.resources.Identifier
import java.nio.file.Path

abstract class SkyHanniAbstractAtlas<K : Any, E : SkyHanniAbstractAtlasEntry> : SkyHanniAbstractItemTexture(), Dumpable {

    protected abstract val identifier: Identifier

    init {
        DelayedRun.runNextTick {
            ChatUtils.debug("Registering SkyHanniAbstractAtlas $identifier")
            Minecraft.getInstance().textureManager.register(identifier, this)
        }
    }

    protected var sizePixels = 0
        private set
    protected val entries = HashMap<K, E>()
    private var packer: SkyHanniAtlasBinPacker? = null

    protected abstract val colorLabel: String
    protected abstract val depthLabel: String

    private val textureUsage = GpuTexture.USAGE_RENDER_ATTACHMENT or
        GpuTexture.USAGE_TEXTURE_BINDING or
        GpuTexture.USAGE_COPY_SRC or
        GpuTexture.USAGE_COPY_DST

    protected fun ensureAllocated() {
        if (texture != null) return
        val size = 512.coerceAtMost(RenderSystem.getDevice().maxTextureSize)
        sizePixels = size
        allocateTextures(size, colorLabel, depthLabel, textureUsage)
        packer = SkyHanniAtlasBinPacker(size)
        onAllocated()
    }

    /** Called immediately after the atlas GPU resources are first allocated. */
    protected open fun onAllocated() {}

    /** Attempts to insert a square slot of [size] pixels. Returns null if the atlas is full. */
    internal fun tryInsert(size: Int): SkyHanniAtlasBinPacker.PackedNode? = packer?.insert(size)

    /** Computes (u, v) atlas coordinates for a slot at ([slotX], [slotY]). */
    protected fun uvForSlot(slotX: Int, slotY: Int): Pair<Float, Float> {
        val u = slotX.toFloat() / sizePixels
        val v = (sizePixels - slotY).toFloat() / sizePixels
        return u to v
    }

    internal fun atlasDebugInfo(): List<String> = listOf(
        "identifier: $identifier",
        "hasTexture: ${texture != null}",
        "entryCount: ${entries.size}",
        "sizePixels: $sizePixels",
    )

    fun invalidate() {
        entries.clear()
        close()
    }

    override fun close() {
        super.close()
        packer = null
    }

    override fun dumpContents(id: Identifier, path: Path) {
        ChatUtils.debug("dumping $id to $path")
        val texture = this.texture ?: return ChatUtils.debug("no texture")
        try {
            TextureUtil.writeAsPNG(path, id.toDebugFileName(), texture, 0) { i -> i }
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(
                e,
                "Failed to dump atlas texture",
                "id" to id.toString(),
                "path" to path.toString(),
            )
        }
    }
}
