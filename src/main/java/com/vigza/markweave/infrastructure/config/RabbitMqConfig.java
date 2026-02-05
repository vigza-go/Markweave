package com.vigza.markweave.infrastructure.config;

import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {
    public static final String EXCHANGE_NAME = "mw.collaboration.boardcast";

    // 1. 定义交换机
    @Bean
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange(EXCHANGE_NAME);
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

}
