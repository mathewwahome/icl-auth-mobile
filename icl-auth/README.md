# icl-auth

`icl-auth` is a Kotlin Multiplatform auth UI library for Compose Multiplatform apps.

It currently provides:

- A configurable `LoginScreen`
- A configurable `SetNewPasswordScreen` for first-time login flows
- Shared auth initialization through `IclAuth.initialize(...)`
- Built-in login API calls
- Built-in password reset API calls
- Built-in validation and error handling
- Per-app and per-screen configuration

## What the library does

The library is designed around two levels of configuration:

1. App-level auth configuration
   Use this once when your app starts.
   It defines the shared auth base URL and common defaults.

2. Screen-level configuration
   Use this when rendering a specific auth screen such as login.
   It defines the endpoint and UI options for that screen.

This lets your app initialize auth once with a base URL such as:

```text
https://auth.example.com
```

Then a screen can use a relative endpoint such as:

```text
/login
```

The library combines them into:

```text
https://auth.example.com/login
```

## Public API

The main public types are:

- `IclAuth`
- `IclAuthConfig`
- `AuthSession`
- `AuthSessionStore`
- `InMemoryAuthSessionStore`
- `LoginScreen`
- `LoginScreenConfig`
- `LoginMessages`
- `LoginSuccess`
- `LoginTokenResponse`
- `LoginFailure`
- `SetNewPasswordScreen`
- `SetNewPasswordScreenConfig`
- `SetNewPasswordMessages`
- `SetNewPasswordReq`
- `SetNewPasswordSuccess`
- `SetNewPasswordFailure`

## Add the library

In this repository, add the module dependency to your app:

```kotlin
commonMain.dependencies {
  implementation(project(":icl-auth"))
}
```

## Initialize auth once

Initialize the library before showing any auth screen.

### Option 1: Initialize in app startup code

```kotlin
import dev.ohs.player.auth.IclAuth
import dev.ohs.player.auth.IclAuthConfig

fun configureAuth() {
  IclAuth.initialize(
    IclAuthConfig(
      baseAuthUrl = "https://auth.example.com",
      providerProfileEndpoint = "/provider/me",
      sessionStore = InMemoryAuthSessionStore,
    )
  )
}
```

### Option 2: Initialize in your root composable

This is the pattern currently used by the reference app.

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.ohs.player.auth.IclAuth
import dev.ohs.player.auth.IclAuthConfig

private val AUTH_CONFIG =
  IclAuthConfig(
    baseAuthUrl = "https://auth.example.com",
    providerProfileEndpoint = "/provider/me",
    sessionStore = InMemoryAuthSessionStore,
  )

@Composable
fun App() {
  remember(AUTH_CONFIG) { IclAuth.initialize(AUTH_CONFIG) }

  // Rest of your app
}
```

## Configure the login screen

Use `LoginScreenConfig` for screen-level settings.

```kotlin
import dev.ohs.player.auth.LoginScreenConfig

private val LOGIN_CONFIG =
  LoginScreenConfig(
    endpoint = "/login",
    showLogo = true,
    showFooter = true,
    showForgotPassword = true,
  )
```

### Important fields

- `endpoint`
  The screen-specific endpoint.
  This is usually a relative path such as `/login`.

- `showLogo`
  Controls whether the screen shows the built-in auth logo.

- `showFooter`
  Controls whether the footer and terms section are shown.

- `showForgotPassword`
  Controls whether the forgot-password action is shown.

- `usernameFieldName`
  The JSON field name sent for the username.
  Default: `idNumber`

- `passwordFieldName`
  The JSON field name sent for the password.
  Default: `password`

- `requestHeaders`
  Extra headers for this screen only.

- `requestTimeoutMillis`
  Optional screen-level timeout override.

- `responseMessageKeys`
  Optional list of JSON keys to inspect for server error messages.

- `messages`
  Optional screen-level override for user-facing error text.

- `responseMessageResolver`
  Optional custom function for mapping response status and body into a message.

## Configure the password reset screen

Use `SetNewPasswordScreenConfig` for password reset flows, including first-time
login and forgot-password journeys.

```kotlin
import dev.ohs.player.auth.SetNewPasswordScreenConfig

private val SET_NEW_PASSWORD_CONFIG =
  SetNewPasswordScreenConfig(
    endpoint = "/provider/reset-password",
    showLogo = true,
    showFooter = true,
  )
