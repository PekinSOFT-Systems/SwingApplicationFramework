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
 * Class      :   LocalStorage.java
 * Author     :   Sean Carrick <sean at pekinsoft dot com>
 * Created    :   Jan 31, 2021 @ 3:11:35 PM
 * Modified   :   Jan 31, 2021
 *  
 * Purpose:     See class JavaDoc
 * 	
 * Revision History:
 *  
 * WHEN          BY                  REASON
 * ------------  ------------------- -------------------------------------------
 * Jan 31, 2021     Sean Carrick             Initial creation.
 * *****************************************************************************
 */

package org.jdesktop.application;

import java.awt.Rectangle;
import java.beans.DefaultPersistenceDelegate;
import java.beans.Encoder;
import java.beans.ExceptionListener;
import java.beans.Expression;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import org.jdesktop.application.utils.Logger;

/**
 * Access to per application, per user, local file storage.
 *
 * @see ApplicationContext#getLocalStorage
 * @see SessionStorage
 * 
 * @author Hans Muller (Original Author)
 * @author Sean Carrick (Adapting Author) &lt;sean at pekinsoft dot com&gt;
 * 
 * @version 1.05
 * @since 1.03
 */
public class LocalStorage extends AbstractBean {
    private static Logger logger = Logger.getLogger(LocalStorage.class.getName());
    private final ApplicationContext context;
    private long storageLimit = -1l;
    private LocalIO localIO = null;
    private final File unspecifiedFile = new File("unspecified");
    private File directory = unspecifiedFile;

    /**
     * Creates a default instance of the LocalStorage class.
     */
    LocalStorage(ApplicationContext context) {
        if (context == null) {
            throw new IllegalArgumentException("null context");
        }
        
        this.context = context;
    }
    
    /**
     * The ApplicationContext singleton for this LocalStorage.
     * 
     * @return the LocalStorage's ApplicationContext singleton
     */
    protected final ApplicationContext getContext() {
        return context;
    }
    
    private void checkFileName(String fileName) {
        if (fileName == null) {
            throw new IllegalArgumentException("null fileName");
        }
    }
    
    public InputStream openInputFile(String fileName) throws IOException {
        checkFileName(fileName);
        return getLocalIO().openInputFile(fileName);
    }
    
    public OutputStream openOutputFile(String fileName) throws IOException {
        checkFileName(fileName);
        return getLocalIO().openOutputFile(fileName);
    }
    
    public boolean deleteFile(String fileName) throws IOException {
        checkFileName(fileName);
        return getLocalIO().deleteFile(fileName);
    }
    
    /* If an exception occurs in the XMLEncoder/Decoder, we want to throw an 
     * IOException. The exceptionThrow listener method does not throw a checked
     * exception, so we just set a flag here and check it when the encode/decode
     * operation finishes.
     */
    private static class AbortExceptionListener implements ExceptionListener {
        
        public Exception exception = null;

        @Override
        public void exceptionThrown(Exception e) {
            if (exception == null) {
                exception = e;
            }
        }
        
    }
    
    private static boolean persistenceDelegatesInitialized = false;

    public void save(Object bean, final String fileName) throws IOException {
        AbortExceptionListener el = new AbortExceptionListener();
        XMLEncoder e = null;
        
        /* Buffere the XMLEncoder's output so that decoding errors do not cause
         * us to trash the current version of the specified file.
         */
        ByteArrayOutputStream bst = new ByteArrayOutputStream();
        try {
            e = new XMLEncoder(bst);
            
            if (!persistenceDelegatesInitialized) {
                e.setPersistenceDelegate(Rectangle.class, new RectanglePD());
                persistenceDelegatesInitialized = true;
            }
            
            e.setExceptionListener(el);
            e.writeObject(bean);
        } finally {
            if (e != null) {
                e.close();
            }
        }
        
        if (el.exception != null) {
            throw new IOException("save failed \"" + fileName + "\"", 
                    el.exception);
        }
        
        // NOTE: Converted try...finally block to try with resources, which 
        //+ automatically closes the resources created.
        try (OutputStream ost = openOutputFile(fileName)) {
            ost.write(bst.toByteArray());
        }
    }
    
