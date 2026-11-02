package com.furuiduo.quote.quote.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.common.PageResult;
import com.furuiduo.quote.common.SearchText;
import com.furuiduo.quote.masterdata.service.DestAddressService;
import com.furuiduo.quote.quote.dto.InquiryDetailResponse;
import com.furuiduo.quote.quote.dto.InquiryListItem;
import com.furuiduo.quote.quote.dto.InquirySaveRequest;
import com.furuiduo.quote.quote.entity.InquiryOrder;
import com.furuiduo.quote.quote.entity.InquiryStatus;
import com.furuiduo.quote.quote.entity.QuoteOrder;
import com.furuiduo.quote.quote.entity.QuoteStatus;
import com.furuiduo.quote.quote.entity.QuoteTransportMode;
import com.furuiduo.quote.quote.repository.InquiryOrderRepository;
import com.furuiduo.quote.quote.repository.QuoteOrderRepository;
import com.furuiduo.quote.quote.support.InquiryNoGenerator;
import com.furuiduo.quote.quote.support.QuoteNoGenerator;
import com.furuiduo.quote.quote.support.QuoteServiceTypesSupport;
import com.furuiduo.quote.sys.entity.SysUser;

@Service
public class InquiryCommandService {

  private final InquiryOrderRepository inquiryOrderRepository;
  private final QuoteOrderRepository quoteOrderRepository;
  private final InquiryNoGenerator inquiryNoGenerator;
  private final QuoteNoGenerator quoteNoGenerator;
  private final InquiryQuoteSyncService inquiryQuoteSyncService;
  private final QuoteSheetGenerateService quoteSheetGenerateService;
  private final DestAddressService destAddressService;

  public InquiryCommandService(
      InquiryOrderRepository inquiryOrderRepository,
      QuoteOrderRepository quoteOrderRepository,
      InquiryNoGenerator inquiryNoGenerator,
      QuoteNoGenerator quoteNoGenerator,
      InquiryQuoteSyncService inquiryQuoteSyncService,
      QuoteSheetGenerateService quoteSheetGenerateService,
      DestAddressService destAddressService) {
    this.inquiryOrderRepository = inquiryOrderRepository;
    this.quoteOrderRepository = quoteOrderRepository;
    this.inquiryNoGenerator = inquiryNoGenerator;
    this.quoteNoGenerator = quoteNoGenerator;
    this.inquiryQuoteSyncService = inquiryQuoteSyncService;
    this.quoteSheetGenerateService = quoteSheetGenerateService;
    this.destAddressService = destAddressService;
  }

  @Transactional
  public PageResult<InquiryListItem> list(
      String status,
      String inquiryNo,
      String customerName,
      String por,
      String pol,
      String pod,
      String zipCode,
      String city,
      String state,
      String pickUpAddress,
      String fumigationPoint,
      Long createdBy,
      int page,
      int pageSize) {
    inquiryQuoteSyncService.syncExpiredQuotesAndInquiries();
    InquiryStatus parsed = parseStatus(status);
    Page<InquiryOrder> result =
        inquiryOrderRepository.search(
            parsed,
            SearchText.orEmpty(inquiryNo),
            SearchText.orEmpty(customerName),
            SearchText.orEmpty(por),
            SearchText.orEmpty(pol),
            SearchText.orEmpty(pod),
            SearchText.orEmpty(zipCode),
            SearchText.orEmpty(city),
            SearchText.orEmpty(state),
            SearchText.orEmpty(pickUpAddress),
            SearchText.orEmpty(fumigationPoint),
            createdBy,
            PageRequest.of(
                Math.max(page - 1, 0),
                Math.min(Math.max(pageSize, 1), 200),
                Sort.by(Sort.Direction.DESC, "updatedAt")));
    return new PageResult<>(
        result.getContent().stream().map(InquiryListItem::from).toList(), result.getTotalElements());
  }

  @Transactional
  public InquiryDetailResponse getById(Long id) {
    inquiryQuoteSyncService.syncExpiredQuotesAndInquiries();
    InquiryOrder order = requireExisting(id);
    String quoteNo = null;
    if (order.getQuoteId() != null) {
      quoteNo =
          quoteOrderRepository.findById(order.getQuoteId()).map(QuoteOrder::getQuoteNo).orElse(null);
    }
    return InquiryDetailResponse.from(order, quoteNo);
  }

