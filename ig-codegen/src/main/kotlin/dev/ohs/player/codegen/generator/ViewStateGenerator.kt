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
import dev.ohs.player.codegen.model.ViewDefinition
import dev.ohs.player.codegen.model.ViewJoinMap
import dev.ohs.player.codegen.util.fieldType
import dev.ohs.player.codegen.util.useSerializersAnnotation
import dev.ohs.player.codegen.writeFormattedTo
import java.io.File

/**
 * Generates a `@Serializable` state data class from a [ViewJoinMap], merging the columns of its
 * pivot [ViewDefinition] and every joined one. The class name is the PascalCased map name plus
 * suffix`State` (e.g. `patientAllergy` generates `PatientAllergyState`).
 */
class ViewStateGenerator(
  basePackage: String,
  private val outputDir: File,
  private val viewDefsMap: Map<String, ViewDefinition>,
) {
  private val statePkg = "$basePackage.state"

  private val serializableClass = ClassName("kotlinx.serialization", "Serializable")

  fun generate(map: ViewJoinMap) {
    val columns = mergedColumns(map)
    val stateName = "${map.name.replaceFirstChar { it.uppercase() }}State"
    generateState(stateName, map.name, columns)
  }

  /** All columns the state carries: the pivot view's, then each joined view's, in order. */
  private fun mergedColumns(map: ViewJoinMap): List<ViewDefinition.Column> = buildList {
    val pivotView =
      viewDefsMap[map.view]
        ?: error("BinaryGenerator: ViewDefinition '${map.view}' not found for map '${map.name}'")
    addAll(pivotView.allColumns())
    map.joins.forEachIndexed { i, join ->
      val joinView =
        viewDefsMap[join.view]
          ?: error(
            "BinaryGenerator: ViewDefinition '${join.view}' not found in join[$i] of '${map.name}'"
          )
      addAll(joinView.allColumns())
    }
  }

  private fun generateState(name: String, mapName: String, columns: List<ViewDefinition.Column>) {
    val distinctColumns = columns.distinctBy { it.name } // unionAll blocks repeat column names
    val constructor = FunSpec.constructorBuilder()
    val stateClass =
      TypeSpec.classBuilder(name)
        .addModifiers(KModifier.DATA)
        .addAnnotation(serializableClass)
        .addProperties(
          distinctColumns.map { column ->
            val type = column.fieldType()
            val default = if (column.collection) "emptyList()" else "null"
            constructor.addParameter(
              ParameterSpec.builder(column.name, type).defaultValue(default).build()
            )

            PropertySpec.builder(column.name, type).initializer(column.name).build()
          }
        )

    FileSpec.builder(statePkg, name)
      .addFileComment("Generated from ViewJoinMap '$mapName'. Do not edit manually.")
      .apply {
        useSerializersAnnotation(distinctColumns.map { it.type })?.let { addAnnotation(it) }
      }
      .addType(stateClass.primaryConstructor(constructor.build()).build())
      .build()
      .writeFormattedTo(outputDir)
  }
}
