param([string[]]$Tasks = @(':app:assembleDebug', ':app:testDebugUnitTest', ':app:lintDebug'))
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Set-Location $projectRoot
$localJava = Join-Path $projectRoot '.tools/java/jdk-17.0.16+8'
if (Test-Path $localJava) { $env:JAVA_HOME = $localJava }
if (Test-Path '.tools/sdk') { $env:ANDROID_HOME = Join-Path $projectRoot '.tools/sdk' }
$env:ANDROID_USER_HOME = Join-Path $projectRoot '.tools/android-user'
$env:ANDROID_AVD_HOME = Join-Path $projectRoot '.tools/avd'
$env:GRADLE_USER_HOME = Join-Path $projectRoot '.tools/gradle-home'
if (Test-Path './gradlew.bat') { & ./gradlew.bat --no-daemon @Tasks }
elseif (Test-Path '.tools/gradle-8.13/bin/gradle.bat') { & .tools/gradle-8.13/bin/gradle.bat --no-daemon @Tasks }
else { throw 'Ouvrir dans Android Studio ou installer Gradle 8.13 pour générer le wrapper.' }
exit $LASTEXITCODE
