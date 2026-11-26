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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Checks the plugin.xml shipped by this experiment: Android callbacks are
 * routed by the host app, so the plugin must not inject its own intent-filter.
 */
public class PluginXmlConfigTest {
    private static Document pluginXml;

    @BeforeClass
    public static void loadPluginXml() throws Exception {
        // Gradle runs unit tests from tests/android; walk up to the repository root.
        File dir = new File(System.getProperty("user.dir")).getAbsoluteFile();
        while (dir != null && !new File(dir, "plugin.xml").isFile()) {
            dir = dir.getParentFile();
        }
        assertNotNull("plugin.xml not found above " + System.getProperty("user.dir"), dir);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        pluginXml = factory.newDocumentBuilder().parse(new File(dir, "plugin.xml"));
        assertEquals("cordova-plugin-oauth", pluginXml.getDocumentElement().getAttribute("id"));
    }

    @Test
    public void androidPlatformDoesNotInjectIntentFilter() {
        Element android = platform("android");

        assertEquals(0, android.getElementsByTagName("intent-filter").getLength());
        for (Element configFile : children(android, "config-file")) {
            assertTrue(
                "Unexpected AndroidManifest.xml config-file",
                !configFile.getAttribute("target").contains("AndroidManifest.xml")
            );
        }
    }

    @Test
    public void androidPluginIsStillRegistered() {
        Element android = platform("android");

        boolean registered = false;
        NodeList params = android.getElementsByTagName("param");
        for (int i = 0; i < params.getLength(); i++) {
            Element param = (Element) params.item(i);
            if ("android-package".equals(param.getAttribute("name"))
                    && "com.ayogo.cordova.oauth.OAuthPlugin".equals(param.getAttribute("value"))) {
                registered = true;
            }
        }
        assertTrue("OAuth android-package registration missing", registered);

        boolean sourceIncluded = false;
        for (Element source : children(android, "source-file")) {
            if ("src/android/OAuthPlugin.java".equals(source.getAttribute("src"))) {
                sourceIncluded = true;
            }
        }
        assertTrue("OAuthPlugin.java source-file missing", sourceIncluded);
    }

    @Test
    public void callbackPreferencesAreStillWritten() {
        List<String> names = new ArrayList<>();
        for (Element configFile : children(pluginXml.getDocumentElement(), "config-file")) {
            for (Element preference : children(configFile, "preference")) {
                names.add(preference.getAttribute("name") + "=" + preference.getAttribute("value"));
            }
        }

        assertTrue(names.toString(), names.contains("OAuthScheme=$URL_SCHEME"));
        assertTrue(names.toString(), names.contains("OAuthHostname=$URL_HOSTNAME"));
    }

    @Test
    public void iosUrlSchemeRegistrationIsKept() {
        Element ios = platform("ios");

        NodeList strings = ios.getElementsByTagName("string");
        boolean schemeRegistered = false;
        for (int i = 0; i < strings.getLength(); i++) {
            if ("$URL_SCHEME".equals(strings.item(i).getTextContent().trim())) {
                schemeRegistered = true;
            }
        }
        assertTrue("iOS CFBundleURLSchemes entry missing", schemeRegistered);
    }

    private static Element platform(String name) {
        for (Element platform : children(pluginXml.getDocumentElement(), "platform")) {
            if (name.equals(platform.getAttribute("name"))) {
                return platform;
            }
        }
        throw new AssertionError("Platform " + name + " missing from plugin.xml");
    }

    private static List<Element> children(Element parent, String localName) {
        List<Element> result = new ArrayList<>();
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && localName.equals(node.getLocalName())) {
                result.add((Element) node);
            }
        }
        return result;
    }
}
