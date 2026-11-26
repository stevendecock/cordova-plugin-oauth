# Quick Reference: experiment/android-host-callback-routing

## Branch Created ✅
```bash
experiment/android-host-callback-routing  # based on main
```

## What Changed

### 1. plugin.xml - REMOVED (11 lines)
**What was removed**: The Android intent-filter injection

```xml
<!-- REMOVED: This block is no longer in plugin.xml -->
<config-file target="AndroidManifest.xml" parent="/manifest/application/activity[@android:label='@string/activity_name']">
  <intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="$URL_SCHEME" android:host="$URL_HOSTNAME" />
  </intent-filter>
</config-file>
```

**What remained**:
- ✅ Android plugin registration: `com.ayogo.cordova.oauth.OAuthPlugin`
- ✅ Preferences: `OAuthScheme`, `OAuthHostname`
- ✅ Plugin source file inclusion
- ✅ All iOS configuration
- ✅ AndroidX framework dependency

### 2. OAuthPluginUnitTest.java - ADDED (3 tests)
**New tests added to existing unit test file**:

```java
@Test
public void callbackWithMatchingSchemeAndHost() {
  // Verifies plugin processes intents with matching OAuth URI
}

@Test
public void callbackWithMismatchedScheme() {
  // Verifies plugin ignores non-matching schemes
}

@Test
public void callbackWithIntentActionNotView() {
  // ⭐ CRITICAL: Verifies callbacks work even if Intent action is not ACTION_VIEW
  // (OutSystems may change the action when forwarding)
}
```

### 3. ManifestConfigTest.java - NEW (3 tests)
**New test file to verify plugin.xml configuration**:

```java
@Test
public void pluginXmlDoesNotContainOAuthIntentFilter() {
  // Confirms plugin.xml does NOT inject intent-filter
}

@Test
public void pluginXmlContainsAndroidPluginRegistration() {
  // Confirms plugin registration and preferences are present
}

@Test
public void pluginXmlPreservesiOSConfiguration() {
  // Confirms iOS configuration is unchanged
}
```

### 4. Documentation Files - NEW
- **BRANCH_SUMMARY.md** - Complete overview of changes and validation
- **EXPERIMENT_ANDROID_HOST_ROUTING.md** - Detailed experiment guide
- **VALIDATION_CHECKLIST.md** - Comprehensive validation report

## Why This Change?

### Problem
OutSystems production Android app has two activities matching the same OAuth callback:
1. MainActivity (plugin's intent-filter)
2. OutSystems redirect activity (app's URL scheme filter)

Result: Android shows a chooser dialog with two identical entries, creating confusion and unclear routing.

### Solution
Remove plugin-injected intent-filter, rely on host app's existing callback routing.

Result: Single clear routing path, no duplicate chooser dialog.

### Trade-off
Host app must now provide and forward the callback Intent itself.

## For OutSystems Integration

### Host App Changes Required

**1. Add intent-filter to MainActivity in AndroidManifest.xml:**
```xml
<intent-filter>
  <action android:name="android.intent.action.VIEW" />
  <category android:name="android.intent.category.DEFAULT" />
  <category android:name="android.intent.category.BROWSABLE" />
  <data android:scheme="com.yourapp" android:host="oauth_callback" />
</intent-filter>
```

**2. Forward callback to Cordova in MainActivity:**
```java
@Override
protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);  // Update current intent for Cordova plugin
}
```

## Testing This Branch

### Fixture-Level (No Device Required)
```bash
cd tests/android
gradle test  # Runs OAuthPluginUnitTest.java and ManifestConfigTest.java
```

Expected: 8 tests passing (5 OAuth + 3 manifest config)

### Device-Level (Requires Android Device or Emulator)
1. Build test APK with this plugin branch
2. Run OAuthPluginTest suite
3. Verify callback is received (logs should show OAuth callback processing)

### OutSystems Device Test
1. Build OutSystems app with this plugin branch
2. Add intent-filter to MainActivity (see above)
3. Trigger OAuth login
4. **Expected**: Single entry in chooser (not two)
5. **Expected**: Login succeeds, callback processed
6. **Verify**: Only one execution path in logs

## Important Notes

✅ **What's Same**
- JavaScript interface unchanged
- Callback processing logic unchanged
- iOS behavior unchanged
- Plugin preferences unchanged
- Callback URI parsing unchanged

❌ **What's Different**
- Plugin no longer injects Android intent-filter
- Host must provide intent-filter
- Host must forward callback Intent

⚠️ **What Requires Testing**
- Real Android build (needs SDK)
- Device callback routing verification
- OutSystems forwarding integration
- Dual-filter chooser elimination

## Rollback

```bash
git checkout main
```

## Questions?

Refer to:
- [EXPERIMENT_ANDROID_HOST_ROUTING.md](EXPERIMENT_ANDROID_HOST_ROUTING.md) - Full experiment guide
- [BRANCH_SUMMARY.md](BRANCH_SUMMARY.md) - Change details
- [VALIDATION_CHECKLIST.md](VALIDATION_CHECKLIST.md) - Validation results

## Status

🟢 **Ready for**: Code review, documentation review, OutSystems device testing  
🟡 **Awaiting**: User approval, OutSystems device test results  
🔴 **Not ready for**: Merge to main (pending device testing confirmation)
