package com.oceanbase.tools.datamocker.model.config.model;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * Database connection information configuration package object
 *
 * @author yh263208
 * @date 2020-12-24 15:30
 * @since OBMOCKER-snapshot-0.1.0
 */
@Getter
@Setter
public class DataBaseConfig {
    private String host;
    private Integer port;
    private String user;
    private String tenant;
    private String cluster;
    private String password;
    private String defaultSchame;
    /**
     * Database connection parameters
     */
    private Map<String, String> connectParam;
}
