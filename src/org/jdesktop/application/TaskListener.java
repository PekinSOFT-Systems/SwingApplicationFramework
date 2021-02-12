/*
 * Copyright (C) 2006 Sun Microsystems, Inc.
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
 *  Class      :   TaskListener.java
 *  Author     :   Sean Carrick
 *  Created    :   Feb 11, 2021 @ 6:06:13 PM
 *  Modified   :   Feb 11, 2021
 *  
 *  Purpose:     See class JavaDoc comment.
 *  
 *  Revision History:
 *  
 *  WHEN          BY                   REASON
 *  ------------  -------------------  -----------------------------------------
 *  Feb 11, 2021  Sean Carrick         Initial creation.
 * *****************************************************************************
 */
package org.jdesktop.application;

import java.util.List;

/**
 * Listener used for observing <code>Task</code> execution. A <code>
 * TaskListener</code> is particularly useful for monitoring the the intermediate
 * results {@link Task#publish published} by a Task in situations where it's not
 * practical to override the Task's {@link Task#process process} method. Note
 * that if what you really want to do is monitor a Task's state and progress, a
 * PropertyChangeListener is probably more appropriate.
 * <p>
 * The Task class runs all TaskListener methods on the event dispatching thread
 * and the source of all TaskEvents is the Task object.</p>
 *
 * @see Task#addTaskListener(org.jdesktop.application.TaskListener) 
 * @see Task#removeTaskListener(org.jdesktop.application.TaskListener) 
 * @see Task#addPropertyChangeListener(java.beans.PropertyChangeListener) 
 *
 * @author Hans Muller (Original Author) &lt;current email unkown&gt;
 * @author Sean Carrick (Updater) &lt;sean at pekinsoft dot com&gt;
 * 
 * @version 1.05
 * @since 1.03
 */
public interface TaskListener<T, V> {

    /**
     * Called just before the Task's {@link Task#doInBackground
     * doInBackground} method is called, i.e. just before the task begins
     * running. The <code>event's</code> source is the Task and its value is null.
     *
     * @param event a TaskEvent whose source is the <code>Task</code> object, value
     * is null
     * 
     * @see Task#doInBackground() 
     * @see TaskEvent#getSource() 
     */
    void doInBackground(TaskEvent<Void> event);

    /**
     * Called each time the Task's {@link Task#process process} method is
     * called. The value of the event is the list of values passed to the
     * process method.
     *
     * @param event a TaskEvent whose source is the <code>Task</code> object and
     * whose value is a list of the values passed to the <code>Task.process()</code>
     * method
     * 
     * @see Task#doInBackground() 
     * @see Task#process(java.util.List) 
     * @see TaskEvent#getSource() 
     * @see TaskEvent#getValue() 
     */
    void process(TaskEvent<List<V>> event);

    /**
     * Called after the Task's {@link Task#succeeded succeeded} completion
     * method is called. The event's value is the value returned by the Task's
     * <code>get</code> method, i.e. the value that is computed by
     * {@link Task#doInBackground}.
     *
     * @param event a TaskEvent whose source is the <code>Task</code> object, and
     * whose value is the value returned by <code>Task.get()</code>.
     * 
     * @see Task#succeeded(java.lang.Object) 
     * @see TaskEvent#getSource() 
     * @see TaskEvent#getValue() 
     */
    void succeeded(TaskEvent<T> event);

    /**
     * Called after the Task's {@link Task#failed failed} completion method is
     * called. The event's value is the Throwable passed to
     * <code>Task.failed()</code>.
     *
     * @param event a TaskEvent whose source is the <code>Task</code> object, and
     * whose value is the Throwable passed to <code>Task.failed()</code>.
     * 
     * @see Task#failed(java.lang.Throwable) 
     * @see TaskEvent#getSource() 
     * @see TaskEvent#getValue() 
     */
    void failed(TaskEvent<Throwable> event);

    /**
     * Called after the Task's {@link Task#cancelled cancelled} method is
     * called. The <code>event's</code> source is the Task and its value is null.
     *
     * @param event a TaskEvent whose source is the <code>Task</code> object, value
     * is null
     * 
     * @see Task#cancelled() 
     * @see Task#get() 
     * @see TaskEvent#getSource() 
     */
    void cancelled(TaskEvent<Void> event);

    /**
     * Called after the Task's {@link Task#interrupted interrupted} method is
     * called. The <code>event's</code> source is the Task and its value is the
     * InterruptedException passed to <code>Task.interrupted()</code>.
     *
     * @param event a TaskEvent whose source is the <code>Task</code> object, and
     * whose value is the InterruptedException passed to
     * <code>Task.interrupted()</code>.
     * 
     * @see Task#interrupted(java.lang.InterruptedException) 
     * @see TaskEvent#getSource() 
     * @see TaskEvent#getValue() 
     */
    void interrupted(TaskEvent<InterruptedException> event);

    /**
     * Called after the Task's {@link Task#finished finished} method is called.
     * The <code>event's</code> source is the Task and its value is null.
     *
     * @param event a TaskEvent whose source is the <code>Task</code> object, value
     * is null.
     * 
     * @see Task#interrupted(java.lang.InterruptedException) 
     * @see TaskEvent#getSource() 
     */
    void finished(TaskEvent<Void> event);

    /**
     * Convenience class that stubs all of the TaskListener interface methods.
     * Using TaskListener.Adapter can simplify building TaskListeners:
     * <pre>
     * </pre>
     */
    class Adapter<T, V> implements TaskListener<T, V> {

        @Override
        public void doInBackground(TaskEvent<Void> event) {
        }

        @Override
        public void process(TaskEvent<List<V>> event) {
        }

        @Override
        public void succeeded(TaskEvent<T> event) {
        }

        @Override
        public void failed(TaskEvent<Throwable> event) {
        }

        @Override
        public void cancelled(TaskEvent<Void> event) {
        }

        @Override
        public void interrupted(TaskEvent<InterruptedException> event) {
        }

        @Override
        public void finished(TaskEvent<Void> event) {
        }
    }
}
