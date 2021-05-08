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
package io.esastack.cabin.tools;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarInputStream;

public interface LoaderClassesWriter {

    /**
     * Write classes needed by the cabin launcher to the JAR.
     *
     * @param jarInputStream the inputStream of the resource containing the loader classes
     *                       to be written
     * @throws IOException if the classes cannot be written
     */
    void writeLoaderClasses(JarInputStream jarInputStream) throws IOException;

    /**
     * Write a single entry to the JAR.
     *
     * @param name        the name of the entry
     * @param inputStream the input stream content
     * @throws IOException if the entry cannot be written
     */
    void writeEntry(String name, InputStream inputStream) throws IOException;

}
