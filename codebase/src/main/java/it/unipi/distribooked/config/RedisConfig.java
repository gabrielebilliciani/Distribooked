package it.unipi.distribooked.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.util.Pool;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
// Specifies that this class is a configuration class for Spring
@Configuration
@EnableRedisRepositories(basePackages = "it.unipi.distribooked.repository.redis")
public class RedisConfig {

    @Autowired
    private Environment environment;

    /**
     * Configuration for local redis
     */
    // Injects the Redis host from application properties, defaulting to "localhost" if not provided
    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    // Injects the Redis port from application properties, defaulting to 6379 if not provided
    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    /**
     * Config for Sentinel
     */
    @Value("${spring.data.redis.sentinel.master:mymaster}")
    private String master;

    @Value("${spring.data.redis.sentinel.nodes:}")
    private String sentinelNodes;

    // Injects the Redis password from application properties, defaulting to an empty string if not provided
    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    // Injects the Redis database index from application properties, defaulting to 0 if not provided
    @Value("${spring.data.redis.database:0}")
    private int redisDatabase;
    /**
     * Creates a Bean for the Redis connection pool configuration.
     * Configures the connection pool for Redis clients using Jedis.
     * - Sets max total connections, idle limits, timeout, and other properties.
     *
     * @return a configured GenericObjectPoolConfig for Jedis instances
     */
    @Bean
    public JedisPoolConfig jedisPoolConfig() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128); // Maximum number of connections in the pool
        poolConfig.setMaxIdle(128); // Maximum number of idle connections
        poolConfig.setMinIdle(16);  // Minimum number of idle connections
        poolConfig.setBlockWhenExhausted(true); // Wait if no connections are available
        poolConfig.setMaxWait(Duration.ofSeconds(1)); // Maximum wait time for a connection
        poolConfig.setJmxEnabled(true); // Enable JMX for monitoring
        poolConfig.setTestOnBorrow(true); // Validate objects before borrowing
        poolConfig.setTestOnReturn(true); // Validate objects before returning to the pool
        poolConfig.setTestWhileIdle(true); // Validate idle objects periodically
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(30)); // Time between eviction checks
        poolConfig.setNumTestsPerEvictionRun(3); // Number of objects tested per eviction run
        return poolConfig;
    }

    @Bean
    public Pool<Jedis> jedisPool() {
        if (environment.acceptsProfiles(Profiles.of("cluster"))) {
            Set<String> sentinels = Arrays.stream(sentinelNodes.split(","))
                    .collect(Collectors.toSet());

            return new JedisSentinelPool(
                    master,
                    sentinels,
                    jedisPoolConfig(),
                    2000,
                    redisPassword.isEmpty() ? null : redisPassword,
                    redisDatabase
            );
        } else if (environment.acceptsProfiles(Profiles.of("test"))){
            return new JedisPool(
                    jedisPoolConfig(),
                    redisHost,
                    redisPort,
                    2000,
                    redisPassword.isEmpty() ? null : redisPassword,
                    1
            );
        }else {
            return new JedisPool(
                    jedisPoolConfig(),
                    redisHost,
                    redisPort,
                    2000,
                    redisPassword.isEmpty() ? null : redisPassword,
                    redisDatabase
            );
        }
    }

    /** LOCAL REDIS CONNECTION FACTORY
     * Creates a Bean for the Redis connection factory.
     * - Configures Redis connection parameters like host, port, password, and database.
     * - Associates the connection pool configuration with the Redis connection factory.
     */
    @Bean
    @Profile("local")
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisHost);
        redisConfig.setPort(redisPort);
        redisConfig.setPassword(redisPassword);
        redisConfig.setDatabase(redisDatabase);

        JedisClientConfiguration clientConfig = JedisClientConfiguration.builder()
                .connectTimeout(Duration.ofSeconds(2))
                .readTimeout(Duration.ofSeconds(2))
                .usePooling()
                .poolConfig(jedisPoolConfig())
                .build();

        return new JedisConnectionFactory(redisConfig, clientConfig);
    }

    @Bean
    @Profile("test")
    public RedisConnectionFactory testRedisConnectionFactory() {

        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisHost);
        redisConfig.setPort(redisPort);
        redisConfig.setPassword(redisPassword);
        redisConfig.setDatabase(1);  // database 1 per i test

        log.info("Using Redis test database " + redisConfig.getDatabase());

        JedisClientConfiguration clientConfig = JedisClientConfiguration.builder()
                .connectTimeout(Duration.ofSeconds(2))
                .readTimeout(Duration.ofSeconds(2))
                .usePooling()
                .poolConfig(jedisPoolConfig())
                .build();

        return new JedisConnectionFactory(redisConfig, clientConfig);
    }

    /** SENTINEL REDIS CONNECTION FACTORY
     * creates a Bean for the Redis connection factory.
     */
