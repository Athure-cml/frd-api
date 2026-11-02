package com.furuiduo.quote.cost.service;

import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.cost.dto.CostTableTemplateLayout;
import com.furuiduo.quote.cost.dto.CostTableTemplateResponse;
import com.furuiduo.quote.cost.dto.CostTableTemplateSaveRequest;
import com.furuiduo.quote.cost.dto.TemplateExcelFile;
import com.furuiduo.quote.cost.entity.CostGridTemplate;

import static com.furuiduo.quote.cost.entity.CostGridTemplate.LIBRARY_COST;
import static com.furuiduo.quote.cost.entity.CostGridTemplate.LIBRARY_QUOTE;
import com.furuiduo.quote.cost.repository.CostGridTemplateRepository;
import com.furuiduo.quote.cost.support.CostFieldCatalog;
import com.furuiduo.quote.cost.support.CostTemplateCodeGenerator;
import com.furuiduo.quote.cost.support.CostTemplateExcelSupport;
import com.furuiduo.quote.cost.support.CostTemplateLayouts;

@Service
public class CostGridTemplateService {

  private final CostGridTemplateRepository repository;
  private final CostTemplateCodeGenerator templateCodeGenerator;

  public CostGridTemplateService(
      CostGridTemplateRepository repository, CostTemplateCodeGenerator templateCodeGenerator) {
    this.repository = repository;
    this.templateCodeGenerator = templateCodeGenerator;
  }

  public List<CostTableTemplateResponse> listByMode(String mode) {
    return listByLibrary(LIBRARY_COST, mode);
  }

  public List<CostTableTemplateResponse> listByLibrary(String library, String mode) {
    String normalizedLibrary = normalizeLibrary(library);
    String normalized = normalizeMode(mode);
    return repository.findByLibraryAndModeOrderByIdAsc(normalizedLibrary, normalized).stream()
        .filter(item -> !item.isLegacy())
        .sorted(
            (a, b) -> {
              if (a.isDefaultTemplate() != b.isDefaultTemplate()) {
                return a.isDefaultTemplate() ? -1 : 1;
              }
              if (a.getCreatedAt() != null && b.getCreatedAt() != null) {
                return b.getCreatedAt().compareTo(a.getCreatedAt());
              }
              return Long.compare(a.getId(), b.getId());
            })
        .map(CostTableTemplateResponse::from)
        .toList();
  }

  public CostTableTemplateResponse getById(Long id) {
    return getById(LIBRARY_COST, id);
  }

  public CostTableTemplateResponse getById(String library, Long id) {
    return CostTableTemplateResponse.from(requireEntity(library, id));
  }

  public CostGridTemplate requireEntity(Long id) {
    return requireEntity(LIBRARY_COST, id);
  }

  public CostGridTemplate requireEntity(String library, Long id) {
    CostGridTemplate entity =
        repository
            .findById(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found"));
    if (!normalizeLibrary(library).equals(normalizeLibrary(entity.getLibrary()))) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found");
    }
    return entity;
  }

