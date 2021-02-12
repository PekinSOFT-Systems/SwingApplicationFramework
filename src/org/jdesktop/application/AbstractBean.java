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
 *  Class      :   AbstractBea.java
 *  Author     :   Sean Carrick
 *  Created    :   Feb 10, 2021 @ 7:11:33 PM
 *  Modified   :   Feb 10, 2021
 *  
 *  Purpose:     See class JavaDoc comment.
 *  
 *  Revision History:
 *  
 *  WHEN          BY                   REASON
 *  ------------  -------------------  -----------------------------------------
 *  ??? ??, 2006  Hans Muller          Initial creation.
 *  Feb 10, 2021  Sean Carrick         Update to Java 11.
 * *****************************************************************************
 */

package org.jdesktop.application;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import javax.swing.SwingUtilities;

/**
 * An encapsulation of the PropertyChangeSupport methods based on
 * java.beans.PropertyChangeSupport. PropertyChangeListeners are fired on the
 * event dispatching thread.
 *
 * <p>
 * Note: this class is only public because the so-called "fix" for javadoc bug
 * <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4780441">4780441</a>
 * still fails to correctly document public methods inherited from a package
 * private class.</p>
 * 
 * <dl><dt>Regarding the note above from the original author:</dt>
 * <dd>According to the bug database, Bug 
 * <a href="https://bugs.java.com/bugdatabase/view_bug.do?bug_id=4780441">#4780441</a>
 * was marked as:
 * <table border="0">
 * <caption>JDK-4780441 : stddoclet: Incorrectly documents inherited members from
 * package-private class</caption>
 * <tr><td><strong>Type</strong>:</td><td>Bug</td></tr>
 * <tr><td><strong>Component</strong>:</td><td>tools</td></tr>
 * <tr><td><strong>Sub-Component</strong>:</td><td>javadoc(tool)</td></tr>
 * <tr><td><strong>Affected Version</strong>:</td><td>1.4.1,1.5.0</td></tr>
 * <tr><td><strong>Priority</strong>:</td><td>P3</td></tr>
 * <tr><td><strong>Status<strong>:</td><td><em>Resolved</em></td></tr>
 * <tr><td><strong>Resolution</strong>:</td><td><em>Fixed</em></td></tr>
 * <tr><td><strong>OS</strong>:</td><td>generic, other, windows_nt</td></tr>
 * <tr><td><strong>CPU</strong>:</td><td>generic, x86</td></tr>
 * <tr><td><strong>Submitted</strong>:</td><td>2002-11-18</td></tr>
 * <tr><td><strong>Updated</strong>:</td><td>2014-05-05</td></tr>
 * <tr><td><strong>Resolved</strong>:</td><td><em>2003-11-23</td></tr>
 * </table>
 * </dd></dl>
 * 
 * @author Hans Muller (Original Author) &lt;current email unknown&gt;
 * @author Sean Carrick (Updater) &lt;sean at pekinsoft.com&gt;
 * 
 * @since 1.03
 * @version 1.05
 */
class AbstractBean {

    private final PropertyChangeSupport pcs;

    public AbstractBean() {
        pcs = new EDTPropertyChangeSupport(this);
    }

    /**
     * Add a PropertyChangeListener to the listener list. The listener is
     * registered for all properties and its <code>propertyChange</code> method
     * will run on the event dispatching thread.
     * <p>
     * If <code>listener</code> is null, no exception is thrown and no action is
     * taken.
     *
     * @param listener the PropertyChangeListener to be added.
     * @see #removePropertyChangeListener
     * @see java.beans.PropertyChangeSupport#addPropertyChangeListener
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    /**
     * Remove a PropertyChangeListener from the listener list.
     * <p>
     * If <code>listener</code> is null, no exception is thrown and no action is
     * taken.</p>
     *
     * @param listener the PropertyChangeListener to be removed.
     * @see #addPropertyChangeListener
     * @see java.beans.PropertyChangeSupport#removePropertyChangeListener
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    /**
     * Add a PropertyChangeListener for a specific property. The listener will
     * be invoked only when a call on firePropertyChange names that specific
     * property. The same listener object may be added more than once. For each
     * property, the listener will be invoked the number of times it was added
     * for that property. If <code>propertyName</code> or <code>listener</code>
     * is null, no exception is thrown and no action is taken.
     *
     * @param propertyName The name of the property to listen on.
     * @param listener the PropertyChangeListener to be added
     * @see java.beans.PropertyChangeSupport#addPropertyChangeListener(String,
     * PropertyChangeListener)
     */
    public void addPropertyChangeListener(String propertyName,
            PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(propertyName, listener);
    }

