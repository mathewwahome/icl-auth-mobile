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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.ohs.player.generated.config.AllergyReactionItemConfig
import dev.ohs.player.generated.state.AllergyReactionState
import dev.ohs.player.library.renderer.ComponentRenderer
import dev.ohs.player.library.renderer.RenderOptions
import dev.ohs.player.reference.app.feature.component.common.Chip

class AllergyReactionItemRenderer :
  ComponentRenderer<AllergyReactionState, AllergyReactionItemConfig> {
  @Composable
  override fun Render(
    item: AllergyReactionState,
    config: AllergyReactionItemConfig,
    options: RenderOptions,
  ) {
    AllergyReactionItemRow(item = item, config = config, modifier = options.modifier)
  }
}

@Composable
fun AllergyReactionItemRow(
  item: AllergyReactionState,
  config: AllergyReactionItemConfig = AllergyReactionItemConfig(),
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(
        text = item.substance ?: "Unknown substance",
        style = MaterialTheme.typography.bodyMedium,
      )
      if (config.showManifestation != false) {
        item.manifestation?.let {
          Text(
            text = it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
    if (config.showSeverity != false) {
      item.severity?.let { severity ->
        Chip(
          label = severity.replaceFirstChar { it.uppercaseChar() },
          containerColor = severityContainerColor(severity),
          contentColor = MaterialTheme.colorScheme.onErrorContainer,
        )
      }
    }
  }
}

@Composable
fun severityContainerColor(severity: String?) =
  when (severity?.lowercase()) {
    "severe" -> MaterialTheme.colorScheme.errorContainer
    "moderate" -> MaterialTheme.colorScheme.tertiaryContainer
    else -> MaterialTheme.colorScheme.secondaryContainer
  }
