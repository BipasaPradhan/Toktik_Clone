package io.muzoo.scalable.web_socket.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.muzoo.scalable.web_socket.listener.CommentMessageListener;
import io.muzoo.scalable.web_socket.listener.LikeCountMessageListener;
import io.muzoo.scalable.web_socket.listener.ViewCountMessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class RedisConfig {
    private static final Logger logger = LoggerFactory.getLogger(RedisConfig.class);

    @Bean
    public RedisMessageListenerContainer redisContainer(RedisConnectionFactory connectionFactory,
                                                        MessageListenerAdapter viewCountListenerAdapter,
                                                        MessageListenerAdapter likeCountListenerAdapter,
                                                        MessageListenerAdapter commentListenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(viewCountListenerAdapter, new PatternTopic("view:count"));
        container.addMessageListener(likeCountListenerAdapter, new PatternTopic("like:count"));
        container.addMessageListener(commentListenerAdapter, new PatternTopic("comment:new"));
        container.setErrorHandler(e -> logger.error("Error in Redis listener container: {}", e.getMessage(), e));
        return container;
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public MessageListenerAdapter viewCountListenerAdapter(ViewCountMessageListener viewListener) {
        return new MessageListenerAdapter(viewListener, "onMessage");
    }

    @Bean
    public MessageListenerAdapter likeCountListenerAdapter(LikeCountMessageListener likeListener) {
        return new MessageListenerAdapter(likeListener, "onMessage");
    }

    @Bean
    public MessageListenerAdapter commentListenerAdapter(CommentMessageListener commentListener) {
        return new MessageListenerAdapter(commentListener, "onMessage");
    }

}
