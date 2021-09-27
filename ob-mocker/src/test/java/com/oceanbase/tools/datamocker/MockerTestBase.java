package com.oceanbase.tools.datamocker;

import org.junit.Rule;
import org.junit.rules.ExpectedException;

/**
 * Test base class, used to introduce some configuration items
 *
 * @author yh263208
 * @date 2020-12-08 15:32
 * @since OBMOCKER_snapshot_0.1.0
 */
public class MockerTestBase {
    @Rule
    public ExpectedException thrown = ExpectedException.none();
}
