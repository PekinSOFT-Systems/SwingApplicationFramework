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
 *  Class      :   View.java
 *  Author     :   Sean Carrick
 *  Created    :   Feb 11, 2021 @ 7:05:36 PM
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

import java.awt.BorderLayout;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JToolBar;

/**
 * A View encapsulates a top-level Application GUI component, like a JFrame or
 * an Applet, and its main GUI elements: a menu bar, tool bar, component, and a
 * status bar. All of the elements are optional (although a View without a main
 * component would be unusual). Views have a <code>JRootPane</code>, which is
 * the root component for all of the Swing Window types as well as JApplet.
 * Setting a View property, like <code>menuBar</code> or <code>toolBar</code>,
 * just adds a component to the rootPane in a way that's defined by the View
 * subclass. By default the View elements are arranged in a conventional way:
 * <ul>
 * <li> <code>menuBar</code> - becomes the rootPane's JMenuBar</li>
 * <li> <code>toolBar</code> - added to <code>BorderLayout.NORTH</code> of the
 * rootPane's contentPane</li>
 * <li> <code>component</code> - added to <code>BorderLayout.CENTER</code> of
 * the rootPane's contentPane</li>
 * <li> <code>statusBar</code> - added to <code>BorderLayout.SOUTH</code> of the
 * rootPane's contentPane</li>
 * </ul>
 * <p>
 * To show or hide a View you call the corresponding Application methods. Here's
 * a simple example:</p>
 * ```java
 * class MyApplication extends SingleFrameApplication {
 *     &#064;ppOverride protected void startup() {
 *         View view = getMainView();
 *         view.setComponent(createMainComponent());
 *         view.setMenuBar(createMenuBar());
 *         show(view);
 *     }
 * }
 * ```
 * <p>
 * The advantage of Views over just configuring a JFrame or JApplet directly, is
 * that a View is more easily moved to an alternative top level container, like
 * a docking framework.</p>
 *
 * @see JRootPane
 * @see Application#show(org.jdesktop.application.View) 
 * @see Application#hide(org.jdesktop.application.View) 
 */
public class View extends AbstractBean {

    private static final Logger logger = Logger.getLogger(View.class.getName());
    private final Application application;
    private ResourceMap resourceMap = null;
    private JRootPane rootPane = null;
    private JComponent component = null;
    private JMenuBar menuBar = null;
    private List<JToolBar> toolBars = Collections.emptyList();
    private JComponent toolBarsPanel = null;
    private JComponent statusBar = null;

    /**
     * Construct an empty View object for the specified Application.
     *
     * @param application the Application responsible for showing/hiding this
     * View
     * 
     * @see Application#show(org.jdesktop.application.View) 
     * @see Application#hide(org.jdesktop.application.View) 
     */
    public View(Application application) {
        if (application == null) {
            throw new IllegalArgumentException("null application");
        }
        this.application = application;
    }

    /**
     * The <code>Application</code> that's responsible for showing/hiding this
     * View.
     *
     * @return the Application that owns this View
     * 
     * @see #getContext
     * @see Application#show(org.jdesktop.application.View) 
     * @see Application#hide(org.jdesktop.application.View) 
     */
    public final Application getApplication() {
        return application;
    }

    /**
     * The <code>ApplicationContext</code> for the {@code
     * Application} that's responsible for showing/hiding this View. This method
     * is just shorthand for <code>getApplication().getContext()</code>.
     *
     * @return the Application that owns this View
     * 
     * @see #getApplication() 
     * @see Application#show(org.jdesktop.application.View) 
     * @see Application#hide(org.jdesktop.application.View) 
     */
    public final ApplicationContext getContext() {
        return getApplication().getContext();
    }

    /**
     * The <code>ResourceMap</code> for this View. This method is just shorthand
     * for <code>getContext().getResourceMap(getClass(), View.class)</code>.
     *
     * @return The <code>ResourceMap</code> for this View
     * 
     * @see #getContext() 
     */
    public ResourceMap getResourceMap() {
        if (resourceMap == null) {
            resourceMap = getContext().getResourceMap(getClass(), View.class);
        }
        return resourceMap;
    }

