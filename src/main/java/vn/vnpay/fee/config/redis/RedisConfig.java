package vn.vnpay.fee.config.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import vn.vnpay.fee.annotation.CustomValue;
import vn.vnpay.fee.annotation.ValueInjector;

public class RedisConfig {
    private volatile static RedisConfig instance;
    private final Logger logger = LoggerFactory.getLogger(RedisConfig.class);
    private JedisPool jedisPool;
    @CustomValue("redis.host")
    private String redisHost;
    @CustomValue("redis.port")
    private int redisPort;
    @CustomValue("redis.username")
    private String redisUsername;
    @CustomValue("redis.password")
    private String redisPassword;
    @CustomValue("connection.pool.redis.maxTotal")
    private int redisMaxTotalConnPool;
    @CustomValue("connection.pool.redis.maxIdle")
    private int redisMinIdleConnPool;
    @CustomValue("connection.pool.redis.minIdle")
    private int redisMaxIdleConnPool;

    public static void initRedisConfig() throws IllegalAccessException {
        if (instance == null) {
            synchronized (RedisConfig.class) {
                if (instance == null) {
                    RedisConfig instanceTemp = new RedisConfig();
                    ValueInjector.injectValues(instanceTemp);
                    instanceTemp.registerJedisPool();
                    instance = instanceTemp;
                }
            }
        }
    }

    public static RedisConfig getInstance() {
        if (instance == null) {
            throw new IllegalStateException("JedisPool not initialized. Call init() before getInstance()");
        }
        return instance;
    }

    private void registerJedisPool() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(redisMaxTotalConnPool);
        poolConfig.setMinIdle(redisMinIdleConnPool);
        poolConfig.setMaxIdle(redisMaxIdleConnPool);
        jedisPool = new JedisPool(poolConfig, redisHost, redisPort);
        jedisPool.addObjects(redisMinIdleConnPool);
        logger.info("Create redis pool configuration with {} connections", jedisPool.getNumIdle());
    }

    public JedisPool getJedisPool() {
        return jedisPool;
    }

    public void returnConnection(Jedis jedis) {
        jedis.close();
    }

}
