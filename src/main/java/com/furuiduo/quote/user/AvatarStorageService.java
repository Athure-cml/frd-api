package com.furuiduo.quote.user;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.sys.entity.SysUser;

@Service
public class AvatarStorageService {

  private static final long MAX_BYTES = 2L * 1024 * 1024;
  private static final Set<String> ALLOWED_TYPES =
      Set.of("image/jpeg", "image/png", "image/gif", "image/webp");

  private final Path avatarDir;

  public AvatarStorageService(@Value("${quote.upload-dir:./uploads}") String uploadDir) {
    this.avatarDir = Paths.get(uploadDir, "avatars").toAbsolutePath().normalize();
  }

  public String store(SysUser user, MultipartFile file) throws IOException {
    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择头像图片");
    }
    if (file.getSize() > MAX_BYTES) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "头像大小不能超过 2MB");
    }

    String contentType = file.getContentType();
    if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "仅支持 JPG、PNG、GIF、WEBP 格式");
    }

    String extension = extensionFor(contentType);
    String filename = "user-" + user.getId() + "-" + System.currentTimeMillis() + extension;
    Files.createDirectories(avatarDir);
    Path target = avatarDir.resolve(filename);
    Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
    return "/uploads/avatars/" + filename;
  }

  private static String extensionFor(String contentType) {
    return switch (contentType) {
      case "image/png" -> ".png";
      case "image/gif" -> ".gif";
      case "image/webp" -> ".webp";
      default -> ".jpg";
    };
  }
}
