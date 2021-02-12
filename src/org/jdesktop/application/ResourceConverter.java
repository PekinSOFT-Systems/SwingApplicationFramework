/*
 * Copyright (C) 2021 PekinSOFT Systems
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
 * 
 * *****************************************************************************
 *  Project    :   SwingApplicationFramework
 *  Class      :   ResourceConverter.java
 *  Author     :   Sean Carrick
 *  Created    :   Feb 11, 2021 @ 9:48:44 AM
 *  Modified   :   Feb 11, 2021
 *  
 *  Purpose:     See class JavaDoc comment.
 *  
 *  Revision History:
 *  
 *  WHEN          BY                   REASON
 *  ------------  -------------------  -----------------------------------------
 *  ??? ??, 2006  Hans Muller          Initial creation.
 *  Feb 11, 2021  Sean Carrick         Updated to Java 11.
 * *****************************************************************************
 */
package org.jdesktop.application;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A base class for converting arbitrary types to and from Strings, as well as a
 * registry of ResourceConverter implementations.
 * <p>
 * The <code>supportsType</code> method defines what types a ResourceConverter
 * supports. By default it returns true for classes that are equal to the
 * constructor's <code>type</code> argument. The <code>parseType</code> methods 
 * converts a string the ResourceConverter's supported type, and the 
 * <code>toString</code> does the inverse, it converts a supported type to a 
 * String. Concrete ResourceConverter subclasses must override <code>parseType()
 * </code> and, in most cases, the <code>toString</code> method as well.</p>
 * <p>
 * This class maintains a registry of ResourceConverters. The <code>forType</code>
 * method returns the first ResourceConverter that supports a particular type,
 * new ResourceConverters can be added with <code>register()</code>. A small set 
 * of generic ResourceConverters are registered by default. They support the 
 * following types:</p>
 * <ul>
 * <li><code>Boolean</code></li>
 * <li><code>Integer</code></li>
 * <li><code>Float</code></li>
 * <li><code>Double</code></li>
 * <li><code>Long</code></li>
 * <li><code>Short</code></li>
 * <li><code>Byte</code></li>
 * <li><code>MessageFormat</code></li>
 * <li><code>URL</code></li>
 * <li><code>URI</code></li>
 * </ul>
 * <p>
 * The Boolean ResourceConverter returns true for "true", "on", "yes", false
 * otherwise. The other primitive type ResourceConverters rely on the
 * corresponding static parse<i>Type</i> method, e.g. <code>Integer.parseInt() 
 * </code>. The MessageFormat ResourceConverter just creates MessageFormat object
 * with the string as its constructor argument. The URL/URI converters just apply 
 * the corresponding constructor to the resource string.</p>
 *
 * @author Hans Muller (Original Author) &lt;current email unknown&gt;
 * @author Sean Carrick (Updater) &lt;sean at pekinsoft dot com&gt;
 * 
 * @version 1.05
 * @since 1.03
 * 
 * @see ResourceMap
 */
public abstract class ResourceConverter {

    protected final Class type;

    public abstract Object parseString(String s, ResourceMap r)
            throws ResourceConverterException;

    public String toString(Object obj) {
        return (obj == null) ? "null" : obj.toString();
    }

    protected ResourceConverter(Class type) {
        if (type == null) {
            throw new IllegalArgumentException("null type");
        }
        this.type = type;
    }

    private ResourceConverter() {
        type = null;
    }  // not used

    public boolean supportsType(Class testType) {
        return type.equals(testType);
    }

    public static class ResourceConverterException extends Exception {

        private final String badString;

        private String maybeShorten(String s) {
            int n = s.length();
            return (n < 128) ? s : s.substring(0, 128) + "...[" + (n - 128) 
                    + " more characters]";
        }

        public ResourceConverterException(String message, String badString,
                Throwable cause) {
            super(message, cause);
            this.badString = maybeShorten(badString);
        }

        public ResourceConverterException(String message, String badString) {
            super(message);
            this.badString = maybeShorten(badString);
        }

        /**
         * {@inheritDoc }
         * @return {@inheritDoc }
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(super.toString());
            sb.append(" string: \"");
            sb.append(badString);
            sb.append("\"");
            return sb.toString();
        }
    }

    /**
     * Registers a <code>ResourceConverter</code> with the registry of the 
     * various <code>ResourceConverter</code>s.
     * 
     * @param resourceConverter the <code>ResourceConverter</code> to register
     */
    public static void register(ResourceConverter resourceConverter) {
        if (resourceConverter == null) {
            throw new IllegalArgumentException("null resourceConverter");
        }
        resourceConverters.add(resourceConverter);
    }

    /**
     * Retrieves a <code>ResourceConverter</code> for the specified type.
     * 
     * @param type the class type converter
     * @return the first <code>ResourceConverter</code> that supports the given
     *          type
     */
    public static ResourceConverter forType(Class type) {
        if (type == null) {
            throw new IllegalArgumentException("null type");
        }
        for (ResourceConverter sc : resourceConverters) {
            if (sc.supportsType(type)) {
                return sc;
            }
        }
        return null;
    }

