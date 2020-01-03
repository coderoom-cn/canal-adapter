package com.hebaibai.miner;


import lombok.Data;

@Data
public class DataSourceConf {
    private String name;
    private String url;
    private String username;
    private String password;
    private String driverClass;
}
