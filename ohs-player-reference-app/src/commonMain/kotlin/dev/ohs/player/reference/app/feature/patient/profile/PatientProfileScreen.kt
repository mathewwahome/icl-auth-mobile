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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ohs.player.generated.state.AllergyReactionState
import dev.ohs.player.generated.state.PatientAllergyState
import dev.ohs.player.generated.state.PatientConditionState
import dev.ohs.player.generated.state.PatientContactState
import dev.ohs.player.generated.state.PatientImmunizationState
import dev.ohs.player.generated.state.PatientMedicationState
import dev.ohs.player.generated.state.PatientSummaryState
import dev.ohs.player.generated.state.PatientTelecomState
import dev.ohs.player.generated.viewtype.ViewTypeCS
import dev.ohs.player.library.registry.LocalViewRegistry
import dev.ohs.player.library.registry.componentRenderer
import dev.ohs.player.library.registry.layoutRenderer
import dev.ohs.player.library.renderer.RenderOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientProfileScreen(patientId: String, onBack: () -> Unit) {
  val viewModel = viewModel(key = patientId) { IpsPatientProfileViewModel(patientId) }
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val registry = LocalViewRegistry.current

  val headerRenderer =
    remember(registry) { registry.componentRenderer<PatientSummaryState>(ViewTypeCS.PatientHeader) }
  val allergySection =
    remember(registry) { registry.layoutRenderer<PatientAllergyState>(ViewTypeCS.SectionCard) }
  val allergyRenderer =
    remember(registry) { registry.componentRenderer<PatientAllergyState>(ViewTypeCS.AllergyItem) }
  val allergyReactionSection =
    remember(registry) { registry.layoutRenderer<AllergyReactionState>(ViewTypeCS.SectionCard) }
  val allergyReactionRenderer =
    remember(registry) {
      registry.componentRenderer<AllergyReactionState>(ViewTypeCS.AllergyReactionItem)
    }
  val medicationSection =
    remember(registry) { registry.layoutRenderer<PatientMedicationState>(ViewTypeCS.SectionCard) }
  val medicationRenderer =
    remember(registry) {
      registry.componentRenderer<PatientMedicationState>(ViewTypeCS.MedicationItem)
    }
  val conditionSection =
    remember(registry) { registry.layoutRenderer<PatientConditionState>(ViewTypeCS.SectionCard) }
  val conditionRenderer =
    remember(registry) {
      registry.componentRenderer<PatientConditionState>(ViewTypeCS.ConditionItem)
    }
  val immunizationSection =
    remember(registry) { registry.layoutRenderer<PatientImmunizationState>(ViewTypeCS.SectionCard) }
  val immunizationRenderer =
    remember(registry) {
      registry.componentRenderer<PatientImmunizationState>(ViewTypeCS.ImmunizationItem)
    }
  val contactSection =
    remember(registry) { registry.layoutRenderer<PatientContactState>(ViewTypeCS.SectionCard) }
  val contactRenderer =
    remember(registry) { registry.componentRenderer<PatientContactState>(ViewTypeCS.ContactItem) }
  val telecomSection =
    remember(registry) { registry.layoutRenderer<PatientTelecomState>(ViewTypeCS.SectionCard) }
  val telecomRenderer =
    remember(registry) { registry.componentRenderer<PatientTelecomState>(ViewTypeCS.TelecomItem) }

  val patient = state?.patient
  val patientName =
    remember(patient) {
      listOfNotNull(patient?.givenName, patient?.familyName).joinToString(" ").ifBlank { "Patient" }
    }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(patientName) },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(
              Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = MaterialTheme.colorScheme.onPrimary,
            )
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
          ),
      )
    }
  ) { padding ->
    val s = state
    if (s == null) {
      Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
      }
      return@Scaffold
    }
    if (s.patient == null) {
      Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        Text("Patient not found")
      }
      return@Scaffold
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(padding),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item(key = "patient_header") {
        Card(
          modifier = Modifier.fillMaxWidth(),
          elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        ) {
          Box(modifier = Modifier.padding(20.dp)) {
            headerRenderer.Render(s.patient, RenderOptions())
          }
        }
      }

      if (s.allergies.isNotEmpty()) {
        item(key = "allergies") {
          allergySection.Render(items = s.allergies, component = allergyRenderer, onItemClick = {})
        }
      }
      if (s.allergyReactions.isNotEmpty()) {
        item(key = "allergy_reactions") {
          allergyReactionSection.Render(
            items = s.allergyReactions,
            component = allergyReactionRenderer,
            onItemClick = {},
          )
        }
      }
      if (s.medications.isNotEmpty()) {
        item(key = "medications") {
          medicationSection.Render(
            items = s.medications,
            component = medicationRenderer,
            onItemClick = {},
          )
        }
      }
      if (s.conditions.isNotEmpty()) {
        item(key = "conditions") {
          conditionSection.Render(
            items = s.conditions,
            component = conditionRenderer,
            onItemClick = {},
          )
        }
      }
      if (s.immunizations.isNotEmpty()) {
        item(key = "immunizations") {
          immunizationSection.Render(
            items = s.immunizations,
            component = immunizationRenderer,
            onItemClick = {},
          )
        }
      }
      if (s.telecoms.isNotEmpty()) {
        item(key = "telecoms") {
          telecomSection.Render(items = s.telecoms, component = telecomRenderer, onItemClick = {})
        }
      }
      if (s.contacts.isNotEmpty()) {
        item(key = "contacts") {
          contactSection.Render(items = s.contacts, component = contactRenderer, onItemClick = {})
        }
      }
    }
  }
}
