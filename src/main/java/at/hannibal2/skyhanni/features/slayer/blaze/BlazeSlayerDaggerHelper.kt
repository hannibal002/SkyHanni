package at.hannibal2.skyhanni.features.slayer.blaze

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.TitleReceivedEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.roundToPrecision
import at.hannibal2.skyhanni.utils.StringUtils.matchRegex
import at.hannibal2.skyhanni.utils.getLorenzVec
import io.github.moulberry.moulconfig.internal.TextRenderUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class BlazeSlayerDaggerHelper {

    private var clientSideClicked = false
    private var textTopLeft = ""
    private var textTopRight = ""
    private var textBottomLeft = ""
    private var textBottomRight = ""

    private var lastDaggerCheck = 0L
    private var lastNearestCheck = 0L
    private var lastNearest: HellionShield? = null

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.slayer.blazeHideDaggerWarning) return

        val message = event.message
        if (message.matchRegex("§cStrike using the §r(.+) §r§cattunement on your dagger!") ||
            message == "§cYour hit was reduced by Hellion Shield!"
        ) {
            event.blockedReason = "blaze_slayer_dagger"
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return

        val player = Minecraft.getMinecraft().thePlayer
        val dagger = getDaggerFromStack(player.inventory.mainInventory[player.inventory.currentItem])
        if (dagger != null) {
            setDaggerText(dagger)
            return
        }

        textTopLeft = ""
        textTopRight = ""
        textBottomLeft = ""
        textBottomRight = ""
    }

    private fun setDaggerText(holding: Dagger) {
        checkActiveDagger()
        lastNearest = findNearest()

        val first = Dagger.entries[SkyHanniMod.feature.slayer.blazeFirstDagger]
        val second = first.other()

        textTopLeft = format(holding, true, first)
        textTopRight = format(holding, true, second)
        textBottomLeft = format(holding, false, first)
        textBottomRight = format(holding, false, second)
    }

    private fun findNearest(): HellionShield? {
        if (!SkyHanniMod.feature.slayer.blazeMarkRightHellionShield) return null

        if (lastNearestCheck + 100 > System.currentTimeMillis()) return lastNearest
        lastNearestCheck = System.currentTimeMillis()

        val playerLocation = LocationUtils.playerLocation()
        return HellionShieldHelper.hellionShieldMobs
            .filter { !it.key.isDead && it.key.getLorenzVec().distance(playerLocation) < 10 && it.key.health > 0 }
            .toSortedMap { a, b ->
                if (a.getLorenzVec().distance(playerLocation) > b.getLorenzVec().distance(playerLocation)) 1 else 0
            }.firstNotNullOfOrNull { it.value }
    }

    private fun format(dagger: Dagger, active: Boolean, compareInHand: Dagger): String {
        var daggerInHand = dagger
        val inHand = dagger == compareInHand

        if (!inHand) {
            daggerInHand = daggerInHand.other()
        }

        var shield = daggerInHand.getActive()
        if (!active) {
            shield = shield.other()
        }

        return if (inHand && active) {
            if (lastNearest == null) {
                "§7[" + shield.chatColor + shield.cleanName + "§7]"
            } else {
                if ((shield == lastNearest)) {
                    "§a[" + shield.chatColor + shield.cleanName.uppercase() + "§a]"
                } else {
                    "§c[§m" + shield.chatColor + shield.cleanName + "§c]"
                }
            }
        } else {
            if (shield == lastNearest) {
                "§6[" + shield.chatColor + shield.cleanName + "§6]"
            } else {
                shield.chatColor + shield.cleanName
            }
        }
    }

    private fun checkActiveDagger() {
        if (lastDaggerCheck + 1_000 > System.currentTimeMillis()) return
        lastDaggerCheck = System.currentTimeMillis()

        for (dagger in Dagger.entries) {
            if (dagger.updated) continue

            val first = dagger.shields[0]
            if (!first.active && !dagger.shields[1].active) {

                val shield = readFromInventory(dagger)
                if (shield != null) {
                    shield.active = true
                    dagger.updated = true
                } else {
                    first.active = true
                }
            }
        }
    }

    private fun readFromInventory(dagger: Dagger): HellionShield? {
        val player = Minecraft.getMinecraft().thePlayer
        for (stack in player.inventory.mainInventory) {
            val otherDagger = getDaggerFromStack(stack) ?: continue
            if (dagger != otherDagger) continue
            for (line in stack.getLore()) {
                if (!line.contains("§7Attuned: ")) continue

                for (shield in dagger.shields) {
                    if (line.contains(shield.cleanName)) {
                        return shield
                    }
                }
            }
        }

        return null
    }

    private fun getDaggerFromStack(stack: ItemStack?): Dagger? {
        val itemName = stack?.name ?: ""
        for (dagger in Dagger.entries) {
            if (dagger.daggerNames.any { itemName.contains(it) }) {
                return dagger
            }
        }

        return null
    }

    @SubscribeEvent
    fun onTitleReceived(event: TitleReceivedEvent) {
        if (!isEnabled()) return


        for (shield in HellionShield.entries) {
            if (shield.formattedName + "§r" == event.title) {
                Dagger.entries.filter { shield in it.shields }.forEach {
                    it.shields.forEach { shield -> shield.active = false }
                    it.updated = true
                }
                shield.active = true
                event.isCanceled = true
                clientSideClicked = false
                return
            }
        }
    }

    private fun isEnabled(): Boolean {
        return LorenzUtils.inSkyBlock && SkyHanniMod.feature.slayer.blazeDaggers
    }

    @SubscribeEvent
    fun onBlockClick(event: BlockClickEvent) {
        if (!isEnabled()) return
        if (clientSideClicked) return
        if (event.clickType != ClickType.RIGHT_CLICK) return

        val itemInHand = event.itemInHand ?: return
        val dagger = getDaggerFromStack(itemInHand)
        dagger?.shields?.forEach { shield -> shield.active = !shield.active }
        clientSideClicked = true

    }

    enum class Dagger(val daggerNames: List<String>, vararg val shields: HellionShield, var updated: Boolean = false) {
        TWILIGHT(
            listOf("Twilight Dagger", "Mawdredge Dagger", "Deathripper Dagger"),
            HellionShield.SPIRIT,
            HellionShield.CRYSTAL
        ),
        FIREDUST(
            listOf("Firedust Dagger", "Kindlebane Dagger", "Pyrochaos Dagger"),
            HellionShield.ASHEN,
            HellionShield.AURIC
        ),
        ;

        fun other(): Dagger = if (this == TWILIGHT) {
            FIREDUST
        } else {
            TWILIGHT
        }

        fun getActive(): HellionShield {
            for (shield in shields) {
                if (shield.active) {
                    return shield
                }
            }
            throw RuntimeException("no active shield found for dagger $this")
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GameOverlayRenderEvent) {
        if (!isEnabled()) return
        if (textTopLeft.isEmpty()) return

        if (Minecraft.getMinecraft().currentScreen != null) return

        val scaledResolution = ScaledResolution(Minecraft.getMinecraft())
        val width = scaledResolution.scaledWidth
        val height = scaledResolution.scaledHeight

        val sizeFactor = (width.toFloat() / 960f).roundToPrecision(3)

        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        val renderer = Minecraft.getMinecraft().fontRendererObj

        GlStateManager.pushMatrix()
        GlStateManager.translate(((width / 2) / 1.18).toFloat(), (height / 3.8).toFloat(), 0.0f)
        GlStateManager.scale(4.0f, 4.0f, 4.0f)
        TextRenderUtils.drawStringCenteredScaledMaxWidth(
            textTopLeft,
            renderer,
            0f,
            0f,
            false,
            (60f * sizeFactor).toInt(),
            0
        )
        GlStateManager.popMatrix()

        GlStateManager.pushMatrix()
        GlStateManager.translate(((width / 2) * 1.18).toFloat(), (height / 3.8).toFloat(), 0.0f)
        GlStateManager.scale(4.0f, 4.0f, 4.0f)
        TextRenderUtils.drawStringCenteredScaledMaxWidth(
            textTopRight,
            renderer,
            0f,
            0f,
            false,
            (60f * sizeFactor).toInt(),
            0
        )
        GlStateManager.popMatrix()

        GlStateManager.pushMatrix()
        GlStateManager.translate(((width / 2) / 1.18).toFloat(), (height / 3.0).toFloat(), 0.0f)
        GlStateManager.scale(4.0f, 4.0f, 4.0f)
        TextRenderUtils.drawStringCenteredScaledMaxWidth(
            textBottomLeft,
            renderer,
            0f,
            0f,
            false,
            (20f * sizeFactor).toInt(),
            0
        )
        GlStateManager.popMatrix()

        GlStateManager.pushMatrix()
        GlStateManager.translate(((width / 2) * 1.18).toFloat(), (height / 3.0).toFloat(), 0.0f)
        GlStateManager.scale(4.0f, 4.0f, 4.0f)
        TextRenderUtils.drawStringCenteredScaledMaxWidth(
            textBottomRight,
            renderer,
            0f,
            0f,
            false,
            (20f * sizeFactor).toInt(),
            0
        )
        GlStateManager.popMatrix()
    }
}

private fun HellionShield.other(): HellionShield {
    for (dagger in BlazeSlayerDaggerHelper.Dagger.entries) {
        if (this in dagger.shields) {
            for (shield in dagger.shields) {
                if (shield != this) {
                    return shield
                }
            }
        }
    }

    throw RuntimeException("Found no other shield for $this")
}
