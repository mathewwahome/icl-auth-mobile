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

import icl.ohs.libs.auth.IclAuthConfig
import icl.ohs.libs.auth.SetNewPasswordScreenConfig
import icl.ohs.libs.auth.model.SetNewPasswordReq
import icl.ohs.libs.auth.network.LoginService
import icl.ohs.libs.auth.network.SetNewPasswordAttemptResult
import icl.ohs.libs.auth.network.buildSetNewPasswordRequestBody
import icl.ohs.libs.auth.network.resolveSetNewPasswordConfig
import icl.ohs.libs.auth.network.validateSetNewPasswordRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest

class SetNewPasswordClientTest {

  @Test
  fun buildSetNewPasswordRequestBody_usesExpectedSchema() {
    val request =
      SetNewPasswordReq(
        temporaryPassword = "temporary-123",
        idNumber = "32645167",
        password = "new-password-456",
      )

    val requestBody = buildSetNewPasswordRequestBody(request)

    assertEquals(
      """{"temporaryPassword":"temporary-123","idNumber":"32645167","password":"new-password-456"}""",
      requestBody,
    )
  }

  @Test
  fun validateSetNewPasswordRequest_rejectsSamePassword() {
    val config =
      resolveSetNewPasswordConfig(
        screenConfig = SetNewPasswordScreenConfig(),
        authConfig = IclAuthConfig(baseAuthUrl = "https://auth.example.com"),
      )

    val failure =
      validateSetNewPasswordRequest(
        config = config,
        request =
          SetNewPasswordReq(
            temporaryPassword = "temporary-123",
            idNumber = "32645167",
            password = "temporary-123",
          ),
      )

    assertEquals(config.messages.samePassword, failure?.message)
  }

  @Test
  fun setNewPassword_returnsSuccessForSuccessfulResponses() = runTest {
    val config =
      resolveSetNewPasswordConfig(
        screenConfig = SetNewPasswordScreenConfig(),
        authConfig = IclAuthConfig(baseAuthUrl = "https://auth.example.com"),
      )
    val client =
      HttpClient(
        MockEngine { request ->
          when {
            request.method == HttpMethod.Post &&
              request.url.encodedPath == "/provider/reset-password" ->
              respond(
                content = """{"status":"success","message":"Password updated"}""",
                status = HttpStatusCode.OK,
                headers =
                  headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
              )

            else -> error("Unexpected request: ${request.method.value} ${request.url}")
          }
        }
      ) {
        expectSuccess = false
      }
    val service = LoginService(client)

    try {
      val result =
        service.setNewPassword(
          config = config,
          request =
            SetNewPasswordReq(
              temporaryPassword = "temporary-123",
              idNumber = "32645167",
              password = "new-password-456",
            ),
        )

      val success = assertIs<SetNewPasswordAttemptResult.Success>(result)
      assertEquals(200, success.value.statusCode)
      assertEquals(
        """{"status":"success","message":"Password updated"}""",
        success.value.responseBody,
      )
    } finally {
      client.close()
    }
  }

  @Test
  fun setNewPassword_returnsServerMessageWhenProvided() = runTest {
    val config =
      resolveSetNewPasswordConfig(
        screenConfig = SetNewPasswordScreenConfig(),
        authConfig = IclAuthConfig(baseAuthUrl = "https://auth.example.com"),
      )
    val client =
      HttpClient(
        MockEngine {
          respond(
            content = """{"message":"Current password is incorrect"}""",
            status = HttpStatusCode.BadRequest,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
          )
        }
      ) {
        expectSuccess = false
      }
    val service = LoginService(client)

    try {
      val result =
        service.setNewPassword(
          config = config,
          request =
            SetNewPasswordReq(
              temporaryPassword = "wrong-current-password",
              idNumber = "32645167",
              password = "new-password-456",
            ),
        )

      val failure = assertIs<SetNewPasswordAttemptResult.Failure>(result)
      assertEquals("Current password is incorrect", failure.value.message)
      assertEquals(400, failure.value.statusCode)
    } finally {
      client.close()
    }
  }
}
