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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.LocalDate

class PatientCardTest {

  private val today = LocalDate(2026, 6, 21)

  @Test
  fun calculateAge_subtractsOne_whenBirthdayNotYetReached() {
    assertEquals("39", calculateAge("1986-12-25", today))
  }

  @Test
  fun calculateAge_full_whenBirthdayPassed() {
    assertEquals("40", calculateAge("1986-01-10", today))
  }

  @Test
  fun calculateAge_full_onBirthday() {
    assertEquals("40", calculateAge("1986-06-21", today))
  }

  @Test
  fun calculateAge_returnsNull_forNullOrMalformed() {
    assertNull(calculateAge(null, today))
    assertNull(calculateAge("not-a-date", today))
    assertNull(calculateAge("1986", today))
  }
}
