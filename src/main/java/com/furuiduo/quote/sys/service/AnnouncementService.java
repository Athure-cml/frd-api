package com.furuiduo.quote.sys.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.sys.PermissionCodes;
import com.furuiduo.quote.sys.dto.AnnouncementResponse;
import com.furuiduo.quote.sys.dto.AnnouncementSaveAction;
import com.furuiduo.quote.sys.dto.AnnouncementSaveRequest;
import com.furuiduo.quote.sys.dto.AnnouncementTickerResponse;
import com.furuiduo.quote.sys.entity.AnnouncementDisplayType;
import com.furuiduo.quote.sys.entity.AnnouncementStatus;
import com.furuiduo.quote.sys.entity.SysAnnouncement;
import com.furuiduo.quote.sys.entity.SysAnnouncementRead;
import com.furuiduo.quote.sys.entity.SysAnnouncementReadId;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.repository.SysAnnouncementReadRepository;
import com.furuiduo.quote.sys.repository.SysAnnouncementRepository;
import com.furuiduo.quote.sys.repository.SysUserRepository;

@Service
public class AnnouncementService {

  private static final List<AnnouncementStatus> PENDING_STATUSES =
      List.of(AnnouncementStatus.PUBLISHED, AnnouncementStatus.SCHEDULED);

  private static final List<AnnouncementDisplayType> MODAL_DISPLAY_TYPES =
      List.of(AnnouncementDisplayType.MODAL, AnnouncementDisplayType.BOTH);

  private static final List<AnnouncementDisplayType> TICKER_DISPLAY_TYPES =
      List.of(AnnouncementDisplayType.TICKER, AnnouncementDisplayType.BOTH);

  private final SysAnnouncementRepository announcementRepository;
  private final SysAnnouncementReadRepository readRepository;
  private final SysUserRepository userRepository;
  private final PermissionService permissionService;

  public AnnouncementService(
      SysAnnouncementRepository announcementRepository,
      SysAnnouncementReadRepository readRepository,
      SysUserRepository userRepository,
      PermissionService permissionService) {
    this.announcementRepository = announcementRepository;
    this.readRepository = readRepository;
    this.userRepository = userRepository;
    this.permissionService = permissionService;
  }

  @Transactional
  public List<AnnouncementResponse> listPending(SysUser user) {
    promoteDueScheduled();
    LocalDateTime now = LocalDateTime.now();
    return announcementRepository
        .findPendingForUser(user.getId(), now, PENDING_STATUSES, MODAL_DISPLAY_TYPES)
        .stream()
        .map(item -> toResponse(item, now, false))
        .toList();
  }

