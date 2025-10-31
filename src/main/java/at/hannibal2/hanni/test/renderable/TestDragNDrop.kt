package at.hannibal2.hanni.test.renderable

import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.compat.BlockCompat
import at.hannibal2.hanni.utils.renderables.Renderable
import at.hannibal2.hanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.hanni.utils.renderables.interactables.DragNDrop.draggable
import at.hannibal2.hanni.utils.renderables.interactables.DragNDrop.droppable
import at.hannibal2.hanni.utils.renderables.interactables.Droppable
import at.hannibal2.hanni.utils.renderables.interactables.toDragItem
import at.hannibal2.hanni.utils.renderables.primitives.placeholder
import at.hannibal2.hanni.utils.renderables.primitives.text
import net.minecraft.init.Items
import net.minecraft.item.ItemStack

@HanniModule(devOnly = true)
object TestDragNDrop : RenderableTestSuite.TestRenderable("drag") {

    override fun renderable(): Renderable {
        val bone = ItemStack(Items.bone, 1).toDragItem()
        val leaf = ItemStack(BlockCompat.getAllLeaves().first(), 1).toDragItem()

        return with(Renderable) {
            vertical(
                draggable(text("A Bone", 1.0), { bone }),
                placeholder(0, 30),
                draggable(text("A Leaf", 1.0), { leaf }),
                placeholder(0, 30),
                droppable(
                    text("Feed Dog", 1.0),
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
            )
        }
    }
}
