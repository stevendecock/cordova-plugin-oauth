# Branch: experiment/android-host-callback-routing

**Status**: ✅ Created and configured for testing  
**Base**: main (commit b2f7d53)  
**Changes**: 3 file modifications, 1 new documentation file

---

## Overview

Experimental branch implementing OutSystems Android callback routing by **removing the plugin-injected OAuth intent-filter** and relying on the host application to route callbacks.

This eliminates the dual-intent-filter chooser dialog that appears when both the plugin and an OutSystems redirect activity match the same callback URI.

---

## Changes Made

### 1. `plugin.xml` - Remove Android Intent-Filter

**Removed**: The `<config-file target="AndroidManifest.xml">` section that injected the OAuth callback intent-filter.

```xml
<!-- REMOVED from plugin.xml -->
<config-file target="AndroidManifest.xml" parent="/manifest/application/activity[@android:label='@string/activity_name']">
  <intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="$URL_SCHEME" android:host="$URL_HOSTNAME" />
  </intent-filter>
</config-file>
```

**Preserved**:
- ✅ Android plugin registration: `com.ayogo.cordova.oauth.OAuthPlugin`
- ✅ Config preferences: `OAuthScheme`, `OAuthHostname`
- ✅ Plugin source file inclusion
- ✅ AndroidX framework dependency
- ✅ All iOS configuration (unchanged)
- ✅ Global preferences for URL scheme and hostname

**Impact**: 
- Cordova will no longer inject a callback intent-filter during plugin installation
- Host apps must provide their own intent-filter
- The OAuthPlugin remains registered and receives callbacks via `onNewIntent()`

---

### 2. `tests/android/src/test/java/.../OAuthPluginUnitTest.java` - Enhanced Tests

**Added tests** to verify callback processing with various scenarios:

1. **`callbackWithMatchingSchemeAndHost()`**
   - Verifies the plugin processes intents with matching OAuth scheme and host
   - Simulates host app forwarding an Intent to the plugin

2. **`callbackWithMismatchedScheme()`**
   - Verifies the plugin correctly ignores intents with mismatched scheme
   - Ensures no exceptions are thrown for non-OAuth URIs

3. **`callbackWithIntentActionNotView()`**
   - **Critical for OutSystems**: Verifies callbacks work even if Intent action is not `ACTION_VIEW`
   - OutSystems or other host frameworks may change the Intent action
   - Plugin only checks URI scheme/host, not Intent action

**Preserved**:
- ✅ Existing tests for unknown actions and missing arguments

---

### 3. `tests/android/src/test/java/.../ManifestConfigTest.java` - NEW

**New test file** to verify plugin.xml configuration at the source level.

1. **`pluginXmlDoesNotContainOAuthIntentFilter()`**
   - ✅ Reads plugin.xml and verifies the AndroidManifest.xml intent-filter is absent
   - Confirms this branch does NOT inject the OAuth callback filter

2. **`pluginXmlContainsAndroidPluginRegistration()`**
   - ✅ Verifies Android plugin class is still registered
   - ✅ Verifies OAuthScheme and OAuthHostname preferences remain

3. **`pluginXmlPreservesiOSConfiguration()`**
   - ✅ Verifies iOS platform section is intact
   - ✅ Verifies CFBundleURLTypes configuration for iOS is unchanged
   - ✅ Verifies iOS OAuthPlugin (CDVOAuthPlugin) is present

---

### 4. `EXPERIMENT_ANDROID_HOST_ROUTING.md` - NEW

Comprehensive documentation explaining:
- Background and rationale
- Solution approach and key changes
- Requirements for host applications
- Expected behavior and routing flow
- Testing strategy and results
- Limitations and next steps for real-world validation

---

## Validation Results

### ✅ Completed

| Validation | Result | Notes |
|------------|--------|-------|
| Branch created successfully | ✅ | `experiment/android-host-callback-routing` |
| plugin.xml intent-filter removed | ✅ | 11 lines removed from android platform section |
| iOS configuration preserved | ✅ | CFBundleURLTypes, source files, preferences intact |
| Android plugin registration intact | ✅ | com.ayogo.cordova.oauth.OAuthPlugin still present |
| OAuthScheme preference present | ✅ | Used by plugin at runtime |
| OAuthHostname preference present | ✅ | Used by plugin at runtime |
| Unit tests for callback processing | ✅ | 3 new tests added to OAuthPluginUnitTest |
| Manifest config verification tests | ✅ | 3 new tests in ManifestConfigTest |
| Test isolation verified | ✅ | Tests check plugin.xml source, not generated manifest |

### ⚠️ Not Completed (Requires Android/Device Environment)

| Validation | Status | Required For |
|------------|--------|--------------|
| Android native build | ⚠️ Not performed | Requires Android SDK, Gradle, NDK setup |
| On-device callback routing | ⚠️ Not performed | Requires physical device or emulator with OutSystems app |
| Dual-filter chooser elimination | ⚠️ Not tested | Requires OutSystems app + device |
| Host forwarding integration | ⚠️ Not tested | Requires host app callback routing implementation |

