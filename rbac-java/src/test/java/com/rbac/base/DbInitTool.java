package com.rbac.base;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class DbInitTool {
    public static void main(String[] args) {
        String rootUrl = "jdbc:mysql://localhost:3306/?serverTimezone=GMT%2B8&allowPublicKeyRetrieval=true&useSSL=false";
        String rootUser = System.getenv().getOrDefault("DB_ROOT_USER", "root");
        String rootPass = System.getenv().getOrDefault("DB_ROOT_PASSWORD", "");
        
        String geminiUser = "gemini_cli";
        String geminiPass = System.getenv().getOrDefault("DB_APP_PASSWORD", "");

        try (Connection conn = DriverManager.getConnection(rootUrl, rootUser, rootPass);
             Statement stmt = conn.createStatement()) {
            
            System.out.println("1. 正在创建 gemini_cli 角色并授权...");
            stmt.execute("CREATE USER IF NOT EXISTS '" + geminiUser + "'@'localhost' IDENTIFIED BY '" + geminiPass + "'");
            stmt.execute("GRANT ALL PRIVILEGES ON tx_ticket.* TO '" + geminiUser + "'@'localhost'");
            stmt.execute("FLUSH PRIVILEGES");
            System.out.println("角色授权成功！");

            System.out.println("2. 正在读取并执行 init.sql...");
            // 读取 init.sql 文件（注意路径）
            String sqlContent = new String(Files.readAllBytes(Paths.get("../init.sql")));
            
            // 简单的 SQL 分隔执行逻辑（按分号分隔，排除注释和空行）
            String[] queries = sqlContent.split(";");
            for (String query : queries) {
                String trimmedQuery = query.trim();
                if (!trimmedQuery.isEmpty() && !trimmedQuery.startsWith("/*")) {
                    stmt.execute(trimmedQuery);
                }
            }
            System.out.println("数据库表结构同步成功！");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
