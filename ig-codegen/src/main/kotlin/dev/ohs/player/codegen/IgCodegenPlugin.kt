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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.TaskProvider

/**
 * Gradle plugin that generates typed Kotlin source from runtime config Binaries.
 *
 * Apply via:
 * ```kotlin
 * plugins { id("dev.ohs.ig-codegen") }
 * ```
 *
 * The plugin:
 * 1. Registers the [IgCodegenTask] under the name `generateIgCode`.
 * 2. Reads `Binary-*.json` and `CodeSystem-*.json` from [IgCodegenExtension.sourcesDir] (default
 *    `src/commonMain/composeResources/files`) — the implementer's own config artifacts.
 * 3. Wires the output directory into the KMP `commonMain` source set so generated sources compile
 *    automatically.
 */
class IgCodegenPlugin : Plugin<Project> {

  override fun apply(project: Project) {
    val extension = project.extensions.create("igCodegen", IgCodegenExtension::class.java)

    val generateTask: TaskProvider<IgCodegenTask> =
      project.tasks.register("generateIgCode", IgCodegenTask::class.java)

    project.afterEvaluate {
      generateTask.configure {
        it.group = "codegen"
        it.description = "Generates Kotlin source from runtime config Binaries/CodeSystem."
        it.sourcesDir.set(
          extension.sourcesDir.orElse(
            project.layout.projectDirectory.dir("src/commonMain/composeResources/files")
          )
        )
        it.packageName.set(extension.packageName)
        it.outputDir.set(
          project.layout.buildDirectory.dir("generated/ig-codegen/commonMain/kotlin")
        )
      }
    }

    // Wire generated sources into KMP commonMain via reflection to avoid hard-coding the
    // Kotlin Gradle plugin version as a compile dependency of this plugin module.
    project.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
      wireSourceSet(project, generateTask, "commonMain")
    }

    project.pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
      wireSourceSet(project, generateTask, "main")
    }
  }

  /**
   * Adds [generateTask]'s output as a source directory to [sourceSetName] on the Kotlin extension.
   * Uses reflection so the plugin does not need the Kotlin Gradle plugin on its own compile
   * classpath.
   */
  private fun wireSourceSet(
    project: Project,
    generateTask: TaskProvider<IgCodegenTask>,
    sourceSetName: String,
  ) {
    project.afterEvaluate {
      val kotlinExt = project.extensions.findByName("kotlin") ?: return@afterEvaluate

      val sourceSets =
        runCatching { kotlinExt.javaClass.getMethod("getSourceSets").invoke(kotlinExt) }
          .getOrElse {
            project.logger.warn("ig-codegen: could not access Kotlin sourceSets: ${it.message}")
            return@afterEvaluate
          }

      val sourceSet =
        runCatching {
            sourceSets.javaClass
              .getMethod("getByName", String::class.java)
              .invoke(sourceSets, sourceSetName)
          }
          .getOrElse {
            project.logger.warn("ig-codegen: sourceSet '$sourceSetName' not found: ${it.message}")
            return@afterEvaluate
          }

      val kotlinSrcSet =
        runCatching {
            sourceSet.javaClass.getMethod("getKotlin").invoke(sourceSet) as SourceDirectorySet
          }
          .getOrElse {
            project.logger.warn("ig-codegen: could not get kotlin source set: ${it.message}")
            return@afterEvaluate
          }

      kotlinSrcSet.srcDir(generateTask.flatMap { it.outputDir })
    }
  }
}
