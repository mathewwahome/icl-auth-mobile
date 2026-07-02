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

import com.squareup.kotlinpoet.FileSpec
import java.io.File
import java.time.Year

private val LICENSE_HEADER =
  """
  /*
   * Copyright ${Year.now().value} Open Health Stack Foundation
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
  """
    .trimIndent()

/** Writes this [FileSpec] to [directory], prepending the Apache 2.0 license header. */
fun FileSpec.writeFormattedTo(directory: File) {
  val outputFile =
    directory.resolve(packageName.replace('.', File.separatorChar)).resolve("$name.kt")
  outputFile.parentFile.mkdirs()
  outputFile.writeText("$LICENSE_HEADER\n$this")
}
