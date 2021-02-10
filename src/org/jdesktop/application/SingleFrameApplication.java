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
 * Class      :   SingleFrameApplication.java
 * Author     :   Sean Carrick <sean at pekinsoft dot com>
 * Created    :   Feb 09, 2021 @ 13:32:43 PM
 * Modified   :   Feb 09, 2021
 *  
 * Purpose:     See class JavaDoc
 * 	
 * Revision History:
 *  
 * WHEN          BY                  REASON
 * ------------  ------------------- -------------------------------------------
 * Feb 09, 2021  Sean Carrick        Initial creation.
 * *****************************************************************************
 */
package org.jdesktop.application;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JWindow;
import javax.swing.RootPaneContainer;
import org.jdesktop.application.utils.Logger;
import org.jdesktop.application.view.FrameView;
import org.jdesktop.application.view.View;

/**
 * An application base class for simple GUIs with one primary JFrame.
 * <p>
 * This class takes care of component property injection, exit processing, and
 * saving/restoring session state in a way that is appropriate for simple single
 * frame applications. The application's JFrame is created automatically, with a
 * WindowListener that calls exit() when the window is closed. Session state is
 * stored when the application shuts down, and restored when the GUI is shown.</p>
 * <p>
 * To use <tt>SingleFrameApplication</tt>, one need only override <tt>startup</tt>,
 * create the GUI's main panel, and apply <tt>show</tt> to that. Here is an 
 * example:</p>
 * ```java
 * Class MyApplication extends SingleFrameApplication {
 *     @Override
 *     protected void startup() {
 *         show(new JLabel("Hello World"));
 *     }
 * }
 * ```
 * <p>
 * The call to <tt>show</tt> in this example creates a JFrame (named "mainFrame"),
 * that contains the "Hello World" JLabel. Before the frame is made visible, the
 * properties of all of the components in the hierarchy are initialized with
 * {@link ResourceMap#injectComponents ResourceMap.injectComponents} and then
 * restored from saved session state (if any) with {@link SessionStorage#restore
 * SessionStorage.restore}. When the application shuts down, session state is 
 * saved.</p>
 * <p>
 * A more realistic tiny example would rely on a ResourceBundle for the JLabel's
 * string and the main frame's title. The automatic injection step only 
 * initializes the properties of named components, so:</p>
 * ```java
 * class MyApplication extends SingleFrameApplication {
 *     @Override protected void startup() {
 *         JLabel label = new JLabel();
 *         label.setName("label");
 *         show(label);
 *     }
 * }
 * ```
 * <p>
 * The ResourceBundle should contain definitions for all of the standard 
 * Application resources, as well as the main frame's title and the label's text.
 * Note that the JFrame that is implicitly created by the <tt>show</tt> method is
 * named "mainFrame".</p>
 * <pre>
 * # resources/MyApplication.properties
 * Application.id = MyApplication
 * Application.title = My Hello World Application
 * Application.version = 1.0
 * Application.vendor = PekinSOFT Systems
 * Application.vendorId = PekinSOFT
 * Application.homepage = https://www.pekinsoft.com
 * Application.description = An example of SingleFrameApplication
 * Application.lookAndFeel = system
 * 
 * mainFrame.title = @{Application.title} @{Application.version}
 * label.text = Hello World
 * </pre>
 *
 * @author Hans Muller (Original Author)
 * @author Sean Carrick (Adapting Author) &lt;sean at pekinsoft dot com&gt;
 * 
 * @version 1.05
 * @since 1.03
 */
public class SingleFrameApplication extends Application {
    // Public Static Constants
    
    // Private Static Constants
    private static final Logger logger = Logger.getLogger(SingleFrameApplication.class.getName());
    
    // Private Member Fields
    private ResourceMap appResources = null;
    
    /**
     * Return the JFrame used to show this application.
     * <p>
     * The frame's name is set to "mainFrame", its title is initialized with the
     * value of the <tt>Application.title</tt> resource and a <tt>WindowListener</tt> 
     * is added that calls <tt>exit</tt> when the user attempts to close the
     * frame.</p>
     * <p>
     * This method may be called at any time; the JFrame is created lazily and
     * cached. For example:</p>
     * ```java
     * protected void startup() {
     *     getMainFrame().setJMenuBar(createMenuBar());
     *     show(createMainPanel());
     * }
     * ```
     * 
     * @return this application's main frame
     * 
     * @see #setMainFrame
     * @see #show
     * @see JFrame#setName
     * @see JFrame#setTitle
     * @see JFrame#addWindowListener
     */
    public final JFrame getMainFrame() {
        return getMainView().getFrame();
    }

