package de.tudresden.inf.mci.brailleplot.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * This class is used to make files and resources available across the app independent from the application running as packed jar or not.
 * @author Leonard Kupper, Georg Graßnick
 * @version 2019-09-24
 */
public final class GeneralResource {

    private String mResourcePath;
    private boolean mValidExternalFile = false;
    private boolean mValidPackedResource = false;
    private static boolean mExportOverride = !isRunFromCompiledJar();
    private static File tempDirectory = new File(System.getProperty("java.io.tmpdir") + File.separator + "brailleplot");
    private static Logger mLogger = LoggerFactory.getLogger(getClassRef());

    /**
     * Create a resource from a file path or resource classpath.
     * @param resourcePath Relative or local path to a file (e.g. C:\example.txt) in the filesystem or classpath pointing to (packed) resource location (e.g. /config/default.properties)
     * @throws IOException If the given path neither determines a valid external file, nor a valid resource.
     */
    public GeneralResource(final String resourcePath) throws IOException {
        this(resourcePath, null);
    }

    /**
     * Create a resource from a file path or resource classpath. This will first check the given file path if it exists as it is, else interpret as absolute, else relative to the set search path, else as jar resource.
     * @param resourcePath Relative or local path to a file (e.g. C:\example.txt) in the filesystem or classpath pointing to (packed) resource location (e.g. /config/default.properties)
     * @param searchPath Relative or local path to be used as base for interpreting the resourcePath relatively to additionally.
     * @throws IOException If the given path neither determines a valid external file, nor a valid resource.
     */
    public GeneralResource(final String resourcePath, final String searchPath) throws IOException {
        File checkFile = new File(resourcePath);
        mLogger.debug("Checking referenced path: " + checkFile);
        if (checkFile.isFile()) {
            mLogger.trace("Interpreting path as file: " + checkFile.getCanonicalPath());
            mResourcePath = checkFile.getCanonicalPath();
            mValidExternalFile = true;
        }
        checkFile = checkFile.getAbsoluteFile();
        mLogger.debug("Checking referenced path as absolute path: " + checkFile);
        if (checkFile.isFile()) {
            mLogger.trace("Interpreting path as absolute file: " + checkFile.getCanonicalPath());
            mResourcePath = checkFile.getCanonicalPath();
            mValidExternalFile = true;
        }
        if (Objects.nonNull(searchPath)) {
            checkFile = new File(searchPath + File.separator + resourcePath);
            mLogger.debug("Looking for referenced path in search path: " + checkFile);
            if (checkFile.isFile()) {
                mLogger.trace("Interpreting path as search path relative file: " + checkFile.getCanonicalPath());
                mResourcePath = checkFile.getCanonicalPath();
                mValidExternalFile = true;
            }
        }
        String resourceSearchPath = searchPath;
        if (resourceSearchPath != null) {
            resourceSearchPath = stripJarPath(resourceSearchPath);
        }
        String resourceClassPath = stripJarPath(resourcePath);
        resourceClassPath = resourceClassPath.replace(File.separator, "/"); // class paths are always separated by forward slash
        InputStream checkStream = getClass().getClassLoader().getResourceAsStream(resourceClassPath);
        mLogger.debug("Checking referenced path as resource: " + resourceClassPath);
        if (Objects.nonNull(checkStream)) {
            mLogger.trace("Interpreting path as resource stream: " + resourceClassPath);
            mResourcePath = resourceClassPath;
            mValidPackedResource = true;
        }
        if (Objects.nonNull(resourceSearchPath)) {
            String relativeResourcePath = new File(resourceSearchPath + File.separator + resourceClassPath).toPath().normalize().toString();
            relativeResourcePath = relativeResourcePath.replace("\\", "/");
            checkStream = getClass().getClassLoader().getResourceAsStream(relativeResourcePath);
            mLogger.debug("Checking referenced path as search path relative resource: " + relativeResourcePath);
            if (Objects.nonNull(checkStream)) {
                mLogger.trace("Interpreting path as resource stream: " + relativeResourcePath);
                mResourcePath = relativeResourcePath;
                mValidPackedResource = true;
            }
        }
        if (!(isValidExternalFile() || isValidPackedResource())) {
            throw new FileNotFoundException("Not recognized as valid file or resource: " + resourcePath);
        }
    }

