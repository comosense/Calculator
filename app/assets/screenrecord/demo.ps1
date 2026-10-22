#
# powershell -ExecutionPolicy Bypass -File .\demo.ps1
#

# 0
#& $adb -s $serial shell input tap 144 377
# 1
#& $adb -s $serial shell input tap 48 308
# 2
#& $adb -s $serial shell input tap 144 308
# 3
#& $adb -s $serial shell input tap 240 308
# 4
#& $adb -s $serial shell input tap 48 240
# 5
#& $adb -s $serial shell input tap 144 240
# 6
#& $adb -s $serial shell input tap 240 240
# 7
#& $adb -s $serial shell input tap 48 171
# 8
#& $adb -s $serial shell input tap 144 171
# 9
#& $adb -s $serial shell input tap 240 171
# .
#& $adb -s $serial shell input tap 240 377
# ()
#& $adb -s $serial shell input tap 336 377
# +
#& $adb -s $serial shell input tap 336 308
# -
#& $adb -s $serial shell input tap 432 308
# x
#& $adb -s $serial shell input tap 336 240
# /
#& $adb -s $serial shell input tap 432 240
# clear
#& $adb -s $serial shell input tap 336 171
# =
#& $adb -s $serial shell input tap 432 171
# switch
#& $adb -s $serial shell input tap 240 445
# display
#& $adb -s $serial shell input tap 240 102
# pi
#& $adb -s $serial shell input tap 240 377
# sin
#& $adb -s $serial shell input tap 80 308
# cos
#& $adb -s $serial shell input tap 240 308
# tan
#& $adb -s $serial shell input tap 400 308
# log
#& $adb -s $serial shell input tap 80 240
# ln
#& $adb -s $serial shell input tap 240 240
# e
#& $adb -s $serial shell input tap 400 240
# sqrt
#& $adb -s $serial shell input tap 80 171
# pow
#& $adb -s $serial shell input tap 240 171
# !
#& $adb -s $serial shell input tap 400 171

$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

if (-not (Test-Path $adb)) {
    $adb = "adb"
}

$serial = "emulator-5554"

Write-Host "check emulator"
& $adb devices

if ($LASTEXITCODE -ne 0) {
    Write-Host "failed to run adb"
    exit 1
}

Write-Host "start"
Start-Sleep -Seconds 1

# ()
& $adb -s $serial shell input tap 336 377
Start-Sleep -MIlliseconds 200

# 0
& $adb -s $serial shell input tap 144 377
Start-Sleep -MIlliseconds 200

# .
& $adb -s $serial shell input tap 240 377
Start-Sleep -MIlliseconds 200

# 2
& $adb -s $serial shell input tap 144 308
Start-Sleep -MIlliseconds 200

# +
& $adb -s $serial shell input tap 336 308
Start-Sleep -MIlliseconds 200

# 0
& $adb -s $serial shell input tap 144 377
Start-Sleep -MIlliseconds 200

# .
& $adb -s $serial shell input tap 240 377
Start-Sleep -MIlliseconds 200

# 3
& $adb -s $serial shell input tap 240 308
Start-Sleep -MIlliseconds 200

# ()
& $adb -s $serial shell input tap 336 377
Start-Sleep -MIlliseconds 200

# x
& $adb -s $serial shell input tap 336 240
Start-Sleep -MIlliseconds 200

# 1
& $adb -s $serial shell input tap 48 308
Start-Sleep -MIlliseconds 200

# 6
& $adb -s $serial shell input tap 240 240
Start-Sleep -MIlliseconds 200

# x
& $adb -s $serial shell input tap 336 240
Start-Sleep -MIlliseconds 200

# 2
& $adb -s $serial shell input tap 144 308
Start-Sleep -Seconds 1

# =
& $adb -s $serial shell input tap 432 171
Start-Sleep -Seconds 1

# 2
& $adb -s $serial shell input tap 144 308
Start-Sleep -MIlliseconds 200

# switch
& $adb -s $serial shell input tap 240 445
Start-Sleep -MIlliseconds 200

# sin
& $adb -s $serial shell input tap 80 308
Start-Sleep -MIlliseconds 200

# pi
& $adb -s $serial shell input tap 240 377
Start-Sleep -MIlliseconds 200

# switch
& $adb -s $serial shell input tap 240 445
Start-Sleep -MIlliseconds 200

