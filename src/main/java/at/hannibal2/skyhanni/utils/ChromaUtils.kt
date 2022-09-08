//package at.hannibal2.skyhanni.utils
//
//import at.hannibal2.skyhanni.utils.StringUtils.removeColor
//import net.minecraft.client.Minecraft
//import net.minecraft.util.EnumChatFormatting
//import java.util.regex.Matcher
//import java.util.regex.Pattern
//
//object ChromaUtils {
//
//    private val CHROMA_REPLACE_PATTERN = Pattern.compile("\u00a7z(.+?)(?=\u00a7|$)")
//    private val startTime = 0
//
//    fun chromaStringByColourCode(str: String): String? {
//        var str = str
//        if (str.contains("\u00a7z")) {
//            val matcher: Matcher = CHROMA_REPLACE_PATTERN.matcher(str)
//            val sb = StringBuffer()
//            while (matcher.find()) {
//                matcher.appendReplacement(
//                    sb,
//                    chromaString(matcher.group(1))
//                        .replace("\\", "\\\\")
//                        .replace("$", "\\$")
//                )
//            }
//            matcher.appendTail(sb)
//            str = sb.toString()
//        }
//        return str
//    }
//
//    fun chromaString(str: String): String? {
//        return chromaString(str, 0f, false)
//    }
//
//    fun chromaString(str: String, offset: Float, bold: Boolean): String {
//        var str = str
//        str = str.removeColor()
//        val currentTimeMillis = System.currentTimeMillis()
//        if (startTime == 0L) startTime =
//            currentTimeMillis
//        var chromaSpeed: Int = NotEnoughUpdates.INSTANCE.config.misc.chromaSpeed
//        if (chromaSpeed < 10) chromaSpeed = 10
//        if (chromaSpeed > 5000) chromaSpeed = 5000
//        val rainbowText = java.lang.StringBuilder()
//        var len = 0
//        for (i in 0 until str.length) {
//            val c = str[i]
//            var index: Int =
//                (offset + len / 12f - (currentTimeMillis - startTime) / chromaSpeed).toInt() % io..Utils.rainbow.size
//            len += Minecraft.getMinecraft().fontRendererObj.getCharWidth(c)
//            if (bold) len++
//            if (index < 0) index += io.github.moulberry.notenoughupdates.util.Utils.rainbow.size
//            rainbowText.append(io.github.moulberry.notenoughupdates.util.Utils.rainbow.get(index))
//            if (bold) rainbowText.append(EnumChatFormatting.BOLD)
//            rainbowText.append(c)
//        }
//        return rainbowText.toString()
//    }
//}