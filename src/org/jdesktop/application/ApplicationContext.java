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
 *  Class      :   ApplicationContext.java
 *  Author     :   Sean Carrick
 *  Created    :   Feb 11, 2021 @ 7:40:07 AM
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

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import javax.swing.JComponent;

/**
 * A singleton that manages shared objects, like actions, resources, and tasks,
 * for <code>Applications</code>.
 * <p>
 * {@link Application Applications} use the <code>ApplicationContext</code> singleton
 * to find global values and services. The majority of the Swing Application
 * Framework API can be accessed through <code>ApplicationContext</code>. The 
 * static <code>getInstance</code> method returns the singleton Typically it's 
 * only called after the application has been {@link Application#launch launched},
 * however it is always safe to call <code>getInstance</code>.
 *
 * @see Application
 * 
 * @author Hans Muller (Original Author) &lt;current email unknown&gt;
 * @author Sean Carrick (Updater) &lt;sean at pekinsoft dot com&gt;
 * 
 * @version 1.05
 * @since 1.03
 */
public class ApplicationContext extends AbstractBean {

    private static final Logger logger = Logger.getLogger(
            ApplicationContext.class.getName());
    private final List<TaskService> taskServices;
    private final List<TaskService> taskServicesReadOnly;
    private ResourceManager resourceManager;
    private ActionManager actionManager;
    private LocalStorage localStorage;
    private SessionStorage sessionStorage;
    private Application application = null;
    private Class applicationClass = null;
    private JComponent focusOwner = null;
    private Clipboard clipboard = null;
    private Throwable uncaughtException = null;
    private TaskMonitor taskMonitor = null;

    protected ApplicationContext() {
        resourceManager = new ResourceManager(this);
        actionManager = new ActionManager(this);
        localStorage = new LocalStorage(this);
        sessionStorage = new SessionStorage(this);
        taskServices = new CopyOnWriteArrayList<>();
        taskServices.add(new TaskService("default"));
        taskServicesReadOnly = Collections.unmodifiableList(taskServices);
    }

    /**
     * Returns the application's class or null if the application hasn't been
     * launched and this property hasn't been set. Once the application has been
     * launched, the value returned by this method is the same as
     * <code>getApplication().getClass()</code>.
     *
     * @return the application's class or null
     * @see #setApplicationClass(java.lang.Class) 
     * @see #getApplication() 
     */
    public final synchronized Class getApplicationClass() {
        return applicationClass;
    }

    /**
     * Called by {@link Application#launch Application.launch()} to record the
     * application's class.
     * <p>
     * This method is only intended for testing, or design time configuration.
     * Normal applications shouldn't need to call it directly.
     *
     * @see #getApplicationClass() 
     */
    public final synchronized void setApplicationClass(Class applicationClass) {
        if (this.application != null) {
            throw new IllegalStateException("application has been launched");
        }
        this.applicationClass = applicationClass;
    }

    /**
     * The <code>Application</code> singleton, or null if <code>launch</code>
     * hasn't been called yet.
     *
     * @return the launched Application singleton.
     * @see Application#launch(java.lang.Class, java.lang.String[]) 
     */
    public final synchronized Application getApplication() {
        return application;
    }

    /* Called by Application.launch().
     */
    synchronized void setApplication(Application application) {
        if (this.application != null) {
            throw new IllegalStateException("application has already been "
                    + "launched");
        }
        this.application = application;
    }

    /**
     * The application's <code>ResourceManager</code> provides read-only cached
     * access to resources in ResourceBundles via the
     * {@link ResourceMap ResourceMap} class.
     *
     * @return this application's ResourceManager.
     * @see #getResourceMap(java.lang.Class, java.lang.Class) 
     */
    public final ResourceManager getResourceManager() {
        return resourceManager;
    }

    /**
     * Change this application's <code>ResourceManager</code>. An
     * <code>ApplicationContext</code> subclass that wanted to fundamentally change
     * the way <code>ResourceMap</code>s were created and cached could replace this
     * property in its constructor.
     * <p>
     * Throws an IllegalArgumentException if resourceManager is null.
     *
     * @param resourceManager the new value of the resourceManager property.
     * @see #getResourceMap(java.lang.Class, java.lang.Class) 
     * @see #getResourceManager() 
     */
    protected void setResourceManager(ResourceManager resourceManager) {
        if (resourceManager == null) {
            throw new IllegalArgumentException("null resourceManager");
        }
        Object oldValue = this.resourceManager;
        this.resourceManager = resourceManager;
        firePropertyChange("resourceManager", oldValue, this.resourceManager);
    }

