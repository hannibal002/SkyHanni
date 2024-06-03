package at.hannibal2.skyhanni.kmixin.injectors.inject

import at.hannibal2.skyhanni.kmixin.addAnnotation
import at.hannibal2.skyhanni.kmixin.addMember
import at.hannibal2.skyhanni.kmixin.addModifiers
import at.hannibal2.skyhanni.kmixin.addParameter
import at.hannibal2.skyhanni.kmixin.annotations.AT_CLASS
import at.hannibal2.skyhanni.kmixin.annotations.INJECT_CLASS
import at.hannibal2.skyhanni.kmixin.annotations.InjectionKind
import at.hannibal2.skyhanni.kmixin.annotations.KSelf
import at.hannibal2.skyhanni.kmixin.annotations.KShadow
import at.hannibal2.skyhanni.kmixin.annotations.KStatic
import at.hannibal2.skyhanni.kmixin.annotations.LOCAL_CAPTURE_CLASS
import at.hannibal2.skyhanni.kmixin.getAs
import at.hannibal2.skyhanni.kmixin.getAsEnum
import at.hannibal2.skyhanni.kmixin.hasAnnotation
import at.hannibal2.skyhanni.kmixin.injectors.InjectionSerializer
import at.hannibal2.skyhanni.kmixin.injectors.InjectionUtils
import at.hannibal2.skyhanni.kmixin.toJava
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import javax.lang.model.element.Modifier

object InjectSerializer : InjectionSerializer {

    override fun readAnnotation(function: KSFunctionDeclaration, annotation: KSAnnotation): AnnotationSpec = with(annotation) {
        AnnotationSpec.builder(INJECT_CLASS)
            .addMember("method", "\"${getAs<String>("method")}\"")
            .addAnnotation("at", AT_CLASS) {
                add("value", "\$S", getAsEnum<InjectionKind>("kind").name)
            }
            .addMember("cancellable", "\$L", getAs<Boolean>("cancellable"))
            .addMember("remap", "\$L", getAs<Boolean>("remap"))
            .addMember(getAs<Boolean>("captureLocals"), "locals", "\$T.CAPTURE_FAILHARD", LOCAL_CAPTURE_CLASS)
            .build()
    }

    override fun write(
        klass: KSClassDeclaration,
        annotation: AnnotationSpec,
        function: KSFunctionDeclaration,
        methodWriter: (MethodSpec.Builder) -> Unit,
        fieldWriter: (FieldSpec.Builder) -> Unit,
    ) {
        val method = MethodSpec.methodBuilder(function.simpleName.asString())
            .addModifiers(Modifier.PRIVATE)
            .addModifiers(function.hasAnnotation(KStatic::class), Modifier.STATIC)
            .addAnnotation(annotation)
            .returns(Void.TYPE)

        function.parameters
            .filter { !it.hasAnnotation(KShadow::class) && !it.hasAnnotation(KSelf::class) }
            .forEach {
                require(!it.hasDefault) { "Default parameters are not supported" }
                method.addParameter(it)
            }

        method.addStatement(
            "\$T.INSTANCE.${function.simpleName.asString()}(${InjectionUtils.createParameterList(function)})",
            klass.toJava()
        )

        methodWriter(method)
        InjectionUtils.gatherShadows(function, fieldWriter, methodWriter)
    }
}
