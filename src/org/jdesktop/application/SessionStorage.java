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
 *  Class      :   SessionStorage.java
 *  Author     :   Sean Carrick
 *  Created    :   Feb 11, 2021 @ 12:21:15 PM
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

import java.applet.Applet;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.TableColumn;

/**
 * Support for storing GUI state that persists between Application sessions.
 * <p>
 * This class simplifies the common task of saving a little bit of an
 * application's GUI "session" state when the application shuts down, and then
 * restoring that state when the application is restarted. Session state is
 * stored on a per component basis, and only for components with a
 * {@link java.awt.Component#getName name} and for which a
 * <code>SessionState.Property</code> object has been defined. SessionState
 * Properties that preserve the <code>bounds Rectangle</code> for Windows,
 * the <code>dividerLocation</code> for <code>JSliderPanes</code> and the
 * <code>selectedIndex</code> for <code>JTabbedPanes</code> are defined by 
 * default. The <code>ApplicationContext</code> {@link
 * ApplicationContext#getSessionStorage getSesssionStorage} method provides a
 * shared <code>SessionStorage</code> object.
 * <p>
 * A typical Application saves session state in its
 * {@link Application#shutdown shutdown()} method, and then restores session
 * state in {@link Application#startup startup()}:</p>
 * ```java
 * public class MyApplication extends Application {
 *     &#064;Override protected void shutdown() {
 *         getContext().getSessionStorage().<b>save</b>(mainFrame, "session.xml");
 *     }
 *     &#064;Override protected void startup() {
 *         ApplicationContext appContext = getContext();
 *         appContext.setVendorId("Sun");
 *         appContext.setApplicationId("SessionStorage1");
 *         // ... create the GUI rooted by JFrame mainFrame
 *         appContext.getSessionStorage().<b>restore</b>(mainFrame, "session.xml");
 *     }
 *     // ...
 * }
 * ```
 * <p>
 * In this example, the bounds of <code>mainFrame</code> as well the session
 * state for any of its <code>JSliderPane</code> or <code>JTabbedPane</code>
 * will be saved when the application shuts down, and restored when
 * the applications starts up again. Note: error handling has been omitted from
 * the example.</p>
 * <p>
 * Session state is stored locally, relative to the user's home directory, by
 * the <code>LocalStorage LocalStorage#save save</code> and
 * {@link LocalStorage#save load} methods. The <code>startup</code> method must set
 * the <code>ApplicationContext vendorId</code> and <code>applicationId</code>
 * properties to ensure that the correct
 * {@link LocalStorage#getDirectory local directory} is selected on all
 * platforms. For example, on Windows XP, the full pathname for filename
 * <code>"session.xml"</code> is typically:</p>
 * <pre>
 * ${userHome}\Application Data\${vendorId}\${applicationId}\session.xml
 * </pre><p>
 * Where the value of <code>${userHome}</code> is the the value of the Java
 * System property <code>"user.home"</code>. On Solaris or Linux the file is:</p>
 * <pre>
 * ${userHome}/.${applicationId}/session.xml
 * </pre><p>
 * and on OSX:</p>
 * <pre>
 * ${userHome}/Library/Application Support/${applicationId}/session.xml
 * </pre>
 * 
 * @author Hans Muller (Original Author) &lt;current email unknown&gt;
 * @author Sean Carrick (Updater) &lt;sean at pekinsoft dot com&gt;
 * 
 * @version 1.05
 * @since 1.03
 *
 * @see ApplicationContext#getSessionStorage() 
 * @see LocalStorage
 */
public class SessionStorage {

    private static final Logger logger = Logger.getLogger(
            SessionStorage.class.getName());
    private final Map<Class, Property> propertyMap;
    private final ApplicationContext context;

    /**
     * Constructs a SessionStorage object. The following {@link
     * Property Property} objects are registered by default:
     * <p>
     * <table border="1">
     * <caption><code>SessionStorage Property</code> Objects</caption>
     * <tr>
     * <th>Base Component Type</th>
     * <th><code>sessionState</code> Property</th>
     * <th><code>sessionState</code> Property Value</th>
     * </tr>
     * <tr>
     * <td>Window</td>
     * <td>WindowProperty</td>
     * <td>WindowState</td>
     * </tr>
     * <tr>
     * <td>JTabbedPane</td>
     * <td>TabbedPaneProperty</td>
     * <td>TabbedPaneState</td>
     * </tr>
     * <tr>
     * <td>JSplitPane</td>
     * <td>SplitPaneProperty</td>
     * <td>SplitPaneState</td>
     * </tr>
     * <tr>
     * <td>JTable</td>
     * <td>TableProperty</td>
     * <td>TableState</td>
     * </tr>
     * </table>
     * <p>
     * Applications typically would not create a <code>SessionStorage</code> object
     * directly, they'd use the shared ApplicationContext value:<p>
     * ```java
     * ApplicationContext ctx = Application.getInstance(MyApplication.class).getContext();
     * SessionStorage ss = ctx.getSesssionStorage();
     * ```
     *
     * @param context the <code>ApplicationContext</code> for this 
     *          <code>SessionStorage</code> object.
     *
     * @see ApplicationContext#getSessionStorage() 
     * @see #getProperty(java.lang.Class) 
     * @see #getProperty(java.awt.Component) 
     */
    protected SessionStorage(ApplicationContext context) {
        if (context == null) {
            throw new IllegalArgumentException("null context");
        }
        this.context = context;
        propertyMap = new HashMap<>();
        propertyMap.put(Window.class, new WindowProperty());
        propertyMap.put(JTabbedPane.class, new TabbedPaneProperty());
        propertyMap.put(JSplitPane.class, new SplitPaneProperty());
        propertyMap.put(JTable.class, new TableProperty());
    }

    /* Acted on the FIX-ME comment from the original and added JavaDoc */
    /**
     * Retrieves the <code>ApplicationContext</code> for this `SessionStorage`
     * object. Since session states are stored per application, the
     * <code>ApplicationContext</code> determines where the `"session.xml"`
     * file is stored on the local system.
     *
     * @see ApplicationContext
     *
     * @return the context of this session's storage facility
     */
    protected final ApplicationContext getContext() {
        return context;
    }

    /* Simply checks the provided parameter and throws IllegalArgumentException
     * in the event either of them are null. No other processing takes place. */
    private void checkSaveRestoreArgs(Component root, String fileName) {
        if (root == null) {
            throw new IllegalArgumentException("null root");
        }
        if (fileName == null) {
            throw new IllegalArgumentException("null fileName");
        }
    }

    /* TODO: maybe...
     * At some point we may replace this with a more complex scheme.
     *
     * Added by Sean Carrick on Feb 11, 2021: Why do/would we want to make it 
     *                                        more complex?
     */
    private String getComponentName(Component c) {
        return c.getName();
    }

