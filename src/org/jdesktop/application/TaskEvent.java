/*
 * Copyright (C) 2006 Sun Microsystems
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
 *  Class      :   TaskEvent.java
 *  Author     :   Sean Carrick
 *  Created    :   Feb 11, 2021 @ 6:02:38 PM
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

import java.util.EventObject;

/**
 * An encapsulation of the value produced one of the <code>Task</code> execution
 * methods: <code>doInBackground()</code>, <code>process</code>, <code>done
 * </code>. The source of a <code>TaskEvent</code> is the <code>Task</code> that 
 * produced the value.
 * 
 * @author Hans Muller (Original Author) &lt;current email unknown&gt;
 * @author Sean Carrick (Updater) &lt;sean at pekinsoft dot com&gt;
 * 
 * @version 1.05
 * @since 1.03
 *
 * @see TaskListener
 * @see Task
 */
public class TaskEvent<T> extends EventObject {

    private final T value;

    /**
     * Returns the value this event represents.
     *
     * @return the <code>value</code> constructor argument.
     */
    public final T getValue() {
        return value;
    }

    /**
     * Construct a <code>TaskEvent</code>.
     *
     * @param source the <code>Task</code> that produced the value.
     * @param value the value, null if type <code>T</code> is <code>Void</code>.
     */
    public TaskEvent(Task source, T value) {
        super(source);
        this.value = value;
    }
}
