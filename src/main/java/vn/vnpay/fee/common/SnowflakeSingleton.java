package vn.vnpay.fee.common;

public class SnowflakeSingleton {
    private static volatile Snowflake instanceForLogId;
    private static volatile Snowflake instanceForDatabaseId;

    public static Snowflake getInstanceForLogId(long type, long nodeId) {
        if (instanceForLogId == null) {
            synchronized (SnowflakeSingleton.class) {
                if (instanceForLogId == null) {
                    instanceForLogId = new Snowflake(type, nodeId);
                }
            }
        }
        return instanceForLogId;
    }

    public static Snowflake getInstanceForDatabaseId(long type, long nodeId) {
        if (instanceForDatabaseId == null) {
            synchronized (SnowflakeSingleton.class) {
                if (instanceForDatabaseId == null) {
                    instanceForDatabaseId = new Snowflake(type, nodeId);
                }
            }
        }
        return instanceForDatabaseId;
    }
}