    /* Return a string that uniquely identifies this component, or null
     * if Component c doesn't have a name per getComponentName().  The
     * pathname is basically the name of all of the components, starting 
     * with c, separated by "/".  This path is the reverse of what's 
     * typical, the first path element is c's name, rather than the name
     * of c's root Window or Applet.  That way pathnames can be 
     * distinguished without comparing much of the string.  The names
     * of intermediate components *can* be null, we substitute 
     * "[type][z-order]" for the name.  Here's an example:
     * 
     * JFrame myFrame = new JFrame();
     * JPanel p = new JPanel() {};  // anonymous JPanel subclass
     * JButton myButton = new JButton();   
     * myButton.setName("myButton");
     * p.add(myButton);
     * myFrame.add(p);
     * 
     * getComponentPathname(myButton) => 
     * "myButton/AnonymousJPanel0/null.contentPane/null.layeredPane/JRootPane0/myFrame"
     * 
     * Notes about name usage in AWT/Swing: JRootPane (inexplicably) assigns 
     * names to it's children (layeredPane, contentPane, glassPane); 
     * all AWT components lazily compute a name.  If we hadn't assigned the
     * JFrame a name, it's name would have been "frame0".
     */
    private String getComponentPathname(Component c) {
        String name = getComponentName(c);
        if (name == null) {
            return null;
        }
        StringBuilder path = new StringBuilder(name);
        while ((c.getParent() != null) && !(c instanceof Window) 
                && !(c instanceof Applet)) {
            c = c.getParent();
            name = getComponentName(c);
            if (name == null) {
                int n = c.getParent().getComponentZOrder(c);
                if (n >= 0) {
                    Class cls = c.getClass();
                    name = cls.getSimpleName();
                    if (name.length() == 0) {
                        name = "Anonymous" + cls.getSuperclass().getSimpleName();
                    }
                    name = name + n;
                } else {
                    // Implies that the component tree is changing
                    // while we're computing the path. Punt.
                    logger.log(Level.WARNING, "Couldn''t compute pathname "
                            + "for {0}", c);
                    return null;
                }
            }
            path.append("/").append(name);
        }
        return path.toString();
    }

    /* Recursively walk the component tree, breadth first, storing the
     * state - Property.getSessionState() - of named components under 
     * their pathname (the key) in stateMap.
     * 
     * Note: the breadth first tree-walking code here should remain 
     * structurally identical to restoreTree().
     */
    private void saveTree(List<Component> roots, Map<String, Object> stateMap) {
        List<Component> allChildren = new ArrayList<>();
        roots.stream().map(root -> {
            if (root != null) {
                Property p = getProperty(root);
                if (p != null) {
                    String pathname = getComponentPathname(root);
                    if (pathname != null) {
                        Object state = p.getSessionState(root);
                        if (state != null) {
                            stateMap.put(pathname, state);
                        }
                    }
                }
            }
            return root;
        }).filter(root -> (root instanceof Container)).map(root -> 
                ((Container) root).getComponents()).filter(children -> 
                        ((children != null) 
                        && (children.length > 0))).forEachOrdered(children -> {
            Collections.addAll(allChildren, children);
        });
        if (allChildren.size() > 0) {
            saveTree(allChildren, stateMap);
        }
    }

    /**
     * Saves the state of each named component in the specified hierarchy to a
     * file using {@link LocalStorage#save LocalStorage.save(fileName)}. Each
     * component is visited in breadth-first order: if a <code>Property</code>
     * {@link #getProperty(Component) exists} for that component, and the
     * component has a {@link java.awt.Component#getName name}, then its
     * {@link Property#getSessionState state} is saved.
     * <p>
     * Component names can be any string however they must be unique relative to
     * the name's of the component's siblings. Most Swing components do not have
     * a name by default, however there are some exceptions: JRootPane
     * (inexplicably) assigns names to it's children (layeredPane, contentPane,
     * glassPane); and all AWT components lazily compute a name, so JFrame,
     * JDialog, and JWindow also have a name by default.</p>
     * <p>
     * The type of sessionState values (i.e. the type of values returned by
     * <code>Property.getSessionState</code>) must be one those supported by
     * {@link java.beans.XMLEncoder XMLEncoder} and
     * {@link java.beans.XMLDecoder XMLDecoder}, for example beans (null
     * constructor, read/write properties), primitives, and Collections. Java
     * bean classes and their properties must be public. Typically beans defined
     * for this purpose are little more than a handful of simple properties. The
     * JDK 6 &#064;ConstructorProperties annotation can be used to eliminate the
     * need for writing set methods in such beans, e.g.</p>
     * ```java
     * public class FooBar {
     *     private String foo, bar;
     *     // Defines the mapping from constructor params to properties
     *     &#064;ConstructorProperties({"foo", "bar"})
     *     public FooBar(String foo, String bar) {
     *         this.foo = foo;
     *         this.bar = bar;
     *     }
     *     public String getFoo() { return foo; }  // don't need setFoo
     *     public String getBar() { return bar; }  // don't need setBar
     * }
     * ```
     *
     * @param root the root of the Component hierarchy to be saved.
     * @param fileName the <code>LocalStorage</code> filename.
     * @throws IOException in the event there is an error accessing the 
     *          filesystem
     * 
     * @see #restore(java.awt.Component, java.lang.String) 
     * @see ApplicationContext#getLocalStorage() 
     * @see LocalStorage#save(java.lang.Object, java.lang.String) 
     * @see #getProperty(java.awt.Component) 
     */
    public void save(Component root, String fileName) throws IOException {
        checkSaveRestoreArgs(root, fileName);
        Map<String, Object> stateMap = new HashMap<>();
        saveTree(Collections.singletonList(root), stateMap);
        LocalStorage lst = getContext().getLocalStorage();
        lst.save(stateMap, fileName);
    }

    /* Recursively walk the component tree, breadth first, restoring the
     * state - Property.setSessionState() - of named components for which 
     * there's a non-null entry under the component's pathName in 
     * stateMap.
     * 
     * Note: the breadth first tree-walking code here should remain 
     * structurally identical to saveTree().
     */
    private void restoreTree(List<Component> roots, Map<String, Object> stateMap) {
        List<Component> allChildren = new ArrayList<>();
        roots.stream().map((Component root) -> {
            if (root != null) {
                Property p = getProperty(root);
                if (p != null) {
                    String pathname = getComponentPathname(root);
                    if (pathname != null) {
                        Object state = stateMap.get(pathname);
                        if (state != null) {
                            p.setSessionState(root, state);
                        } else {
                            logger.log(Level.WARNING, "No saved state for {0}",
                                    root);
                        }
                    }
                }
            }
            return root;
        }).filter(root -> (root instanceof Container)).map(root -> 
                ((Container) root).getComponents()).filter(children -> 
                        ((children != null)
                        && (children.length > 0))).forEachOrdered(children -> {
            Collections.addAll(allChildren, children);
        });
        if (allChildren.size() > 0) {
            restoreTree(allChildren, stateMap);
        }
    }

