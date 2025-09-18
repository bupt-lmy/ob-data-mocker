package com.oceanbase.tools.datamocker.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import java.io.FileReader;

public class TaskConfigLoader {
    public static TaskConfig load(String path) throws Exception {
        Gson gson = new GsonBuilder()
                .serializeNulls()
                .setPrettyPrinting()
                .create();
        // 允许 // 注释、尾逗号等较宽松格式；如果你不需要注释，也可以直接用 gson.fromJson(new FileReader(...))
        try (JsonReader reader = new JsonReader(new FileReader(path))) {
            reader.setLenient(true);
            return gson.fromJson(reader, TaskConfig.class);
        }
    }
}
