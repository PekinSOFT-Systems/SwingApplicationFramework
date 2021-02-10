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
 * Class      :   ApplicationAction.java
 * Author     :   Sean Carrick <sean at pekinsoft dot com>
 * Created    :   Jan 30, 2021 @ 2:08:08 PM
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

import org.jdesktop.application.utils.Logger;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.KeyStroke;

/**
 * The {@link javax.swing.Action} class used to implement the
 * <tt>@Action</tt> annotation. This class is typically not instantiated directly, it's 
 * created as a side effect of constructing an `ApplicationActionMap`:
 * ```java
 * public class MyActions {
 *     @Action
 *     public void anAction() {
 *         // An @Action named "anAction"
 *     }
 * }
 *
 * ApplicationContext ac = ApplicationContext.getInstance();
 * ActionMap actionMap = ac.getActionMap(new MyActions());
 * myButton.setAction(actionMap.get("anAction"));
 * ```
 * <p>
 * When an ApplicationAction is constructed, it initializes all of its
 * properties from the specified `ResourceMap`. Resource names must match
 * the `@Action`'s name, which is the name of the corresponding method, or
 * the value of the optional <tt>@Action</tt> name parameter. To initialize the
 * text and shortDescription properties of the action named `"anAction"`
 * in the previous example, one would define two resources:
 * <pre>
 * anAction.Action.text = Button/Menu/etc label text for anAction
 * anAction.Ation.shortDescription = Tooltip text for anAction
 * </pre>
 * <p>
 * A complete description of the mapping between resources and Action properties
 * can be found in the ApplicationAction {@link #ApplicationAction constructor}
 * documentation.</p>
 * <p>
 * An ApplicationAction's <tt>enabled` and `selected</tt> properties can
 * be delegated to <tt>boolean</tt> properties of the Actions class, by specifying the
 * corresponding property names. This can be done with the `@Action`
 * annotation, e.g.:</p>
 * ```java
 * public class MyActions {
 *     @Action(enabledProperty = "anActionEnabled")
 *     public void anAction() { }
 *     public boolean isAnActionEnabled() {
 *         // Will fire PropertyChange when anActionEnabled changes.
 *         return anActionEnabled;
 *     }
 * }
 * ```
 * <p>
 * If the MyActions class supports PropertyChange events, then the
 * ApplicationAction will track the state of the specified property
 * ("anActionEnabled" in this case) with a PropertyChangeListener.</p>
 * <p>
 * ApplicationActions can automatically <tt>block</tt> the GUI while the
 * <tt>actionPerformed</tt> method is running, depending on the value of block
 * annotation parameter. For example, if the value of block is
 * `Task.BlockingScope.ACTION`, then the action will be disabled while
 * the actionPerformed method runs.</p>
 * <p>
 * An ApplicationAction can have a <tt>proxy</tt> Action, i.e., another Action
 * that provides the <tt>actionPerformed</tt> method, the enabled/selected
 * properties, and values for the Action's long and short descriptions. If the
 * proxy property is set, this ApplicationAction tracks all of the
 * aforementioned properties, and the <tt>actionPerformed</tt> method just calls
 * the proxy's
 * <tt>actionPerformed` method. If a `proxySource</tt> is specified, then
 * it becomes the source of the ActionEvent that's passed to the proxy
 * <tt>actionPeformed</tt> method. Proxy action dispatching is as simple as
 * this:
 * ```java
 * public void actionPeformed(ActionEvent actionEvent) {
 *     javax.swing.Action proxy = getProxy();
 *     if (proxy != null) {
 *         actionEvent.setSource(getProxySource());
 *         proxy.actionPerformed(actionEvent);
 *     }
 *     // ...
 * }
 * ```
 *
 * @author Hans Muller (Original Author)
 * @author Sean Carrick (Adapting Author) &lt;sean at pekinsoft dot com&gt;
 *
 * @see ApplicationContext#getActionMap(Object)
 * @see ResourceMap
 *
 * @version 1.05
 * @since 1.03
 */
public class ApplicationAction extends AbstractAction {

