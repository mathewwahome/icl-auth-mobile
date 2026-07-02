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
import dev.ohs.player.generated.config.ConditionItemConfig
import dev.ohs.player.generated.state.PatientConditionState
import dev.ohs.player.library.renderer.ComponentRenderer
import dev.ohs.player.library.renderer.RenderOptions
import dev.ohs.player.reference.app.feature.component.common.StatusChipData
import dev.ohs.player.reference.app.feature.component.common.StatusRow

private val AmberAccent = Color(0xFFE37400)

class ConditionItemRenderer : ComponentRenderer<PatientConditionState, ConditionItemConfig> {
  @Composable
  override fun Render(
    item: PatientConditionState,
    config: ConditionItemConfig,
    options: RenderOptions,
  ) {
    val status = item.conditionStatus?.lowercase()
    val isActive = status == "active"
    val accentColor =
      when (status) {
        "active" -> AmberAccent
        "resolved" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.outline
      }
    StatusRow(
      title = item.conditionCode ?: "Unknown condition",
      modifier = options.modifier,
      subtitle = if (config.showOnsetDate != false) item.onsetDate?.let { "Since $it" } else null,
      accentColor = accentColor,
      rowBackground = if (isActive) AmberAccent.copy(alpha = 0.06f) else Color.Transparent,
      status =
        if (config.showStatus != false)
          item.conditionStatus?.let {
            val (bg, fg) =
              when (it.lowercase()) {
                "active" -> AmberAccent.copy(alpha = 0.15f) to AmberAccent
                "resolved" ->
                  MaterialTheme.colorScheme.tertiaryContainer to
                    MaterialTheme.colorScheme.onTertiaryContainer
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
