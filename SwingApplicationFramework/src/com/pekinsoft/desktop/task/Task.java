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
 * Class Name: Task.java
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
 * 01/16/2021  Sean Carrick          Initial Adaptation.
 * *****************************************************************************
 */
package com.pekinsoft.desktop.task;

import com.pekinsoft.desktop.Application;
import com.pekinsoft.desktop.utils.Logger;
import com.sun.source.util.TaskListener;
import java.util.List;
import javax.swing.SwingWorker;

/**
 * A type of {@link SwingWorker} that represents an application background task.
 * Tasks add descriptive properties that can be shown to the user, a new set of
 * methods for customizing task completion, support for blocking input to the
 * GUI while the Task is executing, and a {@code TaskListener} that enables one
 * to monitor the three key SwingWorker methods: {@code doInBackground}, {@code 
 * process}, and {@code done}.
 * <p>
 * When a Task completes, the {@code final done} method invokes one of {@code 
 * succeeded}, {@code cancelled}, {@code interrupted}, or {@code failed}. The
 * {@code final done} method invokes {@code finished} when the completion method
 * returns or throws an exception.</p>
 * <p>
 * Tasks should provide localized values for the {@code title}, {@code description},
 * and {@code message} properties in a ResourceBundle for the Task subclass. A
 * {@link ResourceMap} is loaded automatically using the Task subclass as the
 * {@code startClass} and Task.class the {@code stopClass}. This ResourceMap is
 * also used to look up format strings used in calls to {@link #message message},
 * which is used to set the {@code message} property.</p>
 * <p>
 * For example: give a Task called {@code MyTask} defined like this:
 * <pre>
 * class MyTask extends Task<MyResultType, Void> {
 *     protected MyResultType doInBackground() {
 *         message("startMessage", getPlannedSubtaskCount());
 *         // do the work...if an error is encountered:
 *         message("errorMessage");
 *         message("finishedMessage", getActualSubtaskCount(), getFailureCount());
 *         // ...return the resultset
 *     }
 * }
 * </pre>
 * Typically the resources for this class would be defined in the MyTask
 * ResourceBundle, {@code resources/MyTask.properties}:
 * <pre>
 * title = My Task
 * description = A task of mine for my own purposes
 * startMessage = Starting: working on %s subtasks...
 * errorMessage = An unexpected error occurred, skipping subtask
 * finishMessage = Finished: completed %1$s subtasks, %2$s failures
 * </pre>
 * Tasks can specify that input to the GUI is to be blocked while they're being
 * executed. The {@code inputBlocker} property specifies what part of the GUI is
 * to be blocked and how that's accomplished. The {@code inputBlocker} is set 
 * automatically when an {@code &#064;Action} method that returns a Task specifies a 
 * {@link BlockingScope} value for the {@code block} annotation parameter. To
 * customize the way blocking is implemented you can define your own {@code 
 * Task.InputBlocker}. For example, assume that {@code busyGlassPane} is a 
 * component that consumes (and ignores) keyboard and mouse input:
 * <pre>
 * class MyInputBlocker extends InputBlocker {
 *     BusyIndicatorInputBlocker(Task task) {
 *         super(task, Task.BlockingScope.WINDOW, myGlassPane);
 *     }
 *     
 *     protected void block() {
 *         myFrame.setGlasspane(myGlassPane);
 *         busyGlassPane.setVisible(true);
 *     }
 * 
 *     protected void unblock() {
 *         busyGlassPane.setVisible(false);
 *     }
 * }
 * //...
 * </pre>
 * <p>
 * All of the settable properties in this class are bound, i.e., a 
 * PropertyChangeEvent is fired when the value of the property changes. As with
 * the {@code SwingWorker} superclass, all {@code PropertyChangeListeners} run 
 * on the event dispatching thread. This is also true of {@code TaskListeners}.
 *
 * @author Hans Muller (Original Author)
 * @author Sean Carrick (Adapting Author) &lt;sean at pekinsoft dot com&gt;
 */
public abstract class Task<T, V> extends SwingWorker<T, V> {
    private static final Logger logger = Logger.getLogger(Task.class.getName());
    private  final Application application;
    private String resourcePrefix;
    private ResourceMap resourceMap;
    private List<TaskListener<T, V>> taskListeners;
    private InputBlocker inputBlocker;
    private String name = null;
    private String title = null;
    private String description = null;
    private long messageTime = -1L;
    private String message = null;
    private long startTime = -1L;
    private long doneTime = -1L;
    private boolean userCanCancel = true;
    private boolean progressPropertyIsValid = false;
    private TaskService taskService = null;
}
