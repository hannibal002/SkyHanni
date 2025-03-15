package at.hannibal2.skyhanni.utils.compat

import net.minecraft.util.IChatComponent
import net.minecraft.util.ResourceLocation

//#if MC > 1.16
//$$ import net.minecraft.ChatFormatting
//$$ import net.minecraft.network.chat.TextColor
//#endif
//#if MC > 1.21
//$$ import net.minecraft.text.MutableText
//$$ import net.minecraft.text.PlainTextContent
//#endif

fun IChatComponent.directlyContainedText() =
//#if MC < 1.16
    this.unformattedTextForChat
//#elseif MC < 1.21
//$$    this.contents
//#else
//$$        (this.content as? PlainTextContent)?.string().orEmpty()
//#endif

fun IChatComponent?.formattedTextCompat(): String =
//#if MC < 1.16
    this?.formattedText.orEmpty()
//#else
//$$run {
//$$    this ?: return@run ""
//$$    val sb = StringBuilder()
//$$    for (component in iterator()) {
//$$        sb.append(component.style.color?.toChatFormatting()?.toString() ?: "§r")
//$$        sb.append(component.directlyContainedText())
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
//#if MC > 1.21
//$$fun MutableText.withColor(formatting: Formatting): Text {
//$$    return this.styled { it.withColor(formatting) }
//$$}
//#endif

fun String.formattedTextCompat() = this

fun createResourceLocation(domain: String, path: String): ResourceLocation {
    //#if MC < 1.21
    val textureLocation = ResourceLocation(domain, path)
    //#else
    //$$ val textureLocation = Identifier.of(domain, path)
    //#endif
    return textureLocation
}

fun createResourceLocation(path: String): ResourceLocation {
    //#if MC < 1.21
    val textureLocation = ResourceLocation(path)
    //#else
    //$$ val textureLocation = Identifier.of(path)
    //#endif
    return textureLocation
}
