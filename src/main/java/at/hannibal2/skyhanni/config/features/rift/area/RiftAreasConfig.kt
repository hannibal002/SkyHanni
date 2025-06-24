package at.hannibal2.skyhanni.config.features.rift.area

import at.hannibal2.skyhanni.config.features.rift.area.colosseum.ColosseumConfig
import at.hannibal2.skyhanni.config.features.rift.area.dreadfarm.DreadfarmConfig
import at.hannibal2.skyhanni.config.features.rift.area.livingcave.LivingCaveConfig
import at.hannibal2.skyhanni.config.features.rift.area.mirrorverse.MirrorVerseConfig
import at.hannibal2.skyhanni.config.features.rift.area.mountaintop.MountaintopConfig
import at.hannibal2.skyhanni.config.features.rift.area.stillgorechateau.StillgoreChateauConfig
import at.hannibal2.skyhanni.config.features.rift.area.westvillage.WestVillageConfig
import at.hannibal2.skyhanni.config.features.rift.area.wyldwoods.WyldWoodsConfig
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class RiftAreasConfig {
    @ConfigOption(name = "Wyld Woods", desc = "")
    @Accordion
    @Expose
    var wyldWoods: WyldWoodsConfig = WyldWoodsConfig()

    @ConfigOption(name = "West Village", desc = "")
    @Accordion
    @Expose
    var westVillage: WestVillageConfig = WestVillageConfig()

    @Expose
    @ConfigOption(name = "Dreadfarm", desc = "")
    @Accordion
    var dreadfarm: DreadfarmConfig = DreadfarmConfig()

    @ConfigOption(name = "Mirrorverse", desc = "")
    @Accordion
    @Expose
    var mirrorverse: MirrorVerseConfig = MirrorVerseConfig()

    @Expose
    @ConfigOption(name = "Living Metal Cave", desc = "")
    @Accordion
    var livingCave: LivingCaveConfig = LivingCaveConfig()

    @Expose
    @ConfigOption(name = "Colosseum", desc = "")
    @Accordion
    var colosseum: ColosseumConfig = ColosseumConfig()

    @Expose
    @ConfigOption(name = "Stillgore Chateau", desc = "")
    @Accordion
    var stillgoreChateau: StillgoreChateauConfig = StillgoreChateauConfig()

    @Expose
    @ConfigOption(name = "Mountaintop", desc = "")
    @Accordion
    var mountaintop: MountaintopConfig = MountaintopConfig()
}
