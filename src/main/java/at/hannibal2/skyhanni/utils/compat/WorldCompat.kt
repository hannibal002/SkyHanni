package at.hannibal2.skyhanni.utils.compat

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.world.entity.player.Player

fun ClientLevel.getLoadedPlayers(): List<Player> =
//#if MC < 1.14
//$$     this.playerEntities
//#else
this.players()
//#endif
