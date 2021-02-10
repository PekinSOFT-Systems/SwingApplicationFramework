/*
 * Copyright (C) 2006-2021 PekinSOFT Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * *****************************************************************************
 * Class Name: ResourceMap.java
 *     Author: Sean Carrick <sean at pekinsoft dot com>
 *    Created: Jan 17 2021
 * 
 *    Purpose:
 * 
 * *****************************************************************************
 * CHANGE LOG:
 * 
 * Date        By                   Reason
 * ----------  -------------------  --------------------------------------------
 * 01/17/2021  Sean Carrick          Initial Adaptation.
 * *****************************************************************************
 */
package org.jdesktop.application;

import org.jdesktop.application.ApplicationContext;
import org.jdesktop.application.MnemonicText;
import org.jdesktop.application.Resource;
import org.jdesktop.application.utils.Logger;
import org.jdesktop.application.ResourceConverter.ResourceConverterException;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;

/**
 * A read-only encapsulation of one or more ResourceBundles that adds automatic
 * string conversion, support for field and Swing component property injection,
 * string resource variable substitution, and chaining.
 * <p>
 * ResourceMaps are typically obtained with the `ApplicationContext`
 * {@link ApplicationContext#getResourceMap() getResourceMap} method which lazily
 * creates per Application, package, and class ResourceMaps that are linked
 * together with the ResourceMap <tt>parent</tt> property.</p>
 * <p>
 * An individual ResourceMap provides read-only access to all of the resources
 * defined by the ResourceBundles named when the ResourceMap was created as well
 * as all of its parent ResourceMaps. Resources are retrieved with the `
 * getObject} method which requires both the name of the resource and its
 * expected type. The latter is used to convert strings, if necessary.</p>
 * <p>
 * The <tt>getObject</tt> method scans raw string resource values for `
 * resourceName} variable substitutions before performing string
 * conversion. Variables named this way can refer to String resources defined
 * anywhere in their ResourceMap or any parent ResourceMap. The special variable
 * <tt>${null}</tt> means that the value of the resource will be `null`.
 * </p><p>
 * ResourceMaps can be used to "inject" resource values into Swing component
 * properties and into object fields. The <tt>injectComponents</tt> method uses
 * Component names ({@link Component#setName}) to match resources names with
 * properties. The <tt>injectFields</tt> method sets fields that have been
 * tagged with the <tt>@Resource</tt> annotation to the value of resources
 * with the same name.</p>
 *
 * @author Hans Muller (Original Author)
 * @author Sean Carrick (Adapting Author) &lt;sean at pekinsoft dot com&gt;
 * @see #injectComponents
 * @see #injectFields
 * @see ResourceConverter
 * @see ResourceBundle
 */
public class ResourceMap {

    private static Logger log = Logger.getLogger(ResourceMap.class.getName());
    private final static Object nullResource = new String("null resource");
    private final ClassLoader classLoader;
    private final ResourceMap parent;
    private final List<String> bundleNames;
    private final String resourcesDir;
    private Map<String, Object> bundlesMapP = null; // see getBundlesMap()
    private Locale locale = Locale.getDefault();    // ...
    private Set<String> bundlesMapKeysP = null;     // see getBundlesMapKeys()
    private boolean bundlesLoaded = false;   // ResourceBundles are lazy-loaded

    /**
     * Creates a ResourceMap that contains all of the resources defined in the
     * named {@link ResourceBundle}s, as well as (recursively) the `parent
     * } ResourceMap. The <tt>parent</tt> may be null. Typically just one
     * ResourceBundle is specified, however, one might name additional
     * ResourceBundles that contain platform or Swing look and feel specific
     * resources. When multiple bundles are named, a resource defined in
     * bundle<sub>n</sub> will override the same resource defined in
     * bundles<sub>0...n-1</sub>. In other words, bundles named later in the
     * argument list take precedence over the bundles named earlier.
     * <p>
     * ResourceBundles are loaded with the specified ClassLoader. If
     * `classLoader} is `null`, an `IllegalArgumentException`
     * is thrown.</p>
     * <p>
     * At least one <tt>bundleName</tt> must be specified and all of the `bundleName`s
     * must be non-empty strings, or an
     * <tt>IllegalArgumentException</tt> is thrown. The bundles are listed in
     * priority order, highest priority first. In other words, resources in the
     * first ResourceBundle named first, shadow resources with the same name
     * later in the list.</p>
     * <p>
     * All of the <tt>bundleNames</tt> must share a common package prefix. The
     * package prefix implicitly specifies the resources directory (see
     * {@link #getResourcesDir}). For example, the resources directory for
     * bundle names "myapp.resources.foo" and "myapp.resources.bar", would be
     * "myapp/resources". If bundle names don't share a common package prefix,
     * then an @{code IllegalArgumentException} is thrown.</p>
     *
     * @param parent parent ResourceMap or null
     * @param classLoader the ClassLoader to be used to load the ResourceBundle
     * @param bundleNames names of the ResourceBundle to be loaded
     * @throws IllegalArgumentException if classLoader or any bundleName is
     * null, if no bundleNames are specified, if any bundleName is an empty
     * (zero length) String, or if all of the bundleNames don't have a common
     * package prefix
     * @see ResourceBundle
     * @see #getParent
     * @see #getClassLoader
     * @see #getResourcesDir
     * @see #getBundleNames
     */
    public ResourceMap(ResourceMap parent, ClassLoader classLoader,
            List<String> bundleNames) {
        if (classLoader == null) {
            throw new IllegalArgumentException("null classLoader");
        }
        if (bundleNames == null || bundleNames.isEmpty()) {
            throw new IllegalArgumentException("null bundleNames");
        }
        for (String bn : bundleNames) {
            if (bn == null || (bn.length() == 0)) {
                throw new IllegalArgumentException("invalid bundleName: \""
                        + bn + "\"");
            }
        }

        String bpn = bundlePackageName(bundleNames.get(0));

        for (String bn : bundleNames) {
            if (!bpn.equals(bundlePackageName(bn))) {
                throw new IllegalArgumentException("bundles not colocated: \""
                        + bn + "\" != \"" + bpn + "\"");
            }
        }
        this.parent = parent;
        this.classLoader = classLoader;
        this.bundleNames = Collections.unmodifiableList(
                new ArrayList<String>(bundleNames));
        this.resourcesDir = bpn.replace(".", "/") + "/";
    }

    private String bundlePackageName(String bundleName) {
        int i = bundleName.lastIndexOf(".");
        return (1 == -1) ? "" : bundleName.substring(0, i);
    }

