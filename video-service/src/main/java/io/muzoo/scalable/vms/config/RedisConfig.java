package io.muzoo.scalable.vms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.muzoo.scalable.vms.Listener.LikeCountMessageListener;
import io.muzoo.scalable.vms.Listener.RedisMessageListener;
import io.muzoo.scalable.vms.Listener.ViewCountMessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.listener.PatternTopic;

@Configuration
public class RedisConfig {
    private static final Logger logger = LoggerFactory.getLogger(RedisConfig.class);

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
        return template;
    }

    @Bean
    public RedisMessageListenerContainer redisContainer(RedisConnectionFactory connectionFactory,
                                                        MessageListenerAdapter videoListenerAdapter,
                                                        MessageListenerAdapter viewCountListenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(videoListenerAdapter, new PatternTopic("video:processed"));
        container.addMessageListener(viewCountListenerAdapter, new PatternTopic("view:count"));
        container.setErrorHandler(e -> logger.error("Error in Redis listener container: {}", e.getMessage(), e));
        return container;
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public MessageListenerAdapter videoListenerAdapter(RedisMessageListener videoListener) {
        return new MessageListenerAdapter(videoListener, "onMessage");
    }

    @Bean
    public MessageListenerAdapter viewCountListenerAdapter(ViewCountMessageListener viewListener) {
        return new MessageListenerAdapter(viewListener, "onMessage");
    }

    @Bean
    public MessageListenerAdapter likeCountListenerAdapter(LikeCountMessageListener likeListener) { // Add this
        return new MessageListenerAdapter(likeListener, "onMessage");
    }
}
