# Android Host Callback Routing Experiment

This branch (`experiment/android-host-callback-routing`) implements an experimental OutSystems-specific Android callback-routing approach.

## Background

Production Android apps using OutSystems may have multiple activities that could handle OAuth callbacks:

1. **MainActivity**: The primary OAuth-specific intent filter added by this plugin
2. **OutSystems Redirect Activity**: A general-purpose handler for the app's URL scheme

When both intent-filters exist and match the same callback URI, Android shows a chooser dialog with two identically-named entries, creating user confusion and unclear routing behavior.

## Solution

This experiment removes the OAuth-specific intent-filter from `plugin.xml`, relying instead on the host application's existing Android callback/deep-link handler to route the callback to the Cordova activity.

### Key Changes

- **Removed** the `<config-file target="AndroidManifest.xml">` section that injects the OAuth intent-filter into the plugin's platform configuration
- **Preserved** all other plugin functionality:
  - Android Cordova plugin registration (`com.ayogo.cordova.oauth.OAuthPlugin`)
  - `OAuthScheme` and `OAuthHostname` preferences
  - JavaScript interface and callback processing logic
  - iOS configuration and behavior (unchanged)

### What This Requires

**Android host applications must now provide**:

1. A matching intent-filter for the OAuth callback URI (scheme + host) in their `AndroidManifest.xml`
2. Routing logic to forward the callback URL to the Cordova activity/plugin via `onNewIntent()`

Without this host-side integration, OAuth callbacks **will not work** after using this plugin version.

### Expected Behavior

When the OAuth provider redirects to the callback URI:

1. The host app's intent-filter matches and launches/resumes the host activity
2. The host app forwards the callback Intent to the Cordova activity (or directly to the plugin)
3. The plugin's `onNewIntent()` method receives the callback and processes it
4. The callback is dispatched to JavaScript as a `MessageEvent` with `oauth::` prefix

## Testing

### Unit Tests

- **ManifestConfigTest.pluginXmlDoesNotContainOAuthIntentFilter()**: Verifies plugin.xml does not inject Android intent-filter
- **ManifestConfigTest.pluginXmlContainsAndroidPluginRegistration()**: Verifies plugin registration and preferences remain
- **ManifestConfigTest.pluginXmlPreservesiOSConfiguration()**: Verifies iOS configuration is unchanged
- **OAuthPluginUnitTest.callbackWithMatchingSchemeAndHost()**: Verifies callback processing with matching scheme/host
- **OAuthPluginUnitTest.callbackWithIntentActionNotView()**: Verifies callbacks work even if Intent action is changed by host

### Integration Tests

The existing `OAuthPluginTest` suite validates the OAuth flow end-to-end when the host app provides the callback routing. These tests rely on the test app's `AndroidManifest.xml`, which still includes the callback intent-filter.

## Limitations & Next Steps

### Current State

- ✅ Fixture-level unit tests pass (callback processing, plugin registration, preferences)
- ✅ plugin.xml verified to not inject intent-filter
- ✅ iOS configuration unchanged
- ⚠️ **Real native build not performed** (requires Android SDK/build environment)
- ⚠️ **On-device forwarding test not performed** (requires physical device or emulator + OutSystems app)

### Validation Required

For this experiment to be confirmed as a working solution:

1. **Native Build**: Build the plugin with an OutSystems-based Cordova app
2. **Device Test**: On physical device or emulator:
   - Trigger OAuth login
   - Verify only ONE chooser entry appears (or no chooser, if only one filter)
   - Verify callback is received and processed correctly
   - Verify no "intent-filter not found" errors in logs

### Known Issues

- None documented yet. OutSystems routing behavior should be verified on device.

## Reverting This Experiment

To return to the standard plugin with the OAuth-specific intent-filter:

```bash
git checkout main
```

## Git Notes

- Branch: `experiment/android-host-callback-routing`
- **Not merged to main** (experimental only)
- **Not tagged** (pre-release state)
- Preserves unrelated worktree changes
- Does not modify MEAN or OutSystems configurations

## References

- **Original Issue**: Callback chooser dialog with duplicate entries in OutSystems app
- **Plugin Source**: [OAuthPlugin.java](/src/android/OAuthPlugin.java)
- **Plugin Config**: [plugin.xml](/plugin.xml)
- **Test Manifest**: [tests/android/src/main/AndroidManifest.xml](/tests/android/src/main/AndroidManifest.xml)
