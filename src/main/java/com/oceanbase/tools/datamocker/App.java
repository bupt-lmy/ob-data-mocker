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
package com.oceanbase.tools.datamocker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oceanbase.tools.datamocker.agent.*;
import com.oceanbase.tools.datamocker.config.TaskConfig;
import com.oceanbase.tools.datamocker.config.TaskConfigLoader;
import com.oceanbase.tools.datamocker.model.config.MockTaskConfig;
import com.oceanbase.tools.datamocker.schedule.MockContext;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.*;
import java.util.*;

public class App {
    public static void main(String[] args) throws Exception {
        // 1. 读取 JSON 配置
        ObjectMapper mapper = new ObjectMapper();
        MockTaskConfig taskConfig = mapper.readValue(
                new File(App.class.getClassLoader().getResource("data-config.json").getFile()),
                MockTaskConfig.class);

        // 2. 启动模拟数据任务
        ObMockerFactory factory = new ObMockerFactory(taskConfig);
        ObDataMocker mocker = factory.create();
        MockContext context = mocker.start();

        System.out.println("✅ 模拟数据任务已启动，请检查数据库 EMP 表！");
        System.out.println("输出文件夹：" + context.getLogDir());


    }

//    public static void main(String[] args) throws Exception {
//        // 直接传参数，而不是读环境或配置
//        final String apiKey = "sk-474eacf7c14547ddb283b9b10811b6c2";
//        final String endpoint = "https://api.deepseek.com/chat/completions";
//
//        // App.java 中的 main（节选）
//        TaskConfig cfg = TaskConfigLoader.load("emp_task.json");
//
//        DeepSeekService ds = new DeepSeekService(
//                apiKey,
//                endpoint,
//                300
//        );
//
//// 单次调用字符预算（按你网络/模型情况调小或调大）
//        int maxCharsBudgetPerCall = 14000;
//
//// 输出文件（放到配置的目录下）
//        java.io.File outDir = new java.io.File(cfg.output.dir == null ? "./output" : cfg.output.dir);
//        if (!outDir.exists()) outDir.mkdirs();
//        String fileName = (cfg.output.filePrefix == null ? "mock_seed" : cfg.output.filePrefix) + "_" + System.currentTimeMillis() + ".sql";
//        java.io.File outFile = new java.io.File(outDir, fileName);
//
//// ✅ 走“流式分页”生成：一边显示、一边写盘
//        SqlStreamingPagedGenerator.generateToFile(cfg, ds, "deepseek-chat", maxCharsBudgetPerCall, outFile);
//
//        System.out.println("\n✅ 已生成SQL文件: " + outFile.getAbsolutePath());
//
//    }

}