    /**
     * Restores each named component in the specified hierarchy from the session
     * state loaded from a file using
     * {@link LocalStorage#save LocalStorage.load(fileName)}. Each component is
     * visited in breadth-first order: if a
     * {@link #getProperty(Component) Property} exists for that component, and
     * the component has a {@link java.awt.Component#getName name}, then its
     * state is {@link Property#setSessionState restored}.
     *
     * @param root the root of the Component hierarchy to be restored.
     * @param fileName the <code>LocalStorage</code> filename.
     * @throws IOException in the event an error occurs accessing the filesystem
     * 
     * @see #save(java.awt.Component, java.lang.String) 
     * @see ApplicationContext#getLocalStorage() 
     * @see LocalStorage#save(java.lang.Object, java.lang.String) 
     * @see #getProperty(java.awt.Component) 
     */
    public void restore(Component root, String fileName) throws IOException {
        checkSaveRestoreArgs(root, fileName);
        LocalStorage lst = getContext().getLocalStorage();
        Map<String, Object> stateMap = (Map<String, Object>) (lst.load(fileName));
        if (stateMap != null) {
            restoreTree(Collections.singletonList(root), stateMap);
        }
    }

    /**
     * Defines the <code>sessionState</code> property. The value of this property is
     * the GUI state that should be preserved across sessions for the specified
     * component. The type of sessionState values just one those supported by
     * {@link java.beans.XMLEncoder XMLEncoder} and
     * {@link java.beans.XMLDecoder XMLDecoder}, for example beans (null
     * constructor, read/write properties), primitives, and Collections.
     *
     * @see #putProperty
     * @see #getProperty(Class)
     * @see #getProperty(Component)
     */
    public interface Property {

        /**
         * Return the value of the <code>sessionState</code> property, typically a
         * Java bean or a Collection the defines the <code>Component</code> state
         * that should be preserved across Application sessions. This value will
         * be stored with {@link java.beans.XMLEncoder XMLEncoder}, loaded with
         * {@link java.beans.XMLDecoder XMLDecoder}, and passed to
         * <code>setSessionState</code> to restore the Component's state.
         *
         * @param c the Component.
         * @return the <code>sessionState</code> object for Component <code>c</code>.
         * @see #setSessionState
         */
        Object getSessionState(Component c);

        /**
         * Restore Component <code>c</code>'s <code>sessionState</code> from the
         * specified object.
         *
         * @param c the Component.
         * @param state the value of the <code>sessionState</code> property.
         * @see #getSessionState(java.awt.Component) 
         */
        void setSessionState(Component c, Object state);
    }

    /**
     * This Java Bean defines the <code>Window</code> state preserved across
     * sessions: the Window's <code>bounds</code>, and the bounds of the Window's
     * <code>GraphicsConfiguration</code>, i.e. the bounds of the screen that the
     * Window appears on. If the Window is actually a Frame, we also store its
     * extendedState. <code>WindowState</code> objects are stored and restored by the
     * {@link WindowProperty WindowProperty} class.
     *
     * @see WindowProperty
     * @see #save
     * @see #restore
     */
    public static class WindowState {

        private final Rectangle bounds;
        private Rectangle gcBounds = null;
        private int screenCount;
        private int frameState = Frame.NORMAL;

        /**
         * Constructs a default <code>WindowState</code> object. When constructed,
         * the instance object will have only a default undefined <code>bounds
         * </code> assigned to it. At that point, the developer will need to 
         * call the individual setter methods to make this <code>WindowState
         * </code> object usable.
         * 
         * @see java.awt.Frame
         * @see java.awt.GraphicsConfiguration
         * @see java.awt.GraphicsDevice
         * @see java.awt.GraphicsEnvironment
         * @see WindowState#setBounds(java.awt.Rectangle) 
         * @see WindowState#setFrameState(int) 
         * @see WindowState#setGraphicsConfigurationBounds(java.awt.Rectangle) 
         * @see WindowState#setScreenCount(int) 
         */
        public WindowState() {
            bounds = new Rectangle();
        }

        /**
         * Constructs a <code>WindowState</code> object with the specified
         * property values.
         * 
         * @param bounds the window bounds
         * @param gcBounds the <code>GraphicsConfiguration</code> bounds in the
         *          device coordinates. In a multiscreen environment with a 
         *          virtual device, the bounds can have negative X or Y origins.
         * @param screenCount the number of usable screens
         * @param frameState the state of the window. These correlate to the
         *          fields of the {@link java.awt.Frame Frame} class:
         *          <ul><li><code>NORMAL</code>: Not maximized</li>
         *              <li><code>MAXIMIZED_BOTH</code>: Fully maximized</li>
         *              <li><code>MAXIMIZED_HORIZ</code>: Maximized horizontally
         *                  only</li>
         *              <li><code>MAXIMIZED_VERT</code>: Maximized vertically
         *                  only</li>
         *          </ul>
         * 
         * @see java.awt.Frame
         * @see java.awt.GraphicsConfiguration
         * @see java.awt.GraphicsDevice
         * @see java.awt.GraphicsEnvironment
         * @see WindowState#getBounds() 
         * @see WindowState#setBounds(java.awt.Rectangle) 
         * @see WindowState#getFrameState() 
         * @see WindowState#setFrameState(int) 
         * @see WindowState#getGraphicsConfigurationBounds() 
         * @see WindowState#setGraphicsConfigurationBounds(java.awt.Rectangle) 
         * @see WindowState#getScreenCount() 
         * @see WindowState#setScreenCount(int) 
         */
        public WindowState(Rectangle bounds, Rectangle gcBounds, int screenCount, 
                int frameState) {
            if (bounds == null) {
                throw new IllegalArgumentException("null bounds");
            }
            if (screenCount < 1) {
                throw new IllegalArgumentException("invalid screenCount");
            }
            this.bounds = bounds;
            this.gcBounds = gcBounds;  // can be null
            this.screenCount = screenCount;
            this.frameState = frameState;
        }

        /**
         * Gets the bounds of this component in the form of a <code>Rectangle
         * </code> object. The bounds specify this component's width, height,
         * and location relative to its parent.
         * 
         * @return a <code>Rectangle</code> indicating this window's bounds
         * 
         * @see java.awt.Component#getBounds() 
         * @see java.awt.Rectangle
         * @see #setBounds(java.awt.Rectangle) 
         */
        public Rectangle getBounds() {
            return new Rectangle(bounds);
        }

        /**
         * Moves and resizes this component to conform to the new bounding 
         * rectangle <code>bounds</code>. This component's new position is
         * specified by <code>bounds.x</code> and <code>bounds.y</code>, and its
         * new size is specified by <code>bounds.width</code> and <code>
         * bounds.height</code>.
         * <p>
         * This method changes layout-related information, and therefore
         * invalidates the component hierarchy.</p>
         * 
         * @param bounds the new bounding rectangle for this window
         * 
         * @see java.awt.Component#setBounds(java.awt.Rectangle) 
         * @see java.awt.Rectangle
         * @see #getBounds() 
         */
        public void setBounds(Rectangle bounds) {
            this.bounds.setBounds(bounds);
        }
        
