package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.entity.slayer.SlayerDeathEvent
import at.hannibal2.skyhanni.features.slayer.SlayerType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.CollectionUtils.addString
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.formatIntOrUserError
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object CarryTracker {
    private val config get() = SkyHanniMod.feature.misc

    val customers = mutableListOf<Customer>()
    var display = listOf<Renderable>()

    @HandleEvent
    fun onSlayerDeath(event: SlayerDeathEvent) {
        val slayerType = event.slayerType
        val tier = event.tier
        val owner = event.owner
        for (customer in customers) {
            if (customer.name.equals(owner, ignoreCase = true)) continue
            for (carry in customer.carries) {
                val type = carry.type as? SlayerCarryType ?: return
                if (type.slayerType != slayerType) continue
                if (type.tier != tier) continue
                carry.done++
                if (carry.done == carry.requested) {
                    ChatUtils.chat("Carry done for ${customer.name}!")
                    LorenzUtils.sendTitle("§eCarry done!", 3.seconds)
                }
                update()
            }
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!LorenzUtils.inSkyBlock) return

        config.carryPosition.renderRenderables(display, posLabel = "Carry Tracker")
    }

    fun onCommand(args: Array<String>) {
        if (args.size != 3) {
            ChatUtils.userError("Usage: /shcarry <customer name> <type> <amountRequested>")
            return
        }

        val customerName = args[0]

        val rawType = args[1]
        val carryType = getCarryType(rawType) ?: run {
            ChatUtils.userError("Unknown carry type: '$rawType'")
            return
        }

        val amountRequested = args[2].formatIntOrUserError() ?: return

        val newCarry = Carry(carryType, amountRequested)

        for (customer in customers) {
            if (!customer.name.equals(customerName, ignoreCase = true)) continue
            val carries = customer.carries
            for (carry in carries.toList()) {
                if (!newCarry.type.sameType(carry.type)) continue
                carries.remove(carry)
                val newAmountRequested = carry.requested + amountRequested
                if (newAmountRequested < 1) {
                    ChatUtils.userError("New carry amount requested must be positive!")
                    return
                }
                val updatedCarry = Carry(carryType, newAmountRequested)
                updatedCarry.done = carry.done
                carries.add(updatedCarry)
                update()
                ChatUtils.chat("Updated carry: §b$customerName §8x$newAmountRequested ${newCarry.type}")
                return
            }
        }
        if (amountRequested < 1) {
            ChatUtils.userError("Carry amount requested must be positive!")
            return
        }

        val customer = getCustomer(customerName)
        customer.carries.add(newCarry)
        update()
        ChatUtils.chat("Started carry: §b$customerName §8x$amountRequested ${newCarry.type}")
    }

    private fun getCustomer(customerName: String): Customer {
        for (customer in customers) {
            if (customer.name.equals(customerName, ignoreCase = true)) {
                return customer
            }
        }
        val customer = Customer(customerName)
        customers.add(customer)
        return customer
    }

    fun CarryType.sameType(other: CarryType): Boolean = name == other.name && tier == other.tier

    private fun update() {
        val list = mutableListOf<Renderable>()
        if (customers.none { it.carries.isNotEmpty() }) {
            display = emptyList()
            return
        }
        list.addString("§d§lCarries")
        for (customer in customers) {
            val customerName = customer.name
            if (customer.carries.isEmpty()) continue
            list.addString("§b$customerName")

            val carries = customer.carries
            for (carry in carries) {
                val requested = carry.requested
                val done = carry.done
                val color = if (done > requested) "§c" else if (done == requested) "§a" else "§e"
                val text = "$color$done§8/$color$requested"
                list.add(
                    Renderable.clickAndHover(
                        Renderable.string("  ${carry.type} $text"),
                        tips = listOf("§eClick to remove this carry"),
                        onClick = {
                            carries.remove(carry)
                            update()
                        },
                    ),
                )
            }
        }
        display = list
    }

    fun getCarryType(input: String): CarryType? {
        if (input.length == 1) return null
        val rawName = input.dropLast(1)
        val tier = input.last().digitToIntOrNull() ?: return null

        getSlayerType(rawName)?.let {
            return SlayerCarryType(it, tier)
        }

        return null
    }

    fun getSlayerType(name: String): SlayerType? =
        when (name.lowercase()) {
            "rev", "revenant", "zombie" -> SlayerType.REVENANT
            "tara", "tarantula", "spider", "brood", "broodmother" -> SlayerType.TARANTULA
            "sven", "wolf", "packmaster" -> SlayerType.SVEN
            "voidling", "void", "voidgloom", "eman", "enderman" -> SlayerType.VOID
            "inferno", "demon", "demonlord", "blaze" -> SlayerType.INFERNO
            "blood", "bloodfiend", "vamp", "vampire", "riftstalker" -> SlayerType.VAMPIRE

            else -> null
        }


    class Customer(val name: String, val carries: MutableList<Carry> = mutableListOf())

    class Carry(val type: CarryType, val requested: Int, var done: Int = 0)


    abstract class CarryType(val name: String, val tier: Int) {
        override fun toString(): String = "§d$name $tier"
    }

    class SlayerCarryType(val slayerType: SlayerType, tier: Int) : CarryType(slayerType.displayName, tier)
//     class DungeonCarryType(val floor: DungeonFloor, masterMode: Boolean) : CarryType(floor.name, tier)
}
