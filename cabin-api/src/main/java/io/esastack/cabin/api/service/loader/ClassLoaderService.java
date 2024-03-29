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
package io.esastack.cabin.api.service.loader;

import java.net.URL;
import java.util.Map;

/**
 * manage all biz classloader and module classloaders
 */
public interface ClassLoaderService {

    ClassLoader getExtClassLoader();

    ClassLoader getCabinClassLoader();

    Map<String, ClassLoader> getJavaAgentModuleClassLoaders();

    ClassLoader createJavaAgentModuleClassLoader(URL agentUrl);

    ClassLoader getBizModuleClassLoader();

    ClassLoader createBizModuleClassLoader(BizModuleClassLoaderParam param);

    ClassLoader getLibModuleClassLoader(String moduleName);

    ClassLoader createLibModuleClassLoader(LibModuleClassLoaderParam param);

    ClassLoader destroyLibModuleClassLoader(String moduleName);

}