  @Transactional
  public List<AnnouncementTickerResponse> listTicker(SysUser user) {
    promoteDueScheduled();
    LocalDateTime now = LocalDateTime.now();
    return announcementRepository
        .findActiveTicker(now, PENDING_STATUSES, TICKER_DISPLAY_TYPES)
        .stream()
        .map(this::toTickerResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<AnnouncementResponse> listAll(
      SysUser user,
      String keyword,
      String status,
      String createdByName,
      LocalDateTime startAt,
      LocalDateTime endAt) {
    requireView(user);
    LocalDateTime now = LocalDateTime.now();
    return announcementRepository.findAllWithCreatorOrderByUpdatedAtDesc().stream()
        .map(item -> toResponse(item, now, true))
        .filter(item -> matchesKeyword(item, keyword))
        .filter(item -> matchesStatus(item, status))
        .filter(item -> matchesCreator(item, createdByName))
        .filter(item -> matchesPublishedRange(item, startAt, endAt))
        .sorted(
            Comparator.comparing(
                    (AnnouncementResponse item) -> item.publishedAt(),
                    Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(AnnouncementResponse::id, Comparator.reverseOrder()))
        .toList();
  }

  @Transactional
  public AnnouncementResponse create(SysUser user, AnnouncementSaveRequest request) {
    requireManage(user);
    validateSaveRequest(request);
    SysAnnouncement announcement = new SysAnnouncement();
    announcement.setCreatedBy(user);
    applyContent(announcement, request);
    applySaveAction(announcement, request);
    return toResponse(announcementRepository.save(announcement), LocalDateTime.now(), true);
  }

  @Transactional
  public AnnouncementResponse update(SysUser user, Long id, AnnouncementSaveRequest request) {
    requireManage(user);
    validateSaveRequest(request);
    SysAnnouncement announcement = requireAnnouncement(id);
    applyContent(announcement, request);
    applySaveAction(announcement, request);
    return toResponse(announcementRepository.save(announcement), LocalDateTime.now(), true);
  }

  @Transactional
  public AnnouncementResponse copy(SysUser user, Long id) {
    requireManage(user);
    SysAnnouncement source = requireAnnouncement(id);
    SysAnnouncement copy = new SysAnnouncement();
    copy.setCreatedBy(user);
    copy.setTitle(buildCopyTitle(source.getTitle()));
    copy.setContent(source.getContent());
    copy.setValidDays(source.getValidDays());
    copy.setDisplayType(source.getDisplayType());
    copy.setStatus(AnnouncementStatus.DRAFT);
    copy.setEnabled(true);
    copy.setPublishedAt(null);
    copy.setExpiresAt(null);
    return toResponse(announcementRepository.save(copy), LocalDateTime.now(), true);
  }

  @Transactional
  public AnnouncementResponse disable(SysUser user, Long id) {
    requireManage(user);
    SysAnnouncement announcement = requireAnnouncement(id);
    if (announcement.getStatus() == AnnouncementStatus.DRAFT) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "草稿请直接编辑或删除");
    }
    announcement.setStatus(AnnouncementStatus.DISABLED);
    announcement.setEnabled(false);
    return toResponse(announcementRepository.save(announcement), LocalDateTime.now(), true);
  }

  @Transactional
  public void delete(SysUser user, Long id) {
    requireManage(user);
    if (!announcementRepository.existsById(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "公告不存在");
    }
    announcementRepository.deleteById(id);
  }

  @Transactional
  public void acknowledge(SysUser user, Long announcementId) {
    SysAnnouncement announcement = requireAnnouncement(announcementId);
    LocalDateTime now = LocalDateTime.now();
    promoteDueScheduled();
    if (!isVisibleToUsers(announcement, now)) {
      return;
    }
    SysAnnouncementReadId readId = new SysAnnouncementReadId(announcementId, user.getId());
    if (readRepository.existsById(readId)) {
      return;
    }
    SysAnnouncementRead read = new SysAnnouncementRead();
    read.setAnnouncementId(announcementId);
    read.setUserId(user.getId());
    readRepository.save(read);
  }

  private void promoteDueScheduled() {
    LocalDateTime now = LocalDateTime.now();
    for (SysAnnouncement announcement : announcementRepository.findDueScheduled(now)) {
      announcement.setStatus(AnnouncementStatus.PUBLISHED);
      announcement.setEnabled(true);
      finalizeExpiresOnPublish(announcement);
      announcementRepository.save(announcement);
    }
  }

  private void applyContent(SysAnnouncement announcement, AnnouncementSaveRequest request) {
    AnnouncementDisplayType displayType =
        request.displayType() == null
            ? AnnouncementDisplayType.MODAL
            : request.displayType();
    String title = request.title() == null ? "" : request.title().trim();
    String content = request.content() == null ? "" : request.content().trim();
    if (displayType.showsModal()) {
      announcement.setTitle(title);
    } else {
      announcement.setTitle(
          StringUtils.hasText(title) ? title : deriveTitleFromContent(content));
    }
    announcement.setContent(content);
    announcement.setDisplayType(displayType);
    applyValidDays(announcement, request.validDays());
  }

  private void validateSaveRequest(AnnouncementSaveRequest request) {
    AnnouncementDisplayType displayType =
        request.displayType() == null ? AnnouncementDisplayType.MODAL : request.displayType();
    if (displayType.showsModal() && !StringUtils.hasText(request.title())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "标题不能为空");
    }
    if (request.saveAction() == AnnouncementSaveAction.DRAFT) {
      return;
    }
    String plainContent = stripHtmlText(request.content());
    if (displayType.showsModal() && !StringUtils.hasText(plainContent)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "弹窗公告正文不能为空");
    }
    if (displayType.showsTicker() && !StringUtils.hasText(plainContent)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "滚动条公告正文不能为空");
    }
  }

  private String deriveTitleFromContent(String content) {
    String text = stripHtmlText(content);
    if (!StringUtils.hasText(text)) {
      return "滚动公告";
    }
    if (text.length() <= 128) {
      return text;
    }
    return text.substring(0, 125) + "…";
  }

  private void applySaveAction(SysAnnouncement announcement, AnnouncementSaveRequest request) {
    LocalDateTime now = LocalDateTime.now();
    switch (request.saveAction()) {
      case DRAFT -> {
        announcement.setStatus(AnnouncementStatus.DRAFT);
        announcement.setEnabled(true);
        announcement.setPublishedAt(null);
        announcement.setExpiresAt(null);
      }
      case PUBLISH_IMMEDIATE -> {
        announcement.setStatus(AnnouncementStatus.PUBLISHED);
        announcement.setEnabled(true);
        announcement.setPublishedAt(now);
        finalizeExpiresOnPublish(announcement);
      }
      case PUBLISH_SCHEDULED -> {
        LocalDateTime scheduledAt = request.scheduledAt();
        if (scheduledAt == null) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择预约发布时间");
        }
        if (!scheduledAt.isAfter(now)) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "预约发布时间必须晚于当前时间");
        }
        announcement.setStatus(AnnouncementStatus.SCHEDULED);
        announcement.setEnabled(true);
        announcement.setPublishedAt(scheduledAt);
        announcement.setExpiresAt(null);
        finalizeExpiresOnPublish(announcement);
      }
      case SAVE -> {
        if (announcement.getStatus() == AnnouncementStatus.DRAFT) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "草稿请使用保存草稿或发布");
        }
        if (announcement.getPublishedAt() != null) {
          finalizeExpiresOnPublish(announcement);
        }
      }
      default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的保存动作");
    }
  }

  private void applyValidDays(SysAnnouncement announcement, Integer validDays) {
    if (validDays != null && validDays > 0) {
      announcement.setValidDays(validDays);
      if (announcement.getPublishedAt() != null) {
        announcement.setExpiresAt(announcement.getPublishedAt().plusDays(validDays));
      } else {
        announcement.setExpiresAt(null);
      }
      return;
    }
    announcement.setValidDays(null);
    announcement.setExpiresAt(null);
  }

  private void finalizeExpiresOnPublish(SysAnnouncement announcement) {
    if (announcement.getValidDays() != null
        && announcement.getValidDays() > 0
        && announcement.getPublishedAt() != null) {
      announcement.setExpiresAt(
          announcement.getPublishedAt().plusDays(announcement.getValidDays()));
    }
  }

  private boolean isVisibleToUsers(SysAnnouncement announcement, LocalDateTime now) {
    if (announcement.getStatus() == AnnouncementStatus.DRAFT
        || announcement.getStatus() == AnnouncementStatus.DISABLED) {
      return false;
    }
    if (announcement.getPublishedAt() == null || announcement.getPublishedAt().isAfter(now)) {
      return false;
    }
    if (announcement.getExpiresAt() != null && !announcement.getExpiresAt().isAfter(now)) {
      return false;
    }
    return announcement.getStatus() == AnnouncementStatus.PUBLISHED
        || announcement.getStatus() == AnnouncementStatus.SCHEDULED;
  }

  private SysAnnouncement requireAnnouncement(Long id) {
    return announcementRepository
        .findByIdWithCreator(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "公告不存在"));
  }

  private String buildCopyTitle(String title) {
    String suffix = " (副本)";
    if (title.endsWith(suffix)) {
      return title;
    }
    String base = title;
    if (base.length() + suffix.length() > 128) {
      base = base.substring(0, Math.max(1, 128 - suffix.length()));
    }
    return base + suffix;
  }

  private boolean matchesKeyword(AnnouncementResponse item, String keyword) {
    if (!StringUtils.hasText(keyword)) {
      return true;
    }
    String q = keyword.trim().toLowerCase(Locale.ROOT);
    return (item.title() != null && item.title().toLowerCase(Locale.ROOT).contains(q))
        || (item.content() != null && item.content().toLowerCase(Locale.ROOT).contains(q));
  }

  private boolean matchesStatus(AnnouncementResponse item, String status) {
    if (!StringUtils.hasText(status)) {
      return true;
    }
    return status.trim().equalsIgnoreCase(item.status());
  }

  private boolean matchesCreator(AnnouncementResponse item, String createdByName) {
    if (!StringUtils.hasText(createdByName)) {
      return true;
    }
    String q = createdByName.trim().toLowerCase(Locale.ROOT);
    return item.createdByName() != null
        && item.createdByName().toLowerCase(Locale.ROOT).contains(q);
  }

  private boolean matchesPublishedRange(
      AnnouncementResponse item, LocalDateTime startAt, LocalDateTime endAt) {
    if (startAt == null && endAt == null) {
      return true;
    }
    if (!StringUtils.hasText(item.publishedAt())) {
      return false;
    }
    LocalDateTime publishedAt = LocalDateTime.parse(item.publishedAt());
    if (startAt != null && publishedAt.isBefore(startAt)) {
      return false;
    }
    if (endAt != null && publishedAt.isAfter(endAt)) {
      return false;
    }
    return true;
  }

  private void requireView(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.SYS_ANNOUNCEMENT_VIEW)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
  }

  private void requireManage(SysUser user) {
    if (!permissionService.hasPermission(user, PermissionCodes.SYS_ANNOUNCEMENT_MANAGE)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
  }

  private AnnouncementResponse toResponse(
      SysAnnouncement announcement, LocalDateTime now, boolean includeStats) {
    String createdByName = null;
    if (announcement.getCreatedBy() != null) {
      SysUser creator = announcement.getCreatedBy();
      createdByName =
          creator.getRealName() != null && !creator.getRealName().isBlank()
              ? creator.getRealName()
              : creator.getUsername();
    }
    long readCount = 0L;
    long unreadCount = 0L;
    if (includeStats) {
      readCount = readRepository.countByAnnouncementId(announcement.getId());
      long totalUsers = userRepository.countByStatus(1);
      unreadCount = Math.max(totalUsers - readCount, 0L);
    }
    return new AnnouncementResponse(
        announcement.getId(),
        announcement.getTitle(),
        announcement.getContent(),
        announcement.getDisplayType().name(),
        resolveDisplayStatus(announcement, now),
        announcement.getPublishedAt() != null ? announcement.getPublishedAt().toString() : null,
        announcement.getExpiresAt() != null ? announcement.getExpiresAt().toString() : null,
        announcement.getValidDays(),
        createdByName,
        readCount,
        unreadCount,
        announcement.getStatus() != AnnouncementStatus.DISABLED && announcement.isEnabled());
  }

  private String resolveDisplayStatus(SysAnnouncement announcement, LocalDateTime now) {
    if (announcement.getStatus() == AnnouncementStatus.DISABLED) {
      return "DISABLED";
    }
    if (announcement.getStatus() == AnnouncementStatus.DRAFT) {
      return "DRAFT";
    }
    if (announcement.getStatus() == AnnouncementStatus.SCHEDULED) {
      if (announcement.getPublishedAt() != null && announcement.getPublishedAt().isAfter(now)) {
        return "SCHEDULED";
      }
    }
    if (announcement.getExpiresAt() != null && !announcement.getExpiresAt().isAfter(now)) {
      return "EXPIRED";
    }
    if (announcement.getStatus() == AnnouncementStatus.SCHEDULED) {
      return "SCHEDULED";
    }
    return "PUBLISHED";
  }

  private AnnouncementTickerResponse toTickerResponse(SysAnnouncement announcement) {
    return new AnnouncementTickerResponse(
        announcement.getId(),
        resolveTickerText(announcement),
        announcement.getTitle() == null ? "" : announcement.getTitle().trim(),
        announcement.getContent() == null ? "" : announcement.getContent().trim());
  }

  private String resolveTickerText(SysAnnouncement announcement) {
    String content = stripHtmlText(announcement.getContent());
    if (StringUtils.hasText(content)) {
      return content;
    }
    return announcement.getTitle() == null ? "" : announcement.getTitle().trim();
  }

  private String stripHtmlText(String value) {
    if (!StringUtils.hasText(value)) {
      return "";
    }
    return value
        .replaceAll("<[^>]+>", " ")
        .replace("&nbsp;", " ")
        .replaceAll("\\s+", " ")
        .trim();
  }
}
