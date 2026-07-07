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
package dev.ohs.player.reference.app

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.savedstate.read
import dev.ohs.player.reference.app.feature.web.WebContentScreen
import icl.ohs.libs.auth.ForgotPasswordScreen
import icl.ohs.libs.auth.ForgotPasswordScreenConfig
import icl.ohs.libs.auth.IclAuth
import icl.ohs.libs.auth.LoginScreen
import icl.ohs.libs.auth.LoginScreenConfig
import icl.ohs.libs.auth.ResetPasswordScreen
import icl.ohs.libs.auth.ResetPasswordScreenConfig
import icl.ohs.libs.auth.SetNewPasswordScreen
import icl.ohs.libs.auth.SetNewPasswordScreenConfig

private const val AUTH_LOGIN_ROUTE = "auth/login"
private const val AUTH_FORGOT_PASSWORD_ROUTE = "auth/forgot-password"
private const val AUTH_SET_NEW_PASSWORD_ROUTE = "auth/set-new-password"
private const val AUTH_RESET_PASSWORD_ROUTE = "auth/reset-password"
private const val AUTH_WEB_CONTENT_ROUTE = "auth/web-content"
private const val FORGOT_PASSWORD_IDENTIFIER_ARG = "identifier"
private const val SET_NEW_PASSWORD_ID_NUMBER_ARG = "idNumber"
private const val RESET_PASSWORD_IDENTIFIER_ARG = "identifier"
private const val WEB_LINK_KEY_ARG = "linkKey"

private data class AppWebLink(val key: String, val title: String, val url: String)

private data class AuthWebLinksConfig(
  val termsAndConditions: AppWebLink,
  val privacyPolicy: AppWebLink? = null,
  val additionalLinks: List<AppWebLink> = emptyList(),
) {
  fun findByKey(key: String?): AppWebLink? =
    allLinks().firstOrNull { it.key == key } ?: allLinks().firstOrNull()

  private fun allLinks(): List<AppWebLink> = buildList {
    add(termsAndConditions)
    privacyPolicy?.let(::add)
    addAll(additionalLinks)
  }
}

private val LOGIN_SCREEN_CONFIG =
  LoginScreenConfig(
    endpoint = "/provider/login",
    showLogo = true,
    showFooter = true,
    showForgotPassword = true,
  )

private val FORGOT_PASSWORD_SCREEN_CONFIG = ForgotPasswordScreenConfig()

private val SET_NEW_PASSWORD_SCREEN_CONFIG = SetNewPasswordScreenConfig(showFooter = true)
private val RESET_PASSWORD_SCREEN_CONFIG = ResetPasswordScreenConfig(showFooter = true)

private val AUTH_WEB_LINKS =
  AuthWebLinksConfig(
    // Replace these placeholder URLs with your deployed legal and policy pages.
    termsAndConditions =
      AppWebLink(
        key = "terms-and-conditions",
        title = "Terms and Conditions",
        url = "https://example.com/terms-and-conditions",
      ),
    privacyPolicy =
      AppWebLink(
        key = "privacy-policy",
        title = "Privacy Policy",
        url = "https://example.com/privacy-policy",
      ),
  )

