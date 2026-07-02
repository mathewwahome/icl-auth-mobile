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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ohs.player.generated.state.GroupHeaderState
import dev.ohs.player.generated.state.GroupMemberState
import dev.ohs.player.reference.app.data.repository.GroupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GroupProfileUiState(
  val groupHeader: GroupHeaderState? = null,
  val members: List<GroupMemberState> = emptyList(),
)

class GroupProfileViewModel(groupId: String) : ViewModel() {
  private val _uiState = MutableStateFlow<GroupProfileUiState?>(null)
  val uiState: StateFlow<GroupProfileUiState?> = _uiState.asStateFlow()

  init {
    viewModelScope.launch { _uiState.value = GroupRepository.getGroupProfile(groupId) }
  }
}
