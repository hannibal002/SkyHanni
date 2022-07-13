package at.lorenz.mod.dungeon

import at.lorenz.mod.LorenzMod
import at.lorenz.mod.events.LorenzChatEvent
import at.lorenz.mod.utils.LorenzUtils
import at.lorenz.mod.utils.LorenzUtils.matchRegex
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class DungeonBossMessages {

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyblock) return

        if (!LorenzMod.feature.chat.dungeonBossMessages) return

        if (isBoss(event.message)) {
            event.blockedReason = "dungeon_boss"
        }
    }

    private fun isBoss(message: String): Boolean {
        when {
            message.matchRegex("§([cd4])\\[BOSS] (.*)") -> {
                when {
                    message.contains(" The Watcher§r§f: ") -> return true
                    message.contains(" Bonzo§r§f: ") -> return true
                    message.contains(" Scarf§r§f:") -> return true
                    message.contains("Professor§r§f") -> return true
                    message.contains(" Livid§r§f: ") || message.contains(" Enderman§r§f: ") -> return true
                    message.contains(" Thorn§r§f: ") -> return true
                    message.contains(" Sadan§r§f: ") -> return true
                    message.contains(" Maxor§r§c: ") -> return true
                    message.contains(" Storm§r§c: ") -> return true
                    message.contains(" Goldor§r§c: ") -> return true
                    message.contains(" Necron§r§c: ") -> return true
                    message.contains(" §r§4§kWither King§r§c:") -> return true

                    message.endsWith(" Necron§r§c: That is enough, fool!") -> return true
                    message.endsWith(" Necron§r§c: Adventurers! Be careful of who you are messing with..") -> return true
                    message.endsWith(" Necron§r§c: Before I have to deal with you myself.") -> return true
                }
            }

            //M7 - Dragons
            message == "§cThe Crystal withers your soul as you hold it in your hands!" -> return true
            message == "§cIt doesn't seem like that is supposed to go there." -> return true
        }
        return false
    }
}