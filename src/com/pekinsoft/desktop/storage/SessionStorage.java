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
 * Project    :   SwingApplicationFramework
 * Class      :   SessionStorage.java
 * Author     :   Sean Carrick <sean at pekinsoft dot com>
 * Created    :   Jan 31, 2021 @ 3:13:59 PM
 * Modified   :   Jan 31, 2021
 *  
 * Purpose:
 * 	
 * Revision History:
 *  
 * WHEN          BY                  REASON
 * ------------  ------------------- -------------------------------------------
 * Jan 31, 2021     Sean Carrick             Initial creation.
 * *****************************************************************************
 */
package com.pekinsoft.desktop.storage;

import com.pekinsoft.desktop.application.Application;
import com.pekinsoft.desktop.application.ApplicationContext;
import com.pekinsoft.desktop.utils.Logger;
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
import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;

/**
 * Support for storing GUI state that persists between Application sessions.
 * <p>
 * This class simplifies the common task of saving a little bit of an
 * application's GUI "session" state when the application shuts down, and then
 * restoring that state when the application is restarted. Session state is
 * stored on a per component basis, and only for components with a
 * {@link java.awt.Component#getName() name} and for which a {@code
 * SessionState.Property} object has been defined. SessionState Properties that
 * preserve the {@code bounds} {@code Rectangle} for windows, the {@code
 * dividerLocation} for {@code JSplitPane}s and the {@code selectedIndex} for
 * {@code JTabbedPane}s are defined by default. The {@code ApplicationContext}
 * {@link ApplicationContext#getSessionStorage getSessionStorage} method
 * provides a shared {@code SessionStorage} object.</p>
 * <p>
 * A typical Application saves session state in its {@link Application#shutdown
 * shutdown} method, and then restores session state in {@link Application#startup
 * startup}:</p>
 * <pre>
 * public class MyApplication extends Application {
 *     &#064;Override
 *     protected void shutdown() {
 *         getContext().getSessionStorage().<b>save</b>(mainFrame, "session.xml");
 *     }
 *
 *     &#064;Override
 *     protected void startup() {
 *         ApplicationContext appContext = getContext();
 *         appContext.setVendorId("PekinSOFT Systems");
 *         appContext.setApplicationId("SessionStorage1");
 *
 *         // ... create the GUI rooted by JFrame mainFrame
 *         appContext.getSessionStorage().<b>restore</b>(mainFrame, "session.xml");
 *     }
 *
 *     // ... Rest of the MyApplication class methods and overrides
 * }
 * </pre>
 * <p>
 * In this example, the bounds of {@code mainFrame}, as well as the session
 * state for any of its {@code JSplitPane} or {@code JTabbedPane} will be saved
 * when the application shuts down, and restored when the application starts up
 * again.
 * </p>
 * <dl><dt>Note:</dt><dd>Error handling has been omitted for this example.</dd>
 * </dl><p>
 * Session state is stored locally, relative to the user's home directory, by
 * the {@code LocalStorage} {@link LocalStorage#save save} and {@link
 * LocalStorage#load load} methods. The {@code startup} method must set the
 * {@code ApplicationContext} {@code vendorId} and {@code applicationId}
 * properties to ensure that the correct {@link LocalStorage#getDirectory() local
 * directory} is selected on all platforms. For example on Windows XP, the full
 * pathname for filename {@code "session.xml"} is typically:</p>
 * <pre>
 * ${userHome}\Application Data\${vendorId}\${applicationId}\session.xml
 * </pre>
 * <p>
 * Where the value of {@code ${userHome}} is the value of the Java System
 * Property {@code "user.home"}. On Solaris or Linux, the file is:</p>
 * <pre>
 * ${userHome}/.${applicationId}/session.xml
 * </pre>
 * <p>
 * and on OSX:</p>
 * <pre>
 * ${userHome}/Library/Application Support/${applicationId}/session.xml
 * </pre>
 *
 * @see ApplicationContext#getSessionStorage
 * @see LocalStorage
 *
 * @author Hans Muller (Original Author)
 * @author Sean Carrick (Adapting Author) &lt;sean at pekinsoft dot com&gt;
 *
 * @version 0.1.0
 * @since 0.1.0
 */
public class SessionStorage {

    private static Logger logger = Logger.getLogger(SessionStorage.class.getName());
    private final Map<Class, Property> propertyMap;
    private final ApplicationContext context;

