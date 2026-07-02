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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.ohs.player.generated.config.SectionCardConfig
import dev.ohs.player.library.renderer.ConfiguredRenderer
import dev.ohs.player.library.renderer.LayoutRenderer
import dev.ohs.player.library.renderer.RenderOptions

/**
 * Layout renderer that wraps a list of items inside a titled section card. Registered under
 * [dev.ohs.player.generated.viewtype.ViewTypeCS.SectionCard].
 *
 * Supports optional item-count badge and collapsible behavior driven by [SectionCardConfig].
 */
class SectionCardLayoutRenderer<T>(
  private val title: String,
  private val icon: ImageVector,
  private val iconTint: Color? = null,
  private val config: SectionCardConfig = SectionCardConfig(),
) : LayoutRenderer<T> {

  @Composable
  override fun Render(
    items: List<T>,
    component: ConfiguredRenderer<T>,
    key: ((T) -> Any)?,
    onItemClick: (T) -> Unit,
    modifier: Modifier,
  ) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    val tint = iconTint ?: MaterialTheme.colorScheme.primary

    Card(
      modifier = modifier.fillMaxWidth(),
      elevation =
        CardDefaults.elevatedCardElevation(
          defaultElevation = (config.elevation?.floatValue() ?: 2f).dp
        ),
    ) {
      Column {
        Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(tint))
        Column(modifier = Modifier.padding((config.padding?.floatValue() ?: 16f).dp)) {
          Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier =
                Modifier.size(32.dp).clip(CircleShape).background(tint.copy(alpha = 0.12f)),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp),
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = title,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.weight(1f),
            )
            if (config.showItemCount != false) {
              Box(
                modifier =
                  Modifier.clip(CircleShape)
                    .background(tint.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
              ) {
                Text(
                  text = items.size.toString(),
                  style = MaterialTheme.typography.labelMedium,
                  color = tint,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
            if (config.collapsible == true) {
              IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(32.dp)) {
                Icon(
                  imageVector =
                    if (expanded) Icons.Default.KeyboardArrowUp
                    else Icons.Default.KeyboardArrowDown,
                  contentDescription = if (expanded) "Collapse" else "Expand",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(20.dp),
                )
              }
            }
          }

          AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
              HorizontalDivider(
                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
              )
              items.forEachIndexed { i, item ->
                if (i > 0)
                  HorizontalDivider(
                    modifier = Modifier.padding(vertical = 2.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                  )
                component.Render(item, RenderOptions(onClick = { onItemClick(item) }))
              }
            }
          }
        }
      }
    }
  }
}
