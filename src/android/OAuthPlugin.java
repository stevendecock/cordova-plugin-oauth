/**
 * Copyright 2019 - 2022 Ayogo Health Inc.
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

import android.content.Intent;
import android.net.Uri;
import androidx.browser.customtabs.CustomTabsIntent;
import android.text.TextUtils;
import java.net.URLDecoder;

import java.util.ArrayList;
import java.util.List;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaWebViewEngine;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;

import org.json.JSONException;
import org.json.JSONObject;


public class OAuthPlugin extends CordovaPlugin {
    private final String TAG = "OAuthPlugin";
    private CallbackContext oauthCallback = null;
    private boolean didFinishLoading = false;
    private String lastOAuthResult = null;

    /**
     * Executes the request.
     *
     * This method is called from the WebView thread. To do a non-trivial amount of work, use:
     *     cordova.getThreadPool().execute(runnable);
     *
     * To run on the UI thread, use:
     *     cordova.getActivity().runOnUiThread(runnable);
     *
     * @param action          The action to execute.
     * @param rawArgs         The exec() arguments in JSON form.
     * @param callbackContext The callback context used when calling back into JavaScript.
     * @return                Whether the action was valid.
     */
    @Override
    public boolean execute(String action, CordovaArgs args, CallbackContext callbackContext) {
        LOG.i(TAG, "execute: action = " + action);

        if ("startOAuth".equals(action)) {
            try {
                String authEndpoint = args.getString(0);

                LOG.i(TAG, "execute: startOAuth called");
                LOG.i(TAG, "execute: didFinishLoading = " + this.didFinishLoading);

                oauthCallback = callbackContext;

                this.startOAuth(authEndpoint);

                return true;
            } catch (JSONException e) {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR));
                return false;
            }
        }

        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
        return false;
    }


    /**
     * Called when the activity is becoming visible to the user.
     */
    @Override
    public void onStart() {
        onNewIntent(cordova.getActivity().getIntent());
    }


    /**
     * Called when the activity receives a new intent.
     *
     * We use this method to intercept the OAuth callback URI and dispatch a
     * message to the WebView.
     */
    @Override
    public void onNewIntent(Intent intent) {
        LOG.i(TAG, "onNewIntent ENTERED");
        LOG.i(TAG, "onNewIntent: intent = " + intent);

        if (intent != null) {
            LOG.i(TAG, "onNewIntent: action = " + intent.getAction());
            LOG.i(TAG, "onNewIntent: data = " + intent.getData());
        }

        if (intent == null || !intent.getAction().equals(Intent.ACTION_VIEW)) {
            LOG.i(TAG, "onNewIntent: returning because intent is null or action is not ACTION_VIEW");
            return;
        }

        final Uri uri = intent.getData();
        String callbackHost = preferences.getString("oauthhostname", "oauth_callback");

        LOG.i(TAG, "onNewIntent: uri = " + uri);
        LOG.i(TAG, "onNewIntent: uri.scheme = " + uri.getScheme());
        LOG.i(TAG, "onNewIntent: uri.host = " + uri.getHost());
        LOG.i(TAG, "onNewIntent: callbackHost = " + callbackHost);
        LOG.i(TAG, "onNewIntent: didFinishLoading = " + this.didFinishLoading);

        // ORIGINAL AYOGO 4.1.0 CONDITION — unchanged
        if (uri.getHost().equals(callbackHost)) {
            LOG.i(TAG, "OAuth called back with parameters.");

            try {
                JSONObject jsobj = new JSONObject();
                jsobj.put("oauth_callback_url", uri.toString());

                // Parse fragment parameters
                if (uri.getFragment() != null) {
                    String fragment = uri.getFragment();
                    String[] pairs = fragment.split("&");
                    for (String pair : pairs) {
                        String[] keyValue = pair.split("=");
                        if (keyValue.length == 2) {
                            String key = keyValue[0];
                            String value = keyValue[1];
                            jsobj.put(key, value);
                        }
                    }
                }

                // Parse query parameters
                for (String queryKey : uri.getQueryParameterNames()) {
                    jsobj.put(queryKey, uri.getQueryParameter(queryKey));
                }

                LOG.i(TAG, "onNewIntent: OAuth callback parsed");
                LOG.i(TAG, "onNewIntent: didFinishLoading = " + this.didFinishLoading);

                // ORIGINAL AYOGO 4.1.0 LOGIC — unchanged
                if (this.didFinishLoading) {
                    LOG.i(TAG, "onNewIntent: calling dispatchOAuthMessage");
                    dispatchOAuthMessage(jsobj.toString());
                } else {
                    LOG.i(TAG, "onNewIntent: storing result in lastOAuthResult");
                    this.lastOAuthResult = jsobj.toString();
                }
            } catch (JSONException e) {
                LOG.e(TAG, "JSON Serialization failed");
                e.printStackTrace();
            }
        } else {
            LOG.i(TAG, "onNewIntent: callback host did NOT match");
        }
    }

    /**
     * Called when the activity will start interacting with the user.
     *
     * We use this method to indicate to the JavaScript side that the OAuth
     * window has closed (regardless of login status).
     */
    @Override
    public void onResume(boolean multitasking) {
        LOG.i(TAG, "onResume ENTERED");
        LOG.i(TAG, "onResume: oauthCallback present = " + (oauthCallback != null));

        super.onResume(multitasking);

        if (oauthCallback != null) {
            LOG.i(TAG, "onResume: sending OAuth window-close callback");

            oauthCallback.sendPluginResult(
                new PluginResult(PluginResult.Status.OK)
            );
            oauthCallback = null;
        }
    }

    /**
     * Called when a message is sent to plugin.
     *
     * @param id            The message id
     * @param data          The message data
     * @return              Object to stop propagation or null
     */
    @Override
    public Object onMessage(String id, Object data) {
        LOG.i(TAG, "onMessage: id = " + id);

        if (id.equals("onPageFinished")) {
            LOG.i(TAG, "onMessage: onPageFinished received");

            this.didFinishLoading = true;

            LOG.i(TAG, "onMessage: lastOAuthResult present = " + (this.lastOAuthResult != null));

            if (this.lastOAuthResult != null) {
                LOG.i(TAG, "onMessage: dispatching buffered OAuth result");

                this.dispatchOAuthMessage(this.lastOAuthResult);
                this.lastOAuthResult = null;
            }
        }
        return null;
    }

    /**
     * Launches the custom tab with the OAuth endpoint URL.
     *
     * @param url The URL of the OAuth endpoint.
     */
    private void startOAuth(String url) {
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();

        CustomTabsIntent customTabsIntent = builder.build();
        customTabsIntent.intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        customTabsIntent.intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        customTabsIntent.intent.putExtra("android.support.customtabs.extra.ENABLE_URLBAR_HIDING", true);
        customTabsIntent.intent.putExtra("android.support.customtabs.extra.EXTRA_ENABLE_INSTANT_APPS", false);
        customTabsIntent.intent.putExtra("android.support.customtabs.extra.SEND_TO_EXTERNAL_HANDLER", false);
        customTabsIntent.intent.putExtra("androidx.browser.customtabs.extra.SHARE_STATE", 2);
        customTabsIntent.intent.putExtra("androidx.browser.customtabs.extra.DISABLE_BACKGROUND_INTERACTION", false);
        customTabsIntent.intent.putExtra("org.chromium.chrome.browser.customtabs.EXTRA_DISABLE_DOWNLOAD_BUTTON", true);
        customTabsIntent.intent.putExtra("org.chromium.chrome.browser.customtabs.EXTRA_DISABLE_STAR_BUTTON", true);

        customTabsIntent.launchUrl(this.cordova.getActivity(), Uri.parse(url));
    }

    @SuppressWarnings("deprecation")
    private void dispatchOAuthMessage(final String msg) {
        LOG.i(TAG, "dispatchOAuthMessage ENTERED");

        final String msgData = msg.replace("'", "\\'").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
        final String jsCode = "window.dispatchEvent(new MessageEvent('message', { data: 'oauth::" + msgData + "' }));";

        CordovaWebViewEngine engine = this.webView.getEngine();
        if (engine != null) {
            engine.evaluateJavascript(jsCode, null);
        } else {
            this.webView.sendJavascript(jsCode);
        }
    }
}
