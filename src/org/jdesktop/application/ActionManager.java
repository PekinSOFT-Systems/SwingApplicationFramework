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
 *  Class      :   ActionManager.java
 *  Author     :   Sean Carrick
 *  Created    :   Feb 10, 2021 @ 7:37:11 PM
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

import java.awt.KeyboardFocusManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;
import java.util.logging.Logger;
import javax.swing.ActionMap;
import javax.swing.JComponent;

/**
 * The application's <code>ActionManager</code> provides read-only cached access
 * to <code>ActionMap</code>s that contain one entry for each method marked with
 * the <code>@Action</code> annotation in a class.
 *
 *
 * @see ApplicationContext#getActionMap(java.lang.Object) 
 * @see ApplicationActionMap
 * @see ApplicationAction
 * 
 * @author Hans Muller (Original Author) &lt;current email unknown&gt;
 * @author Sean Carrick (Updater) &lt;sean at pekinsoft dot com&gt;
 * 
 * @version 1.05
 * @since 1.03
 */
public class ActionManager extends AbstractBean {

    private static final Logger logger = Logger.getLogger(ActionManager.class.getName());
    private final ApplicationContext context;
    private final WeakHashMap<Object, WeakReference<ApplicationActionMap>> 
            actionMaps;
    private ApplicationActionMap globalActionMap = null;

    protected ActionManager(ApplicationContext context) {
        if (context == null) {
            throw new IllegalArgumentException("null context");
        }
        this.context = context;
        actionMaps = new WeakHashMap<>();
    }

    protected final ApplicationContext getContext() {
        return context;
    }

    private ApplicationActionMap createActionMapChain(
            Class startClass, Class stopClass, Object actionsObject, 
            ResourceMap resourceMap) {
        // All of the classes from stopClass to startClass, inclusive. 
        List<Class> classes = new ArrayList<>();
        for (Class c = startClass;; c = c.getSuperclass()) {
            classes.add(c);
            if (c.equals(stopClass)) {
                break;
            }
        }
        Collections.reverse(classes);
        // Create the ActionMap chain, one per class
        ApplicationContext ctx = getContext();
        ApplicationActionMap parent = null;
        for (Class cls : classes) {
            ApplicationActionMap appAM = new ApplicationActionMap(ctx, cls,
                    actionsObject, resourceMap);
            appAM.setParent(parent);
            parent = appAM;
        }
        return parent;
    }

    /**
     * The <code>ActionMap</code> chain for the entire <code>Application</code>.
     * <p>
     * Returns an <code>ActionMap</code> with the <code>@Action</code>s defined
     * in the application's <code>Application</code> subclass, i.e. the the
     * value of:
     * ```java
     * ApplicationContext.getInstance().getApplicationClass()
     * ```
     * <p>
     * The remainder of the chain contains one <code>ActionMap</code> for
     * each superclass, up to <code>Application.class</code>. The
     * <code>ActionMap.get()</code> method searches the entire chain, so
     * logically, the <code>ActionMap</code> that this method returns contains
     * all of the application-global actions.</p>
     * <p>
     * The value returned by this method is cached.</p>
     *
     * @return the <code>ActionMap</code> chain for the entire
     * <code>Application</code>.
     * @see #getActionMap(java.lang.Class, java.lang.Object)
     * @see ApplicationContext#getActionMap()
     * @see ApplicationContext#getActionMap(java.lang.Class, java.lang.Object)
     * @see ApplicationContext#getActionMap(java.lang.Object)
     */
    public ApplicationActionMap getActionMap() {
        if (globalActionMap == null) {
            ApplicationContext ctx = getContext();
            Object appObject = ctx.getApplication();
            Class appClass = ctx.getApplicationClass();
            ResourceMap resourceMap = ctx.getResourceMap();
            globalActionMap = createActionMapChain(appClass, Application.class, 
                    appObject, resourceMap);
            initProxyActionSupport();  // lazy initialization
        }
        return globalActionMap;
    }

    private void initProxyActionSupport() {
        KeyboardFocusManager kfm = 
                KeyboardFocusManager.getCurrentKeyboardFocusManager();
        kfm.addPropertyChangeListener(new KeyboardFocusPCL());
    }

