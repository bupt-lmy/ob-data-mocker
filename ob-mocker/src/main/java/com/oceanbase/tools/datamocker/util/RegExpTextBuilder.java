package com.oceanbase.tools.datamocker.util;

import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.oceanbase.tools.datamocker.model.exception.MockerError;
import com.oceanbase.tools.datamocker.model.exception.MockerException;
import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import dk.brics.automaton.State;
import dk.brics.automaton.Transition;
import org.apache.commons.lang.StringUtils;

/**
 * 正则表达式生成工具类
 *
 * @author yh263208
 * @date 2021-01-16 19:48
 * @since OBMOCKER_snapshot_0.1.0
 */
public class RegExpTextBuilder {
    /**
     * 匹配特殊的正则表达式，这种正则表达式需要被进行改写
     */
    private static final Pattern PATTERN_REQUOTED = Pattern.compile("\\\\Q(.*?)\\\\E");
    /**
     * 需要对PATTERN_REQUOTED中匹配到的正则表达式进行替换
     */
    private static final Pattern PATTERN_SPECIAL = Pattern.compile("[.^$*+?(){|\\[\\\\@]");
    /**
     * 正则表达式代表的自动机实例
     */
    private final Automaton automaton;
    /**
     * 遍历自动机可能遇到多个路径，利用一个随机对象来增加生成数据的多样性
     */
    private Random random;

    /**
     * 构造函数，用于构造一个正则表达式工具类
     *
     * @param regex  正则表达式
     * @param random 随机对象
     */
    public RegExpTextBuilder(String regex, Random random) {
        if (StringUtils.isBlank(regex) || random == null) {
            throw new MockerException(MockerError.PARAMETER_ERROR, "RegExp or random obj can not be null");
        }
        this.automaton = new RegExp(requote(regex)).toAutomaton();
        this.random = random;
    }

    /**
     * 标准构造函数
     *
     * @param regex 正则表达式
     */
    public RegExpTextBuilder(String regex) {
        this(regex, new Random());
    }

    /**
     * 获取一个符合正则表达式的字符串
     *
     * @return 返回字符串
     */
    public String generate() {
        StringBuilder builder = new StringBuilder();
        generate(builder, automaton.getInitialState());
        return builder.toString();
    }

    /**
     * 生成宇哥符合长度规范的正则表达式字符串
     *
     * @param minLength 字符串的最小长度
     * @param maxLength 字符串的最大长度
     * @return 返回字符串
     */
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
                    throw new MockerException(
                            String.format("Reached accept state before min length (current = %d < min = %d)", walkLength, minLength));
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
            throw new MockerException(String.format(
                    "Exceeded max walk length (%d) before reaching an accept state: target length was %d (min length = %d)", maxLength,
                    targetLength, minLength));
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

    /**
     * 需要对正则表达式进行改写，去掉其中的特殊部分
     *
     * @param regex 正则表达式字符串
     * @return 返回正则表达式
     */
    private static String requote(String regex) {
        StringBuilder sb = new StringBuilder(regex);
        Matcher matcher = PATTERN_REQUOTED.matcher(sb);
        while (matcher.find()) {
            sb.replace(matcher.start(), matcher.end(), PATTERN_SPECIAL.matcher(matcher.group(1)).replaceAll("\\\\$0"));
        }
        return sb.toString();
    }
}
