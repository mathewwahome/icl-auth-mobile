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

import dev.ohs.player.generated.state.GroupHeaderState
import dev.ohs.player.generated.state.GroupListState
import dev.ohs.player.generated.state.GroupMemberState
import dev.ohs.player.reference.app.data.Extraction.extractor
import dev.ohs.player.reference.app.data.datasource.groupListSearchResults
import dev.ohs.player.reference.app.data.datasource.groupProfileSearchResult
import dev.ohs.player.reference.app.feature.group.profile.GroupProfileUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GroupRepository {

  // FhirPathEvaluator is not concurrent-safe. limitedParallelism(1) serializes all extraction on a
  // single background thread without any explicit locking.
  private val extractorDispatcher = Dispatchers.Default.limitedParallelism(1)

  suspend fun getGroups(): List<GroupListState> =
    withContext(extractorDispatcher) {
      groupListSearchResults().flatMap { extractor.extract<GroupListState>(it) }
    }

  suspend fun getGroupProfile(groupId: String): GroupProfileUiState =
    withContext(extractorDispatcher) {
      val result = groupProfileSearchResult(groupId) ?: return@withContext GroupProfileUiState()
      GroupProfileUiState(
        groupHeader = extractor.extract<GroupHeaderState>(result).firstOrNull(),
        members = extractor.extract<GroupMemberState>(result),
      )
    }
}
