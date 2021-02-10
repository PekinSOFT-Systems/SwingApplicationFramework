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
 * Project    :   Northwind
 * Class      :   Application.java
 * Author     :   Sean Carrick <sean at pekinsoft dot com>
 * Created    :   Dec 27, 2020 @ 10:36:03 AM
 * Modified   :   Dec 27, 2020
 *  
 * Purpose:
 *          Provides the basis of the Desktop Framework.
 * 	
 * Revision History:
 *  
 * WHEN          BY                  REASON
 * ------------  ------------------- -------------------------------------------
 * Dec 27, 2020     Sean Carrick             Initial creation.
 * *****************************************************************************
 */
package org.jdesktop.application;

import org.jdesktop.application.Application.ExitListener;
import org.jdesktop.application.enums.SysExits;
import org.jdesktop.application.err.InvalidLoggingLevelException;
import org.jdesktop.application.utils.ArgumentParser;
import org.jdesktop.application.utils.Logger;
import org.jdesktop.application.utils.TerminalErrorPrinter;
import org.jdesktop.application.view.View;
import java.awt.ActiveEvent;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.PaintEvent;
import java.beans.Beans;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.EventListener;
import java.util.EventObject;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * The base class for Swing applications.
 * <p>
 * This class defines a simple lifecycle for Swing application:
 * `initialize`, `startup`, `ready`, and `shutdown`. The
 * <tt>Application`'s `startup</tt> method is responsible for creating the
 * initial GUI and making it visible, and the <tt>shutdown</tt> method for hiding
 * the GUI and performing any other cleanup actions before the application
 * exits. The <tt>initialize</tt> method can be used to configure system
 * properties that must be set before the GUI is constructed and the
 * <tt>ready</tt> method is for applications that want to do a little bit of
 * extra work once the GUI is "ready" to use. Concrete subclasses <em>must</em>
 * override the <tt>startup</tt> method.</p>
 * <p>
 * Applications are started with static <tt>launch</tt> method. Applications use
 * the <tt>ApplicationContext` {@link Application#getContext singleton</tt> to
 * find resources, actions, local storage, and so on.</p>
 * <p>
 * All <tt>Application</tt> subclasses <em>must</em> override the `startup`
 * method and they should call {@link #exit} (which calls `shutdown`) to
 * exit. Here's an example of a complete "Hello World" Application:</p>
 * ```java
 * public class MyApplication extends Application {
 *     JFrame mainFrame = null;
 *
 *     @Override
 *     protected void startup() {
 *         mainFrame = new JFrame("Hello World");
 *         mainFrame.add(new JLabel("Hello World");
 *         mainFrame.addWindowListener(new MainFrameListener());
 *         mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
 *         mainFrame.pack();
 *         mainFrame.setVisible(true);
 *     }
 *
 *     @Override
 *     protected void shutdown() {
 *         mainFrame.setVisible(false);
 *     }
 *
 *     private class MainFrameListener extends WindowAdapter {
 *         public void windowClosing(WindowEvent evt) {
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
 * The <tt>mainFrame`'s `defaultCloseOperation</tt> is set to `DO_NOTHING_ON_CLOSE`
 * because we are handling attempts to close the window by
 * calling <tt>ApplicationContext</tt> {@link exit`.</p>
 * <p>
 * Simple, single-frame applications, like the example, can be defined more
 * easily with the {@link SingleFrameApplication SingleFrameApplication} 
 * <tt>Application</tt> subclass.</p>
 * <p>
 * All of the `Application`'s methods are called (<strong>must</strong> be called)
 * on the EDT.</p>
 * <p>
 * All but the most trivial applications should define a ResourceBundle in the
 * resources subpackage with the same name as the application class, like 
 * <tt>resources/MyApplication.properties`. This `ResourceBundle</tt> contains resources
 * shared by the entire application and should begin with the following standard
 * <tt>Application</tt> resources:</p>
 * <pre>
 * Application.name=A short name, typically just a few words
 * Application.id=Suitable for Application specific identifiers, like file names
 * Application.title=A title suitable for dialogs and frames
 * Application.vendor=A proper name, like PekinSOFT Systems
 * Application.vendorId=Suitable for Application-vendor specific identifiers,
 *                      like file names
 * Application.homepage=A URL like https://framework.pekinsoft.com
 * Application.description.short=A single-line, brief description of the Application
 * Application.lookAndFeel=either system, default, or a lookAndFeel class name
 * </pre>
 * <p>
 * The <tt>Application.lookAndFeel</tt> resource is used to initialize the
 * <tt>UIManager lookAndFeel</tt> as follows:</p>
 * <ul>
 * <li>`system` &mdash; the system (native) look and feel</li>
 * <li>`default` &mdash; use the JVM default, typically the cross-platform
 * look and feel</li>
 * <li>a LookAndFeel class name &mdash; use the specified class</li>
 * </ul>
 * <p>
 * The <tt>Application</tt> framework has four built-in command line switches
 * which are present and available for use in all applications derived from this
 * framework:</p>
 * <ul>
 * <li>`-d` or <tt>--debug</tt> &mdash; either of these two switches may be
 * used to indicate that the application logs should log all messages set at the
 * <tt>Logger.DEBUG</tt> level or higher. This creates more verbose application
 * logs, which helps to track down errors and bugs.</li>
 * <li>`-i` or <tt>--ide</tt> &mdash; either of these two switches should
 * be used in the development environment while developing the software project.
 * What these two switches tell the application is "We are developing this
 * project, so calculate the version number for us."</li></ul><p>
 * As you just read, the Swing Application Framework not only provides a lot of
 * run-time functionality "out-of-the-box," but it also manages versioning of
 * your project during development. We picked an arbitrary number to start the
 * <tt>build` number at, which is 1903. Once the `build</tt> number
 * surpasses 4999, the <tt>revision</tt> number is incremented by 1 and the
 * <tt>build</tt> number is reset to 1903.</p>
 * <p>
 * Once the <tt>revision` number surpasses 30, the `minor</tt> number is
 * incremented by one, and the <tt>revision</tt> number is reset to zero.</p>
 * <p>
 * Once the <tt>minor` number surpasses 10, the `major</tt> number is
 * incremented by one, and the minor number is reset to zero.</p>
 * <p>
 * The way the version numbers are calculated, the version will change only
 * while executing the application with either the <tt>-i</tt> or `--ide`
 * switch passed on the command line. However, each time the project is executed
 * in the IDE, while one of those switches is present in the IDE or Project
 * settings, the version <tt>build</tt> number will increment each time the
 * project is run. It is for this reason that we made the version calculator
 * require 3096 runs of the project with one of the IDE switches present before
 * it updates the <tt>revision</tt> number.</p>
 * <p>
 * When it comes to the <tt>-d` and `--debug</tt> switches, having one of
 * them present during development will help you track down nefarious bugs and
 * logic flaws prior to deploying your project to end-users. Also, if an
 * end-user calls into your tech support after a version has been deployed, your
 * tech support can have the user add one of those two switches to the
 * application startup file so that the user will be able to try to replicate
 * what they were doing when the bug reared its ugly head. Then, if the bug
 * rears its ugly head again, the user will be able to email a detailed log file
 * to tech support to aide them in tracking down the issue.</p>
 * <dl>
 * <dt>Developer's Caveat:</dt><dd>Supplying the <tt>-d</tt> or `--debug`
 * switch on the command line is the smallest part of the battle. The victory
 * comes when you are creating quality log entries throughout your program's
 * source code. We, at PekinSOFT Systems, tend to use log entries as the
 * comments in our source code. Instead of using standard Java inline and block
 * comments, get yourself into the habit of commenting your code with log
 * entries. This way, you will always get high caliber, very detailed logs from
 * your application's end-users if they run into a problem. Simply have them
 * supply one of the two debugging command line switches to the application's
 * startup and you will be able to easily track down the bug/logic flaw/error
 * that prompted the user to call tech support in the first place.</dd></dl>
 * <dl>
 * <dt><em>Sidenote</em>:</dt><dd>To that last statement we would just like to
 * add that the quality log to track down the bug/logic flaw/error will only
 * aide you in your quest <strong><em>if</em></strong> the user is <em>able</em>
 * to recreate the error in the first place.</dd></dl>
 *
 * @see SingleFrameApplication
 * @see ApplicationContext
 * @see UIManager#setLookAndFeel
 *
 * @author Hans Muller (Original Author)
 * @author Sean Carrick (Adapting Author) &lt;sean at pekinsoft dot com&gt;
 *
 * @version 1.05
 * @since 1.03
 */
@ProxyActions({"cut", "copy", "paste", "delete"})
public abstract class Application extends AbstractBean {

    // Default logging level will be set to INFO, unless overridden via a command
    //+ line option.
    private static final int LEVEL = Logger.INFO;

    // The default location for the application setting file. This will need to
    //+ be overriden if you want the application name to be used. to override
    //+ this default, you can use a command line option.
    private static final String DFLT_CONFIG = System.getProperty("user.home")
            + File.separator + ".application.properties";

    // The properties object for the application class. The Desktop system, by
    //+ default, creates a <tt>config</tt> folder under the application folder for
    //+ storing configuration files. The reason is that each class that would
    //+ like to store files in an application will need to have its own config
    //+ file which contains only settings/configuration for that class.
    private static final Properties props = new Properties();

    // In prior PekinSOFT Systems software, all classes within an application
    //+ would write to the same log file. However, for the Desktop API, we 
    //+ decided that each class should create and use its own log file because
    //+ the single log file per application would grow extremely long and defeat
    //+ the purpose of creating log files by making it extremely difficult to
    //+ find the problem(s) the application was having. This was especially true
    //+ when the logging level was set to DEBUG during development. Therefore,
    //+ we have decided to make the log for the <tt>Application</tt> class `private`
    //+ and force each class to have its own log.
    //+
    //+ This will not impose any additional requirements of developers using the
    //+ Desktop API, as the log will be created within the API objects that the
    //+ developers will be either extending or implementing. It will simply be
    //+ there for the developer to use.
    private static Logger logger = Logger.getLogger(Application.class.getName());
    private static Application application = null;
    private final List<ExitListener> exitListeners;
    private final ApplicationContext context;

    private static int major;       // Major version number:          X.x.x xxxx
    private static int minor;       // Minor version number:          x.X.x xxxx
    private static int revision;    // Revision version number:       x.x.X xxxx
    private static long build;      // Build number:                  x.x.x XXXX

    /**
     * Not to be called directly, see 
     * {@link #launch(java.lang.Class, java.lang.String[])  launch}.
     * <p>
     * Subclasses can provide a no-arg constructor to initialize private final
     * state, however, GUI initialization, and anything else that might refer to
     * public API, should be done in the {@link #startup() }
     * startup} method.
     */
    protected Application() {
        exitListeners = new CopyOnWriteArrayList<>();
        context = new ApplicationContext();
    }

    /**
     * Creates an instance of the specified <tt>Application</tt> subclass, sets
     * the <tt>ApplicationContext` `Application</tt> property, and then
     * calls the new <tt>Application`'s `startup` method. The `launch</tt> method is 
     * typically called from the Application's `main`:
     * <pre>
     * public static void main (String[] args) {
     *     Application.launch(MyApplication.class, args);
     * }
     * </pre>
     * <p>
     * The <tt>applicationClass` constructor and `startup</tt> methods run
     * on the event dispatching thread.</p>
     *
     * @param <T> the <tt>Class</tt> type of the application
     * @param applicationClass the <tt>Application</tt> class to launch
     * @param args the <tt>main</tt> method arguments
     * @see #shutdown
     * @see ApplicationContext#getApplication
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
                logger.critical(e, "com.pekinsoft.desktop.application",
                        applicationClass.getName(), "launch",
                        "Swing Application Framework", version(), build);
                throw new Error(msg, e);
            }
        };

        SwingUtilities.invokeLater(doCreateAndShowGUI);
    }

    /* Initializes the ApplicationContext applicationClass and application
     * properties.
     *
     * Note that, as of Java SE 5, referring to a class literal does not force
     * the class to be loaded. More info: 
     * 
     * The original comment was pointint to a reference on http://java.sun.com.
     * I have taken the time to provide that same reference material via The 
     * Wayback Machine (according to a crawl performed on 08/19/2006 @ 11:18:19):
     * http://web.archive.org/web/20060819111819/http://java.sun.com/javase/technologies/compatibility.jsp
     *
     * It is important to perform these initializations early, so that 
     * Application static blocks/initializers happen afterwards.
     */
    static <T extends Application> T create(Class<T> applicationClass)
            throws Exception {
        if (!Beans.isDesignTime()) {
            /* A common mistake for privileged applications that make network
             * requests (and are not applets or web started) is to not configure
             * the http.proxyHost/Port system properties. We paper over that
             * issue here.
             */
            try {
                System.setProperty("java.net.useSystemProxies", "true");
            } catch (SecurityException ignoreException) {
                // Unsigned apps can not set this property.
                logger.debug("Application is unsigned: "
                        + ignoreException.getMessage());
            }
        }

        Constructor<T> ctor = applicationClass.getDeclaredConstructor();
        if (!ctor.isAccessible()) {
            try {
                ctor.setAccessible(true);
            } catch (SecurityException ignore) {
                // ctor.newInstance() will throw an IllegalAccessException
                logger.debug("Avoiding IllegalAccessException");
            }
        }

        T application = ctor.newInstance();

        /* Initialize the ApplicationContext application properties */
        ApplicationContext ctx = application.getContext();
        ctx.setApplicationClass(applicationClass);
        ctx.setApplication(application);

        /* Load the application resource map, notably the Application.*
         * properties.
         */
        ResourceMap appResourceMap = ctx.getResourceMap();
        appResourceMap.putResource("platform", platform());

        if (!Beans.isDesignTime()) {
            /* Initialize the UIManager lookAndFeel proeprty with the 
             * Application.lookAndFeel resource. If teh resource is not defined,
             * we default to "system".
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
                String s = "Could not set LookAndFeel " + key + " = \""
                        + lnfResource + "\"";
                logger.warning(s + e.getMessage() + " cause: "
                        + e.getCause().toString());
            }
        }

        return application;
    }

    /* Defines the default value for the platform resource, either "osx" or 
     * "default". */
    private static String platform() {
        String platform = "default";

        try {
            String osName = System.getProperty("os.name");
            if ((osName != null) && osName.toLowerCase().startsWith("mac os x")) {
                platform = "osx";
            }
        } catch (SecurityException ignore) {
            logger.debug("SecurityException thrown while trying to determine "
                    + "the platform on which the applicaiton is running."
                    + "\n\nMessage: " + ignore.getMessage() + "\n\nCause: "
                    + ignore.getCause().toString());
        }

        return platform;
    }

    // Call the ready method when the eventQ is quiet.
    void waitForReady() {
        new DoWaitForEmptyEventQ().execute();
    }

    /**
     * Responsible for initializations that must occur before the GUI is
     * constructed by `startup`.
     * <p>
     * This method is called by the static <tt>launch` method, before `startup</tt> is 
     * called. Subclasses that want to do any initialization work
     * before <tt>startup` must override it. The `initialize</tt> method
     * runs on the event dispatching thread.</p>
     * <p>
     * The <tt>initialize(String[] args)</tt> method should be called by any
     * <tt>Application</tt> that accepts command-line arguments. For all of
     * PekinSOFT Systems' applications, the <tt>initialize</tt> method is used
     * because all of PekinSOFT Systems' applications use command-line
     * parameters for debugging purposes and calculating application versions.
     * </p>
     * <dl><dt>Note:</dt><dd>When overriding this method, make sure to call the
     * method in the super class:
     * <pre>
     * @Override
     * protected void initialize(String[] args) {
     *     super.initialize(args);
     *
     *     // ...
     *
     * }
     * </pre></dd></dl>
     *
     * @param args the <tt>main</tt> method arguments
     * @see #launch(java.lang.Class, java.lang.String[]) 
     * @see #startup() 
     * @see #shutdown()
     */
    protected void initialize(String[] args) {
        logger.enter(Application.class.getName(), "initialize", args);
        logger.info("Checking command line parameters for debugging flag.");
        ArgumentParser parser = new ArgumentParser(args);
        if (parser.isSwitchPresent("-d") || parser.isSwitchPresent("--debug")) {
            try {
                logger.setLevel(Logger.DEBUG);
            } catch (InvalidLoggingLevelException ex) {
                TerminalErrorPrinter.printException(ex);
            }
        }

        logger.debug("Calculating the application version.");
        if (logger.getLevel() == Logger.DEBUG) {
            logger.exit(Application.class.getName(), "startup(String[])");
            calculateVersion();
        }
    }

    /**
     * Responsible for starting the application; for creating and showing the
     * initial GUI.
     * <p>
     * This method is called by the static <tt>launch</tt> method, subclasses
     * must override it. It runs on the event dispatching thread.
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
     * It is usually important for an application to start up as quickly as
     * possible. Applications can override this method to do some additionale
     * start up work, after the GUI is up and ready to use.</p>
     *
     * @see #launch(java.lang.Class, java.lang.String[]) 
     * @see #startup()
     * @see #shutdown()
     */
    protected void ready() {

    }

    /**
     * Called when the application {@link #exit(java.util.EventObject) 
     * exits}. Subclasses may override this method to do any cleanup tasks that
     * are necessary before exiting. Obviously, you will want to try to do as
     * little as possible at this point. This method runs on the event
     * dispatching thread.
     *
     * @see #startup()
     * @see #ready()
     * @see #exit(java.util.EventObject) 
     * @see #addExitListener(ExitListener)
     */
    protected void shutdown() {
        // TODO: Should call TaskService#shutdownNow() on each TaskService.
    }

    /* An event that sets a flag when it is dispatched and another flag, see 
     * isEventQEmpty(), that indicates if the event queue was empty at dispatch
     * time.
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

    /* Keep queuing up NotifyingEvents until the event queue is empty when the 
     * NotifyingEvent is dispatched().
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
    
    /* When the event queue is empty, give the app a chance to do something, now
     * that the GUID is "ready".
     */
    private class DoWaitForEmptyEventQ extends Task<Void, Void> {
        
        DoWaitForEmptyEventQ() {
            super(Application.this);
        }

        @Override
        protected Void doInBackground() throws Exception {
            waitForEmptyEventQ();
            return null;
        }
        
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
        // TODO: Implement clone method.
    }

    private static void calculateVersion() {
        logger.enter(Application.class.getName(), "calculateVersion()");
        major = Integer.parseInt(props.getProperty("Application.major"));
        minor = Integer.parseInt(props.getProperty("Application.minor"));
        revision = Integer.parseInt(props.getProperty("Application.revision"));
        build = Long.parseLong(props.getProperty("Application.build"));

        logger.debug("Checking build number.");
        if (build > 4999) {
            logger.debug("Build number is 5000 or greater. Incrementing revision.");
            revision++;

            logger.debug("Setting build to 1903 (default starting point).");
            build = 1903;
        } else {
            logger.debug("Build is less than 5000, so incrementing build.");
            build += 3;
        }

        logger.debug("Checking revision number.");
        if (revision > 29) {
            logger.debug("Revision number is 30 or greater. Incrementing minor.");
            minor++;

            logger.debug("Setting revision to 0.");
            revision = 0;
        } else {
            logger.debug("Revision is less than 30, so incrementing revision.");
            revision++;
        }

        logger.debug("Checking minor number.");
        if (minor > 9) {
            logger.debug("Minor is greater than 9. Incrementing major.");
            major++;

            logger.debug("Setting minor to 0.");
            minor = 0;
        } else {
            logger.debug("Minor is less than 10, so incrementing minor");
            minor++;
        }

        logger.debug("Major increases forever, so no need to check it.\n\n\t"
                + "Calculated application version: " + major + "." + minor
                + "." + revision + " build " + build);
        props.setProperty("Application.major", String.valueOf(major));
        props.setProperty("Application.minor", String.valueOf(minor));
        props.setProperty("Application.revision", String.valueOf(revision));
        props.setProperty("Application.build", String.valueOf(build));

        logger.exit(Application.class.getName(), "calculateVersion()");
    }

    /**
     * Provides a means of retrieving the application properties for the Desktop
     * Framework. The properties are the general properties for the entire
     * application being built upon the Framework, such as application name,
     * title, version, storage locations, etc. Since other classes may need
     * access to some (or all) of this information, we have provided a way of
     * accessing that information.
     * <dl>
     * <dt>NOTE:</dt><dd>Though the application-wide properties may be accessed
     * by other classes contained in the project, it is unwise to store all
     * properties from all classes within this properties file. By doing so, the
     * properties file will become unwieldy and grow exponentially by the number
     * of classes in a project. Therefore, it is <strong><em>highly</em>
     * </strong> recommended that each class have its own properties file which
     * stores only the properties for that class.<br><br>As an example of this
     * recommendation, consider the application's main frame, which may be
     * obtained from the <tt>Application</tt> class by calling 
     * `Application.getMainFrame()`. Though the window returned is the main frame
     * of the entire application, it stores its settings in its own
     * configuration file under the `${Application.app.config.folder``
     * location on the user's hard drive.</dd></dl>
     *
     * @return the application-wide properties
     */

    public static Properties getProperties() {
        logger.enter(Application.class.getName(), "getProperties()");

        logger.exit(Application.class.getName(), "getProperties()", props);
        return props;
    }

    /**
     * Provides a centralized means of storing settings for all classes in built
     * upon the Desktop Framework. For example, windows should call this method
     * from their <tt>JFrame.windowClosing()</tt> or
     * <tt>JFrame.windowClosed()</tt> event. By doing so, all settings for that
     * <tt>JFrame</tt> in which the application is interested will be saved for
     * use on next startup.
     *
     * @param propsToStore <tt>java.util.Properties</tt> object for the class
     * wishing to store their settings
     * @return <tt>true` upon successful storage; `false</tt> otherwise
     */
    public static boolean storeSettings(Properties propsToStore) {
        logger.enter(Application.class.getName(), "storeProperties(Properties)",
                propsToStore);

        logger.config("Attempting to write the properties to file.");
        File propsFile = new File(System.getProperty("user.home")
                + File.separator + "."
                + props.getProperty("Application.name").toLowerCase().replace(
                        " ", "_") + ".conf");
        try (FileOutputStream out = new FileOutputStream(propsFile);) {
            props.store(out, "Written at "
                    + props.getProperty("Application.title") + " exit.");
            logger.debug("Properties file written to: "
                    + propsFile.getAbsolutePath());

            logger.exit(Application.class.getName(), "storeProperties(Properties)",
                    true);
            return true;
        } catch (IOException ex) {
            logger.error(ex, "Storing properties file from Application.exit()");
            logger.exit(Application.class.getName(), "storeProperties(Properties)",
                    false);
            return false;
        }
    }
    
    /**
     * Gracefully shutdown the application, calls `exit(null)`. This 
     * version of exit() is convenient if the decision to exit the application
     * was not triggered by an event.
     * 
     * @see #exit(EventObject)
     */
    public final void exit() {
        exit(null);
    }
    
    public void exit(EventObject event) {
        SysExits code = SysExits.EX_OK;
        
        for (ExitListener listener : exitListeners) {
            if (!listener.canExit(event)) {
                return;
            }
        }
        
        try {
            for (ExitListener listener : exitListeners) {
                try {
                    listener.willExit(event);
                } catch (Exception e) {
                    logger.warning("ExitListener.willExit() failed.\n\nMessage: "
                            + e.getMessage() + "\n\nCause: " 
                            + e.getCause().toString());
                    code = SysExits.EX_SOFTWARE;
                }
            }
            
            shutdown();
        } catch (Exception e) {
            logger.warning("unexpected error in Application.shutdown().\n\n"
                    + "Message: " + e.getMessage() + "\n\nCause: " 
                    + e.getCause().toString());
            code = SysExits.EX_SOFTWARE;
        } finally {
            end(code);
        }
    }
    
    protected void end(SysExits status) {
        Runtime.getRuntime().exit(status.toInt());
    }

    /**
     * Returns the version of the project as a string formatted in Dewey
     * Decimal.
     *
     * @return the Dewey Decimal version of the project.
     */
    public static String version() {
        return "Version " + major + "." + minor + "." + revision + "." + build;
    }

    /**
     * Returns the comments/description of the project.
     *
     * @return the project comments/description
     */
    public static String getComments() {
        return props.getProperty("Application.comments");
    }

    /**
     * The ApplicationContext singleton for this Application.
     * 
     * @return the Application's ApplicationContext singleton
     */
    public ApplicationContext getContext() {
        return context;
    }

    /**
     * Give the Application a chance to veto an attempt to exit/quit. An
     * <tt>ExitListener`'s `canExit</tt> method should return false if 
     * there are pending decisions that the user must make before the app exits.
     * A typical <tt>ExitListener</tt> would prompt the user with a modal dialog.
     * <p>
     * The <tt>eventObject</tt> argument will be the value passed to 
     * {@link #exit(java.util.EventObject)  exit() }. It may be null.</p>
     * <p>
     * The <tt>willExit</tt> method is called after the exit has been confirmed.
     * An ExitListener that is going to perform some cleanup work should do so
     * in `willExit`.</p>
     * <p>
     * `ExitListener`s run on the event dispatching thread.</p>
     * <p>
     * <strong>parameter</strong>: event the EventObject that triggered this
     * call or null</p>
     * 
     * @see #exit(java.util.EventObject) 
     * @see #addExitListener
     * @see #removeExitListener
     */
    public interface ExitListener extends EventListener {

        boolean canExit(EventObject event);

        void willExit(EventObject event);
    }
    
    /**
     * Add an <tt>ExitListener</tt> to the list.
     * 
     * @param listener the `ExitListener`
     * 
     * @see #removeExitListener
     * @see #getExitListeners
     */
    public void addExitListener(ExitListener listener) {
        exitListeners.add(listener);
    }
    
    /**
     * Remove an <tt>ExitListener</tt> from the list.
     * 
     * @param listener the `ExitListener`
     * 
     * @see #addExitListener(com.pekinsoft.desktop.application.Application.ExitListener) 
     * @see #getExitListeners
     */
    public void removeExitListener(ExitListener listener) {
        exitListeners.remove(listener);
    }
    
    /**
     * All of the `ExitListener`s added so far.
     * 
     * @return all of the `ExitListener`s
     */
    public ExitListener[] getExitListeners() {
        int size = exitListeners.size();
        return exitListeners.toArray(new ExitListener[size]);
    }
    
    /**
     * The default <tt>Action` for quitting an application, `quit</tt> just
     * exits the application by calling `exit(e)`.
     * 
     * @param e the triggering event
     * 
     * @see #exit(java.util.EventObject) 
     */
    @Action public void quit(ActionEvent e) {
        exit(e);
    }
    
    public static synchronized <T extends Application> T getInstance(
            Class<T> applicationClass) {
        /* Special Case: the application has not been launched. We are
         * constructing the applicationClass here to get the same effect as the
         * NoApplication class serves for getInstance(). We are not launching 
         * the app, no initialize/startup/wait steps.
         */
        try {
            application = create(applicationClass);
        } catch (Exception e) {
            String msg = String.format("Could not construct %s", applicationClass);
            throw new Error(msg, e);
        }
        
        return applicationClass.cast(application);
    }
    
    /**
     * The <tt>Application` singleton, or a placeholder if `launch</tt> has
     * not yet been called.
     * <p>
     * Typically this method is only called after an Application has been launched,
     * however, in some situations, like tests, it is useful to be able to get
     * an <tt>Application</tt> object without actually launching. The <em>
     * placeholder</em> Application object provides access to an `
     * ApplicationContext} singleton and has the same semantics as launching an
     * Application defined like this:</p>
     * <pre>
     * public class PlaceholderApplication extends Application{
     *    public void startup() {}
     * }
     * 
     * Application.launch(PlaceholderApplication.class);
     * </pre>
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
        protected void startup() {}
    }
    
       //------------------------------------------------------------------\\
      //-------------- Prototype support for the View type -----------------\\
     //----------------------------------------------------------------------\\
    /* From here, we can create the functionality for showing docking windows *\
     * once we have the Swing Docking Framework (built-in with this overall   *
     * Framework library) functional in a way that make sense for using with  *
     * our View classes.                                                      *
     \************************************************************************/
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
