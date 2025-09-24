# RocketMQ Order Demo (Java + Spring Boot + MySQL)

## Quickstart
1) Start infra (RocketMQ + MySQL + Dashboard):
```bash
docker compose up -d
./init-topics.sh
```
2) Build and run app:
```bash
cd app
./mvnw -q -v || mvn -v
mvn spring-boot:run
```
3) Open http://localhost:8088 to use the tiny web UI, or use curl:
```bash
# Create order
curl -s -X POST localhost:8088/api/orders -H 'Content-Type: application/json' -d '{"amount": 66.00}'

# Pay
curl -s -X POST localhost:8088/api/orders/<orderId>/pay

# Ship
curl -s -X POST localhost:8088/api/orders/<orderId>/ship
```
You can also open the RocketMQ Dashboard at http://localhost:8082.

Notes:
- Java client connects to RocketMQ **Proxy (8081)**.
- `OrderTxnTopic` is a **TRANSACTION** topic for order creation consistency.
- `OrderFifoTopic` is a **FIFO** topic; events use `orderId` as **message group** to keep per-order sequencing.
