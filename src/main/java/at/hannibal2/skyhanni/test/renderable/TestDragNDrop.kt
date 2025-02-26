package at.hannibal2.skyhanni.test.renderable

import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.renderables.DragNDrop
import at.hannibal2.skyhanni.utils.renderables.Droppable
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.toDragItem
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.ItemStack

@SkyHanniModule(devOnly = true)
object TestDragNDrop : RenderableTestSuit.TestRenderable("drag") {

    override fun renderable(): Renderable? {
        val bone = ItemStack(Items.bone, 1).toDragItem()
        val leaf = ItemStack(Blocks.leaves, 1).toDragItem()

        return Renderable.verticalContainer(
            listOf(
                DragNDrop.draggable(Renderable.string("A Bone"), { bone }),
                Renderable.placeholder(0, 30),
                DragNDrop.draggable(Renderable.string("A Leaf"), { leaf }),
                Renderable.placeholder(0, 30),
                DragNDrop.droppable(
                    Renderable.string("Feed Dog"),
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