# /
& $adb -s $serial shell input tap 432 240
Start-Sleep -MIlliseconds 200

# 2
& $adb -s $serial shell input tap 144 308
Start-Sleep -MIlliseconds 200

# ()
& $adb -s $serial shell input tap 336 377
Start-Sleep -MIlliseconds 200

# +
& $adb -s $serial shell input tap 336 308
Start-Sleep -MIlliseconds 200

# switch
& $adb -s $serial shell input tap 240 445
Start-Sleep -MIlliseconds 200

# cos
& $adb -s $serial shell input tap 240 308
Start-Sleep -MIlliseconds 200

# pi
& $adb -s $serial shell input tap 240 377
Start-Sleep -MIlliseconds 200

# switch
& $adb -s $serial shell input tap 240 445
Start-Sleep -MIlliseconds 200

# ()
& $adb -s $serial shell input tap 336 377
Start-Sleep -Seconds 1

# =
& $adb -s $serial shell input tap 432 171
Start-Sleep -Seconds 1

# +
& $adb -s $serial shell input tap 336 308
Start-Sleep -MIlliseconds 200

# display
& $adb -s $serial shell input tap 240 102
Start-Sleep -MIlliseconds 200

# swipe
& $adb -s $serial shell input swipe 240 240 240 102 500
Start-Sleep -Seconds 1

# tap
& $adb -s $serial shell input tap 240 200
Start-Sleep -Seconds 1

# =
& $adb -s $serial shell input tap 432 171
Start-Sleep -Seconds 1

# clear
& $adb -s $serial shell input tap 336 171
Start-Sleep -MIlliseconds 200

# long-tap
& $adb -s $serial shell input swipe 240 102 240 102 1000
Start-Sleep -Seconds 1

# swipe
& $adb -s $serial shell input swipe 240 102 240 240 500
Start-Sleep -Seconds 1

# back
& $adb -s $serial shell input tap 240 445
Start-Sleep -Seconds 1

# switch
& $adb -s $serial shell input tap 240 445
Start-Sleep -Seconds 1

# pi
& $adb -s $serial shell input tap 240 377
Start-Sleep -Seconds 1

# switch
& $adb -s $serial shell input tap 240 445
Start-Sleep -Seconds 1

# =
& $adb -s $serial shell input tap 432 171
Start-Sleep -Seconds 1

# long-tap
& $adb -s $serial shell input swipe 240 102 240 102 1000
Start-Sleep -MIlliseconds 200

# swipe
& $adb -s $serial shell input swipe 240 240 240 102 500
Start-Sleep -MIlliseconds 200

# swipe
& $adb -s $serial shell input swipe 240 240 240 102 500
Start-Sleep -MIlliseconds 200

# swipe
& $adb -s $serial shell input swipe 240 240 240 102 500
Start-Sleep -MIlliseconds 200

# swipe
& $adb -s $serial shell input swipe 240 240 240 102 500
Start-Sleep -MIlliseconds 200

# back
& $adb -s $serial shell input tap 240 445
Start-Sleep -Seconds 1

# switch
& $adb -s $serial shell input tap 240 445
Start-Sleep -Seconds 1

# pi
& $adb -s $serial shell input tap 240 377
Start-Sleep -Seconds 1

# switch
& $adb -s $serial shell input tap 240 445
Start-Sleep -Seconds 1

# =
& $adb -s $serial shell input tap 432 171
Start-Sleep -Seconds 1

# swipe
& $adb -s $serial shell input swipe 384 102 96 102 500
Start-Sleep -MIlliseconds 200

# swipe
& $adb -s $serial shell input swipe 384 102 96 102 500
Start-Sleep -MIlliseconds 200

# swipe
& $adb -s $serial shell input swipe 384 102 96 102 500
Start-Sleep -MIlliseconds 200

# swipe
& $adb -s $serial shell input swipe 96 102 384 102 500
Start-Sleep -MIlliseconds 200

# swipe
& $adb -s $serial shell input swipe 96 102 384 102 500
Start-Sleep -MIlliseconds 200

# swipe
& $adb -s $serial shell input swipe 96 102 384 102 500
Start-Sleep -Seconds 1

# clear
& $adb -s $serial shell input tap 336 171
Start-Sleep -Seconds 1

Write-Host "finish"