    @Override
    protected void startup() {
        // TODO: Implement method functionality
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    /**
     * Sets the JFrame used to show this application.
     * <p>
     * This method should be called from the startup method by a subclass that
     * wants to construct and initialize the main frame itself. Most applications
     * can rely on the fact that <tt>getMainFrame</tt> lazily constructs the main
     * frame and initializes the <tt>mainFrame</tt> property.</p>
     * <p>
     * If the main frame property was already initialized, either implicitly
     * through a call to <tt>getMainFrame()</tt> or by explicitly calling this
     * method, an IllegalStateException is thrown. If <tt>mainFrame</tt> is null,
     * an IllegalArgumentException is thrown.</p>
     * <p>
     * This property is bound.</p>
     * 
     * @param mainFrame the new value of the mainFrame proeprty
     * 
     * @see #getMainFrame() 
     */
    protected final void setMainFrame(JFrame mainFrame) {
        getMainView().setFrame(mainFrame);
    }
    
    private String sessionFilename(Window window) {
        if (window == null) {
            return null;
        } else {
            String name = window.getName();
            return (name == null) ? null : name + ".session.xml";
        }
    }
    
    /**
     * Initialize the hierarchy with the specified root by injecting resources.
     * <p>
     * By default the <tt>show</tt> methods {@link ResourceMap#injectComponents(java.awt.Component) 
     * inject resources} before initializing the JFrame or JDialog's size, 
     * location, and restoring the window's session state. If the app is showing
     * a window whose resources have already been injected, or that should not
     * be initialized via resource injection, this method can be overridden to
     * defeat the default behavior.</p>
     * 
     * @param root the root component hierarchy
     * 
     * @see ResourceMap#injectComponents(java.awt.Component) 
     * @see #show(javax.swing.JComponent) 
     * @see #show(javax.swing.JFrame)
     * @see #show(javax.swing.JDialog)
     */
    protected void configureWindow(Window root) {
        getContext().getResourceMap().injectComponents(root);
    }
    
    private void initRootPaneContainer(RootPaneContainer c) {
        JComponent rootPane = c.getRootPane();
        
        // These initializations are only done once
        Object k = "SingleFrameApplication.initRootPaneContainer";
        if (rootPane.getClientProperty(k) != null) {
            return;
        }
        
        rootPane.putClientProperty(k, Boolean.TRUE);
        
        // Inject resources
        Container root = rootPane.getParent();
        if (root instanceof Window) {
            configureWindow((Window) root);
        }
        
        // If this is the mainFrame, then close == exit
        JFrame mainFrame = getMainFrame();
        if (c == mainFrame) {
            mainFrame.addWindowListener(new MainFrameListener());
            mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        } else if (root instanceof Window) { // close == save session state
            Window window = (Window) root;
            window.addHierarchyListener(new SecondaryWindowListener());
        }
        
        // If this is a JFrame monitor "normal" (not maximized) bounds
        if (root instanceof JFrame) {
            root.addComponentListener(new FrameBoundsListener());
        }
        
        // If the window's bounds do not appear to have been set, do it
        if (root instanceof Window) {
            Window window = (Window) root;
            
            if (!root.isValid() 
                    || (root.getWidth() == 0) 
                    || (root.getHeight() == 0)) {
                window.pack();
            }
            
            if (!window.isLocationByPlatform() 
                    && (root.getX() == 0) 
                    && (root.getY() == 0)) {
                Component owner = window.getOwner();
                if (owner == null) {
                    owner = (window != mainFrame) ? mainFrame : null;
                }
                
                window.setLocationRelativeTo(owner); // center window
            }
        }
        
        // Restore session state
        if (root instanceof Window) {
            String filename = sessionFilename((Window) root);
            
            if (filename != null) {
                try {
                    getContext().getSessionStorage().restore(root, filename);
                } catch (IOException e) {
                    String msg = String.format("could not restore session [%s]", 
                            filename);
                    msg += "\n\nException: " + e.getCause().toString();
                    msg += "\n\n  Message: " + e.getMessage();
                    logger.warning(msg);
                }
            }
        }
    }
    
    /**
     * Show the specified component in the {@link #getMainFrame() main frame}.
     * Typical applications will call this method after constructing their main
     * GUI panel in the <tt>startup</tt> method.
     * <p>
     * Before the main frame is made visible, the properties of all of the 
     * components in the hierarchy are initialized with {@link 
     * ResourceMap#injectComponents(java.awt.Component) 
     * ResourceMap.injectComponents} and then restored from saved session state
     * (if any) with {@link SessionStorage#restore(java.awt.Component, java.lang.String)
     * SessionStorage.restore}. When the application shuts down, session state
     * is saved.</p>
     * <p>
     * Note that the name of the lazily created main frame (see {@link 
     * #getMainFrame getMainFrame}) is set by default. Session state is only 
     * saved for top level windows with a valid name and then only for component
     * descendants that are named.</p>
     * <p>
     * Throws an IllegalArgumentException if <tt>c</tt> is null.</p>
     * 
     * @param c the main frame's contentPane child
     */
    protected void show(JComponent c) {
        if (c == null) {
            throw new IllegalArgumentException("null component");
        }
        
        JFrame f = getMainFrame();
        f.getContentPane().add(c, BorderLayout.CENTER);
        initRootPaneContainer(f);
        f.setVisible(true);
    }
    
    /**
     * Initialize and show the  JDialog.
     * <p>
     * This method is inteded for showing "secondary" windows, like message
     * dialogs, about boxes, and so on. Unlike the <tt>mainFrame</tt>  , dismissing
     * a secondary window will not exit the applicaiton.</p>
     * <p>
     * Session state is only automatically saved if the specified JDialog has a
     * name, and then only for component descendants that are named.</p>
     * <p>
     * Throws and IllegalArgumentException if <tt>c</tt> is null.</p>
     * 
     * @param c the JDialog to be displayed
     * 
     * @see #show(javax.swing.JComponent) 
     * @see #show(javax.swing.JFrame)
     * @see #configureWindow(java.awt.Window) 
     */
    public void show(JDialog c) {
        if (c == null) {
            throw new IllegalArgumentException("null JDialog");
        }
        
        initRootPaneContainer(c);
        c.setVisible(true);
    }
    
    /**
     * Initialize and show the secondary JFrame.
     * <p>
     * This method is intended for showing "secondary" windows, like child 
     * windows, etc. Unlike the <tt>mainFrame</tt>  , dismissing a secondary window
     * will not exit the application.</p>
     * <p>
     * Session state is only automatically saved if the specified JFrame has a 
     * name, and then only for component descendants that are named.</p>
     * <p>
     * Throws an IllegalArgumentException if <tt>c</tt> is null.</p>
     * 
     * @param c the JFrame to be displayed
     * 
     * @see #show(javax.swing.JComponent) 
     * @see #show(javax.swing.JDialog) 
     * @see #configureWindow(java.awt.Window) 
     */
    public void show(JFrame c) {
        if (c == null) {
            throw new IllegalArgumentException("null JFrame");
        }
        
        initRootPaneContainer(c);
        c.setVisible(true);
    }
    
    private void saveSession(Window window) {
        String filename = sessionFilename(window);
        if (filename != null) {
            try {
                getContext().getSessionStorage().save(window, filename);
            } catch (IOException e) {
                String msg = "could not save session\n\nException: ";
                msg += e.toString() + "\n\nCause: " + e.getCause().toString();
                msg += "\n\nMessage: " + e.getMessage();
                logger.warning(msg);
            }
        }
    }
    
    private boolean isVisibleWindow(Window w) {
        return w.isVisible() && ((w instanceof JFrame) || (w instanceof JDialog)
                || (w instanceof JWindow));
    }
    
    /**
     * Return all of the visible JWindows, JDialogs, and JFrames per
     * Window.getWindows() on Java SE 6, or Frame.getFrames() for earlier 
     * versions of Java.
     * 
     * @return a list of all visible windows
     */
    private List<Window> getVisibleSecondaryWindows() {
        List<Window> rv = new ArrayList<>();
        Method getWindowsM = null;
        
        try {
            getWindowsM = Window.class.getMethod("getWindows");
        } catch (NoSuchMethodException 
                | SecurityException ignore) {
            // Do nothing with the exception.
        }
        
        if (getWindowsM != null) {
            Window[] windows = null;
            try {
                windows = (Window[]) getWindowsM.invoke(null);
            } catch (IllegalAccessException 
                    | IllegalArgumentException 
                    | InvocationTargetException e) {
                String msg = "HCTB - can not get top level windows list\n\n";
                msg += "Exception: " + e.toString() + "\nCause: ";
                msg += e.getCause().toString() + "\n\nMessage: ";
                msg += e.getMessage();
                throw new Error(msg);
            }
            
            if (windows != null) {
                for (Window window : windows) {
                    if (isVisibleWindow(window)) {
                        rv.add(window);
                    }
                }
            }
        } else {
            Frame[] frames = Frame.getFrames();
            if (frames != null) {
                for (Frame frame : frames) {
                    if (isVisibleWindow(frame)) {
                        rv.add(frame);
                    }
                }
            }
        }
        
        return rv;
    }
    
    /**
     * Save session state for the component hierarchy rooted by the mainFrame.
     * SingleFrameApplicaiton subclasses that override shutdown need to remember
     * to call <tt>super.shutdown()</tt>  .
     */
    @Override
    protected void shutdown() {
        saveSession(getMainFrame());
        
        for (Window window : getVisibleSecondaryWindows()) {
            saveSession(window);
        }
    }
    
    private class MainFrameListener extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent e) {
            exit(e);
        }
    }
    
    /* Although it would have been simpler to listen for changes in the secondary
     * window's visibility per either a PropertyChangeEvent on the "visible"
     * property or a change in visibility per ComponentListener, neither listener
     * is notified if the secondary window is disposed. 
     * HierarchyEvent.SHOWING_CHANGED does report the change in all cases, so we
     * use that.
     */
    private class SecondaryWindowListener implements HierarchyListener {

        @Override
        public void hierarchyChanged(HierarchyEvent e) {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (e.getSource() instanceof Window) {
                    Window secondaryWindow = (Window) e.getSource();
                    
                    if (!secondaryWindow.isShowing()) {
                        saveSession(secondaryWindow);
                    }
                }
            }
        }
        
    }
    
    /* In order to properly restore a maximized JFrame, we need to record it's
     * normal (not maximized) bounds. They are recorded under a rootPane client
     * property here, so that they can be session-saved by 
     * WindowProperty#getSessionState().
     */
    private static class FrameBoundsListener implements ComponentListener {
        
        private void maybeSaveFrameSize(ComponentEvent e) {
            if (e.getComponent() instanceof JFrame) {
                JFrame f = (JFrame) e.getComponent();
                
                if ((f.getExtendedState() & Frame.MAXIMIZED_BOTH) == 0) {
                    String clientPropertyKey = "WindowState.normalBounds";
                    f.getRootPane().putClientProperty(clientPropertyKey, 
                            f.getBounds());
                }
            }
        }

        @Override
        public void componentResized(ComponentEvent e) {
            maybeSaveFrameSize(e);
        }

        /* Note from original author:
         *
         * BUG: on WIndows XP, with JDK6, this method is called once when the
         *      frame is maximized, with x,y=-4 and getExtendedState() == 0.
         ***********************************************************************
         * The question now (February 09, 2021) is: Does this bug still exist in
         * Windows (7 and 10) and in JDK11.
         *
         * I am leaving the method body in the state that Hans Muller had left
         * it, with the call to maybeSaveFrameState(e) commented out. If someone
         * has access to 
         */
        @Override
        public void componentMoved(ComponentEvent e) {
            /* maybeSaveFrameSize(e); */
        }

        @Override
        public void componentShown(ComponentEvent e) {
            
        }

        @Override
        public void componentHidden(ComponentEvent e) {
            
        }
        
    }
    
    /* Prototype support for the View type */
    
    private FrameView mainView = null;
    
    public FrameView getMainView() {
        if (mainView == null) {
            mainView = new FrameView(this);
        }
        
        return mainView;
    }
    
    public void show(View view) {
        if ((mainView == null) && (view instanceof FrameView)) {
            mainView = (FrameView) view;
        }
        
        RootPaneContainer c = (RootPaneContainer) view.getRootPane().getParent();
        initRootPaneContainer(c);
        ((Window)c).setVisible(true);
    }

}