    public Object load(String fileName) throws IOException {
        InputStream ist = null;
        try {
            ist = openInputFile(fileName);
        } catch (IOException e) {
            return null;
        }
        
        AbortExceptionListener el = new AbortExceptionListener();
        XMLDecoder d = null;
        try {
            d = new XMLDecoder(ist);
            d.setExceptionListener(el);
            Object bean = d.readObject();
            
            if (el.exception != null) {
                throw new IOException("load failed \"" + fileName + "\"", 
                        el.exception);
            }
            
            return bean;
        } finally {
            if (d != null) {
                d.close();
            }
        }
    }
    
    private void closeStream(Closeable st, String fileName) throws IOException {
        if (st != null) {
            try {
                st.close();
            } catch (IOException e) {
                throw new IOException("close failed \"" + fileName + "\"", e);
            }
        }
    }
    
    public long getStorageLimit() {
        return storageLimit;
    }
    
    public void setStorageLimit(long storageLimit) {
        if (storageLimit < -1L) {
            throw new IllegalArgumentException("invalid storageLimit");
        }
        
        long oldValue = this.storageLimit;
        this.storageLimit = storageLimit;
        firePropertyChange("storageLimit", oldValue, this.storageLimit);
    }
    
    private String getId(String key, String def) {
        ResourceMap appResourceMap = getContext().getResourceMap();
        String id = appResourceMap.getString(key);
        
        if (id == null) {
            logger.warning("unspecified resource " + key + " using " + def);
            id = def;
        } else if (id.trim().length() == 0) {
            logger.warning("empty resource " + key + " using " + def);
        }
        
        return id;
    }
    
    private String getApplicationId() {
        return getId("Application.id", 
                getContext().getApplicationClass().getSimpleName());
    }
    
    private String getVendorId() {
        return getId("Application.vendorId", "UnknownApplicationVendor");
    }
    
    /* The following enum and method only exist to distinguish Windows and OSX
     * for the sake of getDirectory().
     */
    private enum OSId {
        WINDOWS,
        OSX,
        UNIX
    }
    
    private OSId getOSId() {
        PrivilegedAction<String> doGetOsName = new PrivilegedAction<>() {
            @Override
            public String run() {
                return System.getProperty("os.name");
            }
            
        };
        
        OSId id = OSId.UNIX;
        String osName = AccessController.doPrivileged(doGetOsName);
        
        if (osName != null) {
            if (osName.toLowerCase().startsWith("mac os x")) {
                id = OSId.OSX;
            } else if (osName.contains("Windows")) {
                id = OSId.WINDOWS;
            }
        }
        
        return id;
    }
    
    public File getDirectory() {
        if (directory == unspecifiedFile) {
            directory = null;
            String userHome = null;
            
            try {
                userHome = System.getProperty("user.home");
            } catch (SecurityException ignore) {
                // Do nothing with the exception.
            }
            
            if (userHome != null) {
                String applicationId = getApplicationId();
                OSId osId = getOSId();
                
                if (osId == OSId.WINDOWS) {
                    File appDataDir = null;
                    
                    try {
                        String appDataEV = System.getenv("APPDATA");
                        
                        if ((appDataEV != null) && (appDataEV.length() > 0)) {
                            appDataDir = new File(appDataEV);
                        }
                    } catch (SecurityException ignore) {
                        // Do nothing with the exception.
                    }
                    
                    String vendorId = getVendorId();
                    
                    if ((appDataDir != null) && appDataDir.isDirectory()) {
                        // ${APPDATA}\${vendorId}\${applicationId}
                        String path = vendorId + "\\" + applicationId + "\\";
                        directory = new File(appDataDir, path);
                    } else {
                        // ${userHome}\Application Data\${vendorId}\${applicationId}
                        String path = "Application Data\\" + vendorId + "\\"
                                + applicationId + "\\";
                        directory = new File(userHome, path);
                    }
                } else if (osId == OSId.OSX) {
                    // ${userHome}/Library/Application Support/${applicationId}
                    String path = "Library/Application Support/" + applicationId 
                            + "/";
                    directory = new File(userHome, path);
                } else {
                    // ${userHome}/.${applicationId}/
                    String path = "." + applicationId + "/";
                    directory = new File(userHome, path);
                }
            }
        }
        
        return directory;
    }
    
