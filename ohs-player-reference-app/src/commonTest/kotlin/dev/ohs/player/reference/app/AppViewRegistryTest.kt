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
package dev.ohs.player.reference.app

import dev.ohs.player.generated.state.GroupListState
import dev.ohs.player.generated.state.GroupMemberState
import dev.ohs.player.generated.state.PatientAllergyState
import dev.ohs.player.generated.state.PatientConditionState
import dev.ohs.player.generated.state.PatientImmunizationState
import dev.ohs.player.generated.state.PatientMedicationState
import dev.ohs.player.generated.state.PatientSummaryState
import dev.ohs.player.generated.viewtype.ViewTypeCS
import dev.ohs.player.library.layout.GridListRenderer
import dev.ohs.player.library.layout.HorizontalListRenderer
import dev.ohs.player.library.layout.VerticalListRenderer
import dev.ohs.player.library.registry.componentRenderer
import dev.ohs.player.library.registry.layoutRenderer
import kotlin.test.Test

class AppViewRegistryTest {

  @Test
  fun allRequiredRenderersAreRegistered() {
    val registry = buildAppViewRegistry()

    // Group list
    registry.componentRenderer<GroupListState>(ViewTypeCS.GroupCard)
    registry.layoutRenderer<GroupListState>(VerticalListRenderer.VIEW_TYPE)

    // Group profile
    registry.componentRenderer<GroupMemberState>(ViewTypeCS.MemberItem)

    // Patient list — component + every layout
    registry.componentRenderer<PatientSummaryState>(ViewTypeCS.PatientCard)
    registry.layoutRenderer<PatientSummaryState>(VerticalListRenderer.VIEW_TYPE)
    registry.layoutRenderer<PatientSummaryState>(HorizontalListRenderer.VIEW_TYPE)
    registry.layoutRenderer<PatientSummaryState>(GridListRenderer.VIEW_TYPE)

    // Patient IPS profile — all IG-authored item types
    registry.componentRenderer<PatientSummaryState>(ViewTypeCS.PatientHeader)
    registry.componentRenderer<PatientAllergyState>(ViewTypeCS.AllergyItem)
    registry.componentRenderer<PatientMedicationState>(ViewTypeCS.MedicationItem)
    registry.componentRenderer<PatientConditionState>(ViewTypeCS.ConditionItem)
    registry.componentRenderer<PatientImmunizationState>(ViewTypeCS.ImmunizationItem)
  }
}