//    @Bean
//    @Profile("cluster")
//    public RedisConnectionFactory sentinelRedisConnectionFactory() {
//        RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration();
//        sentinelConfig.master(master);
//
//        String[] nodes = sentinelNodes.split(",");
//        for (String node : nodes) {
//            String[] parts = node.split(":");
//            sentinelConfig.sentinel(parts[0], Integer.parseInt(parts[1]));
//        }
//
//        if (!redisPassword.isEmpty()) {
//            sentinelConfig.setPassword(redisPassword);
//        }
//        sentinelConfig.setDatabase(redisDatabase);
//
//        JedisClientConfiguration clientConfig = JedisClientConfiguration.builder()
//                .connectTimeout(Duration.ofSeconds(2))
//                .readTimeout(Duration.ofSeconds(2))
//                .usePooling()
//                .poolConfig(jedisPoolConfig())
//                .build();
//
//        return new JedisConnectionFactory(sentinelConfig, clientConfig);
//    }

    @Bean
    @Profile("cluster")
    public RedisConnectionFactory sentinelRedisConnectionFactory() {
        RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration()
                .master(master);

        Set<String> sentinels = Arrays.stream(sentinelNodes.split(","))
                .collect(Collectors.toSet());

        sentinels.forEach(node -> {
            String[] parts = node.split(":");
            sentinelConfig.sentinel(parts[0], Integer.parseInt(parts[1]));
        });

        if (!redisPassword.isEmpty()) {
            sentinelConfig.setPassword(redisPassword);
        }
        sentinelConfig.setDatabase(redisDatabase);

        JedisClientConfiguration clientConfig = JedisClientConfiguration.builder()
                .connectTimeout(Duration.ofSeconds(5))
                .readTimeout(Duration.ofSeconds(5))
                .usePooling()
                .poolConfig(jedisPoolConfig())
                .build();

        JedisConnectionFactory factory = new JedisConnectionFactory(sentinelConfig, clientConfig);
        factory.afterPropertiesSet();
        return factory;
    }

    /**
     * Creates a Bean for the RedisTemplate.
     * - Configures the template to use the Redis connection factory.
     * - Sets serializers for keys, values, hash keys, and hash values to handle data serialization and deserialization.
     *
     * @param connectionFactory The RedisConnectionFactory used for creating connections
     * @return a configured RedisTemplate for Redis operations
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Main keys (e.g., "Library:123:Books")
        template.setKeySerializer(new StringRedisSerializer());

        // Hash keys (e.g., "Book:456")
        template.setHashKeySerializer(new StringRedisSerializer());

        // Hash values (e.g., simple values like 10)
        template.setHashValueSerializer(new StringRedisSerializer());

        // Value serializer for non-hash data (if needed)
        template.setValueSerializer(new StringRedisSerializer());

        // Disable the default serializer
        template.setEnableDefaultSerializer(false);

        // Enable transaction support
        template.setEnableTransactionSupport(true);

        template.afterPropertiesSet();
        return template;
    }

//    // LOCAL REDIS BEAN
//    @Bean
//    @Profile("local")
//    public Jedis jedis() {
//        return new Jedis(redisHost, redisPort);
//    }
//
//    // SENTINEL JEIDS BEAN
//    @Bean
//    @Profile("cluster")
//    public Jedis sentinelJedis() {
//        Set<String> sentinels = Arrays.stream(sentinelNodes.split(","))
//                .collect(Collectors.toSet());
//
//        JedisSentinelPool sentinelPool = new JedisSentinelPool(
//                master,
//                sentinels,
//                jedisPoolConfig(),
//                2000,
//                //redisPassword,
//                null,
//                redisDatabase
//        );
//
//        return sentinelPool.getResource();
//    }

}
