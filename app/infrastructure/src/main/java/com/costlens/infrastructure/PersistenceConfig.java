package com.costlens.infrastructure;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 持久化装配。@MapperScan 放在基础设施层，避免启动类承担技术细节，
 * 也让 Web 切片测试不会误加载 Mapper。
 */
@Configuration
@MapperScan("com.costlens.infrastructure")
public class PersistenceConfig {
}