    private static final Logger logger = Logger.getLogger(
            ApplicationAction.class.getName());
    private final ApplicationActionMap appAM;
    private final ResourceMap resourceMap;
    private final String actionName;        // See getName()
    private final Method actionMethod;      // See @Action method
    private final String enabledProperty;   // names a bound appAM.getActionsClass() property
    private final Method isEnabledMethod;   // Method object for is/getEnabledProperty
    private final Method setEnabledMethod;  // Method object for setEnabledProperty
    private final String selectedProperty;  // names a bound appAM.getActionsClass() property
    private final Method isSelectedMethod;  // Method object for is/getSelectedProperty
    private final Method setSelectedMethod;   // Method object for setSelectedProperty
    private final Task.BlockingScope block;
    private javax.swing.Action proxy = null;
    private Object proxySource = null;
    private PropertyChangeListener proxyPCL = null;

    /**
     * Construct an <tt>ApplicationAxtion</tt> that implements an `@Action`.
     * <p>
     * If a <tt>ResourceMap</tt> is provided, then all fo the
     * {@link javax.swing.Action Action} properties are initialized with the
     * values of resources whose key begins with `baseName`. `ResourceMap`
     * keys are created by appending an <tt>@Action</tt> resource name, like
     * "`Action.shortDescription`", to the `@Action`'s baseName. For example,
     * given an <tt>@Action</tt> defined like this:</p>
     * ```java
     * @Action void actionBaseName() { }
     * ```
     * <p>
     * Then the shortDescription resource key would be
     * `actionBaseName.Action.shortDescription`, as in:</p>
     * <pre>
     * actionBaseName.Action.shortDescription = Do perform some action
     * </pre>
     * <p>
     * The complete set of @Action resources is:</p>
     * <pre>
     * Action.icon
     * Action.text
     * Action.shortDescription
     * Action.longDescription
     * Action.smallIcon
     * Action.largeIcon
     * Action.command
     * Action.accelerator
     * Action.mnemonic
     * Action.displayedMnemonicIndex
     * </pre>
     * <p>
     * A few of the resources are handled specially:</p>
     * <ul><li>`Action.text`<br>Used to initialize the Action properties
     * with keys `Action.NAME`, `Action.MNEMONIC_KEY`, and
     * `Action.DISPLAYED_MNEMONIC_INDEX`.<br><hr><br>If the resource's
     * value contains an "&" or an "_", it is assumed to mark the following
     * character as the mnemonic. If `Action.mnemonic`/`Action.displayedMnemonic`
     * resources are also defined (an odd case), they will override the mnemonic
     * specified with the Action.text marker character.</li>
     * <li>`Action.icon`<br>Used to initialize both <tt>Action.SMALL_ICON</tt> and 
     * `Action.LARGE_ICON`.<br><hr><br>If Action.smallIcon or
     * Action.largeIcon resources are also defined, they will override the value
     * defined for Action.icon.</li>
     * <li>`Action.displayedMnemonicIndexKey`<br>The corresponding
     * <tt>javax.swing.Action</tt> constant is only defined in Java SE 6. We will set the
     * Action property in earlier versions of Java, as well.<br><hr><br>
     * This has been deprecated as of Swing Application Framework 1.05 due to
     * the framework being updated to Java 11.</li></ul>
     *
     * @param appAM the <tt>ApplicationActionMap</tt> for which this action is being
     * constructed
     * @param resourceMap initial <tt>Action</tt> properties are loaded from this
     * `ResourceMap`
     * @param baseName the name of the `@Action`
     * @param actionMethod unless a proxy is specified, <tt>actionPerformed</tt> calls
     * this method
     * @param enabledProperty name of the enabled property
     * @param selectedProperty name of the selected property
     * @param block how much of the GUI to block while this action executes
     *
     * @see #getName
     * @see ApplicationActionMap#getActionsClass()
     * @see ApplicationActionMap#getActionsObject()
     */
    public ApplicationAction(ApplicationActionMap appAM, ResourceMap resourceMap,
            String baseName, Method actionMethod, String enabledProperty,
            String selectedProperty, Task.BlockingScope block) {
        if (appAM == null) {
            throw new IllegalArgumentException("null appAM");
        }
        if (baseName == null) {
            throw new IllegalArgumentException("null baseName");
        }

        this.appAM = appAM;
        this.resourceMap = resourceMap;
        this.actionName = baseName;
        this.actionMethod = actionMethod;
        this.enabledProperty = enabledProperty;
        this.selectedProperty = selectedProperty;
        this.block = block;

        /* If enabledProperty is specified, lookup the is/set methods and verify
         * the former exists.
         */
        if (enabledProperty != null) {
            setEnabledMethod = propertySetMethod(enabledProperty, boolean.class);
            isEnabledMethod = propertyGetMethod(enabledProperty);

            if (isEnabledMethod == null) {
                throw newNoSuchPropertyException(enabledProperty);
            }
        } else {
            this.isEnabledMethod = null;
            this.setEnabledMethod = null;
        }

        /* If selectedProperty is specified, lookup the is/set methods and verify
         * the former exists.
         */
        if (selectedProperty != null) {
            setSelectedMethod = propertySetMethod(selectedProperty, boolean.class);
            isSelectedMethod = propertyGetMethod(selectedProperty);

            if (isSelectedMethod == null) {
                throw newNoSuchPropertyException(selectedProperty);
            }

            super.putValue(SELECTED_KEY, Boolean.FALSE);
        } else {
            this.isSelectedMethod = null;
            this.setSelectedMethod = null;
        }

        if (resourceMap != null) {
            initActionProperties(resourceMap, baseName);
        }
    }

