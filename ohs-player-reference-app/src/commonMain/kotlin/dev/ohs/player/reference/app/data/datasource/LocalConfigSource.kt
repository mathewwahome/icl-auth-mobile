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
package dev.ohs.player.reference.app.data.datasource

import dev.ohs.player.generated.GeneratedConfigManifest
import dev.ohs.player.library.config.ConfigSource
import ohsplayerreferenceclientapp.ohs_player_reference_app.generated.resources.Res

/**
 * Loads the runtime config Binaries the extractor needs (ViewJoinMap + ViewDefinition) from
 * `composeResources/files/states`. This stands in for a backend download — swapping it for an HTTP
 * fetch is the only change needed to go live, and other resource kinds can be added the same way.
 *
 * Compose resources cannot enumerate a directory at runtime, so the file names come from
 * [GeneratedConfigManifest] — emitted by ig-codegen from the actual files on disk — and each is
 * read directly by path.
 */
object LocalConfigSource : ConfigSource {

  private const val DIR_NAME = "states"

  override suspend fun readAll(): List<String> =
    GeneratedConfigManifest.byDirectory[DIR_NAME].orEmpty().map { fileName ->
      Res.readBytes("files/$DIR_NAME/$fileName").decodeToString()
    }
}
