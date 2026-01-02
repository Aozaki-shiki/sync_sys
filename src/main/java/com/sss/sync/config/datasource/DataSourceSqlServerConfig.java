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

@Configuration
@MapperScan(
        basePackages = {
                "com.sss.sync.infra.mapper.read",
                "com.sss.sync.infra.mapper.sqlserver"
        },
        sqlSessionFactoryRef = "readSqlSessionFactory"
)
@RequiredArgsConstructor
public class DataSourceSqlServerConfig {

  private final MybatisPlusInterceptor mybatisPlusInterceptor;
  private final GlobalConfig globalConfig;

  @Bean
  @ConfigurationProperties("sss.datasource.sqlserver")
  public DataSourceProperties readDataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean(name = "readDataSource")
  public DataSource readDataSource(@Qualifier("readDataSourceProperties") DataSourceProperties props) {
    return props.initializeDataSourceBuilder().build();
  }

  @Bean(name = "readSqlSessionFactory")
  public SqlSessionFactory readSqlSessionFactory(@Qualifier("readDataSource") DataSource ds) throws Exception {
    MybatisSqlSessionFactoryBean bean = new MybatisSqlSessionFactoryBean();
    bean.setDataSource(ds);

    MybatisConfiguration cfg = new MybatisConfiguration();
    cfg.setMapUnderscoreToCamelCase(true);
    bean.setConfiguration(cfg);

    bean.setPlugins(mybatisPlusInterceptor);
    bean.setGlobalConfig(globalConfig);

    return bean.getObject();
  }

  @Bean(name = "readSqlSessionTemplate")
  public SqlSessionTemplate readSqlSessionTemplate(@Qualifier("readSqlSessionFactory") SqlSessionFactory sf) {
    return new SqlSessionTemplate(sf);
  }

  @Bean(name = "readTxManager")
  public org.springframework.jdbc.datasource.DataSourceTransactionManager readTxManager(@Qualifier("readDataSource") DataSource ds) {
    return new org.springframework.jdbc.datasource.DataSourceTransactionManager(ds);
  }
}