package com.example.orderdemo.service;

import com.example.orderdemo.model.Order;
import com.example.orderdemo.model.OrderStatus;
import com.example.orderdemo.repository.OrderRepository;
import com.example.orderdemo.rocketmq.OrderEventPublisher;
import org.apache.rocketmq.client.apis.producer.Transaction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class OrderService {
    private final OrderRepository orderRepo;
    private final OrderEventPublisher publisher;

    /**
     * 依赖注入构造函数
     * 注入 OrderRepository 用于数据库操作
     * 注入 OrderEventPublisher 用于发送 RocketMQ 消息
     * Spring 会自动调用此构造函数进行依赖注入
     * @param orderRepo
     * @param publisher
     */
    public OrderService(OrderRepository orderRepo, OrderEventPublisher publisher) {
        this.orderRepo = orderRepo;
        this.publisher = publisher;
    }

    /**
     * 创建订单，使用 RocketMQ 事务消息保证一致性
     * 生成订单ID：使用 UUID 生成唯一的订单标识
     * 发送半消息：调用 publisher.sendTxnCreated(orderId) 发送事务消息的第一阶段
     * 数据库操作：创建 Order 对象并保存到数据库
     * 事务处理：
     * 成功：提交 RocketMQ 事务消息 (tx.commit())
     * 失败：回滚 RocketMQ 事务消息 (tx.rollback())
     * 资源清理：在 finally 块中清理 ThreadLocal 变量
     */
    /** Create order using RocketMQ TRANSACTION message for consistency. */
    @Transactional
    public String create(BigDecimal amount) throws Exception {
        String orderId = "order-" + UUID.randomUUID().toString().substring(0, 8);
        // 1) Send half message and begin transaction
        publisher.sendTxnCreated(orderId);
        Transaction tx = OrderEventPublisher.TxHolder.get();
        try {
            // 2) Local DB transaction
            Order o = new Order();
            o.setOrderId(orderId);
            o.setAmount(amount);
            o.setStatus(OrderStatus.CREATED);
            orderRepo.save(o);

            // 3) Commit message transaction after DB success
            tx.commit();
            return orderId;
        } catch (Exception e) {
            if (tx != null) {
                try {
                    tx.rollback();
                } catch (Exception ignore) {}
            }
            throw e;
        } finally {
            OrderEventPublisher.TxHolder.clear();
        }
    }

    /**
     * 处理订单支付操作
     * 发送 FIFO（顺序）消息到 RocketMQ
     * 消息格式：订单ID:PAID
     * 消费者接收到消息后会更新订单状态为 PAID
     * 采用异步处理方式，提高系统性能
     * @param orderId
     * @throws Exception
     */
    public void pay(String orderId) throws Exception {
        // send FIFO event; consumer will update DB status
        publisher.sendFifo(orderId, "PAID");
    }

    /**
     * 处理订单发货操作
     * 发送 FIFO（顺序）消息到 RocketMQ
     * 消息格式：订单ID:SHIPPED
     * 消费者接收到消息后会更新订单状态为 SHIPPED
     * 确保订单状态变更的顺序性
     * @param orderId
     * @throws Exception
     */
    public void ship(String orderId) throws Exception {
        publisher.sendFifo(orderId, "SHIPPED");
    }

    /**
     * 标记订单为已支付状态（由消息消费者调用）
     * 查找订单：根据订单ID查找对应的订单
     * 状态检查：只有当前状态 ≤ CREATED 时才允许更新
     * 状态更新：将订单状态设置为 PAID
     * 事务保护：使用 @Transactional 确保数据一致性
     * 防止重复处理：通过状态检查避免重复更新
     * @param orderId
     */
    //called by consumer
    @Transactional
    public void markPaid(String orderId) {
        orderRepo.findByOrderId(orderId).ifPresent(o -> {
            if (o.getStatus().ordinal() == OrderStatus.CREATED.ordinal()) {
                o.setStatus(OrderStatus.PAID);
                orderRepo.save(o);
            }
        });
    }

    /**
     * 标记订单为已发货状态（由消息消费者调用）
     * 查找订单：根据订单ID查找对应的订单
     * 状态检查：只有当前状态 ≤ PAID 时才允许更新
     * 状态更新：将订单状态设置为 SHIPPED
     * 事务保护：使用 @Transactional 确保数据一致性
     * 防止越级更新：确保订单状态按正确顺序流转
     * @param orderId
     */
    @Transactional
    public void markShipped(String orderId) {
        orderRepo.findByOrderId(orderId).ifPresent(o -> {
            if (o.getStatus().ordinal() == OrderStatus.PAID.ordinal()) {
                o.setStatus(OrderStatus.SHIPPED);
                orderRepo.save(o);
            }
        });
    }

}
