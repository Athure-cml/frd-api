package com.furuiduo.quote.sys.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.sys.dto.DepartmentResponse;
import com.furuiduo.quote.sys.dto.DepartmentSaveRequest;
import com.furuiduo.quote.sys.entity.SysDepartment;
import com.furuiduo.quote.sys.repository.SysDepartmentRepository;
import com.furuiduo.quote.sys.repository.SysUserRepository;

@Service
public class SysDepartmentCommandService {

  private final SysDepartmentRepository departmentRepository;
  private final SysUserRepository userRepository;

  public SysDepartmentCommandService(
      SysDepartmentRepository departmentRepository, SysUserRepository userRepository) {
    this.departmentRepository = departmentRepository;
    this.userRepository = userRepository;
  }

  @Transactional
  public DepartmentResponse create(DepartmentSaveRequest request) {
    if (departmentRepository.existsByCode(request.code())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Department code already exists");
    }
    SysDepartment department = new SysDepartment();
    apply(department, request);
    return DepartmentResponse.from(departmentRepository.save(department));
  }

  @Transactional
  public DepartmentResponse update(Long id, DepartmentSaveRequest request) {
    SysDepartment department =
        departmentRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found"));
    if (!department.getCode().equals(request.code())
        && departmentRepository.existsByCode(request.code())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Department code already exists");
    }
    apply(department, request);
    return DepartmentResponse.from(departmentRepository.save(department));
  }

  @Transactional
  public void delete(Long id) {
    SysDepartment department =
        departmentRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found"));
    if (userRepository.countByDepartment_Id(id) > 0) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Cannot delete department with assigned users");
    }
    departmentRepository.delete(department);
  }

  public DepartmentResponse getById(Long id) {
    return departmentRepository
        .findById(id)
        .map(DepartmentResponse::from)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found"));
  }

  private void apply(SysDepartment department, DepartmentSaveRequest request) {
    department.setCode(request.code().trim());
    department.setName(request.name().trim());
    department.setParentId(request.parentId() == null ? 0L : request.parentId());
    department.setSort(request.sort());
    department.setStatus(request.status());
  }
}
