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

import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import dk.brics.automaton.State;
import dk.brics.automaton.Transition;
import org.apache.commons.lang.StringUtils;

/**
 * Regular expression generation tool class
 *
 * @author yh263208
 * @date 2021-01-16 19:48
 * @since OBMOCKER_snapshot_0.1.0
 */
public class RegExpTextBuilder {

    private static final Pattern PATTERN_REQUOTED = Pattern.compile("\\\\Q(.*?)\\\\E");
    private static final Pattern PATTERN_SPECIAL = Pattern.compile("[.^$*+?(){|\\[\\\\@]");
    private final Random random;
    private final Automaton automaton;

    public RegExpTextBuilder(String regex, Random random) {
        if (StringUtils.isBlank(regex) || random == null) {
            throw new IllegalArgumentException("RegExp or random obj can not be null");
        }
        this.random = random;
        this.automaton = new RegExp(requote(regex)).toAutomaton();
    }

    public RegExpTextBuilder(String regex) {
        this(regex, new Random());
    }

    public String generate() {
        StringBuilder builder = new StringBuilder();
        generate(builder, automaton.getInitialState());
        return builder.toString();
    }

    public String generate(int minLength, int maxLength) {
        final StringBuilder builder = new StringBuilder();
        int walkLength = 0;
        State state = automaton.getInitialState();

        final int targetLength = getRandomInt(minLength, maxLength, random);
        while (walkLength < targetLength) {
            List<Transition> transitions = state.getSortedTransitions(false);
            if (transitions.size() == 0) {
                if (walkLength >= minLength) {
                    assert state.isAccept();
                    return builder.toString();
                } else {
                    throw new IllegalStateException(String.format(
                            "Reached accept state before min length (current = %d < min = %d)",
                            walkLength, minLength));
                }
            }
            List<Transition> nonFinalTransitions = transitions.stream()
                    .filter(t -> !t.getDest().getTransitions().isEmpty()).collect(Collectors.toList());
            if (!nonFinalTransitions.isEmpty()) {
                transitions = nonFinalTransitions;
            }
            final int option = getRandomInt(0, transitions.size() - 1, random);
            final Transition transition = transitions.get(option);
            appendChoice(builder, transition);
            state = transition.getDest();
            ++walkLength;
        }
        while (!state.isAccept() && walkLength < maxLength) {
            List<Transition> transitions = state.getSortedTransitions(false);
            if (transitions.size() == 0) {
                assert state.isAccept();
                return builder.toString();
            }
            final int option = getRandomInt(0, transitions.size() - 1, random);
            final Transition transition = transitions.get(option);
            appendChoice(builder, transition);
            state = transition.getDest();
            ++walkLength;
        }
        if (state.isAccept()) {
            return builder.toString();
        } else {
            throw new IllegalStateException(String.format(
                    "Exceeded max walk length (%d) before reaching an accept state: target length was %d (min length = %d)",
                    maxLength, targetLength, minLength));
        }
    }

    private void generate(StringBuilder builder, State state) {
        List<Transition> transitions = state.getSortedTransitions(false);
        if (transitions.size() == 0) {
            assert state.isAccept();
            return;
        }
        int nroptions = state.isAccept() ? transitions.size() : transitions.size() - 1;
        int option = getRandomInt(0, nroptions, random);
        if (state.isAccept() && option == 0) {
            return;
        }
        Transition transition = transitions.get(option - (state.isAccept() ? 1 : 0));
        appendChoice(builder, transition);
        generate(builder, transition.getDest());
    }

    private void appendChoice(StringBuilder builder, Transition transition) {
        char c = (char) getRandomInt(transition.getMin(), transition.getMax(), random);
        builder.append(c);
    }

    private int getRandomInt(int min, int max, Random random) {
        int maxForRandom = max - min + 1;
        return random.nextInt(maxForRandom) + min;
    }

    private static String requote(String regex) {
        StringBuilder sb = new StringBuilder(regex);
        Matcher matcher = PATTERN_REQUOTED.matcher(sb);
        while (matcher.find()) {
            sb.replace(matcher.start(), matcher.end(), PATTERN_SPECIAL.matcher(matcher.group(1)).replaceAll("\\\\$0"));
        }
        return sb.toString();
    }

}
