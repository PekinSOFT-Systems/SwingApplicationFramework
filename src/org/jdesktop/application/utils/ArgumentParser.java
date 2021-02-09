/*
 * Copyright (C) 2020 PekinSOFT Systems
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
 * *****************************************************************************
 *  Project    :   Northwind-Basic
 *  Class      :   ArgumentParser.java
 *  Author     :   Sean Carrick
 *  Created    :   Mar 8, 2020 @ 1:43:17 PM
 *  Modified   :   Mar 8, 2020
 *  
 *  Purpose:
 *  
 *  Revision History:
 *  
 *  WHEN          BY                  REASON
 *  ------------  ------------------- ------------------------------------------
 *  Mar 8, 2020  Sean Carrick        Initial creation.
 * *****************************************************************************
 */

package org.jdesktop.application.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

/**
 * `ArgumentParser` is a library for parsing command line arguments in an 
 * easy, intelligible and efficient manner. This parser breaks down the 
 * difference between switches and targets. Switches are typically single letter
 * options that are prefixed with a single dash (-) or, word or multi-word 
 * options that are prefixed with a double dash (--). Targets are the parameters
 * to the switches.
 * 
 * For example, if your program requires a flag for debugging, you would 
 * provide a single `boolean` switch that takes no parameters, such as
 * `-d` or `--debug`. Your program would then simply check if that
 * switch was present, which would equate to `debugging = true`, or not,
 * which would equate to `debugging = false`.
 * 
 * Alternatively, if the 
 * debugging flag is present, you may want to set a debugging level for verbosity
 * of output. In that use case, your program could check for the existence of 
 * the debugging flag (above) and, if it exists, check for the debugging level,
 * which could be `--debugging-level 2` for a medium verbosity output. In
 * this instance, the switch is `--debugging-level` and the target is 2.
 * 
 * This works for any type of switch and target. You can even have targets
 * with no switches or multiple targets for each switch. If you need targets 
 * without a switch, those targets should be listed first on the command line,
 * as this library interprets all targets following a switch as belonging to 
 * that switch, up to the next switch on the command line.
 * 
 * Also, you can use this library to populate fields in your custom classes,
 * provided you name your fields the same as the command line switch that gathers
 * the data for those fields.
 * 
 * This project is located on GitHub at 
 * https://github.com/SeanCarrick/CLArgParser 
 * 
 * @author Sean Carrick &lt;sean at pekinsoft dot com&gt;
 * 
 * @version 0.1.0
 * @since 0.1.0
 */
