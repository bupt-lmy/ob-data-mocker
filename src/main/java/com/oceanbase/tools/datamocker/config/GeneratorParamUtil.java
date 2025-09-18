/*
 * Copyright (c) 2025 OceanBase.
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

package com.oceanbase.tools.datamocker.config;

import java.util.Map;

public class GeneratorParamUtil {

    public static String getString(Map<String, Object> m, String key, String defVal) {
        if (m == null) return defVal;
        Object v = m.get(key);
        if (v == null) return defVal;
        return String.valueOf(v);
    }

    public static Integer getInt(Map<String, Object> m, String key, Integer defVal) {
        if (m == null) return defVal;
        Object v = m.get(key);
        if (v == null) return defVal;
        if (v instanceof Number) return ((Number) v).intValue();
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception ignore) {
            return defVal;
        }
    }

    public static Long getLong(Map<String, Object> m, String key, Long defVal) {
        if (m == null) return defVal;
        Object v = m.get(key);
        if (v == null) return defVal;
        if (v instanceof Number) return ((Number) v).longValue();
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (Exception ignore) {
            return defVal;
        }
    }
}
