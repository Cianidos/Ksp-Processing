import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

@KotlinPoetKspPreview
class MySymbolProcessor(
    val codeGenerator: CodeGenerator,
) : SymbolProcessor {
    // FIXME I am to large and scary
    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getSymbolsWithAnnotation("AnnotationTesting")
            .filterIsInstance<KSClassDeclaration>()
            .forEach {
                FileSpec.builder(
                    it.packageName.getShortName(),
                    "${it.simpleName.getShortName()}HelloExt"
                )
                    .addFunction(
                        FunSpec.builder("HelloWorld")
                            .receiver(it.toClassName())
                            .addCode(
                                CodeBlock.of(
                                    "println(\"\"\"Hello World From Annotation my Name is ${it.simpleName.getShortName()}\"\"\")"
                                )
                            )
                            .build()
                    ).build().writeTo(codeGenerator, false)
            }
        resolver.getSymbolsWithAnnotation("Memoize")
            .filterIsInstance<KSFunctionDeclaration>()
            .forEach {
                val parentDeclaration = it.parentDeclaration
                val parentClassName =
                    if (parentDeclaration is KSClassDeclaration?) parentDeclaration?.toClassName() else null
                val argName = it.annotations
                    .first { it.shortName.getShortName() == "Memoize" }
                    .arguments.last().value as String

                val funName = argName.ifEmpty { it.simpleName.getShortName() }

                val params = it.parameters.map { param ->
                    ParameterSpec.builder(
                        param.name!!.getShortName(),
                        param.type.toTypeName(),
                    ).build()
                }
                val props = it.parameters.map { param ->
                    PropertySpec.builder(
                        param.name!!.getShortName(),
                        param.type.toTypeName(),
                    )
                        .initializer(param.name!!.getShortName())
                        .build()
                }
                val tmpDataClass = TypeSpec.classBuilder("Tmp$funName")
                    .primaryConstructor(
                        FunSpec.constructorBuilder()
                            .addParameters(params).build()
                    )
                    .addProperties(props)
                    .addModifiers(KModifier.DATA)
                    .addModifiers(KModifier.PRIVATE)
                    .build()

                // Fixme case of none method annotated is note processed
                FileSpec.builder(
                    it.packageName.getShortName(),
                    (parentClassName?.simpleName ?: "") + funName + "Memoize"
                )
                    .addType(tmpDataClass)
                    .addProperty(
                        PropertySpec.builder(
                            "tmpData",
                            HashMap::class.asClassName()
                                .parameterizedBy(
                                    ClassName.bestGuess(tmpDataClass.name!!),
                                    it.returnType!!.toTypeName()
                                )
                        )
                            .initializer("HashMap<${tmpDataClass.name!!}, ${it.returnType!!.toTypeName()}>()")
                            .addModifiers(KModifier.PRIVATE)
                            .build()
                    )
                    .addFunction(
                        funSpec = FunSpec.builder(funName).apply {
                            if (parentClassName != null)
                                receiver(parentClassName)
                            addParameters(
                                params +
                                        ParameterSpec.builder(
                                            "recalc",
                                            Boolean::class.asClassName()
                                        ).defaultValue("false").build()
                            )
                            returns(it.returnType!!.toTypeName())
                            addCode(
                                """
                                val param = ${tmpDataClass.name!!}(${
                                    params.map { it.name }.joinToString(", ")
                                })
                                if (tmpData.contains(param)){
                                    return tmpData[param]!!
                                } else {
                                    // val res = this.${it.simpleName.getShortName()}(${
                                    params.map { it.name }.joinToString(", ")
                                })
                                    this::class.java.declaredMethods.find { it.name == "${it.simpleName.getShortName()}" }!!.let {
                                        it.isAccessible = true
                                        val res = (it.invoke(this, ${
                                    params.map { it.name }.joinToString(", ")
                                }) as ${it.returnType!!.toTypeName()})
                                        tmpData[param] = res
                                        return res
                                    }                                   
                                }
                            """.trimIndent()
                            )
                        }.build()
                    ).build().writeTo(codeGenerator, false)
            }
        return emptyList()
    }

}