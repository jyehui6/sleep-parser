package com.leanstar.sleep.parser.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.leanstar.sleep.parser.config.MqttConfig;
import com.leanstar.sleepparse.util.SleepDataParserUtil;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.Executor;

@Service
public class SleepDataService {

    private static final Logger logger = LoggerFactory.getLogger(SleepDataService.class);

    @Autowired
    private MqttClient mqttClient;

    @Autowired
    private MqttConfig mqttConfig;

    @Autowired
    @Qualifier("sleepDataExecutor")
    private Executor executor;

    @PostConstruct
    public void init() {
        try {
            // 使用注册订阅机制，重连后会自动恢复订阅
            mqttConfig.registerSubscription("iot/+/pub", 2, messageListener);
            logger.info("Sleep data service initialized successfully");
        } catch (Exception e) {
            logger.error("Error initializing sleep data service：", e);
        }
    }

    /**
     * 消息监听器
     */
    private final IMqttMessageListener messageListener = (topic, message) -> {
        executor.execute(() -> processMessage(topic, message));
    };

    /**
     * 处理消息
     */
    private void processMessage(String topic, MqttMessage message) {
        try {
            if (!mqttConfig.isConnected()) {
                logger.warn("MQTT connection not available, message dropped");
                return;
            }

            // 获取设备编号
            String deviceSn = extractDeviceSn(topic);
            if (deviceSn == null) {
                logger.warn("Invalid topic format: {}", topic);
                return;
            }

            String payload = new String(message.getPayload());
            logger.info("Processing message from device：{}, Received message：{}", deviceSn, payload);

            // 解析数据
            ObjectNode result = SleepDataParserUtil.parse(payload);
            logger.info("Parse data：{}", result);
            // 发布解析结果到新主题
            if (result != null) {
                String parsedTopic = "iot/" + deviceSn + "/parsed";
                MqttMessage parsedMessage = new MqttMessage(result.toString().getBytes());
                parsedMessage.setQos(2);
                mqttClient.publish(parsedTopic, parsedMessage);
                logger.info("Published parsed result to：{}, parsed message：{}", parsedTopic, result);
            } else {
                logger.warn("Failed to parse message：{}", payload);
            }
        } catch (Exception e) {
            logger.error("Error processing message：", e);
        }
    }

    /**
     * 提取设备编号
     */
    public String extractDeviceSn(String topic) {
        String[] parts = topic.split("/");
        if (parts.length == 3 && "iot".equals(parts[0]) && "pub".equals(parts[2])) {
            return parts[1];
        }
        return null;
    }

}
