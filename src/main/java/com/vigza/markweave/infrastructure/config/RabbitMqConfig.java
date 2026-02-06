package com.vigza.markweave.infrastructure.config;

import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMqConfig {
    public static final String COLLABORATION_EXCHANGE = "mw.collaboration.boardcast";
    public static final String RETRY_EXCHANGE = "mw.retry.exchange";
    public static final String RETRY_QUEUE = "mw.retry.queue";
    public static final String RETRY_ROUTING_KEY = "retry.key";
    public static final String RETRY_DELAY_EXCHANGE = "mw.retry.delay.exchange";
    public static final String RETRY_DELAY_QUEUE = "mw.retry.delay.queue";
    public static final String RETRY_DELAY_ROUTING_KEY = "retry.delay.key";

    // 1. 定义交换机
    @Bean
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange(COLLABORATION_EXCHANGE);
    }

    // 2. 定义匿名、自动删除的临时队列（每台服务器实例独有）
    @Bean
    public Queue autoDeleteQueue() {
        return new AnonymousQueue();
    }

    // 3. 绑定队列到交换机
    @Bean
    public Binding binding(FanoutExchange fanoutExchange,Queue autoDeleteQueue){
        return BindingBuilder.bind(autoDeleteQueue).to(fanoutExchange);
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
    public Binding retryBinding(DirectExchange retryExchange, Queue retryQueue) {
        return BindingBuilder.bind(retryQueue)
                .to(retryExchange)
                .with(RETRY_ROUTING_KEY);
    }

    @Bean
    public DirectExchange retryDelayExchange() {
        return new DirectExchange(RETRY_DELAY_EXCHANGE);
    }

    @Bean
    public Queue retryDelayQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", RETRY_EXCHANGE);
        args.put("x-dead-letter-routing-key", RETRY_ROUTING_KEY);
        return new Queue(RETRY_DELAY_QUEUE, true, false, false, args);
    }

    @Bean
    public Binding retryDelayBinding(DirectExchange retryDelayExchange, Queue retryDelayQueue) {
        return BindingBuilder.bind(retryDelayQueue)
                .to(retryDelayExchange)
                .with(RETRY_DELAY_ROUTING_KEY);
    }

}
