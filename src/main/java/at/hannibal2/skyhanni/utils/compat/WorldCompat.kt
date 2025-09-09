package at.hannibal2.skyhanni.utils.compat

import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.player.PlayerEntity

fun ClientWorld.getLoadedPlayers(): List<PlayerEntity> =
//#if MC < 1.14
//$$     this.playerEntities
//#else
this.players
//#endif