    /**
     * Returns a {@link ResourceMap#getParent chain} of two or more
     * ResourceMaps. The first encapsulates the ResourceBundles defined for the
     * specified class, and its parent encapsulates the ResourceBundles defined
     * for the entire application.
     * <p>
     * This is just a convenience method that calls      
     * {@link ResourceManager#getResourceMap(Class, Class)
     * ResourceManager.getResourceMap()}. It's defined as:
     * ```java
     * return getResourceManager().getResourceMap(cls, cls);
     * ```
     *
     * @param cls the class that defines the location of ResourceBundles
     * @return a <code>ResourceMap</code> that contains resources loaded from
     * <code>ResourceBundle</code>s found in the resources subpackage of the
     * specified class's package.
     * @see ResourceManager#getResourceMap(java.lang.Class) 
     */
    public final ResourceMap getResourceMap(Class cls) {
        return getResourceManager().getResourceMap(cls, cls);
    }

    /**
     * Returns a {@link ResourceMap#getParent chain} of two or more
     * ResourceMaps. The first encapsulates the ResourceBundles defined for the
     * all of the classes between <code>startClass</code> and <code>stopClass</code>
     * inclusive. It's parent encapsulates the ResourceBundles defined for the
     * entire application.
     * <p>
     * This is just a convenience method that calls      
     * {@link ResourceManager#getResourceMap(Class, Class)
     * ResourceManager.getResourceMap()}. It's defined as:
     * ```java
     * return getResourceManager().getResourceMap(startClass, stopClass);
     * ```
     *
     * @param startClass the first class whose ResourceBundles will be included
     * @param stopClass the last class whose ResourceBundles will be included
     * @return a <code>ResourceMap</code> that contains resources loaded from
     * <code>ResourceBundle</code>s found in the resources subpackage of the
     * specified class's package.
     * @see ResourceManager#getResourceMap(java.lang.Class, java.lang.Class) 
     */
    public final ResourceMap getResourceMap(Class startClass, Class stopClass) {
        return getResourceManager().getResourceMap(startClass, stopClass);
    }

    /**
     * Returns the {@link ResourceMap#getParent()  chain} of ResourceMaps that's
     * shared by the entire application, beginning with the one defined for the
     * Application class, i.e. the value of the <code>applicationClass</code>
     * property.
     * <p>
     * This is just a convenience method that calls      {@link ResourceManager#getResourceMap()
     * ResourceManager.getResourceMap()}. It's defined as:
     * <pre>
     * return getResourceManager().getResourceMap();
     * </pre>
     *
     * @return the Application's ResourceMap
     * @see ResourceManager#getResourceMap()
     * @see #getApplicationClass() 
     */
    public final ResourceMap getResourceMap() {
        return getResourceManager().getResourceMap();
    }

    /**
     *
     * @return this application's ActionManager.
     * @see #getActionMap() 
     */
    public final ActionManager getActionManager() {
        return actionManager;
    }

    /**
     * Change this application's <code>ActionManager</code>. An
     * <code>ApplicationContext</code> subclass that wanted to fundamentally change
     * the way <code>ActionManager</code>s were created and cached could replace this
     * property in its constructor.
     * <p>
     * Throws an IllegalArgumentException if actionManager is null.
     *
     * @param actionManager the new value of the actionManager property.
     * @see #getActionManager() 
     * @see #getActionMap(java.lang.Object) 
     */
    protected void setActionManager(ActionManager actionManager) {
        if (actionManager == null) {
            throw new IllegalArgumentException("null actionManager");
        }
        Object oldValue = this.actionManager;
        this.actionManager = actionManager;
        firePropertyChange("actionManager", oldValue, this.actionManager);
    }

    /**
     * Returns the shared <code>ActionMap</code> chain for the entire
     * <code>Application</code>.
     * <p>
     * This is just a convenience method that calls      
     * {@link ActionManager#getActionMap()
     * ActionManager.getActionMap()}. It's defined as:
     * ```java
     * return getActionManager().getActionMap()
     * ```
     *
     * @return the <code>ActionMap</code> chain for the entire <code>Application</code>.
     * @see ActionManager#getActionMap()
     */
    public final ApplicationActionMap getActionMap() {
        return getActionManager().getActionMap();
    }

    /**
     * Returns the <code>ApplicationActionMap</code> chain for the specified actions
     * class and target object.
     * <p>
     * This is just a convenience method that calls      
     * {@link ActionManager#getActionMap()
     * ActionManager.getActionMap(Class, Object)}. It's defined as:
     * ```java
     * return getActionManager().getActionMap(actionsClass, actionsObject)
     * ```
     *
     * @return the <code>ActionMap</code> chain for the entire <code>Application</code>.
     * @see ActionManager#getActionMap(java.lang.Class, java.lang.Object) 
     */
    public final ApplicationActionMap getActionMap(Class actionsClass, Object actionsObject) {
        return getActionManager().getActionMap(actionsClass, actionsObject);
    }

    /**
     * Defined as <code>getActionMap(actionsObject.getClass(), actionsObject)</code>.
     *
     * @return the <code>ActionMap</code> for the specified object
     * @see #getActionMap(java.lang.Class, java.lang.Object) 
     */
    public final ApplicationActionMap getActionMap(Object actionsObject) {
        if (actionsObject == null) {
            throw new IllegalArgumentException("null actionsObject");
        }
        return getActionManager().getActionMap(actionsObject.getClass(), actionsObject);
    }

