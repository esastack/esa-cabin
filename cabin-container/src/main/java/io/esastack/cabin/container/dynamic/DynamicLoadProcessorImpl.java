package io.esastack.cabin.container.dynamic;

import io.esastack.cabin.api.domain.Module;
import io.esastack.cabin.api.service.deploy.LibModuleLoadService;
import io.esastack.cabin.api.service.share.LibModuleExportService;
import io.esastack.cabin.common.exception.CabinRuntimeException;
import io.esastack.cabin.common.log.CabinLoggerFactory;
import io.esastack.cabin.container.processor.LibModuleExportProcessor;
import io.esastack.cabin.container.processor.LibModuleMergeProcessor;
import io.esastack.cabin.loader.archive.Archive;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DynamicLoadProcessorImpl implements DynamicLoadProcessor {

    private static final Logger LOGGER = CabinLoggerFactory.getLogger(LibModuleExportProcessor.class);

    private LibModuleMergeProcessor libModuleMergeProcessor;
    private LibModuleLoadService libModuleLoadService;
    private LibModuleExportService libModuleExportService;

    public void setLibModuleMergeProcessor(LibModuleMergeProcessor libModuleMergeProcessor) {
        this.libModuleMergeProcessor = libModuleMergeProcessor;
    }

    public void setLibModuleLoadService(LibModuleLoadService libModuleLoadService) {
        this.libModuleLoadService = libModuleLoadService;
    }

    public void setLibModuleExportService(LibModuleExportService libModuleExportService) {
        this.libModuleExportService = libModuleExportService;
    }

    public void installModule(final String moduleJarUrl) {
        final URL[] urls;
        try {
            urls = new URL[]{new URL("file", null, moduleJarUrl)};
        } catch (MalformedURLException e) {
            throw new CabinRuntimeException(String.format("Failed to convert arg %s to URL", moduleJarUrl), e);
        }

        final Map<String, Archive> moduleArchives;
        try {
            moduleArchives = libModuleMergeProcessor.parseLibModulesFromURLs(urls, false, true);
        } catch (IOException e) {
            throw new CabinRuntimeException("Failed to load lib module from:" + moduleJarUrl, e);
        }
        final List<Module> modules = new ArrayList<>();
        for (Map.Entry<String, Archive> entry : moduleArchives.entrySet()) {
            modules.add(libModuleLoadService.loadModule(entry.getKey(), entry.getValue()));
        }

        for (Module module : modules) {
            int countR = libModuleExportService.exportResources(module.getName());
            int countC = libModuleExportService.exportClasses(module.getName());
            LOGGER.info("Dynamic load module {}, exported {} classes and {} resources", moduleJarUrl, countC, countR);
        }
    }

    /**
     * Firstly destroy classes and then destroy classloader and module.
     * @param moduleName module name
     */
    @Override
    public void uninstallModule(String moduleName) {
        libModuleExportService.destroyModule(moduleName);
        final Module module = libModuleLoadService.destroyModule(moduleName);
        if (module == null) {
            LOGGER.info("Module {} does not exist in current cabin container!", moduleName);
        }

    }
}
