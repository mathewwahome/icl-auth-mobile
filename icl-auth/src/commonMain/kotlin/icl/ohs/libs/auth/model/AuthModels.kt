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
package icl.ohs.libs.auth.model

import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Domain models for the auth library: the authenticated session, the raw token response it is built
 * from, and the signed-in provider's profile. These are plain data classes with no networking or
 * Compose dependencies - the network layer parses into them, and view models / screens read from
 * them.
 */
data class AuthSession(
  val accessToken: String,
  val tokenType: String,
  val refreshToken: String? = null,
  val issuedAt: Instant,
  val accessTokenExpiresAt: Instant? = null,
  val refreshTokenExpiresAt: Instant? = null,
  val expiresInSeconds: Long? = null,
  val refreshExpiresInSeconds: Long? = null,
  val notBeforePolicy: Long? = null,
  val sessionState: String? = null,
  val scope: String? = null,
  val firstLogin: Boolean? = null,
  val status: String? = null,
) {
  val authorizationHeader: String
    get() = "${tokenType.trim()} ${accessToken.trim()}"

  fun isAccessTokenExpired(now: Instant = Clock.System.now()): Boolean =
    accessTokenExpiresAt?.let { now >= it } ?: false

  fun isAccessTokenValid(now: Instant = Clock.System.now()): Boolean =
    accessToken.isNotBlank() && tokenType.isNotBlank() && !isAccessTokenExpired(now)

  fun isRefreshTokenExpired(now: Instant = Clock.System.now()): Boolean =
    refreshTokenExpiresAt?.let { now >= it } ?: false
}

data class LoginTokenResponse(
  val accessToken: String? = null,
  val expiresIn: Long? = null,
  val refreshExpiresIn: Long? = null,
  val refreshToken: String? = null,
  val tokenType: String? = null,
  val notBeforePolicy: Long? = null,
  val sessionState: String? = null,
  val scope: String? = null,
  val firstLogin: Boolean? = null,
  val status: String? = null,
)

data class ProviderProfile(val status: String? = null, val user: ProviderUser? = null)

data class ProviderUser(
  val firstName: String? = null,
  val lastName: String? = null,
  val fhirPractitionerId: String? = null,
  val practitionerRole: String? = null,
  val role: String? = null,
  val status: Boolean? = null,
  val id: String? = null,
  val idNumber: String? = null,
  val fullNames: String? = null,
  val phone: String? = null,
  val email: String? = null,
  val locationInfo: ProviderLocationInfo? = null,
  val communityHealthUnits: List<String>? = null,
)

data class ProviderLocationInfo(
  val facility: String? = null,
  val facilityName: String? = null,
  val ward: String? = null,
  val wardName: String? = null,
  val subCounty: String? = null,
  val subCountyName: String? = null,
  val county: String? = null,
  val countyName: String? = null,
  val country: String? = null,
  val countryName: String? = null,
)
