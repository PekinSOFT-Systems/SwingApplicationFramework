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
 * Class Name: TaskEvent.java
 *     Author: Sean Carrick <sean at pekinsoft dot com>
 *    Created: Jan 16 2021
 * 
 *    Purpose:
 * 
 * *****************************************************************************
 * CHANGE LOG:
 * 
 * Date        By                   Reason
 * ----------  -------------------  --------------------------------------------
 * 01/16/2021  Sean Carrick          Initial Creation.
 * *****************************************************************************
 */
package org.jdesktop.application;

import java.util.EventObject;

/**
 * An encapsulation of the value produced by one of the <tt>Task</tt> execution 
 * methods: `doInBackground()}, `process`, `done`. The source
 * of a <tt>TaskEvent` is the `Task</tt> that produced the value.
 * 
 * @see TaskListener
 * @see Task
 *
 * @author Sean Carrick
 */
public class TaskEvent<T> extends EventObject {
    private final T value;
    
    /**
     * Returns the value this event represents.
     * 
     * @return the <tt>value</tt> constructor argument
     */
    public final T getValue() {return value; }
    
    /**
     * Construct a `Task`
     * 
     * @param source the <tt>Task</tt> that produced the value
     * @param value the value, <tt>null} if type `T</tt> is `Void`
     */
    public TaskEvent(Task source, T value) {
        super(source);
        this.value = value;
    }
}
