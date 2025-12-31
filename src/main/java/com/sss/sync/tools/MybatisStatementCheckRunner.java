package com.sss.sync.tools;

import lombok.RequiredArgsConstructor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MybatisStatementCheckRunner implements CommandLineRunner {

  private final SqlSessionFactory mysqlSqlSessionFactory;

  @Override
  public void run(String... args) {
    var cfg = mysqlSqlSessionFactory.getConfiguration();
    String msId = "com.sss.sync.infra.mapper.mysql.MysqlProductMapper.selectById";
    System.out.println("[CHECK] hasMappedStatement(" + msId + ") = " + cfg.hasStatement(msId));
  }
}