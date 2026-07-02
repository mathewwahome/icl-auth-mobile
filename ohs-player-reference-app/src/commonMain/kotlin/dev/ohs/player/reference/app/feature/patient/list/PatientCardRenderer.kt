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
package dev.ohs.player.reference.app.feature.patient.list

import androidx.compose.runtime.Composable
import dev.ohs.player.generated.config.PatientCardConfig
import dev.ohs.player.generated.state.PatientSummaryState
import dev.ohs.player.library.renderer.ComponentRenderer
import dev.ohs.player.library.renderer.RenderOptions

class PatientCardRenderer : ComponentRenderer<PatientSummaryState, PatientCardConfig> {
  @Composable
  override fun Render(
    item: PatientSummaryState,
    config: PatientCardConfig,
    options: RenderOptions,
  ) {
    PatientCard(patient = item, config = config, onClick = options.onClick)
  }
}
