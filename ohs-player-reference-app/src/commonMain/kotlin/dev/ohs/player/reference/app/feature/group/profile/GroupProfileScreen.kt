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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import dev.ohs.player.generated.state.GroupHeaderState
import dev.ohs.player.generated.state.GroupMemberState
import dev.ohs.player.generated.viewtype.ViewTypeCS
import dev.ohs.player.library.registry.LocalViewRegistry
import dev.ohs.player.library.registry.componentRenderer
import dev.ohs.player.library.registry.layoutRenderer
import dev.ohs.player.library.renderer.RenderOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupProfileScreen(groupId: String, onBack: () -> Unit, onMemberClick: (String) -> Unit) {
  val viewModel = viewModel(key = groupId) { GroupProfileViewModel(groupId) }
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val registry = LocalViewRegistry.current

  val headerRenderer =
    remember(registry) { registry.componentRenderer<GroupHeaderState>(ViewTypeCS.GroupHeader) }
  val memberRenderer =
    remember(registry) { registry.componentRenderer<GroupMemberState>(ViewTypeCS.MemberItem) }
  val memberSectionLayout =
    remember(registry) { registry.layoutRenderer<GroupMemberState>(ViewTypeCS.SectionCard) }

  val groupName = state?.groupHeader?.groupName ?: "Household"

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(groupName) },
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

    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(padding),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      s.groupHeader?.let { header ->
        item(key = "group_header") { headerRenderer.Render(header, RenderOptions()) }
      }

      if (s.members.isNotEmpty()) {
        item(key = "members_section") {
          memberSectionLayout.Render(
            items = s.members,
            component = memberRenderer,
            onItemClick = { member -> member.memberId?.let { onMemberClick(it) } },
          )
        }
      }
    }
  }
}