    /**
     * Shorter convenience constructor used to create `ProxyActions`.
     *
     * @see ApplicationActionMap#getProxyActions() 
     *
     * @param appAM the ApplicationActionMap for which this action is being
     * constructed
     * @param resourceMap initial Action properties are loaded from this
     * ResourceMap
     * @param actionName the name of the @Action
     */
    public ApplicationAction(ApplicationActionMap appAM, ResourceMap resourceMap,
            String actionName) {
        this(appAM, resourceMap, actionName, null, null, null,
                Task.BlockingScope.NONE);
    }

    private IllegalArgumentException newNoSuchPropertyException(
            String propertyName) {
        String actionsClassName = appAM.getActionsClass().getName();
        String msg = String.format("no property named %s in %s", propertyName,
                actionsClassName);
        return new IllegalArgumentException(msg);
    }

    /**
     * The name of the <tt>@Action` `enabledProperty</tt> whose value is returned
     * by {@link #isEnabled() `isEnabled``, or `null`.
     *
     * @return the name of the <tt>enabledProperty</tt> or `null`
     *
     * @see #isEnabled()
     */
    public String getEnabledProperty() {
        return enabledProperty;
    }

    /**
     * The name fo the <tt>@Action` `selectedProperty</tt> whose value is returned
     * by {@link #isSelected `isSelected``, or `null`.
     *
     * @return the name of the <tt>selectedProperty</tt> or `null`
     *
     * @see #isSelected
     */
    public String getSelectedProperty() {
        return selectedProperty;
    }

    /**
     * Return the proxy for this action or `null`
     *
     * @return the value of the proxy property
     * @see #setProxy
     * @see #setProxySource
     * @see #actionPerformed(java.awt.event.ActionEvent) 
     */
    public javax.swing.Action getProxy() {
        return proxy;
    }

    /**
     * Set the proxy for this action. If the proxy is non-`null` then we delegate
     * and/or track the following:
     * <ul>
     * <li>`actionPerformed`<br>Our <tt>actionPerformed</tt> method calls
     * the delegate's after the ActionEvent source to be the value of
     * `getProxySource`</li>
     * <li>`shortDescription`<br>If the proxy's `shortDescription`, i.e.,
     * the value for key {@link javax.swing.Action#SHORT_DESCRIPTION
     * <tt>SHORT_DESCRIPTION`</tt> is not `null`, then set this action's `shortDescription`.
     * Most Swing components use the <tt>shortDescription</tt> to initialize their
     * tooltip.</li>
     * <li>`longDescription`<br>If the proxy's `longDescription`, i.e., the
     * value for {@link javax.swing.Action#LONG_DESCRIPTION <tt>LONG_DESCRIPTION`</tt> is
     * not `null`, then set this action's `longDescription`.</li></ul>
     *
     * @see #setProxy(javax.swing.Action)
     * @see #setProxySource
     * @see #actionPerformed(java.awt.event.ActionEvent)
     *
     * @param proxy the proxy action
     */
    public void setProxy(javax.swing.Action proxy) {
        javax.swing.Action oldProxy = this.proxy;
        this.proxy = proxy;

        if (oldProxy != null) {
            oldProxy.removePropertyChangeListener(proxyPCL);
            proxyPCL = null;
        }

        if (this.proxy != null) {
            updateProxyProperties();
            proxyPCL = new ProxyPCL();
            proxy.addPropertyChangeListener(proxyPCL);
        } else if (oldProxy != null) {
            setEnabled(false);
            setSelected(false);
        }

        firePropertyChange("proxy", oldProxy, this.proxy);
    }

