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
package dev.ohs.player.reference.app.feature.group.list

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import dev.ohs.player.generated.config.GroupCardConfig
import dev.ohs.player.generated.state.GroupListState
import dev.ohs.player.generated.viewtype.ViewTypeCS
import dev.ohs.player.library.layout.VerticalListRenderer
import dev.ohs.player.library.registry.ViewRegistry
import dev.ohs.player.library.registry.registerComponent
import dev.ohs.player.library.registry.registerLayout

fun ViewRegistry.registerGroupList() {
  registerComponent<GroupListState, GroupCardConfig>(
    ViewTypeCS.GroupCard,
    GroupCardRenderer(),
    GroupCardConfig(),
  )
  registerLayout<GroupListState>(
    VerticalListRenderer.VIEW_TYPE,
    VerticalListRenderer(contentPadding = PaddingValues(16.dp), itemSpacing = 12.dp),
  )
}
