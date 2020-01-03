package com.hebaibai.miner;

import lombok.Data;

@Data
public class CanalConf {
    private String ip;
    private int port;
    private String user;
    private String password;
    private String destination;
}