```

### Important fields

- `endpoint`
  The password reset endpoint.
  Default: `/provider/reset-password`

- `showLogo`
  Controls whether the screen shows the built-in auth logo.

- `showFooter`
  Controls whether the footer and terms section are shown.

- `requestHeaders`
  Extra headers for this screen only.

- `requestTimeoutMillis`
  Optional screen-level timeout override.

- `responseMessageKeys`
  Optional list of JSON keys to inspect for server error messages.

- `messages`
  Optional screen-level override for user-facing error text.

- `responseMessageResolver`
  Optional custom function for mapping response status and body into a message.

## Use the login screen

Render `LoginScreen` and react to success or failure.

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.ohs.player.auth.IclAuth
import dev.ohs.player.auth.LoginScreen
import dev.ohs.player.auth.LoginScreenConfig

private val LOGIN_CONFIG =
  LoginScreenConfig(
    endpoint = "/login",
    showLogo = true,
    showFooter = true,
    showForgotPassword = true,
  )

@Composable
fun LaunchScreen() {
  var isLoggedIn by rememberSaveable { mutableStateOf(false) }

  if (isLoggedIn) {
    MainScreen()
  } else {
    LoginScreen(
      config = LOGIN_CONFIG,
      onLoginSuccess = {
        println("Auth header: ${IclAuth.currentAuthorizationHeader()}")
        isLoggedIn = true
      },
      onLoginFailure = { failure ->
        println("Login failed: ${failure.message}")
      },
      onForgotPasswordClick = { currentUsername ->
        // Navigate to your forgot-password flow, optionally using currentUsername
      },
      onTermsAndConditionsClick = {
        // Open terms and conditions
      },
      onPrivacyPolicyClick = {
        // Open the privacy policy
      },
    )
  }
}
```

## Use the password reset screen

Render `SetNewPasswordScreen` when login succeeds with `firstLogin = true`, or
open the same screen from your login screen's forgot-password action.
The screen keeps `initialIdNumber` in the background and only renders the three
password inputs.

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.ohs.player.auth.IclAuth
import dev.ohs.player.auth.LoginScreen
import dev.ohs.player.auth.LoginScreenConfig
import dev.ohs.player.auth.SetNewPasswordScreen
import dev.ohs.player.auth.SetNewPasswordScreenConfig

private val LOGIN_CONFIG = LoginScreenConfig(endpoint = "/provider/login")
private val SET_NEW_PASSWORD_CONFIG = SetNewPasswordScreenConfig()

