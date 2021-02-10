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
 *  Class      :   ResourceManager.java
 *  Author     :   Sean Carrick
 *  Created    :   Feb 8, 2021 @ 1:59:31 PM
 *  Modified   :   Feb 8, 2021
 *  
 *  Purpose:    See class JavaDoc
 *  
 *  Revision History:
 *  
 *  WHEN          BY                  REASON
 *  ------------    ------------------- ------------------------------------------
 *  Feb 08, 2021    Sean Carrick        Initial creation.
 * *****************************************************************************
 */
package org.jdesktop.application;

import org.jdesktop.application.utils.Logger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The application's <tt>ResourceManager</tt> provides read-only cached access to
 * resources in <tt>ResourceBundles</tt> via the {@link ResourceMap ResourceMap`
 * class. <tt>ResourceManager</tt> is a property of the
 * <tt>ApplicationContext</tt> and most applications look up resources relative
 * to it, like this:
 * <pre>
 * ApplicationContext appContext = Application.getInstance().getContext();
 * ResourceMap resourceMap = appContext.getResourceMap(MyClass.class);
 * String msg = resourceMap.getString("msg");
 * Icon icon = resourceMap.getIcon("icon");
 * Color color = resourceMap.getColor("color");
 * </pre>
 * <p>
 * {@link ApplicationContext#getResourceMap(java.lang.Class)
 * ApplicationContext.getResourceMap()} just delegates to its
 * <tt>ResourceManager`. The `ResourceMap</tt> in this example contains
 * resources from the ResourceBundle named `MyClass`, and the rest of the
 * chain contains resources shared by the entire application.</p>
 * <p>
 * Resources for a class are defined by an eponymous <tt>ResourceBundle</tt> in a
 * <tt>resources</tt> subpackage. The Application class itself may also provide
 * resources. A complete description of the naming conventions for
 * ResourceBundles is provided by the {@link #getResourceMap(Class)
 * getResourceMap()} method.</p>
 * <p>
 * The mapping from classes and <tt>Application</tt> to a list of ResourceBundle
 * names is handled by two protected methods:
 * {@link #getClassBundleNames(Class) getClassBundleNames}, and
 * {@link #getApplicationBundleNames() getApplicationBundleNames}. Subclasses
 * could override these methods to append additional ResourceBundle names to the
 * default lists.</p>
 *
 * @author Hans Muller (Original Author)
 * @author Sean Carrick (Adapting Author) &lt;sean at pekinsoft dot com&gt;
 *
 * @version 1.05
 * @since 1.03
 *
 * @see ApplicationContext#getResourceManager()
 * @see ApplicationContext#getResourceMap()
 * @see ResourceMap
 */
class ResourceManager extends AbstractBean {
    // Public Static Constants

    // Private Static Constants
    private static final Logger logger = Logger.getLogger(ResourceManager.class.getName());

    // Private Member Fields
    private final Map<String, ResourceMap> resourceMaps;
    private final ApplicationContext context;
    private List<String> applicationBundleNames = null;
    private ResourceMap appResourceMap = null;

    // Constructor(s)
    /**
     * Construct a `ResourceManager`. Typically applications will not
     * create a ResourceManager directly, they will retrieve the shared one from
     * the <tt>ApplicationContext</tt> with:
     * <pre>
     * Application.getInstance().getContext().getResourceManager();
     * </pre>
     * <p>
     * Or just look up `ResourceMap`s with the ApplicationContext
     * convenience method:</p>
     * <pre>
     * Application.getInstance().getContext().getResourceMap(MyClass.class);
     * </pre>
     *
     * @param context the <tt>ApplicationContext</tt> that owns this `ResourceManager`
     * @see ApplicationContext#getResourceManager()
     * @see ApplicationContext#getResourceMap(java.lang.Class)
     */
    public ResourceManager(ApplicationContext context) {
        if (context == null) {
            throw new IllegalArgumentException("null context");
        }

        this.context = context;
        resourceMaps = new ConcurrentHashMap<>();
    }

    /* Acted on the FIX-ME comment from the original and added JavaDoc */
    /**
     * Return the <tt>ApplicationContext</tt> used to create this
     * `ResourceManager`.
     *
     * @return the `ApplicationContext`
     */
    protected final ApplicationContext getContext() {
        return context;
    }

    /* Returns a read-only list of the ResourceBundle names for all of the
     * classes from startClass to (including) stopClass. The bundle names for
     * each class are #getClassBundleNames(Class). The list is in priority order:
     * resources defined in bundles earlier in the list shadow resources with 
     * the same name that appear in bundles that come later.
     */
    private List<String> allBundleNames(Class startClass, Class stopClass) {
        List<String> bundleNames = new ArrayList<>();
        Class limitClass = stopClass.getSuperclass(); // could be null

        for (Class c = startClass; c != limitClass; c = c.getSuperclass()) {
            bundleNames.addAll(getClassBundleNames(c));
        }

        return Collections.unmodifiableList(bundleNames);
    }

    private String bundlePackageName(String bundleName) {
        int i = bundleName.lastIndexOf(".");
        return (i == -1) ? "" : bundleName.substring(0, i);
    }

    /* Creates a parent chain of ResourceMaps for the specified ResourceBundle
     * names. One ResourceMap is created for each subsequence of ResourceBundle
     * names with a common bundle package name, i.e., with a common resourcesDir.
     * The parent of the final ResourceMap in the chain is root.
     */
    private ResourceMap createResourceMapChain(ClassLoader cl, ResourceMap root,
            ListIterator<String> names) {
        if (!names.hasNext()) {
            return root;
        } else {
            String bundleName0 = names.next();
            String rmBundlePackage = bundlePackageName(bundleName0);
            List<String> rmNames = new ArrayList<>();
            rmNames.add(bundleName0);

            while (names.hasNext()) {
                String bundleName = names.next();
                if (rmBundlePackage.equals(bundlePackageName(bundleName))) {
                    rmNames.add(bundleName);
                } else {
                    names.previous();
                    break;
                }
            }

            ResourceMap parent = createResourceMapChain(cl, root, names);
            return createResourceMap(cl, parent, rmNames);
        }
    }

    /* Lazily creates the Application ResourceMap chain, appResourceMap. If the
     * Application has not been launched yet, i.e., if the ApplicationContext
     * applicationClass property has not been set yet, then the ResourceMap just
     * corresponds to Application.class.
     */
    private ResourceMap getApplicationResourceMap() {
        if (appResourceMap == null) {
            List<String> appBundleNames = getApplicationBundleNames();
            Class appClass = getContext().getApplicationClass();

            if (appClass == null) {
                logger.warning("getApplicationResourceMap(): no Application "
                        + "class");
                appClass = Application.class;
            }

            ClassLoader classLoader = appClass.getClassLoader();
            appResourceMap = createResourceMapChain(classLoader, null,
                    appBundleNames.listIterator());
        }

        return appResourceMap;
    }

    /* Lazily creates the ResourceMap chain for the class from startClass to 
     * stopClass.
     */
    private ResourceMap getClassResourceMap(Class startClass, Class stopClass) {
        String classResourceMapKey = startClass.getName() + stopClass.getName();
        ResourceMap classResourceMap = resourceMaps.get(classResourceMapKey);

        if (classResourceMap == null) {
            List<String> classBundleNames = allBundleNames(startClass, stopClass);
            ClassLoader classLoader = startClass.getClassLoader();
            ResourceMap appRM = getResourceMap();
            classResourceMap = createResourceMapChain(classLoader, appRM,
                    classBundleNames.listIterator());
            resourceMaps.put(classResourceMapKey, classResourceMap);
        }

        return classResourceMap;
    }

    /**
     * Returns a {@link ResourceMap#getParent() chain} of `ResourceMap`s
     * that encapsulate the `ResourceBundle`s for each class from `
     * startClass} to `stopClass`, inclusive. The final link in the chain
     * is Application ResourceMap chain, i.e., the value of 
     * {@link #getResourceMap() getResourceMap()}.
     * <p>
     * The ResourceBundle names for the chain of ResourceMaps are defined by
     * {@link #getClassBundleNames(java.lang.Class) } and 
     * {@link #getApplicationBundleNames() }. Collectively they define the
     * standard location for `ResourceBundle`s for a particular class as 
     * the <tt>resources</tt> subpackage. For example, the ResourceBundle for the
     * single class <tt>com.myco.MyScreen</tt> would be named 
     * `com.myco.resources.MyScreen`. Typical ResourceBundles are 
     * ".properties" files, so: `com/myco/resources/MyScreen.properties`.
     * The following table is a list of the ResourceMaps and their constituent
     * ResourceBundles for the same example:</p>
     * <table border ="1" cellpadding="4%">
     *  <caption><em>ResourceMap chain for class MyScreen in MyApp</em></caption>
     *  <tr>
     *      <th></th>
     *      <th>ResourceMap</th>
     *      <th>ResourceBundle names</th>
     *      <th>Typical ResourceBundle files</th>
     *  </tr>
     *  <tr>
     *      <td>1</td>
     *      <td>class: com.myco.MyScreen</td>
     *      <td>com.myco.resources.MyScreen</td>
     *      <td>com/myco/resources/MyScreen.properties</td>
     *  </tr>
     *  <tr>
     *      <td>2</td>
     *      <td>application: com.myco.MyApp</td>
     *      <td>com.myco.resources.MyApp</td>
     *      <td>com/myco/resources/MyApp.properties</td>
     *  </tr>
     *  <tr>
     *      <td>3</td>
     *      <td>application: com.pekinsoft.desktop.application.Application</td>
     *      <td>com.pekinsoft.desktop.application.resources.Application</td>
     *      <td>com.pekinsoft/desktop/application/resources/Application.properties</td>
     *  </tr>
     * </table>
     * <p>
     * None of the ResourceBundles are required to exist. If more than one 
     * ResourceBundle contains a resource with the same name, then the one 
     * earlier in the list has precedence.</p>
     * <p>
     * ResourceMaps are constructed lazily and cached. One ResourceMap is
     * constructed for each sequence of classes in the same package.</p>
     * 
     * @param startClass the first class whose ResourceBundles will be included
     * @param stopClass the last class whose ResourceBundles will be included
     * @return a <tt>ResourceMap</tt> chain that contains resources loaded from
     *          `ResourceBundle`s found in the resources subpackage for 
     *          each class
     * 
     * @see #getClassBundleNames(java.lang.Class) 
     * @see #getApplicationBundleNames() 
     * @see ResourceMap#getParent() 
     * @see ResourceMap#getBundleNames() 
     */
    ResourceMap getResourceMap(Class startClass, Class stopClass) {
        if (startClass == null) {
            throw new IllegalArgumentException("null startClass");
        }
        if (stopClass == null) {
            throw new IllegalArgumentException("null stopClass");
        }
        
        if (!stopClass.isAssignableFrom(startClass)) {
            throw new IllegalArgumentException("startClass is not a subclass, "
                    + "or the same as, stopClass");
        }
        
        return getClassResourceMap(startClass, stopClass);
    }

    /**
     * Return the ResourceMap chain for the specified class. This is just a 
     * convenience method, it is th esame as:
     * <code>getResourceMap(cls, cls)</code>.
     * 
     * @param cls the class that defines the location of the ResourceBundles
     * @return a <tt>ResourceMap</tt> that contains resources loaded from the
     *          `ResourceBundle`s found in the resources subpackage of the
     *          specified class' package
     * 
     * @see #getResourceMap(java.lang.Class, java.lang.Class) 
     */
    public final ResourceMap getResourceMap(Class cls) {
        if (cls == null) {
            throw new IllegalArgumentException("null cls");
        }
        
        return getResourceMap(cls, cls);
    }

    /**
     * Returns the chain of ResourceMaps that is shared by the entire Application,
     * beginning with the resources defined for the application's class, i.e.,
     * the value of the ApplicationContext {@link ApplicationContext#getApplicationClass() 
     * applicationClass} property. If the <tt>applicationClass</tt> property has
     * not been set, e.g. because the application has not been
     * {@link Application#launch(java.lang.Class, java.lang.String[]) launched}
     * yet, then a ResourceMap for just <tt>Application.class</tt> is returned.
     * 
     * @return the Application's ResourceMap
     * 
     * @see ApplicationContext#getResourceMap() 
     * @see ApplicationContext#getApplicationClass() 
     */
    ResourceMap getResourceMap() {
        return getApplicationResourceMap();
    }
    
    /* Convert a class name to an eponymous resource bundle in the resources
     * subpackage. For example, given a class named com.foo.bar.MyClass, the
     * ResourceBundle name would be "com.foo.bar.resources.MyClass". If MyClass
     * is an inner class, only its "simple name" is used. For example, given an
     * inner class named com.foo.bar.OuterClass$InnerClass, the ResourceBundle
     * name would be "com.foo.bar.resources.InnerClass". Although this could
     * result in a collision, creating more complex rules for inner classes 
     * would be a burden for developers.
     *
     * ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
     *  Noted by the original author: Hans Muller
     *  =======================================================================
     *  Noted by the adapting author: Sean Carrick <sean at pekinsoft dot com>
     * vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
     * This method has been updated on Monday, February 08, 2021 by Sean Carrick.
     * The update provides for solving the potential collisions from the comment
     * above (by original author Hans Muller). Instead of appending the simple
     * name of a class to the end of the bundle base name, I changed it to just
     * working with the className variable that we set in the first line of the
     * method:
     *
     * String className = cls.getName();
     *
     * Then, instead of adding the simple name of the class to the end of the
     * created ".resources." subpackage, we just parse the className again:
     *
     * +---------------+
     * |  OLD VERSION  |
     * +---------------+
     * if (i > 0) {
     *     sb.append(className.substring(0, i);
     *     sb.append(".resources.");
     *     sb.append(cls.getSimpleName());
     * } else {
     *     sb.append("resources.");
     *     sb.append(cls.getSimpleName());
     * }
     *
     * +---------------+
     * |  NEW VERSION  |
     * +---------------+
     * if (i > 0) {
     *     sb.append(className.substring(0, i);
     *     sb.append(".resources.");
     *     sb.append(className.substring(i + 1);
     * } else {
     *     sb.append("resources.");
     *     sb.append(className);
     * }
     * 
     * It seems to me that this will work just fine. With this new version, I 
     * have included the following lines of code as well:
     * 
     * int j = className.lastIndexOf("$");
     *
     * if (j > 0) {
     *     className = className.replace('$', '_');
     * }
     *
     * This allows for preventing collisions between inner classes that are
     * named the same, but reside in different classes. Using getSimpleName(),
     * for example on the following two classes, would have created a collision:
     *
     * com.foo.bar.OuterClass1$InnerClass
     * com.foo.bar.OuterClass2$InnerClass
     *
     * the bundle base name for both of those inner classes would then be:
     *
     * com.foo.bar.resources.InnerClass
     *
     * With the modification that I have made, the bundle base names would be:
     *
     * com.foo.bar.resources.Class1_InnerClass
     * com.foo.bar.resources.Class2_InnerClass
     *
     * Anonymous inner classes will also be able to have ResourceBundles, if
     * they needed one for some wierd reason, because they would have the final
     * name of ContainingClass_1, ContainingClass_2, etc. The getSimpleName()
     * method would return a blank string for those and the getCanonicalName()
     * method would return a null. Therefore, this is the best fix.
     */
    private String classBundleBaseName(Class cls) {
        String className = cls.getName();
        StringBuilder sb = new StringBuilder();
        int i = className.lastIndexOf(".");
        
        // Check to see if there is a dollar sign in the class' name. 
        int j = className.lastIndexOf("$");
        
        // If there is, then it is an inner class and we need to change the 
        //+ dollar sign.
        if (j > 0) {
            className = className.replace('$', '_');
        }
        
        if (i > 0) {
            sb.append(className.substring(0, i));
            sb.append(".resources.");
            sb.append(className.substring(i + 1));
        } else {
            sb.append("resources.");
            sb.append(className);
        }
        
        return sb.toString();
    }

    /**
     * Map from a class to a list of the names of the `ResourceBundle`s
     * specific to the class. The list is in priority order: resources defined
     * by the first ResourceBundle shadow resources with the same name that come
     * later.
     * <p>
     * By default this method returns one ResourecBundle whose name is the same
     * as the class' name, but in the <tt>"resources"</tt> subpackage.</p>
     * <p>
     * For example, given a class named `com.foo.bar.MyClass`, the 
     * ResourceBundle name would be `"com.foo.bar.resources.MyClass"`. If
     * MyClass is an inner class, its "parsed name" is used. For example, given
     * an inner class named `com.foo.bar.MyClass$MyInnerClass`, the
     * ResourceBundle name would be 
     * `"com.foo.bar.resources.MyClass_MyInnerClass"`. This prevents any
     * collisions from happening due to <tt>MyInnerClass</tt> being defined in,
     * for example, <tt>com.foo.bar.MyClass</tt> and 
     * `com.foo.bar.YourClass`.</p>
     * <dl><dt>Note:</dt><dd>In the original version of the Swing Application
     * Framework, the "simple name" of the inner class was used due to the 
     * complexity of accomplishing what we have accomplished in this revision.
     * Therefore, with the example above, the inner class 
     * <tt>com.foo.bar.MyClass$MyInnerClass</tt> would have had a ResourceBundle
     * name of `"com.foo.bar.resources.MyInnerClass"`. In the second 
     * example above, there would have been a naming collision between the two 
     * inner classes named <tt>MyClass$MyInnerClass</tt> and 
     * `YourClass$MyInnerClass`. Both of those would have worked out to
     * the ResourceBundle name of `"com.foo.bar.resources.MyInnerClass"`,
     * so one of the classes would not have had access to its resources.</dd>
     * </dt><p>
     * This method is used by the <tt>getResourceMap</tt> methods to compute the
     * list of ResourceBundle names for a new `ResourceMap`.
     * ResourceManager subclasses can override this method to add additional
     * class-specific ResourceBundle names to the list.</p>
     * 
     * @param cls the named ResourceBundles are specific to `cls`
     * @return the names of the ResourceBundles to be loaded for `cls`
     * 
     * @see #getResourceMap() 
     * @see #getApplicationBundleNames() 
     */
    private List<String> getClassBundleNames(Class cls) {
        String bundleName = classBundleBaseName(cls);
        return Collections.singletonList(bundleName);
    }

    /**
     * Called by {@link #getResourceMap() getResourceMap} to construct
     * `ResourceMap`s. By default this method is effectively just:
     * <pre>
     * return new ResourceMap(parent, classLoader, bundleNames);
     * </pre>
     * <p>
     * Custom ResourceManagers might override this method to construct their own
     * ResourceMap subclasses.</p>
     * 
     * @param classLoader the ClassLoader for the class whose resources are to
     *          be loaded
     * @param parent the parent of the class whose resources are to be loaded
     * @param bundleNames the names of the ResourceBundles
     * @return the created ResourceMap
     */
    private ResourceMap createResourceMap(ClassLoader classLoader, 
            ResourceMap parent, List<String> bundleNames) {
        return new ResourceMap(parent, classLoader, bundleNames);
    }
    
    /**
     * The value of the special Application ResourceMap resource named "platform".
     * By default the value of this resource is "osx" if the underlying operating
     * system is Apple OSX or "default".
     * 
     * @return the value of the platform resource
     * 
     * @see #setPlatform(java.lang.String)
     */
    public String getPlatform() {
        return getResourceMap().getString("platform");
    }
    
    /**
     * Defines the value of the special Application ResourceMap resource named
     * "platform". This resource can be used to define platform specific
     * resources. For example:
     * <pre>
     * myLabel.text.osx = A value that is appropriate for OS X
     * myLabel.text.default = A value for other platforms
     * myLabel.text = myLabel.text.${platform}
     * </pre>
     * <p>
     * By default the value of this resource is "osx" if the underlying operating
     * system is Apple OSX or "default". To distinguish other platforms one can
     * reset this property based on the value of the <tt>"os.name"</tt> system
     * property.</p>
     * <p>
     * This method should be called as early as possible, typically in the
     * Application <tt>Application#initialize initialize</tt> method.</p>
     * 
     * @param platform the operating system on which the Application is running
     * 
     * @see #getPlatform() 
     * @see System#getProperty(java.lang.String) 
     */
    public void setPlatform(String platform) {
        if (platform == null) {
            throw new IllegalArgumentException("null platform");
        }
        
        getResourceMap().putResource("platform", platform);
    }

    /**
     * The names of the ResourceBundles to be shared by the entire application.
     * The list is in priority order: resources defined by the first 
     * ResourceBundle shadow resources with the same name that come later.
     * <p>
     * The default value for this proerty is a list of {@link 
     * #getClassBundleNames(java.lang.Class) per-class} ResourceBundle names,
     * beginning with the `Application`'s class and of each of its
     * superclasses, up to `Application.class`. For example, if the
     * Application's class was `com.foo.bar.MyApp`, and MyApp was a 
     * subclass of `SingleFrameApplication.class`, then the ResourceBundle
     * names would be:</p>
     * <code><ol>
     * <li>com.foo.bar.resources.MyApp</li>
     * <li>com.pekinsoft.desktop.application.resources.SingleFrameApplication</li>
     * <li>com.pekinsoft.desktop.application.resources.Application</li>
     * </ol></code>
     * <p>
     * The default value of this property is computed lazily and cached. If it
     * is reset, then all ResourceMaps cached by <tt>getResourceMap</tt> will be
     * updated.</p>
     * 
     * @return a list of all ResourceBundle names shared throughout the 
     *          `Application`
     * 
     * @see #setApplicationBundleNames()
     * @see #getResourceMap() 
     * @see #getClassBundleNames(java.lang.Class) 
     * @see ApplicationContext#getApplication() 
     */
    private List<String> getApplicationBundleNames() {
        // Lazily compute an initial value for this property, unless the 
        //+ application's class has not been specified yet. In that case, we just
        //+ return a placeholder based on Application.class.
        if (applicationBundleNames == null) {
            Class appClass = getContext().getApplicationClass();
            
            if (appClass == null) {
                return allBundleNames(Application.class, Application.class);
            } else {
                applicationBundleNames = allBundleNames(appClass, 
                        Application.class);
            }
        }
        
        return applicationBundleNames;
    }
    
    /**
     * Specify the names of the ResourceBundles to be shared by the entire
     * application. More information about the property is provided by the 
     * {@link #getApplicationBundleNames()} method.
     * 
     * @param bundleNames bundle names to add
     * 
     * @see #getApplicationBundleNames() 
     */
    public void setApplicationBundleNames(List<String> bundleNames) {
        if (bundleNames != null) {
            for (String bundleName : bundleNames) {
                if ((bundleName == null) || (bundleNames.isEmpty())) {
                    throw new IllegalArgumentException("invalid bundle name \""
                            + bundleName + "\"");
                }
            }
        }
        
        Object oldValue = applicationBundleNames;
        if (bundleNames != null) {
            applicationBundleNames = Collections.unmodifiableList(
                    new ArrayList(bundleNames));
        } else {
            applicationBundleNames = null;
        }
        
        resourceMaps.clear();
        firePropertyChange("applicationBundleNames", oldValue, 
                applicationBundleNames);
    }

}
