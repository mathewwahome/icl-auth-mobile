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
package dev.ohs.player.codegen.util

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import dev.ohs.player.codegen.model.ViewConfigDefinition
import dev.ohs.player.codegen.model.ViewDefinition

private val useSerializersClass = ClassName("kotlinx.serialization", "UseSerializers")
private val fhirDateClass = ClassName("dev.ohs.fhir.model.r4", "FhirDate")
private val fhirDateTimeClass = ClassName("dev.ohs.fhir.model.r4", "FhirDateTime")
private val bigDecimalClass = ClassName("com.ionspin.kotlin.bignum.decimal", "BigDecimal")

private const val SERIALIZER_PKG = "dev.ohs.player.library.config"
private val fhirDateSerializer = ClassName(SERIALIZER_PKG, "FhirDateSerializer")
private val fhirDateTimeSerializer = ClassName(SERIALIZER_PKG, "FhirDateTimeSerializer")
private val fhirDecimalSerializer = ClassName(SERIALIZER_PKG, "FhirDecimalSerializer")

/** Collection columns become `List<T>` (non-null elements); scalar columns become `T?`. */
fun ViewDefinition.Column.fieldType(): TypeName {
  val scalar = scalarType(type)
  return if (collection) {
    List::class.asClassName().parameterizedBy(scalar.copy(nullable = false))
  } else {
    scalar.copy(nullable = true)
  }
}

/** Collection properties become `List<T>`; scalars become `T?`. */
fun fieldType(property: ViewConfigDefinition.Property): TypeName {
  val scalar = scalarType(property.type)
  return if (property.collection) {
    List::class.asClassName().parameterizedBy(scalar.copy(nullable = false))
  } else {
    scalar.copy(nullable = true)
  }
}

fun scalarType(fhirType: String?): TypeName =
  when (fhirType?.substringAfterLast('/')) {
    "boolean" -> Boolean::class.asTypeName()
    "decimal" -> bigDecimalClass
    "integer",
    "positiveInt",
    "unsignedInt" -> Int::class.asTypeName()
    "integer64" -> Long::class.asTypeName()
    "date" -> fhirDateClass
    "dateTime",
    "instant" -> fhirDateTimeClass
    else -> String::class.asTypeName()
  }

/**
 * The scalar serializer a FHIR type needs, or `null` for types kotlinx-serialization resolves on
 * its own. The kotlin-fhir models cover these inside resources but expose the serializers as
 * `internal`, so generated `@Serializable` classes attach ours via `@file:UseSerializers`.
 */
fun scalarSerializerFor(fhirType: String?): ClassName? =
  when (fhirType?.substringAfterLast('/')) {
    "decimal" -> fhirDecimalSerializer
    "date" -> fhirDateSerializer
    "dateTime",
    "instant" -> fhirDateTimeSerializer
    else -> null
  }

/**
 * A `@file:UseSerializers(...)` annotation listing the scalar serializers the given FHIR [types]
 * reference, or `null` if none are needed. Attaching them file-wide lets a plain `Json` decode the
 * FHIR scalar fields (including `List<T>` collections) without a contextual `SerializersModule`.
 */
fun useSerializersAnnotation(types: List<String?>): AnnotationSpec? {
  val serializers = types.mapNotNull { scalarSerializerFor(it) }.distinct()
  if (serializers.isEmpty()) return null
  return AnnotationSpec.builder(useSerializersClass)
    .apply { serializers.forEach { addMember("%T::class", it) } }
    .build()
}
