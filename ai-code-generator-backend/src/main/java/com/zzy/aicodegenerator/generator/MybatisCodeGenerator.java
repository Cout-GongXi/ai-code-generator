package com.zzy.aicodegenerator.generator;

import cn.hutool.core.lang.Dict;
import cn.hutool.setting.yaml.YamlUtil;
import com.mybatisflex.codegen.Generator;
import com.mybatisflex.codegen.config.ColumnConfig;
import com.mybatisflex.codegen.config.GlobalConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.util.Map;

public class MybatisCodeGenerator {

    // 可以在这里设置要生成哪些表，或者在全局配置中设置 setGenerateTable，未设置时生成所有表
    private static final String[] TABLE_NAMES = {"user"};

    public static void main(String[] args) {
        // 从配置文件中读取配置项，或者直接在代码中设置配置项
        Dict dict = YamlUtil.loadByPath("application-local.yaml");
        Map<String, Object> dataSourceConfig = dict.getByPath("spring.datasource");
        String url = String.valueOf(dataSourceConfig.get("url"));
        String username = String.valueOf(dataSourceConfig.get("username"));
        String password = String.valueOf(dataSourceConfig.get("password"));
        //配置数据源
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        //创建配置内容
        GlobalConfig globalConfig = createGlobalConfig();

        //通过 datasource 和 globalConfig 创建代码生成器
        Generator generator = new Generator(dataSource, globalConfig);

        //生成代码
        generator.generate();
    }

    public static GlobalConfig createGlobalConfig() {
        //创建配置内容
        GlobalConfig globalConfig = new GlobalConfig();

        //设置根包 （生成的代码会放在这个包下，可以根据需要修改）
        globalConfig.setBasePackage("com.zzy.aicodegenerator.generator.result");

        //设置表前缀和只生成哪些表，setGenerateTable 未配置时，生成所有表
        globalConfig.setTablePrefix("");
        globalConfig.getStrategyConfig().
                setGenerateTable(TABLE_NAMES).
                setLogicDeleteColumn("isDeleted");

        //设置生成 entity 并启用 Lombok
        globalConfig.setEntityGenerateEnable(true);
        globalConfig.setEntityWithLombok(true);
        //设置项目的JDK版本，项目的JDK为14及以上时建议设置该项，小于14则可以不设置
        globalConfig.setEntityJdkVersion(21);

        //设置生成 mapper
        globalConfig.setMapperGenerateEnable(true);
        globalConfig.setMapperXmlGenerateEnable(true);

        // 设置生成 service 和 serviceImpl
        globalConfig.setServiceGenerateEnable(true);
        globalConfig.setServiceImplGenerateEnable(true);

        // 设置生成 controller
        globalConfig.setControllerGenerateEnable(true);

        // 设置生成注释
        globalConfig.getJavadocConfig().setAuthor("zzy").setSince("");
        return globalConfig;
    }
}