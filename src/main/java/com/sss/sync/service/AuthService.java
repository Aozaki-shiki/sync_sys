package com.sss.sync.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sss.sync.common.exception.BizException;
import com.sss.sync.config.security.JwtUtil;
import com.sss.sync.domain.entity.UserInfo;
import com.sss.sync.infra.mapper.mysql.MysqlUserMapper;
import com.sss.sync.infra.mapper.postgres.PostgresUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final MysqlUserMapper mysqlUserMapper;
  private final PostgresUserMapper postgresUserMapper;
  private final JwtUtil jwtUtil;

  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

  public String login(String username, String password) {
    UserInfo u = mysqlUserMapper.selectOne(new LambdaQueryWrapper<UserInfo>()
      .eq(UserInfo::getUsername, username)
      .eq(UserInfo::getDeleted, false)
      .last("LIMIT 1"));

    if (u == null) {
      // postgres 没有 LIMIT 语法差异：MyBatis-Plus last 直接拼接，PG也支持 LIMIT
      u = postgresUserMapper.selectOne(new LambdaQueryWrapper<UserInfo>()
        .eq(UserInfo::getUsername, username)
        .eq(UserInfo::getDeleted, false)
        .last("LIMIT 1"));
    }

    if (u == null) {
      throw BizException.of(401, "USER_NOT_FOUND");
    }

    if (!encoder.matches(password, u.getPasswordHash())) {
      throw BizException.of(401, "INVALID_PASSWORD");
    }

    return jwtUtil.generateAccessToken(u.getUserId(), u.getUsername(), u.getRole());
  }

  public UserInfo findUser(String username) {
    UserInfo u = mysqlUserMapper.selectOne(new LambdaQueryWrapper<UserInfo>()
      .eq(UserInfo::getUsername, username)
      .eq(UserInfo::getDeleted, false)
      .last("LIMIT 1"));
    if (u != null) return u;

    return postgresUserMapper.selectOne(new LambdaQueryWrapper<UserInfo>()
      .eq(UserInfo::getUsername, username)
      .eq(UserInfo::getDeleted, false)
      .last("LIMIT 1"));
  }
}