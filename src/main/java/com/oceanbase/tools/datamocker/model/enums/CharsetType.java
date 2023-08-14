/*
 * Copyright (c) 2023 OceanBase.
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
package com.oceanbase.tools.datamocker.model.enums;

/**
 * Character set enumeration of string type
 *
 * @author yh263208
 * @date 2020-12-06 21:39
 * @since OBMOCKER_snapshot_0.1.0
 */
public enum CharsetType {
    /**
     * utf-8 character encoding format
     */
    UTF_8 {
        @Override
        public String getCharSet() {
            return "UTF-8";
        }
    },
    /**
     * utf-8 character encoding format
     */
    AL32UTF8 {
        @Override
        public String getCharSet() {
            return "UTF-8";
        }
    },
    /**
     * utf-8 character encoding format
     */
    UTF8MB4 {
        @Override
        public String getCharSet() {
            return "UTF-8";
        }
    },
    /**
     * gbk character encoding format
     */
    GBK {
        @Override
        public String getCharSet() {
            return "GBK";
        }
    },
    /**
     * gbk character encoding format
     */
    ZHS16GBK {
        @Override
        public String getCharSet() {
            return "GBK";
        }
    };

    /**
     * Get the string name of the encoding format
     *
     * @return Return name
     */
    abstract public String getCharSet();
}
