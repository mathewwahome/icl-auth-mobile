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
package dev.ohs.player.reference.app.feature.group.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.ohs.player.generated.config.GroupHeaderConfig
import dev.ohs.player.generated.state.GroupHeaderState
import dev.ohs.player.library.renderer.ComponentRenderer
import dev.ohs.player.library.renderer.RenderOptions
import dev.ohs.player.reference.app.feature.component.common.Chip

class GroupHeaderRenderer : ComponentRenderer<GroupHeaderState, GroupHeaderConfig> {
  @Composable
  override fun Render(item: GroupHeaderState, config: GroupHeaderConfig, options: RenderOptions) {
    GroupHeaderCard(item = item, config = config, modifier = options.modifier)
  }
}

@Composable
fun GroupHeaderCard(
  item: GroupHeaderState,
  config: GroupHeaderConfig = GroupHeaderConfig(),
  modifier: Modifier = Modifier,
) {
  val name = item.groupName ?: "Household"
  val initials = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "H"
  val memberCount = item.memberCount ?: "0"
  val headName =
    listOfNotNull(item.headGivenName, item.headFamilyName).joinToString(" ").ifBlank { null }

  Card(
    modifier = modifier.fillMaxWidth(),
    elevation =
      CardDefaults.elevatedCardElevation(
        defaultElevation = (config.elevation?.floatValue() ?: 2f).dp
      ),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding((config.padding?.floatValue() ?: 20f).dp),
      horizontalArrangement = Arrangement.spacedBy(16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier.size(72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = initials,
          style = MaterialTheme.typography.headlineMedium,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
          fontWeight = FontWeight.Bold,
        )
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        HorizontalDivider(
          modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
          color = MaterialTheme.colorScheme.outlineVariant,
        )
        if (config.showMemberCount != false) {
          Row {
            Chip(
              label = "$memberCount member${if (memberCount == "1") "" else "s"}",
              containerColor = MaterialTheme.colorScheme.surfaceVariant,
              contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        if (config.showHeadName != false && headName != null) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector = Icons.Default.Person,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(14.dp),
            )
            Text(
              text = "Head: $headName",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}
