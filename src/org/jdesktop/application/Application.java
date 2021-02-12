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
 *  Class      :   Application.java
 *  Author     :   Sean Carrick
 *  Created    :   Feb 10, 2021 @ 8:02:21 PM
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

import java.awt.ActiveEvent;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.PaintEvent;
import java.beans.Beans;
import java.lang.reflect.Constructor;
import java.util.EventListener;
import java.util.EventObject;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * The base class for Swing applications.
 * <p>
 * This class defines a simple lifecyle for Swing applications: <code>initialize
 * </code>, <code>startup</code>, <code>ready</code>, and <code>shutdown</code>.
 * The <code>Application</code>'s <code>startup</code> method is responsible for
 * creating the initial GUI and making it visible, and the {
 *
 * <code>shutdown</code> method for hiding the GUI and performing any other
 * cleanup actions before the application exits. The <code>initialize</code>
 * method can be used configure system properties that must be set before the
 * GUI is constructed and the <code>ready</code> method is for applications that
 * want to do a little bit of extra work once the GUI is "ready" to use.
 * Concrete subclasses must override the <code>startup</code> method.</p>
 * <p>
 * Applications are started with the static <code>launch</code> method.
 * Applications use the <code>ApplicationContext</code> {@link
 * Application#getContext singleton} to find resources, actions, local storage,
 * and so on.</p>
 * <p>
 * All <code>Application</code> subclasses must override <code>startup</code>
 * and they should call {@link #exit} (which calls <code>shutdown</code>) to
 * exit. Here's an example of a complete "Hello World" Application:
 * ```java
 * public class MyApplication extends Application { 
 *     JFrame mainFrame = null;
 * 
 *     &#064;Override 
 *     protected void startup() { 
 *         mainFrame = new JFrame("Hello World"); 
 *         mainFrame.add(new JLabel("Hello World"));
 *         mainFrame.addWindowListener(new MainFrameListener());
 *         mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
 *         mainFrame.pack(); mainFrame.setVisible(true); 
 *     } 
 * 
 *     &#064;Override 
 *     protected void shutdown() { 
 *         mainFrame.setVisible(false); 
 *     } 
 * 
 *     private class MainFrameListener extends WindowAdapter { 
 *         
 *         public void windowClosing(WindowEvent e) { 
 *             exit(); 
 *         }
 *     } 
 * 
 *     public static void main(String[] args) {
 *         Application.launch(MyApplication.class, args); 
 *     }
 * }
 * ```
 * <p>
 * The <code>mainFrame</code>'s <code>defaultCloseOperation</code> is set to
 * <code>DO_NOTHING_ON_CLOSE</code> because we're handling attempts to close the
 * window by calling <code>ApplicationContext</code> {@link #exit}.</p>
 * <p>
 * Simple single frame applications like the example can be defined more easily
 * with the {@link SingleFrameApplication SingleFrameApplication}
 * <code>Application</code> subclass.</p>
 * <p>
 * All of the Application's methods are called (must be called) on the EDT.</p>
 * <p>
 * All but the most trivial applications should define a ResourceBundle in the
 * resources subpackage with the same name as the application class (like
 * <code>resources/MyApplication.properties</code>). This ResourceBundle
 * contains resources shared by the entire application and should begin with the
 * following the standard Application resources:</p>
 * <pre>
 * Application.name = A short name, typically just a few words
 * Application.id = Suitable for Application specific identifiers, like file names
 * Application.title = A title suitable for dialogs and frames
 * Application.version = A version string that can be incorporated into messages
 * Application.vendor = A proper name, like Sun Microsystems, Inc.
 * Application.vendorId = suitable for Application-vendor specific identifiers, like file names.
 * Application.homepage = A URL like http://www.javadesktop.org
 * Application.description =  One brief sentence
 * Application.lookAndFeel = either system, default, or a LookAndFeel class name
 * </pre>
 * <p>
 * The <code>Application.lookAndFeel</code> resource is used to initialize the
 * <code>UIManager lookAndFeel</code> as follows:</p>
 * <ul>
 * <li><code>system</code> - the system (native) look and feel</li>
 * <li><code>default</code> - use the JVM default, typically the cross platform
 * look and feel</li>
 * <li>a LookAndFeel class name - use the specified class</li>
 * </ul>
 *
 * @see SingleFrameApplication
 * @see ApplicationContext
 * @see UIManager#setLookAndFeel(java.lang.String)
 *
 * @author Hans Muller (Original Author) &lt;current email unknown&gt;
 * @author Sean Carrick (Updater) &lt;sean at pekinsoft dot com&gt;
 *
 * @version 1.05
 * @since 1.03
 */
@ProxyActions({"cut", "copy", "paste", "delete"})

public abstract class Application extends AbstractBean {

    private static final Logger logger = Logger.getLogger(
            Application.class.getName());
    private static Application application = null;
    private final List<ExitListener> exitListeners;
    private final ApplicationContext context;

    /**
     * Not to be called directly, see {@link #launch launch}.
     * <p>
     * Subclasses can provide a no-args constructor to initialize private final
     * state however GUI initialization, and anything else that might refer to
     * public API, should be done in the {@link #startup startup} method.</p>
     */
    protected Application() {
        exitListeners = new CopyOnWriteArrayList<>();
        context = new ApplicationContext();
    }

    /**
     * Creates an instance of the specified <code>Application</code> subclass,
     * sets the <code>ApplicationContext</code> <code>application</code>
     * property, and then calls the new <code>Application</code>'s <code>startup
     * </code> method. The <code>launch</code> method is typically called from
     * the Application's <code>main</code>:
     * ```java 
     *     public static void main(String[] args) { 
     *         Application.launch(MyApplication.class, args); 
     *     }
     * ```
     * <p>
     * The <code>applicationClass</code> <code>startup</code></p>
     *
     * @param <T>
     * @param applicationClass the <code>Application</code> class to launch
     * @param args <code>main</code> method arguments
     * @see #shutdown()
     * @see ApplicationContext#getApplication()
     */
    public static synchronized <T extends Application> void launch(
            final Class<T> applicationClass, final String[] args) {
        Runnable doCreateAndShowGUI = () -> {
            try {
                application = create(applicationClass);
                application.initialize(args);
                application.startup();
                application.waitForReady();
            } catch (Exception e) {
                String msg = String.format("Application %s failed to launch",
                        applicationClass);
                logger.log(Level.SEVERE, msg, e);
                throw (new Error(msg, e));
            }
        };
        SwingUtilities.invokeLater(doCreateAndShowGUI);
    }

    /* Initializes the ApplicationContext applicationClass and application
     * properties.  
     * 
     * Note that, as of Java SE 5, referring to a class literal
     * doesn't force the class to be loaded.  More info:
     * http://java.sun.com/javase/technologies/compatibility.jsp#literal
     * It's important to perform these initializations early, so that
     * Application static blocks/initializers happen afterwards.
     */
    static <T extends Application> T create(Class<T> applicationClass)
            throws Exception {

        if (!Beans.isDesignTime()) {
            /* A common mistake for privileged applications that make
             * network requests (and aren't applets or web started) is to
             * not configure the http.proxyHost/Port system properties.
             * We paper over that issue here.
             */
            try {
                System.setProperty("java.net.useSystemProxies", "true");
            } catch (SecurityException ignoreException) {
                // Unsigned apps can't set this property. 
            }
        }

        /* Construct the Application object.  The following complications, 
         * relative to just calling applicationClass.newInstance(), allow a 
         * privileged app to have a private static inner Application subclass.
         */
        Constructor<T> ctor = applicationClass.getDeclaredConstructor();
        // TODO: Make sure the modification here from the deprecated method
        //+ isAccessible() to the "replacement" method canAccess(Object) does
        //+ not break anything in the API.
        //+                                        -> Sean Carrick, Feb 10, 2021
        if (!ctor.canAccess(applicationClass)) {
            try {
                ctor.setAccessible(true);
            } catch (SecurityException ignore) {
                // ctor.newInstance() will throw an IllegalAccessException
            }
        }
        T app = ctor.newInstance();

        /* Initialize the ApplicationContext application properties
         */
        ApplicationContext ctx = app.getContext();
        ctx.setApplicationClass(applicationClass);
        ctx.setApplication(app);

        /* Load the application resource map, notably the 
	 * Application.* properties.
         */
        ResourceMap appResourceMap = ctx.getResourceMap();

        appResourceMap.putResource("platform", platform());

        if (!Beans.isDesignTime()) {
            /* Initialize the UIManager lookAndFeel property with the
             * Application.lookAndFeel resource.  If the the resource
             * isn't defined we default to "system".
             */
            String key = "Application.lookAndFeel";
            String lnfResource = appResourceMap.getString(key);
            String lnf = (lnfResource == null) ? "system" : lnfResource;
            try {
                if (lnf.equalsIgnoreCase("system")) {
                    String name = UIManager.getSystemLookAndFeelClassName();
                    UIManager.setLookAndFeel(name);
                } else if (!lnf.equalsIgnoreCase("default")) {
                    UIManager.setLookAndFeel(lnf);
                }
            } catch (ClassNotFoundException
                    | IllegalAccessException
                    | InstantiationException
                    | UnsupportedLookAndFeelException e) {
                String s = "Couldn't set LookandFeel " + key + " = \"" 
                        + lnfResource + "\"";
                logger.log(Level.WARNING, s, e);
            }
        }

        return app;
    }

    /* Defines the default value for the platform resource, 
     * either "osx" or "default".
     */
    private static String platform() {
        String platform = "default";
        try {
            String osName = System.getProperty("os.name");
            if ((osName != null) && osName.toLowerCase().startsWith("mac os x")) {
                platform = "osx";
            }
        } catch (SecurityException ignore) {
        }
        return platform;
    }

    /* Call the ready method when the eventQ is quiet.
     */
    void waitForReady() {
        new DoWaitForEmptyEventQ().execute();
    }

    /**
     * Responsible for initializations that must occur before the GUI is
     * constructed by <code>startup</code>.
     * <p>
     * This method is called by the static <code>launch</code> method, before
     * <code>startup</code> is called. Subclasses that want to do any
     * initialization work before <code>startup</code> must override it. The
     * <code>initialize</code> method runs on the event dispatching thread.</p>
     * <p>
     * By default initialize() does nothing.</p>
     *
     * @param args the main method's arguments.
     * @see #launch(java.lang.Class, java.lang.String[])
     * @see #startup()
     * @see #shutdown()
     */
    protected void initialize(String[] args) {
    }

    /**
     * Responsible for starting the application: for creating and showing the
     * initial GUI.
     * <p>
     * This method is called by the static <code>launch</code> method,
     * subclasses must override it. It runs on the event dispatching thread.</p>
     *
     * @see #launch(java.lang.Class, java.lang.String[])
     * @see #initialize(java.lang.String[])
     * @see #shutdown()
     */
    protected abstract void startup();

    /**
     * Called after the startup() method has returned and there are no more
     * events on the {@link Toolkit#getSystemEventQueue system event queue}.
     * When this method is called, the application's GUI is ready to use.
     * <p>
     * It's usually important for an application to start up as quickly as
     * possible. Applications can override this method to do some additional
     * start up work, after the GUI is up and ready to use.
     *
     * @see #launch(java.lang.Class, java.lang.String[])
     * @see #startup()
     * @see #shutdown()
     */
    protected void ready() {
    }

    /**
     * Called when the application {@link #exit exits}. Subclasses may override
     * this method to do any cleanup tasks that are neccessary before exiting.
     * Obviously, you'll want to try and do as little as possible at this point.
     * This method runs on the event dispatching thread.
     *
     * @see #startup()
     * @see #ready()
     * @see #exit()
     * @see #addExitListener(org.jdesktop.application.Application.ExitListener)
     */
    protected void shutdown() {
        // TBD should call TaskService#shutdownNow() on each TaskService
    }

    /* An event that sets a flag when it's dispatched and another
     * flag, see isEventQEmpty(), that indicates if the event queue
     * was empty at dispatch time.
     */
    private static class NotifyingEvent extends PaintEvent implements ActiveEvent {

        private boolean dispatched = false;
        private boolean qEmpty = false;

        NotifyingEvent(Component c) {
            super(c, PaintEvent.UPDATE, null);
        }

        synchronized boolean isDispatched() {
            return dispatched;
        }

        synchronized boolean isEventQEmpty() {
            return qEmpty;
        }

        @Override
        public void dispatch() {
            EventQueue q = Toolkit.getDefaultToolkit().getSystemEventQueue();
            synchronized (this) {
                qEmpty = (q.peekEvent() == null);
                dispatched = true;
                notifyAll();
            }
        }
    }

    /* Keep queuing up NotifyingEvents until the event queue is
     * empty when the NotifyingEvent is dispatched().
     */
    private void waitForEmptyEventQ() {
        boolean qEmpty = false;
        JPanel placeHolder = new JPanel();
        EventQueue q = Toolkit.getDefaultToolkit().getSystemEventQueue();
        while (!qEmpty) {
            NotifyingEvent e = new NotifyingEvent(placeHolder);
            q.postEvent(e);
            synchronized (e) {
                while (!e.isDispatched()) {
                    try {
                        e.wait();
                    } catch (InterruptedException ie) {
                    }
                }
                qEmpty = e.isEventQEmpty();
            }
        }
    }

    /* When the event queue is empty, give the app a chance to do
     * something, now that the GUI is "ready".
     */
    private class DoWaitForEmptyEventQ extends Task<Void, Void> {

        DoWaitForEmptyEventQ() {
            super(Application.this);
        }

        @Override
        protected Void doInBackground() {
            waitForEmptyEventQ();
            return null;
        }

        @Override
        protected void finished() {
            ready();
        }
    }

    /**
     * Gracefully shutdown the application, calls <code>exit(null)</code> This
     * version of exit() is convenient if the decision to exit the application
     * wasn't triggered by an event.
     *
     * @see #exit(EventObject)
     */
    public final void exit() {
        exit(null);
    }

    /**
     * Gracefully shutdown the application.
     * <p>
     * If none of the <code>ExitListener.canExit()</code> methods return false,
     * calls the <code>ExitListener.willExit()</code> methods, then
     * <code>shutdown()</code>, and then exits the Application with {@link #end
     * end}. Exceptions thrown while running willExit() or shutdown() are logged
     * but otherwise ignored.</p>
     * <p>
     * If the caller is responding to an GUI event, it's helpful to pass the
     * event along so that ExitListeners' canExit methods that want to popup a
     * dialog know on which screen to show the dialog. For example:</p>
     * ```java 
     * class ConfirmExit implements Application.ExitListener { 
     *     public boolean canExit(EventObject e) { 
     *         Object source = (e != null) ? e.getSource() : null; 
     *         Component owner = (source instanceof Component) ? (Component)source : null; 
     *         int option = JOptionPane.showConfirmDialog(owner, "Really Exit?"); 
     *         return option == JOptionPane.YES_OPTION; 
     *     } 
     *     
     *     public void willExit(EventObejct e) {
     *     
     *     }
     * }
     * 
     * myApplication.addExitListener(new ConfirmExit());
     * ```
     * <p>
     * The <code>eventObject</code> argument may be null, e.g. if the exit call
     * was triggered by non-GUI code, and <code>canExit</code>, <code>
     * willExit</code> methods must guard against the possibility that the
     * <code>eventObject</code> argument's <code>source</code> is not a
     * <code>Component</code>.
     *
     * @param event the EventObject that triggered this call or null
     * @see #addExitListener(org.jdesktop.application.Application.ExitListener)
     * @see
     * #removeExitListener(org.jdesktop.application.Application.ExitListener)
     * @see #shutdown()
     * @see #end()
     */
    public void exit(EventObject event) {
        for (ExitListener listener : exitListeners) {
            if (!listener.canExit(event)) {
                return;
            }
        }
        try {
            exitListeners.forEach(listener -> {
                try {
                    listener.willExit(event);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "ExitListener.willExit() failed",
                            e);
                }
            });
            shutdown();
        } catch (Exception e) {
            logger.log(Level.WARNING, "unexpected error in Application."
                    + "shutdown()", e);
        } finally {
            end();
        }
    }

    /**
     * Called by {@link #exit exit} to terminate the application. Calls
     * <code>Runtime.getRuntime().exit(0)</code>, which halts the JVM.
     *
     * @see #exit()
     */
    protected void end() {
        Runtime.getRuntime().exit(0);
    }

    /**
     * Give the Application a chance to veto an attempt to exit/quit. An
     * <code>ExitListener</code>'s <code>canExit</code> method should return
     * false if there are pending decisions that the user must make before the
     * app exits. A typical <code>ExitListener</code> would prompt the user with
     * a modal dialog.</p>
     * <p>
     * The <code>eventObject</code> argument will be the the value passed to
     * {@link #exit(EventObject) exit()}. It may be null.</p>
     * <p>
     * The <code>willExit</code> method is called after the exit has been
     * confirmed. An ExitListener that's going to perform some cleanup work
     * should do so in <code>willExit</code>.</p>
     * <p>
     * <code>ExitListeners</code> run on the event dispatching thread.</p>
     *
     * <dl><dt><code>canExit(EventObject event)</code></dt>
     * <dd>The EventObject that triggered this call or null</dd></dl>
     *
     * @see #exit(java.util.EventObject)
     * @see #addExitListener(org.jdesktop.application.Application.ExitListener)
     * @see
     * #removeExitListener(org.jdesktop.application.Application.ExitListener)
     */
    public interface ExitListener extends EventListener {

        boolean canExit(EventObject event);

        void willExit(EventObject event);
    }

    /**
     * Add an <code>ExitListener</code> to the list.
     *
     * @param listener the <code>ExitListener</code>
     * @see #removeExitListener(org.jdesktop.application.Application.ExitListener) 
     * @see #getExitListeners() 
     */
    public void addExitListener(ExitListener listener) {
        exitListeners.add(listener);
    }

    /**
     * Remove an <code>ExitListener</code> from the list.
     *
     * @param listener the <code>ExitListener</code>
     * @see #addExitListener(org.jdesktop.application.Application.ExitListener) 
     * @see #getExitListeners() 
     */
    public void removeExitListener(ExitListener listener) {
        exitListeners.remove(listener);
    }

    /**
     * All of the <code>ExitListeners</code> added so far.
     *
     * @return all of the <code>ExitListeners</code> added so far.
     */
    public ExitListener[] getExitListeners() {
        int size = exitListeners.size();
        return exitListeners.toArray(new ExitListener[size]);
    }

    /**
     * The default <code>Action</code> for quitting an application,
     * <code>quit</code> just exits the application by calling
     * <code>exit(e)</code>.
     *
     * @param e the triggering event
     * @see #exit(java.util.EventObject) 
     */
    @Action
    public void quit(ActionEvent e) {
        exit(e);
    }

    /**
     * The ApplicationContext singleton for this Application.
     *
     * @return the Application's ApplicationContext singleton
     */
    public final ApplicationContext getContext() {
        return context;
    }

    /**
     * The <code>Application</code> singleton.
     * <p>
     * Typically this method is only called after an Application has been
     * launched however in some situations, like tests, it's useful to be able
     * to get an <code>Application</code> object without actually launching. In
     * that case, an instance of the specified class is constructed and
     * configured as it would be by the {@link #launch launch} method. However
     * it's <code>initialize</code> and <code>startup</code> methods are not
     * run.
     *
     * @param <T> type of subclass of <code>Application</code>
     * @param applicationClass this Application's subclass
     * @return the launched Application singleton.
     * @see Application#launch(java.lang.Class, java.lang.String[]) 
     */
    public static synchronized <T extends Application> T getInstance(
            Class<T> applicationClass) {
        if (application == null) {
            /* Special case: the application hasn't been launched.  We're
             * constructing the applicationClass here to get the same effect
             * as the NoApplication class serves for getInstance().  We're
             * not launching the app, no initialize/startup/wait steps.
             */
            try {
                application = create(applicationClass);
            } catch (Exception e) {
                String msg = String.format("Couldn't construct %s",
                        applicationClass);
                throw (new Error(msg, e));
            }
        }
        return applicationClass.cast(application);
    }

    /**
     * The <code>Application</code> singleton, or a placeholder if <code>launch
     * </code> hasn't been called yet.
     * <p>
     * Typically this method is only called after an Application has been
     * launched however in some situations, like tests, it's useful to be able
     * to get an <code>Application</code> object without actually launching. The
     * <i>placeholder</i> Application object provides access to an
     * <code>ApplicationContext</code> singleton and has the same semantics as
     * launching an Application defined like this:</p>
     * ```java 
     * public class PlaceholderApplication extends Application { 
     *     public void startup() { } 
     * } 
     * 
     * Application.launch(PlaceholderApplication.class);
     * ```
     *
     * @return the Application singleton or a placeholder
     * @see Application#launch(java.lang.Class, java.lang.String[])
     * @see Application#getInstance(java.lang.Class)
     */
    public static synchronized Application getInstance() {
        if (application == null) {
            application = new NoApplication();
        }
        return application;
    }

    private static class NoApplication extends Application {

        protected NoApplication() {
            ApplicationContext ctx = getContext();
            ctx.setApplicationClass(getClass());
            ctx.setApplication(this);
            ResourceMap appResourceMap = ctx.getResourceMap();
            appResourceMap.putResource("platform", platform());
        }

        @Override
        protected void startup() {
        }
    }


    /* Prototype support for the View type */
    public void show(View view) {
        Window window = (Window) view.getRootPane().getParent();
        if (window != null) {
            window.pack();
            window.setVisible(true);
        }
    }

    public void hide(View view) {
        view.getRootPane().getParent().setVisible(false);
    }
}
