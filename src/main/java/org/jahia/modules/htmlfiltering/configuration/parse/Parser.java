/*
 * Copyright (C) 2002-2024 Jahia Solutions Group SA. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jahia.modules.htmlfiltering.configuration.parse;

import org.json.JSONArray;
import org.json.JSONObject;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public abstract class Parser {

    public static final String ELEMENTS = "elements";

    private enum PolicyType {
        ALLOW, DISALLOW
    }

    private static final Map<String, Pattern> PATTERNS;

    static {
        PATTERNS = new HashMap<>();
        String onsiteUrl = "(?:[\\p{L}\\p{N}\\\\\\.\\#@\\$%\\+&;\\-_~,\\?=/!{}:]+|\\#(\\w)+)";
        String offsiteUrl = "(\\s*(?:(?:ht|f)tps?://|mailto:)[\\p{L}\\p{N}][\\p{L}\\p{N}\\p{Zs}\\.\\#@\\$%\\+&;:\\-_~,\\?=/!\\(\\)"
                + "]*+\\s*)";
        PATTERNS.put("NUMBER_OR_PERCENT", Pattern.compile("\\d+%?"));
        PATTERNS.put("ONSITE_URL", Pattern.compile(onsiteUrl));
        PATTERNS.put("OFFSITE_URL", Pattern.compile(onsiteUrl));
        PATTERNS.put("LINKS_URL", Pattern.compile(String.format("(?:%s|%s)", onsiteUrl, offsiteUrl)));
        PATTERNS.put("HTML_ID", Pattern.compile("[a-zA-Z0-9\\:\\-_\\.]+"));
        PATTERNS.put("HTML_CLASS", Pattern.compile("[a-zA-Z0-9\\s,\\-_]+"));
        PATTERNS.put("NUMBER", Pattern.compile("[+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)"));
        PATTERNS.put("NAME", Pattern.compile("[a-zA-Z0-9\\-_\\$]+"));
        PATTERNS.put("ALIGN", Pattern.compile("(?i)center|left|right|justify|char"));
        PATTERNS.put("VALIGN", Pattern.compile("(?i)baseline|bottom|middle|top"));
        PATTERNS.put("PARAGRAPH", Pattern.compile("(?:[\\p{L}\\p{N},'\\.\\s\\-_\\(\\)]|&\\d{2};)*+"));
    }

    public abstract String parseToJsonString(JSONObject json);

    public static PolicyFactory parseToPolicy(JSONObject json) {
        HtmlPolicyBuilder builder = new HtmlPolicyBuilder();
        JSONObject filtering = json.getJSONObject("htmlFiltering");

        handleAllow(filtering, builder);
        handleDisallow(filtering, builder);

        return builder.toFactory();
    }

    private static void handleAllow(JSONObject filtering, HtmlPolicyBuilder builder) {
        handleAttributes(filtering, builder, PolicyType.ALLOW);
        handleElements(filtering, builder, PolicyType.ALLOW);
        handleProtocols(filtering, builder, PolicyType.ALLOW);
    }

    private static void handleDisallow(JSONObject filtering, HtmlPolicyBuilder builder) {
        if (filtering.has("disallow")) {
            JSONObject dis = filtering.getJSONObject("disallow");
            handleAttributes(dis, builder, PolicyType.DISALLOW);
            handleElements(dis, builder, PolicyType.DISALLOW);
            handleProtocols(dis, builder, PolicyType.DISALLOW);
        }
    }

    private static void handleProtocols(JSONObject filtering, HtmlPolicyBuilder builder, PolicyType policyType) {
        if (filtering.has("protocols")) {
            Object p = filtering.get("protocols");

            p = convertToJSONArray(p);

            if (policyType == PolicyType.ALLOW) {
                builder.allowUrlProtocols(jsonArrayToArray((JSONArray) p));
            } else {
                builder.disallowUrlProtocols(jsonArrayToArray((JSONArray) p));
            }
        }
    }

    private static void handleElements(JSONObject filtering, HtmlPolicyBuilder builder, PolicyType policyType) {
        if (filtering.has(ELEMENTS)) {
            JSONArray elems = filtering.getJSONArray(ELEMENTS);

            elems.forEach(jsonObject -> {
                Object name = ((JSONObject)jsonObject).get("name");

                name = convertToJSONArray(name);

                if (policyType == PolicyType.ALLOW) {
                    builder.allowElements(jsonArrayToArray((JSONArray) name));
                } else {
                    builder.disallowElements(jsonArrayToArray((JSONArray) name));
                }
            });
        }
    }

    private static void handleAttributes(JSONObject filtering, HtmlPolicyBuilder builder, PolicyType policyType) {
        if  (filtering.has("attributes")) {
            JSONArray attr = filtering.getJSONArray("attributes");

            attr.forEach(jsonObject -> {
                boolean isGlobal = !((JSONObject)jsonObject).has(ELEMENTS);
                Object name = ((JSONObject)jsonObject).get("name");

                name = convertToJSONArray(name);

                HtmlPolicyBuilder.AttributeBuilder attrBuilder;

                if (policyType == PolicyType.ALLOW) {
                    attrBuilder = builder.allowAttributes(jsonArrayToArray((JSONArray) name));
                } else {
                    attrBuilder = builder.disallowAttributes(jsonArrayToArray((JSONArray) name));
                }

                if (((JSONObject)jsonObject).has("pattern")) {
                    String pattern = ((JSONObject)jsonObject).getString("pattern");
                    Pattern p = PATTERNS.containsKey(pattern) ? PATTERNS.get(pattern) : Pattern.compile(pattern);
                    attrBuilder.matching(p);
                }

                handleGlobal((JSONObject) jsonObject, isGlobal, attrBuilder);
            });
        }
    }

    private static void handleGlobal(JSONObject jsonObject, boolean isGlobal, HtmlPolicyBuilder.AttributeBuilder attrBuilder) {
        if (isGlobal) {
            attrBuilder.globally();
        } else {
            Object elems = jsonObject.get(ELEMENTS);

            elems = convertToJSONArray(elems);

            attrBuilder.onElements(jsonArrayToArray((JSONArray) elems));
        }
    }

    private static Object convertToJSONArray(Object name) {
        if (!(name instanceof JSONArray)) {
            name = new JSONArray(new String[]{(String) name});
        }
        return name;
    }

    private static String[] jsonArrayToArray(JSONArray array) {
        String[] a = new String[array.length()];
        for (int i = 0; i < array.length(); i++) {
            a[i] = array.getString(i);
        }
        return a;
    }
}
