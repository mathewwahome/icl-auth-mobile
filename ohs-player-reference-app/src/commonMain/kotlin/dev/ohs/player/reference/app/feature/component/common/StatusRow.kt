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
package dev.ohs.player.reference.app.feature.component.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Colors for the trailing status [Chip] of a [StatusRow]. */
data class StatusChipData(val label: String, val containerColor: Color, val contentColor: Color)

/**
 * A list row with an optional left accent bar, a bold [title] with an optional [subtitle], and an
 * optional trailing status [Chip]. Shared by the medical profile item renderers (allergy,
 * condition, medication, immunization), which differ only in how they map their state to these
 * fields.
 */
@Composable
fun StatusRow(
  title: String,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
  accentColor: Color? = null,
  rowBackground: Color = Color.Transparent,
  status: StatusChipData? = null,
) {
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(6.dp))
        .background(rowBackground)
        .padding(vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    if (accentColor != null) {
      Box(
        modifier =
          Modifier.width(3.dp).height(36.dp).clip(RoundedCornerShape(2.dp)).background(accentColor)
      )
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
      )
      if (subtitle != null) {
        Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = subtitleColor)
      }
    }
    if (status != null) {
      Chip(
        label = status.label,
        containerColor = status.containerColor,
        contentColor = status.contentColor,
      )
    }
  }
}