    /**
     * The shared {@link LocalStorage LocalStorage} object.
     *
     * @return the shared {@link LocalStorage LocalStorage} object.
     */
    public final LocalStorage getLocalStorage() {
        return localStorage;
    }

    /**
     * The shared {@link LocalStorage LocalStorage} object.
     *
     * @param localStorage the shared {@link LocalStorage LocalStorage} object.
     */
    protected void setLocalStorage(LocalStorage localStorage) {
        if (localStorage == null) {
            throw new IllegalArgumentException("null localStorage");
        }
        Object oldValue = this.localStorage;
        this.localStorage = localStorage;
        firePropertyChange("localStorage", oldValue, this.localStorage);
    }

    /**
     * The shared {@link SessionStorage SessionStorage} object.
     *
     * @return the shared {@link SessionStorage SessionStorage} object.
     */
    public final SessionStorage getSessionStorage() {
        return sessionStorage;
    }

    /**
     * The shared {@link SessionStorage SessionStorage} object.
     *
     * @param sessionStorage the shared {@link SessionStorage SessionStorage}
     * object.
     */
    protected void setSessionStorage(SessionStorage sessionStorage) {
        if (sessionStorage == null) {
            throw new IllegalArgumentException("null sessionStorage");
        }
        Object oldValue = this.sessionStorage;
        this.sessionStorage = sessionStorage;
        firePropertyChange("sessionStorage", oldValue, this.sessionStorage);
    }

    /**
     * A shared <code>Clipboard</code>.
     * @return the shared clipboard
     */
    public Clipboard getClipboard() {
        if (clipboard == null) {
            try {
                clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            } catch (SecurityException e) {
                clipboard = new Clipboard("sandbox");
            }
        }
        return clipboard;
    }

    /**
     * The application's focus owner.
     * @return the focus owner
     */
    public JComponent getFocusOwner() {
        return focusOwner;
    }

    void setFocusOwner(JComponent focusOwner) {
        Object oldValue = this.focusOwner;
        this.focusOwner = focusOwner;
        firePropertyChange("focusOwner", oldValue, this.focusOwner);
    }

    private List<TaskService> copyTaskServices() {
        return new ArrayList<>(taskServices);
    }

    public void addTaskService(TaskService taskService) {
        if (taskService == null) {
            throw new IllegalArgumentException("null taskService");
        }
        List<TaskService> oldValue = null, newValue = null;
        boolean changed = false;
        synchronized (taskServices) {
            if (!taskServices.contains(taskService)) {
                oldValue = copyTaskServices();
                taskServices.add(taskService);
                newValue = copyTaskServices();
                changed = true;
            }
        }
        if (changed) {
            firePropertyChange("taskServices", oldValue, newValue);
        }
    }

    public void removeTaskService(TaskService taskService) {
        if (taskService == null) {
            throw new IllegalArgumentException("null taskService");
        }
        List<TaskService> oldValue = null, newValue = null;
        boolean changed = false;
        synchronized (taskServices) {
            if (taskServices.contains(taskService)) {
                oldValue = copyTaskServices();
                taskServices.remove(taskService);
                newValue = copyTaskServices();
                changed = true;
            }
        }
        if (changed) {
            firePropertyChange("taskServices", oldValue, newValue);
        }
    }

    public TaskService getTaskService(String name) {
        if (name == null) {
            throw new IllegalArgumentException("null name");
        }
        for (TaskService taskService : taskServices) {
            if (name.equals(taskService.getName())) {
                return taskService;
            }
        }
        return null;
    }

    /**
     * Returns the default TaskService, i.e. the one named "default":
     * <code>return getTaskService("default")</code>. The
     * {@link ApplicationAction#actionPerformed ApplicationAction actionPerformed}
     * method executes background <code>Tasks</code> on the default TaskService.
     * Application's can launch Tasks in the same way, e.g.
     * ```java
     * ApplicationContext.getInstance().getTaskService().execute(myTask);
     * ```
     *
     * @return the default TaskService.
     * @see #getTaskService(java.lang.String) 
     *
     */
    public final TaskService getTaskService() {
        return getTaskService("default");
    }

    /**
     * Returns a read-only view of the complete list of TaskServices.
     *
     * @return a list of all of the TaskServices.
     * @see #addTaskService(org.jdesktop.application.TaskService) 
     * @see #removeTaskService(org.jdesktop.application.TaskService) 
     */
    public List<TaskService> getTaskServices() {
        return taskServicesReadOnly;
    }

    /**
     * Returns a shared TaskMonitor object. Most applications only need one
     * TaskMonitor for the sake of status bars and other status indicators.
     *
     * @return the shared TaskMonitor object.
     */
    public final TaskMonitor getTaskMonitor() {
        if (taskMonitor == null) {
            taskMonitor = new TaskMonitor(this);
        }
        return taskMonitor;
    }
}
