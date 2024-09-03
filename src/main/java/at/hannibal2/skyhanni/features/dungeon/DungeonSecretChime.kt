package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import net.minecraft.init.Blocks
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object DungeonSecretChime {
    private val config get() = SkyHanniMod.feature.dungeon.secretChime
    private const val WITHER_ESSENCE_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzRkYjRhZGZhOWJmNDhmZjVkNDE3MDdhZTM0ZWE3OGJkMjM3MTY1OWZjZDhjZDg5MzQ3NDlhZjRjY2U5YiJ9fX0="

    @SubscribeEvent
    fun onBlockClick(event: BlockClickEvent) {
        if (!isEnabled() || event.clickType != ClickType.RIGHT_CLICK) return

        val block = event.position.getBlockAt()
        when (block) {
            Blocks.chest, Blocks.trapped_chest, Blocks.lever -> playSound()
            Blocks.skull -> {
                val texture = BlockUtils.getTextureFromSkull(event.position.toBlockPos())
                if (texture == WITHER_ESSENCE_TEXTURE) {
                    playSound()
                }
            }
            else -> return
        }
    }

    fun isEnabled() = !DungeonAPI.inBossRoom && DungeonAPI.inDungeon() && config.enabled

    @JvmStatic
    fun playSound() {
        with(config) {
            SoundUtils.createSound(name, pitch).playSound()
        }
    }
}
