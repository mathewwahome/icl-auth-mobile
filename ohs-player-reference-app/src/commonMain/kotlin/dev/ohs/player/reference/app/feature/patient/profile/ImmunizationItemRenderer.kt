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
package dev.ohs.player.reference.app.feature.patient.profile

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.ohs.player.generated.config.ImmunizationItemConfig
import dev.ohs.player.generated.state.PatientImmunizationState
import dev.ohs.player.library.renderer.ComponentRenderer
import dev.ohs.player.library.renderer.RenderOptions
import dev.ohs.player.reference.app.feature.component.common.StatusChipData
import dev.ohs.player.reference.app.feature.component.common.StatusRow

class ImmunizationItemRenderer :
  ComponentRenderer<PatientImmunizationState, ImmunizationItemConfig> {
  @Composable
  override fun Render(
    item: PatientImmunizationState,
    config: ImmunizationItemConfig,
    options: RenderOptions,
  ) {
    val isCompleted = item.immunizationStatus?.lowercase() == "completed"
    StatusRow(
      title = item.vaccineName ?: "Unknown vaccine",
      modifier = options.modifier,
      subtitle = if (config.showDate != false) item.occurrenceDate?.let { "Given $it" } else null,
      accentColor =
        if (isCompleted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
      rowBackground =
        if (isCompleted) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)
        else Color.Transparent,
      status =
        if (config.showStatus != false)
          item.immunizationStatus?.let {
            val (bg, fg) =
              when (it.lowercase()) {
                "completed" ->
                  MaterialTheme.colorScheme.tertiaryContainer to
                    MaterialTheme.colorScheme.onTertiaryContainer
                "not-done" ->
                  MaterialTheme.colorScheme.errorContainer to
                    MaterialTheme.colorScheme.onErrorContainer
                else ->
                  MaterialTheme.colorScheme.surfaceVariant to
                    MaterialTheme.colorScheme.onSurfaceVariant
              }
            StatusChipData(it.replaceFirstChar { c -> c.uppercaseChar() }, bg, fg)
          }
        else null,
    )
  }
}
