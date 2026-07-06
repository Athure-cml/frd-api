package com.furuiduo.quote.masterdata.seed;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.furuiduo.quote.masterdata.service.UsZipImportService;

/**
 * 启动时从 GeoNames 美国邮编文件（tab 分隔，见项目根目录 readme.txt）导入州/城市/邮编主数据。
 */
@Component
@Order(104)
public class UsZipDataSeeder implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(UsZipDataSeeder.class);

  private final UsZipImportService importService;

  @Value("${quote.masterdata.us-zip.enabled:true}")
  private boolean enabled;

  @Value("${quote.masterdata.us-zip.file-path:./US.txt}")
  private String filePath;

  @Value("${quote.masterdata.us-zip.skip-if-exists:true}")
  private boolean skipIfExists;

  public UsZipDataSeeder(UsZipImportService importService) {
    this.importService = importService;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!enabled) {
      return;
    }
    if (skipIfExists && importService.hasZipData()) {
      log.info("美国邮编主数据已存在，跳过 GeoNames 导入");
      return;
    }

    Path path = Path.of(filePath).toAbsolutePath().normalize();
    if (!Files.isRegularFile(path)) {
      log.warn("未找到美国邮编数据文件：{}，可通过后台导入 US.txt", path);
      return;
    }

    try {
      importService.importFromPath(path);
    } catch (IOException ex) {
      throw new IllegalStateException("导入美国邮编数据失败：" + path, ex);
    }
  }
}