    public void setDirectory(File directory) {
        File oldValue = this.directory;
        this.directory = directory;
        firePropertyChange("directory", oldValue, this.directory);
    }
    
    /* ORIGINAL COMMENT:
     * 
     * Papers over the fact that th eSTring, Throwable IOException constructor
     * was only introduced in Java 6.
     */
    /* *************************************************************************
       * Removed: private static class LSException extends IOException         *
       *                                                                       *
       * from this point in the class. The LSException was a kludge to allow   *
       * for throwing an exception that accepted two params: String, Throwable.*
       * The IOException, prior to Java 6, did not have a String, Throwable    *
       * constructor, so the LSException class provided that functionality. The*
       * secondary constructor to the LSException class was defined as:        *
       *                                                                       *
       *    public LSException(String s, Throwable e) {                        *
       *        super(s);                                                      *
       *        initCause(e);                                                  *
       *    }                                                                  *
       *                                                                       *
       * This kludge was created to convert various exceptions to IOExceptions *
       * to be thrown. I have deleted this kludge class, since the IOException *
       * class now provides a String, Throwable constructor and the kludge is  *
       * no longer needed.                                                     *
       *                                                                       *
       *                                    -- Sean Carrick, February 09, 2021 *
       *************************************************************************
       */
    
    /* ORIGINAL COMMENT:
     *
     * There are some (old) Java classes that are not proper beans. Rectangle is
     * one of these. When running within the secure sandbox, writing a Rectangle
     * with SMLEncoder causes a security exception because 
     * DefaultPersistenceDelegate calls Field.setAccessible(true) to gain access
     * to private fields. This is a workaround for that problem. A bug has been
     * filed, see JDK bug ID 4741757
    */
    /* *************************************************************************
       * Removed: private static class RectanglePD                             *
       *                             extends DefaultPersistenceDelegate        *
       * protected Expression instantiate(Object, Encoder)                     *
       *                                                                       *
       * from this point in the class. The RectanglePD was a kludge to work    *
       * around a security exception when using java.beans.XMLEncoder in a Java*
       * web start application. Since Java Web Start is no longer a supported  *
       * technology, and since the security exception was fixed in Java 6,     *
       * marked as Resolved: 2006-03-16, this class is no longer needed for the*
       * Swing Application Framework library.                                  *
       *                                                                       *
       * The RectanglePD class and instantiate() method were apparently used   *
       * for JNLP launched applications. Since Oracle dropped support for JNLP *
       * in Java 11, as of March 2018, we are not including this functionality *
       * in this update of Swing Application Framework, mostly because the JNLP*
       * API library is no longer included with Java. If we were to include    *
       * this functionality for backwards compatibility, we would need to also *
       * include a library that replaces JNLP, such as IcedTea-Web or Karakun  *
       * AG's OpenWebStart library.                                            *
       *                                                                       *
       * Since our goal in updating the Swing Application Framework is to rely *
       * only upon pure Java/Swing libraries, we are just dropping support for *
       * JNLP functionality.                                                   *
       *                                                                       *
       * To this end, I am including the definitions of any JNLP-related       *
       * classes/methods in this class, and am adding JavaDoc comments to them *
       * that state that the functionality is no longer supported.             *
       *                                                                       *
       *                                    -- Sean Carrick, February 09, 2021 *
       *************************************************************************
       */
    
    /**
     * This class is no longer supported since support of JNLP (Java Web Start)
     * was dropped by Oracle, Inc., in March of 2018, as of Java 11. The class
     * now contains no body, constructors (except default), nor methods.
     * 
     * @deprecated 
     * @since March 2018 by Oracle, Inc.
     */
    @Deprecated
    private static class RectanglePD extends DefaultPersistenceDelegate {
        
    }
    
    /**
     * This method is no longer supported since support of JNLP (Java Web Start)
     * was dropped by Oracle, Inc., in March of 2018, as of Java 11. The method
     * now simply returns `null` if called.
     * 
     * @deprecated 
     * @since March 2018 by Oracle, Inc.
     * 
     * @param oldInstance
     * @param out
     * @return null
     */
    @Deprecated
    protected Expression instantiate(Object oldInstance, Encoder out) {
        return null;
    }
    
    private synchronized LocalIO getLocalIO() {
        if (localIO == null) {
            localIO = getPersistenceServiceIO();
            
            if (localIO == null) {
                localIO = new LocalFileIO();
            }
        }
        
        return localIO;
    }
    