    /**
     * Remove a PropertyChangeListener for a specific property. If
     * <code>listener</code> was added more than once to the same event source
     * for the specified property, it will be notified one less time after being
     * removed. If <code>propertyName</code> is null, no exception is thrown and
     * no action is taken. If <code>listener</code> is null, or was never added
     * for the specified property, no exception is thrown and no action is
     * taken.
     *
     * @param propertyName The name of the property that was listened on.
     * @param listener The PropertyChangeListener to be removed
     * @see
     * java.beans.PropertyChangeSupport#removePropertyChangeListener(String,
     * PropertyChangeListener)
     */
    public synchronized void removePropertyChangeListener(String propertyName,
            PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(propertyName, listener);
    }

    /**
     * An array of all of the <code>PropertyChangeListeners</code> added so far.
     *
     * @return all of the <code>PropertyChangeListeners</code> added so far.
     * @see java.beans.PropertyChangeSupport#getPropertyChangeListeners
     */
    public PropertyChangeListener[] getPropertyChangeListeners() {
        return pcs.getPropertyChangeListeners();
    }

    /**
     * Called whenever the value of a bound property is set.
     * <p>
     * If oldValue is not equal to newValue, invoke the
     * <code>propertyChange</code> method on all of the
     * <code>PropertyChangeListeners</code> added so far, on the event
     * dispatching thread.</p>
     *
     * @param propertyName name of the property being changed
     * @param oldValue the old value (from what it is being changed)
     * @param newValue the new value (to what it is being changed)
     * @see #addPropertyChangeListener
     * @see #removePropertyChangeListener
     * @see java.beans.PropertyChangeSupport#firePropertyChange(String, Object,
     * Object)
     */
    protected void firePropertyChange(String propertyName, Object oldValue, 
            Object newValue) {
        if (oldValue != null && newValue != null && oldValue.equals(newValue)) {
            return;
        }
        pcs.firePropertyChange(propertyName, oldValue, newValue);
    }

    /**
     * Fire an existing PropertyChangeEvent
     * <p>
     * If the event's oldValue property is not equal to newValue, invoke the
     * <code>propertyChange</code> method on all of the
     * <code>PropertyChangeListeners</code> added so far, on the event
     * dispatching thread.</p>
     *
     * @param e the <code>PropertyChangeEvent</code> that caused this to fire
     *
     * @see #addPropertyChangeListener
     * @see #removePropertyChangeListener
     * @see
     * java.beans.PropertyChangeSupport#firePropertyChange(PropertyChangeEvent
     * e)
     */
    protected void firePropertyChange(PropertyChangeEvent e) {
        pcs.firePropertyChange(e);
    }

    private static class EDTPropertyChangeSupport extends PropertyChangeSupport {

        EDTPropertyChangeSupport(Object source) {
            super(source);
        }

        /**
         * {@inheritDoc }
         *
         * @param e {@inheritDoc }
         */
        @Override
        public void firePropertyChange(final PropertyChangeEvent e) {
            if (SwingUtilities.isEventDispatchThread()) {
                super.firePropertyChange(e);
            } else {
                Runnable doFirePropertyChange = () -> {
                    firePropertyChange(e);
                };
                SwingUtilities.invokeLater(doFirePropertyChange);
            }
        }
    }
}