public class ArgumentParser {
    //<editor-fold defaultstate="collapsed" desc="Private Member Fields">
    private String[] args;                          // Default to null
    private HashMap<String, Integer> switchIndices; // Default to null
    private TreeSet<Integer> takenIndices;          // Default to null
    private List<String> targets;                   // Default to null
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Intstance Initializer">
    {
        args = null;
        switchIndices = new HashMap<>();
        takenIndices = new TreeSet<>();
        targets = new ArrayList<>();
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Constructor(s)">
    /**
     * Creates an instance of the `ArgumentParser` class to parse command
     * line arguments into something useful.
     * 
     * @param args The command line argument passed into the implementing program.
     */
    public ArgumentParser (String[] args) {
        parse(args);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Public Methods">
    /**
     * Parse the arguments into a more useful format.
     * 
     * @param arguments The command line arguments given to the calling program.
     */
    public void parse(String[] arguments) {
        this.args = arguments;
        
        switchIndices.clear();
        takenIndices.clear();
        
        int idx = 0;
        
        for ( String str : args ) {
            if ( str.startsWith("-") ) {
                switchIndices.put(str, idx);
                takenIndices.add(idx);    // Use then increment idx.
            }
            idx++;
        }
    }
    
    /**
     * Retrieves the array of arguments that are stored in this object.
     * 
     * @return The command line arguments.
     */
    public String[] getArguments() {
        return args;
    }
    
    /**
     * Retrieves the argument at the given index in the argument array.
     * 
     * @param idx   The index from which to get the argument.
     * @return      The argument stored at the provided index.
     */
    public String getArgument(int idx) {
        return args[idx];
    }
    
    /**
     * Checks to see if the given switch is present in the command line arguments.
     * 
     * @param switchName    The command line switch to check for.
     * @return              {@code true} if the switch is present; {@code falcse}
     *                      otherwise.
     */
    public boolean isSwitchPresent(String switchName) {
        return switchIndices.containsKey(switchName);
    }
    
    /**
     * Retrieve the value from the given switch.
     * 
     * @param switchName    The switch for which to retrieve the value.
     * @return              The value of the given switch.
     */
    public String getSwitchValue(String switchName) {
        return getSwitchValue(switchName, null);
    }
    
    /**
     * Retrieves the value for the given switch, or the default value if the
     * given switch is not found.
     * 
     * @param switchName    The switch for which value is wanted.
     * @param defaultValue  A default value in the event the switch is not
     *                      present.
     * @return              The value, if the switch is present; the supplied
     *                      default value if the switch was not found.
     */
    public String getSwitchValue(String switchName, String defaultValue) {
        if ( !switchIndices.containsKey(switchName) ) {
            return defaultValue;
        }
        
        int idx = switchIndices.get(switchName);
        
        String val = null;
        
        if ( idx + 1 < args.length ) {
            takenIndices.add(idx + 1);
            val = args[idx + 1];
        }
        
        return val == null ? defaultValue : val;
    }
    
    /**
     * Retrieves the value for the given switch as a {@code java.lang.Long}.
     * 
     * @param switchName    The switch for which value is wanted.
     * @return              {@code java.lang.Long} value of the switch value.
     * @throws NumberFormatException if the string does not 
     *                      contain a parsable long.
     */
    public Long getSwitchLongValue(String switchName) {
        return getSwitchLongValue(switchName, null);
    }
    
    /**
     * Retrieves the value for the given switch as a {@code java.lang.Long}, or
     * the provided default value if the given switch is not found.
     * 
     * @param switchName    The switch for which value is wanted.
     * @param defaultValue  A default value in the event the switch is not
     *                      present.
     * @return              {@code java.lang.Long} value, if the switch is 
     *                      present; the supplied default value if the switch is
     *                      not found.
     * @throws NumberFormatException if the string does not 
     *                      contain a parsable long.
     */
    public Long getSwitchLongValue(String switchName, Long defaultValue) {
        String switchValue = getSwitchValue(switchName, null);
        
        if ( switchValue == null ) {
            return defaultValue;
        }
        
        return Long.parseLong(switchValue);
    }
    
    /**
     * Retrieves the value for the given switch as a {@code java.lang.Double}.
     * 
     * @param switchName    The switch for which value is wanted.
     * @return              {@code java.lang.Double} value of the switch value.
     * @throws NumberFormatException if the string does not 
     *                      contain a parsable double.
     */
    public Double getSwitchDoubleValue(String switchName) {
        return getSwitchDoubleValue(switchName, null);
    }
    
    /**
     * Retrieves the value for the given switch as a {@code java.lang.Double}, 
     * or the provided default value if the given switch is not found.
     * 
     * @param switchName    The switch for which value is wanted.
     * @param defaultValue  A default value in the event the switch is not
     *                      present.
     * @return              {@code java.lang.Double} value, if the switch is 
     *                      present; the supplied default value if the switch is
     *                      not found.
     * @throws NumberFormatException if the string does not 
     *                      contain a parsable double.
     */
    public Double getSwitchDoubleValue(String switchName, Double defaultValue) {
        String switchValue = getSwitchValue(switchName, null);
        
        if ( switchValue == null ) {
            return defaultValue;
        }
        
        return Double.parseDouble(switchValue);
    }
    
    /**
     * Retrieves an array of {@code java.lang.String} values for the given 
     * switch.
     * 
     * @param switchName    The switch for which values are wanted.
     * @return              An array of {@code java.lang.String}s containing all
     *                      of the values associated with the given switch. If
     *                      the switch is not found, an array of length zero (0)
     *                      will be returned.
     */
    public String[] getSwitchValues(String switchName) {
        if ( !switchIndices.containsKey(switchName) ) {
            return new String[0];
        }
        
        int switchIndex = switchIndices.get(switchName);
        int nextArgIndex = switchIndex + 1;
        while ( nextArgIndex < args.length 
                && !args[nextArgIndex].startsWith("-") ) {
            takenIndices.add(nextArgIndex);
            nextArgIndex++;
        }
        
        String[] values = new String[nextArgIndex - switchIndex - 1];
        for ( int idx = 0; idx < values.length; idx++ ) {
            values[idx] = args[switchIndex + idx + 1];
        }
        
        return values;
    }
    
    /**
     * Retrieves the switch and its values as a Plain Old Java Object (POJO).
     * 
     * @param clazz The class to which the switch and values should be placed
     *              into.
     * @return      {@code java.lang.Object} of the switch and its values.
     * @throws RuntimeException in the event the POJO cannot be
     *              created.
     */
    public <T> T getSwitchPOJO(Class<T> clazz) {
        try {
//            Class<?> newClass = Class.forName(clazz.getName());
            Constructor<T> constructor = clazz.getConstructor();
            T pojo = constructor.newInstance();
            Field[] fields = clazz.getDeclaredFields();
            
            for ( Field field : fields ) {
                Class fieldType = field.getType();
                String fieldName = "-" + field.getName().replace("_", "-");
                
                // Check to see if the field name is valid.
                String val = getSwitchValue(fieldName);
                if ( val == null ) {
                    fieldName = "-" + fieldName;
                }
                
                if ( fieldType.equals(Boolean.class)
                        || fieldType.equals(boolean.class) ) {
                    field.set(pojo, isSwitchPresent(fieldName));
                } else if ( fieldType.equals(String.class) ) {
                    if ( getSwitchValue(fieldName) != null ) {
                        field.set(pojo, getSwitchValue(fieldName));
                    }
                } else if ( fieldType.equals(Long.class) 
                        || fieldType.equals(long.class) ) {
                    if ( getSwitchLongValue(fieldName) != null ) {
                        field.set(pojo, getSwitchLongValue(fieldName));
                    }
                } else if ( fieldType.equals(Integer.class) 
                        || fieldType.equals(int.class) ) {
                    if ( getSwitchLongValue(fieldName) != null ) {
                        field.set(pojo, getSwitchLongValue(fieldName)
                                .intValue());
                    }
                } else if ( fieldType.equals(Short.class) 
                        || fieldType.equals(short.class) ) {
                    if ( getSwitchLongValue(fieldName) != null ) {
                        field.set(pojo, getSwitchLongValue(fieldName)
                                .shortValue());
                    }
                } else if ( fieldType.equals(Byte.class) 
                        || fieldType.equals(byte.class) ) {
                    if ( getSwitchLongValue(fieldName) != null ) {
                        field.set(pojo, getSwitchLongValue(fieldName)
                                .byteValue());
                    }
                } else if ( fieldType.equals(Double.class) 
                        || fieldType.equals(double.class) ) {
                    if ( getSwitchDoubleValue(fieldName) != null ) {
                        field.set(pojo, getSwitchDoubleValue(fieldName));
                    }
                } else if ( fieldType.equals(Float.class) 
                        || fieldType.equals(float.class) ) {
                    if ( getSwitchDoubleValue(fieldName) != null ) {
                        field.set(pojo, getSwitchDoubleValue(fieldName)
                                .floatValue());
                    }
                } else if ( fieldType.equals(String[].class) ) {
                    String[] values = getSwitchValues(fieldName);
                    if ( values.length != 0 ) {
                        field.set(pojo, values);
                    }
                }
            }
            
            return pojo;
        } catch ( IllegalAccessException | NoSuchMethodException 
                | InstantiationException | InvocationTargetException ex ) {
            throw new RuntimeException("Error creating switch POJO", ex);
        }
    }
    
    /**
     * Retrieves all targets as an array of {@code java.lang.String}s. The 
     * targets that are returned are values that had no switch associated with
     * them.
     * 
     * @return All of the targets that did not have a switch associated with
     *         them.
     */
    public String[] getTargets() {
        String[] aryTargets = new String[args.length - takenIndices.size()];
        int targetIndex = 0;
        
        for ( int idx = 0; idx < args.length; idx++ ) {
            if ( !takenIndices.contains(idx) ) {
                aryTargets[targetIndex++] = args[idx];
            }
        }
        
        return aryTargets;
    }
    //</editor-fold>
}
