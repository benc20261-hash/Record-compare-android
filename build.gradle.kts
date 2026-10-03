// Top-level build file where you can add configuration options common to all sub-projects/modules.
val keystoreFile = file("${rootDir}/debug.keystore")
val base64File = file("${rootDir}/debug.keystore.base64")
if (!keystoreFile.exists() && base64File.exists()) {
  try {
    val decoded = java.util.Base64.getDecoder().decode(base64File.readText().trim())
    keystoreFile.writeBytes(decoded)
  } catch (_: Exception) {}
}

plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.secrets) apply false
  alias(libs.plugins.google.services) apply false
}
