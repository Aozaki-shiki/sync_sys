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
  basePackages = "com.sss.sync.infra.mapper.postgres",
  sqlSessionFactoryRef = "postgresSqlSessionFactory"
)
@RequiredArgsConstructor
public class DataSourcePostgresConfig {

  private final MybatisPlusInterceptor mybatisPlusInterceptor;
  private final GlobalConfig globalConfig;

  @Bean
  @ConfigurationProperties("sss.datasource.postgres")
  public DataSourceProperties postgresDataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean(name = "postgresDataSource")
  public DataSource postgresDataSource(@Qualifier("postgresDataSourceProperties") DataSourceProperties props) {
    return props.initializeDataSourceBuilder().build();
  }

  @Bean(name = "postgresSqlSessionFactory")
  public SqlSessionFactory postgresSqlSessionFactory(@Qualifier("postgresDataSource") DataSource ds) throws Exception {
    MybatisSqlSessionFactoryBean bean = new MybatisSqlSessionFactoryBean();
    bean.setDataSource(ds);

    MybatisConfiguration cfg = new MybatisConfiguration();
    cfg.setMapUnderscoreToCamelCase(true);
    bean.setConfiguration(cfg);

    bean.setPlugins(mybatisPlusInterceptor);
    bean.setGlobalConfig(globalConfig);
    bean.setTypeAliasesPackage("com.sss.sync.domain.entity");

    return bean.getObject();
  }

  @Bean(name = "postgresSqlSessionTemplate")
  public SqlSessionTemplate postgresSqlSessionTemplate(@Qualifier("postgresSqlSessionFactory") SqlSessionFactory sf) {
    return new SqlSessionTemplate(sf);
  }

  @Bean(name = "postgresTxManager")
  public DataSourceTransactionManager postgresTxManager(@Qualifier("postgresDataSource") DataSource ds) {
    return new DataSourceTransactionManager(ds);
  }
}