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
package com.pekinsoft.desktop;

import com.pekinsoft.desktop.enums.SysExits;
import com.pekinsoft.desktop.err.InvalidLoggingLevelException;
import com.pekinsoft.desktop.utils.ArgumentParser;
import com.pekinsoft.desktop.utils.Logger;
import com.pekinsoft.desktop.utils.TerminalErrorPrinter;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * An encapsulation of the PropertyChangeSupport methods based on 
 * java.beans.PropertyChangeSupport. PrpertyChangeListeners are fired on the
 * event dispatching thread.
 *
 * @author Sean Carrick &lt;sean at pekinsoft dot com&gt;
 *
 * @version 0.1.0
 * @since 0.1.0
 */
public class Application {

    //<editor-fold defaultstate="collapsed" desc="Private Member Fields">
    // Default logging level will be set to INFO, unless overridden via a command
    //+ line option.
    private static final int LEVEL = Logger.INFO;

    // The default location for the application setting file. This will need to
    //+ be overriden if you want the application name to be used. to override
    //+ this default, you can use a command line option.
    private static final String DFLT_CONFIG = System.getProperty("user.home")
            + File.separator + ".application.properties";

    // The properties object for the application class. The Desktop system, by
    //+ default, creates a `config` folder under the application folder for
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
    //+ we have decided to make the log for the {@code Application} class `private`
    //+ and force each class to have its own log.
    //+
    //+ This will not impose any additional requirements of developers using the
    //+ Desktop API, as the log will be created within the API objects that the
    //+ developers will be either extending or implementing. It will simply be
    //+ there for the developer to use.
    private static Logger log;

