# Ces tests effacent les données : émulateur QA dédié par défaut.
param([string]$DeviceId = 'emulator-5556')
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Set-Location $projectRoot
$adb = Join-Path $projectRoot '.tools/sdk/platform-tools/adb.exe'
if (!(Test-Path $adb)) { $adb = 'adb' }
& $adb -s $DeviceId install -r app/build/outputs/apk/debug/app-debug.apk
if ($LASTEXITCODE -ne 0) { throw 'Installation APK impossible' }
& $adb -s $DeviceId install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
if ($LASTEXITCODE -ne 0) { throw 'Installation APK de test impossible' }
$testOutput = & $adb -s $DeviceId shell am instrument -w be.moncoachsport.test/androidx.test.runner.AndroidJUnitRunner
$testText = $testOutput -join [Environment]::NewLine
Write-Output $testText
if ($LASTEXITCODE -ne 0 -or $testText -notmatch 'OK \(\d+ tests?\)') { throw 'Les tests instrumentés ont échoué. Consulter la sortie ci-dessus.' }
