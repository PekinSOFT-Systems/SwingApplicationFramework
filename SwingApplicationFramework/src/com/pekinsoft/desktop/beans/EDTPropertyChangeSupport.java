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
 * Class Name: EDTPropertyChangeSupport.java
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
package com.pekinsoft.desktop.beans;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeSupport;
import javax.swing.SwingUtilities;

/**
 *
 * @author Sean Carrick
 */
public class EDTPropertyChangeSupport extends PropertyChangeSupport {
    EDTPropertyChangeSupport(Object source) {
        super(source);
    }
    
    @Override
    public void firePropertyChange(final PropertyChangeEvent evt) {
        if (SwingUtilities.isEventDispatchThread()) {
            super.firePropertyChange(evt);
        } else {
            Runnable doFirePropertyChange = () -> {
                firePropertyChange(evt);
            };
            
            SwingUtilities.invokeLater(doFirePropertyChange);
        }
    }
}
