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
 *  Class      :   LocalStorage.java
 *  Author     :   Sean Carrick
 *  Created    :   Feb 11, 2021 @ 8:12:41 AM
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.Logger;
//import javax.jnlp.BasicService;
//import javax.jnlp.FileContents;
//import javax.jnlp.PersistenceService;
//import javax.jnlp.ServiceManager;
//import javax.jnlp.UnavailableServiceException;

/**
 * Access to per application, per user, local file storage.
 *
 * @see ApplicationContext#getLocalStorage() 
 * @see SessionStorage
 * 
 * @author Hans Muller (Original Author) &lt;current email unknown&gt;
 * @author Sean Carrick (Updater) &lt;sean at pekinsoft dot com&gt;
 * 
 * @version 1.05
 * @since 1.03
 */
public class LocalStorage extends AbstractBean {

    private static final Logger logger = Logger.getLogger(LocalStorage.class.getName());
    private final ApplicationContext context;
    private long storageLimit = -1L;
    private LocalIO localIO = null;
    private final File unspecifiedFile = new File("unspecified");
    private File directory = unspecifiedFile;

    protected LocalStorage(ApplicationContext context) {
        if (context == null) {
            throw new IllegalArgumentException("null context");
        }
        this.context = context;
    }

    /**
     * Retrieves the <code>ApplicationContext</code> under which the current
     * <code>Application</code> is running. The <code>ApplicationContext</code>
     * provides access to various information about the application, including
     * its resources and storage system.
     * 
     * @return the context under which the application is running
     */
    protected final ApplicationContext getContext() {
        return context;
    }

    private void checkFileName(String fileName) {
        if (fileName == null) {
            throw new IllegalArgumentException("null fileName");
        }
    }

    /**
     * Retrieves a <code>java.io.InputStream</code> object from which file data
     * may be read.
     * 
     * @param fileName name of the file
     * @return an <code>InputStream</code> through which the file contents may
     *          be read
     * @throws IOException in the event an error occurs accessing the file
     * 
     * @see java.io.InputStream
     */
    public InputStream openInputFile(String fileName) throws IOException {
        checkFileName(fileName);
        return getLocalIO().openInputFile(fileName);
    }

    /**
     * Retrieves a <code>java.io.OutputStream</code> object to which file data
     * may be written.
     * 
     * @param fileName name of the file
     * @return an <code>OutputStream</code> through which the file contents may
     *          be written
     * @throws IOException in the event an error occurs accessing the file
     * 
     * @see java.io.OutputStream
     */
    public OutputStream openOutputFile(String fileName) throws IOException {
        checkFileName(fileName);
        return getLocalIO().openOutputFile(fileName);
    }

    /**
     * Provides a means by which the file specified by <code>fileName</code> may
     * be deleted from the underlying disk.
     * 
     * @param fileName the name of the file to delete
     * @return <code>true</code> if the file is successfully deleted; 
     *          <code>false</code> otherwise
     * @throws IOException in the event an error occurs accessing the file
     * 
     * @see java.io.File#delete() 
     */
    public boolean deleteFile(String fileName) throws IOException {
        checkFileName(fileName);
        return getLocalIO().deleteFile(fileName);
    }

    /* If an exception occurs in the XMLEncoder/Decoder, we want
     * to throw an IOException.  The exceptionThrow listener method
     * doesn't throw a checked exception so we just set a flag
     * here and check it when the encode/decode operation finishes
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
        /* Buffer the XMLEncoder's output so that decoding errors don't
	 * cause us to trash the current version of the specified file.
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
        try (XMLDecoder d = new XMLDecoder(ist)) {
            d.setExceptionListener(el);
            Object bean = d.readObject();
            if (el.exception != null) {
                throw new IOException("load failed \"" + fileName + "\"", 
                        el.exception);
            }
            return bean;
        }
    }

    private void closeStream(Closeable st, String fileName) throws IOException {
        if (st != null) {
            try {
                st.close();
            } catch (java.io.IOException e) {
                throw new IOException("close failed \"" + fileName + "\"", e);
            }
        }
    }

    /**
     * Retrieves the storage limit on size of files or disk space.
     * 
     * @return size limit for storage
     */
    public long getStorageLimit() {
        return storageLimit;
    }

