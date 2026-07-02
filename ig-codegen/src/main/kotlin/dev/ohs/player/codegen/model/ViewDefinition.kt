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
package dev.ohs.player.codegen.model

import kotlinx.serialization.Serializable

/**
 * Codegen view of a ViewDefinition — only the fields code generation needs to derive the typed
 * state class: the resource and its column tree. Runtime concerns the generator never reads
 * (`constant`, `where`, `status`, …) are intentionally omitted; the full SQL-on-FHIR model lives in
 * the library (`dev.ohs.player.library.config.ViewDefinition`), where extraction uses them. Unknown
 * JSON keys are ignored on decode.
 */
@Serializable
data class ViewDefinition(
  val resourceType: String = "",
  val name: String,
  val resource: String,
  val select: List<SelectBlock> = emptyList(),
) {
  /**
   * One node of the select tree. May carry [column]s, nested [select] nodes, a
   * [forEach]/[forEachOrNull] re-rooting evaluation at each element of a path, and a [unionAll] of
   * further select nodes whose rows are concatenated. (Only column names matter for codegen — the
   * flat state schema is the union of every column in the tree.)
   */
  @Serializable
  data class SelectBlock(
    val column: List<Column> = emptyList(),
    val select: List<SelectBlock> = emptyList(),
    val forEach: String? = null,
    val forEachOrNull: String? = null,
    val unionAll: List<SelectBlock> = emptyList(),
  )

  /**
   * A single output column.
   *
   * [collection] = true means the path returns multiple values; the generated state field will be
   * `List<T>` instead of `T?`.
   */
  @Serializable
  data class Column(
    val name: String,
    val path: String,
    val type: String? = null,
    val collection: Boolean = false,
    val description: String? = null,
  )

  /**
   * Every column the view can produce, walking the whole select tree (unionAll branches share a
   * schema).
   */
  fun allColumns(): List<Column> = select.flatMap { it.collectColumns() }

  private fun SelectBlock.collectColumns(): List<Column> = buildList {
    addAll(column)
    select.forEach { addAll(it.collectColumns()) }
    unionAll.firstOrNull()?.let { addAll(it.collectColumns()) }
  }
}
