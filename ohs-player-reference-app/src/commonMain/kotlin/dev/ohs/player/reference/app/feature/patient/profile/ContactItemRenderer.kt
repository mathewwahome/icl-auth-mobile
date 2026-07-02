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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.ohs.player.generated.config.ContactItemConfig
import dev.ohs.player.generated.state.PatientContactState
import dev.ohs.player.library.renderer.ComponentRenderer
import dev.ohs.player.library.renderer.RenderOptions
import dev.ohs.player.reference.app.feature.component.common.Chip

class ContactItemRenderer : ComponentRenderer<PatientContactState, ContactItemConfig> {
  @Composable
  override fun Render(
    item: PatientContactState,
    config: ContactItemConfig,
    options: RenderOptions,
  ) {
    ContactItemRow(item = item, config = config, modifier = options.modifier)
  }
}

@Composable
fun ContactItemRow(
  item: PatientContactState,
  config: ContactItemConfig = ContactItemConfig(),
  modifier: Modifier = Modifier,
) {
  val initials =
    buildString {
        item.contactGivenName?.firstOrNull()?.uppercaseChar()?.let { append(it) }
        item.contactFamilyName?.firstOrNull()?.uppercaseChar()?.let { append(it) }
      }
      .ifBlank { "?" }
  val fullName =
    listOfNotNull(item.contactGivenName, item.contactFamilyName).joinToString(" ").ifBlank {
      "Unknown"
    }

  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Box(
      modifier =
        Modifier.size(40.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.tertiaryContainer),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = initials,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
        fontWeight = FontWeight.Bold,
      )
    }
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(
        text = fullName,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
      )
      if (config.showPhone != false) {
        item.contactPhone?.let {
          Text(
            text = it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
    if (config.showRelationship != false) {
      item.contactRelationship?.let {
        Chip(
          label = it,
          containerColor = MaterialTheme.colorScheme.tertiaryContainer,
          contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        )
      }
    }
  }
}
