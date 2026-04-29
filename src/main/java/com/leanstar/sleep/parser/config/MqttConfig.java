package com.leanstar.sleep.parser.config;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;

@Configuration
public class MqttConfig {

    private static final Logger logger = LoggerFactory.getLogger(MqttConfig.class);

    @Value("${mqtt.broker}")
    private String broker;

    @Value("${mqtt.client.id}")
    private String clientIdPrefix;

    @Value("${mqtt.username:}")
    private String username;

    @Value("${mqtt.password:}")
    private String password;

    // MQTT客户端实例
    private MqttClient mqttClient;

    @Bean
    public MqttClient mqttClient() throws Exception {
        String clientId = clientIdPrefix + System.currentTimeMillis();
        MemoryPersistence persistence = new MemoryPersistence();

        mqttClient = new MqttClient(broker, clientId, persistence);
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        connOpts.setConnectionTimeout(10);  // 连接超时时间
        connOpts.setKeepAliveInterval(60);  // 心跳间隔
        connOpts.setMaxInflight(100);  // 最大并发消息数
        connOpts.setAutomaticReconnect(true);  // 自动重连

        // 设置用户名和密码（如果配置了）
        if (!username.isEmpty() && !password.isEmpty()) {
            connOpts.setUserName(username);
            connOpts.setPassword(password.toCharArray());
            logger.info("MQTT connection with authentication enabled");
        }

        // 关闭跟踪，减少日志开销
        System.setProperty("org.eclipse.paho.client.mqttv3.trace", "false");

        mqttClient.connect(connOpts);
        logger.info("MQTT connected successfully");
        return mqttClient;
    }

    @PreDestroy
    public void cleanup() {
        if (mqttClient != null) {
            try {
                if (mqttClient.isConnected()) {
                    mqttClient.disconnect();
                }
                mqttClient.close();
                logger.info("MQTT client closed");
            } catch (Exception e) {
                logger.error("Error closing MQTT client: {}", e.getMessage());
            }
        }
    }

}
