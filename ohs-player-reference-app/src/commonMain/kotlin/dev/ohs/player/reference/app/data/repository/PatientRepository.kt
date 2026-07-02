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
package dev.ohs.player.reference.app.data.repository

import dev.ohs.player.generated.state.AllergyReactionState
import dev.ohs.player.generated.state.PatientAllergyState
import dev.ohs.player.generated.state.PatientConditionState
import dev.ohs.player.generated.state.PatientContactState
import dev.ohs.player.generated.state.PatientImmunizationState
import dev.ohs.player.generated.state.PatientMedicationState
import dev.ohs.player.generated.state.PatientSummaryState
import dev.ohs.player.generated.state.PatientTelecomState
import dev.ohs.player.reference.app.data.Extraction.extractor
import dev.ohs.player.reference.app.data.datasource.allPatientIds
import dev.ohs.player.reference.app.data.datasource.patientProfileSearchResult
import dev.ohs.player.reference.app.data.datasource.patientSummarySearchResult
import dev.ohs.player.reference.app.feature.patient.profile.ProfileUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PatientRepository {

  // FhirPathEvaluator holds mutable state is not concurrent-safe.
  // limitedParallelism(1) serializes all extraction on a single background thread without any
  // explicit locking.
  private val extractorDispatcher = Dispatchers.Default.limitedParallelism(1)

  suspend fun getPatients(): List<PatientSummaryState> =
    withContext(extractorDispatcher) {
      allPatientIds().mapNotNull { id ->
        patientSummarySearchResult(id)?.let {
          extractor.extract<PatientSummaryState>(it).firstOrNull()
        }
      }
    }

  suspend fun getPatientProfile(patientId: String): ProfileUiState =
    withContext(extractorDispatcher) {
      val result = patientProfileSearchResult(patientId) ?: return@withContext ProfileUiState()
      ProfileUiState(
        patient = extractor.extract<PatientSummaryState>(result).firstOrNull(),
        allergies = extractor.extract<PatientAllergyState>(result),
        allergyReactions = extractor.extract<AllergyReactionState>(result),
        medications = extractor.extract<PatientMedicationState>(result),
        conditions = extractor.extract<PatientConditionState>(result),
        immunizations = extractor.extract<PatientImmunizationState>(result),
        contacts =
          extractor.extract<PatientContactState>(result).filter {
            it.contactGivenName != null || it.contactFamilyName != null
          },
        telecoms = extractor.extract<PatientTelecomState>(result).filter { it.telecomValue != null },
      )
    }
}