    private static ResourceConverter[] resourceConvertersArray = {
        new BooleanResourceConverter("true", "on", "yes"),
        new IntegerResourceConverter(),
        new MessageFormatResourceConverter(),
        new FloatResourceConverter(),
        new DoubleResourceConverter(),
        new LongResourceConverter(),
        new ShortResourceConverter(),
        new ByteResourceConverter(),
        new URLResourceConverter(),
        new URIResourceConverter()
    };
    private static List<ResourceConverter> resourceConverters
            = new ArrayList<>(Arrays.asList(resourceConvertersArray));

    private static class BooleanResourceConverter extends ResourceConverter {

        private final String[] trueStrings;

        BooleanResourceConverter(String... trueStrings) {
            super(Boolean.class);
            this.trueStrings = trueStrings;
        }

        /**
         * {@inheritDoc }
         * 
         * @param s {@inheritDoc }
         * @param ignore {@inheritDoc }
         * @return {@inheritDoc }
         */
        @Override
        public Object parseString(String s, ResourceMap ignore) {
            s = s.trim();
            for (String trueString : trueStrings) {
                if (s.equalsIgnoreCase(trueString)) {
                    return Boolean.TRUE;
                }
            }
            return Boolean.FALSE;
        }

        /**
         * {@inheritDoc }
         * 
         * @param testType {@inheritDoc }
         * @return {@inheritDoc }
         */
        @Override
        public boolean supportsType(Class testType) {
            return testType.equals(Boolean.class) 
                    || testType.equals(boolean.class);
        }
    }

    private static abstract class NumberResourceConverter 
            extends ResourceConverter {

        private final Class primitiveType;

        NumberResourceConverter(Class type, Class primitiveType) {
            super(type);
            this.primitiveType = primitiveType;
        }

        /**
         * Parses the provided <code>java.lang.String</code> into a valid number.
         * If the string is not able to be parsed to a valid number, then a
         * <code>java.lang.NumberFormatException</code> is thrown.
         * 
         * @param s the <code>java.lang.String</code> to be parsed
         * @return the number to which <code>s</code> was parsed
         * @throws <code>NumberFormatException</code> in the event an error
         *          occurs while parsing the resource 
         * 
         * @see ResourceConverter
         * @see java.lang.NumberFormatException
         */
        protected abstract Number parseString(String s) 
                throws NumberFormatException;

        /**
         * {@inheritDoc }
         * 
         * @param s {@inheritDoc }
         * @param ignore {@inheritDoc }
         * @return {@inheritDoc }
         * @throws <code>ResourceConverterException</code> in the event an error
         *          occurs while parsing the resource
         * 
         * @see java.lang.String
         * @see ResourceConverterException
         */
        @Override
        public Object parseString(String s, ResourceMap ignore) 
                throws ResourceConverterException {
            try {
                return parseString(s);
            } catch (NumberFormatException e) {
                throw new ResourceConverterException("invalid " 
                        + type.getSimpleName(), s, e);
            }
        }

        /**
         * {@inheritDoc }
         * 
         * @param testType {@inheritDoc }
         * @return {@inheritDoc }
         */
        @Override
        public boolean supportsType(Class testType) {
            return testType.equals(type) || testType.equals(primitiveType);
        }
    }

    private static class FloatResourceConverter extends NumberResourceConverter {

        FloatResourceConverter() {
            super(Float.class, float.class);
        }

        /**
         * {@inheritDoc }
         * @param s {@inheritDoc }
         * @return {@inheritDoc }
         * @throws {@inheritDoc } 
         */
        @Override
        protected Number parseString(String s) throws NumberFormatException {
            return Float.parseFloat(s);
        }
    }

    private static class DoubleResourceConverter extends NumberResourceConverter {

        DoubleResourceConverter() {
            super(Double.class, double.class);
        }

        /**
         * {@inheritDoc }
         * 
         * @param s {@inheritDoc }
         * @return {@inheritDoc }
         * @throws {@inheritDoc } 
         */
        @Override
        protected Number parseString(String s) throws NumberFormatException {
            return Double.parseDouble(s);
        }
    }