    /**
     * The <code>JRootPane</code> for this View. All of the components for this
     * View must be added to its rootPane. Most applications will do so by
     * setting the View's <code>component</code>, <code>menuBar</code>,
     * <code>toolBar</code>, and <code>statusBar</code> properties.
     *
     * @return The <code>rootPane</code> for this View
     * 
     * @see #setComponent(javax.swing.JComponent) 
     * @see #setMenuBar(javax.swing.JMenuBar) 
     * @see #setToolBar(javax.swing.JToolBar) 
     * @see #setStatusBar(javax.swing.JComponent) 
     */
    public JRootPane getRootPane() {
        if (rootPane == null) {
            rootPane = new JRootPane();
            rootPane.setOpaque(true);
        }
        return rootPane;
    }

    private void replaceContentPaneChild(JComponent oldChild, JComponent newChild, String constraint) {
        Container contentPane = getRootPane().getContentPane();
        if (oldChild != null) {
            contentPane.remove(oldChild);
        }
        if (newChild != null) {
            contentPane.add(newChild, constraint);
        }
    }

    /**
     * The main {JComponent} for this View.
     *
     * @return The <code>component</code> for this View
     * 
     * @see #setComponent(javax.swing.JComponent) 
     */
    public JComponent getComponent() {
        return component;
    }

    /**
     * Set the single main Component for this View. It's added to the
     * <code>BorderLayout.CENTER</code> of the rootPane's contentPane. If the
     * component property was already set, the old component is removed first.
     * <p>
     * This is a bound property. The default value is null.</p>
     *
     * @param component The <code>component</code> for this View
     * 
     * @see #getComponent() 
     */
    public void setComponent(JComponent component) {
        JComponent oldValue = this.component;
        this.component = component;
        replaceContentPaneChild(oldValue, this.component, BorderLayout.CENTER);
        firePropertyChange("component", oldValue, this.component);
    }

    /**
     * The main <code>JMenuBar</code> for this View.
     *
     * @return The <code>menuBar</code> for this View
     * 
     * @see #setMenuBar(javax.swing.JMenuBar) 
     */
    public JMenuBar getMenuBar() {
        return menuBar;
    }

    public void setMenuBar(JMenuBar menuBar) {
        JMenuBar oldValue = getMenuBar();
        this.menuBar = menuBar;
        getRootPane().setJMenuBar(menuBar);
        firePropertyChange("menuBar", oldValue, menuBar);
    }

    public List<JToolBar> getToolBars() {
        return toolBars;
    }

    public void setToolBars(List<JToolBar> toolBars) {
        if (toolBars == null) {
            throw new IllegalArgumentException("null toolbars");
        }
        List<JToolBar> oldValue = getToolBars();
        this.toolBars = Collections.unmodifiableList(new ArrayList(toolBars));
        JComponent oldToolBarsPanel = this.toolBarsPanel;
        JComponent newToolBarsPanel = null;
        if (this.toolBars.size() == 1) {
            newToolBarsPanel = toolBars.get(0);
        } else if (this.toolBars.size() > 1) {
            newToolBarsPanel = new JPanel();
            for (JComponent toolBar : this.toolBars) {
                newToolBarsPanel.add(toolBar);
            }
        }
        replaceContentPaneChild(oldToolBarsPanel, newToolBarsPanel, BorderLayout.NORTH);
        firePropertyChange("toolBars", oldValue, this.toolBars);
    }

    public final JToolBar getToolBar() {
        List<JToolBar> tBars = getToolBars();
        return (tBars.isEmpty()) ? null : tBars.get(0);
    }

    public final void setToolBar(JToolBar toolBar) {
        JToolBar oldValue = getToolBar();
        List<JToolBar> tBars = Collections.emptyList();
        if (toolBar != null) {
            tBars = Collections.singletonList(toolBar);
        }
        setToolBars(tBars);
        firePropertyChange("toolBar", oldValue, toolBar);
    }

    public JComponent getStatusBar() {
        return statusBar;
    }

    public void setStatusBar(JComponent statusBar) {
        JComponent oldValue = this.statusBar;
        this.statusBar = statusBar;
        replaceContentPaneChild(oldValue, this.statusBar, BorderLayout.SOUTH);
        firePropertyChange("statusBar", oldValue, this.statusBar);
    }

}
