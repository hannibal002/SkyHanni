package at.hannibal2.skyhanni.kmixin.annotations

import com.squareup.javapoet.ClassName

val MIXIN_CLASS: ClassName = ClassName.get("org.spongepowered.asm.mixin", "Mixin")
val SHADOW_CLASS: ClassName = ClassName.get("org.spongepowered.asm.mixin", "Shadow")
val AT_CLASS: ClassName = ClassName.get("org.spongepowered.asm.mixin.injection", "At")
val INJECT_CLASS: ClassName = ClassName.get("org.spongepowered.asm.mixin.injection", "Inject")
val REDIRECT_CLASS: ClassName = ClassName.get("org.spongepowered.asm.mixin.injection", "Redirect")
val OPCODES_CLASS: ClassName = ClassName.get("org.objectweb.asm", "Opcodes")
val PSEUDO_CLASS: ClassName = ClassName.get("org.spongepowered.asm.mixin", "Pseudo")
val LOCAL_CAPTURE_CLASS: ClassName = ClassName.get("org.spongepowered.asm.mixin.injection.callback", "LocalCapture")
val FINAL_CLASS: ClassName = ClassName.get("org.spongepowered.asm.mixin", "Final")
