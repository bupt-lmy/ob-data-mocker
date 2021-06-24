package com.oceanbase.tools.datamocker.datatype;

import com.oceanbase.tools.datamocker.generator.BaseGenerator;
import com.oceanbase.tools.datamocker.model.config.model.DataTypeConfig;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import com.oceanbase.tools.datamocker.model.mock.MockColumnData;
import lombok.Getter;
import org.apache.commons.lang.Validate;

/**
 * Abstract data type class, used to encapsulate some basic data type logic
 *
 * @author yh263208
 * @date 2020-12-10 15:42
 * @since OBMOCKER_snapshot_0.1.0
 */
public abstract class AbstractDataType<T, V extends Comparable<? super V>> {
    /**
     * Pre-check result cache
     */
    private Boolean preCheck = null;
    @Getter
    private final T defaultValue;
    @Getter
    private final Boolean allowNull;
    /**
     * The minimum value that the corresponding type of the data type can reach in the database. The
     * value can be specified manually. If not specified, it is the minimum value that the data type can
     * represent.
     */
    protected V lowValue = null;
    /**
     * The maximum value that the corresponding type of the data type can reach in the database. The
     * value can be specified manually. If not specified, it is the maximum value that the data type can
     * represent.
     */
    protected V highValue = null;
    /**
     * A data generator is bound by default
     */
    protected BaseGenerator<V, T> generator;
    /**
     * OB mode corresponding to this data type
     */
    @Getter
    private final ObModeType dialectType;

    /**
     * The constructor of the abstract base class, where you need to pass in the random data generator
     * bound to this data type, and specify the OB mode corresponding to the data type and the database
     * type in this mode
     *
     * @param obModeType dialect type
     * @param defaultValue default value for type
     * @param allowNull Whether it is allowed to be empty
     */
    protected AbstractDataType(ObModeType obModeType, T defaultValue, Boolean allowNull) {
        Validate.notNull(obModeType, "ObModeType can not be null for AbstractDataType");
        Validate.notNull(allowNull, "AllowNull config can not be null for AbstractDataType");
        this.dialectType = obModeType;
        this.allowNull = allowNull;
        this.defaultValue = defaultValue;
    }

    /**
     * The constructor of the abstract base class, where you need to pass in the random data generator
     * bound to this data type, and specify the OB mode corresponding to the data type and the database
     * type in this mode
     *
     * @param generator data generator
     * @param obModeType dialect type
     * @param defaultValue default value for type
     * @param allowNull Whether it is allowed to be empty
     */
    protected AbstractDataType(BaseGenerator<V, T> generator, ObModeType obModeType, T defaultValue,
            Boolean allowNull) {
        Validate.notNull(obModeType, "ObModeType can not be null for AbstractDataType");
        this.dialectType = obModeType;
        this.allowNull = allowNull;
        this.defaultValue = defaultValue;
        this.bind(generator);
    }

    /**
     * Get the type of factory instance
     *
     * @return Return to factory instance
     */
    abstract public DataTypeFactory<? extends AbstractDataType<T, V>, ? extends DataTypeConfig, ? extends BaseGenerator<V, T>> getFactory();

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
     * The maximum amount of unique data that this type can generate under the constraints of the
     * specified data generator. This value is determined by two indicators. The first indicator is the
     * amount of unique data that the data generator itself can generate. The other indicator is that
     * the data type is in The maximum amount of non-repetitive data that can be generated under
     * precision constraints, whichever is smaller
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
    abstract protected T preProcessingBeforeOutput(T value);

    /**
     * To string method, used to convert a generic type into a string type
     *
     * @param value Generic type
     * @return Returns the converted string type
     */
    abstract public String toString(T value);

    /**
     * Generate data summary, used to convert a large data into a data summary to reduce data storage
     * costs
     *
     * @param value The content of the data
     * @return Back to summary
     */
    abstract public T toDigest(T value);

    /**
     * Type conversion method, used for data compatibility, converts a type of data into the default
     * corresponding type of the data type
     *
     * @param resultSetObject object value from jdbc, eg. resultSet.getObject(1);
     * @return converted value
     */
    public T convertFromJdbcObjectToJavaObject(Object resultSetObject) {
        return (T) resultSetObject;
    }

    /**
     * Sometimes jdbc has different read type <code>getObject</code> and write type
     * <code>setObject</code> for the same database type (eg. year) (eg. <code>getObject</code> of
     * <code>year</code> type is <code>Date</code>, and write type <code>setObject</code> is short).
     * This method is needed for conversion.
     *
     * @param javaObject object for java
     * @return object for jdbc write
     */
    public Object convertFromJavaObjectToJdbcObject(T javaObject) {
        return javaObject;
    }

    /**
     * The data generator object binding method, the reason why it is a public type method is because
     * the data generator can bind a new
     *
     * @param generator data generator
     */
    public void bind(BaseGenerator<V, T> generator) {
        Validate.notNull(generator, "DataGenerator can not be null for AbstractDataType#bind");
        this.generator = generator;
        this.generator.setAllowNull(getAllowNull());
        this.generator.setDefaultValue(getDefaultValue());
        this.preCheck = null;
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
                    String.format("Max or min value %s for data type %s is out of range [%s,%s]", value.toString(),
                            toString(), lowValue.toString(), highValue.toString()));
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
                    String.format("Min value can not be bigger than max value \"%s\" for data type %s",
                            highValue().toString(), toString()));
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
                    String.format("Max value can not be smaller than min value \"%s\" for data type %s",
                            lowValue().toString(), toString()));
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
            throw new MockerException(MockerError.OPERATION_FAILURE, "Generator can not be null");
        }
        if (this.preCheck == null) {
            this.preCheck = generator.preCheck(lowValue(), highValue());
        }
        if (this.preCheck == null || !this.preCheck) {
            throw new MockerException(MockerError.PARAMETER_ERROR,
                    String.format("Data check of column \"%s\" for generator is not passed", this));
        }
        return preProcessingBeforeOutput(generator.next(lowValue(), highValue()));
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
        return (getFactory().toString() + this.dialectType.name()).hashCode();
    }

    /**
     * Convert method to generate a mock column
     *
     * @param columnName column name
     * @param jdbcObject column value
     * @return mock column
     */
    public MockColumnData<T> convertFromJdbcObjectToMockColumn(String columnName, Object jdbcObject) {
        Validate.notEmpty(columnName, "ColumnName can not be null for AbstractDataType#toMockColumn");
        return new MockColumnData<>(columnName, this, this.convertFromJdbcObjectToJavaObject(jdbcObject));
    }

}
