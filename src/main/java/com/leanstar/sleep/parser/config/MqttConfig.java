package com.leanstar.sleep.parser.config;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;

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

    private MqttClient mqttClient;
    private String clientId;  // 保存clientId，重连时使用相同的ID
    private List<Subscription> subscriptions = new ArrayList<>();  // 保存订阅列表，重连后恢复

    @Bean
    public MqttClient mqttClient() throws Exception {
        clientId = clientIdPrefix;
        MemoryPersistence persistence = new MemoryPersistence();

        mqttClient = new MqttClient(broker, clientId, persistence);
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(false);   // 保持会话，重连后恢复订阅
        connOpts.setConnectionTimeout(15); // 连接超时时间
        connOpts.setKeepAliveInterval(60); // 心跳间隔
        connOpts.setMaxInflight(200); // 最大并发消息数

        // 设置用户名和密码（如果配置了）
        if (!username.isEmpty() && !password.isEmpty()) {
            connOpts.setUserName(username);
            connOpts.setPassword(password.toCharArray());
            logger.info("MQTT connection with authentication enabled");
        }

        // 关闭跟踪，减少日志开销
        System.setProperty("org.eclipse.paho.client.mqttv3.trace", "false");

        // 设置连接回调
        mqttClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                if (reconnect) {
                    logger.info("MQTT reconnection successful, restoring subscriptions...");
                    restoreSubscriptions();
                } else {
                    logger.info("MQTT connected successfully with clientId: {}", clientId);
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                logger.error("MQTT connection lost: {}", cause.getMessage());
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {}

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {}
        });

        mqttClient.connect(connOpts);
        return mqttClient;
    }

    /**
     * 注册订阅（用于重连后恢复）
     */
    public void registerSubscription(String topic, int qos, IMqttMessageListener listener) {
        subscriptions.add(new Subscription(topic, qos, listener));
        // 如果已连接，立即订阅
        if (isConnected()) {
            try {
                mqttClient.subscribe(topic, qos, listener);
                logger.info("Subscribed to topic: {}", topic);
            } catch (MqttException e) {
                logger.error("Failed to subscribe to topic {}: {}", topic, e.getMessage());
            }
        }
    }

    /**
     * 恢复所有订阅
     */
    private void restoreSubscriptions() {
        logger.info("Restoring {} subscriptions...", subscriptions.size());
        for (Subscription subscription : subscriptions) {
            try {
                mqttClient.subscribe(subscription.topic, subscription.qos, subscription.listener);
                logger.info("Restored subscription to topic: {}", subscription.topic);
            } catch (MqttException e) {
                logger.error("Failed to restore subscription to topic {}: {}", subscription.topic, e.getMessage());
            }
        }
    }

    /**
     * 订阅信息内部类
     */
    private static class Subscription {
        String topic;
        int qos;
        IMqttMessageListener listener;

        Subscription(String topic, int qos, IMqttMessageListener listener) {
            this.topic = topic;
            this.qos = qos;
            this.listener = listener;
        }
    }

    /**
     * 检查连接状态
     */
    public boolean isConnected() {
        return mqttClient != null && mqttClient.isConnected();
    }

    /**
     * 尝试重连
     */
    public void reconnect() {
        if (mqttClient != null && !mqttClient.isConnected()) {
            try {
                logger.info("Attempting MQTT reconnection...");
                mqttClient.reconnect();
                // 等待连接完成
                int maxWait = 10;
                int waitCount = 0;
                while (!mqttClient.isConnected() && waitCount < maxWait) {
                    Thread.sleep(1000);
                    waitCount++;
                }
                if (mqttClient.isConnected()) {
                    logger.info("MQTT reconnection successful");
                    // 手动触发订阅恢复（确保订阅被恢复）
                    restoreSubscriptions();
                } else {
                    logger.error("MQTT reconnection failed after {} seconds", maxWait);
                }
            } catch (Exception e) {
                logger.error("MQTT reconnection failed: {}", e.getMessage());
            }
        }
    }

    /**
     * 获取MQTT客户端实例
     */
    public MqttClient getMqttClient() {
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
