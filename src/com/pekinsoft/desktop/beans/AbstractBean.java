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
 * Class Name: AbstractBean.java
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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 *
 * @author Sean Carrick
 */
public class AbstractBean {
    private final PropertyChangeSupport pcs;
    
    public AbstractBean() {
        pcs = new EDTPropertyChangeSupport(this);
    }
    
    /**
     * Add a PropertyChangeListener to the listener list. The listener is 
     * registered for all properties and its {@code propertyChange} method will
     * run on the event dispatching thread.
     * <p>
     * If {@code listener} is {@code null}, no exception is thrown and no action
     * is taken.</p>
     * 
     * @param listener the PropertyChangeListener to be added
     * @see #removePropertyChangeListener
     * @see java.beans.PropertyChangeSupport#addPropertyChangeListener
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }
    
    /**
     * Add a PropertyChangeListener for a specific property. The listener will
     * be invoked only when a call on firePropertyChange names that specific
     * property. The same listener object may be added more than once. For each
     * property, the listener will be invoked the number of times it was added
     * for that property. If the {@code propertyName} or {@code listener} is
     * {@code null}, no exception is thrown and no action is taken.
     * 
     * @param propertyName the name of the property to listen on
     * @param listener the PropertyChangeListener to be added
     * @see java.beans.PropertyChangeListener#addPropertyChangeListener(String,
     *          PropertyChangeListener)
     */
    public void addPropertyChangeListener(String propertyName, 
            PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(propertyName, listener);
    }
    
    /**
     * Remove a PropertyChangeListener from the listener list.
     * <p>
     * If {@code listener} is {@code null}, no exception is thrown and no action
     * is taken.</p>
     * 
     * @param listener the PropertyChangeListener to be removed
     * @see #addPropertyChangeListener
     * @see java.beans.PropertyChangeListener#removePropertyChangeListener
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }
}
