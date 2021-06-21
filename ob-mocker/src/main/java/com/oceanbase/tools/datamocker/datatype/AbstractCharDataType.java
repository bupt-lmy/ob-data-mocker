package com.oceanbase.tools.datamocker.datatype;

import java.io.UnsupportedEncodingException;

import com.oceanbase.tools.datamocker.generator.BaseGenerator;
import com.oceanbase.tools.datamocker.generator.CharGeneratorBase;
import com.oceanbase.tools.datamocker.model.enums.CharsetType;
import com.oceanbase.tools.datamocker.model.enums.ObModeType;
import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;

/**
 * Abstract character type, used to describe the data type of the string type in the database
 *
 * @author yh263208
 * @date 2020-12-11 20:16
 * @since OBMOCKER_snapshot_0.1.0
 */
public abstract class AbstractCharDataType extends AbstractDataType<String, Integer> {
    /**
     * Whether to store as a unicode string
     */
    private final Boolean isUnicode;
    /**
     * String encoding format of the database
     */
    private final CharsetType charsetType;
    /**
     * Field length
     */
    private final Integer length;

    /**
     * The constructor of the abstract base class, where you need to pass in the random data generator bound to this data type,
     * and specify the OB mode corresponding to the data type and the database type in this mode
     *
     * @param generator   Character type data generator
     * @param charsetType Character encoding format
     * @param dialectType ob mode
     * @param length      Type length
     * @param allowNull   Whether to allow null values
     */
    public AbstractCharDataType(CharGeneratorBase generator, ObModeType dialectType, CharsetType charsetType, Integer length,
            String defaultValue, Boolean allowNull, Boolean isUnicode) {
        super(generator, dialectType, defaultValue, allowNull);
        validateParam(charsetType, length);
        generator.setCharset(charsetType);
        generator.setUnicode(isUnicode);
        this.charsetType = charsetType;
        this.length = length;
        this.isUnicode = isUnicode;
    }

    /**
     * The constructor of the abstract base class, where you need to pass in the random data generator bound to this data type,
     * and specify the OB mode corresponding to the data type and the database type in this mode
     *
     * @param charsetType Character encoding format
     * @param dialectType ob mode
     * @param length Type length
     * @param allowNull Whether to allow null values
     */
    protected AbstractCharDataType(ObModeType dialectType, CharsetType charsetType, Integer length, String defaultValue, Boolean allowNull,
            Boolean isUnicode) {
        super(dialectType, defaultValue, allowNull);
        validateParam(charsetType, length);
        this.charsetType = charsetType;
        this.length = length;
        this.isUnicode = isUnicode;
    }

    /**
     * Verify that the parameters in the construction method are legal
     *
     * @param charsetType Character set type
     * @param length Character length
     * @throws MockerException An exception is thrown when verification fails
     */
    public void validateParam(CharsetType charsetType, Integer length) {
        if (charsetType == null) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Char set for data type can not be null");
        }
        if (length == null || length <= 0) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "Datatype length can not be null or smaller than zero");
        }
    }

    /**
     * The minimum byte length of the character type, default is 1
     *
     * @return Return length
     */
    @Override
    protected Integer minValueForType() {
        return 1;
    }

    /**
     * The default maximum length of the character type is the length passed in
     * by the constructor and cannot be less than zero
     *
     * @return Return length
     */
    @Override
    protected Integer maxValueForType() {
        return this.length;
    }

    /**
     * The data generator object binding method, the reason why it is a public
     * type method is because the data generator can bind a new
     *
     * @param generator Data generator
     */
    @Override
    public void bind(BaseGenerator<Integer, String> generator) {
        super.bind(generator);
        ((CharGeneratorBase) generator).setCharset(charset());
        ((CharGeneratorBase) generator).setUnicode(isUnicode());
    }

    public CharsetType charset() {
        return this.charsetType;
    }

    public Boolean isUnicode() {
        return this.isUnicode;
    }

    /**
     * The maximum amount of unique data that this type can generate under
     * the constraints of the specified data generator
     *
     * @return Return specific value
     */
    @Override
    public Long distinctLimit() {
        if (generator == null || generator.count(lowValue(), highValue()) == null) {
            return Long.MAX_VALUE;
        }
        return generator.count(lowValue(), highValue());
    }

    /**
     * Pre-check method, used to verify whether the data
     * generated by the data generator is legal
     *
     * @param value Data to be inserted
     * @return Return the verified data
     * @throws MockerException An exception is thrown if verification fails
     */
    @Override
    protected String preTreat(String value) {
        if (value == null) {
            return null;
        }
        Integer maxLength = highValue();
        int realLength;
        try {
            if (isUnicode()) {
                realLength = value.length();
            } else {
                realLength = value.getBytes(charset().getCharSet()).length;
            }
        } catch (UnsupportedEncodingException e) {
            throw new MockerException(MockerError.UNKNOWN_ERROR, e.getMessage());
        }
        if (realLength > maxLength) {
            throw new MockerException(MockerError.VALUE_OUT_OFRANGE,
                    String.format("The length of then generator's value is bigger than bound [0,%d]", maxLength));
        }
        return value;
    }

    /**
     * Return string
     *
     * @param value String passed in as parameter
     * @return String returned
     */
    @Override
    public String toString(String value) {
        if (value == null) {
            return "NULL";
        }
        return String.format("'%s'", value);
    }

    /**
     * Returns the data summary, the number type data summary is itself
     *
     * @param value original value
     * @return Back to summary
     */
    @Override
    public String toDigest(String value) {
        return value;
    }
}
