package at.hannibal2.skyhanni.utils

import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.ModContainer

fun StackTraceElement.getClassInstance(): Class<*> {
    return Class.forName(this.className)
}

private val packageLookup by lazy {
    Loader.instance().modList
        .flatMap { mod -> mod.ownedPackages.map { it to mod } }
        .toMap()
}

val Class<*>.shPackageName
    get() =
        canonicalName.substringBeforeLast('.')

fun Class<*>.getModContainer(): ModContainer? {
    return packageLookup[shPackageName]
}


