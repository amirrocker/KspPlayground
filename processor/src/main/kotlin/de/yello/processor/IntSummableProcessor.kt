package de.yello.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ksp.TypeParameterResolver
import com.squareup.kotlinpoet.ksp.toTypeName
import de.yello.processor.annotation.IntSummable

class IntSummableProcessor(
    private val options: Map<String, String>,
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    private lateinit var intType: KSType

    override fun finish() {
        println("finish called in processor")
    }

    override fun onError() {
        error("error occurred in processor")
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        intType = resolver.builtIns.intType
        val symbols = resolver.getSymbolsWithAnnotation(IntSummable::class.qualifiedName!!)
        val unableToProcess = symbols.filterNot { it.validate() }

        symbols.filter {
                it is KSClassDeclaration &&
                it.validate()
            }
            .forEach {
                it.accept(Visitor(), Unit)
            }
        return unableToProcess.toList()
    }

    // convienience method for KSVisitor<Unit,Unit>
    inner class Visitor : KSVisitorVoid() {

        private lateinit var ksType: KSType
        private lateinit var packageName: String
        private var summables: MutableList<String> = mutableListOf()

        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            val qualifiedName = classDeclaration.qualifiedName?.asString()
            if (qualifiedName == null) {
                logger.error(
                    "@IntSummable must target classes with qualified names",
                    classDeclaration
                )
                return
            }
            if (!classDeclaration.isDataClass()) {
                logger.error(
                    "@IntSummable cannot target a non-data class $qualifiedName",
                    classDeclaration
                )
                return
            }
            if (classDeclaration.typeParameters.any()) {
                logger.error(
                    "@IntSummable must be data classes without type parameters",
                    classDeclaration
                )
            }

            ksType = classDeclaration.asType(emptyList())
            packageName = classDeclaration.packageName.asString()
            classDeclaration.getAllProperties()
                .forEach {
                    it.accept(this, Unit)
                }

            if (summables.isEmpty()) {
                println("visitClassDeclaration -> summables.isEmpty called ")
                return
            }

            // kotlinPoet stuff
            val fileSpec = FileSpec.builder(
                packageName = packageName,
                fileName = classDeclaration.simpleName.asString() + "Ext"
            ).apply {
                addFunction(
                    FunSpec.builder("sumInts")
                        .receiver(ksType.toTypeName(TypeParameterResolver.EMPTY))
                        .returns(Int::class)
                        .addStatement("val sum = %L", summables.joinToString(" + "))
                        .addStatement("return sum")
                        .build()
                )
            }.build()

//            fileSpec.writeTo(System.out /*codeGenerator = codeGenerator, aggregating = false*/)
            codeGenerator.createNewFile(
                dependencies = Dependencies(aggregating = false),
                packageName = packageName,
                fileName = classDeclaration.simpleName.asString()
            ).use { outputStream ->
                outputStream.writer()
                    .use {
                        fileSpec.writeTo(it)
                    }
            }
        }

        override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: Unit) {
            if (property.type.resolve().isAssignableFrom(intType)) {
                val name = property.simpleName.asString()
                summables.add(name)
            }
        }

        private fun KSClassDeclaration.isDataClass():Boolean {
            println("isDataClass called")
            return modifiers.contains(Modifier.DATA)
        }
    }
}

private sealed class UnsupportedIntSummableException : Exception() {
    object DataClassWithTypeParameters: UnsupportedIntSummableException()
    object NonDataClassException: UnsupportedIntSummableException()
}

private data class ClassDetails(
val type: KSType,
val simpleName: String,
val packageName: String
)