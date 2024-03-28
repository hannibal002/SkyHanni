/**
 * This file contains safe dereferences for [Const.unsafeMutable]. By convention `unconst` wrappers should only available
 * for types without interior mutability.
 */

package at.hannibal2.skyhanni.utils.const

import net.minecraft.item.Item


inline val Const<String>.unconst: String get() = unsafeMutable
inline val Const<Int>.unconst: Int get() = unsafeMutable
inline val Const<Long>.unconst: Long get() = unsafeMutable
inline val Const<Item>.unconst: Item get() = unsafeMutable
