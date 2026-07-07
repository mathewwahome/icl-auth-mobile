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
package icl.ohs.libs.auth

import kotlin.time.Clock
import kotlin.time.Instant

object IclAuth {

  private var configuration: IclAuthConfig? = null
  private var providerProfile: ProviderProfile? = null
  private val sessionStore: AuthSessionStore?
    get() = configuration?.sessionStore

  val isInitialized: Boolean
    get() = configuration != null

  val hasSession: Boolean
    get() = currentSession() != null

  val hasProviderProfile: Boolean
    get() = providerProfile != null

  fun initialize(config: IclAuthConfig) {
    configuration = config
  }

  fun clearSession() {
    sessionStore?.session = null
    providerProfile = null
  }

  fun clear() {
    clearSession()
    configuration = null
  }

  fun currentSession(): AuthSession? = sessionStore?.session

  fun currentProviderProfile(): ProviderProfile? = providerProfile

  fun currentProviderUser(): ProviderUser? = providerProfile?.user

  fun hasValidAccessToken(now: Instant = Clock.System.now()): Boolean =
    currentValidSession(now) != null

  fun currentAccessToken(now: Instant = Clock.System.now()): String? =
    currentValidSession(now)?.accessToken

  fun currentTokenType(now: Instant = Clock.System.now()): String? =
    currentValidSession(now)?.tokenType

  fun currentAuthorizationHeader(now: Instant = Clock.System.now()): String? =
    currentValidSession(now)?.authorizationHeader

  fun currentAuthHeaders(now: Instant = Clock.System.now()): Map<String, String> =
    currentAuthorizationHeader(now)?.let { mapOf("Authorization" to it) }.orEmpty()

  internal fun currentConfiguration(): IclAuthConfig? = configuration

  private fun currentValidSession(now: Instant): AuthSession? =
    currentSession()?.takeIf { it.isAccessTokenValid(now) }

  internal fun updateSession(
    session: AuthSession?,
    sessionStore: AuthSessionStore? = this.sessionStore,
  ) {
    sessionStore?.session = session
  }

  internal fun updateProviderProfile(providerProfile: ProviderProfile?) {
    this.providerProfile = providerProfile
  }
}
