package at.hannibal2.hanni.utils.compat

import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.entity.player.EntityPlayer

fun WorldClient.getLoadedPlayers(): List<EntityPlayer> =
//#if MC < 1.14
    this.playerEntities
//#else
//$$ this.players
//#endif