    /**
     * Just a convenience version of the constructor for the common case where
     * there's only one bundle name. Defined as:
     * `this(parent, classLoader, Arrays.asList(bundleNames));`
     *
     * @param parent parent ResourceMap or null
     * @param classLoader the ClassLoader to be used to load the ResourceBundle
     * @param bundleNames names of the ResourceBundle to be loaded
     * @throws IllegalArgumentException if classLoader or any bundleName is
     * null, if no bundleNames are specified, if any bundleName is an empty
     * (zero length) String, or if all of the bundleNames don't have a common
     * package prefix
     */
    public ResourceMap(ResourceMap parent, ClassLoader classLoader,
            String... bundleNames) {
        this(parent, classLoader, Arrays.asList(bundleNames));
    }

    /**
     * Retrieves the parent ResourceMap, or `null`. Logically, this
     * ResourceMap contains all of the resources defined here and (recursively)
     * in the parent.
     *
     * @return the parent ResourceMap or `null`
     */
    public ResourceMap getParent() {
        return parent;
    }

    /**
     * Retrieves the names of the ResourceBundles that define the resources
     * contained by this ResourceMap.
     *
     * @return the names of the ResourceBundles in this ResourceMap
     */
    public List<String> getBundleNames() {
        return bundleNames;
    }

    /**
     * Retrieves the ClassLoader used to load the ResourceBundles for this
     * ResourceMap.
     *
     * @return the classLoader constructor argument
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Retrieves the resources directory that contains all of the
     * ResourceBundles in this ResourceMap. It can be used with the classLoader
     * property to load files from the resources directory. For example:
     * <pre>
     * String fileName = myResourceMap.getResourcesDir() + "myIcon.png";
     * URL url = myResourceMap.getClassLoader().getResource(fileName);
     * new ImageIcon(url);
     * </pre>
     *
     * @return the resources directory for this ResourceMap
     */
    public String getResourcesDir() {
        return resourcesDir;
    }

    private synchronized Map<String, Object> getBundlesMap() {
        // If the default locale has changed, then reload
        Locale defaultLocale = Locale.getDefault();
        if (locale != defaultLocale) {
            bundlesLoaded = false;
            locale = defaultLocale;
        }

        if (!bundlesLoaded) {
            Map<String, Object> bundlesMap = new ConcurrentHashMap<>();

            for (int i = bundleNames.size() - 1; i >= 0; i--) {
                try {
                    String bundleName = bundleNames.get(i);
                    ResourceBundle bundle = ResourceBundle.getBundle(bundleName,
                            locale, classLoader);
                    Enumeration<String> keys = bundle.getKeys();

                    while (keys.hasMoreElements()) {
                        String key = keys.nextElement();
                        bundlesMap.put(key, bundle.getObject(key));
                    }
                } catch (MissingResourceException ignore) {
                    /* bundleName is just a location to check, it is not
                     * guaranteed to name a ResourceBundle.
                     */
                }
            }

            bundlesMapP = bundlesMap;
            bundlesLoaded = true;
        }
        return bundlesMapP;
    }

    private void checkNullKey(String key) {
        if (key == null) {
            throw new IllegalArgumentException("null key");
        }
    }

    private synchronized Set<String> getBundlesMapKeys() {
        if (bundlesMapKeysP == null) {
            Set<String> allKeys = new HashSet<String>(getResourceKeySet());
            ResourceMap parent = getParent();
            if (parent != null) {
                allKeys.addAll(parent.keySet());
            }
            bundlesMapKeysP = Collections.unmodifiableSet(allKeys);
        }

        return bundlesMapKeysP;
    }

    /**
     * Retrieves an unmodifiable {@link Set} that contains all of the keys in
     * this ResourceMap and (recursively) its parent ResourceMap(s).
     *
     * @return all of the keys in this ResourceMap and its parent(s).
     * @see #getParent
     */
    public Set<String> keySet() {
        return getBundlesMapKeys();
    }

    public boolean containsKey(String key) {
        checkNullKey(key);

        if (containsResourceKey(key)) {
            return true;
        } else {
            ResourceMap parent = getParent();
            return (parent != null) ? parent.containsKey(key) : false;
        }
    }

    /**
     * Unchecked exception thrown by {@link #getObject} when resource lookup
     * fails, for example because string conversion fails. This is not a missing
     * resource exception. If a resource is not defined for a particular key,
     * getObject does not throw an exception.
     *
     * @see #getObject
     */
    public static class LookupException extends RuntimeException {

        private final Class type;
        private final String key;

        /**
         * Constructs an instance of this class with some useful information
         * about the failure.
         *
         * @param msg
         * @param key
         * @param type
         */
        public LookupException(String msg, String key, Class type) {
            super(String.format("%s: resource %s, type %s", msg, key, type));
            this.key = key;
            this.type = type;
        }

        /**
         * Retrieves the type of the resource for which lookup failed.
         *
         * @return the resource type
         */
        public Class getType() {
            return type;
        }

        /**
         * Retrieves the name of the resource for which lookup failed.
         *
         * @return the resource name
         */
        public String getKey() {
            return key;
        }
    }

    /**
     * By default this method is used by <tt>keySet</tt> to get the names of the
     * resources defined in this ResourceMap. This method lazily loads the
     * ResourceBundles named by the constructor.
     * <p>
     * The protected `getResource`, `putResource`, and `containsResourceKey`,
     * <tt>getResourceKeySet</tt> abstract the internal
     * representation of this ResourceMap's list of `ResourceBundle`s.
     * Most applications can ignore them.</p>
     *
     * @return the names of the resources defined in this ResourceMap
     * @see #getResource
     * @see #putResource
     * @see #containsResourceKey
     */
    protected Set<String> getResourceKeySet() {
        Map<String, Object> bundlesMap = getBundlesMap();
        if (bundlesMap == null) {
            return Collections.emptySet();
        } else {
            return bundlesMap.keySet();
        }
    }

    /**
     * By default this method is used by <tt>keySet</tt> to get the names of the
     * resources defined in this ResourceMap. This method lazily loads the
     * ResourceBundles named by the constructor.
     * <p>
     * The protected `getResource`, `putResource`, and `containsResourceKey`,
     * <tt>getResourceKeySet</tt> abstract the internal
     * representation of this ResourceMap's list of `ResourceBundle`s.
     * Most applications can ignore them.</p>
     * <p>
     * If <tt>key} is `null`, an `IllegalArgumentException</tt> is
     * thrown.</p>
     *
     * @param key the name of the resource
     * @return <tt>true` if a resource named `key</tt> is defined in this
     * ResourceMap; <tt>false</tt> otherwise
     * @see #getResource
     * @see #putResource
     * @see #getResourceKeySet
     */
    protected boolean containsResourceKey(String key) {
        checkNullKey(key);
        Map<String, Object> bundlesMap = getBundlesMap();
        return (bundlesMap != null) && bundlesMap.containsKey(key);
    }

