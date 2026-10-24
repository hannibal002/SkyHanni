package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.api.FakeNpcApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierUtils
import at.hannibal2.skyhanni.data.NpcData
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LocationUtils
import net.minecraft.client.Minecraft
import net.minecraft.world.item.component.ResolvableProfile

@SkyHanniModule
object TestFakeNpc {

    @HandleEvent
    private fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shfakenpc") {
            description = "Fake NPC Testing Command"
            category = DEVELOPER_TEST
            literal("add") {
                arg("id", BrigadierArguments.string()) { idArg ->
                    argCallback("name", BrigadierArguments.string()) { name ->
                        val id = getArg(idArg)
                        val profile = ResolvableProfile.createResolved(
                            Minecraft.getInstance().gameProfile
                        )
                        val data = NpcData(
                            id = id,
                            name = name,
                            skinProfile = profile,
                            position = LocationUtils.playerLocation(),
                            dialogue = listOf(
                                "§e[SkyHanni Fake NPC]§r Hello, I am $name!",
                                "§e[SkyHanni Fake NPC]§r Nice to meet you!"
                            ),
                        )
                        FakeNpcApi.addNpc(data)
                    }
                }
            }
            literal("remove") {
                argCallback(
                    "id",
                    BrigadierArguments.string(),
                    BrigadierUtils.dynamicSuggestionProvider { FakeNpcApi.getNpcs().map { it.id } },
                ) { id ->
                    FakeNpcApi.removeNpc(id)
                }
            }
        }
    }
}
