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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import dev.ohs.player.generated.config.GroupHeaderConfig
import dev.ohs.player.generated.config.MemberItemConfig
import dev.ohs.player.generated.config.SectionCardConfig
import dev.ohs.player.generated.state.GroupHeaderState
import dev.ohs.player.generated.state.GroupMemberState
import dev.ohs.player.generated.viewtype.ViewTypeCS
import dev.ohs.player.library.registry.ViewRegistry
import dev.ohs.player.library.registry.registerComponent
import dev.ohs.player.library.registry.registerLayout
import dev.ohs.player.reference.app.feature.component.common.SectionCardLayoutRenderer

fun ViewRegistry.registerGroupProfile() {
  registerComponent<GroupHeaderState, GroupHeaderConfig>(
    ViewTypeCS.GroupHeader,
    GroupHeaderRenderer(),
    GroupHeaderConfig(),
  )
  registerComponent<GroupMemberState, MemberItemConfig>(
    ViewTypeCS.MemberItem,
    MemberItemRenderer(),
    MemberItemConfig(),
  )
  registerLayout<GroupMemberState>(
    ViewTypeCS.SectionCard,
    SectionCardLayoutRenderer(
      title = "Members",
      icon = Icons.Default.Person,
      config = SectionCardConfig(collapsible = true),
    ),
  )
}
