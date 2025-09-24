#!/usr/bin/env bash
set -euo pipefail
# Create FIFO and TRANSACTION topics and the ordered consumer group.
docker exec -it rmqbroker sh -lc '
mqadmin updateTopic -n 127.0.0.1:9876 -t OrderFifoTopic -c DefaultCluster -a +message.type=FIFO
mqadmin updateSubGroup -n 127.0.0.1:9876 -c DefaultCluster -g OrderFifoGroup -o true
mqadmin updateTopic -n 127.0.0.1:9876 -t OrderTxnTopic  -c DefaultCluster -a +message.type=TRANSACTION
echo "Topics and group created."
'
