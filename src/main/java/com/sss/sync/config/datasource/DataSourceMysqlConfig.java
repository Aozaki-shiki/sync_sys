package com.sss.sync.config.datasource;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

@Configuration
@MapperScan(
  basePackages = "com.sss.sync.infra.mapper.mysql",
  sqlSessionFactoryRef = "mysqlSqlSessionFactory"
)
@RequiredArgsConstructor
public class DataSourceMysqlConfig {

  private final MybatisPlusInterceptor mybatisPlusInterceptor;
  private final GlobalConfig globalConfig;

  @Bean
  @ConfigurationProperties("sss.datasource.mysql")
  public DataSourceProperties mysqlDataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean(name = "mysqlDataSource")
  public DataSource mysqlDataSource(@Qualifier("mysqlDataSourceProperties") DataSourceProperties props) {
    return props.initializeDataSourceBuilder().build();
  }

  @Bean(name = "mysqlSqlSessionFactory")
  public SqlSessionFactory mysqlSqlSessionFactory(@Qualifier("mysqlDataSource") DataSource ds) throws Exception {
    MybatisSqlSessionFactoryBean bean = new MybatisSqlSessionFactoryBean();
    bean.setDataSource(ds);

    MybatisConfiguration cfg = new MybatisConfiguration();
    cfg.setMapUnderscoreToCamelCase(true);
    bean.setConfiguration(cfg);

    bean.setPlugins(mybatisPlusInterceptor);
    bean.setGlobalConfig(globalConfig);

    return bean.getObject();
  }

  @Bean(name = "mysqlSqlSessionTemplate")
  public SqlSessionTemplate mysqlSqlSessionTemplate(@Qualifier("mysqlSqlSessionFactory") SqlSessionFactory sf) {
    return new SqlSessionTemplate(sf);
  }

  @Bean(name = "mysqlTxManager")
  public DataSourceTransactionManager mysqlTxManager(@Qualifier("mysqlDataSource") DataSource ds) {
    return new DataSourceTransactionManager(ds);
  }
}