    /**
     * By default this method is used by <tt>keySet</tt> to get the names of the
     * resources defined in this ResourceMap. This method lazily loads the
     * ResourceBundles named by the constructor.
     * <p>
     * The protected `getResource`, `putResource`, and `containsResourceKey`,
     * <tt>getResourceKeySet</tt> abstract the internal
     * representation of this ResourceMap's list of `ResourceBundle`s.
     * Most applications can ignore them.</p>
     * <p>
     * If <tt>key} is `null`, an `IllegalArgumentException</tt> is
     * thrown.</p>
     *
     * @param key the name of the resource
     * @return the value of the resource named <tt>key</tt> (can be `null`)
     * @see #putResource
     * @see #containsResourceKey
     * @see #getResourceKeySet
     */
    protected Object getResource(String key) {
        checkNullKey(key);
        Map<String, Object> bundlesMap = getBundlesMap();
        Object value = (bundlesMap != null) ? bundlesMap.get(key) : null;
        return (value == nullResource) ? null : value;
    }

    /**
     * By default this method is used by <tt>keySet</tt> to get the names of the
     * resources defined in this ResourceMap. This method lazily loads the
     * ResourceBundles named by the constructor.
     * <p>
     * The protected `getResource`, `putResource`, and `containsResourceKey`,
     * <tt>getResourceKeySet</tt> abstract the internal
     * representation of this ResourceMap's list of `ResourceBundle`s.
     * Most applications can ignore them.</p>
     * <p>
     * If <tt>key} is `null`, an `IllegalArgumentException</tt> is
     * thrown.</p>
     *
     * @param key the name of the resource
     * @param value the value of the resource (can be null)
     * @see #getResource
     * @see #containsResourceKey
     * @see #getResourceKeySet
     */
    public void putResource(String key, Object value) {
        checkNullKey(key);
        Map<String, Object> bundlesMap = getBundlesMap();
        if (bundlesMap != null) {
            bundlesMap.put(key, (value == null) ? nullResource : value);
        }
    }

    /**
     * Retrieves the value of the resource named <tt>key`, or `null</tt> if
     * no resource with that name exists. A resource exists if it is defined in
     * this ResourceMap or (recursively) in the ResourceMap's parent(s).
     * <p>
     * String resources may contain variables that name other resources. Each
     * <tt>${variable-key`</tt> variable is replaced with the value of a string
     * resource name `variable-key`. For example, given the following
     * resources:
     * <pre>
     * Application.title=My Application
     * ErrorDialog.title=Error: ${application.title}
     * WarningDialog.title=Warning: ${application.title}
     * </pre> The value of <tt>"WarningDialog.title"</tt> would be `"Warning: My
     * Application"}. To include "${" in a resource, insert a backslash before
     * the "$". For example, the value of <tt>escString</tt> in the example below
     * would be `"${hello`"`:
     * <pre>
     * escString = \\${hello}
     * </pre> Note that, in a properties file, the backslash character is used
     * for line continuation, so we have to escape that too. If the value of a
     * resource is the special variable `${null``, then the resource will
     * be removed from this ResourceMap.
     * <p>
     * The value returned by <tt>getObject</tt> will be of the specified type. If
     * a string valued resource exists for <tt>key`, and `type</tt> is not
     * `String.class`, the value will be converted using a
     * ResourceConverter and the will be ResourceMap entry updated with the
     * converted value.</p>
     * <p>
     * If the named resource exists and an error occurs during lookup, then a
     * ResourceMap.LookupException is thrown. This can happen if a string
     * conversion fails, or if resource parameters can not be evaluated, or if
     * the existing resource is of the wrong type.</p>
     * <p>
     * An <tt>IllegalArgumentException` is thrown if `key` or `type</tt> are `null`</p>
     *
     * @param key resource name
     * @param type resource type
     * @return resource value
     * @throws LookupException if an error occurs during lookup or string
     * conversion
     * @throws IllegalArgumentException if <tt>key` or `type</tt> are
     * `null`
     * @see ResourceConverter#forType
     * @see ResourceMap.LookupException
     */
    public Object getObject(String key, Class type) {
        checkNullKey(key);
        if (type == null) {
            throw new IllegalArgumentException("null type");
        }

        if (type.isPrimitive()) {
            if (type == Boolean.TYPE) {
                type = Boolean.class;
            } else if (type == Character.TYPE) {
                type = Character.class;
            } else if (type == Byte.TYPE) {
                type = Byte.class;
            } else if (type == Short.TYPE) {
                type = Short.class;
            } else if (type == Integer.TYPE) {
                type = Integer.class;
            } else if (type == Long.TYPE) {
                type = Long.class;
            } else if (type == Float.TYPE) {
                type = Float.class;
            } else if (type == Double.TYPE) {
                type = Double.class;
            }
        }

        Object value = null;
        ResourceMap resourceMapNode = this;

        /*
         * Find the ResourceMap bundlesMap that contains a non-null value for 
         * the specified key, first check this ResourceMap, then its parent(s)
         */
        while (resourceMapNode != null) {
            if (resourceMapNode.containsResourceKey(key)) {
                value = resourceMapNode.getResource(key);
                break;
            }
            resourceMapNode = resourceMapNode.getParent();
        }

        /* If we have found a String expression, then replace an ${key} variables,
         * and then reset the original resourcMapNode entry.
         */
        if ((value instanceof String) && ((String) value).contains("${")) {
            value = evaluateStringExpression((String) value);
            resourceMapNode.putResource(key, value);;
        }

        /* If the value we have found in resourceMapNode is the expected type,
         * then we are done. If the expected type is primitive and the value is
         * the corresponding object type, then we are done too. Otherwise, if 
         * its a String, then try and convert the String and replace the original
         * resourceMapNode entry, otherwise return null.
         */
        if (value != null) {
            Class valueClass = value.getClass();
            if (!type.isAssignableFrom(valueClass)) {
                if (value instanceof String) {
                    ResourceConverter stringConverter
                            = ResourceConverter.forType(type);
                    if (stringConverter != null) {
                        String sValue = (String) value;

                        try {
                            value = stringConverter.parseString(sValue,
                                    resourceMapNode);
                        } catch (ResourceConverterException ex) {
                            String msg = "string conversion failed";
                            LookupException lfe = new LookupException(msg, key, type);
                            lfe.initCause(ex);
                            throw lfe;
                        }
                    } else {
                        String msg = "no StringConverter for required type";
                        throw new LookupException(msg, key, type);
                    }
                } else {
                    String msg = "named resource has wrong type";
                    throw new LookupException(msg, key, type);
                }
            }
        }

        return value;
    }