        /*
        ************************************************************************
        * The following method was added as a matter of convenience for the end*
        * user, as well as to better conform to the standard set by the        *
        * java.awt.Component.                                                  *
        *                                                                      *
        *                                        -> Sean Carrick, Feb 11, 2021 *
        ************************************************************************
        */
        /**
         * Moves and resizes this component. The new location of the top-left
         * corner is specified by <code>x</code> and <code>y</code>, and the new
         * size is specified by <code>width</code> and <code>height</code>.
         * <p>
         * This method changes the layout-related information, and therefore,
         * invalidates the component hierarchy.</p>
         * 
         * @param x the new <em>x</em>-coordinate of the window
         * @param y the new <em>y</em>-coordinate of the window
         * @param width the new <code>width</code> of the window
         * @param height the new <code>height</code> of the window
         * 
         * @see java.awt.Component#setBounds(int, int, int, int) 
         * @see #setBounds(java.awt.Rectangle) 
         * @see #getBounds() 
         */
        public void setBounds(int x, int y, int width, int height) {
            Rectangle bounds = new Rectangle(x, y, width, height);
            this.bounds.setBounds(bounds);
        }
        
        /**
         * Returns the number of screens available.
         * 
         * @return the number of available screens
         * 
         * @see java.awt.GraphicsEnvironment#getScreenDevices() 
         * @see #setScreenCount(int) 
         */
        public int getScreenCount() {
            return screenCount;
        }

        /*
        ************************************************************************
        * TODO: Find out why we are allowing to set the screen count, when this*
        * is a property we get from the java.awt.GraphicsEnvironment, which    *
        * does not provide for setting the number of screens.                  *
        *                                                                      *
        *                                        -> Sean Carrick, Feb 11, 2021 *
        ************************************************************************
        */
        /**
         * Sets the number of screens available
         * 
         * @param screenCount the number of available screens
         * 
         * @see #getScreenCount() 
         */
        public void setScreenCount(int screenCount) {
            this.screenCount = screenCount;
        }

        /**
         * Gets the state of the window. The state is represented as a bitwise
         * mask.
         * <ul>
         * <li><code>NORMAL</code>: Indicates that no state bits are set.</li>
         * <li><code>ICONIFIED</code></li>
         * <li><code>MAXIMIZED_VERT</code></li>
         * <li><code>MAXIMIZED_HORIZ</code></li>
         * <li><code>MAXIMIZED_BOTH</code>: concatenates <code>MAXIMIZED_HORIZ
         *      </code> and <code>MAXIMIZED_VERT</code></li>
         * </ul>
         * 
         * @return an int of the frame state constants
         * 
         * @see java.awt.Frame#getExtendedState() 
         * @see #setFrameState(int) 
         */
        public int getFrameState() {
            return frameState;
        }

        /**
         * Sets the state of the window. The state is represented as a bitwise
         * mask.
         * <ul>
         * <li><code>NORMAL</code>: Indicates that no state bits are set.</li>
         * <li><code>ICONIFIED</code></li>
         * <li><code>MAXIMIZED_VERT</code></li>
         * <li><code>MAXIMIZED_HORIZ</code></li>
         * <li><code>MAXIMIZED_BOTH</code>: concatenates <code>MAXIMIZED_HORIZ
         *      </code> and <code>MAXIMIZED_VERT</code></li>
         * </ul>
         * 
         * @param frameState a bitwise mask for of the frame state constants
         * 
         * @see java.awt.Frame#setExtendedState(int) 
         * @see #getFrameState() 
         */
        public void setFrameState(int frameState) {
            this.frameState = frameState;
        }

       /**
        * Returns the bounds of the <code>GraphicsConfiguration</code> in the
        * device coordinates. In a multi-screen environment with a virtual device,
        * the bounds can have negative X or Y origins.
        * 
        * @return the bounds of the area covered by the 
        *           <code>GraphicsConfiguration</code>
        * 
        * @see java.awt.GraphicsConfiguration#getBounds() 
        * @see #setGraphicsConfigurationBounds(java.awt.Rectangle) 
        */
        public Rectangle getGraphicsConfigurationBounds() {
            return (gcBounds == null) ? null : new Rectangle(gcBounds);
        }

        /*
        ************************************************************************
        * TODO: Find out why we are allowing to set the bounds of the          *
        * GraphicsConfiguration, when this is a property we get from the       *
        * java.awt.GraphicsEnvironment, which does not provide for setting the *
        * GraphicsConfiguration bounds.                                        *
        *                                                                      *
        *                                        -> Sean Carrick, Feb 11, 2021 *
        ************************************************************************
        */
        /**
         * Sets the bounds of the <code>GraphicsConfiguration</code> for the
         * window.
         * 
         * @param gcBounds a <code>Rectangle</code> representing the graphics
         *          bounds of the window in screen coordinates
         * 
         * @see java.awt.GraphicsConfiguration
         * @see #getGraphicsConfigurationBounds() 
         */
        public void setGraphicsConfigurationBounds(Rectangle gcBounds) {
            this.gcBounds = (gcBounds == null) ? null : new Rectangle(gcBounds);
        }
    }

    /**
     * A <code>sessionState</code> property for Window.
     * <p>
     * This class defines how the session state for <code>Windows</code> is
     * {@link WindowProperty#getSessionState saved} and and
     * {@link WindowProperty#setSessionState restored} in terms of a property
     * called <code>sessionState</code>. The Window's <code>bounds Rectangle</code> is
     * saved and restored if the dimensions of the Window's screen have not
     * changed.
     * <p>
     * <code>WindowProperty</code> is registered for <code>Window.class</code> by default,
     * so this class applies to the AWT <code>Window}, {@code Dialog</code>, and
     * <code>Frame</code> class, as well as their Swing counterparts:
     * <code>JWindow</code>, <code>JDialog</code>, and <code>JFrame</code>.
     *
     * @see #save(java.awt.Component, java.lang.String) 
     * @see #restore(java.awt.Component, java.lang.String) 
     * @see WindowState
     */
    public static class WindowProperty implements Property {

        private void checkComponent(Component component) {
            if (component == null) {
                throw new IllegalArgumentException("null component");
            }
            if (!(component instanceof Window)) {
                throw new IllegalArgumentException("invalid component");
            }
        }

        private int getScreenCount() {
            return GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices().length;
        }

