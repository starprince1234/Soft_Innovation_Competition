# Android Real Device Runbook

## 1. Preconditions
- Android SDK path: `F:\Android\SDK`
- APK built from:
  - `JiGuYunYu_Frontend/Android/jiguyunyu_2.0/app/build/outputs/apk/debug/app-debug.apk`
- Backend starts from project root:
  - this repository root

## 2. Start Full Docker Backend (only VLM is mock)
Optional but recommended before compose up (build Python image with persistent cache, dependency changes only):

```powershell
.\scripts\build_python_image_cached.ps1 -ImageTag "jigu-python:0.1.0"
```

Run in PowerShell at project root:

```powershell
docker compose --env-file .env.backend -f docker-compose.backend.yml up -d
docker compose --env-file .env.backend -f docker-compose.backend.yml ps
```

Expected status:
- `jigu-mysql`: healthy
- `jigu-redis`: healthy
- `jigu-python`: healthy
- `jigu-java`: healthy
- `jigu-mock-vlm`: running/healthy
- `jigu-nginx`: running

## 3. Verify End-to-End Detect Flow
Run:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\e2e_java_detect_mock_vlm.ps1
```

Expected output includes:
- `"ok": true`
- `"status": "COMPLETED"`
- `"imageUrlContainsExpectedBucket": true`

## 4. Connect Phone and Prepare ADB
1. Enable Developer Options and USB debugging on the phone.
2. Connect USB and allow RSA debugging prompt.
3. Run:

```powershell
& "F:\Android\SDK\platform-tools\adb.exe" start-server
& "F:\Android\SDK\platform-tools\adb.exe" devices
```

Expected: one device in `device` state (not `unauthorized`).

## 5. Make Phone Access Host Backend
The app API URL is injected by Gradle `API_BASE_URL`.
Default value:
- `http://10.0.2.2:8080/`

Requirements:
- Phone and PC must be on the same Wi-Fi.
- If your host WLAN IP changes, rebuild with `-PAPI_BASE_URL=http://<host-lan-ip>:8080/` and reinstall APK.

Optional fallback (when using USB): map phone localhost to host localhost:

```powershell
& "F:\Android\SDK\platform-tools\adb.exe" reverse tcp:8080 tcp:8080
```

Optional verify:

```powershell
& "F:\Android\SDK\platform-tools\adb.exe" reverse --list
```

Should show `tcp:8080 tcp:8080`.

## 6. Install APK to Phone
Install:

```powershell
& "F:\Android\SDK\platform-tools\adb.exe" install -r ".\JiGuYunYu_Frontend\Android\jiguyunyu_2.0\app\build\outputs\apk\debug\app-debug.apk"
```

Expected: `Success`.

## 7. On-Device Validation Steps
1. Open app `JiGuYunYu`.
2. Register/Login.
3. Enter detect page and upload/take a photo.
4. Wait for detect result.

Auth notes:
- Username must be at least 3 characters.
- Password must be at least 6 characters.
- Self-registration creates `PUBLIC` role users only.
- `ARCHAEOLOGIST` / `MANAGER` are managed roles and should be assigned later via admin-side role management.

Success criteria:
- Login and API calls succeed.
- Detect task completes and result renders in app.
- Backend logs show Java -> Python internal detect call and mock VLM completion.

## 8. Useful Troubleshooting
- If `adb devices` empty:
  - change USB mode to File Transfer
  - replug USB and accept RSA prompt
  - run `adb kill-server; adb start-server`
- If app cannot access backend:
  - ensure phone and PC are in same Wi-Fi subnet
  - verify host URL is reachable: `http://<host-lan-ip>:8080/api/v1/health`
  - if still failing, use USB fallback: re-run `adb reverse tcp:8080 tcp:8080`
  - confirm Docker still healthy with `docker compose ... ps`
- If detect fails:
  - re-run `scripts/e2e_java_detect_mock_vlm.ps1` to isolate backend chain first

## 9. Full Command Walkthrough (Executed)

This is the full sequence for "remove old debug APK -> build fresh APK -> install to phone -> launch app".

### 9.1 Step-by-step commands

Run in PowerShell from any directory:

```powershell
$root = "<repo-root>"
$proj = "$root\JiGuYunYu_Frontend\Android\jiguyunyu_2.0"
$apk = "$proj\app\build\outputs\apk\debug\app-debug.apk"
$adb = "F:\Android\SDK\platform-tools\adb.exe"
```

1) Delete old local debug APK:

```powershell
if (Test-Path $apk) { Remove-Item -Force $apk }
```

2) Clean + rebuild debug APK:

```powershell
Set-Location $proj
.\gradlew.bat clean assembleDebug
```

3) Check phone connection:

```powershell
& $adb start-server
& $adb devices
```

4) Enable USB reverse (phone localhost:8080 -> PC localhost:8080):

```powershell
& $adb reverse tcp:8080 tcp:8080
```

5) Uninstall old app package from phone (optional but recommended):

```powershell
& $adb uninstall com.example.jiguyunyu
```

6) Install fresh debug APK:

```powershell
& $adb install -r $apk
```

7) Launch app directly:

```powershell
& $adb shell am start -n com.example.jiguyunyu/.MainActivity
```

### 9.2 One-shot command

```powershell
$root = "<repo-root>"; $proj = "$root\JiGuYunYu_Frontend\Android\jiguyunyu_2.0"; $apk = "$proj\app\build\outputs\apk\debug\app-debug.apk"; $adb = "F:\Android\SDK\platform-tools\adb.exe"; if (Test-Path $apk) { Remove-Item -Force $apk }; Set-Location $proj; .\gradlew.bat clean assembleDebug; & $adb start-server; & $adb devices; & $adb reverse tcp:8080 tcp:8080; & $adb uninstall com.example.jiguyunyu; & $adb install -r $apk; & $adb shell am start -n com.example.jiguyunyu/.MainActivity
```

### 9.3 Real-device debugging helpers

Keep live logs:

```powershell
& $adb logcat
```

Filter app logs quickly:

```powershell
& $adb logcat | Select-String "jiguyunyu|AndroidRuntime|FATAL EXCEPTION"
```
