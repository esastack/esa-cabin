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

import io.esastack.cabin.common.CompoundEnumeration;
import io.esastack.cabin.common.exception.CabinLoaderException;
import io.esastack.cabin.common.log.CabinLoggerFactory;
import io.esastack.cabin.common.util.CabinStringUtil;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class LibModuleClassLoader extends AbstractClassLoader {

    private static final Logger LOGGER = CabinLoggerFactory.getLogger(LibModuleClassLoader.class);

    private Set<String> providedClasses;

    private Set<String> importClasses;

    private List<String> importPackages;

    private List<String> importResources;

    private boolean loadFromBizClassLoader;

    public LibModuleClassLoader(final String moduleName, final URL[] urls) {
        super(moduleName, urls);
    }

    public void setProvidedClasses(final List<String> providedClasses) {
        this.providedClasses = new HashSet<>(providedClasses);
    }

    public void setImportClasses(final List<String> importClasses) {
        this.importClasses = new HashSet<>(importClasses);
    }

    public void setImportPackages(final List<String> importPackages) {
        this.importPackages = importPackages;
    }

    public void setImportResources(final List<String> importResources) {
        this.importResources = importResources;
    }

    public void setLoadFromBizClassLoader(final boolean loadFromBizClassLoader) {
        this.loadFromBizClassLoader = loadFromBizClassLoader;
    }

    @Override
    protected Class<?> loadClass0(final String name, final boolean resolve) throws CabinLoaderException {

        //load classed which has been loaded by this classloader, the relationship is recorded by jvm
        Class<?> clazz = loadFromRecords(name);
        if (clazz != null) {
            debugClassLoadMessage(clazz, "loadFromRecords", name);
            return clazz;
        }

        //load jdk classes
        clazz = loadJdkClass(name);
        if (clazz != null) {
            debugClassLoadMessage(clazz, "loadJdkClass", name);
            return clazz;
        }

        //load cabin classed, such as spi, etc.
        clazz = loadCabinClass(name);
        if (clazz != null) {
            debugClassLoadMessage(clazz, "loadCabinClass", name);
            return clazz;
        }

        //load shared classes
        clazz = loadSharedClass(name);
        if (clazz != null) {
            debugClassLoadMessage(clazz, "loadSharedClass", name);
            return clazz;
        }

        /*
          load provided classes from biz classloader, all the provided dependencies of a lib module should been in the
          biz classpath, or else exception will be thrown, no further load steps would be done.
         */
        clazz = loadProvidedClassFromBiz(name);
        if (clazz != null) {
            debugClassLoadMessage(clazz, "loadProvidedClassFromBiz", name);
            return clazz;
        }

        /*
          load import classes from biz classloader, some classes such as Spring/Jedis/SPI/Log4j/Logback/Slf4j, etc,
          should always be loaded from Biz first
         */
        clazz = loadImportClassFromBiz(name);
        if (clazz != null) {
            debugClassLoadMessage(clazz, "loadImportClassFromBiz", name);
            return clazz;
        }

        /*
         * Load local classes from classpath urls;
         * TODO, maybe we should restrict the import classes from other modules; otherwise if some module exported
         * packages such as io.netty, all modules will be affected.
         */
        clazz = loadLocalClass(name);
        if (clazz != null) {
            debugClassLoadMessage(clazz, "loadLocalClass", name);
            return clazz;
        }

        /*
         * Load all the classes from biz again, for last trying.
         */
        clazz = loadExternalClassFromBiz(name);
        if (clazz != null) {
            debugClassLoadMessage(clazz, "loadExternalBizClass", name);
            return clazz;
        }

        //load agent classes, if BIZ class is enhanced in java agent, it's inner code may use the classes of agent.
        clazz = loadAgentClass(name);
        if (clazz != null) {
            debugClassLoadMessage(clazz, "loadAgentClass", name);
            return clazz;
        }
        throw new CabinLoaderException(
                String.format("Could not load class {%s} from module {%s}", name, getModuleName()));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Enumeration<URL> getResources0(final String name) throws IOException {
        final List<Enumeration<URL>> enumerations = new ArrayList<>();

        enumerations.add(getJdkResources(name));

        enumerations.add(getImportBizResources(name));

        enumerations.add(getLocalResources(name));

        enumerations.add(getExportResources(name));

        enumerations.add(getJavaAgentResources(name));

        return new CompoundEnumeration<>(enumerations.toArray(new Enumeration[0]));
    }

    @Override
    public URL getResource0(final String name) {
        URL url = getJdkResource(name);

        if (url == null) {
            url = getImportBizResource(name);
        }

        if (url == null) {
            url = getLocalResource(name);
        }

        if (url == null) {
            url = getExportResource(name);
        }

        if (url == null) {
            url = getExternalResourceFromBiz(name);
        }

        if (url == null) {
            url = getJavaAgentResource(name);
        }

        return url;
    }

    private Enumeration<URL> getImportBizResources(final String name) throws IOException {
        if (loadFromBizClassLoader && importResources != null && importResources.size() != 0) {
            for (String importResource : importResources) {
                if (name.equals(importResource)) {
                    return ((AbstractClassLoader) classLoaderService.getBizModuleClassLoader()).getLocalResources(name);
                }
            }
        }
        return Collections.emptyEnumeration();
    }

    private URL getImportBizResource(final String name) {
        if (loadFromBizClassLoader && importResources != null && importResources.size() != 0) {
            for (String importResource : importResources) {
                if (name.equals(importResource)) {
                    return ((AbstractClassLoader) classLoaderService.getBizModuleClassLoader()).getLocalResource(name);
                }
            }
        }
        return null;
    }

    private URL getExternalResourceFromBiz(final String name) {
        if (loadFromBizClassLoader) {
            return ((AbstractClassLoader) classLoaderService.getBizModuleClassLoader()).getLocalResource(name);
        }
        return null;
    }

    /**
     * All the provided classes must be loaded by BizModuleClassloader, or else Exception is thrown.
     */
    private Class<?> loadProvidedClassFromBiz(final String name) throws CabinLoaderException {
        if (providedClasses != null && providedClasses.size() != 0) {
            if (providedClasses.contains(name)) {
                try {
                    return ((AbstractClassLoader) classLoaderService.getBizModuleClassLoader())
                            .loadClassFromClasspath(name);
                } catch (Throwable t) {
                    String msg = String.format("Could not load provided class {%s} of module {%s} from biz classloader",
                            name, getModuleName());
                    LOGGER.error(msg, t);
                    throw new CabinLoaderException(msg, t);
                }
            }
        }
        return null;
    }

    @Override
    protected boolean shouldImportClassFromBiz(final String name) {
        if (super.shouldImportClassFromBiz(name)) {
            return true;
        }
        if (importClasses != null && importClasses.contains(name)) {
            return true;
        }

        if (importPackages != null && importPackages.size() != 0) {
            for (String packageName : importPackages) {
                if (CabinStringUtil.isNotBlank(packageName) && name.startsWith(packageName)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Must load from local class path.
     */
    @Override
    protected Class<?> loadImportClassFromBiz(final String name) throws CabinLoaderException {
        if (loadFromBizClassLoader) {
            return super.loadImportClassFromBiz(name);
        }
        return null;
    }

    private Class<?> loadExternalClassFromBiz(final String name) throws CabinLoaderException {
        if (loadFromBizClassLoader) {
            return loadBizClass(name);
        }
        return null;
    }
}
