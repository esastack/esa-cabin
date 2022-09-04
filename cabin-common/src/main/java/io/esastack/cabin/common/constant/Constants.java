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
package io.esastack.cabin.common.constant;

import io.esastack.cabin.common.util.CabinStringUtil;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class Constants {

    //-----------------------------------JAR FILE CONSTANTS START------------------------------------
    /**
     * In Jdk 11 javax.*(Spring bean often use it on some beans) is not loaded by ext classloader,
     * it's not jdk class anymore, so we import it;
     * All lib Modules import log dependencies from biz; avoiding the Link error while assign logger object
     * (loaded by biz classloader) to reference loaded by module Classloader.
     * "esa.commons.spi" is added for avoiding SPI 'Feature' error.
     */
    public static final String[] DEFAULT_IMPORT_PKG = {
            "javax",
            "org.aspectj",
            "org.slf4j",
            "ch.qos.logback",
            "org.apache.log4j",
            "org.apache.logging.log4j",
            "org.apache.logging.slf4j",
            "org.apache.commons.logging",
            "esa.commons.spi",
    };

    public static final boolean IMPORT_PKG_ENABLE =
            Boolean.parseBoolean(System.getProperty("import.pkg.enable", "true"));

    public static final String[] CUSTOM_IMPORT_PKG =
            Arrays.stream(System.getProperty("custom.import.pkg", "").split(","))
                    .filter(CabinStringUtil::isNotBlank)
                    .map(String::trim)
                    .collect(Collectors.toList()).toArray(new String[0]);
    //-----------------------------------JAR FILE CONSTANTS END------------------------------------

    //-----------------------------------JAR FILE CONSTANTS START------------------------------------
    public static final String FILE_IN_JAR_SPLITTER = "!/";

    public static final String DIRECTORY_SPLITTER = "/";

    public static final String JAR_FILE_SUFFIX = ".jar";

    public static final String CLASS_FILE_SUFFIX = ".class";

    public static final String NESTED_CONF_DIRECTORY = "conf/";

    public static final String NESTED_LIB_DIRECTORY = "libs/";

    public static final String NESTED_MODULE_DIRECTORY = "modules/";

    public static final String CABIN_CORE_DIRECTORY = "ESA-CABIN/core/";

    public static final String CABIN_MODULE_DIRECTORY = "ESA-CABIN/modules/";

    public static final String APP_CLASSES_DIRECTORY = "APP-INF/classes/";

    public static final String APP_LIB_DIRECTORY = "APP-INF/libs/";

    public static final String CONF_BASE_DIR = "conf/";

    public static final String CABIN_CONF_BASE_DIR = "conf/cabin";

    public static final String MODULE_CONFIG = "conf/module.config";

    public static final String JDK_SPI_DIRECTORY = "META-INF/services/";

    public static final String ESA_SPI_DIRECTORY = "META-INF/esa/";

    public static final String ESA_SPI_DIRECTORY_INTERNAL = "META-INF/esa/internal/";

    public static final String SPRINGBOOT_FACTORIES = "META-INF/spring.factories";

    public static final String PROVIDED_CLASS_FILE = "conf/provided.classes";

    public static final String EXPORTED_CLASS_FILE = "conf/export.classes";

    public static final String EXPORTED_RESOURCE_FILE = "conf/export.resources";

    public static final String URL_JAR_PROTOCOL = "jar";

    public static final String URL_FILE_PROTOCOL = "file";

    //-----------------------------------JAR FILE CONSTANTS END--------------------------------------

    //-----------------------------------CABIN CLASS CONSTANTS START---------------------------------
    public static final String CABIN_LAUNCHER_CLASSNAME = "io.esastack.cabin.support.boot.launcher.CabinJarLauncher";

    public static final String BIZ_CLASSLOADER_NAME = "io.esastack.cabin.container.service.loader.BizClassLoader";

    public static final String CABIN_CONTAINER_CLASSNAME = "io.esastack.cabin.container.CabinContainer";

    public static final String CABIN_UNIT_TEST_MAIN_CLASSNAME = "io.esastack.cabin.unit.test.fake.MainClass";

    public static final String CABIN_UNIT_TEST_MAIN_METHOD = "unitTestMainMethod";

    //-----------------------------------CABIN CLASS CONSTANTS END-----------------------------------

    //------------------------------------MANIFEST CONSTANTS START-----------------------------------
    public static final String MANIFEST_PATH = "META-INF/MANIFEST.MF";

    public static final String MANIFEST_VERSION = "Manifest-Version";

    public static final String MANIFEST_CABIN_VERSION = "CabinVersion";

    public static final String MANIFEST_START_CLASS = "Start-Class";

    public static final String MANIFEST_MAIN_CLASS = "Main-Class";

    public static final String MANIFEST_MODULE_VERSION = "Module-Version";

    public static final String MANIFEST_MODULE_NAME = "Module-Name";

    public static final String MANIFEST_MODULE_DESC = "Module-Description";

    public static final String MANIFEST_MODULE_GROUP_ID = "Module-GroupId";

    public static final String MANIFEST_MODULE_ARTIFACT_ID = "Module-ArtifactId";

    public static final String MANIFEST_MODULE_PRIORITY = "Module-Priority";

    public static final String MANIFEST_EXPORT_CLASSES = "Export-Classes";

    public static final String MANIFEST_EXPORT_PACKAGES = "Export-Packages";

    public static final String MANIFEST_EXPORT_RESOURCES = "Export-Resources";

    public static final String MANIFEST_EXPORT_JARS = "Export-Jars";

    public static final String MANIFEST_IMPORT_CLASSES = "Import-Classes";

    public static final String MANIFEST_IMPORT_PACKAGES = "Import-Packages";

    public static final String MANIFEST_IMPORT_RESOURCES = "Import-Resources";

    public static final String MANIFEST_LOAD_FROM_BIZ = "LoadFromBizClassLoader";

    //------------------------------------MANIFEST CONSTANTS END------------------------------------

    //------------------------------------SYSTEM PROPERTY CONSTANTS START------------------------------------

    public static final String LAZY_LOAD_EXPORTED_CLASSES_ENABLED = "cabin.export.lazy";

    public static final String CABIN_LOG_LEVEL = "cabin.log.level";

    public static final String CABIN_DUPLICATED_MODULE_IGNORE = "cabin.module.duplicated.ignore";

    public static final String CABIN_MODULE_DIR = "cabin.module.dir";

    public static final String CABIN_MODULE_DIR_DEFAULT = "cabin_module_dir";

    public static final String CHARACTER_ANY = "*";
    //------------------------------------SYSTEM PROPERTY CONSTANTS END------------------------------------

    //------------------------------------JAVAAGENT CONSTANTS START-----------------------------------

    public static final String JAVA_AGENT_MARK = "-javaagent:";

    public static final String JAVA_AGENT_OPTION_MARK = "=";

    //------------------------------------JAVAAGENT CONSTANTS END-----------------------------------

}