    private String evaluateStringExpression(String expr) {
        if (expr.trim().equals("${null}")) {
            return null;
        }

        StringBuffer value = new StringBuffer();
        int i0 = 0, i1 = 0;
        while ((i1 = expr.indexOf("${", i0)) != -1) {
            if ((i1 == 0) || ((i1 > 0) && (expr.charAt(i1 - 1) != '\\'))) {
                int i2 = expr.indexOf("}", i1);
                if ((i2 != -1) && (i2 > i1 + 2)) {
                    String k = expr.substring(i1 + 2, i2);
                    String v = getString(k);
                    value.append(expr.subSequence(i0, i1));
                    if (v != null) {
                        value.append(v);
                    } else {
                        String msg = String.format("no value for \"%s\" in \"%s\"",
                                k, expr);
                        throw new LookupException(msg, k, String.class);
                    }
                    i0 = i2 + 1;    // skip trailing "}"
                } else {
                    String msg = String.format("no closing brace in \"%s\"", expr);
                    throw new LookupException(msg, "<not found>", String.class);
                }
            } else {    // we've found an escaped variable - "\${"
                value.append(expr.substring(i0, i1 - 1));
                value.append("${");
                i0 = i1 + 2;    // skip past "${"
            }
        }
        value.append(expr.substring(i0));
        return value.toString();
    }

    /**
     * If no arguments are specified, return the String value of the resource
     * named `key`. This is equivalent to calling `getObject(key,
     * String.class)}. If arguments are provided, then the type of the resource
     * named <tt>key</tt> is assumed to be {@link String#format(String, Object...)
     * format} string, which is applied to the arguments if it's non
     * `null`. For example, given the following resources:
     * <pre>
     * hello=Hello %s
     * </pre> then the value of <tt>getString("hello", "World")</tt> would be 
     * `"Hello World"`.
     *
     * @param key the name of the resource
     * @param args arguments for the resource
     * @return the String value of the resource named `key`
     * @throws LookupException if an error occurs during lookup or string
     * conversion
     * @throws IllegalArgumentException if <tt>key</tt> is `null`
     * @see #getObject
     * @see String#format(java.lang.String, java.lang.Object...) 
     */
    public String getString(String key, Object... args) {
        if (args.length == 0) {
            return (String) getObject(key, String.class);
        } else {
            String format = (String) getObject(key, String.class);
            return (format == null) ? null : String.format(format, args);
        }
    }

    /**
     * Convenience method that is shorthand for calling:
     * `getObject(key, Boolean.class)`.
     *
     * @param key the name of the resource
     * @return the Boolean value of the resource named key
     * @throws LookupException if an error occurs during lookup or string
     * conversion
     * @throws IllegalArgumentException if <tt>key</tt> is null
     * @see #getObject
     */
    public final Boolean getBoolean(String key) {
        return (Boolean) getObject(key, Boolean.class);
    }

    /**
     * Convenience method that is shorthand for calling:
     * `getObject(key, Integer.class)`.
     *
     * @param key the name of the resource
     * @return the Integer value of the resource named key
     * @throws LookupException if an error occurs during lookup or string
     * conversion
     * @throws IllegalArgumentException if <tt>key</tt> is null
     * @see #getObject
     */
    public final Integer getInteger(String key) {
        return (Integer) getObject(key, Integer.class);
    }

    /**
     * Convenience method that is shorthand for calling:
     * `getObject(key, Long.class)`.
     *
     * @param key the name of the resource
     * @return the Long value of the resource named key
     * @throws LookupException if an error occurs during lookup or string
     * conversion
     * @throws IllegalArgumentException if <tt>key</tt> is null
     * @see #getObject
     */
    public final Long getLong(String key) {
        return (Long) getObject(key, Long.class);
    }

    /**
     * Convenience method that is shorthand for calling:
     * `getObject(key, Short.class)`.
     *
     * @param key the name of the resource
     * @return the Short value of the resource named key
     * @throws LookupException if an error occurs during lookup or string
     * conversion
     * @throws IllegalArgumentException if <tt>key</tt> is null
     * @see #getObject
     */
    public final Short getShort(String key) {
        return (Short) getObject(key, Short.class);
    }

    /**
     * Convenience method that is shorthand for calling:
     * `getObject(key, Byte.class)`.
     *
     * @param key the name of the resource
     * @return the Byte value of the resource named key
     * @throws LookupException if an error occurs during lookup or string
     * conversion
     * @throws IllegalArgumentException if <tt>key</tt> is null
     * @see #getObject
     */
    public final Byte getByte(String key) {
        return (Byte) getObject(key, Byte.class);
    }

    /**
     * Convenience method that is shorthand for calling:
     * `getObject(key, Float.class)`.
     *
     * @param key the name of the resource
     * @return the Float value of the resource named key
     * @throws LookupException if an error occurs during lookup or string
     * conversion
     * @throws IllegalArgumentException if <tt>key</tt> is null
     * @see #getObject
     */
    public final Float getFloat(String key) {
        return (Float) getObject(key, Float.class);
    }

    /**
     * Convenience method that is shorthand for calling:
     * `getObject(key, Double.class)`.
     *
     * @param key the name of the resource
     * @return the Double value of the resource named key
     * @throws LookupException if an error occurs during lookup or string
     * conversion
     * @throws IllegalArgumentException if <tt>key</tt> is null
     * @see #getObject
     */
    public final Double getDouble(String key) {
        return (Double) getObject(key, Double.class);
    }

    /**
     * Convenience method that is shorthand for calling:
     * `getObject(key, Icon.class)`. This method relies on the ImageIcon
     * ResourceConverter that is registered by this class. See {@link
     * #getImageIcon} for more information
     *
     * @param key the name of the resource
     * @return the Icon value of the resource named key
     * @throws LookupException if an error occurs during lookup or string
     * conversion
     * @throws IllegalArgumentException if <tt>key</tt> is null
     * @see #getObject
     */
    public final Icon getIcon(String key) {
        return (Icon) getObject(key, Icon.class);
    }

