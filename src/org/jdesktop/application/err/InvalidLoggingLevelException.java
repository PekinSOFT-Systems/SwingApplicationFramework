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
 *  Class      :   InvalidLoggingLevelException.java
 *  Author     :   Sean Carrick
 *  Created    :   Mar 8, 2020 @ 12:42:58 PM
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

package org.jdesktop.application.err;

/**
 *
 * @author Sean Carrick &lt;sean at pekinsoft dot com&gt;
 * 
 * @version 0.1.0
 * @since 0.1.0
 */
public class InvalidLoggingLevelException extends Exception {

    /**
     * Creates a new instance of <code>InvalidLoggingLevelException</code> without detail message.
     */
    public InvalidLoggingLevelException() {
        super();
    }


    /**
     * Constructs an instance of <code>InvalidLoggingLevelException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public InvalidLoggingLevelException(String msg) {
        super(msg);
    }
}
