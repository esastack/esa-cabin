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
package io.esastack.cabin.log;

import org.apache.maven.plugin.logging.Log;

public class Logger implements Log {

    private static final String CABIN_LOG_PREFIX = "[Cabin Package] ";

    private final Log log;

    public Logger(Log log) {
        this.log = log;
    }

    @Override
    public boolean isDebugEnabled() {
        return log.isDebugEnabled();
    }

    @Override
    public void debug(final CharSequence charSequence) {
        final String msg = CABIN_LOG_PREFIX + charSequence;
        log.debug(msg);
    }

    @Override
    public void debug(final CharSequence charSequence, final Throwable throwable) {
        final String msg = CABIN_LOG_PREFIX + charSequence;
        log.debug(msg, throwable);
    }

    @Override
    public void debug(final Throwable throwable) {
        log.debug(throwable);
    }

    @Override
    public boolean isInfoEnabled() {
        return log.isInfoEnabled();
    }

    @Override
    public void info(final CharSequence charSequence) {
        final String msg = CABIN_LOG_PREFIX + charSequence;
        log.info(msg);
    }

    @Override
    public void info(final CharSequence charSequence, final Throwable throwable) {
        final String msg = CABIN_LOG_PREFIX + charSequence;
        log.info(msg, throwable);
    }

    @Override
    public void info(final Throwable throwable) {
        log.info(throwable);
    }

    @Override
    public boolean isWarnEnabled() {
        return log.isWarnEnabled();
    }

    @Override
    public void warn(final CharSequence charSequence) {
        final String msg = CABIN_LOG_PREFIX + charSequence;
        log.warn(msg);
    }

    @Override
    public void warn(final CharSequence charSequence, final Throwable throwable) {
        final String msg = CABIN_LOG_PREFIX + charSequence;
        log.warn(msg, throwable);
    }

    @Override
    public void warn(final Throwable throwable) {
        log.warn(throwable);
    }

    @Override
    public boolean isErrorEnabled() {
        return log.isErrorEnabled();
    }

    @Override
    public void error(final CharSequence charSequence) {
        final String msg = CABIN_LOG_PREFIX + charSequence;
        log.error(msg);
    }

    @Override
    public void error(final CharSequence charSequence, final Throwable throwable) {
        final String msg = CABIN_LOG_PREFIX + charSequence;
        log.error(msg, throwable);
    }

    @Override
    public void error(final Throwable throwable) {
        log.error(throwable);
    }
}