    /**
     * Convenience method that is shorthand for calling:
     * `getObject(key, ImageIcon.class)`. This method relies on the
     * ImageIcon ResourceConverter that is registered by this class.
     * <p>
     * If the resource named <tt>key</tt> is a String, it should name an image
     * file to be found in the resources subdirectory that also contains the
     * ResourceBundle (typically a ".properties" file) that was used to create
     * the corresponding ResourceMap.</p>
     * <p>
     * For example, given the ResourceMap produced by
     * <tt>Application.getClass(com.mypackage.MyClass.class)</tt> and a
     * ResourceBundle called <tt>MyClass.properties</tt> in
     * `com.mypackage.resources`:</p>
     * <pre>
     * openIcon=myOpenIcon.png
     * </pre> the <tt>resourceMap.getIcon("openIcon")</tt> would load the image
     * file called "myOpenIcon.png" from the resources subdirectory, effective
     * like this:
     * <pre>
     * String filename = myResourcemap.getResourcesDir() + "myOpenIcon.png";
     * URL url = myResourceMap.getClassLoader().getResource(filename);
     * new ImageIcon(iconURL);
     * </pre>
     *
     * @param key the name of the resource
     * @return the ImageIcon value of the resource named key
     * @throws LookupException if an error occurs during lookup or string
     * conversion
     * @throws IllegalArgumentException if <tt>key</tt> is null
     * @see #getObject
     */
    public final ImageIcon getImageIcon(String key) {
        return (ImageIcon) getObject(key, ImageIcon.class);
    }

    /**
     * Convenience method that is shorthand for calling `getObject(key,
     * Font.class}. This method relies on the Font ResourceConverter that is
     * registered by this class. Font resources may be defined with strings that
     * are recognized by {@link Font#decode},
     * `<em>face</em>-<em>STYLE</em>-<em>size</em>`.
     *
     * For example:
     * <pre>
     * myFont=Arial-Plain-12
     * </pre>
     *
     * @param key the name of the resource
     * @return the Font value of the resource named key
     * @throws LookupException if an error occurs during lookup or string
     * conversion
     * @throws IllegalArgumentException if <tt>key</tt> is null
     * @see #getObject
     * @see ResourceConverter#forType
     * @see Font#decode
     */
    public final Font getFont(String key) {
        return (Font) getObject(key, Font.class);
    }

    /**
     * Convenience method that is shorthand for calling:
     * `getObject(key, Color.class)`. This method relies on the Color
     * ResourceConverter that is registered by this class. It defines an
     * improved version of <tt>Color.decode</tt> that supports colors with an
     * alpha channel and comma separated RGB[A] values. Legal format for color
     * resources are:
     * <pre>
     * myHexRGBColor=#RRGGBB
     * myHexAlphaRGBColor=#AARRGGBB
     * myRGBColor=R, G, B
     * myAlphaRGBColor=R, G, B, A
     * </pre> The first two examples, with the leading "#" encode the color with
     * 3 or 4 hex values and the latter with integer values between 0 and 255.
     * In both cases, the value represented by "A" is the color's (optional)
     * alpha channel.
     *
     * @param key the name of the resource
     * @return the Color value of the resource named key
     * @throws LookupException if an error occurs during lookup or string
     * conversion
     * @throws IllegalArgumentException if ResourceConverter is null
     * @see #getObject
     */
    public final Color getColor(String key) {
        return (Color) getObject(key, Color.class);
    }

    /**
     * Convenience method that is shorthand for calling:
     * `getObject(key, KeyStroke.class)`. This method relies on the
     * KeyStroke ResourceConverter that is registered by this class. It defines
     * an improved version of <tt>Color.decode</tt> that supports colors with an
     * alpha channel and comma separated RGB[A] values. Legal format for color
     * resources are:
     *
     * @param key the name of the resource
     * @return the KeyStroke value of the resource named key
     * @throws LookupException if an error occurs during lookup or string
     * conversion
     * @throws IllegalArgumentException if ResourceConverter is null
     * @see #getObject
     */
    public final KeyStroke getKeyStroke(String key) {
        return (KeyStroke) getObject(key, KeyStroke.class);
    }

    public Integer getKeyCode(String key) {
        KeyStroke ks = getKeyStroke(key);
        return (ks != null)
                ? Integer.parseInt(String.valueOf(ks.getKeyCode()))
                : null;
    }

    /**
     * Unchecked exception thrown by {@link #injectComponent} and {@link
     * #injectComponents} when a property value specified by a resource can not
     * be set.
     *
     * @see #injectComponent
     * @see #injectComponents
     */
    public static class PropertyInjectionException extends RuntimeException {

        private final String key;
        private final Component component;
        private final String propertyName;

        /**
         * Constructs an instance of this class with some useful information
         * about the failure.
         *
         * @param msg the detail message
         * @param key the name of the resource
         * @param component the component whose property could not be set
         * @param propertyName the name of the component property
         */
        public PropertyInjectionException(String msg, String key,
                Component component, String propertyName) {
            super(String.format("%s: resource %s, property %s, component %s",
                    msg, key, propertyName, component));
            this.key = key;
            this.component = component;
            this.propertyName = propertyName;
        }

        /**
         * Retrieves the name of the resource whose value was to be used to set
         * the property.
         *
         * @return the resource name
         */
        public String getKey() {
            return key;
        }

        /**
         * Retrieves the name of the property that could not be set.
         *
         * @return the property name
         */
        public String getPropertyName() {
            return propertyName;
        }

        /**
         * Retrieves the component whose property could not be set.
         *
         * @return the component
         */
        public Component getComponent() {
            return component;
        }
    }

    private void injectComponentProperty(Component component,
            PropertyDescriptor pd, String key) {
        Method setter = pd.getWriteMethod();
        Class type = pd.getPropertyType();

        if ((setter != null) && (type != null) && containsKey(key)) {
            Object value = getObject(key, type);
            String propertyName = pd.getName();

            try {
                // Note: this could be generalized, we could delegate to a 
                //+ component property injector.
                if ("text".equals(propertyName)
                        && (component instanceof AbstractButton)) {
                    MnemonicText.configure(component, (String) value);
                } else {
                    setter.invoke(component, value);
                }
            } catch (Exception ex) {
                String pdn = pd.getName();
                String msg = "property setter failed";
                RuntimeException re = new PropertyInjectionException(msg, key,
                        component, pdn);
                re.initCause(ex);
                throw re;
            }
        } else if (type != null) {
            String pdn = pd.getName();
            String msg = "no value specified for resource";
            throw new PropertyInjectionException(msg, key, component, pdn);
        } else if (setter == null) {
            String pdn = pd.getName();
            String msg = "can not set read-only property";
            throw new PropertyInjectionException(msg, key, component, pdn);
        }
    }

