# Sleep-Parser

## 项目简介

Sleep-Parser 是一个基于 Spring Boot 的 MQTT 数据解析服务，专门用于处理和解析能斯达智能睡眠监测产品设备睡眠数据。该服务通过 MQTT 协议接收设备发送的睡眠数据，解析后将结果发布到指定主题，方便其他系统或应用程序使用。

## 功能特性

- **MQTT 数据接收**：通过 MQTT 协议接收设备发送的睡眠数据
- **数据解析**：使用专门的解析工具解析睡眠数据
- **结果发布**：将解析结果发布到指定的 MQTT 主题
- **线程池处理**：使用线程池处理消息，提高并发处理能力
- **安全认证**：支持 MQTT 服务器的用户名和密码认证
- **自动重连**：当 MQTT 连接断开时自动重连
- **日志管理**：使用 logback 进行详细的日志记录
- **Docker 支持**：提供 Docker 部署方案

## 项目结构

```
sleep-parser/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── leanstar/
│   │   │           └── sleepparser/
│   │   │               ├── config/
│   │   │               │   ├── MqttConfig.java      # MQTT 连接配置
│   │   │               │   └── ThreadPoolConfig.java # 线程池配置
│   │   │               ├── service/
│   │   │               │   └── SleepDataService.java # 睡眠数据处理服务
│   │   │               └── SleepParserApplication.java # 应用程序入口
│   │   └── resources/
│   │       ├── application.yml      # 应用配置文件
│   │       └── logback-plus.xml     # 日志配置文件
│   └── test/
│       └── java/
│           └── com/
│               └── leanstar/
│                   └── sleepparser/
│                       └── SleepParserApplicationTests.java # 测试类
├── .editorconfig          # 编辑器配置
├── .gitignore
├── Dockerfile           # Docker 构建文件
├── docker-compose.yml   # Docker Compose 配置文件
├── pom.xml
└── README.md            # 项目说明
```

## 配置说明

项目使用 `application.yml` 作为配置文件，主要配置项如下：

```yaml
spring:
  application:
    name: sleep-parser
  profiles:
    active: default

server:
  port: 8080

logging:
  level:
    com.leanstar.sleepparser: info
    org.springframework: warn
  config: classpath:logback-plus.xml

mqtt:
  broker: ${MQTT_BROKER:tcp://192.168.10.188:1883}
  client:
    id: ${MQTT_CLIENT_ID:sleep-parser-server}
  username: ${MQTT_USERNAME:}
  password: ${MQTT_PASSWORD:}
```

## 快速开始

### 环境要求

- JDK 1.8 或更高版本
- Maven 3.6 或更高版本
- MQTT 服务器（如 EMQX 5.8.8）
- Docker 和 Docker Compose（用于容器化部署）

### 构建项目

```bash
# 克隆项目
git clone https://github.com/jyehui6/sleep-parser.git
cd sleep-parser

# 构建项目
mvn clean package
```

### 运行项目

#### 方法一：直接运行

```bash
# 运行项目
java -jar target/sleep-parser.jar
```

#### 方法二：使用 Docker 部署

```bash
# 构建并运行容器
docker-compose up -d

# 查看容器状态
docker-compose ps

# 查看日志
docker-compose logs -f sleep-parser-server

# 停止容器
docker-compose down
```

### Docker 环境变量配置

在 `docker-compose.yml` 文件中，你可以通过环境变量来配置 MQTT 连接：

- `MQTT_BROKER`：MQTT 服务器地址
- `MQTT_USERNAME`：MQTT 用户名
- `MQTT_PASSWORD`：MQTT 密码
- `MQTT_CLIENT_ID`：客户端 ID 前缀

## 使用方法

1. **设备发送数据**：设备向 `iot/{deviceSn}/pub` 主题发送睡眠数据
2. **服务处理**：Sleep Parser 服务接收数据，解析后将结果发布到 `iot/{deviceSn}/parsed` 主题
3. **其他系统订阅**：其他系统可以订阅 `iot/{deviceSn}/parsed` 主题获取解析后的睡眠数据

### 主题格式

- **数据输入主题**：`iot/{deviceSn}/pub`，其中 `{deviceSn}` 是设备编号（字母和数字组合）
- **数据输出主题**：`iot/{deviceSn}/parsed`，其中 `{deviceSn}` 与输入主题中的设备编号相同

## 项目依赖

| 依赖 | 版本 | 用途               |
|------|------|------------------|
| spring-boot-starter | 2.7.18 | Spring Boot 核心依赖 |
| org.eclipse.paho.client.mqttv3 | 1.2.5 | MQTT 客户端库        |
| jackson-databind | 2.13.5 | JSON 解析库         |
| sleepparse | 1.0.0.RELEASE | 睡眠数据解析插件         |

## 日志配置

项目使用 logback 进行日志管理，日志配置文件为 `logback-plus.xml`：

- **日志文件位置**：`./logs` 目录
- **日志文件类型**：
  - `sys-console.log`：控制台输出日志
  - `sys-info.log`：INFO 级别的日志
  - `sys-error.log`：ERROR 级别的日志
- **日志滚动策略**：基于时间滚动，每天生成一个新的日志文件
- **日志保留时间**：INFO 和 ERROR 日志保留 30 天，控制台日志保留 1 天