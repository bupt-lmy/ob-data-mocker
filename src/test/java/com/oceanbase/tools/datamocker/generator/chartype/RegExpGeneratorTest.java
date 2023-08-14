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
package com.oceanbase.tools.datamocker.generator.chartype;

import com.oceanbase.tools.datamocker.MockerTestBase;
import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import dk.brics.automaton.State;
import org.junit.Test;

/**
 * Regular expression data generator test class
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
