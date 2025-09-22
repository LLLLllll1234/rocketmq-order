package com.example.orderdemo.rocketmq;

import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientConfigurationBuilder;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

    @Bean
    public ClientServiceProvider clientServiceProvider() {
        return ClientServiceProvider.loadService();
    }

    @Bean
    public ClientConfiguration clientConfiguration() {
        ClientConfigurationBuilder builder = ClientConfiguration.newBuilder().setEndpoints(endpoints);
        return builder.build();
    }

    /** Producer for FIFO (normal ordered messages). */
    @Bean(destroyMethod = "close")
    public Producer fifoProducer(ClientServiceProvider provider, ClientConfiguration cfg) throws Exception {
        return provider.newProducerBuilder().setClientConfiguration(cfg).setTopics(fifoTopic).build();
    }
}