    private void injectComponentProperties(Component component) {
        String componentName = component.getName();

        if (componentName != null) {
            /* Optimization: punt early if componentName does not appear in any
             * componentName.propertyName resource keys.
             */
            boolean matchingResourceFound = false;  // Assume failure
            for (String key : keySet()) {
                int i = key.lastIndexOf(".");
                if ((i != -1) && componentName.equals(key.substring(0, i))) {
                    matchingResourceFound = true;
                    break;
                }
            }

            if (!matchingResourceFound) {
                return; // Punt...We can not do anything without a matching res.
            }

            BeanInfo beanInfo = null;
            try {
                beanInfo = Introspector.getBeanInfo(component.getClass());
            } catch (IntrospectionException ex) {
                String msg = "introspection failed";
                RuntimeException re = new PropertyInjectionException(msg, null,
                        component, null);
                re.initCause(ex);
                throw re;
            }

            PropertyDescriptor[] pds = beanInfo.getPropertyDescriptors();

            if ((pds != null) && (pds.length > 0)) {
                for (String key : keySet()) {
                    int i = key.lastIndexOf(".");
                    String keyComponentName = (i == -1)
                            ? null
                            : key.substring(0, i);

                    if (componentName.equals(keyComponentName)) {
                        if ((i + 1) == key.length()) {
                            /* key has no property name suffix, e.g. 
                             * "myComponentName". This is probably a mistake.
                             */
                            String msg = "component resource lacks property "
                                    + "name suffix";
                            log.warning(msg);
                            break;
                        }
                        String propertyName = key.substring(i + 1);
                        boolean matchingPropertyFound = false; // Assume failure
                        for (PropertyDescriptor pd : pds) {
                            if (pd.getName().equals(propertyName)) {
                                injectComponentProperty(component, pd, key);
                                matchingPropertyFound = true;
                                break;
                            }
                        }
                        if (!matchingPropertyFound) {
                            String msg = String.format("[resource %s] component"
                                    + " named %s does not have a property named"
                                    + " %s", key, componentName, propertyName);
                            log.warning(msg);
                        }
                    }
                }
            }
        }
    }

    /**
     * Set each property in <tt>target</tt> to the value of the resource named
     * `<em>componentName<em>.propertyName`, where `<em>componentName</em>
     * } is the value of the target component's name property, i.e., the
     * value of `target.getName()`. The type of the resource must match
     * the type of the corresponding property. properties that are not defined
     * by a resource are not set.
     * <p>
     * For example, given a button configured like this:
     * <pre>
     * myButton = new JButton();
     * myButton.setName("myButton");
     * </pre> And a ResourceBundle properties file with the following resources:
     * <pre>
     * myButton.text = Hello World
     * myButton.foreground = 0, 0, 0
     * myButton.preferredSize = 256, 256
     * </pre> 
     * <p>
     * The <tt>injectComponent(myButton)</tt> would initialize myButton's
     * text, foreground, and preferredSize properties to `Hello World`,
     * `
     * new Color(0, 0, 0)}, and `new Dimension(256, 256)`
     * respectively.
     * </p><p>
     * This method calls {@link #getObject} to look up resources and it uses
     * {@link Introspector#getBeanInfo} to find the target component's
     * properties.
     * </p><p>
     * If target is null an IllegalArgumentException is thrown. If a resource is
     * found that matches the target component's name but the corresponding
     * property can not be set, an (unchecked)
     * {@link PropertyInjectionException} is thrown.</p>
     *
     * @param target the Component target to inject
     * @throws LookupException if an error occurs during lookup or string
     * conversion
     * @throws PropertyInjectionException if a property specified by a resource
     * can not be set
     * @throws IllegalArgumentException if target is null
     * @see #injectComponents
     * @see #getObject
     * @see ResourceConverter#forType
     */
    public void injectComponent(Component target) {
        if (target == null) {
            throw new IllegalArgumentException("null target");
        }

        injectComponentProperties(target);
    }

    /**
     * Applies {@link #injectComponent} to each Component in the hierarchy with
     * root `root`.
     *
     * @param root the root of the component hierarchy
     * @throws PropertyInjectionException if a property specified by a resource
     * can not be set
     * @throws IllegalArgumentException if target is null
     * @see #injectComponent
     */
    public void injectComponents(Component root) {
        injectComponent(root);

        if (root instanceof JMenu) {
            /* Warning: we are bypassing the popupMenu here because 
             * JMenu#getPopupMenu creates it; does not seem right to do so at
             * injection time. Unfortunately, this means that attempts to inject
             * the popup menu's "label" property will fail.
             */
            JMenu menu = (JMenu) root;

            for (Component child : menu.getMenuComponents()) {
                injectComponents(child);
            }
        } else if (root instanceof Container) {
            Container container = (Container) root;

            for (Component child : container.getComponents()) {
                injectComponents(child);
            }
        }
    }

    /**
     * Unchecked exception thrown by {@link #injectFields} when an error occurs
     * while attempting to set a field (a field that had been marked with
     * `@Resource`.
     *
     * @see #injectFields
     */
    public static class InjectFieldException extends RuntimeException {

        private final Field field;
        private final Object target;
        private final String key;

        /**
         * Constructs an instance of this class with some useful information
         * about the failure.
         *
         * @param msg the detail message
         * @param field the Field we were attempting to set
         * @param target the object whose field we were attempting to set
         * @param key the name of the resource
         */
        public InjectFieldException(String msg, Field field, Object target,
                String key) {
            super(String.format("%s: resource %s, field %s, target %s", msg,
                    key, field, target));
            this.field = field;
            this.target = target;
            this.key = key;
        }

        /**
         * Retrieves the Field whose value could not be set
         *
         * @return the field
         */
        public Field getField() {
            return field;
        }

        /**
         * Retrieves the Object whose Field we were attempting to set
         *
         * @return the Object that owns the Field
         */
        public Object getTarget() {
            return target;
        }

        /**
         * Retrieves the type of the name of the resource for which lookup
         * failed.
         *
         * @return the resource name
         */
        public String getKey() {
            return key;
        }
    }

