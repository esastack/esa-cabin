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
package io.esastack.cabin.support.bootstrap.thread;

import io.esastack.cabin.common.exception.CabinRuntimeException;

public class IsolatedThreadGroup extends ThreadGroup {

    private final Object monitor = new Object();

    private Throwable exception;

    public IsolatedThreadGroup(final String name) {
        super(name);
    }

    @Override
    public void uncaughtException(final Thread thread, final Throwable ex) {
        if (!(ex instanceof ThreadDeath)) {
            synchronized (this.monitor) {
                this.exception = (this.exception == null ? ex : this.exception);
            }
            ex.printStackTrace();
        }
    }

    public synchronized void rethrowUncaughtException() {
        synchronized (this.monitor) {
            if (this.exception != null) {
                throw new CabinRuntimeException("An exception occurred while running. " + this.exception.getMessage(),
                        this.exception);
            }
        }
    }
}