    /**
     * Sets the storage limit on sizes of files or disk space allowed.
     * 
     * @param storageLimit size limit for storage
     */
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
            logger.log(Level.WARNING, "unspecified resource {0} using {1}", 
                    new Object[]{key, def});
            id = def;
        } else if (id.trim().length() == 0) {
            logger.log(Level.WARNING, "empty resource {0} using {1}", 
                    new Object[]{key, def});
            id = def;
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

    /* The following enum and method only exist to distinguish 
     * Windows and OSX for the sake of getDirectory().
     */
    private enum OSId {
        WINDOWS, OSX, UNIX
    }

    private OSId getOSId() {
        PrivilegedAction<String> doGetOSName = () -> System.getProperty("os.name");
        OSId id = OSId.UNIX;
        String osName = AccessController.doPrivileged(doGetOSName);
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
            }
            if (userHome != null) {
                String applicationId = getApplicationId();
                OSId osId = getOSId();
                if (osId == null) {
                    // ${userHome}/.${applicationId}/
                    String path = "." + applicationId + "/";
                    directory = new File(userHome, path);
                } else switch (osId) {
                    case WINDOWS:
                        File appDataDir = null;
                        try {
                            String appDataEV = System.getenv("APPDATA");
                            if ((appDataEV != null) && (appDataEV.length() > 0)) {
                                appDataDir = new File(appDataEV);
                            }
                        } catch (SecurityException ignore) {
                        }   String vendorId = getVendorId();
                        if ((appDataDir != null) && appDataDir.isDirectory()) {
                            // ${APPDATA}\{vendorId}\${applicationId}
                            String path = vendorId + "\\" + applicationId + "\\";
                            directory = new File(appDataDir, path);
                        } else {
                            // ${userHome}\Application Data\${vendorId}\${applicationId}
                            String path = "Application Data\\" + vendorId + "\\" 
                                    + applicationId + "\\";
                            directory = new File(userHome, path);
                        }   break;
                    case OSX:
                        {
                            // ${userHome}/Library/Application Support/${applicationId}
                            String path = "Library/Application Support/" 
                                    + applicationId + "/";
                            directory = new File(userHome, path);
                            break;
                        }
                    default:
                        {
                            // ${userHome}/.${applicationId}/
                            String path = "." + applicationId + "/";
                            directory = new File(userHome, path);
                            break;
                        }
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

    /* Papers over the fact that the String,Throwable IOException 
     * constructor was only introduced in Java 6.
     */
    /* *************************************************************************
       * Removed definition of `LSException extends IOException` from this     *
       * location because it was simply a kludge due to the `IOException` not  * 
       * having a:                                                             *
       * `public IOException(java.lang.String, java.lang.throwable.Throwable)` *
       * constructor available prior to JDK6. Since this API is being updated  *
       * to JDK11, it will support the standard `IOException` now without the  *
       * kludge. I have also changed all of the calls to the `LSException`     *
       * constructor with the calls to the `IOException` constructor throughout*
       * this class.                                                           *
       *                                                                       *
       *                                         -> Sean Carrick, Feb 11, 2021 *
       *************************************************************************
    */
    

    /* There are some (old) Java classes that aren't proper beans.  Rectangle
     * is one of these.  When running within the secure sandbox, writing a 
     * Rectangle with XMLEncoder causes a security exception because 
     * DefaultPersistenceDelegate calls Field.setAccessible(true) to gain
     * access to private fields.  This is a workaround for that problem.
     * A bug has been filed, see JDK bug ID 4741757  
     */
    /* *************************************************************************
       * The JDK bug #4741757 was fixed in May of 2003, according to the bug   *
       * database. Therefore, the `RectanglePD` class is no longer necessary   *
       * so I have deprecated it. `instantiate(java.lang.Object,               *
       * java.beans.Encoder)` will simply return `null` from this point on and *
       * the class will simply create an empty object. I see no harm in doing  *
       * this, as this class was only used for JNLP situations and JNLP support*
       * was dropped by Oracle, Inc., in March of 2018, so we no longer need to*
       * provide support for it. If a developer wants to use JNLP, they will   *
       * need to obtain a new library that provides that API, such as IcedTea. *
       * Since there are now multiple APIs that provide the JNLP support, it   *
       * would be an added difficulty to us to have to maintain multiple JNLP  *
       * classes/methods that allow the developers using this Framework to be  *
       * able to count on this Framework to provide access to/from JNLP apps.  *
       *                                                                       *
       *                                         -> Sean Carrick, Feb 11, 2021 *
       *************************************************************************
    */
    @Deprecated
    private static class RectanglePD extends DefaultPersistenceDelegate {

        @Deprecated
        public RectanglePD() {
            
        }

        @Deprecated
        protected Expression instantiate(Object oldInstance, Encoder out) {
            return null;
        }
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
                throw new IOException("couldn't open input file \"" + fileName 
                        + "\"", e);
            }
        }

        @Override
        public OutputStream openOutputFile(String fileName) throws IOException {
            File dir = getDirectory();
            if (!dir.isDirectory()) {
                if (!dir.mkdirs()) {
                    throw new IOException("couldn't create directory " + dir);
                }
            }
            File path = new File(dir, fileName);
            try {
                return new BufferedOutputStream(new FileOutputStream(path));
            } catch (IOException e) {
                throw new IOException("couldn't open output file \"" + fileName
                        + "\"", e);
            }
        }

        @Override
        public boolean deleteFile(String fileName) throws IOException {
            File path = new File(getDirectory(), fileName);
            return path.delete();
        }
    }

    /* Determine if we're a web started application and the
     * JNLP PersistenceService is available without forcing
     * the JNLP API to be class-loaded.  We don't want to 
     * require apps that aren't web started to bundle javaws.jar
     */
    /*
    ****************************************************************************
    * As per the note above discussing the `RectanglePD` class, JNLP is no     *
    * longer officially supported by Java and Oracle, Inc. Therefore, the      *
    * method below has also been deprecated, as it relies on the deprecated    *
    * class `PersistenceServiceIO` for JNLP launched applications. However, in *
    * this instance, I have left all of the code for the `PersistenceServiceIO`*
    * class in place, just commented out so that no functionality may be       *
    * provided. I did this for historical purposes, in case we do decide to    *
    * provide some web-launched capability for a third-party API, such as      *
    * IcedTea.                                                                 *
    *                                                                          *
    *                                            -> Sean Carrick, Feb 11, 2021 *
    ****************************************************************************
    */
    private LocalIO getPersistenceServiceIO() {
        try {
            Class smClass = Class.forName("javax.jnlp.ServiceManager");
            Method getServiceNamesMethod = smClass.getMethod("getServiceNames");
            String[] serviceNames = (String[]) getServiceNamesMethod.invoke(null);
            boolean psFound = false;
            boolean bsFound = false;
            for (String serviceName : serviceNames) {
                if (serviceName.equals("javax.jnlp.BasicService")) {
                    bsFound = true;
                } else if (serviceName.equals("javax.jnlp.PersistenceService")) {
                    psFound = true;
                }
            }
            if (bsFound && psFound) {
                return new PersistenceServiceIO();
            }
        } catch (ClassNotFoundException 
                | IllegalAccessException 
                | IllegalArgumentException 
                | NoSuchMethodException 
                | SecurityException 
                | InvocationTargetException ignore) {
            // either the classes or the services can't be found
        }
        return null;
    }

    /**
     * The <tt>PersistenceServiceIO</tt> class presented methods for accessing
     * systems via the Java Network Launch Protocol (JNLP). However, support for
     * JNLP was discontinued in March 2018 by Oracle, Inc. Therefore, we have
     * deprecated this class, since its use would require providing another
     * library <tt>JAR</tt> file, such as IceTea. In keeping the number of files
     * to be distributed smaller, we just will not provide for JNLP launching.
     *
     * @deprecated
     */
    @Deprecated
    private class PersistenceServiceIO extends LocalIO {

//        private BasicService bs;
//        private PersistenceService ps;
//
//        private String initFailedMessage(String s) {
//            return getClass().getName() + " initialization failed: " + s;
//        }
        /**
         * This constructor will not create anything except for an empty object.
         *
         * @deprecated
         */
        @Deprecated
        PersistenceServiceIO() {
//            try {
//                bs = (BasicService) ServiceManager.lookup("javax.jnlp.BasicService");
//                ps = (PersistenceService) ServiceManager.lookup("javax.jnlp.PersistenceService");
//            } catch (UnavailableServiceException e) {
//                logger.log(Level.SEVERE, initFailedMessage("ServiceManager.lookup"), e);
//                bs = null;
//                ps = null;
//            }
        }

//        private void checkBasics(String s) throws IOException {
//            if ((bs == null) || (ps == null)) {
//                throw new IOException(initFailedMessage(s));
//            }
//        }
//
//        private URL fileNameToURL(String name) throws IOException {
//            try {
//                return new URL(bs.getCodeBase(), name);
//            } catch (MalformedURLException e) {
//                throw new LSException("invalid filename \"" + name + "\"", e);
//            }
//        }
        /**
         * <tt>openInputFile</tt> will only return a null value.
         *
         * @param fileName
         * @return
         * @throws IOException
         * @deprecated
         */
        @Deprecated
        @Override
        public InputStream openInputFile(String fileName) throws IOException {
//            checkBasics("openInputFile");
//            URL fileURL = fileNameToURL(fileName);
//            try {
//                return new BufferedInputStream(ps.get(fileURL).getInputStream());
//            } catch (Exception e) {
//                throw new LSException("openInputFile \"" + fileName + "\" failed", e);
//            }
            return null;
        }

        /**
         * <tt>openOutputFile</tt> will only return a null value.
         *
         * @param fileName
         * @return
         * @throws IOException
         * @deprecated
         */
        @Deprecated
        @Override
        public OutputStream openOutputFile(String fileName) throws IOException {
//            checkBasics("openOutputFile");
//            URL fileURL = fileNameToURL(fileName);
//            try {
//                FileContents fc = null;
//                try {
//                    fc = ps.get(fileURL);
//                } catch (FileNotFoundException e) {
//                    /* Verify that the max size for new PersistenceService 
//		     * files is >= 100K (2^17) before opening one.
//                     */
//                    long maxSizeRequest = 131072L;
//                    long maxSize = ps.create(fileURL, maxSizeRequest);
//                    if (maxSize >= maxSizeRequest) {
//                        fc = ps.get(fileURL);
//                    }
//                }
//                if ((fc != null) && (fc.canWrite())) {
//                    return new BufferedOutputStream(fc.getOutputStream(true));
//                } else {
//                    throw new IOException("unable to create FileContents object");
//                }
//            } catch (Exception e) {
//                throw new LSException("openOutputFile \"" + fileName + "\" failed", e);
//            }

            return null;
        }

        /**
         * <tt>deleteFile</tt> will only return <tt>false</tt>.
         *
         * @param fileName
         * @return
         * @throws IOException
         * @deprecated
         */
        @Deprecated
        @Override
        public boolean deleteFile(String fileName) throws IOException {
//            checkBasics("deleteFile");
//            URL fileURL = fileNameToURL(fileName);
//            try {
//                ps.delete(fileURL);
//                return true;
//            } catch (Exception e) {
//                throw new LSException("openInputFile \"" + fileName + "\" failed", e);
//            }

            return false;
        }
    } // End of PersistenceServiceIO class
}
