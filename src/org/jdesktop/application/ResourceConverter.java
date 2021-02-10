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
 * Class Name: ResourceConverter.java
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
 * 01/17/2021  Sean Carrick          Initial Creation.
 * *****************************************************************************
 */
package org.jdesktop.application;

import org.jdesktop.application.ResourceMap;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A base class for converting arbitrary types to and from {@code String}s, as
 * well as a registry of ResourceConverter implementations.
 * <p>
 * The {@code supportsType} method defines what types a ResourceConverter 
 * supports. By default it returns {@code true} for classes that are equal to 
 * the constructor's {@code type} argument. The {@code parseType} methods 
 * converts a string to the ResourceConverter's supported type, and the {@code 
 * toString} does the inverse: it converts a supported type to a {@code String}.
 * Concrete ResourceConverter subclasses must override {@code parseType()} and,
 * in most cases, the {@code toString()} method as well.</p>
 * <p>
 * This class maintains a registry of ResourceConverters. The {@code forType}
 * method returns the first ResourceConverter that supports a particular type,
 * new ResourceConverters can be added with {@code register()}. A small set of
 * generic ResourceConverters are registered by default. They support the 
 * following types:</p>
 * <ul>
 * <li><tt>Boolean</tt></li>
 * <li><tt>Integer</tt></li>
 * <li><tt>Float</tt></li>
 * <li><tt>Double</tt></li>
 * <li><tt>Long</tt></li>
 * <li><tt>Short</tt></li>
 * <li><tt>Byte</tt></li>
 * <li><tt>MessageType</tt></li>
 * <li><tt>URL</tt></li>
 * <li><tt>URI</tt></li>
 * </ul>
 * <p>
 * The Boolean ResourceConverter returns {@code true} for "true", "on", "yes";
 * {@code false} otherwise. The other primitive type ResourceConverters rely on
 * the corresponding {@code static parse<i>Type</i>} method, e.g., {@code 
 * Integer.parseInt()}. The MessageFormat ResourceConverter just creates 
 * MessageFormat object with the string as its constructor argument. The URL/URI
 * converters just apply the corresponding constructor to the resource string.
 * </p>
 * 
 * @author Hans Muller (Original Author)
 * @author Sean Carrick (Adapting Author) &lt;sean at pekinsoft dot com&gt;
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
    
    public boolean supportsType(Class testType) {
        return type.equals(testType);
    }
    
    public static class ResourceConverterException extends Exception {
        private final String badString;
        
        private String maybeShorten(String s) {
            int n = s.length();
            return (n < 128) ? s : s.substring(0, 128) + "...[" + (n-128) 
                    + "more characters]";
        }
        
        public ResourceConverterException(String message, String badString,
                Throwable cause) {
            super(message, cause);
            this.badString = maybeShorten(badString);
        }
        
        public ResourceConverterException(String message, String badString) {
            super(message);
            this.badString = maybeShorten(message);
        }
        
        public String toString() {
            StringBuffer sb = new StringBuffer(super.toString());
            sb.append(" string: \"");
            sb.append(badString);
            sb.append("\"");
            return sb.toString();
        }
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
    private static List<ResourceConverter> resourceConverters = new ArrayList<>(
            Arrays.asList(resourceConvertersArray));
        
    public static void register(ResourceConverter resourceConverter) {
        if (resourceConverter == null) {
            throw new IllegalArgumentException("null resourceConverter");
        }

        resourceConverters.add(resourceConverter);
    }

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
    
    private static class BooleanResourceConverter extends ResourceConverter {
        private final String[] trueStrings;
        
        BooleanResourceConverter(String ... trueStrings) {
            super(Boolean.class);
            this.trueStrings = trueStrings;
        }

        @Override
        public Object parseString(String s, ResourceMap r) 
                throws ResourceConverterException {
            s = s.trim();
            for (String trueString : trueStrings) {
                if (s.equalsIgnoreCase(trueString)) {
                    return Boolean.TRUE;
                }
            }
            
            return Boolean.FALSE;
        }
        
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
        
        protected abstract Number parseString(String s) 
                throws NumberFormatException;
        
        @Override
        public Object parseString(String s, ResourceMap r) 
                throws ResourceConverterException {
            try {
                return parseString(s);
            } catch (NumberFormatException ex) {
                throw new ResourceConverterException("invalid " 
                        + type.getSimpleName(), s, ex);
            }
        }
        
        @Override
        public boolean supportsType(Class testType) {
            return testType.equals(type) || testType.equals(primitiveType);
        }
    }
    
    private static class FloatResourceConverter extends NumberResourceConverter {
        FloatResourceConverter() {
            super(Float.class, float.class);
        }

        @Override
        protected Number parseString(String s) throws NumberFormatException {
            return Float.parseFloat(s);
        }
    }
    
    private static class DoubleResourceConverter extends NumberResourceConverter {
        DoubleResourceConverter() {
            super(Double.class, double.class);
        }

        @Override
        protected Number parseString(String s) throws NumberFormatException {
            return Double.parseDouble(s);
        }
    }
    
    private static abstract class INumberResourceConverter extends ResourceConverter {
        private final Class primitiveType;
        
        INumberResourceConverter(Class type, Class primitiveType) {
            super(type);
            this.primitiveType = primitiveType;
        }
        
        protected abstract Number parseString(String s, int radix) 
                throws NumberFormatException;
        
        @Override
        public Object parseString(String s, ResourceMap ignor) 
                throws ResourceConverterException {
            try {
                String[] nar = s.split("&");    // number ampersand radix
                int radix = (nar.length == 2) ? Integer.parseInt(nar[1]) : -1;
                return parseString(nar[0], radix);
            } catch (NumberFormatException ex) {
                throw new ResourceConverterException("invalid " 
                        + type.getSimpleName(), s, ex);
            }
        }
        
        @Override
        public boolean supportsType(Class testType) {
            return testType.equals(type) || testType.equals(primitiveType);
        }
    }
    
    private static class ByteResourceConverter extends INumberResourceConverter {
        ByteResourceConverter() {
            super(Byte.class, byte.class);
        }
        
        @Override
        protected Number parseString(String s, int radix) 
                throws NumberFormatException {
            return (radix == -1) ? Byte.decode(s) : Byte.parseByte(s, radix);
        }
    }
  
    private static class IntegerResourceConverter extends INumberResourceConverter {
        IntegerResourceConverter() {
            super(Integer.class, int.class);
        }

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

        @Override
        protected Number parseString(String s, int radix) 
                throws NumberFormatException {
            return (radix == -1) ? Short.decode(s) : Short.parseShort(s, radix);
        }
    }
    
    private static class MessageFormatResourceConverter extends ResourceConverter {
        MessageFormatResourceConverter() {
            super(MessageFormat.class);
        }

        @Override
        public Object parseString(String s, ResourceMap r) 
                throws ResourceConverterException {
            return new MessageFormat(s);
        }
    }
    
    private static class URLResourceConverter extends ResourceConverter {
        URLResourceConverter() {
            super(URL.class);
        }

        @Override
        public Object parseString(String s, ResourceMap r) 
                throws ResourceConverterException {
            try {
                return new URL(s);
            } catch (MalformedURLException ex) {
                throw new ResourceConverterException("invalid URL", s, ex);
            }
        }
    }
    
    private static class URIResourceConverter extends ResourceConverter {
        URIResourceConverter() {
            super(URI.class);
        }

        @Override
        public Object parseString(String s, ResourceMap r) 
                throws ResourceConverterException {
            try {
                return new URI(s);
            } catch (URISyntaxException ex) {
                throw new ResourceConverterException("invalid URI", s, ex);
            }
        }
    }
}
