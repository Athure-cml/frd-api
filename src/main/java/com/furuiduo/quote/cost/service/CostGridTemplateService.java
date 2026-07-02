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
    String normalized = normalizeMode(mode);
    return repository.findByModeOrderByIdAsc(normalized).stream()
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
    return CostTableTemplateResponse.from(requireEntity(id));
  }

  public CostGridTemplate requireEntity(Long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found"));
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

  @Transactional
  public CostTableTemplateResponse create(CostTableTemplateSaveRequest request) {
    String mode = normalizeMode(request.mode());
    String code = templateCodeGenerator.next(mode);
    String name = normalizeName(request.name());
    CostTableTemplateLayout layout = normalizeLayout(mode, request.layout());

    CostGridTemplate entity = new CostGridTemplate();
    entity.setMode(mode);
    entity.setCode(code);
    entity.setName(name);
    entity.setLayout(layout);
    boolean isDefault = Boolean.TRUE.equals(request.isDefault()) || repository.countByMode(mode) == 0;
    entity.setDefaultTemplate(isDefault);
    entity.touch();
    repository.save(entity);

    if (isDefault) {
      clearOtherDefaults(entity);
    }
    return CostTableTemplateResponse.from(entity);
  }

  @Transactional
  public CostTableTemplateResponse update(Long id, CostTableTemplateSaveRequest request) {
    CostGridTemplate entity = requireEntity(id);
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
      long count = repository.countByMode(mode);
      if (count <= 1) {
        entity.setDefaultTemplate(true);
      } else {
        entity.setDefaultTemplate(false);
      }
    }

    entity.touch();
    repository.save(entity);
    return CostTableTemplateResponse.from(entity);
  }

  @Transactional
  public void delete(Long id) {
    CostGridTemplate entity = requireEntity(id);
    if (entity.isDefaultTemplate()) {
      long count = repository.countByMode(entity.getMode());
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
    CostGridTemplate entity = requireEntity(id);
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
    String normalized = normalizeMode(mode);
    if (templateId != null) {
      CostGridTemplate entity = requireEntity(templateId);
      if (!entity.getMode().equals(normalized)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "模板与成本库类型不匹配");
      }
      return entity.getLayout();
    }
    return repository
        .findFirstByModeAndDefaultTemplateTrue(normalized)
        .map(CostGridTemplate::getLayout)
        .orElseGet(() -> defaultLayoutForMode(normalized));
  }

  public TemplateExcelFile exportById(Long id) {
    CostGridTemplate entity = requireEntity(id);
    return buildExport(entity.getMode(), entity.getCode(), entity.getName(), entity.getLayout());
  }

  public TemplateExcelFile exportPreview(CostTableTemplateSaveRequest request) {
    String mode = normalizeMode(request.mode());
    String code = resolvePreviewCode(mode, request.code());
    String name = normalizeName(request.name());
    CostTableTemplateLayout layout = normalizeLayout(mode, request.layout());
    return buildExport(mode, code, name, layout);
  }

  private TemplateExcelFile buildExport(
      String mode, String code, String name, CostTableTemplateLayout layout) {
    byte[] content = CostTemplateExcelSupport.buildWorkbook(mode, code, name, layout);
    String filename = CostTemplateExcelSupport.buildFilename(code, mode);
    return new TemplateExcelFile(filename, content);
  }

  private void clearOtherDefaults(CostGridTemplate current) {
    repository
        .findByModeOrderByIdAsc(current.getMode())
        .forEach(
            item -> {
              if (!item.getId().equals(current.getId()) && item.isDefaultTemplate()) {
                item.setDefaultTemplate(false);
                item.touch();
                repository.save(item);
              }
            });
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
