package at.hannibal2.skyhanni.test.renderable

import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.compat.BlockCompat
import at.hannibal2.skyhanni.utils.renderables.DragNDrop
import at.hannibal2.skyhanni.utils.renderables.Droppable
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.StringRenderable
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable
import at.hannibal2.skyhanni.utils.renderables.toDragItem
import net.minecraft.init.Items
import net.minecraft.item.ItemStack

@SkyHanniModule(devOnly = true)
object TestDragNDrop : RenderableTestSuite.TestRenderable("drag") {

    override fun renderable(): Renderable {
        val bone = ItemStack(Items.bone, 1).toDragItem()
        val leaf = ItemStack(BlockCompat.getAllLeaves().first(), 1).toDragItem()

        return VerticalContainerRenderable(
            listOf(
                DragNDrop.draggable(StringRenderable("A Bone", 1.0), { bone }),
                Renderable.placeholder(0, 30),
                DragNDrop.draggable(StringRenderable("A Leaf", 1.0), { leaf }),
                Renderable.placeholder(0, 30),
                DragNDrop.droppable(
                    StringRenderable("Feed Dog", 1.0),
                    object : Droppable {
                        override fun handle(drop: Any?) {
                            val unit = drop as ItemStack
                            if (unit.item == Items.bone) {
                                ChatUtils.chat("Oh, a bone!")
                            } else {
                                ChatUtils.chat("Disgusting that is not a bone!")
                            }
                        }

                        override fun validTarget(item: Any?) = item is ItemStack

                    },
                ),
            ),
        )
    }
}