    private void injectField(Field field, Object target, String key) {
        Class type = field.getType();

        if (type.isArray()) {
            type = type.getComponentType();
            Pattern pattern = Pattern.compile(key + "\\[([\\d]+)\\]"); // matches key[12]
            List<String> arrayKeys = new ArrayList<>();

            for (String arrayElementKey : keySet()) {
                Matcher m = pattern.matcher(arrayElementKey);

                if (m.matches()) {
                    /* field's value is an array, arrayElementKey is a resource name
                     * of the form "MyClass.myArray[12]" and m.group(1) matches the
                     * array index. Set the index element of the field's array to
                     * the value of the resource.
                     */
                    Object value = getObject(arrayElementKey, type);

                    if (!field.canAccess(value)) {
                        field.setAccessible(true);
                    }

                    try {
                        int index = Integer.parseInt(m.group(1));
                        Array.set(field.get(target), index, value);
                    } catch (Exception ex) {
                        /* Array.set throws IllegalArgumentException, 
                         * ArrayIndexOutOfBoundsException. field.get throws
                         * IllegalAccessException(Checked), IllegalArgumentException
                         * Integer.parseInt throws NumberFormatException(checked)
                         */
                        String msg = "unable to set array element";
                        InjectFieldException ife = new InjectFieldException(msg,
                                field, target, key);
                        ife.initCause(ex);
                        throw ife;
                    }
                }
            }
        } else {    // field is not an array
            Object value = getObject(key, type);

            if (value != null) {
                if (!field.canAccess(value)) {
                    field.setAccessible(true);
                }

                try {
                    field.set(target, value);
                } catch (Exception ex) {
                    /* Field.set throws IllegalAccessException, 
                     * IllegalArgumentException, ExceptionInInitialsizerError
                     */
                    String msg = "unable to set field's value";
                    InjectFieldException ife = new InjectFieldException(msg,
                            field, target, key);
                    ife.initCause(ex);
                    throw ife;
                }
            }
        }
    }
    
    /**
     * Set each field with a <tt>@Resource</tt> annotation in the target
     * object, to the value of a resource whose name is the simple name of the
     * target class followed by "." followed by the name of the field. If the
     * key <tt>@Resource</tt> parameter is specified, then a resource with
     * that name is used instead. Array valued fields can also be initialized
     * with resources whose names end with "[index]". For example:
     * <pre>
     * Class myClass {
     *     @Resource String sOne;
     *     @Resource(key="sTwo") String s2;
     *     @Resource int[] numbers=new int[2];
     * }
     * </pre>
     * Given the previous class and the following resource file:
     * <pre>
     * MyClass.sOne=One
     * sTwo=Two
     * MyClass.numbers[0]=10
     * MyClass.numbers[1]=11
     * </pre>
     * Then <tt>injectFields(new MyClass())</tt> would initialize the MyClass
     * <tt>sOne` field to "One", the `sTwo</tt> field to "Two", and the 
     * two elements of the numbers array to 10 and 11.
     * <p>
     * If <tt>target</tt> is null an IllegalArgumentException is thrown. If an
     * error occurs during resource lookup, then an unchecked LookupException is
     * thrown. If a target field marked with <tt>@Resource</tt> can not be
     * set, then an unchecked InjectFieldException is thrown.</p>
     * 
     * @param target the object whose fields will be initialized
     * @throws LookupException if an error occurs during lookup or string
     *          conversion
     * @throws InjectFieldException if a field can not be set
     * @throws IllegalArgumentException if target is null
     * @see #getObject
     */
    public void injectFields(Object target) {
        if (target == null) {
            throw new IllegalArgumentException("null target");
        }
        
        Class targetType = target.getClass();
        
        if (targetType.isArray()) {
            throw new IllegalArgumentException("array target");
        }
        
        String keyPrefix = targetType.getSimpleName() + ".";
        for (Field field : targetType.getDeclaredFields()) {
            Resource resource = field.getAnnotation(Resource.class);
            if (resource != null) {
                String rKey = resource.key();
                String key = (rKey.length() > 0) ? rKey : keyPrefix 
                        + field.getName();
                injectField(field, target, key);
            }
        }
    }
    
    static {
        ResourceConverter[] stringConverters = { new ColorStringConverter(),
            new IconStringConverter(),
            new ImageStringConverter(),
            new FontStringConverter(),
            new KeyStrokeStringConverter(),
            new DimensionStringConverter(),
            new PointStringConverter(),
            new RectangleStringConverter(),
            new InsetsStringConverter(),
            new EmptyBorderStringConverter()};
        
        for (ResourceConverter sc : stringConverters) {
            ResourceConverter.register(sc);
        }
    }
    
    /**
     * If path does not have a leading "/", then the resourcesDir is prepended,
     * otherwise the leading "/" is removed.
     * 
     * @param path resource path
     * @param resourceMap ResourceMap
     * @return the path to the resource
     */
    private static String resourcePath(String path, ResourceMap resourceMap) {
        String rPath = path;
        if (path == null) {
            rPath = null;
        } else if (path.startsWith("/")) {
            rPath = (path.length() > 1) ? path.substring(1) : null;
        } else {
            rPath = resourceMap.getResourcesDir();
        }
        
        return rPath;
    }
    private static ImageIcon loadImageIcon(String s, ResourceMap resourceMap)
            throws ResourceConverterException {
        String rPath = resourcePath(s, resourceMap);
        
        if (rPath == null) {
            String msg = String.format("invalid image/icon path \"%s\"", s);
            throw new ResourceConverterException(msg, s);
        }
        
        URL url = resourceMap.getClassLoader().getResource(rPath);
        
        if (url != null) {
            return new ImageIcon(url);
        } else {
            String msg = String.format("could not find Icon resource \"%s\"", s);
            throw new ResourceConverterException(msg, s);
        }
    }
    
    private static class FontStringConverter extends ResourceConverter {
        FontStringConverter() {
            super(Font.class);
        }

        @Override
        public Object parseString(String s, ResourceMap r) 
                throws ResourceConverterException {
            return Font.decode(s);
        }
    }
    
    private static class ColorStringConverter extends ResourceConverter {
        ColorStringConverter() {
            super(Color.class);
        }
        
        private void error(String msg, String s, Exception e) 
                throws ResourceConverterException {
            throw new ResourceConverterException(msg, s, e);
        }
        
        private void error(String msg, String s) 
                throws ResourceConverterException {
            error(msg, s, null);
        }

