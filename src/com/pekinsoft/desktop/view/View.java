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
 * Class Name: View.java
 *     Author: Sean Carrick <sean at pekinsoft dot com>
 *    Created: Feb 06 2021
 * 
 *    Purpose:
 * 
 * *****************************************************************************
 * CHANGE LOG:
 * 
 * Date        By                   Reason
 * ----------  -------------------  --------------------------------------------
 * 02/37/2021  Sean Carrick          Initial Creation.
 * *****************************************************************************
 */
package com.pekinsoft.desktop.view;

import com.pekinsoft.desktop.application.Application;
import com.pekinsoft.desktop.application.ApplicationContext;
import com.pekinsoft.desktop.beans.AbstractBean;
import com.pekinsoft.desktop.resources.ResourceMap;
import com.pekinsoft.desktop.utils.Logger;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JToolBar;

/**
 * A View encapsulates a top-level Application GUI component, like a JFrame, and
 * its main GUI elements: a menu bar, tool bar, component, and a status bar. All
 * of the elements are optional (although a View without a main component would
 * be unusual). Views have a {@code JRootPane}, which is the root component for
 * all of the Swing Window types. Setting a View property, like {@code menuBar}
 * or {@code toolBar}, just adds a component to the rootPane in a way that is
 * defined by the View subclass. By default, the View elements are arranged in a
 * conventional way:
 * <ul>
 * <li> {@code menuBar} &mdash; becomes the rootPane's JMenuBar</li>
 * <li> {@code toolBar} &mdash; added to {@code BorderLayout.NORTH} of the
 * rootPane's contentPane</li>
 * <li> {@code component} &mdash: added to {@code BorderLayout.CENTER} of the
 * rootPane's contentPane</li>
 * <li> {@code statusBar} &mdash; added to {@code BorderLayout.SOUTH} of the
 * rootPane's contentPane</li>
 * </ul>
 * <p>
 * To show or hide a View, you call the corresponding Application methods. Here
 * is a simple example:</p>
 * <pre>
 * class MyApplication extends SingleFrameApplication {
 *     &#064;Override protected void startup() {
 *         View view = getMainView();
 *         view.setComponent(createMainComponent());
 *         view.setMenuBar(createMenuBar());
 *         show(view);
 *     }
 * }
 * </pre>
 * <p>
 * The advantage of Views over just configuring a JFrame directly, is that a
 * View is more easily moved to an alternative top level container, like a
 * docking framework.</p>
 *
 * @see JRootPane
 * @see Application#show(View)
 * @see Application#hide(View)
 *
 * @author Hans Muller (Original Author)
 * @author Sean Carrick (Adapting Author) &lt;sean at pekinsoft dot com&gt;
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
     *          View
     * @see Application#show(View)
     * @see Application#hide(View)
     */
    public View(Application application) {
        if (application == null) {
            throw new IllegalArgumentException("null application");
        }

        this.application = application;
    }
    
    /**
     * The {@code Application} that is responsible for showing/hiding this View.
     * 
     * @return the Application that owns this View
     */
    public final Application getApplication() {
        return application;
    }
    
    /**
     * The {@code ApplicationContext} for the {@code Application} that is 
     * responsible for showing/hiding this View. This method is just shorthand 
     * for {@code getApplication().getContext()}.
     * 
     * @return the ApplicationContext for the Application that owns this View
     * @see #getApplication
     * @see Application#show(View)
     * @see Application#hide(View)
     */
    public final ApplicationContext getContext() {
        return getApplication().getContext();
    }
    
    /**
     * The {@code ResourceMap} for this View. This method is just shorthand for
     * {@code getContext().getResourceMap(getClass(), View.class)}.
     * 
     * @return the {@code ResourceMap} for this View
     * @see #getContext
     */
    public ResourceMap getResourceMap() {
        if (resourceMap == null) {
            resourceMap = getContext().getResourceMap(getClass(), View.class);
        }
        
        return resourceMap;
    }
    
    /**
     * The {@code JRootPane} for this View. All of the components for this View
     * must be added to its rootPane. Most applications will do so by setting 
     * the View's {@code component}, {@code menuBar}, {@code toolBar}, and
     * {@code statusBar} properties.
     * 
     * @return the {@code rootPane} for this View
     * @see #setComponent
     * @see #setMenuBar
     * @see #setToolBar
     * @see #setStatusBar
     */
    public JRootPane getRootPane() {
        if (rootPane == null) {
            rootPane = new JRootPane();
            rootPane.setOpaque(true);
        }
        
        return rootPane;
    }
    
    private void replaceContentPaneChild(JComponent oldChild, 
            JComponent newChild, String constraint) {
        if (oldChild != null) {
            getRootPane().remove(oldChild);
        }
        if (newChild != null) {
            getRootPane().add(newChild, constraint);
        }
    }
    
    /**
     * The main {@code JComponent} for this View.
     * 
     * @return the {@code component} for this View
     * @see #setComponent
     */
    public JComponent getComponent() {
        return component;
    }
    
    /**
     * Set the single main Component for this View. It is added to the {@code 
     * BorderLayout.CENTER} of the rootPane's contentPane. if the component
     * property was already set, the old component is removed first.
     * <p>
     * This is a bound property. The default value is null.</p>
     * 
     * @param component the {@code component} for this View
     * @see #getComponent
     */
    public void setComponent(JComponent component) {
        JComponent oldValue = this.component;
        this.component = component;
        replaceContentPaneChild(oldValue, this.component, BorderLayout.CENTER);
        firePropertyChange("component", oldValue, this.component);
    }
    
    /**
     * The main {@code JMenuBar} for this View.
     * 
     * @return the {@code menuBar} for this View
     * @see #setMenuBar
     */
    public JMenuBar getMenuBar() {
        return menuBar;
    }
    
    /**
     * Sets the {@code JMenuBar} for this View.
     * <p>
     * This is a bound property. The default value is null.</p>
     * 
     * @param menuBar the {@code JMenuBar} for this View.
     * @see #getMenuBar
     */
    public void setMenuBar(JMenuBar menuBar) {
        JMenuBar oldValue = getMenuBar();
        this.menuBar = menuBar;
        getRootPane().setJMenuBar(menuBar);
        firePropertyChange("menuBar", oldValue, this.menuBar);
    }
    
    /**
     * The {@code JToolBar} list for this View.
     * 
     * @return all {@code JTooBar}s owned by this View
     * @see #setToolBars(List)
     */
    public List<JToolBar> getToolBars() {
        return toolBars;
    }
    
    /**
     * Sets the list of {@code JToolBar}s for this View.
     * <p>
     * This is a bound property. The default value is null.</p>
     * 
     * @param toolBars the list of {@code JToolBar}s
     * @see #getToolBars
     */
    public void setToolBars(List<JToolBar> toolBars) {
        if (toolBars == null) {
            throw new IllegalArgumentException("null toolBars");
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
        
        replaceContentPaneChild(oldToolBarsPanel, newToolBarsPanel, 
                BorderLayout.NORTH);
        firePropertyChange("toolBars", oldValue, this.toolBars);
    }
    
    /**
     * The primary (or main) {@code JToolBar} for this View.
     * 
     * @return the primary (or main) {@code JToolBar} for this View
     * @see #setToolBar(JToolBar)
     */
    public final JToolBar getToolBar() {
        List<JToolBar> toolBars = getToolBars();
        return (toolBars.size() == 0) ? null : toolBars.get(0);
    }
    
    /**
     * Sets the primary (or main) {@code JToolBar} for this View.
     * <p>
     * This is a bound property. The default value is null.</p>
     * 
     * @param toolBar the primary (or main) {@code JToolBar} for this View
     * @see #getToolBar
     */
    public void setToolBar(JToolBar toolBar) {
        JToolBar oldValue = getToolBar();
        List<JToolBar> toolBars = Collections.emptyList();
        if (toolBar != null) {
            toolBars = Collections.singletonList(toolBar);
        }
        
        setToolBars(toolBars);
        firePropertyChange("toolBar", oldValue, toolBar);
    }
    
    /**
     * The status bar for this View.
     * 
     * @return the status bar for this View
     * @see setStatusBar(JComponent)
     */
    public JComponent getStatusBar() {
        return statusBar;
    }
    
    /**
     * Sets the status bar for this View.
     * <p>
     * This is a bound property. The default value is null.</p>
     * 
     * @param statusBar the status bar for this View
     * @see #getStatusBar
     */
    public void setStatusBar(JComponent statusBar) {
        JComponent oldValue = this.statusBar;
        this.statusBar = statusBar;
        replaceContentPaneChild(oldValue, this.statusBar, BorderLayout.SOUTH);
        firePropertyChange("statusBar", oldValue, this.statusBar);
    }
    
}