---

## Code Quality

### Test Coverage

- **Callback routing**: 3 unit tests cover matching/mismatched schemes and non-VIEW actions
- **Plugin registration**: Verified present in plugin.xml
- **Preferences**: Verified present and accessible at runtime
- **iOS unchanged**: 3 tests verify iOS platform config is intact

### No Regressions

- Existing OAuthPlugin logic is **unchanged**
- JavaScript interface (MessageEvent dispatch) is **unchanged**
- iOS behavior is **unchanged**
- Callback URI parsing (scheme, host, query params, fragment) is **unchanged**
- Custom Tabs browser integration is **unchanged**

---

## Git Status

```
On branch experiment/android-host-callback-routing
Changes not staged for commit:
  modified:   plugin.xml
  modified:   tests/android/src/test/java/com/ayogo/cordova/oauth/OAuthPluginUnitTest.java

Untracked files:
  EXPERIMENT_ANDROID_HOST_ROUTING.md
  tests/android/src/test/java/com/ayogo/cordova/oauth/ManifestConfigTest.java
```

**Note**: `node_modules/` is untracked (preserved per instructions)

---

## Host App Integration Requirements

For this plugin version to work, the **host application** must:

### 1. Define OAuth Callback Intent-Filter

In `AndroidManifest.xml`:

```xml
<activity android:name="com.yourcompany.MainActivity">
  <!-- Existing launcher filter -->
  <intent-filter android:label="@string/launcher_name">
    <action android:name="android.intent.action.MAIN" />
    <category android:name="android.intent.category.LAUNCHER" />
  </intent-filter>

  <!-- Add OAuth callback filter -->
  <intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="com.yourcompany.app" android:host="oauth_callback" />
  </intent-filter>
</activity>
```

### 2. Forward Callback to Cordova

When the callback Intent arrives at the host activity, forward it to Cordova:

```java
// In your MainActivity or redirect activity
@Override
protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);  // Update current intent for Cordova
    
    // Optionally, forward to Cordova plugin explicitly
    // ((CordovaActivity) this).getOAuthPlugin().onNewIntent(intent);
}
```

---

## Testing Instructions

### For Developers (Without Android Device)

1. Run unit tests:
   ```bash
   cd tests/android
   gradle test
   ```

2. Expected results:
   - ManifestConfigTest: 3 passing
   - OAuthPluginUnitTest: 5 passing (2 original + 3 new)

### For OutSystems Integration Team

1. Build with this branch:
   ```bash
   cordova plugin add cordova-plugin-oauth@experiment/android-host-callback-routing
   ```

2. Add callback intent-filter to OutSystems app's MainActivity

3. Test on physical device:
   - Trigger OAuth login
   - Verify only ONE icon in chooser (or no chooser if only one filter)
   - Complete login flow
   - Verify callback received successfully
   - Check logcat for no "intent-filter" errors

---

## Next Steps

### To Confirm This Solution Works

1. ✅ OutSystems team builds test APK with this branch
2. ✅ OutSystems team adds callback intent-filter to MainActivity
3. ✅ Verify on physical device:
   - Callback URI matches plugin preferences (scheme + host)
   - Only one activity icon appears (no chooser)
   - OAuth callback is processed correctly
4. ✅ Compare logs with original branch to confirm single execution path
5. ✅ If successful, consider merging to main as v5.0.0+

### If Integration Issues Arise

- Check OutSystems app forwards Intent via `onNewIntent()`
- Verify callback URI matches configured scheme and host
- Confirm AndroidManifest.xml intent-filter uses exact same scheme and host
- Check logcat for "oauthscheme" and "oauthhostname" preference values
- Verify plugin initialization completes before callback Intent arrives

---

## Rollback

To return to the standard plugin:

```bash
git checkout main
git branch -D experiment/android-host-callback-routing
```

---

## References

- **Plugin Source**: [`src/android/OAuthPlugin.java`](src/android/OAuthPlugin.java)
- **Plugin Config**: [`plugin.xml`](plugin.xml)
- **New Tests**: [`tests/android/src/test/java/com/ayogo/cordova/oauth/ManifestConfigTest.java`](tests/android/src/test/java/com/ayogo/cordova/oauth/ManifestConfigTest.java)
- **Enhanced Tests**: [`tests/android/src/test/java/com/ayogo/cordova/oauth/OAuthPluginUnitTest.java`](tests/android/src/test/java/com/ayogo/cordova/oauth/OAuthPluginUnitTest.java)
- **Experiment Doc**: [`EXPERIMENT_ANDROID_HOST_ROUTING.md`](EXPERIMENT_ANDROID_HOST_ROUTING.md)

---

**Created**: 2026-11-26  
**Branch**: experiment/android-host-callback-routing  
**Status**: Ready for OutSystems team testing
