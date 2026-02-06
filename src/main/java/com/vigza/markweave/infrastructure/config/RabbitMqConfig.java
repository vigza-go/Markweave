package com.vigza.markweave.infrastructure.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties.Retry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {
    public static final String COLLABORATION_EXCHANGE = "mw.collaboration.exchange";
    public static final String COLLABORATION_MSG_QUEUE = "mw.collaboration.queue";


    public static final String RETRY_EXCHANGE = "mw.retry.exchange";
    public static final String RETRY_QUEUE = "mw.retry.queue";
    public static final String RETRY_ROUTING_KEY = "mw.retry.key";
    public static final String RETRY_DELAY_QUEUE = "mw.retry.delay.queue";
    public static final String RETRY_DELAY_ROUTING_KEY = "mw,retry.delay.key";

    @Bean
    public FanoutExchange collaborationExchange(){
        return new FanoutExchange(COLLABORATION_EXCHANGE);
    }

    @Bean
    public Queue collaborationMsgQueue(){
        return new Queue(COLLABORATION_MSG_QUEUE);
    }

    @Bean
    public Binding collaborationBinding(FanoutExchange collaborationExchange,Queue collaborationMsgQueue){
        return BindingBuilder.bind(collaborationMsgQueue).to(collaborationExchange);
    }

    @Bean
    public DirectExchange retryExchange() {
        return new DirectExchange(RETRY_EXCHANGE);
    }

    @Bean
    public Queue retryQueue() {
        return new Queue(RETRY_QUEUE);
    }

    @Bean
    public Queue retryDelayQueue() {
        return new Queue(RETRY_DELAY_QUEUE);
    }

    @Bean
    public Binding retryDelayBinding(DirectExchange retryExchange, Queue retryDelayQueue) {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", RETRY_EXCHANGE);
        args.put("x-dead-letter-routing-key", RETRY_ROUTING_KEY);
        return BindingBuilder.bind(retryDelayQueue).to(retryExchange).with(RETRY_DELAY_ROUTING_KEY);
    }

    @Bean
    Binding retryBinding(DirectExchange retryExchange, Queue retryQueue) {
        return BindingBuilder.bind(retryQueue).to(retryExchange).with(RETRY_ROUTING_KEY);
    }

}
