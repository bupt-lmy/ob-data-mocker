package com.oceanbase.tools.datamocker.generator.chartype;

import com.oceanbase.tools.datamocker.MockerTestBase;
import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import dk.brics.automaton.State;
import org.junit.Test;

/**
 * 正则表达式数据生成器测试类
 *
 * @author yh263208
 * @date 2021-01-22 15:57
 * @since OBMOCKER_snasphot_0.1.0
 */
public class RegExpGeneratorTest extends MockerTestBase {
    @Test
    public void test() {
        String pattern = "[a-z0-9]{0,3}";
        RegExp regExp = new RegExp(pattern);
        Automaton automaton = regExp.toAutomaton();
        State state = automaton.getInitialState();
        System.out.println(state.isAccept());
        System.out.println(automaton.isFinite());
    }
}