  @Transactional
  public InquiryDetailResponse create(SysUser user, InquirySaveRequest request) {
    validateBasic(request);
    InquiryOrder order = new InquiryOrder();
    order.setInquiryNo(inquiryNoGenerator.next());
    order.setStatus(InquiryStatus.PENDING_SUBMIT);
    order.setCreatedBy(user.getId());
    order.setCreatedByName(user.getRealName());
    order.setDeptId(user.getDepartment() != null ? user.getDepartment().getId() : null);
    applySave(order, request);
    InquiryOrder saved = inquiryOrderRepository.save(order);
    return InquiryDetailResponse.from(saved, null);
  }

  @Transactional
  public InquiryDetailResponse update(SysUser user, Long id, InquirySaveRequest request) {
    validateBasic(request);
    InquiryOrder order = requireExisting(id);
    assertOwnerOrAdminEditable(user, order);
    if (order.getStatus() != InquiryStatus.PENDING_SUBMIT) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "仅待递交询价单可编辑");
    }
    applySave(order, request);
    order.touch();
    return InquiryDetailResponse.from(inquiryOrderRepository.save(order), null);
  }

  /**
   * 递交报价员：生成报价单（待报价），询价单 → 待报价。
   */
  @Transactional
  public InquiryDetailResponse submit(SysUser user, Long id) {
    InquiryOrder inquiry = requireExisting(id);
    assertOwnerOrAdminEditable(user, inquiry);
    if (inquiry.getStatus() != InquiryStatus.PENDING_SUBMIT) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "仅待递交询价单可递交");
    }
    if (inquiry.getQuoteId() != null) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "该询价单已生成报价单");
    }
    validateReadyToSubmit(inquiry);

    QuoteOrder quote = new QuoteOrder();
    quote.setQuoteNo(quoteNoGenerator.next());
    quote.setStatus(QuoteStatus.PENDING_QUOTE);
    quote.setInquiryId(inquiry.getId());
    // 操作人留给报价员首次保存时写入，不沿用询价递交人
    quote.setCreatedBy(null);
    quote.setCreatedByName(null);
    quote.setDeptId(user.getDepartment() != null ? user.getDepartment().getId() : null);
    copyInquiryToQuote(inquiry, quote);
    QuoteOrder savedQuote = quoteOrderRepository.save(quote);
    if (savedQuote.getRootQuoteId() == null) {
      savedQuote.setRootQuoteId(savedQuote.getId());
      savedQuote.setRevisionNo(0);
      savedQuote.setCurrentVersion(true);
      savedQuote = quoteOrderRepository.save(savedQuote);
    }

    inquiry.setQuoteId(savedQuote.getId());
    inquiry.setStatus(InquiryStatus.PENDING_QUOTE);
    inquiry.setSubmittedAt(LocalDateTime.now());
    inquiry.touch();
    InquiryOrder savedInquiry = inquiryOrderRepository.save(inquiry);
    return InquiryDetailResponse.from(savedInquiry, savedQuote.getQuoteNo());
  }

  @Transactional
  public void delete(SysUser user, Long id) {
    InquiryOrder order = requireExisting(id);
    assertOwnerOrAdminEditable(user, order);
    if (order.getStatus() != InquiryStatus.PENDING_SUBMIT) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "仅待递交询价单可删除");
    }
    order.setDeletedAt(LocalDateTime.now());
    order.touch();
    inquiryOrderRepository.save(order);
  }

  private void copyInquiryToQuote(InquiryOrder inquiry, QuoteOrder quote) {
    quote.setServiceTypes(QuoteServiceTypesSupport.copyOf(inquiry.getServiceTypes()));
    quote.setCustomerId(inquiry.getCustomerId());
    quote.setCustomerName(inquiry.getCustomerName());
    quote.setTransportMode(
        inquiry.getTransportMode() == null ? QuoteTransportMode.SEA : inquiry.getTransportMode());
    quote.setRouteSummary(inquiry.getRouteSummary());
    quote.setValidUntil(inquiry.getValidUntil());
    quote.setZipCode(inquiry.getZipCode());
    quote.setCity(inquiry.getCity());
    quote.setState(inquiry.getState());
    quote.setPor(inquiry.getPor());
    quote.setPol(inquiry.getPol());
    quote.setPod(inquiry.getPod());
    quote.setFumigationEnabled(Boolean.TRUE.equals(inquiry.getFumigationEnabled()));
    quote.setFumigationPoint(inquiry.getFumigationPoint());
    quote.setOakType(inquiry.getOakType());
    quote.setPickUpAddress(inquiry.getPickUpAddress());
    quote.setRemark(inquiry.getRemark());
    quote.setSheetRemark(inquiry.getRemark());
    // 按 POD 套 DOC FEE（是否中国港）；询价无 CIF，保险/代理费仍留给报价员填写
    String pod = inquiry.getPod();
    if (pod != null && !pod.isBlank()) {
      quote.setDocUsd(quoteSheetGenerateService.previewDocFee(pod.trim()));
    }
  }

  private void applySave(InquiryOrder order, InquirySaveRequest request) {
    List<String> types =
        request.serviceTypes() == null
            ? new ArrayList<>()
            : new ArrayList<>(QuoteServiceTypesSupport.copyOf(request.serviceTypes()));
    order.setServiceTypes(types);
    order.setCustomerId(request.customerId());
    String customerName =
        request.customerName() == null ? "" : request.customerName().trim();
    order.setCustomerName(customerName.isEmpty() ? "—" : customerName);
    order.setTransportMode(
        request.transportMode() == null ? QuoteTransportMode.SEA : request.transportMode());
    order.setRouteSummary(trimToNull(request.routeSummary()));
    // 仅新建且未传日期时默认当天；更新时缺省保留原值，避免被今天覆盖
    if (request.quoteDate() != null) {
      order.setQuoteDate(request.quoteDate());
    } else if (order.getId() == null) {
      order.setQuoteDate(LocalDate.now());
    }
    order.setValidUntil(request.validUntil());
    // 地址规范化：缺 ZIP 用 City+State 补；缺 City/State 用 ZIP 反带；City 统一主数据写法
    DestAddressService.RouteAddress address =
        destAddressService.normalizeRouteAddress(
            request.zipCode(), request.city(), request.state());
    order.setZipCode(trimToNull(address.zipCode()));
    order.setCity(trimToNull(address.city()));
    order.setState(trimToNull(address.state()));
    order.setPor(trimToNull(request.por()));
    order.setPol(trimToNull(request.pol()));
    order.setPod(trimToNull(request.pod()));
    order.setFumigationEnabled(Boolean.TRUE.equals(request.fumigationEnabled()));
    order.setFumigationPoint(trimToNull(request.fumigationPoint()));
    order.setOakType(request.oakType());
    order.setPickUpAddress(trimToNull(request.pickUpAddress()));
    order.setRemark(trimToNull(request.remark()));
  }

  private void validateBasic(InquirySaveRequest request) {
    if (request == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请求不能为空");
    }
  }

  private void validateReadyToSubmit(InquiryOrder order) {
    if (order.getServiceTypes() == null || order.getServiceTypes().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择服务类型");
    }
    if (order.getPor() == null || order.getPor().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写 POR");
    }
    if (order.getPod() == null || order.getPod().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写 POD");
    }
  }

  private InquiryOrder requireExisting(Long id) {
    return inquiryOrderRepository
        .findByIdAndDeletedAtIsNull(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "询价单不存在"));
  }

  private void assertOwnerOrAdminEditable(SysUser user, InquiryOrder order) {
    if (user == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
    }
    // 数据权限细化后续与报价单对齐；当前先允许创建人编辑
    if (order.getCreatedBy() != null && !order.getCreatedBy().equals(user.getId())) {
      // 仍允许：由权限码控制接口访问，这里不硬拦其他人
    }
  }

  private static InquiryStatus parseStatus(String status) {
    if (status == null || status.isBlank()) {
      return null;
    }
    try {
      return InquiryStatus.valueOf(status.trim());
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的询价单状态");
    }
  }

  private static String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
