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
package io.esastack.cabin.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileUtils {

    public static String digest(final InputStream fileInputStream, final String algorithm) throws IOException {
        try {
            final DigestInputStream inputStream =
                    new DigestInputStream(fileInputStream, MessageDigest.getInstance(algorithm));
            try {
                final byte[] buffer = new byte[4098];
                while (inputStream.read(buffer) != -1) { //NOPMD
                    // Read the entire stream
                }
                return bytesToHex(inputStream.getMessageDigest().digest());
            } finally {
                inputStream.close();
            }
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String bytesToHex(final byte[] bytes) {
        final StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    public static String getCompatiblePath(final String path) {
        if (System.getProperty("os.name").toLowerCase().contains("window")) {
            return path.replace("\\", "/");
        }
        return path;
    }
}