@Composable
fun AuthNavigation(onAuthenticated: () -> Unit) {
  val navController = rememberNavController()

  NavHost(navController = navController, startDestination = AUTH_LOGIN_ROUTE) {
    composable(AUTH_LOGIN_ROUTE) {
      LoginScreen(
        config = LOGIN_SCREEN_CONFIG,
        onLoginSuccess = { success ->
          if (success.tokenResponse?.firstLogin == true) {
            navController.navigateToSetNewPassword(
              initialIdNumber =
                success.providerProfile?.user?.idNumber?.takeIf(String::isNotBlank)
                  ?: success.username.orEmpty()
            )
          } else {
            onAuthenticated()
          }
        },
        onForgotPasswordClick = { username ->
          navController.navigateToForgotPassword(initialIdentifier = username)
        },
        onTermsAndConditionsClick = {
          navController.navigateToWebLink(AUTH_WEB_LINKS.termsAndConditions)
        },
        onPrivacyPolicyClick =
          AUTH_WEB_LINKS.privacyPolicy?.let { link -> { navController.navigateToWebLink(link) } },
      )
    }

    composable(
      route =
        "$AUTH_FORGOT_PASSWORD_ROUTE?$FORGOT_PASSWORD_IDENTIFIER_ARG={$FORGOT_PASSWORD_IDENTIFIER_ARG}",
      arguments =
        listOf(
          navArgument(FORGOT_PASSWORD_IDENTIFIER_ARG) {
            type = NavType.StringType
            defaultValue = ""
          }
        ),
    ) { backStackEntry ->
      val initialIdentifier =
        backStackEntry.arguments?.read { getString(FORGOT_PASSWORD_IDENTIFIER_ARG) }.orEmpty()

      ForgotPasswordScreen(
        config = FORGOT_PASSWORD_SCREEN_CONFIG,
        initialIdentifier = initialIdentifier,
        onSubmit = {
          // Hook the actual forgot-password backend call here when the endpoint is available.
          Result.success(Unit)
        },
        onIAlreadyHaveCodeClick = { identifier ->
          navController.navigateToResetPassword(identifier = identifier)
        },
        onBackToLoginClick = { navController.popBackStack() },
      )
    }

    composable(
      route =
        "$AUTH_SET_NEW_PASSWORD_ROUTE?$SET_NEW_PASSWORD_ID_NUMBER_ARG={$SET_NEW_PASSWORD_ID_NUMBER_ARG}",
      arguments =
        listOf(
          navArgument(SET_NEW_PASSWORD_ID_NUMBER_ARG) {
            type = NavType.StringType
            defaultValue = ""
          }
        ),
    ) { backStackEntry ->
      val initialIdNumber =
        backStackEntry.arguments?.read { getString(SET_NEW_PASSWORD_ID_NUMBER_ARG) }.orEmpty()

      SetNewPasswordScreen(
        config = SET_NEW_PASSWORD_SCREEN_CONFIG,
        initialIdNumber = initialIdNumber,
        onPasswordResetSuccess = {
          IclAuth.clearSession()
          onAuthenticated()
        },
        onBackToLoginClick = {
          IclAuth.clearSession()
          navController.popBackStack(AUTH_LOGIN_ROUTE, inclusive = false)
        },
        onTermsAndConditionsClick = {
          navController.navigateToWebLink(AUTH_WEB_LINKS.termsAndConditions)
        },
        onPrivacyPolicyClick =
          AUTH_WEB_LINKS.privacyPolicy?.let { link -> { navController.navigateToWebLink(link) } },
      )
    }

    composable(
      route =
        "$AUTH_RESET_PASSWORD_ROUTE?$RESET_PASSWORD_IDENTIFIER_ARG={$RESET_PASSWORD_IDENTIFIER_ARG}",
      arguments =
        listOf(
          navArgument(RESET_PASSWORD_IDENTIFIER_ARG) {
            type = NavType.StringType
            defaultValue = ""
          }
        ),
    ) { backStackEntry ->
      val identifier =
        backStackEntry.arguments?.read { getString(RESET_PASSWORD_IDENTIFIER_ARG) }.orEmpty()

      ResetPasswordScreen(
        config = RESET_PASSWORD_SCREEN_CONFIG,
        identifier = identifier,
        onPasswordResetSuccess = {
          IclAuth.clearSession()
          navController.popBackStack(AUTH_LOGIN_ROUTE, inclusive = false)
        },
        onBackToLoginClick = {
          IclAuth.clearSession()
          navController.popBackStack(AUTH_LOGIN_ROUTE, inclusive = false)
        },
        onTermsAndConditionsClick = {
          navController.navigateToWebLink(AUTH_WEB_LINKS.termsAndConditions)
        },
      )
    }

    composable(
      route = "$AUTH_WEB_CONTENT_ROUTE?$WEB_LINK_KEY_ARG={$WEB_LINK_KEY_ARG}",
      arguments =
        listOf(
          navArgument(WEB_LINK_KEY_ARG) {
            type = NavType.StringType
            defaultValue = AUTH_WEB_LINKS.termsAndConditions.key
          }
        ),
    ) { backStackEntry ->
      val linkKey = backStackEntry.arguments?.read { getString(WEB_LINK_KEY_ARG) }
      val link = AUTH_WEB_LINKS.findByKey(linkKey) ?: AUTH_WEB_LINKS.termsAndConditions

      WebContentScreen(
        title = link.title,
        url = link.url,
        onBack = { navController.popBackStack() },
      )
    }
  }
}

private fun androidx.navigation.NavController.navigateToSetNewPassword(initialIdNumber: String) {
  navigate("$AUTH_SET_NEW_PASSWORD_ROUTE?$SET_NEW_PASSWORD_ID_NUMBER_ARG=${initialIdNumber.trim()}")
}

private fun androidx.navigation.NavController.navigateToResetPassword(identifier: String) {
  navigate("$AUTH_RESET_PASSWORD_ROUTE?$RESET_PASSWORD_IDENTIFIER_ARG=${identifier.trim()}")
}

private fun androidx.navigation.NavController.navigateToForgotPassword(initialIdentifier: String) {
  navigate(
    "$AUTH_FORGOT_PASSWORD_ROUTE?$FORGOT_PASSWORD_IDENTIFIER_ARG=${initialIdentifier.trim()}"
  )
}

private fun androidx.navigation.NavController.navigateToWebLink(link: AppWebLink) {
  navigate("$AUTH_WEB_CONTENT_ROUTE?$WEB_LINK_KEY_ARG=${link.key}")
}
