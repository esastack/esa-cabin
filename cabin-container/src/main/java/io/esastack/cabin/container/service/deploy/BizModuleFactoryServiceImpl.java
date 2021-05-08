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
package io.esastack.cabin.container.service.deploy;

import io.esastack.cabin.api.service.deploy.BizModuleFactoryService;
import io.esastack.cabin.api.service.loader.BizModuleClassLoaderParam;
import io.esastack.cabin.api.service.loader.ClassLoaderService;
import io.esastack.cabin.common.exception.CabinRuntimeException;
import io.esastack.cabin.common.util.CabinStringUtil;
import io.esastack.cabin.container.domain.BizModule;

import java.net.URL;

import static io.esastack.cabin.common.constant.Constants.CABIN_UNIT_TEST_MAIN_CLASSNAME;
import static io.esastack.cabin.common.constant.Constants.CABIN_UNIT_TEST_MAIN_METHOD;

public class BizModuleFactoryServiceImpl implements BizModuleFactoryService<BizModule> {

    private volatile ClassLoaderService classLoaderService;

    @SuppressWarnings("unused")
    public void setClassLoaderService(final ClassLoaderService service) {
        classLoaderService = service;
    }

    @Override
    public BizModule createModule(final URL[] bizUrls, final String[] args) throws CabinRuntimeException {
        final boolean isUnitTest = CabinStringUtil.isNotBlank(args[0]) &&
                CabinStringUtil.isNotBlank(args[1]) &&
                args[0].equals(CABIN_UNIT_TEST_MAIN_CLASSNAME) &&
                args[1].equals(CABIN_UNIT_TEST_MAIN_METHOD);
        final BizModuleClassLoaderParam param = BizModuleClassLoaderParam.newBuilder()
                .urls(bizUrls)
                .isUnitTest(isUnitTest)
                .build();
        final ClassLoader classLoader = classLoaderService.createBizModuleClassLoader(param);
        return BizModule.newBuilder()
                .name("Biz Identity")
                .urls(bizUrls)
                .mainClass(args[0])
                .mainMethod(args[1])
                .arguments(getRealArguments(args))
                .classLoader(classLoader)
                .unitTest(isUnitTest)
                .build();
    }

    private String[] getRealArguments(final String[] args) {
        assert args.length >= 2;
        final String[] arguments = new String[args.length - 2];
        if (args.length > 2) {
            System.arraycopy(args, 2, arguments, 0, args.length - 2);
        }
        return arguments;
    }
}