        /**
         * Returns a {@link WindowState WindowState} object for
         * <code>Window c</code>.
         * <p>
         * Throws an <code>IllegalArgumentException</code> if <code>Component c</code>
         * isn't a non-null <code>Window</code>.
         *
         * @param c the <code>Window</code> whose bounds will be stored in a
         * <code>WindowState</code> object.
         * @return the <code>WindowState</code> object
         * 
         * @see #setSessionState(java.awt.Component, java.lang.Object) 
         * @see WindowState
         */
        @Override
        public Object getSessionState(Component c) {
            checkComponent(c);
            int frameState = Frame.NORMAL;
            if (c instanceof Frame) {
                frameState = ((Frame) c).getExtendedState();
            }
            GraphicsConfiguration gc = c.getGraphicsConfiguration();
            Rectangle gcBounds = (gc == null) ? null : gc.getBounds();
            Rectangle frameBounds = c.getBounds();
            /* If this is a JFrame created by FrameView and it's been maximized,
             * retrieve the frame's normal (not maximized) bounds.  More info:
             * see FrameStateListener#windowStateChanged in FrameView.
             */
            if ((c instanceof JFrame) && (0 != (frameState & Frame.MAXIMIZED_BOTH))) {
                String clientPropertyKey = "WindowState.normalBounds";
                Object r = ((JFrame) c).getRootPane().getClientProperty(clientPropertyKey);
                if (r instanceof Rectangle) {
                    frameBounds = (Rectangle) r;
                }
            }
            return new WindowState(frameBounds, gcBounds, getScreenCount(), frameState);
        }

        /**
         * Restore the <code>Window's</code> bounds if the dimensions of its screen
         * (<code>GraphicsConfiguration</code>) haven't changed, the number of
         * screens hasn't changed, and the
         * {@link Window#isLocationByPlatform isLocationByPlatform} property,
         * which indicates that native Window manager should pick the Window's
         * location, is false. More precisely:
         * <p>
         * If <code>state,</code> is non-null, and Window <code>c's</code>
         * <code>GraphicsConfiguration</code>
         * {@link GraphicsConfiguration#getBounds bounds} matches the
         * {@link WindowState#getGraphicsConfigurationBounds WindowState's value},
         * and Window <code>c's</code>
         * {@link Window#isLocationByPlatform isLocationByPlatform} property is
         * false, then set the Window's to the
         * {@link WindowState#getBounds saved value}.
         * <p>
         * Throws an <code>IllegalArgumentException</code> if <code>c</code> is not a
         * <code>Window</code> or if <code>state</code> is non-null but not an instance of
         * {@link WindowState}.
         *
         * @param c the Window whose state is to be restored
         * @param state the <code>WindowState</code> to be restored
         * 
         * @see #getSessionState
         * @see WindowState
         */
        @Override
        public void setSessionState(Component c, Object state) {
            checkComponent(c);
            if ((state != null) && !(state instanceof WindowState)) {
                throw new IllegalArgumentException("invalid state");
            }
            Window w = (Window) c;
            if (!w.isLocationByPlatform() && (state != null)) {
                WindowState windowState = (WindowState) state;
                Rectangle gcBounds0 = windowState.getGraphicsConfigurationBounds();
                int sc0 = windowState.getScreenCount();
                GraphicsConfiguration gc = c.getGraphicsConfiguration();
                Rectangle gcBounds1 = (gc == null) ? null : gc.getBounds();
                int sc1 = getScreenCount();
                if ((gcBounds0 != null) && (gcBounds0.equals(gcBounds1)) && (sc0 == sc1)) {
                    boolean resizable = true;
                    if (w instanceof Frame) {
                        resizable = ((Frame) w).isResizable();
                    } else if (w instanceof Dialog) {
                        resizable = ((Dialog) w).isResizable();
                    }
                    if (resizable) {
                        w.setBounds(windowState.getBounds());
                    }
                }
                if (w instanceof Frame) {
                    ((Frame) w).setExtendedState(windowState.getFrameState());
                }
            }
        }
    }

    /**
     * This Java Bean records the <code>selectedIndex</code> and {@code
     * tabCount} properties of a <code>JTabbedPane</code>. A 
     * <code>TabbedPaneState</code> object created by {@link
     * TabbedPaneProperty#getSessionState} and used to restore the selected tab
     * by {@link TabbedPaneProperty#setSessionState}.
     *
     * @see TabbedPaneProperty
     * @see #save(java.awt.Component, java.lang.String) 
     * @see #restore(java.awt.Component, java.lang.String) 
     */
    public static class TabbedPaneState {

        private int selectedIndex;
        private int tabCount;

        /**
         * Constructs a default <code>TabbedPaneState</code> object with no tabs
         * and the <code>sectedIndex</code> property set to <code>-1</code.
         * 
         * @see #setSelectedIndex(int) 
         * @see #setTabCount(int) 
         * @see TabbedPaneState#TabbedPaneState(int, int) 
         * @see TabbedPaneState
         * @see javax.swing.JTabbedPane
         */
        public TabbedPaneState() {
            selectedIndex = -1;
            tabCount = 0;
        }

        /**
         * Constructs a <code>TabbedPaneState</code> object with the specified
         * number of tabs and the specified tab (<code>selectedIndex</code>)
         * selected.
         * <p>
         * Throws an <code>IllegalArgumentException</code> in the event that the
         * <code>tabCount</code> is less than zero (0); or, if the <code>
         * selectedIndex</code> is less than negative one (-1), or greater than
         * the number of tabs specified by <code>tabCount</code>.</p>
         * 
         * @param selectedIndex the index of the tab to be selected
         * @param tabCount the total number of tabs
         * 
         * @see #getSelectedIndex() 
         * @see #getTabCount() 
         * @see javax.swing.JTabbedPane#getSelectedIndex() 
         * @see javax.swing.JTabbedPane#getTabCount() 
         */
        public TabbedPaneState(int selectedIndex, int tabCount) {
            if (tabCount < 0) {
                throw new IllegalArgumentException("invalid tabCount");
            }
            if ((selectedIndex < -1) || (selectedIndex > tabCount)) {
                throw new IllegalArgumentException("invalid selectedIndex");
            }
            this.selectedIndex = selectedIndex;
            this.tabCount = tabCount;
        }

        /**
         * Returns the index of the currently selected tab. Returns <code>-1</code>
         * if there is no currently selected tab.
         * 
         * @return the selected tab index
         * 
         * @see #setSelectedIndex(int)
         * @see #getTabCount() 
         * @see #setTabCount(int) 
         */
        public int getSelectedIndex() {
            return selectedIndex;
        }

        /**
         * Sets the selected index for the tabbed pane. The index must be a valid
         * tab index or <code>-1</code>, which indicates that no tab should be
         * selected (can also be used when there are no tabs in the tabbed pane).
         * If a <code>-1</code> value is specified when the tabbed pane contains
         * one or more tabs, then the results will be implementation defined.
         * <p>
         * Throws an <code>IllegalArgumentException</code> if the value of <code>
         * selectedIndex</code> is less that negative one (<code>&lt;-1</code>).
         * </p>
         * 
         * @param selectedIndex the index to be selected, or <code>-1</code> to
         *          select no tab at all
         * 
         * @see #getSelectedIndex() 
         * @see #getTabCount() 
         * @see #setTabCount(int) 
         */
        public void setSelectedIndex(int selectedIndex) {
            if (selectedIndex < -1) {
                throw new IllegalArgumentException("invalid selectedIndex");
            }
            this.selectedIndex = selectedIndex;
        }

