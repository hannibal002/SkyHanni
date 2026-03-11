package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod

object DevApi {

    val config get() = SkyHanniMod.feature.dev

    // TODO move all usages of SkyHanniMod.feature.dev into the at.hannibal2.skyhanni.test package
}