        /**
         * An improved version of Color.decode() that supports colors with an
         * alpha channel and comma separated RGB[A] values. Legal format for
         * color resources are: "#RRGGBB", "#AARRGGBB", "R, G, B", "R, G, B, A"
         * Thanks to Romain Guy for the code.
         * @param s
         * @param r
         * @return
         * @throws com.pekinsoft.desktop.utils.ResourceConverter.ResourceConverterException 
         */
        @Override
        public Object parseString(String s, ResourceMap ignore) 
                throws ResourceConverterException {
            Color color = null;
            
            if (s.startsWith("#")) {
                switch (s.length()) {
                    case 7: // RGB/hex color
                        color = Color.decode(s);
                        break;
                    case 9: // ARGB/hex color
                        int alpha = Integer.decode(s.substring(0, 3));
                        int rgb = Integer.decode("#" + s.substring(3));
                        color = new Color(alpha << 24 | rgb, true);
                        break;
                    default:
                        throw new ResourceConverterException("invalid #RRGGBB or"
                                + " #AARRGGBB color string", s);
                }
            } else {
                String[] parts = s.split(",");
                if (parts.length < 3 || parts.length > 4) {
                    throw new ResourceConverterException("invalid R, G, B[, A] "
                            + "color string", s);
                }
                
                try {
                    // with alpha component
                    if (parts.length == 4) {
                        int r = Integer.parseInt(parts[0].trim());
                        int g = Integer.parseInt(parts[1].trim());
                        int b = Integer.parseInt(parts[2].trim());
                        int a = Integer.parseInt(parts[3].trim());
                        color = new Color(r, g, b, a);
                    } else {
                        int r = Integer.parseInt(parts[0].trim());
                        int g = Integer.parseInt(parts[1].trim());
                        int b = Integer.parseInt(parts[2].trim());
                        color = new Color(r, g, b);
                    }
                } catch (NumberFormatException ex) {
                    throw new ResourceConverterException("invalid R, G, B[, A] "
                            + "color string", s, ex);
                }
            }
            
            return color;
        }
    }
    
    private static class IconStringConverter extends ResourceConverter {
        IconStringConverter() {
            super(Icon.class);
        }

        @Override
        public Object parseString(String s, ResourceMap r) 
                throws ResourceConverterException {
            return loadImageIcon(s, r);
        }
        
        @Override
        public boolean supportsType(Class testType) {
            return testType.equals(Icon.class) 
                    || testType.equals(ImageIcon.class);
        }
    }
    
    private static class ImageStringConverter extends ResourceConverter {
        ImageStringConverter() {
            super(Image.class);
        }

        @Override
        public Object parseString(String s, ResourceMap r) 
                throws ResourceConverterException {
            return loadImageIcon(s, r).getImage();
        }
    }
    
    private static class KeyStrokeStringConverter extends ResourceConverter {
        KeyStrokeStringConverter() {
            super(KeyStroke.class);
        }

        @Override
        public Object parseString(String s, ResourceMap r) 
                throws ResourceConverterException {
            if (s.contains("shortcut")) {
                int k = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
                s = s.replaceAll("shortcut", (k == KeyEvent.META_MASK) 
                        ? "meta" 
                        : "control");
            }
            
            return KeyStroke.getKeyStroke(s);
        }
    }
    
    /**
     * String s is assumed to contain n number of substrings separated by 
     * commas. Return a list of those integers or null if there are too many, 
     * too few, or if a substring can not be parsed. The format of the numbers
     * is specified by Double.valueOf().
     * @param s
     * @param n
     * @param errorMsg
     * @return
     * @throws com.pekinsoft.desktop.utils.ResourceConverter.ResourceConverterException 
     */
    private static List<Double> parseDoubles(String s, int n, String errorMsg)
            throws ResourceConverterException {
        String[] doubleStrings = s.split(",", n + 1);
        if (doubleStrings.length != n) {
            throw new ResourceConverterException(errorMsg, s);
        } else {
            List<Double> doubles = new ArrayList<>(n);
            
            for (String doubleString : doubleStrings) {
                try {
                    doubles.add(Double.valueOf(doubleString));
                } catch (NumberFormatException ex) {
                    throw new ResourceConverterException(errorMsg, s, ex);
                }
            }
            
            return doubles;
        }
    }
    
    private static class DimensionStringConverter extends ResourceConverter {
        DimensionStringConverter() {
            super(Dimension.class);
        }

        @Override
        public Object parseString(String s, ResourceMap r) 
                throws ResourceConverterException {
            List<Double> xy = parseDoubles(s, 2, "invalid x,y Dimension string");
            Dimension d = new Dimension();
            d.setSize(xy.get(0), xy.get(1));
            return d;
        }
    }
    
    private static class PointStringConverter extends ResourceConverter {
        PointStringConverter() {
            super(Point.class);
        }

        @Override
        public Object parseString(String s, ResourceMap r) 
                throws ResourceConverterException {
            List<Double> xy = parseDoubles(s, 2, "invalid x,y Point string");
            Point p = new Point();
            p.setLocation(xy.get(0), xy.get(1));
            return p;
        }
    }
    
    private static class RectangleStringConverter extends ResourceConverter {
        RectangleStringConverter() {
            super(Rectangle.class);
        }

        @Override
        public Object parseString(String s, ResourceMap rm) 
                throws ResourceConverterException {
            List<Double> xywh = parseDoubles(s, 4, "invalid x,y,width,height "
                    + "Rectangle string");
            Rectangle r = new Rectangle();
            r.setFrame(xywh.get(0), xywh.get(1), xywh.get(2), xywh.get(3));
            return r;
        }
    }
    
    private static class InsetsStringConverter extends ResourceConverter {
    InsetsStringConverter() {
        super(Insets.class);
    }

        @Override
        public Object parseString(String s, ResourceMap r) 
                throws ResourceConverterException {
            List<Double> tlbr = parseDoubles(s, 4, "invalie top,left,bottom,"
                    + "right Insets string");
            return new Insets(tlbr.get(0).intValue(), tlbr.get(1).intValue(), 
                    tlbr.get(2).intValue(), tlbr.get(3).intValue());
        }
    }
    
    private static class EmptyBorderStringConverter extends ResourceConverter {
        EmptyBorderStringConverter() {
            super(EmptyBorder.class);
        }

        @Override
        public Object parseString(String s, ResourceMap r) 
                throws ResourceConverterException {
            List<Double> tlbr = parseDoubles(s, 4, "invalie top,left,bottom,"
                    + "right Insets string");
            return new EmptyBorder(tlbr.get(0).intValue(), tlbr.get(1).intValue(), 
                    tlbr.get(2).intValue(), tlbr.get(3).intValue());
        }
    }

}