    private abstract class LocalIO {
        public abstract InputStream openInputFile(String fileName) throws IOException;
        public abstract OutputStream openOutputFile(String fileName) throws IOException;
        public abstract boolean deleteFile(String fileName) throws IOException;
    }
    
    private class LocalFileIO extends LocalIO {

        @Override
        public InputStream openInputFile(String fileName) throws IOException {
            File path = new File(getDirectory(), fileName);
            
            try {
                return new BufferedInputStream(new FileInputStream(path));
            } catch (IOException e) {
                throw new IOException("could not open input file \"" + fileName
                        + "\"", e);
            }
        }

        @Override
        public OutputStream openOutputFile(String fileName) throws IOException {
            File dir = getDirectory();
            if (!dir.isDirectory()) {
                if (!dir.mkdirs()) {
                    throw new IOException("could not create directory " + dir);
                }
            }
            
            File path = new File(dir, fileName);
            try {
                return new BufferedOutputStream(new FileOutputStream(path));
            } catch (IOException e) {
                throw new IOException("could not open output file \"" + fileName
                        + "\"", e);
            }
        }

        @Override
        public boolean deleteFile(String fileName) throws IOException {
            File path = new File(getDirectory(), fileName);
            return path.delete();
        }
        
    }


    /* ORIGINAL COMMENT:
     * 
     * Determine if we are a web started application and the JNLP
     * PersistenceService is available without forcing the JNLP API to be
     * class-loaded. We do not want to require apps that are not web started
     * to bundle javaws.jar.
     */
    /**
     * This method is no longer supported since support of JNLP (Java Web Start)
     * was dropped by Oracle, Inc., in March of 2018, as of Java 11. The method
     * now simply returns `null` if called.
     * 
     * @deprecated 
     * @since March 2018 by Oracle, Inc.
     * 
     * @return null
     */
    @Deprecated
    private LocalIO getPersistenceServiceIO() {
        return null;
    }
    
    /*
    ****************************************************************************
    * Since JNLP is no longer a valid technology due to security reasons, I    *
    * removed the method:                                                      *
    *                                                                          *
    * private LocalIO getPersistenceServiceIO()                                *
    * private class PersistenceServiceIO extends localIO                       *
    *                                                                          *
    * from this point in the class. Due to removing these items,               *
    * the LocalStorage class is now complete.
    *                                       -- Sean Carrick, February 09, 2021 *
    ****************************************************************************
    */
    /**
     * This class is no longer supported since support of JNLP (Java Web Start)
     * was dropped by Oracle, Inc., in March of 2018, as of Java 11. The class
     * now contains no body, constructors (except default), nor methods.
     * 
     * @deprecated 
     * @since March 2018 by Oracle, Inc.
     */
    @Deprecated
    private class PersistenceServiceIO extends LocalIO {

    /**
     *This method is no longer supported since support of JNLP (Java Web Start)
     * was dropped by Oracle, Inc., in March of 2018, as of Java 11. The method
     * now simply returns `null` if called.
     * 
     * @deprecated 
     * @since March 2018 by Oracle, Inc.
     * 
     * @param fileName
     * @return null
     */
        @Override
        @Deprecated
        public InputStream openInputFile(String fileName) throws IOException {
            return null;
        }

    /**
     *This method is no longer supported since support of JNLP (Java Web Start)
     * was dropped by Oracle, Inc., in March of 2018, as of Java 11. The method
     * now simply returns `null` if called.
     * 
     * @deprecated 
     * @since March 2018 by Oracle, Inc.
     * 
     * @param fileName
     * @return null
     */
        @Override
        @Deprecated
        public OutputStream openOutputFile(String fileName) throws IOException {
            return null;
        }

    /**
     *This method is no longer supported since support of JNLP (Java Web Start)
     * was dropped by Oracle, Inc., in March of 2018, as of Java 11. The method
     * now simply returns `null` if called.
     * 
     * @deprecated 
     * @since March 2018 by Oracle, Inc.
     * 
     * @param fileName
     * @return null
     */
        @Override
        @Deprecated
        public boolean deleteFile(String fileName) throws IOException {
            return false;
        }
        
    }
}
