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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.ohs.player.generated.config.TelecomItemConfig
import dev.ohs.player.generated.state.PatientTelecomState
import dev.ohs.player.library.renderer.ComponentRenderer
import dev.ohs.player.library.renderer.RenderOptions

class TelecomItemRenderer : ComponentRenderer<PatientTelecomState, TelecomItemConfig> {
  @Composable
  override fun Render(
    item: PatientTelecomState,
    config: TelecomItemConfig,
    options: RenderOptions,
  ) {
    TelecomItemRow(item = item, config = config, modifier = options.modifier)
  }
}

@Composable
fun TelecomItemRow(
  item: PatientTelecomState,
  config: TelecomItemConfig = TelecomItemConfig(),
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    val isEmail = item.telecomSystem?.lowercase() == "email"
    Icon(
      imageVector = if (isEmail) Icons.Default.Email else Icons.Default.Phone,
      contentDescription = item.telecomSystem,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(20.dp),
    )
    if (config.showSystemLabel != false) {
      Text(
        text = item.telecomSystem?.replaceFirstChar { it.uppercaseChar() } ?: "",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier,
      )
    }
    Text(text = item.telecomValue ?: "", style = MaterialTheme.typography.bodyMedium)
  }
}