  public String normalizeMode(String mode) {
    if (mode == null || mode.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mode is required");
    }
    String normalized = mode.trim().toLowerCase(Locale.ROOT);
    if (!normalized.equals("road")
        && !normalized.equals("sea")
        && !normalized.equals("fumigation")
        && !normalized.equals("rail")) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid mode: " + mode);
    }
    return normalized;
  }

  public String normalizeLibrary(String library) {
    if (library == null || library.isBlank()) {
      return LIBRARY_COST;
    }
    String normalized = library.trim().toLowerCase(Locale.ROOT);
    if (!LIBRARY_COST.equals(normalized) && !LIBRARY_QUOTE.equals(normalized)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid library: " + library);
    }
    return normalized;
  }

  @Transactional
  public CostTableTemplateResponse create(CostTableTemplateSaveRequest request) {
    return create(LIBRARY_COST, request);
  }

  @Transactional
  public CostTableTemplateResponse create(String library, CostTableTemplateSaveRequest request) {
    String normalizedLibrary = normalizeLibrary(library);
    String mode = normalizeMode(request.mode());
    String code = templateCodeGenerator.next(normalizedLibrary, mode);
    String name = normalizeName(request.name());
    CostTableTemplateLayout layout = normalizeLayout(mode, request.layout());

    CostGridTemplate entity = new CostGridTemplate();
    entity.setLibrary(normalizedLibrary);
    entity.setMode(mode);
    entity.setCode(code);
    entity.setName(name);
    entity.setLayout(layout);
    boolean isDefault =
        Boolean.TRUE.equals(request.isDefault())
            || countActiveTemplates(normalizedLibrary, mode) == 0;
    entity.setDefaultTemplate(isDefault);
    entity.setLegacy(false);
    entity.touch();
    repository.save(entity);

    if (isDefault) {
      clearOtherDefaults(entity);
    }
    return CostTableTemplateResponse.from(entity);
  }

  @Transactional
  public CostTableTemplateResponse update(Long id, CostTableTemplateSaveRequest request) {
    return update(LIBRARY_COST, id, request);
  }

  @Transactional
  public CostTableTemplateResponse update(
      String library, Long id, CostTableTemplateSaveRequest request) {
    CostGridTemplate entity = requireEntity(library, id);
    rejectLegacyMutation(entity);
    String mode = entity.getMode();
    if (!mode.equals(normalizeMode(request.mode()))) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mode cannot be changed");
    }

    entity.setName(normalizeName(request.name()));
    entity.setLayout(normalizeLayout(mode, request.layout()));

    boolean wantsDefault = Boolean.TRUE.equals(request.isDefault());
    if (wantsDefault) {
      entity.setDefaultTemplate(true);
      clearOtherDefaults(entity);
    } else if (entity.isDefaultTemplate()) {
      long count = repository.countByLibraryAndMode(entity.getLibrary(), mode);
      if (count <= 1) {
        // 唯一模板必须保持默认
        entity.setDefaultTemplate(true);
      } else {
        entity.setDefaultTemplate(false);
        entity.touch();
        repository.save(entity);
        promoteAnotherDefault(entity);
        return CostTableTemplateResponse.from(requireEntity(id));
      }
    }

    entity.touch();
    repository.save(entity);
    return CostTableTemplateResponse.from(entity);
  }

  @Transactional
  public void delete(Long id) {
    delete(LIBRARY_COST, id);
  }

  @Transactional
  public void delete(String library, Long id) {
    CostGridTemplate entity = requireEntity(library, id);
    rejectLegacyMutation(entity);
    if (entity.isDefaultTemplate()) {
      long count = repository.countByLibraryAndMode(entity.getLibrary(), entity.getMode());
      if (count <= 1) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Cannot delete the only default template");
      }
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Set another template as default before deleting");
    }
    repository.delete(entity);
  }

  @Transactional
  public CostTableTemplateResponse setDefault(Long id) {
    return setDefault(LIBRARY_COST, id);
  }

  @Transactional
  public CostTableTemplateResponse setDefault(String library, Long id) {
    CostGridTemplate entity = requireEntity(library, id);
    rejectLegacyMutation(entity);
    entity.setDefaultTemplate(true);
    entity.touch();
    repository.save(entity);
    clearOtherDefaults(entity);
    return CostTableTemplateResponse.from(entity);
  }

  public CostTableTemplateLayout defaultLayoutForMode(String mode) {
    return switch (normalizeMode(mode)) {
      case "road" -> CostTemplateLayouts.roadDefault();
      case "sea" -> CostTemplateLayouts.seaDefault();
      case "fumigation" -> CostTemplateLayouts.fumigationDefault();
      case "rail" -> CostTemplateLayouts.railDefault();
      default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid mode");
    };
  }

  public CostTableTemplateLayout resolveExportLayout(String mode, Long templateId) {
    return resolveExportLayout(LIBRARY_COST, mode, templateId);
  }

  public CostTableTemplateLayout resolveExportLayout(String library, String mode, Long templateId) {
    String normalizedLibrary = normalizeLibrary(library);
    String normalized = normalizeMode(mode);
    if (templateId != null) {
      CostGridTemplate entity = requireEntity(normalizedLibrary, templateId);
      if (!entity.getMode().equals(normalized)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "模板与类型不匹配");
      }
      return entity.getLayout();
    }
    return repository
        .findFirstByLibraryAndModeAndDefaultTemplateTrue(normalizedLibrary, normalized)
        .map(CostGridTemplate::getLayout)
        .orElseGet(() -> defaultLayoutForMode(normalized));
  }

  public TemplateExcelFile exportById(Long id) {
    return exportById(LIBRARY_COST, id);
  }

  public TemplateExcelFile exportById(String library, Long id) {
    CostGridTemplate entity = requireEntity(library, id);
    return buildExport(
        entity.getMode(), entity.getCode(), entity.getName(), entity.getLayout(), false);
  }

  public TemplateExcelFile exportPreview(CostTableTemplateSaveRequest request) {
    return exportPreview(LIBRARY_COST, request);
  }

  public TemplateExcelFile exportPreview(String library, CostTableTemplateSaveRequest request) {
    normalizeLibrary(library);
    String mode = normalizeMode(request.mode());
    String code = resolvePreviewCode(mode, request.code());
    String name = normalizeName(request.name());
    CostTableTemplateLayout layout = normalizeLayout(mode, request.layout());
    return buildExport(mode, code, name, layout, true);
  }

  private TemplateExcelFile buildExport(
      String mode,
      String code,
      String name,
      CostTableTemplateLayout layout,
      boolean preview) {
    byte[] content = CostTemplateExcelSupport.buildWorkbook(mode, code, name, layout);
    String filename =
        preview
            ? CostTemplateExcelSupport.buildPreviewFilename(mode)
            : CostTemplateExcelSupport.buildFilename(code, mode);
    return new TemplateExcelFile(filename, content);
  }

  private void clearOtherDefaults(CostGridTemplate current) {
    repository
        .findByLibraryAndModeOrderByIdAsc(current.getLibrary(), current.getMode())
        .forEach(
            item -> {
              if (item.isLegacy()) {
                return;
              }
              if (!item.getId().equals(current.getId()) && item.isDefaultTemplate()) {
                item.setDefaultTemplate(false);
                item.touch();
                repository.save(item);
              }
            });
  }

  /** 取消当前默认后，自动将同 mode 下另一条模板设为默认，避免零默认。 */
  private void promoteAnotherDefault(CostGridTemplate cleared) {
    boolean hasActiveDefault =
        repository
            .findByLibraryAndModeOrderByIdAsc(cleared.getLibrary(), cleared.getMode())
            .stream()
            .anyMatch(item -> !item.isLegacy() && item.isDefaultTemplate());
    if (hasActiveDefault) {
      return;
    }
    repository.findByLibraryAndModeOrderByIdAsc(cleared.getLibrary(), cleared.getMode()).stream()
        .filter(item -> !item.isLegacy())
        .filter(item -> !item.getId().equals(cleared.getId()))
        .findFirst()
        .ifPresent(
            item -> {
              item.setDefaultTemplate(true);
              item.touch();
              repository.save(item);
            });
  }

  private long countActiveTemplates(String library, String mode) {
    return repository.findByLibraryAndModeOrderByIdAsc(library, mode).stream()
        .filter(item -> !item.isLegacy())
        .count();
  }

  private static void rejectLegacyMutation(CostGridTemplate entity) {
    if (entity.isLegacy()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Legacy fumigation template is locked");
    }
  }

  private String resolvePreviewCode(String mode, String requestCode) {
    if (requestCode != null && !requestCode.isBlank()) {
      return normalizeCode(requestCode);
    }
    return mode + "_preview";
  }

  private String normalizeCode(String code) {
    try {
      return CostFieldCatalog.normalizeCode(code);
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
  }

  private String normalizeName(String name) {
    if (name == null || name.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
    }
    return name.trim();
  }

  private CostTableTemplateLayout normalizeLayout(String mode, CostTableTemplateLayout layout) {
    try {
      CostFieldCatalog.validateLayout(mode, layout);
      return layout;
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
  }
}
