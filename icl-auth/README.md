# `icl-auth`

`icl-auth` is a Kotlin Multiplatform auth UI library for Compose Multiplatform
apps.

It currently provides:

- `LoginScreen`
- `ForgotPasswordScreen`
- `ResetPasswordScreen`
- `SetNewPasswordScreen`
- shared auth/session state through `IclAuth`
- built-in login and password reset API handling for the login, reset-password,
  and set-new-password flows

All public types live in the `icl.ohs.libs.auth` package.

## Maven coordinates

Default coordinates for publication from this repository:

```text
io.github.intellisoft-consulting:icl-auth:<version>
```

## Consume from another project

### Maven Local

Publish locally from this repository:

```shell
./gradlew :icl-auth:publishToMavenLocal
```

Then consume it:

```kotlin
repositories {
  mavenLocal()
  google()
  mavenCentral()
}

commonMain.dependencies {
  implementation("io.github.intellisoft-consulting:icl-auth:0.1.0-SNAPSHOT")
}
```

### GitHub Packages

```kotlin
repositories {
  google()
  mavenCentral()
  maven {
    url = uri("https://maven.pkg.github.com/IntelliSOFT-Consulting/icl-auth-mobile")
    credentials {
      username = providers.gradleProperty("gpr.user").orNull
      password = providers.gradleProperty("gpr.key").orNull
    }
  }
}

commonMain.dependencies {
  implementation("io.github.intellisoft-consulting:icl-auth:<version>")
}
```

## Publish from this repository

### Local publish

```shell
./gradlew :icl-auth:publishToMavenLocal
```

### GitHub Packages publish

```shell
export GITHUB_ACTOR=your-github-username
export GITHUB_TOKEN=your-github-token
export VERSION_NAME=0.1.0

./gradlew :icl-auth:publishAllPublicationsToGitHubPackagesRepository
```

Optional Gradle properties/env vars supported by the publish configuration:

- `POM_GROUP_ID`
- `VERSION_NAME`
- `MAVEN_REPOSITORY_URL`
- `MAVEN_USERNAME`
- `MAVEN_PASSWORD`
- `SIGNING_KEY`
- `SIGNING_PASSWORD`

## Initialize auth

Initialize `IclAuth` once before rendering auth screens that rely on built-in
API handling.

```kotlin
import icl.ohs.libs.auth.IclAuth
import icl.ohs.libs.auth.IclAuthConfig
import icl.ohs.libs.auth.model.InMemoryAuthSessionStore

private val AUTH_CONFIG =
  IclAuthConfig(
    baseAuthUrl = "https://auth.example.com",
    providerProfileEndpoint = "/provider/me",
    sessionStore = InMemoryAuthSessionStore,
  )

fun configureAuth() {
  IclAuth.initialize(AUTH_CONFIG)
}
```

## Login example

```kotlin
import androidx.compose.runtime.Composable
import icl.ohs.libs.auth.IclAuth
import icl.ohs.libs.auth.LoginScreen
import icl.ohs.libs.auth.LoginScreenConfig

private val LOGIN_CONFIG = LoginScreenConfig(endpoint = "/login")

@Composable
fun AuthRoute(onLoggedIn: () -> Unit, onForgotPassword: (String) -> Unit) {
  LoginScreen(
    config = LOGIN_CONFIG,
    onLoginSuccess = { success ->
      println(IclAuth.currentAuthorizationHeader())
      println(success.providerProfile?.user?.idNumber)
      onLoggedIn()
    },
    onForgotPasswordClick = onForgotPassword,
  )
}
```

## Forgot password example

`ForgotPasswordScreen` is intentionally transport-agnostic. Your app supplies the
submit callback.

```kotlin
import androidx.compose.runtime.Composable
import icl.ohs.libs.auth.ForgotPasswordScreen

@Composable
fun ForgotPasswordRoute(
  identifier: String,
  onBackToLogin: () -> Unit,
  onIAlreadyHaveCode: (String) -> Unit,
  sendResetLink: suspend (String) -> Result<Unit>,
) {
  ForgotPasswordScreen(
    initialIdentifier = identifier,
    onSubmit = sendResetLink,
    onBackToLoginClick = onBackToLogin,
    onIAlreadyHaveCodeClick = onIAlreadyHaveCode,
  )
}
```

## Reset password example

```kotlin
import androidx.compose.runtime.Composable
import icl.ohs.libs.auth.ResetPasswordScreen
import icl.ohs.libs.auth.ResetPasswordScreenConfig

private val RESET_PASSWORD_CONFIG = ResetPasswordScreenConfig()

@Composable
fun ResetPasswordRoute(identifier: String, onDone: () -> Unit) {
  ResetPasswordScreen(
    config = RESET_PASSWORD_CONFIG,
    identifier = identifier,
    onPasswordResetSuccess = { onDone() },
  )
}
```

## First-login password reset example

```kotlin
import androidx.compose.runtime.Composable
import icl.ohs.libs.auth.SetNewPasswordScreen
import icl.ohs.libs.auth.SetNewPasswordScreenConfig

private val SET_NEW_PASSWORD_CONFIG = SetNewPasswordScreenConfig()

@Composable
fun FirstLoginRoute(idNumber: String, onDone: () -> Unit, onBack: () -> Unit) {
  SetNewPasswordScreen(
    config = SET_NEW_PASSWORD_CONFIG,
    initialIdNumber = idNumber,
    onPasswordResetSuccess = { onDone() },
    onBackToLoginClick = onBack,
  )
}
```

## Session helpers

`IclAuth` exposes helpers for reading the current auth state:

```kotlin
val session = IclAuth.currentSession()
val providerProfile = IclAuth.currentProviderProfile()
val providerUser = IclAuth.currentProviderUser()
val authHeader = IclAuth.currentAuthorizationHeader()
val authHeaders = IclAuth.currentAuthHeaders()
```

Clear session state without dropping configuration:

```kotlin
IclAuth.clearSession()
```

Clear both session state and configuration:

```kotlin
IclAuth.clear()
```

## Custom session storage

Provide your own `AuthSessionStore` if you want to integrate persistence:

```kotlin
import icl.ohs.libs.auth.model.AuthSession
import icl.ohs.libs.auth.model.AuthSessionStore

class MySessionStore : AuthSessionStore {
  override var session: AuthSession? = null
}
```

Then pass it into `IclAuthConfig`.

## Notes

- Relative endpoints are resolved against `IclAuthConfig.baseAuthUrl`.
- Absolute endpoints are used as-is.
- `LoginScreen` and the reset flows surface server messages when they can extract
  them from the response body.
- `ForgotPasswordScreen` does not perform network I/O by itself; the host app
  owns that callback.
