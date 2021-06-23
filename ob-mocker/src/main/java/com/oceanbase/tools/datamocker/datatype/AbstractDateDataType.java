package com.oceanbase.tools.datamocker.datatype;

import java.util.Date;
import java.util.TimeZone;

import com.oceanbase.tools.datamocker.generator.BaseDateGenerator;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * Abstract date type, used to represent the date type in the database
 *
 * @author yh263208
 * @date 2020-12-16 14:39
 * @since OBMOCKER_0.1.0_snapshot
 */
public abstract class AbstractDateDataType<T extends Comparable<? super T>> extends AbstractDataType<T, T> {
    /**
     * Time zone, the default is the current time zone
     */
    private TimeZone timeZone = TimeZone.getDefault();

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
    public AbstractDateDataType(BaseDateGenerator<T> generator, ObModeType dialectType, T defaultValue,
            Boolean allowNull) {
        super(generator, dialectType, defaultValue, allowNull);
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
    protected AbstractDateDataType(ObModeType dialectType, T defaultValue, Boolean allowNull) {
        super(dialectType, defaultValue, allowNull);
    }

    /**
     * The constructor of the abstract base class, where you need to pass in the random data generator
     * bound to this data type, and specify the OB mode corresponding to the data type and the database
     * type in this mode
     *
     * @param dialectType dialect type
     * @param timeZone time zone
     * @param defaultValue default value for type
     * @param allowNull Whether it is allowed to be empty
     */
    protected AbstractDateDataType(ObModeType dialectType, TimeZone timeZone, T defaultValue, Boolean allowNull) {
        super(dialectType, defaultValue, allowNull);
        this.timeZone = timeZone;
    }

    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    protected TimeZone timeZone() {
        return this.timeZone;
    }

    /**
     * The maximum number of unique digits that can be generated under the constraints of the type
     * itself (such as precision, number of significant digits)
     *
     * @param minDate left limit date
     * @param maxDate eight limit date
     * @return Returns the maximum number of data that can be generated
     */
    abstract protected Long limitForType(T minDate, T maxDate);

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
    protected T preProcessingBeforeOutput(T value) {
        if (value == null) {
            return null;
        }
        if (value.compareTo(lowValue()) < 0) {
            throw new MockerException(MockerError.ILLEGAL_RETURN_VALUE, "Date can not be smaller than low value");
        } else if (value.compareTo(highValue()) > 0) {
            throw new MockerException(MockerError.ILLEGAL_RETURN_VALUE, "Date can not be bigger than high value");
        }
        return value;
    }

    @Override
    public T toDigest(T value) {
        return value;
    }
}
