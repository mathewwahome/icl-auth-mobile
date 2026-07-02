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
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import dev.ohs.player.codegen.model.CodeSystem
import dev.ohs.player.codegen.writeFormattedTo
import java.io.File

/**
 * Generates a view-type constants object from a [CodeSystem]. Each concept becomes a `ViewType`
 * property named after its code (e.g. `val PatientHeader = ViewType("PatientHeader")`); the object
 * is named after the CodeSystem (e.g. `ViewTypeCS`).
 */
class ViewTypeGenerator(
  private val basePackage: String,
  private val subPackage: String,
  private val outputDir: File,
) {

  private val viewTypeClass = ClassName("dev.ohs.player.library.registry", "ViewType")

  fun generate(codeSystem: CodeSystem) {
    FileSpec.builder("$basePackage.$subPackage", codeSystem.name)
      .addFileComment("Generated from CodeSystem/${codeSystem.id}. Do not edit manually.")
      .addType(
        TypeSpec.objectBuilder(codeSystem.name)
          .addKdoc(
            buildString {
                codeSystem.title?.let { append(it).append(".\n") }
                codeSystem.description?.let { append("\n").append(it) }
              }
              .trim()
          )
          .addProperties(
            codeSystem.concept.map { concept ->
              PropertySpec.builder(concept.code, viewTypeClass)
                .initializer("%T(%S)", viewTypeClass, concept.code)
                .addKdoc(concept.display ?: concept.code)
                .build()
            }
          )
          .build()
      )
      .build()
      .writeFormattedTo(outputDir)
  }
}
