package com.oceanbase.tools.datamocker.model.mock;

import com.oceanbase.tools.datamocker.datatype.AbstractDataType;
import com.oceanbase.tools.datamocker.util.Pair;

/**
 * 一个简单的JavaBean，用于封装模拟数据每一列上生成的数据
 *
 * @author yh263208
 * @date 2021-02-22 11:44
 * @since OBMOCKER_snapshit_0.1.0
 */
public class MockColumn {
    /**
     * 一个数据列，包含两部分信息，一部分是该列的类型，另一部分是该列生成出的数据
     */
    private Pair<AbstractDataType, Object> column;
}
