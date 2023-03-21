package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.SendTitleHelper
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.GardenToolChangeEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import net.minecraft.client.audio.ISound
import net.minecraft.client.audio.PositionedSound
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class WrongFungiCutterWarning {
    private var mode = FungiMode.UNKNOWN

    private var lastPlaySoundTime = 0L
    private val sound = object : PositionedSound(ResourceLocation("random.orb")) {
        init {
            volume = 50f
            repeat = false
            repeatDelay = 0
            attenuationType = ISound.AttenuationType.NONE
        }
    }


    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        val message = event.message
        if (message == "§eFungi Cutter Mode: §r§cRed Mushrooms") {
            mode = FungiMode.RED
        }
        if (message == "§eFungi Cutter Mode: §r§cBrown Mushrooms") {
            mode = FungiMode.BROWN
        }
    }

    @SubscribeEvent
    fun onBlockClick(event: BlockClickEvent) {
        if (event.clickType == ClickType.LEFT_CLICK) {
            val toString = event.getBlockState.toString()
            if (toString == "minecraft:red_mushroom") {
                if (mode == FungiMode.BROWN) {
                    notifyWrong()
                }
            }
            if (toString == "minecraft:brown_mushroom") {
                if (mode == FungiMode.RED) {
                    notifyWrong()
                }
            }
        }
    }

    private fun notifyWrong() {
        if (!SkyHanniMod.feature.garden.fungiCutterWarn) return

        SendTitleHelper.sendTitle("§cWrong Fungi Cutter Mode!", 2_000)
        if (System.currentTimeMillis() > lastPlaySoundTime + 3_00) {
            lastPlaySoundTime = System.currentTimeMillis()
            sound.playSound()
        }
    }

    @SubscribeEvent
    fun onGardenToolChange(event: GardenToolChangeEvent) {
        val crop = event.crop ?: ""
        if (crop == "Mushroom") {
            readItem(event.heldItem!!)
        } else {
            mode = FungiMode.UNKNOWN
        }
    }

    private fun readItem(item: ItemStack) {
        for (line in item.getLore()) {
            if (line == "§eMode: §cRed Mushrooms") {
                mode = FungiMode.RED
            }

            if (line == "§eMode: §cBrown Mushrooms") {
                mode = FungiMode.BROWN
            }
        }
    }

    enum class FungiMode {
        RED,
        BROWN,
        UNKNOWN
    }
}