@Composable
fun LaunchScreen() {
  var isLoggedIn by rememberSaveable { mutableStateOf(false) }
  var firstLoginIdNumber by rememberSaveable { mutableStateOf<String?>(null) }

  when {
    isLoggedIn -> MainScreen()
    firstLoginIdNumber != null ->
      SetNewPasswordScreen(
        config = SET_NEW_PASSWORD_CONFIG,
        initialIdNumber = firstLoginIdNumber.orEmpty(),
        onPasswordResetSuccess = {
          firstLoginIdNumber = null
          isLoggedIn = true
        },
        onBackToLoginClick = {
          firstLoginIdNumber = null
          IclAuth.clearSession()
        },
      )
    else ->
      LoginScreen(
        config = LOGIN_CONFIG,
        onLoginSuccess = { success ->
          if (success.tokenResponse?.firstLogin == true) {
            firstLoginIdNumber = success.username
          } else {
            isLoggedIn = true
          }
        },
        onForgotPasswordClick = { currentUsername ->
          firstLoginIdNumber = currentUsername
        },
      )
  }
}
```

## Session and token handling

The library can keep the login token response as an auth session for reuse by the rest of your app.

When a login call succeeds and the response contains both `access_token` and `token_type`, `icl-auth` automatically:

- The access token
- The token type such as `Bearer`
- The refresh token when available
- The token issue time
- The computed access-token expiry time
- The computed refresh-token expiry time
- Session metadata such as `scope`, `session_state`, `status`, and `firstLogin`
- Sends an authenticated `GET` request to the configured provider profile endpoint
- Stores the parsed provider profile in memory for the current app session

If the login response contains `firstLogin = true`, the library stores the auth
session but skips the provider profile request so your app can route the user to
`SetNewPasswordScreen` first.

### Read the current session

```kotlin
val session = IclAuth.currentSession()
val providerProfile = IclAuth.currentProviderProfile()
val providerUser = IclAuth.currentProviderUser()
val authHeader = IclAuth.currentAuthorizationHeader()
val headers = IclAuth.currentAuthHeaders()
```

`currentAuthorizationHeader()` only returns a value while the access token is still valid.

Example:

```kotlin
val authHeader = IclAuth.currentAuthorizationHeader()
// Result: "Bearer eyJ..."
```

If the token is expired, the helper returns `null` instead of an invalid header.

### Read the session from login success

The login callback also returns the parsed token response and the computed auth session:

```kotlin
LoginScreen(
  config = LOGIN_CONFIG,
  onLoginSuccess = { result ->
    val username = result.username
    val token = result.tokenResponse?.accessToken
    val authHeader = result.session?.authorizationHeader
    val providerUser = result.providerProfile?.user
  },
)
```

### Use a custom session store

By default, the library uses `InMemoryAuthSessionStore`, which keeps the token only for the life of the running app process.

If you need to restore auth after app restart, provide your own `AuthSessionStore` implementation:

```kotlin
class MySessionStore : AuthSessionStore {
  override var session: AuthSession? = null
}
```

Then initialize the library with it:

```kotlin
IclAuth.initialize(
  IclAuthConfig(
    baseAuthUrl = "https://auth.example.com",
    providerProfileEndpoint = "/provider/me",
    sessionStore = MySessionStore(),
  )
)
```

### Clear the stored session

```kotlin
IclAuth.clearSession()
```

This removes the current auth session but keeps the library configuration intact.

## How URL resolution works

### Relative endpoint

If you initialize:

```kotlin
IclAuth.initialize(
  IclAuthConfig(baseAuthUrl = "https://auth.example.com")
)
```

And the screen uses:

```kotlin
LoginScreenConfig(endpoint = "/provider/login")
```

The library sends the request to:

```text
https://auth.example.com/provider/login
```

### Absolute endpoint

If you pass a full URL in `endpoint`, the library uses it directly.

```kotlin
LoginScreenConfig(
  endpoint = "https://staging-auth.example.com/provider/login"
)
```

In that case, `baseAuthUrl` is ignored for that screen.

## How the request body is built

The login request is sent as JSON.

By default, the library sends:

```json
{
  "idNumber": "the-entered-username",
  "password": "the-entered-password"
}
```

The first-time password reset request is also sent as JSON.

```json
{
  "temporaryPassword": "the-entered-current-password",
  "idNumber": "the-user-id-number",
  "password": "the-entered-new-password"
}
```

If your backend expects different field names, configure them:

```kotlin
LoginScreenConfig(
  endpoint = "/login",
  usernameFieldName = "username",
  passwordFieldName = "password"
)
```

That sends:

```json
{
  "username": "the-entered-username",
  "password": "the-entered-password"
}
```

## Global app-level defaults

Use `IclAuthConfig` for defaults shared by all auth screens.

```kotlin
IclAuth.initialize(
  IclAuthConfig(
    baseAuthUrl = "https://auth.example.com",
    defaultRequestHeaders =
      mapOf(
        "X-Client-Id" to "ohs-player",
        "X-Platform" to "android",
      ),
    requestTimeoutMillis = 20_000,
    responseMessageKeys = listOf("message", "error", "detail"),
  )
)
```

### `IclAuthConfig` fields

- `baseAuthUrl`
  Required base URL for relative endpoints.

- `defaultRequestHeaders`
  Headers added to every auth request unless overridden by a screen.

- `requestTimeoutMillis`
  Default timeout for all auth requests.

- `responseMessageKeys`
  Keys the library checks when extracting an error message from a JSON response.

- `messages`
  Default user-facing messages for auth screens.

## Per-screen overrides

Screen settings override global settings where it makes sense.

For example:

```kotlin
IclAuth.initialize(
  IclAuthConfig(
    baseAuthUrl = "https://auth.example.com",
    defaultRequestHeaders = mapOf("X-App" to "reference-app"),
    requestTimeoutMillis = 15_000,
  )
)

val loginConfig =
  LoginScreenConfig(
    endpoint = "/login",
    requestHeaders = mapOf("X-App" to "reference-app-login"),
    requestTimeoutMillis = 30_000,
  )
