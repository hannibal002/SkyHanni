@file:OptIn(KotlinPoetJavaPoetPreview::class, KspExperimental::class)

package at.hannibal2.skyhanni.kmixin

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeName
import com.squareup.kotlinpoet.ARRAY
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.javapoet.KotlinPoetJavaPoetPreview
import com.squareup.kotlinpoet.javapoet.toJClassName
import com.squareup.kotlinpoet.javapoet.toJTypeName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import javax.lang.model.element.Modifier
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
fun <T> KSAnnotation.getAs(id: String) =
    this.arguments.first { it.name?.asString() == id }.value as T

inline fun <reified T : Enum<T>> KSAnnotation.getAsEnum(id: String): T =
    enumValueOf<T>(this.getAs<KSType>(id).declaration.simpleName.asString())

fun KSAnnotated.hasAnnotation(klass: KClass<out Annotation>) = this.isAnnotationPresent(klass)

fun KSType.toJava(): TypeName = when {
    this.toTypeName() == UNIT -> TypeName.VOID
    this.toTypeName() == ARRAY -> ArrayTypeName.of(this.arguments.first().type!!.toJava())
    else -> this.toTypeName().toJTypeName()
}

fun KSTypeReference.isType(type: TypeName) = this.toJava() == type
fun KSTypeReference.toJava() = this.resolve().toJava()
fun KSClassDeclaration.toJava(): TypeName = when (this.toClassName()) {
    UNIT -> TypeName.VOID
    else -> this.toClassName().toJClassName()
}

fun MethodSpec.Builder.addParameter(parameter: KSValueParameter): MethodSpec.Builder =
    this.addParameter(parameter.type.toJava(), parameter.name!!.asString())

fun MethodSpec.Builder.addModifiers(add: Boolean, vararg modifiers: Modifier): MethodSpec.Builder =
    if (add) this.addModifiers(*modifiers) else this

fun AnnotationSpec.Builder.addMember(
    add: Boolean,
    name: String,
    format: String,
    vararg args: Any
): AnnotationSpec.Builder = if (add) this.addMember(name, format, *args) else this

fun AnnotationSpec.Builder.addAnnotation(
    name: String,
    klass: ClassName,
    props: PropertyBuilder.() -> Unit
): AnnotationSpec.Builder {
    val propsMap = PropertyBuilder()
    props(propsMap)
    val annotation = "@\$T(${propsMap.entries.joinToString { (k, v) -> "$k = ${v.first}" }})"

    return this.addMember(name, annotation, klass, *propsMap.values.map { it.second }.toTypedArray())
}

class PropertyBuilder : LinkedHashMap<String, Pair<String, Any?>>() {

    fun add(name: String, value: Any?) {
        add(name, "\$L", value)
    }

    fun add(name: String, format: String, value: Any?) {
        this[name] = format to value
    }
}
