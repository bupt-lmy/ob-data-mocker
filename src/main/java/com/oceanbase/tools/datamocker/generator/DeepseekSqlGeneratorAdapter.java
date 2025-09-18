/*
 * Copyright (c) 2025 OceanBase.
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

package com.oceanbase.tools.datamocker.generator;


import com.oceanbase.tools.datamocker.agent.*;
import com.oceanbase.tools.datamocker.config.TaskConfig;
import com.oceanbase.tools.datamocker.config.TaskConfigLoader;
import org.apache.commons.io.FileUtils;

import java.io.File;

public class DeepseekSqlGeneratorAdapter {

    public static File generateFromTaskConfig(
            String taskConfigPath,
            String apiKey,
            String endpoint,
            int timeoutSeconds
    ) throws Exception {

        // 1) 读取 LLM Task 配置（你之前写好的 TaskConfig/Loader）
        TaskConfig cfg = TaskConfigLoader.load(taskConfigPath);

        int maxCharsBudgetPerCall = 14000;


        // 3) DeepSeek 客户端
        DeepSeekService ds = new DeepSeekService(apiKey, endpoint, timeoutSeconds);

        // 输出文件（放到配置的目录下）
        java.io.File outDir = new java.io.File(cfg.output.dir == null ? "./output" : cfg.output.dir);
        if (!outDir.exists()) outDir.mkdirs();
        String fileName = (cfg.output.filePrefix == null ? "mock_seed" : cfg.output.filePrefix) + "_" + System.currentTimeMillis() + ".sql";
        java.io.File outFile = new java.io.File(outDir, fileName);
        SqlStreamingPagedGenerator.generateToFile(cfg, ds, "deepseek-chat", maxCharsBudgetPerCall, outFile);


        System.out.println("\n✅ LLM-SQL 已生成: " + outFile.getAbsolutePath());
        return outFile;
    }
}