```

In this example:

- The final URL is `https://auth.example.com/login`
- `X-App` becomes `reference-app-login`
- The screen timeout becomes `30_000`

## Error handling

The library handles several kinds of failures for you.

### Validation errors

These are handled before the request is sent:

- Missing auth base URL and endpoint
- Empty username
- Empty password
- Empty username and password
- Empty current password
- Empty new password
- Empty confirm password
- New password and confirm password mismatch
- Current password and new password being the same

### Network failures

If the request cannot be completed, the user sees the configured network error message.

### Server failures

If the server returns a non-2xx response, the library tries to find a message in the response body.

By default it checks:

- `message`
- `error`
- `detail`

If no usable message is found, the library falls back to the configured
screen-specific messages.

For `LoginScreen`, that means:

- `4xx` returns the configured invalid-credentials message
- `5xx` returns the configured server-error message
- anything else returns the configured unexpected-error message

For `SetNewPasswordScreen`, that means:

- `5xx` returns the configured server-error message
- other unmapped responses return the configured unexpected-error message

## Customize messages

You can customize error messages globally:

```kotlin
IclAuth.initialize(
  IclAuthConfig(
    baseAuthUrl = "https://auth.example.com",
    messages =
      LoginMessages(
        emptyUsername = "ID number is required.",
        emptyPassword = "Password is required.",
        invalidCredentials = "The provided credentials are not valid.",
        networkError = "We could not reach the server. Check your connection and try again.",
      ),
  )
)
```

Or per screen:

```kotlin
val loginConfig =
  LoginScreenConfig(
    endpoint = "/login",
    messages =
      LoginMessages(
        invalidCredentials = "Login failed. Please confirm your ID and password.",
      ),
  )
```

Password reset messages can also be customized per screen:

```kotlin
val setNewPasswordConfig =
  SetNewPasswordScreenConfig(
    messages =
      SetNewPasswordMessages(
        emptyTemporaryPassword = "Current password is required.",
        emptyPassword = "New password is required.",
        emptyConfirmPassword = "Please confirm your new password.",
        passwordMismatch = "The new passwords do not match.",
      ),
  )
```

## Custom response message mapping

If your backend uses a special error format, use `responseMessageResolver`.

```kotlin
val loginConfig =
  LoginScreenConfig(
    endpoint = "/login",
    responseMessageResolver = { statusCode, responseBody ->
      when {
        statusCode == 423 -> "Your account is locked. Please contact support."
        statusCode == 429 -> "Too many attempts. Please wait and try again."
        else -> null
      }
    },
  )
```

If this function returns a non-blank string, that message is used first.

## Success and failure callbacks

### Success

`onLoginSuccess` returns:

- `statusCode`
- `responseBody`
- `username`
- `tokenResponse`
- `session`
- `providerProfile`

Example:

```kotlin
LoginScreen(
  config = LoginScreenConfig(endpoint = "/login"),
  onLoginSuccess = { success ->
    println(success.statusCode)
    println(success.responseBody)
    println(success.username)
    println(success.tokenResponse?.accessToken)
    println(success.tokenResponse?.refreshToken)
    println(success.session?.authorizationHeader)
    println(success.providerProfile?.user?.idNumber)
  },
)
```

`LoginSuccess` includes:

- `statusCode`
- `responseBody`
- `username`
- `tokenResponse`
- `session`
- `providerProfile`

The library now parses standard token-style fields such as:

- `access_token`
- `refresh_token`
- `expires_in`
- `refresh_expires_in`
- `token_type`
- `session_state`
- `scope`
- `firstLogin`
- `status`

The library still does not persist sessions for you.
Your app remains responsible for:

- navigation after success
- token persistence
- session management

### Password reset success

`onPasswordResetSuccess` returns:

- `statusCode`
- `responseBody`

Example:

```kotlin
SetNewPasswordScreen(
  config = SetNewPasswordScreenConfig(),
  initialIdNumber = "32645167",
  onPasswordResetSuccess = { success ->
    println(success.statusCode)
    println(success.responseBody)
  },
)
```

### Failure

`onLoginFailure` returns:

- `message`
- `statusCode` when available
- `responseBody` when available

Example:

```kotlin
LoginScreen(
  config = LoginScreenConfig(endpoint = "/login"),
  onLoginSuccess = { },
  onLoginFailure = { failure ->
    println(failure.message)
    println(failure.statusCode)
    println(failure.responseBody)
  },
)
```

