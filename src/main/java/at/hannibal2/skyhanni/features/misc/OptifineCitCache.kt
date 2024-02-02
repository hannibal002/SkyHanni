// Taken with Permission from
// <https://git.nea.moe/nea/neuhax/src/branch/master/src/main/kotlin/moe/nea/sky/features/fopt/OptifineCustomItemCacheKey.kt>
// under the LGPL 3.0 License

package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.isInt
import at.hannibal2.skyhanni.utils.StringUtils.trailingS
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.optifine.CustomItemProperties

object OptifineCitCache {
    private val config get() = SkyHanniMod.feature.misc.optifineCitCache

    private val map: MutableMap<OptifineCustomItemCacheKey, CacheResult> = mutableMapOf()
    private val histogram = Histogram<CacheStats>(100)
    private var cacheStats = CacheStats()

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (event.eventPhase != TickEvent.Phase.END) return
        cacheStats.cacheCount = map.size
        histogram.append(cacheStats)
        cacheStats = CacheStats()
        map.clear()
    }

    fun onCommand(args: Array<String>) {
        var amount = Int.MAX_VALUE
        if (args.isNotEmpty() && args[0].isInt()) {
            amount = args[0].toInt()
        }

        LorenzUtils.chat("Optifine Cache Size:")
        histogram.forEachIndexed { index, stats ->
            val ago = histogram.size - index
            if (ago <= amount) {
                LorenzUtils.chat("§b${ago}§e tick${ago.trailingS()} ago: §a${stats.cacheHits}§e-§c${stats.cacheMisses}§e-§b${stats.cacheCount}")
            }
        }
    }

    @JvmStatic
    fun retrieveCacheHit(itemstack: ItemStack, type: Int): CacheResult? {
        if (!config) return null
        val cacheResult = map[OptifineCustomItemCacheKey(itemstack, type)]
        if (cacheResult == null) {
            cacheStats.cacheMisses++
        } else {
            cacheStats.cacheHits++
        }
        return cacheResult
    }

    @JvmStatic
    fun storeCacheElement(itemstack: ItemStack, type: Int, properties: CacheResult) {
        map[OptifineCustomItemCacheKey(itemstack, type)] = properties
    }
}

class CacheResult(val customItemProperties: CustomItemProperties?)

data class CacheStats(var cacheHits: Int = 0, var cacheMisses: Int = 0, var cacheCount: Int = 0)

class OptifineCustomItemCacheKey(val stack: ItemStack, val type: Int) {
    override fun equals(other: Any?): Boolean {
        if (other !is OptifineCustomItemCacheKey) return false
        return stack === other.stack && type == other.type
    }

    override fun hashCode(): Int {
        return System.identityHashCode(stack) + type * 31
    }
}

class Histogram<T>(private val maxSize: Int) : Iterable<T> {
    private val dequeue = ArrayDeque<T>()
    fun append(element: T) {
        dequeue.addLast(element)
        if (dequeue.size > maxSize) {
            dequeue.removeFirst()
        }
    }

    val size get() = dequeue.size

    override fun iterator(): Iterator<T> {
        return dequeue.iterator()
    }
}
