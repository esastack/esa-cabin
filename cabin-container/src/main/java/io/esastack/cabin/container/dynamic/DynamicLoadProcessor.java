package io.esastack.cabin.container.dynamic;

public interface DynamicLoadProcessor {

    void installModule(final String moduleJarUrl);

    void uninstallModule(final String moduleName);
}
