package at.hannibal2.skyhanni.mixins.init;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;

import java.util.Collection;

/**
 * Inject just before a for loop which iterates over a local variable containing an array.
 *
 * <pre>{@code
 * String [] s = new String[10];
 *
 * // <-- Injection point here
 * for (String e : s) {
 *
 * }
 * }
 * </pre>
 * Does not work for more complex instructions which call functions or do other operations inside the for loop header.
 * Does not work for {@link java.util.Iterator iterators}.
 *
 * <p>Set the lvIndex arg to specify which lvIndex to search for when selecting the loop.</p>
 *
 *
 * <pre>{@code
 * @Inject(method = "...", at = @At(value = "SKYHANNI_FORLOOP_LOCAL_VAR", args = "lvIndex=1"))
 * }</pre>
 */
@InjectionPoint.AtCode("SKYHANNI_FORLOOP_LOCAL_VAR")
public class BeforeForLoopInjectionPoint extends InjectionPoint {
    private final int lvIndex;

    public BeforeForLoopInjectionPoint(InjectionPointData data) {
        lvIndex = data.get("lvIndex", -1);
    }

    @Override
    public boolean find(String desc, org.objectweb.asm.tree.InsnList insns, Collection<org.objectweb.asm.tree.AbstractInsnNode> nodes) {
        for (org.objectweb.asm.tree.AbstractInsnNode p = insns.getFirst(); p != null; p = p.getNext()) {
            if (p.getOpcode() != Opcodes.ARRAYLENGTH) {
                continue;
            }
            org.objectweb.asm.tree.AbstractInsnNode loadLoopVar = p.getPrevious();
            if (loadLoopVar == null || loadLoopVar.getOpcode() != Opcodes.ALOAD) continue;
            org.objectweb.asm.tree.AbstractInsnNode storeLoopVar = loadLoopVar.getPrevious();
            if (storeLoopVar == null || storeLoopVar.getOpcode() != Opcodes.ASTORE) continue;
            AbstractInsnNode loadLoopArg = storeLoopVar.getPrevious();
            if (loadLoopArg == null || loadLoopArg.getOpcode() != Opcodes.ALOAD) continue;
            if (lvIndex != -1 && ((VarInsnNode) loadLoopArg).var != lvIndex) continue;
            nodes.add(loadLoopArg);
        }
        return false;
    }
}
