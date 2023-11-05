package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.enums.OutsideSbFeature
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.makeAccessible
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.jsonobjects.ModGuiSwitcherJson
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.client.ClientCommandHandler
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object QuickModMenuSwitch {
    private val config get() = SkyHanniMod.feature.misc.quickModMenuSwitch
    private var display = emptyList<List<Any>>()
    private var latestGuiPath = ""

    private var mods: List<Mod>? = null

    private var currentlyOpeningMod = ""
    private var lastGuiOpen = 0L

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val modsJar = event.getConstant<ModGuiSwitcherJson>("ModGuiSwitcher")
        mods = buildList {
            out@ for ((name, mod) in modsJar.mods) {
                for (path in mod.guiPath) {
                    try {
                        Class.forName(path)
                        add(Mod(name, mod.description, mod.command, mod.guiPath))
                        continue@out
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return

        if (event.isMod(5)) {
            update()
        }
    }

    class Mod(val name: String, val description: List<String>, val command: String, val guiPath: List<String>) {

        fun isInGui() = guiPath.any { latestGuiPath.startsWith(it) }
    }

    private fun update() {
        var openGui = Minecraft.getMinecraft().currentScreen?.javaClass?.name ?: "none"
        openGui = handleAbstractGuis(openGui)
        if (latestGuiPath != openGui) {
            latestGuiPath = openGui

            if (SkyHanniMod.feature.dev.debug.modMenuLog) {
                LorenzUtils.debug("Open GUI: $latestGuiPath")
            }
        }
        val mods = mods ?: return

        display = if (!shouldShow(mods)) {
            emptyList()
        } else {
            renderDisplay(mods)
        }
    }

    private fun shouldShow(mods: List<Mod>): Boolean {
        if (config.insideEscapeMenu && isEscapeMenu(latestGuiPath)) return true
        if (config.insidePlayerInventory && latestGuiPath == "net.minecraft.client.gui.inventory.GuiInventory") return true

        return mods.any { it.isInGui() }
    }

    private fun isEscapeMenu(path: String) = when (path) {
        "net.minecraft.client.gui.GuiIngameMenu" -> true
        "me.powns.togglesneak.gui.screens.GuiOptionsReplace" -> true

        else -> false
    }

    private fun handleAbstractGuis(openGui: String): String {
        if (openGui == "gg.essential.vigilance.gui.SettingsGui") {
            val clazz = Class.forName("gg.essential.vigilance.gui.SettingsGui")
            val titleBarDelegate = clazz.getDeclaredField("titleBar\$delegate").makeAccessible()
                .get(Minecraft.getMinecraft().currentScreen)
            val titleBar =
                titleBarDelegate.javaClass.declaredFields[0].makeAccessible().get(titleBarDelegate)
            val gui = titleBar.javaClass.getDeclaredField("gui").makeAccessible().get(titleBar)
            val config = gui.javaClass.getDeclaredField("config").makeAccessible().get(gui)

            return config.javaClass.name
        }
        if (openGui == "cc.polyfrost.oneconfig.gui.OneConfigGui") {
            /** TODO support different oneconfig mods:
             * Partly Sane Skies
             * Dankers SkyBlock Mod
             * Dulkir
             */
        }

        return openGui
    }

    private fun renderDisplay(mods: List<Mod>) = buildList {
        for (mod in mods) {
            val currentlyOpen = mod.isInGui()
            val nameFormat = if (currentlyOpen) "§c" else ""
            var opening = mod.name == currentlyOpeningMod
            if (currentlyOpen && opening) {
                currentlyOpeningMod = ""
                opening = false
            }
            val nameSuffix = if (opening) " §7(opening...)" else ""
            val renderable = Renderable.link(
                Renderable.string(nameFormat + mod.name),
                bypassChecks = true,
                onClick = { open(mod) },
                condition = { System.currentTimeMillis() > lastGuiOpen + 250 }
            )
            add(listOf(renderable, nameSuffix))
        }
    }

    private fun open(mod: Mod) {
        lastGuiOpen = System.currentTimeMillis()
        currentlyOpeningMod = mod.name
        update()
        try {
            when (mod.command) {
                "patcher" -> {
                    val patcher = Class.forName("club.sk1er.patcher.Patcher")
                    val instance = patcher.getDeclaredField("instance").get(null)
                    val config = instance.javaClass.getDeclaredMethod("getPatcherConfig").invoke(instance)
                    val gui = Class.forName("gg.essential.vigilance.Vigilant").getDeclaredMethod("gui").invoke(config)
                    val guiUtils = Class.forName("gg.essential.api.utils.GuiUtil")
                    for (method in guiUtils.declaredMethods) {
                        try {
                            method.invoke(null, gui)
                            return
                        } catch (_: Exception) {
                        }
                    }
                    LorenzUtils.chat("§c[SkyHanni] Error trying to open the gui for mod " + mod.name + "!")
                }

                "hytil" -> {
                    val hytilsReborn = Class.forName("cc.woverflow.hytils.HytilsReborn")
                    val instance = hytilsReborn.getDeclaredField("INSTANCE").get(null)
                    val config = instance.javaClass.getDeclaredMethod("getConfig").invoke(instance)
                    val gui = Class.forName("gg.essential.vigilance.Vigilant").getDeclaredMethod("gui").invoke(config)
                    val guiUtils = Class.forName("gg.essential.api.utils.GuiUtil")
                    for (method in guiUtils.declaredMethods) {
                        try {
                            method.invoke(null, gui)
                            return
                        } catch (_: Exception) {
                        }
                    }
                    LorenzUtils.chat("§c[SkyHanni] Error trying to open the gui for mod " + mod.name + "!")
                }

                else -> {
                    val thePlayer = Minecraft.getMinecraft().thePlayer
                    ClientCommandHandler.instance.executeCommand(thePlayer, "/${mod.command}")
                }
            }
        } catch (e: Exception) {
            ErrorManager.logError(e, "Error trying to open the gui for mod " + mod.name)
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiScreenEvent.DrawScreenEvent.Post) {
        if (!isEnabled()) return

        GlStateManager.pushMatrix()
        config.pos.renderStringsAndItems(display, posLabel = "Quick Mod Menu Switch")
        GlStateManager.popMatrix()
    }

    fun isEnabled() = (LorenzUtils.inSkyBlock || SkyHanniMod.feature.misc.showOutsideSB.contains(OutsideSbFeature.QUICK_MOD_MENU_SWITCH)) && config.enabled

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "dev.modMenuLog", "dev.debug.modMenuLog")
    }
}
