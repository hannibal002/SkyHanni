package at.hannibal2.skyhanni.utils.compat

import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.potion.Potion

//#if MC < 1.14
fun WorldClient.getPlayers(): List<EntityPlayer> =
    this.playerEntities
//#else
//$$ fun getPlayers() {}
//#endif


fun Entity.getNameAsString(): String =
    this.name
//#if MC >= 1.14
//$$ .getString()
//#endif

fun EntityArmorStand.getArmorOrFullInventory() =
//#if MC < 1.14
    this.inventory
//#else
//$$ this.getArmorInventoryList()
//#endif

fun Minecraft.isOnMainThread() =
//#if MC < 1.14
    this.isCallingFromMinecraftThread
//#else
//$$ this.isOnExecutionThread
//#endif

object Effects {
    val invisibility =
    //#if MC <1.14
        Potion.invisibility
    //#else
    //$$    net.minecraft.potion.Effects.INVISIBILITY
    //#endif
}


