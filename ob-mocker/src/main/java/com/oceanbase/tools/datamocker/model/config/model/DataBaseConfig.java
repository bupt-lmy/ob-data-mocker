package com.oceanbase.tools.datamocker.model.config.model;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * 数据库连接信息配置封装对象
 *
 * @author yh263208
 * @date 2020-12-24 15:30
 * @since OBMOCKER-snapshot-0.1.0
 */
@Getter
@Setter
public class DataBaseConfig {
    /**
     * 数据库的host
     */
    private String host;
    /**
     * 数据库的连接端口
     */
    private Integer port;
    /**
     * 业务数据库的用户名
     */
    private String user;
    /**
     * 租户名
     */
    private String tenant;
    /**
     * 集群名
     */
    private String cluster;
    /**
     * 数据库连接密码
     */
    private String password;
    /**
     * 默认数据库名称
     */
    private String defaultSchame;
    /**
     * 数据库连接参数
     */
    private Map<String, String> connectParam;
}
