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
 *  Class      :   TaskService.java
 *  Author     :   Sean Carrick
 *  Created    :   Feb 11, 2021 @ 6:57:44 PM
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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;

/**
 * @author Hans Muller (Original Author) &lt;current email unknown&gt;
 * @author Sean Carrick (Updater) &lt;sean at pekinsoft dot com&gt;
 *
 * @version 1.05
 * @since 1.03
 */
public class TaskService extends AbstractBean {

    private final String name;
    private final ExecutorService executorService;
    private final List<Task> tasks;
    private final PropertyChangeListener taskPCL;

    public TaskService(String name, ExecutorService executorService) {
        if (name == null) {
            throw new IllegalArgumentException("null name");
        }
        if (executorService == null) {
            throw new IllegalArgumentException("null executorService");
        }
        this.name = name;
        this.executorService = executorService;
        this.tasks = new ArrayList<>();
        this.taskPCL = new TaskPCL();
    }

    public TaskService(String name) {
        this(name, new ThreadPoolExecutor(
                3, // corePool size
                10, // maximumPool size
                1L, TimeUnit.SECONDS, // non-core threads time to live
                new LinkedBlockingQueue<Runnable>()));
    }

    public final String getName() {
        return name;
    }

    private List<Task> copyTasksList() {
        synchronized (tasks) {
            if (tasks.isEmpty()) {
                return Collections.emptyList();
            } else {
                return new ArrayList<>(tasks);
            }
        }
    }

    private class TaskPCL implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent e) {
            String propertyName = e.getPropertyName();
            if ("done".equals(propertyName)) {
                Task task = (Task) (e.getSource());
                if (task.isDone()) {
                    List<Task> oldTaskList, newTaskList;
                    synchronized (tasks) {
                        oldTaskList = copyTasksList();
                        tasks.remove(task);
                        task.removePropertyChangeListener(taskPCL);
                        newTaskList = copyTasksList();
                    }
                    firePropertyChange("tasks", oldTaskList, newTaskList);
                    Task.InputBlocker inputBlocker = task.getInputBlocker();
                    if (inputBlocker != null) {
                        inputBlocker.unblock();
                    }
                }
            }
        }
    }

    private void maybeBlockTask(Task task) {
        final Task.InputBlocker inputBlocker = task.getInputBlocker();
        if (inputBlocker == null) {
            return;
        }
        if (inputBlocker.getScope() != Task.BlockingScope.NONE) {
            if (SwingUtilities.isEventDispatchThread()) {
                inputBlocker.block();
            } else {
                Runnable doBlockTask = () -> {
                    inputBlocker.block();
                };
                SwingUtilities.invokeLater(doBlockTask);
            }
        }
    }

    public void execute(Task task) {
        if (task == null) {
            throw new IllegalArgumentException("null task");
        }
        if (!task.isPending() || (task.getTaskService() != null)) {
            throw new IllegalArgumentException("task has already been executed");
        }
        task.setTaskService(this);
        // TBD: what if task has already been submitted?
        List<Task> oldTaskList, newTaskList;
        synchronized (tasks) {
            oldTaskList = copyTasksList();
            tasks.add(task);
            newTaskList = copyTasksList();
            task.addPropertyChangeListener(taskPCL);
        }
        firePropertyChange("tasks", oldTaskList, newTaskList);
        maybeBlockTask(task);
        executorService.execute(task);
    }

    public List<Task> getTasks() {
        return copyTasksList();
    }

    public final void shutdown() {
        executorService.shutdown();
    }

    public final List<Runnable> shutdownNow() {
        return executorService.shutdownNow();
    }

    public final boolean isShutdown() {
        return executorService.isShutdown();
    }

    public final boolean isTerminated() {
        return executorService.isTerminated();
    }

    public final boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executorService.awaitTermination(timeout, unit);
    }
}
