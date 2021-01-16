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
 * Class Name: TaskListener.java
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
package com.pekinsoft.desktop.support;

import com.pekinsoft.desktop.event.TaskEvent;
import com.pekinsoft.desktop.task.Task;
import java.util.List;

/**
 * Listener used for observing {@code Task} execution. A {@code TaskListener} is
 * particularly useful monitoring the intermediate results {@link Task#publish
 * published} by a Task in situations where it's not practical to override the
 * Task's {@link Task#process process} method. Note that if what you really want
 * to do is monitor a Task's state and progress, a PropertyChangeListener is
 * probably more appropriate.
 * <p>
 * The Task class runs all TaskListener methods on the event dispatching thread
 * and the source of all TaskEvents is the Task object.</p>
 * 
 * @see Task#addTaskListener
 * @see Task#removeTaskListener
 * @see Task#addPropertyChangeListener
 *
 * @author Hans Muller (Original Author)
 * @author Sean Carrick (Adapting Author) &lt;sean at pekinsoft dot com&gt;
 */
public interface TaskListener<T, V> {
    
    /**
     * Called just before the Task's {@link Task#doInBackground doInBackground}
     * method is called, i.e., just before the task begins running. The {@code 
     * event}'s source is the Task and its value is {@code null}.
     * 
     * @param event a TaskEvent whose source is the {@code Task} object, value 
     *          is {@code null}
     * @see Task#doInBackground
     * @see TaskEvent#getSource
     */
    void doInBackground(TaskEvent<Void> event);
    
    /**
     * Called each time the Task's {@link Task#process process} method is called.
     * The value of the event is the list of values passed to the process method.
     * 
     * @param event a TaskEvent whose source is the {@code Task} object and
     *          whose value is a list of the values passed to the {@code
     *          Task.process()} method
     * @see Task#doInBackground
     * @see Task#process
     * @see TaskEvent#getSource
     * @see TaskEvent#getValue
     */
    void process(TaskEvent<List<V>> event);
    
    /**
     * Called after the Task's {@code Task#succeeded} method is called. The
     * event's value is the value returned by the Task's {@code get} method, i.e.,
     * the value that is computed by {@link Task#doInBackground}.
     * 
     * @param event a TaskEvent whose source is the {@code Task} object, and
     *          whose value is the value returned by {@code Task.get()}
     * @see Task#succeeded
     * @see TaskEvent#getSource
     * @see TaskEvent#getValue
     */
    void succeeded(TaskEvent<T> event);
    
    /**
     * Called after the Task's {@link Task#failed failed} method is called. The
     * event's value is the Throwable passed to {@code Task.failed()}.
     * 
     * @param event a TaskEvent whose source is the {@code Task} object, and
     *          whose value is the Throwable passed to {@code Task.failed()}
     * @see Task#failed
     * @see TaskEvent#getSource
     * @see TaskEvent#getValue
     */
    void failed(TaskEvent<Throwable> event);
    
    /**
     * Called after the Task's {@link Task#cancelled cancelled} method is called.
     * The {@code event}'s source is the Task and its value is {@code null}.
     * 
     * @param event a TaskEvent whose source is the {@code Task} obeject, value
     *          is {@code null}
     * @see Task#cancelled
     * @see Task#get
     * @see Task#getSource
     */
    void cancelled(TaskEvent<Void> event);
    
    /**
     * Called after the Task's {@link Task#interrupted interrupted} method is
     * called. The {@code event}'s source is the Task and its value is the
     * InterruptedException passed to {@code Task.interrupted()}.
     * 
     * @param event a TaskEvent whose source is the {@code Task} object, and
     *          whose value is the InterruptedException passed to {@code 
     *          Task.interrupted()}
     * @see Task#interrupted
     * @see TaskEvent#getSource
     * @see TaskEvent#getValue
     */
    void interrupted(TaskEvent<InterruptedException> event);
    
    /**
     * Called after the Task's {@link Task#finished finished} method is called.
     * The {@code event}'s source is the Task and its value is null.
     * 
     * @param event a TaskEvent whose source is the {@code Task} object, value
     *          is {@code null}
     * @see Task#interrupted
     * @see TaskEvent#getSource
     */
    void finished(TaskEvent<Void> event);
    
    /**
     * Convenience class that stubs all of the TaskListener interface methods.
     * Using TaskListener.Adapter can simplify building TaskListeners.
     * @param <T>
     * @param <V> 
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
