/* Copyright (C) 2016 Thibaut Le Guilly et Mathieu Mangeot
*
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in
* all copies or substantial portions of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
* THE SOFTWARE.
*/

package jibiki.fr.shishito.Util;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

/**
 * An implementation of <a
 * href="http://java.sun.com/javase/6/docs/api/javax/xml/namespace/NamespaceContext.html">
 * NamespaceContext </a>. Instances are immutable.
 *
 * @author McDowell
 */
final class NamespaceContextMap implements
        NamespaceContext {

    private final Map<String, String> prefixMap;
    private final Map<String, Set<String>> nsMap;

    /**
     * Constructor that takes a map of XML prefix-namespaceURI values. A defensive
     * copy is made of the map. An IllegalArgumentException will be thrown if the
     * map attempts to remap the standard prefixes defined in the NamespaceContext
     * contract.
     *
     * @param prefixMappings
     *          a map of prefix:namespaceURI values
     */
    private NamespaceContextMap(
            Map<String, String> prefixMappings) {
        prefixMap = createPrefixMap(prefixMappings);
        nsMap = createNamespaceMap(prefixMap);
    }

    /**
     * Convenience constructor.
     *
     * @param mappingPairs
     *          pairs of prefix-namespaceURI values
     */
    public NamespaceContextMap(String... mappingPairs) {
        this(toMap(mappingPairs));
    }

    private static Map<String, String> toMap(
            String... mappingPairs) {
        Map<String, String> prefixMappings = new HashMap<>(
                mappingPairs.length / 2);
        for (int i = 0; i < mappingPairs.length; i++) {
            prefixMappings
                    .put(mappingPairs[i], mappingPairs[++i]);
        }
        return prefixMappings;
    }

    private Map<String, String> createPrefixMap(
            Map<String, String> prefixMappings) {
        Map<String, String> prefixMap = new HashMap<>(
                prefixMappings);
        addConstant(prefixMap, XMLConstants.XML_NS_PREFIX,
                XMLConstants.XML_NS_URI);
        addConstant(prefixMap, XMLConstants.XMLNS_ATTRIBUTE,
                XMLConstants.XMLNS_ATTRIBUTE_NS_URI);
        return Collections.unmodifiableMap(prefixMap);
    }

    private void addConstant(Map<String, String> prefixMap,
                             String prefix, String nsURI) {
        String previous = prefixMap.put(prefix, nsURI);
        if (previous != null && !previous.equals(nsURI)) {
            throw new IllegalArgumentException(prefix + " -> "
                    + previous + "; see NamespaceContext contract");
        }
    }

    private Map<String, Set<String>> createNamespaceMap(
            Map<String, String> prefixMap) {
        Map<String, Set<String>> nsMap = new HashMap<>();
        for (Map.Entry<String, String> entry : prefixMap
                .entrySet()) {
            String nsURI = entry.getValue();
            Set<String> prefixes = nsMap.get(nsURI);
            if (prefixes == null) {
                prefixes = new HashSet<>();
                nsMap.put(nsURI, prefixes);
            }
            prefixes.add(entry.getKey());
        }
        for (Map.Entry<String, Set<String>> entry : nsMap
                .entrySet()) {
            Set<String> readOnly = Collections
                    .unmodifiableSet(entry.getValue());
            entry.setValue(readOnly);
        }
        return nsMap;
    }

    @Override
    public String getNamespaceURI(String prefix) {
        checkNotNull(prefix);
        String nsURI = prefixMap.get(prefix);
        return nsURI == null ? XMLConstants.NULL_NS_URI : nsURI;
    }

    @Override
    public String getPrefix(String namespaceURI) {
        checkNotNull(namespaceURI);
        Set<String> set = nsMap.get(namespaceURI);
        return set == null ? null : set.iterator().next();
    }

    @Override
    public Iterator<String> getPrefixes(String namespaceURI) {
        checkNotNull(namespaceURI);
        Set<String> set = nsMap.get(namespaceURI);
        return set.iterator();
    }

    private void checkNotNull(String value) {
        if (value == null) {
            throw new IllegalArgumentException("null");
        }
    }

    /**
     * @return an unmodifiable map of the mappings in the form prefix-namespaceURI
     */
    public Map<String, String> getMap() {
        return prefixMap;
    }

}