        /**
         * Returns the total number of tabs contained in the tabbed pane.
         * 
         * @return an integer specifying the number of tabbed pages
         * 
         * @see #getSelectedIndex() 
         * @see #setSelectedIndex(int) 
         * @see #setTabCount(int) 
         */
        public int getTabCount() {
            return tabCount;
        }

        /*
        ************************************************************************
        * TODO: Figure out why we are providing a setTabCount method when the  *
        *       underlying JTabbedPane does not provide such a method.         *
        *                                                                      *
        *                                        -> Sean Carrick, Feb 11, 2021 *
        ************************************************************************
        */
        /**
         * Sets the number of tabs contained in the tabbed pane.
         * 
         * @param tabCount the number of tabs in the tabbed pane
         * 
         * @see #getSelectedIndex() 
         * @see #setSelectedIndex(int) 
         * @see #getTabCount() 
         */
        public void setTabCount(int tabCount) {
            if (tabCount < 0) {
                throw new IllegalArgumentException("invalid tabCount");
            }
            this.tabCount = tabCount;
        }
    }

    /**
     * A <code>sessionState</code> property for JTabbedPane.
     * <p>
     * This class defines how the session state for <code>JTabbedPanes</code> is
     * {@link WindowProperty#getSessionState saved} and and
     * {@link WindowProperty#setSessionState restored} in terms of a property
     * called <code>sessionState</code>. The JTabbedPane's <code>selectedIndex</code> is
     * saved and restored if the number of tabs (<code>tabCount</code>) hasn't
     * changed.
     * <p>
     * <code>TabbedPaneProperty</code> is registered for <code>JTabbedPane.class
     * </code> by default, so this class applies to JTabbedPane and
     * any subclass of JTabbedPane. One can override the default with the
     * {@link #putProperty putProperty} method.
     *
     * @see TabbedPaneState
     * @see #save
     * @see #restore
     */
    public static class TabbedPaneProperty implements Property {

        private void checkComponent(Component component) {
            if (component == null) {
                throw new IllegalArgumentException("null component");
            }
            if (!(component instanceof JTabbedPane)) {
                throw new IllegalArgumentException("invalid component");
            }
        }

        /**
         * Returns a {@link TabbedPaneState TabbedPaneState} object for
         * <code>JTabbedPane c</code>.
         * <p>
         * Throws an <code>IllegalArgumentException</code> if <code>Component c</code>
         * isn't a non-null <code>JTabbedPane</code>.
         *
         * @param c the <code>JTabbedPane</code> whose selectedIndex will be
         * recorded in a <code>TabbedPaneState</code> object.
         * 
         * @return the <code>TabbedPaneState</code> object
         * @see #setSessionState
         * @see TabbedPaneState
         */
        @Override
        public Object getSessionState(Component c) {
            checkComponent(c);
            JTabbedPane p = (JTabbedPane) c;
            return new TabbedPaneState(p.getSelectedIndex(), p.getTabCount());
        }

        /**
         * Restore the <code>JTabbedPane</code>'s <code>selectedIndex</code> property if
         * the number of {@link JTabbedPane#getTabCount tabs} has not changed.
         * <p>
         * Throws an <code>IllegalArgumentException</code> if <code>c</code> is not a
         * <code>JTabbedPane</code> or if <code>state</code> is non-null but not an
         * instance of {@link TabbedPaneState}.
         *
         * @param c the JTabbedPane whose state is to be restored
         * @param state the <code>TabbedPaneState</code> to be restored
         * @see #getSessionState
         * @see TabbedPaneState
         */
        @Override
        public void setSessionState(Component c, Object state) {
            checkComponent(c);
            if ((state != null) && !(state instanceof TabbedPaneState)) {
                throw new IllegalArgumentException("invalid state");
            }
            JTabbedPane p = (JTabbedPane) c;
            TabbedPaneState tps = (TabbedPaneState) state;
            if (p.getTabCount() == tps.getTabCount()) {
                p.setSelectedIndex(tps.getSelectedIndex());
            }
        }
    }

    /**
     * This Java Bean records the <code>dividerLocation</code> and {@code
     * orientation} properties of a <code>JSplitPane</code>. A {@code
     * SplitPaneState} object created by {@link
     * SplitPaneProperty#getSessionState} and used to restore the selected tab
     * by {@link SplitPaneProperty#setSessionState}.
     *
     * @see SplitPaneProperty
     * @see #save
     * @see #restore
     */
    public static class SplitPaneState {

        private int dividerLocation = -1;
        private int orientation = JSplitPane.HORIZONTAL_SPLIT;

        private void checkOrientation(int orientation) {
            if ((orientation != JSplitPane.HORIZONTAL_SPLIT)
                    && (orientation != JSplitPane.VERTICAL_SPLIT)) {
                throw new IllegalArgumentException("invalid orientation");
            }
        }

        /**
         * Constructs a new <code>SplitPaneState</code> object with the specified
         * orientation and divider location.
         * 
         * @param dividerLocation the initial position of the pane divider
         * @param orientation the orientation of the pane splits. Equivalent to:
         *          <ul>
         *              <li><code>JSplitPane.HORIZONTAL_SPLIT</code>: divider
         *                  is represented vertically, thereby effectively
         *                  splitting the pane into two panels horizontally.</li>
         *              <li><code>JSplitPane.VERTICAL_SPLIT</code>: divider is
         *                  represented horizontally, thereby effectively 
         *                  splitting the pane into two panels vertically.</li>
         *          </ul>
         * 
         * @see javax.swing.JSplitPane
         * @see SplitPaneState
         * @see #getDividerLocation() 
         * @see #setDividerLocation(int) 
         * @see #getOrientation() 
         * @see #setOrientation(int) 
         */
        public SplitPaneState(int dividerLocation, int orientation) {
            checkOrientation(orientation);
            if (dividerLocation < -1) {
                throw new IllegalArgumentException("invalid dividerLocation");
            }
            this.dividerLocation = dividerLocation;
            this.orientation = orientation;
        }

        /**
         * Returns the last value passed to <code>setDividerLocation</code>. The
         * value returned from this method may differ from the actual divider
         * location (if <code>setDividerLocation</code> was passed a value
         * bigger than the current size).
         * 
         * @return an integer specifying the location of the divider
         * 
         * @see javax.swing.JSplitPane#getDividerLocation() 
         * @see #setDividerLocation(int) 
         * @see SplitPaneState
         */
        public int getDividerLocation() {
            return dividerLocation;
        }

