package com.example.orderdemo.rocketmq;

import lombok.extern.log4j.Log4j2;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientConfigurationBuilder;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.consumer.FilterExpression;
import org.apache.rocketmq.client.apis.consumer.FilterExpressionType;
import org.apache.rocketmq.client.apis.consumer.PushConsumer;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.client.apis.producer.TransactionChecker;
import org.apache.rocketmq.client.apis.producer.TransactionResolution;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.ByteBuffer;
import java.util.Collections;

@Log4j2
@Configuration
public class RocketMQConfig {
    @Value("${app.rocketmq.endpoints}")
    private String endpoints;

    @Value("${app.rocketmq.fifoTopic}")
    private String fifoTopic;

    @Value("${app.rocketmq.txnTopic}")
    private String txnTopic;

    @Value("${app.rocketmq.fifoGroup}")
    private String fifoGroup;

    /**
     * 创建 RocketMQ 客户端服务提供者
     * @return
     */
    @Bean
    public ClientServiceProvider clientServiceProvider() {
        return ClientServiceProvider.loadService();
    }

    /**
     * 创建客户端配置对象
     * 设置 RocketMQ 服务器的连接端点（从 app.rocketmq.endpoints 配置项读取）
     * 这个配置会被所有的生产者和消费者共享使用
     * 包含连接超时、重试策略等基础配置
     * @return
     */
    @Bean
    public ClientConfiguration clientConfiguration() {
        ClientConfigurationBuilder builder = ClientConfiguration.newBuilder().setEndpoints(endpoints);
        return builder.build();
    }

    /**
     *  创建 FIFO（顺序）消息生产者
     * 用于发送有序消息，保证同一订单的消息按顺序处理
     * 绑定到 fifoTopic（从配置文件读取）
     * destroyMethod = "close" 确保应用关闭时正确释放资源
     */
    /** Producer for FIFO (normal ordered messages). */
    @Bean(destroyMethod = "close")
    public Producer fifoProducer(ClientServiceProvider provider, ClientConfiguration cfg) throws Exception {
        return provider.newProducerBuilder().setClientConfiguration(cfg).setTopics(fifoTopic).build();
    }

    /**
     * 事务消息检查器，用于处理事务消息的回查机制
     * 当 RocketMQ 无法确定事务消息状态时，会调用此检查器
     * 通过查询数据库中是否存在该订单来决定事务结果：
     * 如果订单存在 → COMMIT（提交事务消息）
     * 如果订单不存在 → ROLLBACK（回滚事务消息）
     * 这是分布式事务一致性的关键组件
     */
    /** Transaction checker used by txnProducer to resolve unknown states via DB lookup.*/
    @Bean
    public TransactionChecker transactionChecker(com.example.orderdemo.repository.OrderRepository orderRepo) {
        return messageView -> {
            String orderId = messageView.getProperties().get("OrderId");
            boolean exists = orderRepo.existsByOrderId(orderId);
            return exists ? TransactionResolution.COMMIT : TransactionResolution.ROLLBACK;
        };
    }

    /*
     * 创建事务消息生产者
     * 用于发送事务消息，确保数据库操作和消息发送的原子性
     * 设置事务检查器，用于处理未知状态的事务消息
     * 绑定到 txnTopic（事务消息主题）
     * 在订单创建场景中使用，保证订单入库和消息发送的一致性
     */
    /** Transaction producer for order creation */
    @Bean(destroyMethod = "close")
    public Producer txnProducer(ClientServiceProvider provider, ClientConfiguration cfg,
                               TransactionChecker checker) throws Exception {
        return provider.newProducerBuilder()
                .setClientConfiguration(cfg)
                .setTransactionChecker(checker)
                .setTopics(txnTopic) // bind txn topic for recovery
                .build();
    }

    /*
      创建 FIFO 主题的推送式消费者
      订阅设置：订阅 fifoTopic，接收所有标签的消息（"*"）
      消费组：使用 fifoGroup 作为消费者组
      消息处理逻辑：
      解析消息体格式：订单ID:操作类型
      根据操作类型调用相应的服务方法：
      PAID → 调用 orderService.markPaid(orderId) 标记为已支付
      SHIPPED → 调用 orderService.markShipped(orderId) 标记为已发货
      返回消费结果（成功或失败）
     */
    /** Push consumer for FIFO topic to update order state (PAID, SHIPPED). */
    @Bean(destroyMethod = "close")
    public PushConsumer fifoConsumer(ClientServiceProvider provider, ClientConfiguration cfg,
                                     com.example.orderdemo.service.OrderService orderService) throws Exception {
        FilterExpression fe = new FilterExpression("*", FilterExpressionType.TAG);
        return provider.newPushConsumerBuilder()
                .setClientConfiguration(cfg)
                .setConsumerGroup(fifoGroup)
                .setSubscriptionExpressions(Collections.singletonMap(fifoTopic, fe))
                .setMessageListener(messageView -> {
                    ByteBuffer bodyBuffer = messageView.getBody();
                    byte[] bodyBytes = new byte[bodyBuffer.remaining()];
                    bodyBuffer.get(bodyBytes);
                    String body = new String(bodyBytes, java.nio.charset.StandardCharsets.UTF_8);
                    try {
                        String[] parts = body.split("：");
                        String orderId = parts[0];
                        String step =  parts[1];
                        if ("PAID".equalsIgnoreCase(step)) {
                            orderService.markPaid(orderId);
                        } else if ("SHIPPED".equalsIgnoreCase(step)) {
                            orderService.markShipped(orderId);
                        }
                        return org.apache.rocketmq.client.apis.consumer.ConsumeResult.SUCCESS;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return ConsumeResult.FAILURE;
                    }
                })
                .build();
    }


}
