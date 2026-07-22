package com.bingbaihanji.luca.plugin.code.generator.util;

/**
 * 命名转换工具类：提供数据库命名与 Java 命名之间的常用转换方法。
 * 所有方法均为静态工具方法，无状态，线程安全。
 */
public class NamingUtils {

    /**
     * 下划线转小驼峰: user_name -> userName
     * 遍历每个字符，遇到下划线则将下一个字符转大写，其余字符转小写。
     */
    public static String toCamelCase(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        StringBuilder sb = new StringBuilder();
        boolean upperCase = false;   // 标记是否需要将下一个字符大写
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '_') {
                upperCase = true;    // 遇到下划线，设置标志位
            } else if (upperCase) {
                sb.append(Character.toUpperCase(c));   // 下划线后的字符转大写
                upperCase = false;
            } else {
                sb.append(Character.toLowerCase(c));   // 普通字符转小写
            }
        }
        return sb.toString();
    }

    /**
     * 下划线转大驼峰(PascalCase): user_name -> UserName
     * 先转小驼峰，再将首字母大写。
     */
    public static String toPascalCase(String name) {
        String camel = toCamelCase(name);
        if (camel == null || camel.isEmpty()) {
            return camel;
        }
        return Character.toUpperCase(camel.charAt(0)) + camel.substring(1);
    }

    /**
     * 去除表名前缀: stripTablePrefix("t_user", "t_") -> "user"
     * prefix 为空或 null 时直接返回原名；匹配时不区分大小写。
     */
    public static String stripTablePrefix(String tableName, String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return tableName;
        }
        if (tableName != null && tableName.toLowerCase().startsWith(prefix.toLowerCase())) {
            return tableName.substring(prefix.length());
        }
        return tableName;
    }

    /**
     * 类名转 URL 前缀: UserInfo -> userInfo（首字母小写）
     * 用于生成 Controller 的 @RequestMapping 前缀或变量名。
     */
    public static String toLowerFirst(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }
}
