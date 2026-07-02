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
package dev.ohs.player.auth

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest

class LoginClientTest {

  @Test
  fun validateLoginRequest_returnsMissingUrlMessageWhenAuthIsNotInitialized() {
    IclAuth.clear()
    val config = resolveLoginConfig(screenConfig = LoginScreenConfig(endpoint = "/login"))

    val failure = validateLoginRequest(config = config, username = "nurse", password = "secret")

    assertEquals(config.messages.missingLoginUrl, failure?.message)
  }

  @Test
  fun resolveLoginConfig_combinesBaseUrlAndEndpoint() {
    IclAuth.initialize(IclAuthConfig(baseAuthUrl = "https://auth.example.com"))

    val config = resolveLoginConfig(screenConfig = LoginScreenConfig(endpoint = "/provider/login"))

    assertEquals("https://auth.example.com/provider/login", config.loginUrl)
  }

  @Test
  fun buildLoginRequestBody_usesConfiguredFieldNames() {
    val config =
      resolveLoginConfig(
        screenConfig =
          LoginScreenConfig(
            endpoint = "/login",
            usernameFieldName = "email",
            passwordFieldName = "pin",
          ),
        authConfig = IclAuthConfig(baseAuthUrl = "https://auth.example.com"),
      )

    val requestBody = buildLoginRequestBody(config = config, username = "demo", password = "1234")

    assertEquals("""{"email":"demo","pin":"1234"}""", requestBody)
  }

  @Test
  fun login_returnsSuccessForSuccessfulResponses() = runTest {
    val config =
      resolveLoginConfig(
        screenConfig = LoginScreenConfig(endpoint = "/login"),
        authConfig = IclAuthConfig(baseAuthUrl = "https://auth.example.com"),
      )
    val client =
      HttpClient(
        MockEngine {
          respond(
            content = """{"token":"abc123"}""",
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
          )
        }
      ) {
        expectSuccess = false
      }
    val service = LoginService(client)

    try {
      val result = service.login(config = config, username = "nurse", password = "secret")

      val success = assertIs<LoginAttemptResult.Success>(result)
      assertEquals(200, success.value.statusCode)
      assertEquals("""{"token":"abc123"}""", success.value.responseBody)
    } finally {
      client.close()
    }
  }

  @Test
  fun login_returnsServerMessageWhenProvided() = runTest {
    val config =
      resolveLoginConfig(
        screenConfig = LoginScreenConfig(endpoint = "/login"),
        authConfig = IclAuthConfig(baseAuthUrl = "https://auth.example.com"),
      )
    val client =
      HttpClient(
        MockEngine {
          respond(
            content = """{"message":"Credentials rejected by server"}""",
            status = HttpStatusCode.Unauthorized,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
          )
        }
      ) {
        expectSuccess = false
      }
    val service = LoginService(client)

    try {
      val result = service.login(config = config, username = "nurse", password = "wrong")

      val failure = assertIs<LoginAttemptResult.Failure>(result)
      assertEquals("Credentials rejected by server", failure.value.message)
      assertEquals(401, failure.value.statusCode)
    } finally {
      client.close()
    }
  }
}
