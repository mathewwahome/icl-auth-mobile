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

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val OhsPrimary = Color(0xFF0B57D0)
private val OhsOnPrimary = Color.White
private val OhsPrimaryContainer = Color(0xFFD3E3FD)
private val OhsOnPrimaryContainer = Color(0xFF041E49)

private val OhsSecondary = Color(0xFF00639B)
private val OhsOnSecondary = Color.White
private val OhsSecondaryContainer = Color(0xFFC2E7FF)
private val OhsOnSecondaryContainer = Color(0xFF001E31)

private val OhsTertiary = Color(0xFF146C2E)
private val OhsOnTertiary = Color.White
private val OhsTertiaryContainer = Color(0xFFC4EED0)
private val OhsOnTertiaryContainer = Color(0xFF072711)

private val OhsError = Color(0xFFB3261E)
private val OhsOnError = Color.White
private val OhsErrorContainer = Color(0xFFF9DEDC)
private val OhsOnErrorContainer = Color(0xFF601410)

private val OhsBackground = Color(0xFFFFFFFF)
private val OhsSurface = Color(0xFFFFFFFF)
private val OhsOnSurface = Color(0xFF1F1F1F)
private val OhsSurfaceVariant = Color(0xFFE1E3F8)
private val OhsOnSurfaceVariant = Color(0xFF45464F)
private val OhsOutline = Color(0xFF757680)

private val OhsLightColorScheme =
  lightColorScheme(
    primary = OhsPrimary,
    onPrimary = OhsOnPrimary,
    primaryContainer = OhsPrimaryContainer,
    onPrimaryContainer = OhsOnPrimaryContainer,
    secondary = OhsSecondary,
    onSecondary = OhsOnSecondary,
    secondaryContainer = OhsSecondaryContainer,
    onSecondaryContainer = OhsOnSecondaryContainer,
    tertiary = OhsTertiary,
    onTertiary = OhsOnTertiary,
    tertiaryContainer = OhsTertiaryContainer,
    onTertiaryContainer = OhsOnTertiaryContainer,
    error = OhsError,
    onError = OhsOnError,
    errorContainer = OhsErrorContainer,
    onErrorContainer = OhsOnErrorContainer,
    background = OhsBackground,
    onBackground = OhsOnSurface,
    surface = OhsSurface,
    onSurface = OhsOnSurface,
    surfaceVariant = OhsSurfaceVariant,
    onSurfaceVariant = OhsOnSurfaceVariant,
    outline = OhsOutline,
  )

private val OhsDarkColorScheme =
  darkColorScheme(
    primary = Color(0xFFA8C7FA),
    onPrimary = Color(0xFF0842A0),
    primaryContainer = Color(0xFF0842A0),
    onPrimaryContainer = Color(0xFFD3E3FD),
    secondary = Color(0xFF7FCFFF),
    onSecondary = Color(0xFF004A77),
    secondaryContainer = Color(0xFF004A77),
    onSecondaryContainer = Color(0xFFC2E7FF),
    tertiary = Color(0xFF91D5A3),
    onTertiary = Color(0xFF0F5223),
    tertiaryContainer = Color(0xFF0F5223),
    onTertiaryContainer = Color(0xFFC4EED0),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF1F1F1F),
    onBackground = Color(0xFFE3E3E3),
    surface = Color(0xFF1F1F1F),
    onSurface = Color(0xFFE3E3E3),
    surfaceVariant = Color(0xFF45464F),
    onSurfaceVariant = Color(0xFFC5C6D0),
    outline = Color(0xFF8F9099),
  )

@Composable
fun OhsPlayerTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
  val colorScheme = if (darkTheme) OhsDarkColorScheme else OhsLightColorScheme
  MaterialTheme(colorScheme = colorScheme, content = content)
}