    /**
     * Constructs a SessionStorage object. The following
     * {@link Property Property} objects are registered by default:
     * <p>
     * <table border="1" cellpadding="4%">
     * <tr>
     * <th>Base Component Type</th>
     * <th>sessionState Property</th>
     * <th>sessionState Property Value</th>
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
     * Applications typically would not create a {@code SessionStorage} object
     * directly, they would use the shared ApplicationContext value:</p>
     * <pre>
     * ApplicationContext ctx = Application.getInstance(MyApplication.class).getContext();
     * SessionStorage ss = ctx.getSessionStorage();
     * </pre>
     *
     * @param context the ApplicationContext for this SessionStorage object.
     *
     * @see ApplicationContext#getSessionStorage()
     * @see #getProperty(Class)
     * @see #getProperty(Component)
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

    /* Acted on the FIXME comment from the original and added JavaDoc */
    /**
     * Retrieves the {@code ApplicationContext} for this {@code SessionStorage}
     * object. Since session states are stored per application, the
     * {@code ApplicationContext} determines where the <tt>"session.xml"</tt>
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

    /* At some point we may replace this with a more complex scheme. */
    private String getComponentName(Component c) {
        return c.getName();
    }

    /* Return a string that uniquely identifies this component, or null if 
     * Component c does not have a name per getComponentName(). The pathname is
     * basically the name of all of the components, starting with c, separated
     * by "/". Thi spath is the reverse of what is typical, the first path
     * element is c's name, rather than the name of c's root Window. That way
     * pathnames can be distinguished without comparing much of the string. The
     * names of intermediate components *can* be null, we substitute
     * "[type][z-order]" for the name. Here's an example:
     *
     * JFrame myFrame = new JFrame();
     * myFrame.setName("myFrame");
     * JPanel p = new JPanel(){}; // Anonymous JPanel subclass
     * JButton myButton = new JButton();
     * myButton.setName("myButton");
     * p.add(myButton);
     * myFrame.add(p);
     *
     * getComponentPathname(myButton) =>
     * "myButton/AnonymousJPanel0/null.contentPane/null.layeredPane/JRootPane0/myFrame"
     *
     * Notes about name usage in AWT/Swing: JRootPane (inexplicably) assigns
     * names to its children (layeredPane, contentPane, glassPane); all AWT
     * components lazily compute a name. If we had not assigned the JFrame a
     * name, its name would have been "frame0".
     */
    ////////////////////////////////////////////////////////////////////////////
    // ADAPTATION NOTE: Since most browsers have dropped support for Applets, //
    //     we are not going to support them within the Swing Application      //
    //     Framework library, either. It simply makes no sense to provide a   //
    //     feature or use that is no longer widely supported due to security  //
    //     reasons.                                                           //
    //                                       - Sean Carrick (Adapting Author) //
    ////////////////////////////////////////////////////////////////////////////
    private String getComponentPathname(Component c) {
        String name = getComponentName(c);
        if (name == null) {
            return null;
        }

        StringBuilder path = new StringBuilder(name);

        while ((c.getParent() != null) && !(c instanceof Window)) {
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
                    // Implies that the component tree is changing while we are
                    //+ computing the path. Punt...
                    logger.warning("Could NOT compute pathname for " + c);
                    return null;
                }
            }
            path.append("/").append(name);
        }
        return path.toString();
    }

    /* Recursively walk the component tree, beadth first, storing the state -
     * Property.getSessionState() - of named components under their pathname
     * (the key) in stateMap.
     *
     * Note: the beadth first tree-walking code here should remain structurally
     * identical to restoreTree().
     */
    private void saveTree(List<Component> roots, Map<String, Object> stateMap) {
        List<Component> allChildren = new ArrayList<>();

        for (Component root : roots) {
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
            if (root instanceof Container) {
                Component[] children = ((Container) root).getComponents();

                if ((children != null) && (children.length > 0)) {
                    Collections.addAll(allChildren, children);
                }
            }
        }

        if (allChildren.size() > 0) {
            saveTree(allChildren, stateMap);
        }
    }

    /**
     * Saves the state of each named component in the specified hierarchy to a
     * file using {@link LocalStorage#save(java.util.Map, java.lang.String)
     * LocalStorage.save(fileName)}. Each component is visited in breadth-first
     * order: if a {@code Property} {@link #getProperty(Component) exists} for
     * that component, and the component has a {@link java.awt.Component#getName()
     * name}, then its {@link Property#getSessionState state} is saved.
     * <p>
     * Component names can be any string however they must be unique relative to
     * the names of the component's siblings. Most Swing components do not have
     * a name by default, however there are some exceptions: JRootPane
     * (inexplicably) assigns names to its children (layeredPane, contentPane,
     * and glassPane); and all AWT components lazily compute a name, so JFrame,
     * JDialog, and JWindow also have a name by default.</p>
     * <p>
     * The type of the sessionState values (i.e., the type of values returned by
     * {@code Property.getSessionState}) must be one of those supported by
     * {@link java.beans.XMLEncoder XMLEncoder} and {@link java.beans.XMLDecoder
     * XMLDecoder}, for example beans (null constructor, read/write properties),
     * primitives, and Collections. Java bean classes and their properties must
     * be public. Typically beans defined for this purpose are little more than
     * a handful of simple properties. The JDK 6 &#064;ConstructorProperties
     * annotation can be used to eliminate the need for writing set methods in
     * such beans, e.g.</p>
     * <pre>
     * public class FooBar {
     *     private String foo, bar;
     *
     *     // Defines the mapping from constructor params to properties
     *     &#064;ConstructorProperties({"foo", "bar"})
     *     public FooBar(Stirng foo, String bar) {
     *         this.foo = foo;
     *         this.bar = bar;
     *     }
     *
     *     public String getFoo() { return foo; } // Don't need setFoo
     *     public String getBar() { return bar; } // Don't need setBar
     * }
     * </pre>
     *
     * @param root the root of the Component hierarchy to be saved
     * @param fileName the {@code LocalStorage} filename
     * @throws IOException in the event some sort of input/output error occurs
     *
     * @see #restore
     * @see ApplicationContext#getLocalStorage()
     * @see LocalStorage#save(java.util.Map, java.lang.String)
     * @see #getProperty(Component)
     */
    public void save(Component root, String fileName) throws IOException {
        checkSaveRestoreArgs(root, fileName);

        Map<String, Object> stateMap = new HashMap<>();

        saveTree(Collections.singletonList(root), stateMap);

        LocalStorage lst = getContext().getLocalStorage();
        lst.save(stateMap, fileName);
    }

    /* Recursively walk the component tree, breadth first, restoring the state -
     * Property.setSessionState() - of named components for which there's a
     * non-null entry under the component's pathName in stateMap.
     */
    private void restoreTree(List<Component> roots, Map<String, Object> stateMap) {
        List<Component> allChildren = new ArrayList<>();

        for (Component root : roots) {
            if (root != null) {
                Property p = getProperty(root);

                if (p != null) {
                    String pathname = getComponentPathname(root);
                    if (pathname != null) {
                        Object state = stateMap.get(pathname);
                        if (state != null) {
                            p.setSessionState(root, state);
                        } else {
                            logger.warning("No saved state for " + root);
                        }
                    }
                }
            }

            if (root instanceof Container) {
                Component[] children = ((Container) root).getComponents();
                if ((children != null) && (children.length > 0)) {
                    Collections.addAll(allChildren, children);
                }
            }
        }

        if (allChildren.size() > 0) {
            restoreTree(allChildren, stateMap);
        }
    }

    /**
     * Restores each named component in the specified hierarchy from the session
     * state loaded from a file using
     * {@link LocalStorage#load LocalStorage.load(fileName)}. Each component is
     * visited in breadth-first order: if a
     * {@link #getProperty(Component) Property} exists for that component, and
     * the component has a {@link java.awt.Component#getName() name}, then its
     * state is {@link Property#setSessionState restored}.
     *
     * @param root the root of the Component hierarchy to be restored
     * @param fileName the {@code LocalStorage} filename
     * @throws IOException In the event errors occur while reading filename
     * @see #save(java.awt.Component, java.lang.String)
     * @see ApplicationContext#getLocalStorage()
     * @see LocalStorage#save(java.util.Map, java.lang.String)
     * @see #getProperty(Component)
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
     * Defines the {@code sessionState} property. The value of this property is
     * the GUI state that should be preserved across sessions for the specified
     * GUI component. The type of sessionState values must be one of those
     * supported by {@link java.beans.XMLEncoder XMLEncoder} and
     * {@link java.beans.XMLDecoder XMLDecoder}, for example beans (null
     * constructor, read/write properties), primitives, and Collections.
     *
     * @see #putProperty
     * @see #getProperty(Class)
     * @see #getProperty(Component)
     */
    public interface Property {

        /**
         * Return the value of the {@code sessionState} property, typically a
         * Java bean or a Collection that defines the {@code Component} state
         * that should be preserved across Application sessions. This value will
         * be stored with {@link java.beans.XMLEncoder XMLEncoder}, loaded with
         * {@link java.beans.XMLDecoder XMLDecoder}, and passed to {@code
         * setSessionState} to restore the Component's state.
         *
         * @param c the Component
         * @return the {@code sessionState} object for Component {@code c}
         * @see #setSessionState
         */
        Object getSessionState(Component c);

        /**
         * Restore Component {@code c}'s {@code sessionState} from the specified
         * object.
         *
         * @param c the Component
         * @param state the value of the {@code sessionState} property
         * @see #getSessionState(java.awt.Component)
         */
        void setSessionState(Component c, Object state);
    }

    /**
     * This Java Bean defines the {@code Window} state preserved across
     * sessions: the Window's {@code bounds}, and the bounds of the Window's
     * {@code GraphicsConfiguration}, i.e., the bounds of the screen on which
     * the Window appears. If the Window is actually a Frame, we also store its
     * extendedState. {@code WindowState} objects are stored and restored by the
     * {@link WindowProperty WindowProperty} class.
     */
    public static class WindowState {

        private final Rectangle bounds;
        private Rectangle gcBounds = null;
        private int screenCount;
        private int frameState = Frame.NORMAL;

        public WindowState() {
            bounds = new Rectangle();
        }

        public WindowState(Rectangle bounds, Rectangle gcBounds, int screenCount,
                int frameState) {
            if (bounds == null) {
                throw new IllegalArgumentException("null bounds");
            }
            if (screenCount < 1) {
                throw new IllegalArgumentException("invalid screenCount");
            }

            this.bounds = bounds;
            this.gcBounds = gcBounds;
            this.screenCount = screenCount;
            this.frameState = frameState;
        }

        public Rectangle getBounds() {
            return bounds;
        }

        public void setBounds(Rectangle bounds) {
            this.bounds.setBounds(bounds);
        }

        public int getScreenCount() {
            return screenCount;
        }

        public void setScreenCount(int screenCount) {
            this.screenCount = screenCount;
        }

        public int getFrameState() {
            return frameState;
        }

        public void setFrameState(int frameState) {
            this.frameState = frameState;
        }

        public Rectangle getGraphicsConfiguration() {
            return gcBounds;
        }

        public void setGraphicsConfiguration(Rectangle gcBounds) {
            this.gcBounds = (gcBounds == null) ? null : new Rectangle(gcBounds);
        }

    }

    /**
     * A {@code sessionState} property for Window.
     * <p>
     * This class defines how th esession state for {@code Window}s is
     * {@link WindowProperty#getSessionState(java.awt.Component) saved} and null null     {@link WindowProperty#setSessionState(java.awt.Component, java.lang.Object) 
     * restored} in terms of a property called {@code sessionState}. The
     * Window's {@code bounds Rectangle} is saved and restored if the dimensions
     * of the Window's screen have not changed.</p>
     * <p>
     * {@code WindowProperty} is registered for {@code Window.class} by default,
     * so this class applies to the AWT {@code Window}, {@code Dialog}, and
     * {@code Frame} class, as well as their Swing counterparts:
     * {@code JWindow}, {@code JDialog}, and {@code JFrame}.</p>
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
         * {@code Window c}.
         * <p>
         * Throws an {@code IllegalArgumentException} if {@code Component c} is
         * not a non-null {@code Window}.
         *
         * @param c the {@code Window} whose bounds will be stored in a
         * {@code WindowState} object
         * @return the {@code WindowState} object
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

            /* If this is a JFrame created by FrameView and it has been maximized,
             * retrieve the frame's normal (not maximized) bounds. More info:
             * see FrameStateListener#windowStateChanged in FrameView.
             */
            if ((c instanceof JFrame) && (0 != (frameState & Frame.MAXIMIZED_BOTH))) {
                String clientPropertyKey = "WindowState.normalBounds";
                Object r = ((JFrame) c).getRootPane().getClientProperty(
                        clientPropertyKey);
                if (r instanceof Rectangle) {
                    frameBounds = (Rectangle) r;
                }
            }

            return new WindowState(frameBounds, gcBounds, getScreenCount(),
                    frameState);
        }

        /**
         * Restore the {@code Window}'s bounds if the dimensions of its screen
         * ({@code GraphicsConfiguration}) have not changed, the number of
         * screens has not changed, and the {@link Window#isLocationByPlatform()
         * isLocationByPlatform} property, which indicates that native Window
         * manager should pick the Window's location, is false. More precisely:
         * <p>
         * If {@code state} is non-null, and Window {@code c}'s {@code
         * GraphicsConfiguration} {@link GraphicsConfiguration#getBounds bounds}
         * matches the {@link WindowState#getGraphicsConfiguration() WindowState's
         * value}, and Window {@code c}'s {@link Window#isLocationByPlatform()
         * isLocationByPlatform} property is false, then set the Window's
         * location to the value
         * {@link WindowState#getBounds() saved value}.</p>
         * <p>
         * Throws an {@code IllegalArgumentException} if {@code c} is not a
         * {@code Window} or if {@code state} is non-null but not an instance of
         * {@link WindowState}.</p>
         *
         * @param c the Window whose state is to be restored
         * @param state the {@code WindowState} to be restored
         * @see #getSessionState(java.awt.Component)
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
                Rectangle gcBounds0 = windowState.getGraphicsConfiguration();
                int sc0 = windowState.getScreenCount();
                GraphicsConfiguration gc = c.getGraphicsConfiguration();
                Rectangle gcBounds1 = (gc == null) ? null : gc.getBounds();
                int sc1 = getScreenCount();

                if ((gcBounds0 != null) && (gcBounds0.equals(gcBounds1))
                        && (sc0 == sc1)) {
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
     * This Java Bean records the {@code selectedIndex} and {@code tabCount}
     * properties of a {@code JTabbedPane}. A {@code TabbedPaneState} object
     * created by {@link TabbedPaneProperty#getSessionState} and used to restore
     * the selected tab by {@link TabbedPaneProperty#setSessionState}.
     *
     * @see TabbedPaneProperty
     * @see #save(java.awt.Component, java.lang.String)
     * @see #restore(java.awt.Component, java.lang.String)
     */
    public static class TabbedPaneState {

        private int selectedIndex;
        private int tabCount;

        public TabbedPaneState() {
            selectedIndex = -1;
            tabCount = 0;
        }

        public TabbedPaneState(int selectedIndex, int tabCount) {
            if (tabCount < 0) {
                throw new IllegalArgumentException("invalid tabCount");
            }
            if (selectedIndex < -1) {
                throw new IllegalArgumentException("invalid selectedIndex");
            }

            this.selectedIndex = selectedIndex;
            this.tabCount = tabCount;
        }

        public int getSelectedIndex() {
            return selectedIndex;
        }

        public void setSelectedIndex(int selectedIndex) {
            if (selectedIndex < -1) {
                throw new IllegalArgumentException("invalid selectedIndex");
            }

            this.selectedIndex = selectedIndex;
        }

        public int getTabCount() {
            return tabCount;
        }

        public void setTabCount(int tabCount) {
            if (tabCount < 0) {
                throw new IllegalArgumentException("invalid tabCount");
            }

            this.tabCount = tabCount;
        }
    }

    /**
     * A {@code sessionState} property for {@code JTabbedPane}s.
     * <p>
     * This class defines how the session state for {@code JTabbedPane}s is
     * {@link WindowProperty#getSessionState(java.awt.Component) saved} and      {@link WindowProperty#setSessionState(java.awt.Component, java.lang.Object) 
     * restored} in terms of a property called {@code sessionState}. The
     * JTabbedPane's {@code selectedIndex} is saved and restored if the number
     * of tabs ({@code tabCount}) has not changed.</p>
     * <p>
     * {@code TabbedPaneProperty} is registered for {@code JTabbedPane.class} by
     * default, so this class applies to JTabbedPane and any subclass of
     * JTabbedPane. One can override the default with the {@link #putProperty
     * putProperty} method.
     *
     * @see TabbedPaneState
     * @see #save(java.awt.Component, java.lang.String)
     * @see #restore(java.awt.Component, java.lang.String)
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
         * Returns a {@link TabbedPaneState TabbedPaneState} object for {@code
         * JTabbedPane c}.
         * <p>
         * Throws an {@code IllegalArgumentException} if {@code Component c} is
         * not a non-null {@code JTabbedPane}.
         *
         * @param c the {@code JTabbedPane} whose selectedIndex will be recorded
         * in a {@code TabbedPaneState} object
         * @return the {@code TabbedPaneState} object
         * @see #setSessionState(java.awt.Component, java.lang.Object)
         * @see TabbedPaneState
         */
        @Override
        public Object getSessionState(Component c) {
            checkComponent(c);

            JTabbedPane p = (JTabbedPane) c;

            return new TabbedPaneState(p.getSelectedIndex(), p.getTabCount());
        }

        /**
         * Restore the {@code JTabbedPane}'s {@code selectedIndex} property if
         * the number of {@link JTabbedPane#getTabCount() tabs} has not changed.
         * <p>
         * Throws an {@code IllegalArgumentException} if {@code c} is not a
         * {@code JTabbedPane} or if {@code state} is non-null but is not an
         * instance of {@link TabbedPaneState}.
         *
         * @param c the JTabbedPane whose state is to be restored
         * @param state the {@code TabbedPaneState} to restore
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
     * This Java Bean records the {@code dividerLocation} and
     * {@code orientation} properties of a {@code JSplitPane}. A
     * {@code SplitPaneState} object created by
     * {@link SplitPaneProperty#getSessionState} and used to restore the
     * orientation by {@link SplitPaneProperty#setSessionState}.
     *
     * @see SplitPaneProperty
     * @see #save(java.awt.Component, java.lang.String)
     * @see #restore(java.awt.Component, java.lang.String)
     */
    public static class SplitPaneState {

        private int dividerLocation = -1;
        private int orientation = JSplitPane.HORIZONTAL_SPLIT;

        private void checkOrientation(int orientation) {
            if ((orientation != JSplitPane.VERTICAL_SPLIT)
                    && (orientation != JSplitPane.HORIZONTAL_SPLIT)) {
                throw new IllegalArgumentException("invalid orientation");
            }
        }

        public SplitPaneState(int dividerLocation, int orientation) {
            checkOrientation(orientation);
            if (dividerLocation < -1) {
                throw new IllegalArgumentException("invalid dividerLocation");
            }

            this.dividerLocation = dividerLocation;
            this.orientation = orientation;
        }

        public int getDividerLocation() {
            return dividerLocation;
        }

        public void setDividerLocation(int dividerLocation) {
            if (dividerLocation < -1) {
                throw new IllegalArgumentException("invalid dividerLocation");
            }

            this.dividerLocation = dividerLocation;
        }

        public int getOrientation() {
            return orientation;
        }

        public void setOrientation(int orientation) {
            checkOrientation(orientation);

            this.orientation = orientation;
        }
    }

    /**
     * A {@code sessionState} property for JSplitPane.
     * <p>
     * This class defines how the session state for {@code JSplitPane}s is
     * {@link WindowProperty#getSessionState(java.awt.Component) saved} and      {@link WindowProperty#setSessionState(java.awt.Component, java.lang.Object) 
     * restored} in terms of a property called {@code sessionState}. The
     * JSplitPane's {@code dividerLocation} is saved and restored if its
     * {@code orientation} has not changed</p>
     * <p>
     * {@code SplitPaneProperty} is registered for {@code JSplitPane.class} by
     * default, so this class applies to JSplitPane and any subclass of
     * JSplitPane. One can override the default with the
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
         * Returns a {@link SplitPaneState SplitPaneState} object for {@code
         * JSplitPane c}. If the split pane's {@code dividerLocation} is -1,
         * indicating that either the divider has not been moved, or it has been
         * reset, then return null.
         *
         * @param c the {@code JSplitPane} whose dividerLocation will be
         * recorded in a {@code SplitPaneState} object
         * @return the {@code SplitPaneState} object
         * @see #setSessionState(java.awt.Component, java.lang.Object)
         * @see SplitPaneState
         */
        @Override
        public Object getSessionState(Component c) {
            checkComponent(c);

            JSplitPane p = (JSplitPane) c;
            return new SplitPaneState(p.getUI().getDividerLocation(p),
                    p.getOrientation());
        }

        /**
         * Restore the {@code JSplitPane}'s {@code dividerLocation} property if
         * its {@link JSplitPane#getOrientation() orientation} has not changed.
         * <p>
         * Throws an {@code IllegalArgumentException} if {@code c} is not a
         * {@code JSplitPane} or if {@code state} is non-null but not an
         * instance of {@link SplitPaneState}.
         *
         * @param c the JSplitPane whose state is to be restored
         * @param state the {@code SplitPaneState} to be restored
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
}