        /**
         * Sets the location of the divider. This is passed off to the look and
         * feel implementation, and then listeners are notified.
         * 
         * @param dividerLocation an <code>int</code> specifying a UI-specific
         *          value (typically a pixel count)
         * 
         * @see javax.swing.JSplitPane#setDividerLocation(int) 
         * @see #getDividerLocation() 
         * @see SplitPaneState
         */
        public void setDividerLocation(int dividerLocation) {
            if (dividerLocation < -1) {
                throw new IllegalArgumentException("invalid dividerLocation");
            }
            this.dividerLocation = dividerLocation;
        }

        /**
         * Returns the orientation.
         * 
         * @return an integer giving the orientation
         * 
         * @see javax.swing.JSplitPane#getOrientation() 
         * @see #setOrientation(int) 
         * @see SplitPaneState
         */
        public int getOrientation() {
            return orientation;
        }

        /**
         * Sets the orientation, or how the splitter is divided. The option are:
         * <ul>
         *  <li><code>JSplitPane.VERTICAL_SPLIT</code> (above/below orientation
         *      of components)</li>
         *  <li><code>JSplitPane.HORIZONTAL_SPIT</code> (left/right orientation
         *      of components)</li>
         * </ul>
         * 
         * @param orientation an integer specifying the orientation
         * 
         * @see javax.swing.JSplitPane#setOrientation(int) 
         * @see #getOrientation() 
         * @see SplitPaneState
         */
        public void setOrientation(int orientation) {
            checkOrientation(orientation);
            this.orientation = orientation;
        }
    }

    /**
     * A <code>sessionState</code> property for JSplitPane.
     * <p>
     * This class defines how the session state for <code>JSplitPanes</code> is
     * {@link WindowProperty#getSessionState saved} and and
     * {@link WindowProperty#setSessionState restored} in terms of a property
     * called <code>sessionState</code>. The JSplitPane's <code>dividerLocation</code> is
     * saved and restored if its <code>orientation</code> hasn't changed.
     * <p>
     * <code>SplitPaneProperty</code> is registered for <code>JSplitPane.class
     * </code> by default, so this class applies to JSplitPane and any
     * subclass of JSplitPane. One can override the default with the
     * {@link #putProperty putProperty} method.
     *
     * @see SplitPaneState
     * @see #save(java.awt.Component, java.lang.String) 
     * @see #restore(java.awt.Component, java.lang.String) 
     */
    public static class SplitPaneProperty implements Property {

        private void checkComponent(Component component) {
            if (component == null) {
                throw new IllegalArgumentException("null component");
            }
            if (!(component instanceof JSplitPane)) {
                throw new IllegalArgumentException("invalid component");
            }
        }

        /**
         * Returns a {@link SplitPaneState SplitPaneState} object for
         * <code>JSplitPane c</code>. If the split pane's <code>dividerLocation</code> is
         * -1, indicating that either the divider hasn't been moved, or it's
         * been reset, then return null.
         * <p>
         * Throws an <code>IllegalArgumentException</code> if <code>Component c</code>
         * isn't a non-null <code>JSplitPane</code>.
         *
         * @param c the <code>JSplitPane</code> whose dividerLocation will be recorded
         * in a <code>SplitPaneState</code> object.
         * @return the <code>SplitPaneState</code> object
         * 
         * @see #setSessionState(java.awt.Component, java.lang.Object) 
         * @see SplitPaneState
         */
        @Override
        public Object getSessionState(Component c) {
            checkComponent(c);
            JSplitPane p = (JSplitPane) c;
            return new SplitPaneState(p.getUI().getDividerLocation(p), p.getOrientation());
        }

        /**
         * Restore the <code>JSplitPane</code>'s <code>dividerLocation</code> property if
         * its {@link JSplitPane#getOrientation orientation} has not changed.
         * <p>
         * Throws an <code>IllegalArgumentException</code> if <code>c</code> is not a
         * <code>JSplitPane</code> or if <code>state</code> is non-null but not an
         * instance of {@link SplitPaneState}.
         *
         * @param c the JSplitPane whose state is to be restored
         * @param state the <code>SplitPaneState</code> to be restored
         * 
         * @see #getSessionState(java.awt.Component) 
         * @see SplitPaneState
         */
        @Override
        public void setSessionState(Component c, Object state) {
            checkComponent(c);
            if ((state != null) && !(state instanceof SplitPaneState)) {
                throw new IllegalArgumentException("invalid state");
            }
            JSplitPane p = (JSplitPane) c;
            SplitPaneState sps = (SplitPaneState) state;
            if (p.getOrientation() == sps.getOrientation()) {
                p.setDividerLocation(sps.getDividerLocation());
            }
        }
    }

    /**
     * This Java Bean records the <code>columnWidths</code> for all of the columns in
     * a JTable. A width of -1 is used to mark <code>TableColumns</code> that are not
     * resizable.
     *
     * @see TableProperty
     * @see #save(java.awt.Component, java.lang.String) 
     * @see #restore(java.awt.Component, java.lang.String) 
     */
    public static class TableState {

        private int[] columnWidths = new int[0];

        private int[] copyColumnWidths(int[] columnWidths) {
            if (columnWidths == null) {
                throw new IllegalArgumentException("invalid columnWidths");
            }
            int[] copy = new int[columnWidths.length];
            System.arraycopy(columnWidths, 0, copy, 0, columnWidths.length);
            return copy;
        }

        /**
         * Constructs a new <code>TableState</code> object with the specified
         * column widths.
         * 
         * @param columnWidths the widths of each of the columns
         * 
         * @see #getColumnWidths() 
         * @see #setColumnWidths(int[]) 
         * @see javax.swing.JTable
         */
        public TableState(int[] columnWidths) {
            this.columnWidths = copyColumnWidths(columnWidths);
        }

        /**
         * Returns the widths of all of the columns of the table.
         * 
         * @return the column widths as an array of <code>int</code>s
         * 
         * @see #setColumnWidths(int[]) 
         * @see TableState
         */
        public int[] getColumnWidths() {
            return copyColumnWidths(columnWidths);
        }

        /**
         * Sets the widths of all of the table's columns.
         * 
         * @param columnWidths an array of <code>int</code>s containing the 
         *          widths of all the columns
         * 
         * @see #getColumnWidths() 
         * @see TableState
         */
        public void setColumnWidths(int[] columnWidths) {
            this.columnWidths = copyColumnWidths(columnWidths);
        }
    }

    /**
     * A <code>sessionState</code> property for JTable
     * <p>
     * This class defines how the session state for <code>JTables</code> is
     * {@link WindowProperty#getSessionState saved} and and
     * {@link WindowProperty#setSessionState restored} in terms of a property
     * called <code>sessionState</code>. We save and restore the width of each
     * resizable <code>TableColumn</code>, if the number of columns haven't changed.
     * <p>
     * <code>TableProperty</code> is registered for <code>JTable.class</code> by
     * default, so this class applies to JTable and any
     * subclass of JTable. One can override the default with the
     * {@link #putProperty putProperty} method.
     *
     * @see TableState
     * @see #save(java.awt.Component, java.lang.String) 
     * @see #restore(java.awt.Component, java.lang.String) 
     */
    public static class TableProperty implements Property {