    /**
     * Return the value that becomes the <tt>ActionEvent</tt> source before the
     * <tt>ActionEvent</tt> is passed along to the proxy `Action`.
     *
     * @see #getProxy()
     * @see #setProxySource
     * @see ActionEvent#getSource()
     *
     * @return source of the `ActionEvent`
     */
    public Object getProxySource() {
        return proxySource;
    }

    /**
     * Set the value that becomes the <tt>ActionEvent</tt> source before the
     * <tt>ActionEvent</tt> is passed along to the proxy `Action`.
     *
     * @param source the <tt>ActionEvent</tt> source
     * @see #getProxy()
     * @see #getProxySource()
     * @see ActionEvent#setSource(java.lang.Object)
     */
    public void setProxySource(Object source) {
        Object oldValue = this.proxySource;
        this.proxySource = source;
        firePropertyChange("proxySource", oldValue, this.proxySource);
    }

    private void maybePutDescriptionValue(String key, javax.swing.Action proxy) {
        Object s = proxy.getValue(key);
        if (s instanceof String) {
            putValue(key, (String) s);
        }
    }

    private void updateProxyProperties() {
        javax.swing.Action proxy = getProxy();

        if (proxy != null) {
            setEnabled(proxy.isEnabled());
            Object s = proxy.getValue(SELECTED_KEY);
            setSelected((s instanceof Boolean) && ((Boolean) s).booleanValue());
            maybePutDescriptionValue(javax.swing.Action.SHORT_DESCRIPTION, proxy);
            maybePutDescriptionValue(javax.swing.Action.LONG_DESCRIPTION, proxy);
        }
    }

    /* This PCL is added to the proxy action, i.e., getProxy(). We track the
     * folowing properties of the proxy action we are bound to: enabled, 
     * selected, longDescription, and shortDescription. We only mirror the
     * description properties if they are non-null.
     */
    private class ProxyPCL implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            String propertyName = evt.getPropertyName();

