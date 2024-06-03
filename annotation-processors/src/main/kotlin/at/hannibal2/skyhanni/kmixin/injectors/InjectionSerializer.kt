package at.hannibal2.skyhanni.kmixin.injectors
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.MethodSpec

interface InjectionSerializer {

    fun readAnnotation(function: KSFunctionDeclaration, annotation: KSAnnotation): AnnotationSpec

    fun write(
        klass: KSClassDeclaration,
        annotation: AnnotationSpec,
        function: KSFunctionDeclaration,
        methodWriter: (MethodSpec.Builder) -> Unit,
        fieldWriter: (FieldSpec.Builder) -> Unit,
    )
}