        private void checkComponent(Component component) {
            if (component == null) {
                throw new IllegalArgumentException("null component");
            }
            if (!(component instanceof JTable)) {
                throw new IllegalArgumentException("invalid component");
            }
        }

        /**
         * Returns a {@link TableState TableState} object for <code>JTable c</code>
         * or null, if none of the JTable's columns are
         * {@link TableColumn#getResizable resizable}. A width of -1 is used to
         * mark <code>TableColumns</code> that are not resizable.
         * <p>
         * Throws an <code>IllegalArgumentException</code> if <code>Component c</code>
         * isn't a non-null <code>JTable</code>.
         *
         * @param c the <code>JTable</code> whose columnWidths will be saved in a
         * <code>TableState</code> object.
         * @return the <code>TableState</code> object or null
         * @see #setSessionState(java.awt.Component, java.lang.Object) 
         * @see TableState
         */
        @Override
        public Object getSessionState(Component c) {
            checkComponent(c);
            JTable table = (JTable) c;
            int[] columnWidths = new int[table.getColumnCount()];
            boolean resizableColumnExists = false;
            for (int i = 0; i < columnWidths.length; i++) {
                TableColumn tc = table.getColumnModel().getColumn(i);
                columnWidths[i] = (tc.getResizable()) ? tc.getWidth() : -1;
                if (tc.getResizable()) {
                    resizableColumnExists = true;
                }
            }
            return (resizableColumnExists) ? new TableState(columnWidths) : null;
        }

        /**
         * Restore the width of each resizable <code>TableColumn</code>, if the
         * number of columns haven't changed.
         * <p>
         * Throws an <code>IllegalArgumentException</code> if <code>c</code> is not a
         * <code>JTable</code> or if <code>state</code> is not an instance of
         * {@link TableState}.
         *
         * @param c the JTable whose column widths are to be restored
         * @param state the <code>TableState</code> to be restored
         * @see #getSessionState(java.awt.Component) 
         * @see TableState
         */
        @Override
        public void setSessionState(Component c, Object state) {
            checkComponent(c);
            if (!(state instanceof TableState)) {
                throw new IllegalArgumentException("invalid state");
            }
            JTable table = (JTable) c;
            int[] columnWidths = ((TableState) state).getColumnWidths();
            if (table.getColumnCount() == columnWidths.length) {
                for (int i = 0; i < columnWidths.length; i++) {
                    if (columnWidths[i] != -1) {
                        TableColumn tc = table.getColumnModel().getColumn(i);
                        if (tc.getResizable()) {
                            tc.setPreferredWidth(columnWidths[i]);
                        }
                    }
                }
            }
        }
    }

    private void checkClassArg(Class cls) {
        if (cls == null) {
            throw new IllegalArgumentException("null class");
        }
    }

    /**
     * Returns the <code>Property</code> object that was
     * {@link #putProperty registered} for the specified class or a superclass.
     * If no Property has been registered, return null. To lookup the session
     * state <code>Property</code> for a <code>Component</code> use
     * {@link #getProperty(Component)}.
     * <p>
     * Throws an <code>IllegalArgumentException</code> if <code>cls</code> is null.
     * </>
     *
     * @param cls the class to which the returned <code>Property</code> applies
     * @return the <code>Property</code> registered with <code>putProperty</code> for the
     * specified class or the first one registered for a superclass of
     * <code>cls</code>.
     * 
     * @see #getProperty(java.awt.Component) 
     * @see #putProperty(java.lang.Class, org.jdesktop.application.SessionStorage.Property) 
     * @see #save(java.awt.Component, java.lang.String) 
     * @see #restore(java.awt.Component, java.lang.String) 
     */
    public Property getProperty(Class cls) {
        checkClassArg(cls);
        while (cls != null) {
            Property p = propertyMap.get(cls);
            if (p != null) {
                return p;
            }
            cls = cls.getSuperclass();
        }
        return null;
    }

    /**
     * Register a <code>Property</code> for the specified class. One can clear the
     * <code>Property</code> for a class by setting the entry to null:
     * <pre>
     * sessionStorage.putProperty(myClass.class, null);
     * </pre>
     * <p>
     * Throws an <code>IllegalArgumentException</code> if <code>cls</code> is null.
     * Throws an <code>IllegalArgumentException</code> if <code>cls</code> is null.
     * </p>
     *
     * @param cls the class to which <code>property</code> applies.
     * @param property the <code>Property</code> object to register or null.
     * 
     * @see #getProperty(java.awt.Component) 
     * @see #getProperty(java.lang.Class) 
     * @see #save(java.awt.Component, java.lang.String) 
     * @see #restore(java.awt.Component, java.lang.String) 
     */
    public void putProperty(Class cls, Property property) {
        checkClassArg(cls);
        propertyMap.put(cls, property);
    }

    /**
     * If a <code>sessionState Property</code> object exists for the specified
     * Component return it, otherwise return null. This method is used by the
     * {@link #save save} and {@link #restore restore} methods to lookup the
     * <code>sessionState Property</code> object for each component to whose session
     * state is to be saved or restored.
     * <p>
     * The <code>putProperty</code> method registers a Property object for a class.
     * One can specify a Property object for a single Swing component by setting
     * the component's client property, like this:
     * <pre>
     * myJComponent.putClientProperty(SessionState.Property.class, myProperty);
     * </pre> One can also create components that implement the
     * <code>SessionState.Property</code> interface directly.
     *
     * @return if <code>Component c</code> implements <code>Session.Property</code>, then
     * <code>c</code>, if <code>c</code> is a <code>JComponent</code> with a <code>Property</code>
     * valued {@link javax.swing.JComponent#getClientProperty client property}
     * under (client property key) <code>SessionState.Property.class</code>, then
     * return that, otherwise return the value of
     * <code>getProperty(c.getClass())</code>.
     * <p>
     * Throws an <code>IllegalArgumentException</code> if <code>Component c</code> is
     * null.
     * 
     * @param c the component for which to get the property
     *
     * @see javax.swing.JComponent#putClientProperty(java.lang.Object, java.lang.Object) 
     * @see #getProperty(java.lang.Class) 
     * @see #putProperty(java.lang.Class, org.jdesktop.application.SessionStorage.Property) 
     * @see #save(java.awt.Component, java.lang.String) 
     * @see #restore(java.awt.Component, java.lang.String) 
     */
    public final Property getProperty(Component c) {
        if (c == null) {
            throw new IllegalArgumentException("null component");
        }
        if (c instanceof Property) {
            return (Property) c;
        } else {
            Property p = null;
            if (c instanceof JComponent) {
                Object v = ((JComponent) c).getClientProperty(Property.class);
                p = (v instanceof Property) ? (Property) v : null;
            }
            return (p != null) ? p : getProperty(c.getClass());
        }
    }
}
