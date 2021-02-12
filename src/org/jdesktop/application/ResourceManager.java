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
 *  Created    :   Feb 11, 2021 @ 10:32:46 AM
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * The application's <code>ResourceManager</code> provides read-only cached
 * access to resources in <code>ResourceBundles</code> via the
 * {@link ResourceMap ResourceMap} class.  <code>ResourceManager</code> is a
 * property of the <code>ApplicationContext</code> and most applications look up
 * resources relative to it, like this:
 * ```java
 * ApplicationContext appContext = Application.getInstance().getContext();
 * ResourceMap resourceMap = appContext.getResourceMap(MyClass.class);
 * String msg = resourceMap.getString("msg");
 * Icon icon = resourceMap.getIcon("icon");
 * Color color = resourceMap.getColor("color");
 * ```
 * <p>
 * {@link ApplicationContext#getResourceMap(Class) ApplicationContext.getResourceMap()}
 * just delegates to its <code>ResourceManager</code>. The
 * <code>ResourceMap</code> in this example contains resources from the
 * ResourceBundle named <code>MyClass</code>, and the rest of the chain contains
 * resources shared by the entire application.</p>
 * <p>
 * Resources for a class are defined by an eponymous <code>ResourceBundle</code>
 * in a <code>resources</code> subpackage. The Application class itself may also
 * provide resources. A complete description of the naming conventions for
 * ResourceBundles is provided by the
 * {@link #getResourceMap(Class) getResourceMap()} method.</p>
 * <p>
 * The mapping from classes and <code>Application</code> to a list
 * ResourceBundle names is handled by two protected methods:  {@link #getClassBundleNames(Class) getClassBundleNames},
 * {@link #getApplicationBundleNames() getApplicationBundleNames}. Subclasses
 * could override these methods to append additional ResourceBundle names to the
 * default lists.</p>
 *
 * @see ApplicationContext#getResourceManager() 
 * @see ApplicationContext#getResourceMap() 
 * @see ResourceMap
 * 
 * @author Hans Muller (Original Author) &lt;current email unknown&gt;
 * @author Sean Carrick (Updater) &lt;sean at pekinsoft dot com&gt;
 * 
 * @version 1.05
 * @since 1.03
 */
public class ResourceManager extends AbstractBean {

    private static final Logger logger = Logger.getLogger(
            ResourceManager.class.getName());
    private final Map<String, ResourceMap> resourceMaps;
    private final ApplicationContext context;
    private List<String> applicationBundleNames = null;
    private ResourceMap appResourceMap = null;

    /*
    ****************************************************************************
    * Fixed the following JavaDoc by adding the JavaDoc for the parameter.     *
    *                                                                          *
    *                                            -> Sean Carrick, Feb 11, 2021 *
    ****************************************************************************
    */
    /**
     * Construct a <code>ResourceManager</code>. Typically applications will not
     * create a ResourceManager directly, they'll retrieve the shared one from
     * the <code>ApplicationContext</code> with:
     * ```java
     * Application.getInstance().getContext().getResourceManager()
     * ```
     * Or just look up <code>ResourceMaps</code> with the
     * ApplicationContext convenience method:
     * ```java
     * Application.getInstance().getContext().getResourceMap(MyClass.class)
     * ```
     *
     * @param context the context in which this application is running
     *
     * @see ApplicationContext#getResourceManager() 
     * @see ApplicationContext#getResourceMap() 
     */
    protected ResourceManager(ApplicationContext context) {
        if (context == null) {
            throw new IllegalArgumentException("null context");
        }
        this.context = context;
        resourceMaps = new ConcurrentHashMap<>();
    }

    /*
    ****************************************************************************
    * Fixed the F I X M E comment that was here by adding the requested JavaDoc*
    * comment block.                                                           *
    *                                                                          *
    *                                            -> Sean Carrick, Feb 11, 2021 *
    ****************************************************************************
    */
    /**
     * Retrieves the <code>ApplicationContext</code>, which is the context in
     * which the <code>Application</code> is running, for the current
     * application. This context is shared among the various classes that make
     * up the Swing Application Framework.
     * <p>
     * The <code>ApplicationContext</code> is the mechanism by which the parts
     * of the application are able to share resources.</p>
     * 
     * @return the context in which the current application is running
     */
    protected final ApplicationContext getContext() {
        return context;
    }

    /* Returns a read-only list of the <code>ResourceBundle</code> names for all 
     * of the classes from <code>startClass</code> to (including) <code>stopClass
     * </code>. The bundle names for each class are <code>#getClassBundleNames(
     * Class)</code>. The list is in priority order: resources defined in bundles
     * earlier in the list shadow resources with the same name that
     * appear bundles that come later.
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

    /* Creates a parent chain of ResourceMaps for the specfied
     * ResourceBundle names.  One ResourceMap is created for each
     * subsequence of ResourceBundle names with a common bundle
     * package name, i.e. with a common resourcesDir.  The parent 
     * of the final ResourceMap in the chain is root.
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

    /* Lazily creates the Application ResourceMap chain,
     * appResourceMap.  If the Application hasn't been launched yet,
     * i.e. if the ApplicationContext applicationClass property hasn't
     * been set yet, then the ResourceMap just corresponds to
     * Application.class.
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

    /* Lazily creates the ResourceMap chain for the the class from 
     * startClass to stopClass.
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
     * Returns a {@link ResourceMap#getParent chain} of <code>ResourceMap</code>s 
     * that encapsulate the <code>ResourceBundles</code> for each class from 
     * <code>startClass</code> to (including) <code>stopClass</code>. The final 
     * link in the chain is <code>Application ResourceMap</code> chain, i.e. the 
     * value of {@link #getResourceMap() getResourceMap()}.
     * <p>
     * The <code>ResourceBundle</code> names for the chain of ResourceMaps are 
     * defined by {@link #getClassBundleNames} and 
     * {@link #getApplicationBundleNames}. Collectively they define the standard 
     * location for <code>ResourceBundles</code> for a particular class as the
     * <code>resources</code> subpackage. For example, the <code>ResourceBundle
     * </code> for the single class <code>com.myco.MyScreen</code>, would be named
     * <code>com.myco.resources.MyScreen</code>. Typical <code>ResourceBundle</code>s 
     * are ".properties" files, so: {@codecom/foo/bar/resources/MyScreen.properties}.
     * The following table is a list of the <code>ResourceMap</code>s and their
     * constituent <code>ResourceBundle</code>s for the same example:
     * <p>
     * <table border="1">
     * <caption><em><code>ResourceMap</code> chain for class <code>MyScreen</code>
     *  in <code>MyApp</code></em></caption>
     * <tr>
     * <th></th>
     * <th><code>ResourceMap</code></th>
     * <th><code>ResourceBundle</code> names</th>
     * <th>Typical <code>ResourceBundle</code> files</th>
     * </tr>
     * <tr>
     * <td>1</td>
     * <td>class: <code>com.myco.MyScreen</code></td>
     * <td><code>com.myco.resources.MyScreen</code></td>
     * <td>com/myco/resources/MyScreen.properties</td>
     * </tr>
     * <tr>
     * <td>2/td>
     * <td>application: <code>com.myco.MyApp</code></td>
     * <td><code>com.myco.resources.MyApp</code></td>
     * <td>com/myco/resources/MyApp.properties</td>
     * </tr>
     * <tr>
     * <td>3</td>
     * <td>application: <code>javax.swing.application.Application</code></td>
     * <td><code>javax.swing.application.resources.Application</code></td>
     * <td>javax.swing/application/resources/Application.properties</td>
     * </tr>
     * </table>
     * <p>
     * None of the <code>ResourceBundle</code>s are required to exist. If more 
     * than one <code>ResourceBundle</code> contains a resource with the same 
     * name then the one earlier in the list has precedence.</p>
     * <p>
     * <code>ResourceMap</code>s are constructed lazily and cached. One <code>
     * ResourceMap</code> is constructed for each sequence of classes in the 
     * same package.</p>
     *
     * @param startClass the first class whose <code>ResourceBundle</code>s will
     *          be included
     * @param stopClass the last class whose <code>ResourceBundle</code>s will
     *          be included
     * @return a <code>ResourceMap</code> chain that contains resources loaded
     * from <code>ResourceBundle</code>s found in the resources subpackage for
     * each class.
     * 
     * @see #getClassBundleNames(java.lang.Class) 
     * @see #getApplicationBundleNames() 
     * @see ResourceMap#getParent() 
     * @see ResourceMap#getBundleNames() 
     */
    public ResourceMap getResourceMap(Class startClass, Class stopClass) {
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
     * Return the ResourcedMap chain for the specified class. This is just a
     * convenience method, it's the same as: <code>getResourceMap(cls, cls)</code>.
     *
     * @param cls the class that defines the location of <code>
     *          ResourceBundle</code>s
     * @return a <code>ResourceMap</code> that contains resources loaded from
     * <code>ResourceBundle</code>s found in the resources subpackage of the
     * specified class's package.
     * @see #getResourceMap(Class, Class)
     */
    public final ResourceMap getResourceMap(Class cls) {
        if (cls == null) {
            throw new IllegalArgumentException("null class");
        }
        return getResourceMap(cls, cls);
    }

    /**
     * Returns the chain of <code>ResourceMap</code>s that's shared by the entire
     * application, beginning with the resources defined for the application's
     * class, i.e. the value of the <code>ApplicationContext</code>
     * {@link ApplicationContext#getApplicationClass()  applicationClass} property.
     * If the <code>applicationClass</code> property has not been set, e.g.
     * because the application has not been 
     * {@link Application#launch(java.lang.Class, java.lang.String[])  launched}
     * yet, then a <code>ResourceMap</code> for just <code>Application.class
     * </code> is returned.
     *
     * @return the <code>Application</code>'s <code>ResourceMap</code>
     * @see ApplicationContext#getResourceMap()
     * @see ApplicationContext#getApplicationClass() 
     */
    public ResourceMap getResourceMap() {
        return getApplicationResourceMap();
    }

    /**
     * The names of the <code>ResourceBundle</code>s to be shared by the entire
     * application. The list is in priority order: resources defined by the first
     * <code>ResourceBundle</code> shadow resources with the the same name that 
     * come later.
     * <p>
     * The default value for this property is a list of {@link
     * #getClassBundleNames per-class} <code>ResourceBundle</code> names, 
     * beginning with the <code>Application</code>'s class and of each of its 
     * superclasses, up to <code>Application.class</code>. For example, if the 
     * <code>Application</code>'s class was <code>com.foo.bar.MyApp</code>, and 
     * <code>MyApp</code> was a subclass of <code>SingleFrameApplication.class
     * </code>, then the <code>ResourceBundle</code> names would be: 
     * <code><ol>
     * <li>com.foo.bar.resources.MyApp</li>
     * <li>javax.swing.application.resources.SingleFrameApplication</li>
     * <li>javax.swing.application.resources.Application</li>
     * </ol></code>
     * <p>
     * The default value of this property is computed lazily and cached. If it's
     * reset, then all <code>ResourceMap</code>s cached by <code>getResourceMap
     * </code> will be updated.</p>
     *
     * @see #setApplicationBundleNames(java.util.List) 
     * @see #getResourceMap() 
     * @see #getClassBundleNames(java.lang.Class) 
     * @see ApplicationContext#getApplication() 
     */
    public List<String> getApplicationBundleNames() {
        /* Lazily compute an initial value for this property, unless the
	 * application's class hasn't been specified yet.  In that case
	 * we just return a placeholder based on Application.class.
         */
        if (applicationBundleNames == null) {
            Class appClass = getContext().getApplicationClass();
            if (appClass == null) {
                return allBundleNames(Application.class, Application.class); // placeholder
            } else {
                applicationBundleNames = allBundleNames(appClass, Application.class);
            }
        }
        return applicationBundleNames;
    }

    /**
     * Specify the names of the <code>ResourceBundle</code>s to be shared by the
     * entire application. More information about the property is provided by the
     * {@link #getApplicationBundleNames} method.
     *
     * @param bundleNames list of all of the shared bundle names
     * 
     * @see #setApplicationBundleNames(java.util.List) 
     */
    public void setApplicationBundleNames(List<String> bundleNames) {
        if (bundleNames != null) {
            bundleNames.stream().filter(bundleName -> ((bundleName == null) 
                    || (bundleNames.isEmpty()))).forEachOrdered(bundleName -> {
                throw new IllegalArgumentException("invalid bundle name \"" 
                        + bundleName + "\"");
            });
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
     *  Noted by the original author: Hans Muller in 2006
     *  =======================================================================
     *  Noted by the adapting author: Sean Carrick on Feb 08, 2021
     * vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
     *
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
     * method would return a null. Therefore, in my opinion, this is the best 
     * fix to avoid ResourceBundle name collisions.
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
     * Map from a class to a list of the names of the
     * <code>ResourceBundle</code>s specific to the class. The list is in
     * priority order: resources defined by the first <code>ResourceBundle</code>
     * shadow resources with the the same name that come later.
     * <p>
     * By default this method returns one ResourceBundle whose name is the same
     * as the class's name, but in the <code>"resources"</code> subpackage.</p>
     * <p>
     * For example, given a class named <code>com.foo.bar.MyClass</code>, the
     * ResourceBundle name would be <code>"com.foo.bar.resources.MyClass"</code>. 
     * If MyClass is an inner class, only its "simple name" is used. For example, 
     * given an inner class named <code>com.foo.bar.OuterClass$InnerClass</code>, 
     * the <code>ResourceBundle</code> name would be 
     * <code>"com.foo.bar.resources.InnerClass"</code>.</p>
     * <p>
     * This method is used by the <code>getResourceMap</code> methods to compute
     * the list of <code>ResourceBundle</code> names for a new <code>ResourceMap
     * </code>. <code>ResourceManager</code> subclasses can override this method
     * to add additional class-specific <code>ResourceBundle</code> names to the
     * list.</p>
     *
     * @param cls the named ResourceBundles are specific to <code>cls</code>.
     * @return the names of the ResourceBundles to be loaded for <code>cls</code>
     * 
     * @see #getResourceMap() 
     * @see #getApplicationBundleNames() 
     */
    protected List<String> getClassBundleNames(Class cls) {
        String bundleName = classBundleBaseName(cls);
        return Collections.singletonList(bundleName);
    }

    /**
     * Called by {@link #getResourceMap} to construct <code>ResourceMaps</code>.
     * By default this method is effectively just:
     * ```java
     * return new ResourceMap(parent, classLoader, bundleNames);
     * ```
     * Custom <code>ResourceManager</code>s might override this method to 
     * construct their own <code>ResourceMap</code> subclasses.
     * 
     * @param classLoader the class loader for the class to which the <code>
     *          ResourceBundle</code> belongs
     * @param parent the parent of the class to which the <code>ResourceBundle
     *          </code> belongs
     * @param bundleNames the names of the <code>ResourceBundle</code>s belonging
     *          to the specified class
     * @return <code>ResourceMap</code> of the chain of resources to which the
     *          specified class has access
     */
    protected ResourceMap createResourceMap(ClassLoader classLoader, 
            ResourceMap parent, List<String> bundleNames) {
        return new ResourceMap(parent, classLoader, bundleNames);
    }

    /**
     * The value of the special <code>Application ResourceMap</code> resource 
     * named "platform". By default the value of this resource is "osx" if the
     * underlying operating environment is Apple OSX or "default".
     *
     * @return the value of the platform resource
     * @see #setPlatform(java.lang.String) 
     */
    public String getPlatform() {
        return getResourceMap().getString("platform");
    }

    /**
     * Defines the value of the special <code>Application ResourceMap</code> 
     * resource named "platform". This resource can be used to define platform 
     * specific resources. For example:
     * <pre>
     * myLabel.text.osx = A value that's appropriate for OSX
     * myLabel.text.default = A value for other platforms
     * myLabel.text = myLabel.text.${platform}
     * </pre>
     * <p>
     * By default the value of this resource is "osx" if the underlying
     * operating environment is Apple OSX or "default". To distinguish other
     * platforms one can reset this property based on the value of the
     * <code>"os.name"</code> system property.</p>
     * <p>
     * This method should be called as early as possible, typically in the
     * <code>Application</code> {@link Application#initialize initialize} method.
     * </p>
     * 
     * @param platform the operating system name to set as the platform on which
     *          the <code>Application</code> is running
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
}
