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
package icl.ohs.libs.auth.network

import icl.ohs.libs.auth.IclAuth
import icl.ohs.libs.auth.IclAuthConfig
import icl.ohs.libs.auth.LoginScreenConfig
import icl.ohs.libs.auth.ResetPasswordScreenConfig
import icl.ohs.libs.auth.SetNewPasswordScreenConfig
import icl.ohs.libs.auth.model.AuthSessionStore
import icl.ohs.libs.auth.model.LoginMessages
import icl.ohs.libs.auth.model.ResetPasswordMessages
import icl.ohs.libs.auth.model.SetNewPasswordMessages

/**
 * Merges a screen-level config with the app-wide [IclAuthConfig] into the concrete values a network
 * request needs (resolved URL, headers, timeouts, messages). Screens/view models never build
 * requests directly off [IclAuthConfig] - they go through these resolvers so the merge logic lives
 * in one place.
 */
internal data class ResolvedLoginConfig(
  val loginUrl: String?,
  val providerProfileUrl: String?,
  val requestHeaders: Map<String, String>,
  val usernameFieldName: String,
  val passwordFieldName: String,
  val requestTimeoutMillis: Long,
  val responseMessageKeys: List<String>,
  val messages: LoginMessages,
  val responseMessageResolver: ((statusCode: Int, responseBody: String) -> String?)?,
  val sessionStore: AuthSessionStore?,
)

internal data class ResolvedSetNewPasswordConfig(
  val resetPasswordUrl: String?,
  val requestHeaders: Map<String, String>,
  val requestTimeoutMillis: Long,
  val responseMessageKeys: List<String>,
  val messages: SetNewPasswordMessages,
  val responseMessageResolver: ((statusCode: Int, responseBody: String) -> String?)?,
)

internal data class ResolvedResetPasswordConfig(
  val resetPasswordUrl: String?,
  val requestHeaders: Map<String, String>,
  val minPasswordLength: Int,
  val requestTimeoutMillis: Long,
  val responseMessageKeys: List<String>,
  val messages: ResetPasswordMessages,
  val responseMessageResolver: ((statusCode: Int, responseBody: String) -> String?)?,
)

internal fun resolveLoginConfig(
  screenConfig: LoginScreenConfig,
  authConfig: IclAuthConfig? = IclAuth.currentConfiguration(),
): ResolvedLoginConfig =
  ResolvedLoginConfig(
    loginUrl =
      resolveAuthUrl(baseAuthUrl = authConfig?.baseAuthUrl, endpoint = screenConfig.endpoint),
    providerProfileUrl =
      resolveAuthUrl(
        baseAuthUrl = authConfig?.baseAuthUrl,
        endpoint = authConfig?.providerProfileEndpoint.orEmpty(),
      ),
    requestHeaders = authConfig?.defaultRequestHeaders.orEmpty() + screenConfig.requestHeaders,
    usernameFieldName = screenConfig.usernameFieldName,
    passwordFieldName = screenConfig.passwordFieldName,
    requestTimeoutMillis =
      screenConfig.requestTimeoutMillis ?: authConfig?.requestTimeoutMillis ?: 15_000,
    responseMessageKeys =
      screenConfig.responseMessageKeys
        ?: authConfig?.responseMessageKeys
        ?: listOf("message", "error", "detail"),
    messages = screenConfig.messages ?: authConfig?.messages ?: LoginMessages(),
    responseMessageResolver = screenConfig.responseMessageResolver,
    sessionStore = authConfig?.sessionStore,
  )

internal fun resolveSetNewPasswordConfig(
  screenConfig: SetNewPasswordScreenConfig,
  authConfig: IclAuthConfig? = IclAuth.currentConfiguration(),
): ResolvedSetNewPasswordConfig =
  ResolvedSetNewPasswordConfig(
    resetPasswordUrl =
      resolveAuthUrl(baseAuthUrl = authConfig?.baseAuthUrl, endpoint = screenConfig.endpoint),
    requestHeaders = authConfig?.defaultRequestHeaders.orEmpty() + screenConfig.requestHeaders,
    requestTimeoutMillis =
      screenConfig.requestTimeoutMillis ?: authConfig?.requestTimeoutMillis ?: 15_000,
    responseMessageKeys =
      screenConfig.responseMessageKeys
        ?: authConfig?.responseMessageKeys
        ?: listOf("message", "error", "detail"),
    messages = screenConfig.messages ?: SetNewPasswordMessages(),
    responseMessageResolver = screenConfig.responseMessageResolver,
  )

internal fun resolveResetPasswordConfig(
  screenConfig: ResetPasswordScreenConfig,
  authConfig: IclAuthConfig? = IclAuth.currentConfiguration(),
): ResolvedResetPasswordConfig =
  ResolvedResetPasswordConfig(
    resetPasswordUrl =
      resolveAuthUrl(baseAuthUrl = authConfig?.baseAuthUrl, endpoint = screenConfig.endpoint),
    requestHeaders = authConfig?.defaultRequestHeaders.orEmpty() + screenConfig.requestHeaders,
    minPasswordLength = screenConfig.minPasswordLength,
    requestTimeoutMillis =
      screenConfig.requestTimeoutMillis ?: authConfig?.requestTimeoutMillis ?: 15_000,
    responseMessageKeys =
      screenConfig.responseMessageKeys
        ?: authConfig?.responseMessageKeys
        ?: listOf("message", "error", "detail"),
    messages = screenConfig.messages ?: ResetPasswordMessages(),
    responseMessageResolver = screenConfig.responseMessageResolver,
  )

internal fun resolveAuthUrl(baseAuthUrl: String?, endpoint: String): String? {
  val normalizedEndpoint = endpoint.trim()
  if (normalizedEndpoint.isBlank()) {
    return null
  }

  if (
    normalizedEndpoint.startsWith(prefix = "https://", ignoreCase = true) ||
      normalizedEndpoint.startsWith(prefix = "http://", ignoreCase = true)
  ) {
    return normalizedEndpoint
  }

  val normalizedBase = baseAuthUrl?.trim()?.takeIf(String::isNotBlank) ?: return null
  return normalizedBase.removeSuffix("/") + "/" + normalizedEndpoint.removePrefix("/")
}
