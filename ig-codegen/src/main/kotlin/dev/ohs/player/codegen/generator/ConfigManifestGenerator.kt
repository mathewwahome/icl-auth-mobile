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

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import dev.ohs.player.codegen.writeFormattedTo
import java.io.File

/**
 * Generates a manifest of the config resource files, keyed by their containing directory under
 * `composeResources/files` (e.g. `states` to `[Binary-Allergy.json, …]`).
 *
 * Compose resources cannot enumerate a directory at runtime, so the runtime [ConfigSource] needs
 * the file names up front. Emitting them as compiled-in constants — derived from the actual files
 * on disk at build time — removes the hand-maintained `manifest.txt` while still letting the
 * runtime read each resource directly by path.
 */
class ConfigManifestGenerator(private val basePackage: String, private val outputDir: File) {

  fun generate(filesByDirectory: Map<String, List<String>>) {
    val listOfString = List::class.asClassName().parameterizedBy(String::class.asClassName())
    val mapType =
      Map::class.asClassName().parameterizedBy(String::class.asClassName(), listOfString)

    val initializer =
      CodeBlock.builder()
        .add("mapOf(\n")
        .indent()
        .apply {
          filesByDirectory.toSortedMap().forEach { (directory, files) ->
            val sorted = files.sorted()
            val elements = sorted.joinToString(", ") { "%S" }
            add("%S to listOf($elements),\n", directory, *sorted.toTypedArray())
          }
        }
        .unindent()
        .add(")")
        .build()

    FileSpec.builder(basePackage, OBJECT_NAME)
      .addFileComment("Generated config resource manifest. Do not edit manually.")
      .addType(
        TypeSpec.objectBuilder(OBJECT_NAME)
          .addKdoc(
            "Config resource file names keyed by their directory under `composeResources/files`."
          )
          .addProperty(
            PropertySpec.builder("byDirectory", mapType).initializer(initializer).build()
          )
          .build()
      )
      .build()
      .writeFormattedTo(outputDir)
  }

  private companion object {
    const val OBJECT_NAME = "GeneratedConfigManifest"
  }
}
