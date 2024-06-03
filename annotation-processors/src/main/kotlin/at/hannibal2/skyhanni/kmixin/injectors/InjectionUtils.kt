package at.hannibal2.skyhanni.kmixin.injectors

import at.hannibal2.skyhanni.kmixin.annotations.FINAL_CLASS
import at.hannibal2.skyhanni.kmixin.annotations.KSelf
import at.hannibal2.skyhanni.kmixin.annotations.KShadow
import at.hannibal2.skyhanni.kmixin.annotations.SHADOW_CLASS
import at.hannibal2.skyhanni.kmixin.annotations.ShadowKind
import at.hannibal2.skyhanni.kmixin.getAsEnum
import at.hannibal2.skyhanni.kmixin.hasAnnotation
import at.hannibal2.skyhanni.kmixin.toJava
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec
import javax.lang.model.element.Modifier
import kotlin.reflect.KClass

object InjectionUtils {

    fun KSAnnotated.getAnnotation(annotation: KClass<out Annotation>): KSAnnotation {
        return this.annotations.first {
            it.annotationType.resolve().declaration.qualifiedName!!.asString() == annotation.qualifiedName
        }
    }

    fun gatherShadows(
        function: KSFunctionDeclaration,
        fieldWriter: (FieldSpec.Builder) -> Unit,
        methodWriter: (MethodSpec.Builder) -> Unit,
    ) {
        function.parameters
            .filter { it.hasAnnotation(KShadow::class) }
            .forEach {
                val annotation = it.getAnnotation(KShadow::class)

                when (val kind = annotation.getAsEnum<ShadowKind>("kind")) {
                    ShadowKind.FIELD, ShadowKind.FINAL_FIELD -> {
                        val spec = FieldSpec.builder(it.type.toJava(), it.name!!.asString())
                            .addModifiers(Modifier.PRIVATE)
                            .addAnnotation(SHADOW_CLASS)

                        if (kind == ShadowKind.FINAL_FIELD) {
                            spec.addAnnotation(FINAL_CLASS)
                        }

                        fieldWriter(spec)
                    }
                    ShadowKind.METHOD -> {
                        require(it.type.resolve().isFunctionType) { "Shadow method must be a function" }

                        val types = it.type.resolve().arguments.map { it.type!!.toJava() }
                        val parameters = types.dropLast(1)
                        val returnType = types.last()

                        val method = MethodSpec.methodBuilder(it.name!!.asString())
                            .addModifiers(Modifier.ABSTRACT)
                            .apply {
                                parameters.forEachIndexed { index, type -> addParameter(type, "arg$index") }
                            }
                            .addAnnotation(SHADOW_CLASS)
                            .returns(returnType)

                        methodWriter(method)
                    }
                }
            }
    }

    fun createParameterList(function: KSFunctionDeclaration): String {
        return function.parameters.joinToString(", ") {
            when {
                it.hasAnnotation(KSelf::class) -> "(${it.type.toJava()}) (Object) this"
                it.hasAnnotation(KShadow::class) -> {
                    val kind = it.getAnnotation(KShadow::class).getAsEnum<ShadowKind>("kind")
                    when (kind) {
                        ShadowKind.FIELD, ShadowKind.FINAL_FIELD -> it.name!!.asString()
                        ShadowKind.METHOD -> "this::${it.name!!.asString()}"
                    }
                }
                else -> it.name!!.asString()
            }
        }
    }
}
