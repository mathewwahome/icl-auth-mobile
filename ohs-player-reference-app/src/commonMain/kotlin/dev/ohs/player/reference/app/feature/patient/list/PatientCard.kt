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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.ohs.player.generated.config.PatientCardConfig
import dev.ohs.player.generated.state.PatientSummaryState
import dev.ohs.player.reference.app.feature.component.common.CardView
import dev.ohs.player.reference.app.feature.component.common.StatusChip
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

@Composable
fun PatientCard(
  patient: PatientSummaryState,
  config: PatientCardConfig = PatientCardConfig(),
  onClick: (() -> Unit)? = null,
) {
  val initials =
    buildString {
        patient.givenName?.firstOrNull()?.uppercaseChar()?.let { append(it) }
        patient.familyName?.firstOrNull()?.uppercaseChar()?.let { append(it) }
      }
      .ifBlank { "?" }
  val fullName =
    listOfNotNull(patient.givenName, patient.familyName).joinToString(" ").ifBlank { "Unknown" }

  CardView(
    elevationDp = config.elevation?.floatValue() ?: 2f,
    contentPaddingDp = config.padding?.floatValue() ?: 16f,
    onClick = onClick,
  ) {
    header {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier =
            Modifier.size(52.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = initials,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold,
          )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = fullName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
          val subtitleParts = buildList {
            if (config.showAge != false) {
              calculateAge(patient.birthDate?.toString())?.let { add("Age $it") }
            }
            if (config.showGender != false) {
              patient.gender?.let { add(it.replaceFirstChar { c -> c.uppercaseChar() }) }
            }
          }
          if (subtitleParts.isNotEmpty()) {
            Text(
              text = subtitleParts.joinToString(" · "),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          patient.mrn?.let {
            Text(
              text = it,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.outline,
            )
          }
        }
        Column(
          horizontalAlignment = Alignment.End,
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          if (config.showStatusChip != false) {
            StatusChip(isActive = patient.active ?: false)
          }
          if (onClick != null) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
              modifier = Modifier.size(16.dp),
            )
          }
        }
      }
    }
  }
}

internal fun calculateAge(
  birthDate: String?,
  today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
): String? {
  if (birthDate == null) return null
  return try {
    val birth = LocalDate.parse(birthDate)
    var age = today.year - birth.year
    if (today.month < birth.month || (today.month == birth.month && today.day < birth.day)) {
      age--
    }
    age.toString()
  } catch (_: Exception) {
    null
  }
}
