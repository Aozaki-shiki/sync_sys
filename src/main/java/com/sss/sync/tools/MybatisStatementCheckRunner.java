package com.sss.sync.tools;

import lombok.RequiredArgsConstructor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MybatisStatementCheckRunner implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(MybatisStatementCheckRunner.class);
  
  private final SqlSessionFactory mysqlSqlSessionFactory;

  @Override
  public void run(String... args) {
    // Only run when debug mode is explicitly enabled via system property
    if (!"true".equalsIgnoreCase(System.getProperty("mybatis.debug.check"))) {
      return;
    }
    
    var cfg = mysqlSqlSessionFactory.getConfiguration();
    String msId = "com.sss.sync.infra.mapper.mysql.MysqlProductMapper.selectById";
    log.debug("[CHECK] hasMappedStatement({}) = {}", msId, cfg.hasStatement(msId));
  }
}