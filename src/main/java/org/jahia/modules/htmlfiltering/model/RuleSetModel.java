/*
 * Copyright (C) 2002-2025 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.htmlfiltering.model;

import java.util.List;

public class RuleSetModel {
    private List<String> protocols;
    private List<ElementModel> elements;

    public List<String> getProtocols() {
        return protocols;
    }

    public void setProtocols(List<String> protocols) {
        this.protocols = protocols;
    }

    public List<ElementModel> getElements() {
        return elements;
    }

    public void setElements(List<ElementModel> elements) {
        this.elements = elements;
    }

    @Override
    public String toString() {
        return "RuleSetModel{" +
                "protocols=" + protocols +
                ", elements=" + elements +
                '}';
    }
}
