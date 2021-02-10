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
 * Class      :   ApplicationActionMap.java
 * Author     :   Sean Carrick <sean at pekinsoft dot com>
 * Created    :   Jan 30, 2021 @ 9:03:07 AM
 * Modified   :   Jan 30, 2021
 *  
 * Purpose:
 * 	
 * Revision History:
 *  
 * WHEN          BY                  REASON
 * ------------  ------------------- -------------------------------------------
 * Jan 30, 2021     Sean Carrick             Initial creation.
 * *****************************************************************************
 */

package org.jdesktop.application;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.ActionMap;

/**
 * An {@link javax.swing.ActionMap} class where each entry corresponds to an 
 * `@Action` method from a single `actionsClass` (i.e., a
 * class that contains one or more `@Action`s). Each entry's key is
 * the `@Action`'s {@link ApplicationAction` that calls the `
 * @Action}s method. For example, the code below prints `"Hello World"
 * }:
 * <pre>
 * public class HelloWorldActions {
 *     public @Action void Hello() {
 *         System.out.print("Hello "};
 *     }
 *     
 *     public @Action void World() {
 *         System.out.println("World");
 *     }
 * // ...
 * ApplicationActionMap appAM = new ApplicationActionMap(SimpleActions.class);
 * ActionEvent e = new ActionEvent("no src", ActionEvent.ACTION_PERFORMED, "no cmd");
 * appAM.get("Hello").actionPerformed(e);
 * appAM.get("World").actionPerformed(e);
 * }
 * </pre>
 * <p>
 * If a `ResourceMap` is provided, then each `ApplicationAction`'s
 * ({@link javax.swing.Action#putValue(java.lang.String, java.lang.Object) putValue},
 * {@link javax.swing.Action#getValue(java.lang.String) getValue}) properties
 * are initialized from the ResourceMap.</p>
 * <p>
 * TBD: Explain use of resourceMap, including action types, actionsObject, 
 * actionsClass, and ProxyActions.
 * 
 * @see ApplicationAction
 * @see ResourceMap
 *
 * @author Hans Muller (Original Author)
 * @author Sean Carrick (Adapting Author) &lt;sean at pekinsoft dot com&gt;
 * 
 * @version 1.05
 * @since 1.03
 */
public class ApplicationActionMap extends ActionMap {
    
    private final ApplicationContext context;
    private final ResourceMap resourceMap;
    private final Class actionsClass;
    private final Object actionsObject;
    private final List<ApplicationAction> proxyActions;
    
    /**
     * Creates a default instance of the ApplicationActionMap class.
     * 
     * @param context
     * @param actionsClass
     * @param actionsObject
     * @param resourceMap
     */
    public ApplicationActionMap (ApplicationContext context, Class actionsClass,
            Object actionsObject, ResourceMap resourceMap) {
        if (context == null) {
            throw new IllegalArgumentException("null context");
        }
        if (actionsClass == null) {
            throw new IllegalArgumentException("null actionsClass");
        }
        if (actionsObject == null) {
            throw new IllegalArgumentException("null actionsObject");
        }
        if (!(actionsClass.isInstance(actionsObject))) {
            throw new IllegalArgumentException("actionsObject not an instanceof"
                    + " actionsClass");
        }
        
        this.context = context;
        this.actionsClass = actionsClass;
        this.actionsObject = actionsObject;
        this.resourceMap = resourceMap;
        this.proxyActions = new ArrayList<>();
        addAnnotationActions(resourceMap);
        maybeAddActionsPCL();
    }
    
    public final ApplicationContext getContext() {
        return context;
    }
    
    public final Class getActionsClass() {
        return actionsClass;
    }
    
    public final Object getActionsObject() {
        return actionsObject;
    }
    
