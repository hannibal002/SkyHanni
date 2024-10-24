package at.hannibal2.skyhanni.utils.compat

import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.potion.Potion
import net.minecraft.util.IChatComponent
//#if MC > 1.12
//$$ import net.minecraft.init.PotionTypes
//#endif
//#if MC > 1.16
//$$ import net.minecraft.ChatFormatting
//$$ import net.minecraft.network.chat.TextColor
//#endif
//#if MC > 1.20
//$$ import net.minecraft.text.MutableText
//$$ import net.minecraft.text.PlainTextContent
//#endif

fun WorldClient.getLoadedPlayers(): List<EntityPlayer> =
//#if MC < 1.14
    this.playerEntities
//#else
//$$ this.players()
//#endif

fun Entity.getNameAsString(): String =
    this.name
//#if MC >= 1.14
//$$ .string
//#endif

fun EntityArmorStand.getArmorOrFullInventory() =
//#if MC < 1.12
    this.inventory
//#else
//$$ this.armorInventoryList
//#endif

fun Minecraft.isOnMainThread() =
//#if MC < 1.14
    this.isCallingFromMinecraftThread
//#else
//$$ this.isSameThread
//#endif

fun IChatComponent.getDirectlyContainedText() =
//#if MC < 1.16
    this.unformattedTextForChat
//#elseif MC < 1.20
//$$    this.contents
//#else
//$$        (this.content as? PlainTextContent)?.string() ?: ""
//#endif

fun IChatComponent.getFormattedTextCompat() =
//#if MC < 1.16
    this.formattedText
//#else
//$$run {
//$$    val sb = StringBuilder()
//$$    for (component in iterator()) {
//$$        sb.append(component.style.color?.toChatFormatting()?.toString() ?: "§r")
//$$        sb.append(component.getDirectlyContainedText())
//$$        sb.append("§r")
//$$    }
//$$    sb.toString()
//$$}
//$$
//$$private val textColorLUT = ChatFormatting.entries
//$$    .mapNotNull { formatting -> formatting.color?.let { it to formatting } }
//$$    .toMap()
//$$
//$$fun TextColor.toChatFormatting(): ChatFormatting? {
//$$    return textColorLUT[this.value]
//$$}
//$$
//$$fun Component.iterator(): Sequence<Component> {
//$$    return sequenceOf(this) + siblings.asSequence().flatMap { it.iterator() } // TODO: in theory we want to properly inherit styles here
//$$}
//#endif

//#if MC > 1.20
//$$fun MutableText.withColor(formatting: Formatting): Text {
//$$    return this.styled { it.withColor(formatting) }
//$$}
//#endif

object Effects {
    val invisibility =
        //#if MC <1.12
        Potion.invisibility
    //#else
    //$$    PotionTypes.INVISIBILITY
    //#endif
}
