package com.example.orderdemo.rocketmq;

import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.apache.rocketmq.client.apis.producer.Transaction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OrderEventPublisher {
    private final ClientServiceProvider provider;
    private final Producer fifoProducer;
    private final Producer txnProducer;
    private final String fifoTopic;
    private final String txnTopic;

    /**
     * 依赖注入初始化
     * 注入 ClientServiceProvider：用于创建消息对象
     * 注入 fifoProducer：FIFO 顺序消息生产者
     * 注入 txnProducer：事务消息生产者
     * 获取配置的主题名称
     * @param provider
     * @param fifoProducer
     * @param txnProducer
     * @param fifoTopic
     * @param txnTopic
     */
    public OrderEventPublisher(ClientServiceProvider provider,
                               Producer fifoProducer,
                               Producer txnProducer,
                               @Value("${app.rocketmq.fifoTopic}") String fifoTopic,
                               @Value("${app.rocketmq.txnTopic}") String txnTopic) {
        this.provider = provider;
        this.fifoProducer = fifoProducer;
        this.txnProducer = txnProducer;
        this.fifoTopic = fifoTopic;
        this.txnTopic = txnTopic;
    }

    /**
     * 发送 FIFO 顺序消息，用于订单状态变更事件
     * setTopic(fifoTopic)：设置目标主题
     * setKeys(orderId)：设置消息键（用于查询和过滤）
     * setTag("order_event")：设置消息标签
     * setMessageGroup(orderId)：关键！设置消息组，确保同一订单的消息有序处理
     * setBody(...)：消息体格式为 "订单ID:状态"
     * @param orderId
     * @param step
     * @return
     * @throws Exception
     */
    public SendReceipt sendFifo(String orderId, String step) throws Exception {
        Message msg = provider.newMessageBuilder()
                .setTopic(fifoTopic)
                .setKeys(orderId)
                .setTag("order_event")
                .setBody((orderId + ":" + step).getBytes())
                .build();
        return fifoProducer.send(msg);
    }

    /**
     * 发送事务消息，用于订单创建事件
     * 事务流程：
     * beginTransaction()：开始事务
     * 构建消息：
     * setTopic(txnTopic)：事务消息主题
     * setTag("order_created")：订单创建标签
     * addProperty("OrderId", orderId)：重要！添加属性，用于 transactionChecker 回查
     * 消息体格式："created:订单ID"
     * send(msg, tx)：发送半消息（事务预提交状态）
     * TxHolder.set(tx)：将事务句柄存储到 ThreadLocal，供业务层使用
     * @param orderId
     * @throws Exception
     */
    public void sendTxnCreated(String orderId) throws Exception {
        final Transaction tx = txnProducer.beginTransaction();
        Message msg = provider.newMessageBuilder()
                .setTopic(txnTopic)
                .setKeys(orderId)
                .setTag("order_created")
                .addProperty("OrderId", orderId)
                .setBody(("created: " + orderId).getBytes())
                .build();
        txnProducer.send(msg, tx);
        TxHolder.set(tx);
    }

    /**
     * 事务句柄的线程局部存储工具类
     * set(Transaction t)：将事务对象存储到当前线程
     * get()：获取当前线程的事务对象
     * clear()：清除当前线程的事务对象（防止内存泄漏）
     */
    public static class TxHolder {
        private static final ThreadLocal<Transaction> TL = new ThreadLocal<>();
        public static void set(Transaction t) {TL.set(t);}
        public static Transaction get() {return TL.get();}
        public static void clear() {TL.remove();}
    }



}
