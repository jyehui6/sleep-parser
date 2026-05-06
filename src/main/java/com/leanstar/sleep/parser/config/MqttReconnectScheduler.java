package com.leanstar.sleep.parser.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class MqttReconnectScheduler {

    private static final Logger logger = LoggerFactory.getLogger(MqttReconnectScheduler.class);

    @Autowired
    private MqttConfig mqttConfig;

    @Value("${mqtt.reconnect.delay:5000}")
    private long reconnectDelay;

    /**
     * 定时检查MQTT连接状态，默认每30秒检查一次
     */
    @Scheduled(fixedRateString = "${mqtt.heartbeat.interval:30000}")
    public void checkConnection() {
        if (!mqttConfig.isConnected()) {
            logger.warn("MQTT connection lost, attempting reconnect...");
            try {
                mqttConfig.reconnect();
                // 等待重连完成
                Thread.sleep(reconnectDelay);
                if (mqttConfig.isConnected()) {
                    logger.info("MQTT reconnection successful");
                } else {
                    logger.error("MQTT reconnection failed");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("MQTT reconnection interrupted");
            }
        }
    }

}