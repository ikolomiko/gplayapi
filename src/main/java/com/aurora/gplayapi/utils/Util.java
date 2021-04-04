/*
 *     GPlayApi
 *     Copyright (C) 2020  Aurora OSS
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 */

package com.aurora.gplayapi.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class Util {

    public static Map<String, String> parseResponse(byte[] response) {
        final Map<String, String> keyValueMap = new HashMap<>();
        final StringTokenizer stringTokenizer = new StringTokenizer(new String(response), "\n\r");
        while (stringTokenizer.hasMoreTokens()) {
            final String[] keyValue = stringTokenizer.nextToken().split("=", 2);
            if (keyValue.length >= 2) {
                keyValueMap.put(keyValue[0], keyValue[1]);
            }
        }
        return keyValueMap;
    }

    public static long parseLong(String intAsString, long defaultValue) {
        try {
            return Long.parseLong(intAsString);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