    private static abstract class INumberResourceConverter 
            extends ResourceConverter {

        private final Class primitiveType;

        INumberResourceConverter(Class type, Class primitiveType) {
            super(type);
            this.primitiveType = primitiveType;
        }

        /**
         * Parses a given <code>java.lang.String</code> into a valid number value.
         * In the event that the given string cannot be parsed into a valid
         * number, a <code>java.lang.NumberFormatException</code> is thrown.
         * 
         * @param s the <code>java.lang.String</code> to be parsed
         * @param radix the base of the number
         * @return the parsed numerical value of the provided string
         * @throws NumberFormatException in the event that the string cannot be
         *          parsed into a valid numerical value
         */
        protected abstract Number parseString(String s, int radix) 
                throws NumberFormatException;

        /**
         * {@inheritDoc }
         * @param s {@inheritDoc }
         * @param ignore {@inheritDoc }
         * @return {@inheritDoc }
         * @throws {@inheritDoc }
         */
        @Override
        public Object parseString(String s, ResourceMap ignore) 
                throws ResourceConverterException {
            try {
                String[] nar = s.split("&"); // number ampersand radix
                int radix = (nar.length == 2) ? Integer.parseInt(nar[1]) : -1;
                return parseString(nar[0], radix);
            } catch (NumberFormatException e) {
                throw new ResourceConverterException("invalid " 
                        + type.getSimpleName(), s, e);
            }
        }

        /**
         * {@inheritDoc }
         * 
         * @param testType {@inheritDoc }
         * @return {@inheritDoc }
         */
        @Override
        public boolean supportsType(Class testType) {
            return testType.equals(type) || testType.equals(primitiveType);
        }
    }

    private static class ByteResourceConverter extends INumberResourceConverter {

        ByteResourceConverter() {
            super(Byte.class, byte.class);
        }

        /**
         * {@inheritDoc }
         * 
         * @param s {@inheritDoc }
         * @param radix {@inheritDoc }
         * @return {@inheritDoc }
         * @throws NumberFormatException {@inheritDoc }
         */
        @Override
        protected Number parseString(String s, int radix) 
                throws NumberFormatException {
            return (radix == -1) ? Byte.decode(s) : Byte.parseByte(s, radix);
        }
    }

    private static class IntegerResourceConverter 
            extends INumberResourceConverter {

        IntegerResourceConverter() {
            super(Integer.class, int.class);
        }

        /**
         * {@inheritDoc }
         * 
         * @param s {@inheritDoc }
         * @param radix {@inheritDoc }
         * @return {@inheritDoc }
         * @throws NumberFormatException {@inheritDoc }
         */
        @Override
        protected Number parseString(String s, int radix) 
                throws NumberFormatException {
            return (radix == -1) ? Integer.decode(s) : Integer.parseInt(s, radix);
        }
    }

    private static class LongResourceConverter extends INumberResourceConverter {

        LongResourceConverter() {
            super(Long.class, long.class);
        }

        /**
         * {@inheritDoc }
         * 
         * @param s {@inheritDoc }
         * @param radix {@inheritDoc }
         * @return {@inheritDoc }
         * @throws NumberFormatException {@inheritDoc }
         */
        @Override
        protected Number parseString(String s, int radix) 
                throws NumberFormatException {
            return (radix == -1) ? Long.decode(s) : Long.parseLong(s, radix);
        }
    }

    private static class ShortResourceConverter extends INumberResourceConverter {

        ShortResourceConverter() {
            super(Short.class, short.class);
        }

        /**
         * {@inheritDoc }
         * 
         * @param s {@inheritDoc }
         * @param radix {@inheritDoc }
         * @return {@inheritDoc }
         * @throws NumberFormatException {@inheritDoc }
         */
        @Override
        protected Number parseString(String s, int radix) 
                throws NumberFormatException {
            return (radix == -1) ? Short.decode(s) : Short.parseShort(s, radix);
        }
    }

    private static class MessageFormatResourceConverter 
            extends ResourceConverter {

        MessageFormatResourceConverter() {
            super(MessageFormat.class);
        }

        /**
         * {@inheritDoc }
         * 
         * @param s {@inheritDoc }
         * @param ignore {@inheritDoc }
         * @return {@inheritDoc }
         */
        @Override
        public Object parseString(String s, ResourceMap ignore) {
            return new MessageFormat(s);
        }
    }

    private static class URLResourceConverter extends ResourceConverter {

        URLResourceConverter() {
            super(URL.class);
        }

        /**
         * {@inheritDoc }
         * 
         * @param s {@inheritDoc }
         * @param ignore {@inheritDoc }
         * @return {@inheritDoc }
         * @throws ResourceConverterException {@inheritDoc }
         */
        @Override
        public Object parseString(String s, ResourceMap ignore) 
                throws ResourceConverterException {
            try {
                return new URL(s);
            } catch (MalformedURLException e) {
                throw new ResourceConverterException("invalid URL", s, e);
            }
        }
    }

    private static class URIResourceConverter extends ResourceConverter {

        URIResourceConverter() {
            super(URI.class);
        }

        /**
         * {@inheritDoc }
         * @param s {@inheritDoc }
         * @param ignore {@inheritDoc }
         * @return {@inheritDoc }
         * @throws ResourceConverterException {@inheritDoc }
         */
        @Override
        public Object parseString(String s, ResourceMap ignore) 
                throws ResourceConverterException {
            try {
                return new URI(s);
            } catch (URISyntaxException e) {
                throw new ResourceConverterException("invalid URI", s, e);
            }
        }
    }
}
