package vn.vnpay.fee.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import vn.vnpay.fee.config.redis.RedisConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static vn.vnpay.fee.handle.RequestHandler.logIdThreadLocal;

public class CommonUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    static Logger logger = LoggerFactory.getLogger(CommonUtil.class);
    private CommonUtil() {
    }

    public static byte[] objectToBytes(Object object) throws IOException {
        return objectMapper.writeValueAsBytes(object);
    }

    public static <T> T bytesToObject(byte[] bytes, Class<T> clazz) throws IOException {
        return objectMapper.readValue(bytes, clazz);
    }

    public static <T> T bytesToObject(byte[] bytes, TypeReference<T> valueTypeRef) throws IOException {
        return objectMapper.readValue(bytes, valueTypeRef);
    }

    public static String objectToJson(Object object) throws JsonProcessingException {
        return objectMapper.writeValueAsString(object);
    }

    public static LocalDateTime convertLongToLocalDateTime(long timestamp) {
        Instant instant = Instant.ofEpochMilli(timestamp);
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    public static byte[] getBytesFromInputStream(InputStream is) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[0xFFFF];
        for (int len = is.read(buffer); len != -1; len = is.read(buffer)) {
            os.write(buffer, 0, len);
        }
        return os.toByteArray();
    }

    public static Map<String, List<String>> splitQuery(String query) throws UnsupportedEncodingException {
        Map<String, List<String>> queryParams = new LinkedHashMap<>();
        if (query != null) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
                if (!queryParams.containsKey(key)) {
                    queryParams.put(key, new LinkedList<>());
                }
                String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : null;
                queryParams.get(key).add(value);
            }
        }
        return queryParams;
    }


    public static synchronized String generateLogId() {
        Snowflake snowflake = SnowflakeSingleton.getInstanceForLogId(3, 5);
        return Long.toString(snowflake.nextId());
    }

    public static boolean isExpired(String requestTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        LocalDateTime parsedTime = LocalDateTime.parse(requestTime, formatter);
        LocalDateTime currentTime = LocalDateTime.now();
        long minutesDiff = java.time.Duration.between(parsedTime, currentTime).toMinutes();
        return Math.abs(minutesDiff) > 10;
    }

    public static boolean isExistKey(String requestId) {
        String logId = logIdThreadLocal.get();
        RedisConfig redisConfig = RedisConfig.getInstance();
        Jedis jedis = null;
        try {
            jedis = redisConfig.getJedisPool().getResource();
            return jedis.exists(requestId);
        } catch (JedisConnectionException e) {
            logger.error("[{}] - Error connecting to Redis", logId, e);
            return false;
        } catch (Exception e) {
            logger.error("[{}] - An occur error when check exist requestId on redis", logId, e);
            return false;
        } finally {
            if (jedis != null) {
                redisConfig.returnConnection(jedis);
            }
        }
    }

    public static boolean pushRedis(String requestId) {
        String logId = logIdThreadLocal.get();
        RedisConfig redisConfig = RedisConfig.getInstance();
        Jedis jedis = null;
        try {
            jedis = redisConfig.getJedisPool().getResource();
            String resultPushMessage = "";
            if (jedis != null) {
                long timeToLife = processTimeToLife();
                resultPushMessage = jedis.setex(requestId, timeToLife, "");
            }
            boolean isPushMessageSuccessfully = "OK".equalsIgnoreCase(resultPushMessage);
            if (isPushMessageSuccessfully) {
                logger.info("[{}] - Push message with requestId : {} to Redis successfully !", logId,
                        requestId);
            }
            return isPushMessageSuccessfully;
        } catch (JedisConnectionException e) {
            logger.error("[{}] - Error connecting to Redis", logId, e);
            return false;
        } catch (Exception e) {
            logger.error("[{}] - Error push message to redis", logId, e);
            return false;
        } finally {
            if (jedis != null) {
                redisConfig.returnConnection(jedis);
            }
        }
    }

    private static long processTimeToLife() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endOfDay = LocalDateTime.of(now.getYear(), now.getMonth(), now.getDayOfMonth(), 23, 59, 59);
        return endOfDay.toEpochSecond(ZoneOffset.UTC) - now.toEpochSecond(ZoneOffset.UTC);
    }

    public static String getNextId() {
        Snowflake snowflake = SnowflakeSingleton.getInstanceForDatabaseId(6, 5);
        return Long.toString(snowflake.nextId());
    }
}
