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
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import android.content.Intent;
import android.net.Uri;
import java.util.Collections;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPreferences;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaWebViewEngine;
import org.apache.cordova.PluginResult;
import org.json.JSONException;
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
    public void callbackReceivedBeforePageLoadIsDispatchedAfterPageFinished() {
        CordovaWebView webView = mock(CordovaWebView.class);
        CordovaWebViewEngine engine = mock(CordovaWebViewEngine.class);
        when(webView.getEngine()).thenReturn(engine);
        OAuthPlugin plugin = createPlugin(webView);

        plugin.onNewIntent(callbackIntent(mockCallbackUri("testscheme://testhost?code=abc123")));
        verify(engine, never()).evaluateJavascript(any(), any());

        plugin.onMessage("onPageFinished", null);

        String js = captureDispatchedJavascript(engine);
        assertTrue(js, js.contains("oauth::"));
        assertTrue(js, js.contains("\"code\":\"abc123\""));
    }

    @Test
    public void callbackIntentWithNonViewActionIsDispatched() {
        CordovaWebView webView = mock(CordovaWebView.class);
        CordovaWebViewEngine engine = mock(CordovaWebViewEngine.class);
        when(webView.getEngine()).thenReturn(engine);
        OAuthPlugin plugin = createPlugin(webView);
        plugin.onMessage("onPageFinished", null);

        // Host frameworks may forward the callback with an action other than ACTION_VIEW.
        Intent intent = callbackIntent(mockCallbackUri("testscheme://testhost?code=abc123"));
        lenient().when(intent.getAction()).thenReturn(Intent.ACTION_MAIN);

        plugin.onNewIntent(intent);

        String js = captureDispatchedJavascript(engine);
        assertTrue(js, js.contains("\"code\":\"abc123\""));
    }

    @Test
    public void callbackWithOtherSchemeIsIgnored() {
        CordovaWebView webView = mock(CordovaWebView.class);
        OAuthPlugin plugin = createPlugin(webView);
        plugin.onMessage("onPageFinished", null);

        Uri uri = mock(Uri.class);
        when(uri.getScheme()).thenReturn("otherscheme");
        when(uri.getHost()).thenReturn("testhost");

        plugin.onNewIntent(callbackIntent(uri));

        verifyNoInteractions(webView);
    }

    private static OAuthPlugin createPlugin(CordovaWebView webView) {
        CordovaPreferences preferences = new CordovaPreferences();
        preferences.set("OAuthScheme", "testscheme");
        preferences.set("OAuthHostname", "testhost");

        OAuthPlugin plugin = new OAuthPlugin();
        plugin.privateInitialize("OAuth", mock(CordovaInterface.class), webView, preferences);
        return plugin;
    }

    // android.net.Uri is a stub in local unit tests, so the parsed parts are mocked.
    private static Uri mockCallbackUri(String url) {
        Uri uri = mock(Uri.class);
        when(uri.getScheme()).thenReturn("testscheme");
        when(uri.getHost()).thenReturn("testhost");
        when(uri.getFragment()).thenReturn(null);
        when(uri.getQueryParameterNames()).thenReturn(Collections.singleton("code"));
        when(uri.getQueryParameter("code")).thenReturn("abc123");
        when(uri.toString()).thenReturn(url);
        return uri;
    }

    private static Intent callbackIntent(Uri uri) {
        Intent intent = mock(Intent.class);
        when(intent.getData()).thenReturn(uri);
        return intent;
    }

    private static String captureDispatchedJavascript(CordovaWebViewEngine engine) {
        ArgumentCaptor<String> js = ArgumentCaptor.forClass(String.class);
        verify(engine).evaluateJavascript(js.capture(), isNull());
        return js.getValue();
    }
}