    public boolean isValidExternalFile() {
        return mValidExternalFile;
    }

    public boolean isValidPackedResource() {
        return mValidPackedResource;
    }

    /**
     * Open the resource as InputStream. The caller is supposed to close the stream on his own after usage.
     * @return InputStream of the external file / jar resource represented by this instance.
     */
    public InputStream getInputStream() {
        try {
            if (isValidExternalFile()) {
                return new FileInputStream(getResourcePath());
            }
            return getClass().getClassLoader().getResourceAsStream(mResourcePath);
        } catch (Exception e) {
            throw new RuntimeException("Error while opening resource as stream: ", e);
        }
    }

    /**
     * Get the validated path pointing to this resource.
     * @return String containing either a file path or a resource classpath.
     */
    public String getResourcePath() {
        return mResourcePath;
    }


    /*
        STATIC HELP METHODS
        ===================
     */

    /**
     * Get a jar resource as file. When the app runs from a jar this will cause the resource to be exported to the filesystem.
     * @param path Absolute classpath pointing to jar resource. Omit the leading "/". If this point to a resource directory the directory and all contents are exported.
     * @return A File instance representing the resource on the file system.
     */
    public static File getOrExportResourceFile(final String path) {
        if (isRunFromCompiledJar()) {
            try {
                JarFile jar = openJarFile();
                final Enumeration<JarEntry> packedFiles = jar.entries();
                while (packedFiles.hasMoreElements()) {
                    final String name = packedFiles.nextElement().getName();
                    if (name.startsWith(path)) { // filter for subpath
                        Path target = tempDirectory.toPath().resolve(name);
                        // create parent directory if needed
                        File parentDir = target.toFile();
                        if (!name.endsWith("/")) {
                            parentDir = target.toFile().getParentFile();
                        }
                        if (Objects.nonNull(parentDir)) {
                            if (parentDir.mkdirs()) {
                                mLogger.trace("Created runtime resource directory: " + parentDir.getAbsolutePath());
                            }
                        }
                        // export resource file
                        InputStream currentResource = getClassRef().getResourceAsStream("/" + name); // preceding slash for absolute classpath reference
                        if (Objects.nonNull(currentResource)) {
                            if (mExportOverride || target.toFile().createNewFile()) {
                                long bytes = Files.copy(currentResource, target, StandardCopyOption.REPLACE_EXISTING);
                                mLogger.trace("Exported runtime resource '" + name + "' to '" + target + "' (" + bytes + " bytes)");
                            }
                        }
                    }
                }
                jar.close();
                return tempDirectory.toPath().resolve(path).toFile();
            } catch (Exception e) {
                throw new RuntimeException("Error while reading jar contents.", e);
            }
        } else {
            Class cl = getClassRef();
            URL resource = cl.getResource("/" + path); // preceding slash for absolute classpath reference
            String directoryPath = resource.getPath();
            return new File(directoryPath);
        }
    }

    private static JarFile openJarFile() {
        try {
            if (!isRunFromCompiledJar()) {
                throw new IllegalStateException("Not running from jar.");
            }
            File jarFile = new File(getClassRef().getProtectionDomain().getCodeSource().getLocation().getPath());
            return new JarFile(jarFile);
        } catch (Exception e) {
            throw new RuntimeException("Error while retrieving JarFile reference.", e);
        }
    }

    private static boolean isRunFromCompiledJar() {
        return (getClassRef().getResource("").getProtocol().equals("jar"));
    }

    private static Class getClassRef() {
        return GeneralResource.class;
    }

    /**
     * Removes the the jar file path from the given path.
     * Example: file:/home/user/BraillePlot.jar!/config/example.properties -> config/example.properties.
     * Note that this method identifies the distinction between jar path and inner path by the '!' (exclamation mark) character. Please
     * make sure your packaged resource does not contain this character in it's name or path. If you don't, things will break.
     * @param path The string representation of the path to use.
     * @return The input path where any leading path outside of the jar file is truncated.
     */
    private static String stripJarPath(final String path) {
        String ret = path.substring(path.lastIndexOf('!') + 1);
        if (ret.startsWith("/")) {
            ret = ret.substring(1);
        }
        return ret;
    }
}
