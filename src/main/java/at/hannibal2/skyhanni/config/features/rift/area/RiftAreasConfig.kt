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
    val wyldWoods: WyldWoodsConfig = WyldWoodsConfig()

    @ConfigOption(name = "West Village", desc = "")
    @Accordion
    @Expose
    val westVillage: WestVillageConfig = WestVillageConfig()

    @Expose
    @ConfigOption(name = "Dreadfarm", desc = "")
    @Accordion
    val dreadfarm: DreadfarmConfig = DreadfarmConfig()

    @ConfigOption(name = "Mirrorverse", desc = "")
    @Accordion
    @Expose
    val mirrorverse: MirrorVerseConfig = MirrorVerseConfig()

    @Expose
    @ConfigOption(name = "Living Metal Cave", desc = "")
    @Accordion
    val livingCave: LivingCaveConfig = LivingCaveConfig()

    @Expose
    @ConfigOption(name = "Colosseum", desc = "")
    @Accordion
    val colosseum: ColosseumConfig = ColosseumConfig()

    @Expose
    @ConfigOption(name = "Stillgore Chateau", desc = "")
    @Accordion
    val stillgoreChateau: StillgoreChateauConfig = StillgoreChateauConfig()

    @Expose
    @ConfigOption(name = "Mountaintop", desc = "")
    @Accordion
    val mountaintop: MountaintopConfig = MountaintopConfig()
}
