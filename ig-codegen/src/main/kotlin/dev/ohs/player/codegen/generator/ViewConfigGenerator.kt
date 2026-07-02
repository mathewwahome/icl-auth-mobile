/*
 * Copyright 2026 Open Health Stack Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.ohs.player.codegen.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import dev.ohs.player.codegen.model.ViewConfigDefinition
import dev.ohs.player.codegen.util.fieldType
import dev.ohs.player.codegen.util.useSerializersAnnotation
import dev.ohs.player.codegen.writeFormattedTo
import java.io.File

/**
 * Generates a `@Serializable` config data class from a [ViewConfigDefinition] Binary.
 *
 * The class name is the PascalCased view type plus suffix `Config` (e.g. `PatientHeader` →
 * `PatientHeaderConfig`), and each declared property becomes a nullable field. The library
 * deserializes the Binary's property values into an instance of this class at runtime.
 */
class ViewConfigGenerator(basePackage: String, private val outputDir: File) {

  private val configPkg = "$basePackage.config"
  private val serializableClass = ClassName("kotlinx.serialization", "Serializable")

  fun generate(def: ViewConfigDefinition) {
    require(def.viewType.isNotBlank()) {
      "ViewConfigGenerator: ViewConfig Binary is missing a viewType."
    }
    val name = "${def.viewType.replaceFirstChar { it.uppercase() }}Config"

    val constructor = FunSpec.constructorBuilder()
    val clazz =
      TypeSpec.classBuilder(name).addModifiers(KModifier.DATA).addAnnotation(serializableClass)

    def.property.forEach { property ->
      val type = fieldType(property)
      val default = if (property.collection) "emptyList()" else "null"
      constructor.addParameter(
        ParameterSpec.builder(property.name, type).defaultValue(default).build()
      )
      clazz.addProperty(
        PropertySpec.builder(property.name, type).initializer(property.name).build()
      )
    }

    FileSpec.builder(configPkg, name)
      .addFileComment("Generated from ViewConfig Binary '${def.viewType}'. Do not edit manually.")
      .apply { useSerializersAnnotation(def.property.map { it.type })?.let { addAnnotation(it) } }
      .addType(clazz.primaryConstructor(constructor.build()).build())
      .build()
      .writeFormattedTo(outputDir)
  }
}
