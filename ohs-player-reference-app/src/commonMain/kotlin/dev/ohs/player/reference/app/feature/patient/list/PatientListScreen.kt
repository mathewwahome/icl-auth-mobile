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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.ohs.player.generated.state.PatientSummaryState
import dev.ohs.player.generated.viewtype.ViewTypeCS
import dev.ohs.player.library.layout.VerticalListRenderer
import dev.ohs.player.library.scaffold.ListScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientListScreen(onPatientClick: (String) -> Unit) {
  val viewModel: PatientListViewModel = viewModel { PatientListViewModel() }
  val patients by viewModel.patients.collectAsStateWithLifecycle()

  if (patients == null) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      CircularProgressIndicator()
    }
    return
  }

  ListScaffold<PatientSummaryState>(
    items = patients!!,
    onItemClick = { onPatientClick(it.patientId ?: "") },
    key = { it.patientId ?: it.hashCode().toString() },
  ) {
    component(ViewTypeCS.PatientCard)
    layout(VerticalListRenderer.VIEW_TYPE)
    topBar {
      TopAppBar(
        title = { Text("Patients") },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
          ),
      )
    }
    emptyState { Text("No patients") }
  }
}
