/*
 * Copyright 2021 OPPO ESA Stack Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.esastack.cabin.container.service.loader;

import io.esastack.cabin.api.service.loader.ClassLoaderService;
import io.esastack.cabin.api.service.share.SharedClassService;
import io.esastack.cabin.api.service.share.SharedResourceService;
import io.esastack.cabin.common.CompoundEnumeration;
import io.esastack.cabin.common.constant.Constants;
import io.esastack.cabin.common.exception.CabinLoaderException;
import io.esastack.cabin.common.exception.CabinRuntimeException;
import io.esastack.cabin.common.log.CabinLoggerFactory;
import io.esastack.cabin.common.util.CabinStringUtil;
import io.esastack.cabin.container.service.CabinServiceManager;
import io.esastack.cabin.loader.jar.Handler;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

public abstract class AbstractClassLoader extends URLClassLoader {

    private static final Logger LOGGER = CabinLoggerFactory.getLogger(AbstractClassLoader.class);

    private static final String[] CABIN_LOAD_PACKAGES = new String[]{
            "io.esastack.cabin.api",
            "io.esastack.cabin.common",
            "io.esastack.cabin.loader",
            "io.esastack.cabin.support.bootstrap"
    };

    protected final String moduleName;

    protected final ClassLoaderService classLoaderService =
            CabinServiceManager.get().getService(ClassLoaderService.class);

    protected final SharedClassService sharedClassService =
            CabinServiceManager.get().getService(SharedClassService.class);

    protected final SharedResourceService sharedResourceService =
            CabinServiceManager.get().getService(SharedResourceService.class);

    public AbstractClassLoader(final String moduleName, final URL[] urls) {
        super(urls, null);
        this.moduleName = moduleName;
        if (LOGGER.isDebugEnabled()) {
            LOGGER.info(CabinStringUtil.urlsToString("ClassLoader URLs of Module {" + moduleName + "} : ", urls));
        }
    }

    public String getModuleName() {
        return moduleName;
    }

    @Override
    protected Class<?> loadClass(final String name, final boolean resolve) throws CabinLoaderException {

        if (CabinStringUtil.isBlank(name)) {
            throw new CabinRuntimeException("class name is blank");
        }

        synchronized (getClassLoadingLock(name)) {
            Handler.setUseFastConnectionExceptions(true);
            try {
                doDefinePackage(name);
                final Class<?> clazz = loadClass0(name, resolve);
                if (resolve) {
                    resolveClass(clazz);
                }
                return clazz;
            } finally {
                Handler.setUseFastConnectionExceptions(false);
            }
        }
    }

    /**
     * Behaviour inherited from JDK Classloader, only affected sealed package.
     */
    private void doDefinePackage(final String clazzName) {
        final int lastIndex = clazzName.lastIndexOf('.');
        if (lastIndex >= 0) {
            final String packName = clazzName.substring(0, lastIndex);
            if (getPackage(packName) == null) {
                try {
                    doDefinePackage(clazzName, packName);
                } catch (Throwable ex) {
                    //Ignore
                }
            }
        }
    }

    private void doDefinePackage(final String clazzName, final String packName) {
        try {
            AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> {
                final StringBuilder packBuf = new StringBuilder();
                final StringBuilder clazzBuf = new StringBuilder();
                final String packageEntryName = packBuf.append(packName.replace('.', '/'))
                        .append("/")
                        .toString();
                final String classEntryName = clazzBuf.append(clazzName.replace('.', '/'))
                        .append(".class")
                        .toString();
                for (URL url : getURLs()) {
                    try {
                        final URLConnection connection = url.openConnection();
                        if (connection instanceof JarURLConnection) {
                            final JarFile jarFile = ((JarURLConnection) connection).getJarFile();
                            if (jarFile.getEntry(classEntryName) != null
                                    && jarFile.getEntry(packageEntryName) != null
                                    && jarFile.getManifest() != null) {
                                definePackage(packName, jarFile.getManifest(), url);
                                return null;
                            }
                        }
                    } catch (IOException ex) {
                        // Ignore
                    }
                }
                return null;
            }, AccessController.getContext());
        } catch (java.security.PrivilegedActionException ex) {
            // Ignore
        }
    }

    @Override
    public Enumeration<URL> getResources(final String name) throws IOException {
        Handler.setUseFastConnectionExceptions(true);
        try {
            if (name == null) {
                throw new CabinRuntimeException("Could not find resource for null");
            }
            return getResources0(name);
        } finally {
            Handler.setUseFastConnectionExceptions(false);
        }
    }

    @Override
    public URL getResource(final String name) {
        Handler.setUseFastConnectionExceptions(true);
        try {
            if (name == null) {
                throw new NullPointerException();
            }
            return getResource0(name);
        } finally {
            Handler.setUseFastConnectionExceptions(false);
        }
    }

    protected abstract Class<?> loadClass0(String name, boolean resolve) throws CabinLoaderException;

    protected abstract Enumeration<URL> getResources0(String name) throws IOException;

    protected abstract URL getResource0(String name);

    public Class<?> loadClassFromClasspath(final String name) throws CabinLoaderException {
        Class<?> clazz = loadFromRecords(name);
        if (clazz == null) {
            clazz = loadLocalClass(name);
        }
        return clazz;
    }

    protected Class<?> loadImportClassFromBiz(final String name) throws CabinLoaderException {
        if (shouldImportClassFromBiz(name)) {
            try {
                final Class<?> clazz = ((AbstractClassLoader) classLoaderService.getBizModuleClassLoader())
                        .loadClassFromClasspath(name);
                if (clazz != null) {
                    return clazz;
                }
            } catch (Throwable t) {
                throw new CabinLoaderException("Failed to load from biz classloader", t);
            }
            LOGGER.warn("Cannot load imported class {} of module {} from biz classloader", name, getModuleName());
        }
        return null;
    }

    protected Class<?> loadFromRecords(final String name) throws CabinLoaderException {
        try {
            return findLoadedClass(name);
        } catch (Throwable t) {
            throw new CabinLoaderException(String.format("Failed to find %s from loaded classes", name), t);
        }
    }

    protected Class<?> loadJdkClass(final String name) throws CabinLoaderException {
        try {
            return classLoaderService.getExtClassLoader().loadClass(name);
        } catch (ClassNotFoundException e) {
            //ignore
        } catch (Throwable e) {
            throw new CabinLoaderException(String.format("Failed to load %s from jdk classes", name), e);
        }
        return null;
    }

    protected Class<?> loadCabinClass(final String name) throws CabinLoaderException {
        boolean shouldLoad = false;
        for (String pkg : CABIN_LOAD_PACKAGES) {
            if (name.startsWith(pkg)) {
                shouldLoad = true;
                break;
            }
        }

        if (shouldLoad) {
            try {
                return classLoaderService.getCabinClassLoader().loadClass(name);
            } catch (ClassNotFoundException e) {
                //ignore
            } catch (Throwable t) {
                throw new CabinLoaderException(
                        String.format("Failed to load class %s from container classloader", name), t);
            }
        }
        return null;
    }

    protected Class<?> loadSharedClass(final String name) throws CabinLoaderException {
        if (sharedClassService != null) {
            try {
                return sharedClassService.getSharedClass(name);
            } catch (Throwable t) {
                throw new CabinLoaderException(String.format("Failed to load %s from shared classes", name), t);
            }
        }
        return null;
    }

    protected Class<?> loadBizClass(final String name) throws CabinLoaderException {
        try {
            return ((AbstractClassLoader) classLoaderService.getBizModuleClassLoader()).loadClassFromClasspath(name);
        } catch (ClassNotFoundException e) {
            return null;
        } catch (Throwable t) {
            throw new CabinLoaderException("Failed to load from biz classloader", t);
        }
    }

    protected Class<?> loadLocalClass(final String name) throws CabinLoaderException {
        try {
            return findClass(name);
        } catch (ClassNotFoundException e) {
            //ignore, if not found, return null
        } catch (Throwable t) {
            throw new CabinLoaderException(String.format("Failed to load %s from classpath", name), t);
        }
        return null;
    }

    /**
     * Why we need to load Agent Classes ?
     * The classes in agent jars may be used to enhance the Biz classes and Lib classes, using javassist or asm;
     * So the agent classes and methods may appear in the enhanced class byte code, as these classes executing, the
     * agent classes would be loaded by the Classloader of the enhanced class.
     * Here, only classes in agent local classpath should be loaded.
     */
    protected Class<?> loadAgentClass(final String name) throws CabinLoaderException {
        for (Map.Entry<String, ClassLoader> entry : classLoaderService.getJavaAgentModuleClassLoaders().entrySet()) {
            final String agentUrl = entry.getKey();
            final AbstractClassLoader classLoader = (AbstractClassLoader) entry.getValue();
            try {
                final Class<?> clazz = classLoader.loadClassFromClasspath(name);
                if (clazz != null) {
                    return clazz;
                }
            } catch (ClassNotFoundException e) {
                //ignore
            } catch (Throwable t) {
                throw new CabinLoaderException(
                        String.format("Failed to load agent class %s from Agent classloader of %s", name, agentUrl), t);
            }

        }
        return null;
    }

    protected Enumeration<URL> getJdkResources(final String name) throws IOException {
        return classLoaderService.getExtClassLoader().getResources(name);
    }

    protected Enumeration<URL> getLocalResources(final String name) throws IOException {
        return super.getResources(name);
    }

    @SuppressWarnings("unchecked")
    protected Enumeration<URL> getExportResources(final String name) throws IOException {
        final List<Enumeration<URL>> enumerations = new ArrayList<>();
        final List<ClassLoader> classLoaders = sharedResourceService.getResourceClassLoaders(name);
        if (classLoaders != null) {
            for (ClassLoader classLoader : classLoaders) {
                enumerations.add(((AbstractClassLoader) classLoader).getLocalResources(name));
            }
        }
        return new CompoundEnumeration<>(enumerations.toArray(new Enumeration[0]));
    }

    protected Enumeration<URL> getBizResources(final String name) throws IOException {
        return ((AbstractClassLoader) classLoaderService.getBizModuleClassLoader()).getLocalResources(name);
    }

    @SuppressWarnings("unchecked")
    protected Enumeration<URL> getJavaAgentResources(final String name) throws IOException {
        final List<Enumeration<URL>> enumerations = new ArrayList<>();
        for (ClassLoader classLoader : classLoaderService.getJavaAgentModuleClassLoaders().values()) {
            enumerations.add(((AbstractClassLoader) classLoader).getLocalResources(name));
        }
        return new CompoundEnumeration<>(enumerations.toArray(new Enumeration[0]));
    }

    protected URL getJdkResource(final String name) {
        return classLoaderService.getExtClassLoader().getResource(name);
    }

    protected URL getLocalResource(final String name) {
        return super.getResource(name);
    }

    protected URL getExportResource(final String name) {
        final List<ClassLoader> classLoaders = sharedResourceService.getResourceClassLoaders(name);
        if (classLoaders != null) {
            for (ClassLoader classLoader : classLoaders) {
                final URL resource = ((AbstractClassLoader) classLoader).getLocalResource(name);
                if (resource != null) {
                    return resource;
                }
            }
        }
        return null;
    }

    protected URL getBizResource(final String name) {
        return ((AbstractClassLoader) classLoaderService.getBizModuleClassLoader()).getLocalResource(name);
    }

    /**
     * One useful example is that when javassist is used with jdk11, the ClassLoader::getResource is used to determine
     * whether a class exists while compiling new classes.
     */
    protected URL getJavaAgentResource(final String name) {
        for (ClassLoader classLoader : classLoaderService.getJavaAgentModuleClassLoaders().values()) {
            final URL resource = ((AbstractClassLoader) classLoader).getLocalResource(name);
            if (resource != null) {
                return resource;
            }
        }
        return null;
    }

    protected boolean shouldImportClassFromBiz(final String name) {
        if (Constants.IMPORT_PKG_ENABLE) {
            for (String packageName : Constants.DEFAULT_IMPORT_PKG) {
                if (CabinStringUtil.isNotBlank(packageName) && name.startsWith(packageName)) {
                    return true;
                }
            }
        }

        for (String packageName : Constants.CUSTOM_IMPORT_PKG) {
            if (CabinStringUtil.isNotBlank(packageName) && name.startsWith(packageName)) {
                return true;
            }
        }

        return false;
    }

    protected void debugClassLoadMessage(final Class<?> clazz, final String method, final String className) {
        if (LOGGER.isDebugEnabled()) {
            String position = "unknown";
            if (clazz.getProtectionDomain() != null && clazz.getProtectionDomain().getCodeSource() != null
                    && clazz.getProtectionDomain().getCodeSource().getLocation() != null) {
                position = clazz.getProtectionDomain().getCodeSource().getLocation().toString();
            }
            LOGGER.debug(String.format("Loaded class for Module {%s}: class {%s} in {%s} at {%s} method",
                    getModuleName(), className, position, method));
        }
    }
}