    private static int major;       // Major version number:          X.x.x xxxx
    private static int minor;       // Minor version number:          x.X.x xxxx
    private static int revision;    // Revision version number:       x.x.X xxxx
    private static long build;      // Build number:                  x.x.x XXXX
    
    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
        // TODO: Implement clone method.
    }
    //</editor-fold>

    // TODO: Add logging to Application class methods that are missing it.
    //<editor-fold defaultstate="collapsed" desc="Constructor(s)">
    private Application() {
        try ( InputStream in = getClass().getClassLoader().getResourceAsStream(
                "/com/pekinsoft/desktop/config/Application.properties")) {
            props.load(in);

            log = new Logger(props.getProperty("Application.app.logs.folder"),
                    Integer.parseInt(props.getProperty("Application.logging.level",
                            String.valueOf(LEVEL))));
        } catch (IOException ex) {
            TerminalErrorPrinter.printException(ex);
        }

        log.config("Application has been initialized.");
    }
    //</editor-fold>

    /**
     * The {@code startup(String[] args)} method is the starting point for any
     * project using the Desktop Framework. This method takes care of intializing
     * various items of the Framework and prepares the overall Framework for
     * use. This method should be the very first method called by any project
     * building upon the Desktop Framework.
     * 
     * @param args the command line arguments
     */
    public static void startup(String[] args) {
        log.enter(Application.class.getName(), "startup(String[])", args);

        log.info("Checking command line parameters for debugging flag.");
        ArgumentParser parser = new ArgumentParser(args);
        if (parser.isSwitchPresent("-d") || parser.isSwitchPresent("--debug")) {
            try {
                log.setLevel(Logger.DEBUG);
            } catch (InvalidLoggingLevelException ex) {
                TerminalErrorPrinter.printException(ex);
            }
        }

        log.debug("Calculating the application version.");
        if (log.getLevel() == Logger.DEBUG) {
            log.exit(Application.class.getName(), "startup(String[])");
            calculateVersion();
        }
    }

    private static void calculateVersion() {
        log.enter(Application.class.getName(), "calculateVersion()");
        major = Integer.parseInt(props.getProperty("Application.major"));
        minor = Integer.parseInt(props.getProperty("Application.minor"));
        revision = Integer.parseInt(props.getProperty("Application.revision"));
        build = Long.parseLong(props.getProperty("Application.build"));

        log.debug("Checking build number.");
        if (build > 4999) {
            log.debug("Build number is 5000 or greater. Incrementing revision.");
            revision++;

            log.debug("Setting build to 1903 (default starting point).");
            build = 1903;
        } else {
            log.debug("Build is less than 5000, so incrementing build.");
            build += 3;
        }

        log.debug("Checking revision number.");
        if (revision > 19) {
            log.debug("Revision number is 20 or greater. Incrementing minor.");
            minor++;

            log.debug("Setting revision to 0.");
            revision = 0;
        } else {
            log.debug("Revision is less than 20, so incrementing revision.");
            revision++;
        }

        log.debug("Checking minor number.");
        if (minor > 9) {
            log.debug("Minor is greater than 9. Incrementing major.");
            major++;

            log.debug("Setting minor to 0.");
            minor = 0;
        } else {
            log.debug("Minor is less than 10, so incrementing minor");
            minor++;
        }

        log.debug("Major increases forever, so no need to check it.\n\n\t"
                + "Calculated application version: " + major + "." + minor
                + "." + revision + " build " + build);
        props.setProperty("Application.major", String.valueOf(major));
        props.setProperty("Application.minor", String.valueOf(minor));
        props.setProperty("Application.revision", String.valueOf(revision));
        props.setProperty("Application.build", String.valueOf(build));

        log.exit(Application.class.getName(), "calculateVersion()");
    }
    
    protected void launch(
    )

    /**
     * Provides a means of retrieving the application properties for the 
     * Desktop Framework. The properties are the general properties for the
     * entire application being built upon the Framework, such as application
     * name, title, version, storage locations, etc. Since other classes may
     * need access to some (or all) of this information, we have provided a way
     * of accessing that information.
     * <dl>
     * <dt>NOTE:</dt><dd>Though the application-wide properties may be accessed
     * by other classes contained in the project, it is unwise to store all 
     * properties from all classes within this properties file. By doing so,
     * the properties file will become unwieldy and grow exponentially by the
     * number of classes in a project. Therefore, it is <strong><em>highly</em>
     * </strong> recommended that each class have its own properties file which
     * stores only the properties for that class.<br><br>As an example of this
     * recommendation, consider the application's main frame, which may be
     * obtained from the {@code Application} class by calling {@code 
     * Application.getMainFrame()}. Though the window returned is the main frame
     * of the entire application, it stores its settings in its own configuration
     * file under the {@code ${Application.app.config.folder}} location on the
     * user's hard drive.</dd></dl>
     * 
     * @return the application-wide properties
     */
    public static Properties getProperties() {
        log.enter(Application.class.getName(), "getProperties()");

        log.exit(Application.class.getName(), "getProperties()", props);
        return props;
    }

    /**
     * Provides a centralized means of storing settings for all classes in built
     * upon the Desktop Framework. For example, windows should call this method
     * from their {@code JFrame.windowClosing()} or {@code JFrame.windowClosed()}
     * event. By doing so, all settings for that {@code JFrame} in which the
     * application is interested will be saved for use on next startup.
     * 
     * @param propsToStore {@code java.util.Properties} object for the class
     *          wishing to store their settings
     * @return {@code true} upon successful storage; {@code false} otherwise
     */
    public static boolean storeSettings(Properties propsToStore) {
        log.enter(Application.class.getName(), "storeProperties(Properties)",
                propsToStore);

        log.config("Attempting to write the properties to file.");
        File propsFile = new File(System.getProperty("user.home")
                + File.separator + "."
                + props.getProperty("Application.name").toLowerCase().replace(
                        " ", "_") + ".conf");
        try ( FileOutputStream out = new FileOutputStream(propsFile);) {
            props.store(out, "Written at " 
                    + props.getProperty("Application.title") + " exit.");
            log.debug("Properties file written to: "
                    + propsFile.getAbsolutePath());
            
            log.exit(Application.class.getName(), "storeProperties(Properties)", 
                    true);
            return true;
        } catch (IOException ex) {
            log.error(ex, "Storing properties file from Application.exit()");
            log.exit(Application.class.getName(), "storeProperties(Properties)",
                    false);
            return false;
        }
    }

    /**
     * Provides a means to exit the application in a normalized manner. By using
     * this method to exit, we are able to provide useful meaning to the
     * underlying operating system, such as the status by which we are exiting,
     * meaning, whether or not it is a normal exit or an exit with an error.
     *
     * When exiting using this method, the application is able to store to disk
     * any properties settings that have been updated, as well as perform any
     * other necessary housekeeping tasks, such as removing any temporary files
     * that were created.
     *
     * @param exitStatus `com.pekinsoft.utilities.enums.SysExits` enumeration
     * value
     */
    public static void exit(SysExits exitStatus) {
        log.enter(props.getProperty("Application.name"), "exit", exitStatus.toInt());
        
        log.debug("Storing settings for the " + Application.class.getName()
                + " class.");
        storeSettings(props);
        
        log.debug("Calling System.exit(" + exitStatus.toInt() + ")");
        log.debug("---> Exit Status: " + exitStatus.toString());
        log.exit("com.pekinsoft.northwind.Application", "exit", exitStatus.toInt());
        System.exit(exitStatus.toInt());
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
    //</editor-fold>
}
