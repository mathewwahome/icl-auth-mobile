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
 * Model for deserializing a ViewConfig Binary — a self-describing UI config bound to a view type.
 *
 * Each [property] declares one field (name + FHIR type); codegen turns the property list into a
 * typed `@Serializable` config class. The property's default value (`value*`) is ignored here — it
 * is read at runtime by the library.
 */
@Serializable
data class ViewConfigDefinition(
  val resourceType: String = "",
  val viewType: String = "",
  val property: List<Property> = emptyList(),
) {
  @Serializable
  data class Property(val name: String, val type: String? = null, val collection: Boolean = false)
}
