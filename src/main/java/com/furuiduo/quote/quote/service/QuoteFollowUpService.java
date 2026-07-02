package com.furuiduo.quote.quote.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.quote.dto.QuoteFollowUpResponse;
import com.furuiduo.quote.quote.dto.QuoteFollowUpSaveRequest;
import com.furuiduo.quote.quote.entity.QuoteFollowUp;
import com.furuiduo.quote.quote.entity.QuoteOrder;
import com.furuiduo.quote.quote.repository.QuoteFollowUpRepository;
import com.furuiduo.quote.quote.repository.QuoteOrderRepository;
import com.furuiduo.quote.quote.support.QuoteDateTimes;
import com.furuiduo.quote.sys.entity.SysUser;

@Service
public class QuoteFollowUpService {

  private final QuoteFollowUpRepository followUpRepository;
  private final QuoteOrderRepository quoteOrderRepository;
  private final QuoteAccessService quoteAccessService;

  public QuoteFollowUpService(
      QuoteFollowUpRepository followUpRepository,
      QuoteOrderRepository quoteOrderRepository,
      QuoteAccessService quoteAccessService) {
    this.followUpRepository = followUpRepository;
    this.quoteOrderRepository = quoteOrderRepository;
    this.quoteAccessService = quoteAccessService;
  }

  public List<QuoteFollowUpResponse> list(SysUser user, Long quoteId) {
    QuoteOrder order = requireQuote(user, quoteId);
    return followUpRepository.findByQuoteOrderIdOrderByFollowUpAtDesc(order.getId()).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  public QuoteFollowUpResponse create(SysUser user, Long quoteId, QuoteFollowUpSaveRequest request) {
    validateRequest(request);
    QuoteOrder order = requireQuote(user, quoteId);
    QuoteFollowUp record = new QuoteFollowUp();
    record.setQuoteOrder(order);
    record.setFollowStatus(request.followStatus().trim());
    record.setContent(request.content().trim());
    record.setFollowUpBy(user.getId());
    record.setFollowUpByName(user.getRealName());
    record.setFollowUpAt(LocalDateTime.now());
    record.setUpdatedAt(LocalDateTime.now());
    order.setFollowUpBy(user.getId());
    order.setFollowUpByName(user.getRealName());
    order.setUpdatedAt(LocalDateTime.now());
    quoteOrderRepository.save(order);
    return toResponse(followUpRepository.save(record));
  }

  @Transactional
  public QuoteFollowUpResponse update(
      SysUser user, Long quoteId, Long followUpId, QuoteFollowUpSaveRequest request) {
    validateRequest(request);
    requireQuote(user, quoteId);
    QuoteFollowUp record =
        followUpRepository
            .findById(followUpId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "跟进记录不存在"));
    if (!record.getQuoteOrder().getId().equals(quoteId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "跟进记录不存在");
    }
    if (!record.getFollowUpBy().equals(user.getId())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅可编辑本人跟进记录");
    }
    record.setFollowStatus(request.followStatus().trim());
    record.setContent(request.content().trim());
    record.setUpdatedAt(LocalDateTime.now());
    return toResponse(followUpRepository.save(record));
  }

  @Transactional
  public void delete(SysUser user, Long quoteId, Long followUpId) {
    requireQuote(user, quoteId);
    QuoteFollowUp record =
        followUpRepository
            .findById(followUpId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "跟进记录不存在"));
    if (!record.getQuoteOrder().getId().equals(quoteId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "跟进记录不存在");
    }
    if (!record.getFollowUpBy().equals(user.getId())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅可删除本人跟进记录");
    }
    followUpRepository.delete(record);
  }

  private QuoteOrder requireQuote(SysUser user, Long quoteId) {
    return quoteAccessService.requireReadable(user, quoteId);
  }

  private void validateRequest(QuoteFollowUpSaveRequest request) {
    if (request.followStatus() == null || request.followStatus().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "跟进状态不能为空");
    }
    if (request.content() == null || request.content().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "跟进内容不能为空");
    }
  }

  private QuoteFollowUpResponse toResponse(QuoteFollowUp record) {
    return new QuoteFollowUpResponse(
        record.getId(),
        record.getFollowStatus(),
        record.getContent(),
        record.getFollowUpBy(),
        record.getFollowUpByName(),
        QuoteDateTimes.format(record.getFollowUpAt()),
        QuoteDateTimes.format(record.getUpdatedAt()));
  }
}