    /**
     * Returns the <code>ApplicationActionMap</code> chain for the specified
     * actions class and target object.
     * <p>
     * The specified class can contain methods marked with the
     * <code>@Action</code> annotation. Each one will be turned into an
     * {@link ApplicationAction ApplicationAction} object and all of them will
     * be added to a single {@link ApplicationActionMap ApplicationActionMap}.
     * All of the <code>ApplicationAction</code>s invoke their
     * <code>actionPerformed</code> method on the specified
     * <code>actionsObject</code>. The parent of the returned
     * <code>ActionMap</code> is the global <code>ActionMap</code> that contains
     * the <code>@Action</code>s defined in this application's
     * <code>Application</code> subclass.</p>
     * <p>
     * To bind an <code>@Action</code> to a Swing component, one specifies the
     * <code>@Action</code>'s name in an expression like this:</p>
     * ```java
     * ApplicationContext ctx = Application.getInstance(MyApplication.class).getContext();
     * MyActions myActions = new MyActions();
     * myComponent.setAction(ac.getActionMap(myActions).get("myAction"));
     * ```
     * <p>
     * The value returned by this method is cached. The lifetime of the cached
     * entry will be the same as the lifetime of the <code>actionsObject</code>
     * and the <code>ApplicationActionMap</code> and <code>ApplicationAction</code>s
     * that refer to it. In other words, if you drop all references to the 
     * <code>actionsObject</code>, including its <code>ApplicationAction</code>s
     * and their <code>ApplicationActionMaps</code>, then the cached 
     * <code>ActionMap</code> entry will be cleared.</p>
     *
     * @param actionsClass the class that contains the action of interest
     * @param actionsObject the object upon which the actions should take place
     * @return an <code>ApplicationActionMap</code> of all of the registered
     *          actions
     * 
     * @see #getActionMap()
     * @see ApplicationContext#getActionMap()
     * @see ApplicationContext#getActionMap(java.lang.Class, java.lang.Object) 
     * @see ApplicationContext#getActionMap(java.lang.Object)
     */
    public ApplicationActionMap getActionMap(Class actionsClass, 
            Object actionsObject) {
        if (actionsClass == null) {
            throw new IllegalArgumentException("null actionsClass");
        }
        if (actionsObject == null) {
            throw new IllegalArgumentException("null actionsObject");
        }
        if (!actionsClass.isAssignableFrom(actionsObject.getClass())) {
            throw new IllegalArgumentException("actionsObject not instanceof "
                    + "actionsClass");
        }
        synchronized (actionMaps) {
            WeakReference<ApplicationActionMap> ref = actionMaps.get(
                    actionsObject);
            ApplicationActionMap classActionMap = (ref != null) ? ref.get() 
                    : null;
            if ((classActionMap == null) || (classActionMap.getActionsClass() 
                    != actionsClass)) {
                ApplicationContext ctx = getContext();
                Class actionsObjectClass = actionsObject.getClass();
                ResourceMap resourceMap = ctx.getResourceMap(actionsObjectClass, 
                        actionsClass);
                classActionMap = createActionMapChain(actionsObjectClass, 
                        actionsClass, actionsObject, resourceMap);
                ActionMap lastActionMap = classActionMap;
                while (lastActionMap.getParent() != null) {
                    lastActionMap = lastActionMap.getParent();
                }
                lastActionMap.setParent(getActionMap());
                actionMaps.put(actionsObject, new WeakReference(classActionMap));
            }
            return classActionMap;
        }
    }

    private final class KeyboardFocusPCL implements PropertyChangeListener {

        private final TextActions textActions;

        KeyboardFocusPCL() {
            textActions = new TextActions(getContext());
        }

        @Override
        public void propertyChange(PropertyChangeEvent e) {
            if ("permanentFocusOwner".equals(e.getPropertyName())) {
                JComponent oldOwner = getContext().getFocusOwner();
                Object newValue = e.getNewValue();
                JComponent newOwner = (newValue instanceof JComponent) ? (JComponent) newValue : null;
                textActions.updateFocusOwner(oldOwner, newOwner);
                getContext().setFocusOwner(newOwner);
                updateAllProxyActions(oldOwner, newOwner);
            }
        }
    }

    /* For each proxyAction in each ApplicationActionMap, if
     * the newFocusOwner's ActionMap includes an Action with the same
     * name then bind the proxyAction to it, otherwise set the proxyAction's 
     * proxyBinding to null.  [TBD: synchronize access to actionMaps]
     */
    private void updateAllProxyActions(JComponent oldFocusOwner, JComponent newFocusOwner) {
        if (newFocusOwner != null) {
            ActionMap ownerActionMap = newFocusOwner.getActionMap();
            if (ownerActionMap != null) {
                updateProxyActions(getActionMap(), ownerActionMap, newFocusOwner);
                actionMaps.values().stream().map(appAMRef -> 
                        appAMRef.get()).filter(appAM -> 
                                !(appAM == null)).forEachOrdered(appAM -> {
                    updateProxyActions(appAM, ownerActionMap, newFocusOwner);
                });
            }
        }
    }

    /* For each proxyAction in appAM: if there's an action with the same
     * name in the focusOwner's ActionMap, then set the proxyAction's proxy
     * to the matching Action.  In other words: calls to the proxyAction
     * (actionPerformed) will delegate to the matching Action.
     */
    private void updateProxyActions(ApplicationActionMap appAM, ActionMap ownerActionMap, JComponent focusOwner) {
        appAM.getProxyActions().forEach(proxyAction -> {
            String proxyActionName = proxyAction.getName();
            javax.swing.Action proxy = ownerActionMap.get(proxyActionName);
            if (proxy != null) {
                proxyAction.setProxy(proxy);
                proxyAction.setProxySource(focusOwner);
            } else {
                proxyAction.setProxy(null);
                proxyAction.setProxySource(null);
            }
        });
    }
}
