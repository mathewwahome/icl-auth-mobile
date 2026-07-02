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
package dev.ohs.player.codegen

import dev.ohs.player.codegen.generator.ConfigManifestGenerator
import dev.ohs.player.codegen.generator.ViewConfigGenerator
import dev.ohs.player.codegen.generator.ViewStateGenerator
import dev.ohs.player.codegen.generator.ViewTypeGenerator
import dev.ohs.player.codegen.model.CodeSystem
import dev.ohs.player.codegen.model.ViewConfigDefinition
import dev.ohs.player.codegen.model.ViewDefinition
import dev.ohs.player.codegen.model.ViewJoinMap
import dev.ohs.player.codegen.util.json
import dev.ohs.player.codegen.util.resourceType
import java.io.File
import kotlinx.serialization.json.jsonObject
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Gradle task that generates Kotlin source from runtime config Binaries and CodeSystem.
 *
 * Every `Binary-*.json` and `CodeSystem-*.json` under [sourcesDir] is routed by its top-level
 * `resourceType` — the same discriminator the runtime `ConfigStore` uses — and turned into typed
 * code:
 * - **ViewDefinition** to columns feeding state generation
 * - **ViewJoinMap** to a `@Serializable` state data class in the `state` package
 * - **ViewConfig** to a `@Serializable` config data class in the `config` package
 * - **CodeSystem** to a view-type constants object in the `viewtype` package
 */
@CacheableTask
abstract class IgCodegenTask : DefaultTask() {

  /** Directory tree of runtime config `Binary-*.json` and `CodeSystem-*.json` files. */
  @get:InputDirectory
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val sourcesDir: DirectoryProperty

  /** Root Kotlin package for all emitted files (e.g. `dev.ohs.player.generated`). */
  @get:Input abstract val packageName: Property<String>

  /** Directory into which generated `.kt` files are written. */
  @get:OutputDirectory abstract val outputDir: DirectoryProperty

  @TaskAction
  fun generate() {
    val sourcesRoot = sourcesDir.get().asFile
    require(sourcesRoot.isDirectory) {
      "ig-codegen: sourcesDir '${sourcesRoot.absolutePath}' is not a directory."
    }

    val outDir = outputDir.get().asFile
    outDir.deleteRecursively()
    outDir.mkdirs()

    val pkg = packageName.get()

    val configFiles =
      sourcesRoot
        .walkTopDown()
        .filter {
          it.isFile &&
            (it.name.startsWith("Binary-") || it.name.startsWith("CodeSystem-")) &&
            it.extension == "json"
        }
        .toList()

    val filesByDirectory =
      configFiles.groupBy { it.parentFile.name }.mapValues { it.value.map(File::getName) }
    ConfigManifestGenerator(pkg, outDir).generate(filesByDirectory)

    val resources =
      configFiles.mapNotNull { file ->
        runCatching { json.parseToJsonElement(file.readText()).jsonObject }.getOrNull()
      }

    // ViewDefinitions provide the columns; index them by name for the state generator.
    val viewDefs =
      resources
        .filter { it.resourceType() == VIEW_DEFINITION }
        .map { json.decodeFromJsonElement(ViewDefinition.serializer(), it) }
        .associateBy { it.name }

    // ViewJoinMap to state class.
    val binaryGen = ViewStateGenerator(pkg, outDir, viewDefs)
    resources
      .filter { it.resourceType() == VIEW_JOIN_MAP }
      .map { json.decodeFromJsonElement(ViewJoinMap.serializer(), it) }
      .forEach { map ->
        binaryGen.generate(map)
        logger.lifecycle("ig-codegen: generated state to ${map.name}")
      }

    // ViewConfig to config class.
    val configGen = ViewConfigGenerator(pkg, outDir)
    resources
      .filter { it.resourceType() == VIEW_CONFIG }
      .map { json.decodeFromJsonElement(ViewConfigDefinition.serializer(), it) }
      .forEach { def ->
        configGen.generate(def)
        logger.lifecycle("ig-codegen: generated config to ${def.viewType}")
      }

    // CodeSystem to view-type constants.
    val codeSystemGen = ViewTypeGenerator(pkg, "viewtype", outDir)
    resources
      .filter { it.resourceType() == "CodeSystem" }
      .map { json.decodeFromJsonElement(CodeSystem.serializer(), it) }
      .forEach { cs ->
        codeSystemGen.generate(cs)
        logger.lifecycle("ig-codegen: generated view types to ${cs.name}")
      }
  }

  private companion object {
    const val VIEW_DEFINITION = "https://sql-on-fhir.org/ig/StructureDefinition/ViewDefinition"
    const val VIEW_JOIN_MAP = "http://ohs.dev/StructureDefinition/ViewJoinMap"
    const val VIEW_CONFIG = "http://ohs.dev/StructureDefinition/ViewConfig"
  }
}