    /**
     * All of the `@ProxyAction`s recursively defined by this `
     * ApplicationActionMap} and its parent ancestors.
     * <p>
     * Returns a read-only list of the `@ProxyAction`s defined by this
     * `ApplicationActionMap`'s `actionClass` and, recursively, by
     * this `ApplicationActionMap`'s parent. If there are no proxyActions,
     * an empty list is returned.</p>
     * 
     * @return a list of all the proxyActions for this `ApplicationActionMap`,
     *          or an empty list if none
     */
    public List<ApplicationAction> getProxyActions() {
        // TBD: proxyActions that shadow should be merged.
        ArrayList<ApplicationAction> allProxyActions = new ArrayList<>(proxyActions);
        ActionMap parent = getParent();
        while (parent != null) {
            if (parent instanceof ApplicationActionMap) {
                allProxyActions.addAll(((ApplicationActionMap)parent).proxyActions);
            }
            parent = parent.getParent();
        }
        
        return Collections.unmodifiableList(allProxyActions);
    }
    
    private String aString(String s, String emptyValue) {
        return (s.length() == 0) ? emptyValue : s;
    }
    
    private void putAction(String key, ApplicationAction action) {
        if (get(key) != null) {
            // TODO: log a warning - two actions with the same key.
        }
        put(key, action);
    }
    
    /**
     * Add Actions for each actionsClass method with an @Action annotation and
     * for the class's @ProxyActions annotation.
     * 
     * @param resourceMap 
     */
    private void addAnnotationActions(ResourceMap resourceMap) {
        Class<?> actionsClass = getActionsClass();
        
        // @Action
        for (Method m : actionsClass.getDeclaredMethods()) {
            Action action = m.getAnnotation(Action.class);
            
            if (action != null) {
                String methodName = m.getName();
                String enabledProperty = aString(action.enabledProperty(), null);
                String selectedProperty = aString(action.selectedProperty(), null);
                String actionName = aString(action.name(), methodName);
                Task.BlockingScope block = action.block();
                ApplicationAction appAction = new ApplicationAction(this, 
                        resourceMap, actionName, m, enabledProperty, 
                        selectedProperty, block);
                put(actionName, appAction);
            }
        }
        
        // @ProxyAction
        ProxyActions proxyActionsAnnotation = actionsClass.getAnnotation(ProxyActions.class);
        if (proxyActionsAnnotation != null) {
            for (String actionName : proxyActionsAnnotation.value()) {
                ApplicationAction appAction = new ApplicationAction(this, 
                        resourceMap, actionName);
                appAction.setEnabled(false); // Will track the enabled property
                                             //+ of the Actions it's bound to.
                putAction(actionName, appAction);
                proxyActions.add(appAction);
            }
        }
    }
    
    /**
     * If any of the ApplicationActions need to track an enabled or selected
     * property defined in actionsClass, then add our PropertyChangeListener. If
     * none of the @Actions in actionClass provide an enabledProperty or 
     * selectedProperty argument, then we don't need to do this.
     */
    private void maybeAddActionsPCL() {
        boolean needsPCL = false;
        Object[] keys = keys();
        
        if (keys != null) {
            for (Object key : keys) {
                javax.swing.Action value = get(key);
                
                if (value instanceof ApplicationAction) {
                    ApplicationAction actionAdapter = (ApplicationAction) value;
                    if ((actionAdapter.getEnabledProperty() != null) 
                            || (actionAdapter.getSelectedProperty() != null)) {
                        needsPCL = true;
                        break;
                    }
                }
            }
            
            if (needsPCL) {
                try {
                    Class actionsClass = getActionsClass();
                    Method m = actionsClass.getMethod("addPropertyChangeListener", 
                            PropertyChangeListener.class);
                    m.invoke(getActionsObject(), new ActionsPCL());
                } catch (Exception ex) {
                    String s = "addPropertyChangeListener undefinded " 
                            + actionsClass;
                    throw new Error(s, ex);
                }
            }
        }
    }
    
    private class ActionsPCL implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            String propertyName = evt.getPropertyName();
            Object[] keys = keys();
            
            if (keys != null) {
                for (Object key : keys) {
                    javax.swing.Action value = get(key);
                    if (value instanceof ApplicationAction) {
                        ApplicationAction appAction = (ApplicationAction) value;
                        
                        if (propertyName.equals(appAction.getEnabledProperty())) {
                            appAction.forwardPropertyChangeEvent(evt, "enabled");
                        } else if (propertyName.equals(appAction.getSelectedProperty())) {
                            appAction.forwardPropertyChangeEvent(evt, "selected");
                        }
                    }
                }
            }
        }
    }

}
