package com.oceanbase.tools.datamocker.datatype;

import com.oceanbase.tools.datamocker.generator.BaseGenerator;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * Abstract data type class, used to encapsulate some basic data type logic
 *
 * @author yh263208
 * @date 2020-12-10 15:42
 * @since OBMOCKER_snapshot_0.1.0
 */
public abstract class AbstractDataType<T, V extends Comparable> {
    private final T defaultValue;
    private final Boolean allowNull;
    /**
     * Pre-check result cache
     */
    private Boolean preCheck = null;
    /**
     * The minimum value that the corresponding type of the data type can reach in the database.
     * The value can be specified manually. If not specified, it is the minimum value that the data type can represent.
     */
    protected V lowValue = null;
    /**
     * The maximum value that the corresponding type of the data type can reach in the database.
     * The value can be specified manually. If not specified, it is the maximum value that the data type can represent.
     */
    protected V highValue = null;
    /**
     * A data generator is bound by default
     */
    protected BaseGenerator<V, T> generator;
    /**
     * OB mode corresponding to this data type
     */
    private final ObModeType dialectType;

    /**
     * The constructor of the abstract base class, where you need to pass in the random data generator bound to this data type,
     * and specify the OB mode corresponding to the data type and the database type in this mode
     *
     * @param dialectType  dialect type
     * @param defaultValue default value for type
     * @param allowNull    Whether it is allowed to be empty
     */
    protected AbstractDataType(ObModeType dialectType, T defaultValue, Boolean allowNull) {
        this.dialectType = dialectType;
        this.allowNull = allowNull;
        this.defaultValue = defaultValue;
    }

    /**
     * The constructor of the abstract base class, where you need to pass in the random data generator bound to this data type,
     * and specify the OB mode corresponding to the data type and the database type in this mode
     *
     * @param generator data generator
     * @param dialectType dialect type
     * @param defaultValue default value for type
     * @param allowNull Whether it is allowed to be empty
     */
    protected AbstractDataType(BaseGenerator<V, T> generator, ObModeType dialectType, T defaultValue, Boolean allowNull) {
        this.dialectType = dialectType;
        this.allowNull = allowNull;
        this.defaultValue = defaultValue;
        this.bind(generator);
    }

    /**
     * Get the type of factory instance
     *
     * @return Return to factory instance
     */
    abstract public DataTypeFactory getFactory();

    /**
     * The lower limit of the value that the corresponding type can represent in the database
     *
     * @return Return minimum
     */
    abstract protected V minValueForType();

    /**
     * The upper limit of the value that the corresponding type can represent in the database
     *
     * @return Returns the maximum value
     */
    abstract protected V maxValueForType();

    /**
     * The maximum amount of unique data that this type can generate under the constraints
     * of the specified data generator. This value is determined by two indicators.
     * The first indicator is the amount of unique data that the data generator itself can generate.
     * The other indicator is that the data type is in The maximum amount of non-repetitive
     * data that can be generated under precision constraints, whichever is smaller
     *
     * @return Return specific value
     */
    abstract public Long distinctLimit();

    /**
     * Data generator generated value preprocessing method
     *
     * @param value Pass in the value used for preprocessing
     * @return Return the processed value
     */
    abstract protected T preTreat(T value);

    /**
     * To string method, used to convert a generic type into a string type
     *
     * @param value Generic type
     * @return Returns the converted string type
     */
    abstract public String toString(T value);

    /**
     * Generate data summary, used to convert a large data into a data summary to reduce data storage costs
     *
     * @param value The content of the data
     * @return Back to summary
     */
    abstract public T toDigest(T value);

    /**
     * Type conversion method, used for data compatibility,
     * converts a type of data into the default corresponding type of the data type
     *
     * @param value original value
     * @return converted value
     */
    public T convert(Object value) {
        return (T) value;
    }

    /**
     * The data generator object binding method, the reason why it is a
     * public type method is because the data generator can bind a new
     *
     * @param generator data generator
     */
    public void bind(BaseGenerator<V, T> generator) {
        this.generator = generator;
        this.generator.setAllowNull(allowNull());
        this.generator.setDefaultValue(defaultValue());
        this.preCheck = null;
    }

    public Boolean allowNull() {
        return this.allowNull;
    }

    public T defaultValue() {
        return this.defaultValue;
    }

    public ObModeType getDialectType() {
        return dialectType;
    }

    /**
     * Check whether the boundary value is legal
     *
     * @param value Boundary value to be set
     */
    protected void validateValue(V value) {
        if (value == null) {
            return;
        }
        V lowValue = minValueForType();
        V highValue = maxValueForType();
        if (lowValue == null || highValue == null) {
            throw new MockerException(MockerError.ILLEGAL_RETURN_VALUE,
                    "Lowest or highest value can not be null for data type " + toString());
        }
        if (value.compareTo(lowValue) < 0 || value.compareTo(highValue) > 0) {
            throw new MockerException(MockerError.VALUE_OUT_OFRANGE,
                    String.format("Max or min value %s for data type %s is out of range [%s,%s]",
                            value.toString(), toString(), lowValue.toString(), highValue.toString()));
        }
    }

    /**
     * Set the low value of the data type
     *
     * @param value low value
     */
    public void setLowValue(V value) {
        validateValue(value);
        if (value.compareTo(highValue()) > 0) {
            throw new MockerException(MockerError.VALUE_OUT_OFRANGE,
                    String.format("Min value can not be bigger than max value \"%s\" for data type %s", highValue().toString(),
                            toString()));
        }
        this.lowValue = value;
    }

    /**
     * Set the high value of the data type
     *
     * @param value high value
     */
    public void setHighValue(V value) {
        validateValue(value);
        if (value.compareTo(lowValue()) < 0) {
            throw new MockerException(MockerError.VALUE_OUT_OFRANGE,
                    String.format("Max value can not be smaller than min value \"%s\" for data type %s", lowValue().toString(),
                            toString()));
        }
        this.highValue = value;
    }

    /**
     * Data generation method, call this method to generate a data that conforms to the distribution
     *
     * @return Return the generated data
     */
    public T acquire() {
        if (generator == null) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Generator can not be null");
        }
        if (this.preCheck == null) {
            this.preCheck = generator.preCheck(lowValue(), highValue());
        }
        if (this.preCheck == null || !this.preCheck) {
            throw new MockerException(MockerError.PARAMETER_ERROR,
                    String.format("Data check of column \"%s\" for generator is not passed", this));
        }
        return preTreat(generator.next(lowValue(), highValue()));
    }

    public V lowValue() {
        if (this.lowValue == null) {
            this.lowValue = minValueForType();
        }
        return this.lowValue;
    }

    public V highValue() {
        if (this.highValue == null) {
            this.highValue = maxValueForType();
        }
        return this.highValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractDataType<T, V> that = (AbstractDataType<T, V>) o;
        return this.dialectType == that.dialectType && getFactory() == that.getFactory();
    }

    @Override
    public int hashCode() {
        StringBuffer buffer = new StringBuffer(getFactory().toString());
        buffer.append(this.dialectType.name());
        return buffer.toString().hashCode();
    }

}
