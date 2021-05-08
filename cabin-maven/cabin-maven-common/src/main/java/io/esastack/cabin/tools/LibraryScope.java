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

public interface LibraryScope {

    LibraryScope COMPILE = new LibraryScope() {

        @Override
        public String toString() {
            return "compile";
        }

    };

    LibraryScope RUNTIME = new LibraryScope() {

        @Override
        public String toString() {
            return "runtime";
        }

    };

    LibraryScope PROVIDED = new LibraryScope() {

        @Override
        public String toString() {
            return "provided";
        }

    };

    LibraryScope MODULE = new LibraryScope() {

        @Override
        public String toString() {
            return "module";
        }

    };

    LibraryScope CONTAINER = new LibraryScope() {

        @Override
        public String toString() {
            return "container";
        }

    };

}
