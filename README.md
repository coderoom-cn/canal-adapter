
# canal-adapter

该项目为canal客户端，根据项目需要做了一定的定制化需求。

canal为生产者，canal-adapter是消费者，使用canal-adapter之前需要安装配置canal（搭配canal生产者）

## 数据库同步方案
接收canal的binlog，然后处理转换binlog。实现实时etl

## canal
Canal 会将自己伪装成 MySQL 从节点（Slave），并从主节点（Master）获取 Binlog，解析和贮存后供下游消费端使用。


***

### canal 开启

1. MySQL 主节点开启 Binlog。
```
log-bin=mysql-bin #添加这一行就ok
binlog-format=ROW #选择row模式
server_id=1 #配置mysql replaction需要定义，不能和canal的slaveId重复
```
2. mysql slave 相关权限 授权给canal。
```
# 创建用户
mysql> CREATE USER 'canal'@'localhost' IDENTIFIED BY 'canal';
# 授权
mysql> GRANT ALL PRIVILEGES ON *.* TO 'canal'@'%' ;
# 查看用户权限
mysql> show grants canal
```
3. 配置canal。
    - canal.properties : 这个是系统根配置文件。
    - instance.properties: 一个是example实例下的instance.properties,这个是instance级别的配置文件，每个instance一份。
    
4. 启动客户端

### canal 配置文件
#### canal.properties
##### 文件位置：
canal/conf
部分关键参数说明
```
#################################################
#########         common argument        #############
#################################################
canal.id= 1
# 系统根配置文件调用实例配置文件，而实例配置文件调用数据库信息，所以canal.ip为本地的ip地址，
端口号自己设置即可，消费者(客户端)与canal连接也是通过这个ip和端口。
canal.ip= 172.16.42.107
canal.port= 11111
.
. # 中间省略 #
.
#################################################
#########         destinations        #############
#################################################
# 此模块为一个实例模块，canal支持一对多，所以这个实例的名称为：example (消费者注册)#
canal.destinations= example
```
#### instance.properties
##### 文件位置
canal/conf/example

在配置canal之前，我们先连接mysql，查看MySQL master status
```
mysql> show master status;
+------------------+----------+--------------+------------------+-------------------+
| File             | Position | Binlog_Do_DB | Binlog_Ignore_DB | Executed_Gtid_Set |
+------------------+----------+--------------+------------------+-------------------+
| mysql-bin.000021 |     4241 |              |                  |                   |
+------------------+----------+--------------+------------------+-------------------+
1 row in set (0.00 sec)
```
File：mysql-bin.000021 
Position：4241 
记住这两个值
##### 关键配置信息
```
## mysql serverId
canal.instance.mysql.slaveId=234 # 不能与mysql配置文件中的slaveId重复
# position info
canal.instance.master.address=192.168.187.130:3306 # 为数据库地址 我的数据库在虚拟机中
canal.instance.master.journal.name=mysql-bin.000021 # 填入我们刚才查看的master status
canal.instance.master.position=4241  # 填入我们刚才查看的master status
canal.instance.master.timestamp=

# username/password
canal.instance.dbUsername=canal # 自己数据库的信息 使用我们刚开权限的用户 canal
canal.instance.dbPassword=canal # 自己数据库的信息
canal.instance.defaultDatabaseName=mytest # 自己数据库的信息
canal.instance.connectionCharset=UTF-8 # 自己数据库的信息

```


### 参考
[canal 1.0.25 快速启动配置](https://segmentfault.com/a/1190000012862191)