`onPasswordResetFailure` returns:

- `message`
- `statusCode` when available
- `responseBody` when available

Example:

```kotlin
SetNewPasswordScreen(
  config = SetNewPasswordScreenConfig(),
  initialIdNumber = "32645167",
  onPasswordResetSuccess = { },
  onPasswordResetFailure = { failure ->
    println(failure.message)
    println(failure.statusCode)
    println(failure.responseBody)
  },
)
```

## Headers

The library merges headers in this order:

1. Global headers from `IclAuthConfig.defaultRequestHeaders`
2. Screen headers from `LoginScreenConfig.requestHeaders` or `SetNewPasswordScreenConfig.requestHeaders`

If the same key exists in both, the screen value wins.

## Reconfiguring auth

If you need to switch environments at runtime:

```kotlin
IclAuth.initialize(
  IclAuthConfig(baseAuthUrl = "https://staging-auth.example.com")
)
```

If you need to clear configuration:

```kotlin
IclAuth.clear()
```

## Example: reference-app style setup

```kotlin
private val AUTH_CONFIG =
  IclAuthConfig(
    baseAuthUrl = "https://auth.nphiis.health.go.ke",
  )

private val LOGIN_CONFIG =
  LoginScreenConfig(
    endpoint = "/provider/login",
    showLogo = true,
    showFooter = true,
    showForgotPassword = true,
  )

private val SET_NEW_PASSWORD_CONFIG = SetNewPasswordScreenConfig()

@Composable
fun App() {
  remember(AUTH_CONFIG) { IclAuth.initialize(AUTH_CONFIG) }

  var isLoggedIn by rememberSaveable { mutableStateOf(false) }
  var firstLoginIdNumber by rememberSaveable { mutableStateOf<String?>(null) }

  when {
    isLoggedIn -> MainScreen()
    firstLoginIdNumber != null ->
      SetNewPasswordScreen(
        config = SET_NEW_PASSWORD_CONFIG,
        initialIdNumber = firstLoginIdNumber.orEmpty(),
        onPasswordResetSuccess = {
          firstLoginIdNumber = null
          isLoggedIn = true
        },
      )
    else ->
      LoginScreen(
        config = LOGIN_CONFIG,
        onLoginSuccess = { success ->
          if (success.tokenResponse?.firstLogin == true) {
            firstLoginIdNumber = success.username
          } else {
            isLoggedIn = true
          }
        },
        onForgotPasswordClick = { currentUsername ->
          firstLoginIdNumber = currentUsername
        },
        onTermsAndConditionsClick = {},
        onPrivacyPolicyClick = {},
      )
  }
}
```

## Current scope

At the moment, `icl-auth` exposes:

- the login screen flow
- the first-time password reset flow

The same pattern can be reused for future auth components:

- initialize base auth settings once
- pass a screen-level endpoint for each auth screen
- let the library handle request execution and error display

## Troubleshooting

### The screen says auth is not configured

Make sure both are true:

- `IclAuth.initialize(...)` has been called
- `LoginScreenConfig.endpoint` is not blank

### The backend expects a different username key

Set `usernameFieldName` explicitly:

```kotlin
LoginScreenConfig(
  endpoint = "/login",
  usernameFieldName = "username"
)
```

### The backend expects a full URL for one screen

Pass the full URL in `endpoint`:

```kotlin
LoginScreenConfig(
  endpoint = "https://other-auth.example.com/login"
)
```

### I need a custom server error message

Use either:

- `responseMessageKeys`
- `responseMessageResolver`
- `messages`

## Source references

The current implementation lives in:

- [`IclAuth.kt`](./src/libraryCommonMain/kotlin/dev/ohs/player/auth/IclAuth.kt)
- [`LoginModels.kt`](./src/libraryCommonMain/kotlin/dev/ohs/player/auth/LoginModels.kt)
- [`LoginClient.kt`](./src/libraryCommonMain/kotlin/dev/ohs/player/auth/LoginClient.kt)
- [`LoginScreen.kt`](./src/libraryCommonMain/kotlin/dev/ohs/player/auth/LoginScreen.kt)
- [`SetNewPasswordScreen.kt`](./src/libraryCommonMain/kotlin/dev/ohs/player/auth/SetNewPasswordScreen.kt)