            if ((propertyName == null) || "enabled".equals(propertyName)
                    || "selected".equals(propertyName)
                    || javax.swing.Action.SHORT_DESCRIPTION.equals(propertyName)
                    || javax.swing.Action.LONG_DESCRIPTION.equals(propertyName)) {
                updateProxyProperties();
            }
        }

    }

    /* Original source had definitions for SELECTED_KEY, DISPLAYED_MNEMONIC_INDEX_KEY,
     * and LARGE_ICON_KEY placed here, with the explanAction that these constants
     * were only defined in Mustang (1.6), and referenced the JDK6 API 
     * documentation. As we are updating this framework library to a more modern
     * Java version (JDK11), we no longer need to have those three constants 
     * defined internally, therefore we have eliminated those definitions. We
     * are placing this comment here for the sake of posterity, also why the
     * definitions are made below.
     *
     * private static final String SELECTED_KEY = "SwingSelectedKey";
     * private static final String DESPLAYED_MNEMONIC_INDEX_KEY = 
     *         "SwingDisplayedMnemonicIndexKey";
     * private static final String LARGE_ICON_KEY = "SwingLargeIconKey";
     *
     */
 /* Init all of the javax.swing.Action properties for the @Action named
     * actionName.
     */
    private void initActionProperties(ResourceMap resourceMap, String baseName) {
        boolean iconOrNameSpecified = false; // true if Action's icon/name
        //+ properties set
        String typedName = null;

        // Action.text => Action.NAME, MNEMONIC_KEY, DISPLAYED_MNEMONIC_INDEX_KEY
        String text = resourceMap.getString(baseName + ".Action.text");

        if (text != null) {
            MnemonicText.configure(this, text);
            iconOrNameSpecified = true;
        }

        // Action.mnemonic => Action.MNEMONIC_KEY
        Integer mnemonic = resourceMap.getKeyCode(baseName + ".Action.mnemonic");

        if (mnemonic != null) {
            putValue(javax.swing.Action.MNEMONIC_KEY, mnemonic);
        }

        // Action.displayedMnemonicIndex => Action.DISPLAYED_MNEMONIC_INDEX_KEY
        Integer index = resourceMap.getInteger(baseName
                + ".Action.displayedMnemonicIndex");

        if (index != null) {
            putValue(javax.swing.Action.DISPLAYED_MNEMONIC_INDEX_KEY, index);
        }

        // Action.accelerator => Action.ACCELERATOR_KEY
        KeyStroke key = resourceMap.getKeyStroke(baseName
                + ".Action.accelerator");

        if (key != null) {
            putValue(javax.swing.Action.ACCELERATOR_KEY, key);
        }

        // Action.icon => Action.SMALL_ICON, LARGE_ICON_KEY
        Icon icon = resourceMap.getIcon(baseName + ".Action.icon");

        if (icon != null) {
            putValue(javax.swing.Action.SMALL_ICON, icon);
            putValue(javax.swing.Action.LARGE_ICON_KEY, icon);
            iconOrNameSpecified = true;
        }

        // Action.smallIcon => Action.SMALL_ICON
        Icon smallIcon = resourceMap.getIcon(baseName + ".Action.smallIcon");

        if (smallIcon != null) {
            putValue(javax.swing.Action.SMALL_ICON, smallIcon);
            iconOrNameSpecified = true;
        }

        // Action.largeIcon => Action.LARGE_ICON
        Icon largeIcon = resourceMap.getIcon(baseName + ".Action.largeIcon");

        if (largeIcon != null) {
            putValue(javax.swing.Action.LARGE_ICON_KEY, largeIcon);
            iconOrNameSpecified = true;
        }

        // Action.shortDescription => Action.SHORT_DESCRIPTION
        putValue(javax.swing.Action.SHORT_DESCRIPTION,
                resourceMap.getString(baseName + ".Action.shortDescription"));

        // Action.longDescription => Action.LONG_DESCRIPTION
        putValue(javax.swing.Action.LONG_DESCRIPTION,
                resourceMap.getString(baseName + ".Action.longDescription"));

        // Action.command => Action.ACTION_COMMAND_KEY
        putValue(javax.swing.Action.ACTION_COMMAND_KEY,
                resourceMap.getString(baseName + ".Action.command"));

        // If no visual was defined for this Action, i.e., no text and no icon,
        //+ then we default Action.NAME.
        if (!iconOrNameSpecified) {
            putValue(javax.swing.Action.NAME, actionName);
        }
    }

    private String propertyMethodName(String prefix, String propertyName) {
        return prefix + propertyName.substring(0, 1).toUpperCase()
                + propertyName.substring(1);
    }

    private Method propertyGetMethod(String propertyName) {
        String[] getMethodNames = {
            propertyMethodName("is", propertyName),
            propertyMethodName("get", propertyName)
        };

        Class actionsClass = appAM.getActionsClass();

        for (String name : getMethodNames) {
            try {
                return actionsClass.getMethod(name);
            } catch (NoSuchMethodException ignore) {
                // Do nothing. Return null below.
            }
        }

        return null;
    }

    private Method propertySetMethod(String propertyName, Class type) {
        Class actionsClass = appAM.getActionsClass();

        try {
            return actionsClass.getMethod(propertyMethodName("set", propertyName),
                    type);
        } catch (NoSuchMethodException returnNull) {
            return null;
        }
    }

    /**
     * The name of this Action. This string begins with the name of the
     * corresponding @Action method (unless the <tt>name`</tt> `@Action`
     * parameter was specified.
     * <p>
     * This name is used as a prefix to look up action resources, and the
     * <tt>ApplicationContext` Framework uses it as the key for this `Action</tt> in the
     * `ApplicationActionMaps`.</p>
     * 
     * <dl><dt>Note:</dt><dd>This property should not be confused with the
     * {@link javax.swing.Action#NAME <tt>Action.NAME`</tt> key. That key is actually
     * used to initialize the <tt>text</tt> properties of Swing components,
     * which is why we call the corresponding <tt>ApplicationAction</tt> resource
     * "`Action.text`", as in:
     * <pre>
     * myCloseButton.Action.text = Close
     * </pre></dd></dl>
     *
     * @return the (read-only) value of the name property
     */
    public String getName() {
        return actionName;
    }

    /**
     * The resourceMap for this Action.
     *
     * @return the (read-only) value of the resourceMap property
     */
    public ResourceMap getResourceMap() {
        return resourceMap;
    }

    /**
     * Provides parameter values to <tt>@Action</tt> methods. By default, parameter
     * values are selected based exclusively on their type:
     * <table border=1>
     * <caption>Action Methods</caption>
     * <tr>
     * <th>Parameter Type</th>
     * <th>Parameter Value</th>
     * </tr>
     * <tr>
     * <td>`ActionEvent`</td>
     * <td>`actionEvent`</td>
     * </tr>
     * <tr>
     * <td>`javax.swing.Action`</td>
     * <td>This <tt>ApplicationAction</tt> object</td>
     * </tr>
     * <tr>
     * <td>`ActionMap`</td>
     * <td>The <tt>ActionMap</tt> that contains this `Action`</td>
     * </tr>
     * <tr>
     * <td>`ResourceMap`</td>
     * <td>The <tt>ResourceMap` of the `ActionMap</tt> that contains this
     * `Action`</td>
     * </tr>
     * <tr>
     * <td>`ApplicationContext`</td>
     * <td> The value of `ApplicationContext.getInstance()`</td>
     * </tr>
     * </table>
     * <p>
     * <tt>ApplicationAction</tt> subclasses may also select values based on the value of
     * the <tt>Action.Parameter</tt> annotation, which is passed along as the
     * <tt>pKey</tt> argument to this method:</p>
     * ```java
     * @Action
     * public void doAction(@Action.Parameter("myKey") String myParameter) {
     *     // The value of myParameter is computed by:
     *     //+ getActionArgument(String.class, "myKey", actionEvent)
     * }
     * ```
     * <p>
     * If <tt>pType` and `pKey</tt> are not recognized, this method calls
     * {@link #actionFailed} with an `IllegalArgumentException`.
     *
     * @param pType parameter type
     * @param pKey the value of the <tt>@Action.Parameter</tt> annotation
     * @param actionEvent the <tt>ActionEvent</tt> that triggered this Action
     * @return an <tt>Object</tt> representing this `ActionArgument`
     */
    protected Object getActionArgument(Class pType, String pKey,
            ActionEvent actionEvent) {
        Object argument = null;

        if (pType == ActionEvent.class) {
            argument = actionEvent;
        } else if (pType == javax.swing.Action.class) {
            argument = this;
        } else if (pType == ActionMap.class) {
            argument = appAM;
        } else if (pType == ResourceMap.class) {
            argument = resourceMap;
        } else if (pType == ApplicationContext.class) {
            argument = appAM.getContext();
        } else if (pType == Application.class) {
            argument = appAM.getContext().getApplication();
        } else {
            Exception e = new IllegalArgumentException("unrecognied @Action "
                    + "method parameter");
            actionFailed(actionEvent, e);
        }

        return argument;
    }

    private Task.InputBlocker createInputBlocker(Task task, ActionEvent event) {
        Object target = event.getSource();

        if (block == Task.BlockingScope.ACTION) {
            target = this;
        }

        return new DefaultInputBlocker(task, block, target, this);
    }

    private void noProxyActionPerformed(ActionEvent actionEvent) {
        Object taskObject = null;

        // Create the arguments array for actionMethod by calling
        //+ getActionArgument() for each parameter.
        Annotation[][] allPAnnotations = actionMethod.getParameterAnnotations();
        Class<?>[] pTypes = actionMethod.getParameterTypes();
        Object[] arguments = new Object[pTypes.length];

        for (int i = 0; i < pTypes.length; i++) {
            String pKey = null;

            for (Annotation pAnnotation : allPAnnotations[i]) {
                if (pAnnotation instanceof Action.Parameter) {
                    pKey = ((Action.Parameter) pAnnotation).value();
                    break;
                }
            }

            arguments[i] = getActionArgument(pTypes[i], pKey, actionEvent);
        }

        // Call target.actionMethod(arguments). If the return value is a Task,
        //+ then execute it.
        try {
            Object target = appAM.getActionsObject();
            taskObject = actionMethod.invoke(target, arguments);
        } catch (IllegalAccessException 
                | IllegalArgumentException 
                | InvocationTargetException e) {
            actionFailed(actionEvent, e);
        }

        if (taskObject instanceof Task) {
            Task task = (Task) taskObject;

            if (task.getInputBlocker() == null) {
                task.setInputBlocker(createInputBlocker(task, actionEvent));
            }

            ApplicationContext ctx = appAM.getContext();
            ctx.getTaskService().execute(task);
        }
    }

    @Override
    public boolean accept(Object sender) {
        return super.accept(sender); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * This method implements this `Action`'s behavior.
     * <p>
     * If there is a proxy <tt>Action` then call its `actionPerformed</tt> method.
     * Otherwise, call the <tt>@Action</tt> method with parameter values provided by
     * `getActionArgument()`. If anything goes wrong, call `actionFailed()`.
     *
     * @param e {@inheritDoc}
     * @see #setProxy(javax.swing.Action)
     * @see #getActionArgument(java.lang.Class, java.lang.String,
     * java.awt.event.ActionEvent)
     * @see Task
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        javax.swing.Action proxy = getProxy();

        if (proxy != null) {
            e.setSource(getProxySource());
            proxy.actionPerformed(e);
        } else if (actionMethod != null) {
            noProxyActionPerformed(e);
        }
    }

    /**
     * If the proxy action is <tt>null` and `enabledProperty</tt> was specified,
     * then return the value of the <tt>enabled</tt> property's is/get method applied to
     * our `ApplicationActionMap`'s `actionsObject`. Otherwise, return the
     * value of this <tt>Action`'s `enabled</tt> property.
     *
     * @return {@inheritDoc}
     * @see #setProxy(javax.swing.Action)
     * @see #setEnabled(boolean)
     * @see ApplicationActionMap#getActionsObject()
     */
    @Override
    public boolean isEnabled() {
        if ((getProxy() != null) || (isEnabledMethod == null)) {
            return super.isEnabled();
        } else {
            try {
                Object b = isEnabledMethod.invoke(appAM.getActionsObject());
                return (Boolean) b;
            } catch (Exception e) {
                throw newInvokeError(isEnabledMethod, e);
            }
        }
    }

    /**
     * If the proxy action is <tt>null` and `enabledProperty</tt> was specified,
     * the set the value of the <tt>enabled</tt> property by invoking the corresponding
     * <tt>set</tt> method on our ApplicationActionMap`'s `actionsObject`.
     * Otherwise, set the value of this <tt>Action`'s `enabled</tt> property.
     *
     * @param enabled {@inheritDoc}
     * @see #setProxy(javax.swing.Action)
     * @see #isEnabled()
     * @see ApplicationActionMap#getActionsObject()
     */
    @Override
    public void setEnabled(boolean enabled) {
        if ((getProxy() != null) || (setEnabledMethod == null)) {
            super.setEnabled(enabled);
        } else {
            try {
                setEnabledMethod.invoke(appAM.getActionsObject(), enabled);
            } catch (Exception e) {
                throw newInvokeError(setEnabledMethod, e, enabled);
            }
        }
    }

    /**
     * If the proxy action is <tt>null` and `selectedProperty</tt> was specified,
     * then return the value of the <tt>selected</tt> property's is/get method applied to
     * our `ApplicationActionMap`'s `actionsObject`. Otherwise, return the
     * value of this <tt>Action`'s `enabled</tt> property.
     *
     * @return <tt>true` if this `Action`'s `JTobbleButton</tt> is selected
     * @see #setProxy(javax.swing.Action)
     * @see #setSelected(boolean)
     * @see ApplicationActionMap#getActionsObject()
     */
    public boolean isSelected() {
        if ((getProxy() != null) || (isSelectedMethod == null)) {
            Object v = getValue(SELECTED_KEY);
            return (v instanceof Boolean) ? ((Boolean) v) : false;
        } else {
            try {
                Object b = isSelectedMethod.invoke(appAM.getActionsObject());
                return (Boolean) b;
            } catch (IllegalAccessException 
                    | IllegalArgumentException 
                    | InvocationTargetException e) {
                throw newInvokeError(isSelectedMethod, e);
            }
        }
    }

    /**
     * If the proxy action is <tt>null` and `selectedProperty</tt> was specified,
     * then set the value of the <tt>selected</tt> property by invoking the corresponding
     * <tt>set</tt> method on our `ApplicationActionMap`'s `actionsObject`.
     * Otherwise, set the value of this <tt>Action`'s `selected</tt> property.
     *
     * @param selected this `Action`'s `JToggleButton`'s value
     * @see #setProxy(javax.swing.Action)
     * @see #isSelected()
     * @see ApplicationActionMap#getActionsObject()
     */
    public void setSelected(boolean selected) {
        if ((getProxy() != null) || (setSelectedMethod == null)) {
            super.putValue(SELECTED_KEY, Boolean.valueOf(selected));
        } else {
            try {
                super.putValue(SELECTED_KEY, Boolean.valueOf(selected));
                if (selected != isSelected()) {
                    setSelectedMethod.invoke(appAM.getActionsObject(), selected);
                }
            } catch (Exception e) {
                throw newInvokeError(setSelectedMethod, e, selected);
            }
        }
    }

    /**
     * Keeps the <tt>@Action selectedProperty</tt> in sync when the value of
     * <tt>key</tt> is `Action.SELECTED_KEY`.
     *
     * @param key {@inheritDoc}
     * @param value {@inheritDoc}
     */
    @Override
    public void putValue(String key, Object value) {
        if (SELECTED_KEY.equals(key) && (value instanceof Boolean)) {
            setSelected((Boolean) value);
        } else {
            super.putValue(key, value);
        }
    }

    /* Throw an Error because invoking Method m on the actionsObject, with the
     * specified arguments, failed.
     */
    private Error newInvokeError(Method m, Exception e, Object... args) {
        String argsString = (args.length == 0) ? "" : args[0].toString();

        for (int i = 1; i < args.length; i++) {
            argsString += ", " + args[i];
        }

        String actionsClassName = appAM.getActionsObject().getClass().getName();
        String msg = String.format("%s.%s(%s) failed", actionsClassName, m,
                argsString);

        return new Error(msg, e);
    }

    /* Forward the @Action class' ProperyChangeEvent e to this Action's 
     * PropertyChangeListeners using actionPropertyName instead of the original
     * @Action class' propery name. This method is used by 
     * ApplicationActionMap#ActionsPCL to forward @Action enabledProperty and
     * selectedProperty changes.
     */
    public void forwardPropertyChangeEvent(PropertyChangeEvent e,
            String actionPropertyName) {
        if ("selected".equals(actionPropertyName)
                && (e.getNewValue() instanceof Boolean)) {
            putValue(SELECTED_KEY, (Boolean) e.getNewValue());
        }

        firePropertyChange(actionPropertyName, e.getOldValue(), e.getNewValue());
    }

    /* Log enough output for a developer to figure out what went wrong. */
    private void actionFailed(ActionEvent actionEvent, Exception e) {
        // Handling the original libraries TBD functionality...
        logger.error(e, toString());

        throw new Error(e);
    }

    /**
     * Returns a string representation of this <tt>ApplicationAction</tt> that
     * should be useful for debugging. If the action is enabled, its name is
     * enclosed by parentheses; if it is selected, then a "+" appears after the
     * name. If the action will appear with a text label, then that is included
     * too. If the action has a proxy, then we append the string for the proxy
     * action.
     *
     * @return a string representation of this `ApplicationAction`
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(getClass().getName());
        sb.append(" ");
        if (isEnabled()) {
            sb.append("(");
        }

        sb.append(getName());
        Object selectedValue = getValue(SELECTED_KEY);

        if (selectedValue instanceof Boolean) {
            if (((Boolean) selectedValue)) {
                sb.append("+");
            }
        }

        if (isEnabled()) {
            sb.append(")");
        }

        Object nameValue = getValue(javax.swing.Action.NAME); // [getName()].Action.text

        if (nameValue instanceof String) {
            sb.append(" \"");
            sb.append((String) nameValue);
            sb.append("\"");
        }

        if (getProxy() != null) {
            sb.append(" Proxy for: ");
            sb.append(getProxy().toString());
        }

        return sb.toString();
    }

}
