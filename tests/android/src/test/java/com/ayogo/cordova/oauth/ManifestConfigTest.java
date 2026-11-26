/**
 * Copyright 2026 Steven Decock
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Verifies that plugin.xml does not add an OAuth-specific intent-filter,
 * since host applications are expected to provide their own callback routing.
 */
@RunWith(MockitoJUnitRunner.class)
public class ManifestConfigTest {
    @Test
    public void pluginXmlDoesNotContainOAuthIntentFilter() throws Exception {
        // Read plugin.xml from the project root
        String projectRoot = System.getProperty("user.dir");
        String pluginXmlPath = Paths.get(projectRoot).resolve("plugin.xml").toString();
        
        // Fall back to relative path if above doesn't work
        if (!new File(pluginXmlPath).exists()) {
            pluginXmlPath = Paths.get(projectRoot)
                .getParent()
                .getParent()
                .getParent()
                .resolve("plugin.xml")
                .toString();
        }

        String content = new String(Files.readAllBytes(Paths.get(pluginXmlPath)), StandardCharsets.UTF_8);

        // Verify the Android config-file for AndroidManifest.xml does NOT exist
        // The plugin should NOT inject an intent-filter
        assertFalse(
            "plugin.xml should not contain intent-filter config for AndroidManifest.xml",
            content.contains("<config-file target=\"AndroidManifest.xml\"") &&
            content.contains("intent-filter")
        );
    }

    @Test
    public void pluginXmlContainsAndroidPluginRegistration() throws Exception {
        // Read plugin.xml from the project root
        String projectRoot = System.getProperty("user.dir");
        String pluginXmlPath = Paths.get(projectRoot).resolve("plugin.xml").toString();
        
        // Fall back to relative path if above doesn't work
        if (!new File(pluginXmlPath).exists()) {
            pluginXmlPath = Paths.get(projectRoot)
                .getParent()
                .getParent()
                .getParent()
                .resolve("plugin.xml")
                .toString();
        }

        String content = new String(Files.readAllBytes(Paths.get(pluginXmlPath)), StandardCharsets.UTF_8);

        // Verify the Android plugin registration is present
        assertTrue(
            "plugin.xml should contain Android OAuth plugin registration",
            content.contains("com.ayogo.cordova.oauth.OAuthPlugin")
        );

        // Verify config preferences are still present
        assertTrue(
            "plugin.xml should contain OAuthScheme preference",
            content.contains("OAuthScheme")
        );

        assertTrue(
            "plugin.xml should contain OAuthHostname preference",
            content.contains("OAuthHostname")
        );
    }

    @Test
    public void pluginXmlPreservesiOSConfiguration() throws Exception {
        // Read plugin.xml from the project root
        String projectRoot = System.getProperty("user.dir");
        String pluginXmlPath = Paths.get(projectRoot).resolve("plugin.xml").toString();
        
        // Fall back to relative path if above doesn't work
        if (!new File(pluginXmlPath).exists()) {
            pluginXmlPath = Paths.get(projectRoot)
                .getParent()
                .getParent()
                .getParent()
                .resolve("plugin.xml")
                .toString();
        }

        String content = new String(Files.readAllBytes(Paths.get(pluginXmlPath)), StandardCharsets.UTF_8);

        // Verify iOS platform is still present
        assertTrue(
            "plugin.xml should still contain iOS platform configuration",
            content.contains("<platform name=\"ios\">")
        );

        // Verify iOS OAuthPlugin is present
        assertTrue(
            "plugin.xml should still contain iOS OAuthPlugin",
            content.contains("CDVOAuthPlugin")
        );

        // Verify CFBundleURLTypes is present for iOS
        assertTrue(
            "plugin.xml should still contain iOS CFBundleURLTypes configuration",
            content.contains("CFBundleURLTypes")
        );
    }
}
