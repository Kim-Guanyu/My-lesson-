package com.mdkj.util;
import com.mybatisflex.codegen.Generator;
import com.mybatisflex.codegen.config.EntityConfig;
import com.mybatisflex.codegen.config.GlobalConfig;
import com.mybatisflex.codegen.dialect.JdbcTypeMapping;
import com.zaxxer.hikari.HikariDataSource;

import java.math.BigDecimal;
import java.util.Set;


public class GenUtil {
    /** MySQL主机地址 */
    private static final String MYSQL_HOST = "192.168.211.132:3306";
    /** 数据库账号 */
    private static final String MYSQL_USERNAME = "root";
    /** 数据库密码 */
    private static final String MYSQL_PASSWORD = "123456";
    /** 作者 */
    private static final String AUTHOR = "Kim-Guanyu";
    /** 根包 */
    private static final String BASE_PACKAGE = "com.mdkj";

    /**
     * 获取 MyBatisFlex 生成器对象（生成指定表）
     *
     * @param dbName     数据库名称
     * @param moduleName 子模块名: 指定生成到哪个子项目中
     * @param tableNames 根据数据库种的哪些表进行生成
     * @return MyBatisFlex 生成器对象
     */
    public static Generator getGenerator(String dbName, String moduleName, Set<String> tableNames) {
        // 将 BigDecimal 类型转为 Double 类型
        JdbcTypeMapping.registerMapping(BigDecimal.class, Double.class);

        // 配置 HikariDataSource 对象（Hikari 数据源）
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setUsername(MYSQL_USERNAME);
        dataSource.setPassword(MYSQL_PASSWORD);
        dataSource.setJdbcUrl("jdbc:mysql://" + MYSQL_HOST + "/" + dbName + "?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8");

        // 配置 GlobalConfig 对象（MyBatisFlex 生成器配置）
        GlobalConfig globalConfig = new GlobalConfig();
        // 设置逻辑删除字段
        globalConfig.setLogicDeleteColumn("deleted");
        // 设置乐观锁字段
        globalConfig.setVersionColumn("version");
        // 注释配置: 设置作者但不设置日期
        globalConfig.getJavadocConfig().setAuthor(AUTHOR).setSince("v1.0.0");
        // 包配置: src目录，mapper配置文件目录，根包
        globalConfig.getPackageConfig().setSourceDir(moduleName + "/src/main/java").setBasePackage(BASE_PACKAGE);
        // 策略配置: 指定数据库
        globalConfig.getStrategyConfig().setGenerateSchema(dbName);
        // 生成实体类：Lombok注解 + Swagger注解 + JDK版本 + 重新生成时覆盖
        globalConfig.enableEntity()
                .setWithLombok(true)
                .setWithSwagger(true).setSwaggerVersion(EntityConfig.SwaggerVersion.DOC)
                .setJdkVersion(17)
                .setOverwriteEnable(true);
        // 生成实体类辅助类：重新生成时覆盖
        globalConfig.enableTableDef().setOverwriteEnable(true);
        // 生成数据层代码（接口）：重新生成时覆盖
        globalConfig.enableMapper().setOverwriteEnable(true);
        // 生成业务层代码（接口）：重新生成时覆盖
        globalConfig.enableService().setOverwriteEnable(true);
        // 生成业务层代码（实现类）：重新生成时覆盖
        globalConfig.enableServiceImpl().setOverwriteEnable(true);
        // 生成控制层代码：使用 /api/v1/ 前缀 + 重新生成时覆盖
        globalConfig.enableController().setRequestMappingPrefix("/api/v1").setOverwriteEnable(true);
        // 指定生成哪些表
        if (null != tableNames) {
            globalConfig.setGenerateTables(tableNames);
        }
        // 创建生成器对象并返回
        return new Generator(dataSource, globalConfig);
    }

    /**
     * 获取 MyBatisFlex 生成器对象（生成全部表）
     *
     * @param dbName     数据库名称
     * @param moduleName 子模块名: 指定生成到哪个子项目中
     * @return MyBatisFlex 生成器对象
     */
    public static Generator getGenerator(String dbName, String moduleName) {
        return getGenerator(dbName, moduleName, null);
    }
}
