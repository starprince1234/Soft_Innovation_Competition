package com.jigu.cloud.common.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 日期时间工具类。
 * <p>
 * 统一使用 {@code java.time} API，禁止使用 {@code java.util.Date}。
 */
public final class DateUtils {

    /** 标准日期时间格式 */
    public static final String PATTERN_DATETIME = "yyyy-MM-dd HH:mm:ss";
    public static final DateTimeFormatter FORMATTER_DATETIME = DateTimeFormatter.ofPattern(PATTERN_DATETIME);

    /** 标准日期格式 */
    public static final String PATTERN_DATE = "yyyy-MM-dd";
    public static final DateTimeFormatter FORMATTER_DATE = DateTimeFormatter.ofPattern(PATTERN_DATE);

    /** 默认时区 */
    public static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Shanghai");

    private DateUtils() {
        // 工具类禁止实例化
    }

    /** 返回当前时间（上海时区） */
    public static LocalDateTime now() {
        return LocalDateTime.now(DEFAULT_ZONE);
    }

    /** 格式化为标准日期时间字符串 */
    public static String format(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(FORMATTER_DATETIME);
    }

    /** 解析标准日期时间字符串 */
    public static LocalDateTime parse(String text) {
        return text == null ? null : LocalDateTime.parse(text, FORMATTER_DATETIME);
    }
}
