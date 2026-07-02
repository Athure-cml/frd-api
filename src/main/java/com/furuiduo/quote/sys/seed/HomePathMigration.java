package com.furuiduo.quote.sys.seed;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.furuiduo.quote.sys.repository.SysUserRepository;

/** 将历史默认首页从报价分析迁移到工作台。 */
@Component
@Order(0)
public class HomePathMigration implements ApplicationRunner {

  private final SysUserRepository userRepository;

  public HomePathMigration(SysUserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    userRepository
        .findAll()
        .forEach(
            user -> {
              if ("/analytics".equals(user.getHomePath())) {
                user.setHomePath("/workspace");
                userRepository.save(user);
              }
            });
  }
}
