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

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import dev.ohs.player.library.registry.LocalViewRegistry
import dev.ohs.player.reference.app.buildAppViewRegistry
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class PatientListScreenTest {

  @Test
  fun tappingPatient_invokesOnPatientClickWithMatchingId() = runComposeUiTest {
    val registry = buildAppViewRegistry()
    var clickedId: String? = null
    setContent {
      CompositionLocalProvider(LocalViewRegistry provides registry) {
        MaterialTheme { PatientListScreen(onPatientClick = { clickedId = it }) }
      }
    }

    waitUntil(timeoutMillis = 5_000L) {
      onAllNodesWithText("Amina Diallo").fetchSemanticsNodes().isNotEmpty()
    }
    onNodeWithText("Amina Diallo").performClick()
    assertEquals("p1", clickedId)
  }
}
