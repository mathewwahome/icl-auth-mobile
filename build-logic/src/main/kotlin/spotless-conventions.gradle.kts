import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
  id("com.diffplug.spotless")
}

val ktfmtVersion: String =
  extensions
    .getByType(VersionCatalogsExtension::class.java)
    .named("libs")
    .findVersion("ktfmt")
    .get()
    .requiredVersion

val licenseText: String =
  checkNotNull(
      Thread.currentThread().contextClassLoader.getResourceAsStream("license-header.txt")
    )
    .use { it.reader().readText() }

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
  ratchetFrom = "origin/main"
  kotlin {
    target("src/**/*.kt")
    ktfmt(ktfmtVersion).googleStyle()
    licenseHeader(licenseText)
  }
  kotlinGradle {
    target("*.gradle.kts")
    ktfmt(ktfmtVersion).googleStyle()
    licenseHeader(licenseText, "(^(?![\\/ ]\\*).*\$)")
  }
}
