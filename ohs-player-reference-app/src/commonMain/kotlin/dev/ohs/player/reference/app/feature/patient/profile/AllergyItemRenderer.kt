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
import dev.ohs.player.generated.config.AllergyItemConfig
import dev.ohs.player.generated.state.PatientAllergyState
import dev.ohs.player.library.renderer.ComponentRenderer
import dev.ohs.player.library.renderer.RenderOptions
import dev.ohs.player.reference.app.feature.component.common.StatusChipData
import dev.ohs.player.reference.app.feature.component.common.StatusRow

class AllergyItemRenderer : ComponentRenderer<PatientAllergyState, AllergyItemConfig> {
  @Composable
  override fun Render(
    item: PatientAllergyState,
    config: AllergyItemConfig,
    options: RenderOptions,
  ) {
    val showCriticality = config.showCriticality != false
    StatusRow(
      title = item.substance ?: "Unknown substance",
      modifier = options.modifier,
      subtitle =
        if (showCriticality) item.criticality?.replaceFirstChar { it.uppercaseChar() } else null,
      subtitleColor = criticalityColor(item.criticality).copy(alpha = 0.85f),
      accentColor = if (showCriticality) criticalityColor(item.criticality) else null,
      rowBackground =
        if (item.criticality?.lowercase() == "high")
          criticalityColor(item.criticality).copy(alpha = 0.06f)
        else Color.Transparent,
      status =
        if (config.showStatus != false)
          item.allergyStatus?.let {
            StatusChipData(
              label = it.replaceFirstChar { c -> c.uppercaseChar() },
              containerColor = MaterialTheme.colorScheme.secondaryContainer,
              contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
          }
        else null,
    )
  }
}

@Composable
fun criticalityColor(criticality: String?): Color =
  when (criticality?.lowercase()) {
    "high" -> MaterialTheme.colorScheme.error
    "moderate" -> Color(0xFFE37400)
    "low" -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.onSurfaceVariant
  }
