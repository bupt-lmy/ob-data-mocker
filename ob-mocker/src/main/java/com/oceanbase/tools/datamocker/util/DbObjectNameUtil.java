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
package com.oceanbase.tools.datamocker.util;

import org.apache.commons.lang.StringUtils;

/**
 * Util for DB Object name
 *
 * @author yh263208
 * @date 2021-06-16 11:42
 * @since OB_MOCKER_0.1.1
 */
public class DbObjectNameUtil {
    /**
     * To convert the name, you can use single quotation marks or backslashes as naming, but single
     * quotation marks and backslashes in the oracle mode and mysql mode appear as escape symbols in the
     * string. If you want to make the single quotation mark also appear in the string If it appears
     * normally, a single quotation mark must be added before the single quotation mark as an escape.
     * This method is to play its role.
     *
     * @param objectName object name for db object
     * @param escapeChars escape char list
     * @return converted object name
     */
    public static String doubleCharToEscape(String objectName, char... escapeChars) {
        if (objectName == null) {
            return null;
        }
        char[] realEscapeChars = new char[escapeChars.length];
        int commitIndex = 0;
        for (char aChar : escapeChars) {
            if (StringUtils.contains(objectName, aChar)) {
                realEscapeChars[commitIndex++] = aChar;
            }
        }
        if (commitIndex == 0) {
            return objectName;
        }
        char[] returnVal = new char[objectName.length() * 2];
        int j = 0;
        for (int i = 0; i < objectName.length(); i++) {
            char item = objectName.charAt(i);
            for (int k = 0; k < commitIndex; k++) {
                char escapeChar = realEscapeChars[k];
                if (item == escapeChar) {
                    returnVal[j++] = escapeChar;
                }
            }
            returnVal[j++] = item;
        }
        return new String(returnVal, 0, j);
    }

}
