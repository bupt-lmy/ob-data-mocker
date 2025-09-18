// SqlStreamingPagedGenerator.java
package com.oceanbase.tools.datamocker.agent;

import com.oceanbase.tools.datamocker.config.TaskConfig;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;

public class SqlStreamingPagedGenerator {

    private char lastWrittenChar = '\n';          // 全局/成员：上一次写入的最后字符
    boolean firstDeltaInThisCall = true; // 本次分页的第一块流式内容


    /** 生成到指定文件；实时打印落盘 */
    public static File generateToFile(TaskConfig cfg,
                                      DeepSeekService ds,
                                      String model,
                                      int maxCharsBudgetPerCall,
                                      File outFile) throws Exception {

        // 准备目录
        File parent = outFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        int total = cfg.output.rows != null ? cfg.output.rows : 500;
        int rowsThisCall = RowSizeEstimator.decideRowsPerCall(cfg, maxCharsBudgetPerCall);

        // 统计已生成的“元组数”（粗略用(计数）
        final int[] generated = {0};

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outFile), 64 * 1024)) {

            // 1) 首段（要求含BEGIN;，禁止COMMIT;）
            String firstPrompt = PagedSqlPrompt.firstChunk(cfg, Math.min(rowsThisCall, total));
            System.out.println("=== 首段开始（流式） ===");

            streamOnePromptAndWrite(ds, model, firstPrompt, bw, /*allowBegin*/true, /*allowCommit*/false, generated);

            // 没有BEGIN或没有INSERT，认为首段失败
            bw.flush();
            // 粗检：文件前几 KB 应含 BEGIN 和 INSERT
            // （如要更强校验，可读取 outFile 前 8192 bytes 再断言）
            System.out.println("\n=== 首段结束，已生成估算行数：" + generated[0] + " ===");

            // 2) 中间段
            while (generated[0] < total) {
                int remaining = total - generated[0];
                boolean isFinal = remaining <= rowsThisCall;

                String prompt = isFinal
                        ? PagedSqlPrompt.finalChunk(cfg, remaining, generated[0])
                        : PagedSqlPrompt.nextChunk(cfg, Math.min(rowsThisCall, remaining), generated[0]);

                System.out.println("=== " + (isFinal ? "尾段" : "续段") + "开始（流式） ===");
                streamOnePromptAndWrite(ds, model, prompt, bw,
                        /*allowBegin*/false,
                        /*allowCommit*/isFinal, // 只有尾段允许COMMIT;
                        generated);
                bw.flush();
                System.out.println("\n=== " + (isFinal ? "尾段" : "续段") + "结束，累计估算行数：" + generated[0] + " ===");

                // 动态收敛/放宽 rowsThisCall：用返回字符数粗略评估
                // （这里我们在回调中统计了 lastChunkChars）
                int lastChars = lastChunkChars;
                if (lastChars > (maxCharsBudgetPerCall * 0.9)) {
                    rowsThisCall = Math.max(20, (int) (rowsThisCall * 0.7));
                } else if (lastChars < (maxCharsBudgetPerCall * 0.5)) {
                    rowsThisCall = Math.min(rowsThisCall + 20, rowsThisCall * 2);
                }

                if (isFinal) break;
            }

            // 3) 若未落地 COMMIT;（比如模型忘了输出），手动补一次
            // 为避免重复，我们只在文件末尾补，不回读文件；由调用者确保提示词已尽力要求。
            if (!lastChunkHasCommit) {
                bw.write("\nCOMMIT;\n");
                bw.flush();
            }
        }

        System.out.println("✅ 流式生成完成： " + outFile.getAbsolutePath());
        return outFile;
    }

    // --- 内部状态（用于动态调参/校验） ---
    private static volatile int lastChunkChars = 0;
    private static volatile boolean lastChunkHasCommit = false;

    /**
     * 以流式方式请求一次 Prompt，并边播边清洗/打印/写盘。
     * 同时粗略统计“新增元组数”（通过 '(' 计数）
     */
    private static void streamOnePromptAndWrite(DeepSeekService ds, String model, String prompt,
                                                BufferedWriter bw,
                                                boolean allowBegin, boolean allowCommit,
                                                int[] generated) throws Exception {

        final StringBuilder chunkAcc = new StringBuilder(32 * 1024);
        final int[] tuplesInThisChunk = {0};
        final boolean[] seenBegin = {false};

        ds.streamChatCompletionRaw(model, prompt, 0.2, delta -> {
            String piece = SqlStreamSanitizer.sanitizeDelta(delta);

            // 约束：不允许BEGIN/COMMIT时，删除之
            if (!allowBegin) piece = piece.replaceAll("(?i)\\bBEGIN;\\s*", "");
            if (!allowCommit) piece = piece.replaceAll("(?i)\\bCOMMIT;\\s*", "");

            // 首段需要看到BEGIN;（仅用于标记，不强行写入时机）
            // 放在方法内，作为闭包状态
            final StringBuilder rolling = new StringBuilder(128);

// 回调里替换为：
            String upper = piece.toUpperCase();
            rolling.append(upper);
            if (rolling.length() > 128) {
                rolling.delete(0, rolling.length() - 128); // 只保留末尾128字符
            }

            if (allowBegin && !seenBegin[0]) {
                if (rolling.toString().matches("(?s).*\\bBEGIN\\b\\s*;?.*")) {
                    seenBegin[0] = true;
                }
            }
            if (allowCommit) {
                if (rolling.toString().matches("(?s).*\\bCOMMIT\\b\\s*;?.*")) {
                    lastChunkHasCommit = true;
                }
            }


            // 实时输出到控制台
            System.out.print(piece);

            // 实时写盘
            try {
                bw.write(piece);
            } catch (Exception e) { /* 让异常抛到外层 */ }

            // 统计元组数（非常粗略，但实时）
            for (int i = 0; i < piece.length(); i++) {
                if (piece.charAt(i) == '(') tuplesInThisChunk[0]++;
            }

            chunkAcc.append(piece);
        });

        // 一次prompt结束
        bw.write("\n"); // 统一换行
        bw.flush();

        lastChunkChars = chunkAcc.length();
        generated[0] += tuplesInThisChunk[0];

        if (allowBegin && !seenBegin[0]) {
            throw new IllegalStateException("首段未检测到 BEGIN;，请检查提示词或响应。");
        }
        if (chunkAcc.indexOf("INSERT INTO") < 0) {
            throw new IllegalStateException("本段未检测到 INSERT INTO，响应可能被清洗掉或模型跑题。");
        }
    }
}
