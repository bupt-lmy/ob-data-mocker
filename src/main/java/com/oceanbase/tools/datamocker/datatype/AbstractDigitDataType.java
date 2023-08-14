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
package com.oceanbase.tools.datamocker.datatype;

import com.oceanbase.tools.datamocker.generator.BaseDigitalGenerator;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;

/**
 * Abstract base class for digital types
 *
 * @author yh263208
 * @date 2020-12-10 15:54
 * @since OBMOCKER_snashot_0.1.0
 */
public abstract class AbstractDigitDataType<T extends Comparable<T>> extends AbstractDataType<T, T> {
    /**
     * Is it a signed number
     */
    private Boolean isSigned = Boolean.TRUE;

    /**
     * The constructor of the abstract base class, where you need to pass in the random data generator
     * bound to this data type, and specify the OB mode corresponding to the data type and the database
     * type in this mode
     *
     * @param generator data generator
     * @param dialectType dialect type
     * @param defaultValue default value for type
     * @param allowNull Whether it is allowed to be empty
     */
    public AbstractDigitDataType(BaseDigitalGenerator<T> generator, ObModeType dialectType, T defaultValue,
            Boolean allowNull) {
        super(generator, dialectType, defaultValue, allowNull);
    }

    /**
     * The constructor of the abstract base class, where you need to pass in the random data generator
     * bound to this data type, and specify the OB mode corresponding to the data type and the database
     * type in this mode
     *
     * @param generator data generator
     * @param dialectType dialect type
     * @param defaultValue default value for type
     * @param allowNull Whether it is allowed to be empty
     * @param isSigned Is it a signed number
     */
    public AbstractDigitDataType(BaseDigitalGenerator<T> generator, ObModeType dialectType, T defaultValue,
            Boolean allowNull, Boolean isSigned) {
        super(generator, dialectType, defaultValue, allowNull);
        this.isSigned = isSigned;
    }

    /**
     * The constructor of the abstract base class, where you need to pass in the random data generator
     * bound to this data type, and specify the OB mode corresponding to the data type and the database
     * type in this mode
     *
     * @param dialectType dialect type
     * @param defaultValue default value for type
     * @param allowNull Whether it is allowed to be empty
     */
    protected AbstractDigitDataType(ObModeType dialectType, T defaultValue, Boolean allowNull) {
        super(dialectType, defaultValue, allowNull);
    }

    /**
     * The maximum number of unique digits that can be generated under the constraints of the type
     * itself (such as precision, number of significant digits)
     *
     * @param lowValue left limit value
     * @param highValue right limit value
     * @return Returns the maximum number of data that can be generated
     */
    abstract protected Long limitForType(T lowValue, T highValue);

    public Boolean signed() {
        return this.isSigned;
    }

    /**
     * The maximum amount of unique data that this type can generate under the constraints of the
     * specified data generator. This value is determined by two indicators. The first indicator is the
     * amount of unique data that the data generator itself can generate. The other indicator is that
     * the data type is in The maximum amount of non-repetitive data that can be generated under
     * precision constraints, whichever is smaller
     *
     * @return Return specific value
     */
    @Override
    public Long distinctLimit() {
        if (generator == null || generator.count(lowValue(), highValue()) == null) {
            return limitForType(lowValue(), highValue());
        }
        return limitForType(lowValue(), highValue()) < generator.count(lowValue(), highValue())
                ? limitForType(lowValue(), highValue())
                : generator.count(lowValue(), highValue());
    }

    @Override
    public T toDigest(T value) {
        return value;
    }

}
