/**
 * Copyright 2022 Ayogo Health Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ayogo.cordova.oauth;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.content.Intent;
import android.net.Uri;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.PluginResult;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OAuthPluginUnitTest {
    @Test
    public void executeWithUnknownAction() throws JSONException {
        OAuthPlugin plugin = new OAuthPlugin();

        CallbackContext context = mock(CallbackContext.class);
        boolean retVal = plugin.execute("unknownAction", "[]", context);
        assertEquals(false, retVal);

        ArgumentCaptor<PluginResult> result = ArgumentCaptor.forClass(PluginResult.class);
        verify(context).sendPluginResult(result.capture());
        assertEquals(PluginResult.Status.INVALID_ACTION.ordinal(), result.getValue().getStatus());
    }

    @Test
    public void executeOAuthWithMissingArgument() throws JSONException {
        OAuthPlugin plugin = new OAuthPlugin();

        CallbackContext context = mock(CallbackContext.class);
        boolean retVal = plugin.execute("startOAuth", "[]", context);
        assertEquals(false, retVal);

        ArgumentCaptor<PluginResult> result = ArgumentCaptor.forClass(PluginResult.class);
        verify(context).sendPluginResult(result.capture());
        assertEquals(PluginResult.Status.ERROR.ordinal(), result.getValue().getStatus());
    }

    @Test
    public void callbackWithMatchingSchemeAndHost() throws JSONException {
        OAuthPlugin plugin = new OAuthPlugin();
        plugin.preferences.set("oauthscheme", "testscheme");
        plugin.preferences.set("oauthhostname", "testhost");

        // Create an Intent with matching scheme and host
        Intent intent = new Intent();
        intent.setData(Uri.parse("testscheme://testhost?code=abc123&state=xyz"));

        // Call onNewIntent to process the callback
        plugin.onNewIntent(intent);

        // Verify that the plugin processes the intent successfully
        // (No exception should be thrown, and the method should handle the URI)
    }

    @Test
    public void callbackWithMismatchedScheme() throws JSONException {
        OAuthPlugin plugin = new OAuthPlugin();
        plugin.preferences.set("oauthscheme", "testscheme");
        plugin.preferences.set("oauthhostname", "testhost");

        // Create an Intent with mismatched scheme
        Intent intent = new Intent();
        intent.setData(Uri.parse("wrongscheme://testhost?code=abc123"));

        // Call onNewIntent - should not process
        plugin.onNewIntent(intent);

        // No exception should be thrown
    }

    @Test
    public void callbackWithIntentActionNotView() throws JSONException {
        OAuthPlugin plugin = new OAuthPlugin();
        plugin.preferences.set("oauthscheme", "testscheme");
        plugin.preferences.set("oauthhostname", "testhost");

        // Create an Intent with ACTION_SEND instead of ACTION_VIEW
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setData(Uri.parse("testscheme://testhost?code=abc123"));

        // onNewIntent should still extract and process the URI
        plugin.onNewIntent(intent);

        // No exception should be thrown - callback should be processed
    }
}
