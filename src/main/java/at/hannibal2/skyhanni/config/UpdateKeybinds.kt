package at.hannibal2.skyhanni.config

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.NotificationManager
import at.hannibal2.skyhanni.data.SkyHanniNotification
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.SkyHanniConfigSearchResetCommand
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.json.Shimmy
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import com.google.gson.JsonPrimitive
import kotlin.time.Duration

@SkyHanniModule
object UpdateKeybinds {

    private fun makeMap(): Map<Int, Int> {
        // keys are keycode on 1.8, values are keycode on 1.21
        val map = mutableMapOf<Int, Int>()
        map[0] = -1
        map[11] = 48
        map[2] = 49
        map[3] = 50
        map[4] = 51
        map[5] = 52
        map[6] = 53
        map[7] = 54
        map[8] = 55
        map[9] = 56
        map[10] = 57
        map[30] = 65
        map[40] = 39
        map[48] = 66
        map[43] = 92
        map[14] = 259
        map[46] = 67
        map[58] = 280
        map[51] = 44
        map[32] = 68
        map[211] = 261
        map[208] = 264
        map[18] = 69
        map[207] = 269
        map[28] = 257
        map[13] = 61
        map[1] = 256
        map[33] = 70
        map[59] = 290
        map[68] = 299
        map[87] = 300
        map[88] = 301
        map[100] = 302
        map[101] = 303
        map[102] = 304
        map[103] = 305
        map[104] = 306
        map[105] = 307
        map[113] = 308
        map[60] = 291
        map[61] = 292
        map[62] = 293
        map[63] = 294
        map[64] = 295
        map[65] = 296
        map[66] = 297
        map[67] = 298
        map[34] = 71
        map[41] = 96
        map[35] = 72
        map[199] = 268
        map[23] = 73
        map[210] = 260
        map[36] = 74
        map[37] = 75
        map[82] = 320
        map[79] = 321
        map[80] = 322
        map[81] = 323
        map[75] = 324
        map[76] = 325
        map[77] = 326
        map[71] = 327
        map[72] = 328
        map[73] = 329
        map[78] = 334
        map[83] = 330
        map[181] = 331
        map[156] = 335
        map[141] = 336
        map[55] = 332
        map[74] = 333
        map[38] = 76
        map[203] = 263
        map[56] = 342
        map[26] = 91
        map[29] = 341
        map[42] = 340
        map[219] = 343
        map[50] = 77
        map[12] = 45
        map[49] = 78
        map[69] = 282
        map[24] = 79
        map[25] = 80
        map[209] = 267
        map[201] = 266
        map[197] = 284
        map[52] = 46
        map[183] = 283
        map[16] = 81
        map[19] = 82
        map[205] = 262
        map[184] = 346
        map[27] = 93
        map[157] = 345
        map[54] = 344
        map[220] = 347
        map[31] = 83
        map[70] = 281
        map[39] = 59
        map[53] = 47
        map[57] = 32
        map[20] = 84
        map[15] = 258
        map[22] = 85
        map[200] = 265
        map[47] = 86
        map[17] = 87
        map[45] = 88
        map[21] = 89
        map[44] = 90
        map[-100] = 0
        map[-99] = 1
        map[-98] = 2
        return map
    }

    var keybinds: MutableList<String> = mutableListOf()

    private val logger = LorenzLogger("keybind_upgrader")

    private fun fixKeybinds(shouldFlip: Boolean) {
        var keybindMap = makeMap()
        if (shouldFlip) {
            keybindMap = keybindMap.entries.associateBy({ it.value }) { it.key }
        }
        for (keybind in keybinds) {
            val shimmy = Shimmy.makeShimmy(SkyHanniMod.feature, keybind.split("."))
            if (shimmy == null) {
                try {
                    ErrorManager.skyHanniError("Could not create shimmy for path $keybind")
                } catch (_: Exception) {
                    continue
                }
            }

            val currentValue = shimmy.getJson().asInt
            if (keybindMap.containsKey(currentValue)) {
                val newValue = keybindMap[currentValue]
                shimmy.setJson(JsonPrimitive(newValue))

                logger.log("$keybind old $currentValue")
                logger.log("$keybind new $newValue")
            } else {
                SkyHanniConfigSearchResetCommand.resetCommand(arrayOf("reset", "config.$keybind"))
                logger.log("Couldn't find a mapping for $keybind value: $currentValue")
                ChatUtils.chat("Could not convert keybind for $keybind, please set it manually in /sh")
                val text = listOf(
                    "§c§lMissing Keybind Mapping Data",
                    "§cData used to convert your skyhanni keybinds between versions is outdated",
                    "§cPlease join the SkyHanni Discord and message in §l#support§r§c to get support.",
                )
                NotificationManager.queueNotification(SkyHanniNotification(text, Duration.INFINITE, false))
            }
        }
    }

    private var hasUpdated = false

    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
        if (hasUpdated) return
        hasUpdated = true
        val lastMcVersion = SkyHanniMod.feature.lastMinecraftVersion ?: "1.8.9"
        val currentMcVersion = PlatformUtils.MC_VERSION
        if (lastMcVersion == currentMcVersion || lastMcVersion != "1.8.9" && currentMcVersion != "1.8.9") {
            return
        }

        fixKeybinds(lastMcVersion != "1.8.9")
        SkyHanniMod.feature.lastMinecraftVersion = currentMcVersion
    }
}
