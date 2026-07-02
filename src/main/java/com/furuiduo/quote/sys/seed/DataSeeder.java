package com.furuiduo.quote.sys.seed;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.furuiduo.quote.sys.PermissionCodes;
import com.furuiduo.quote.sys.entity.DataScope;
import com.furuiduo.quote.sys.entity.PermissionType;
import com.furuiduo.quote.sys.entity.SysDepartment;
import com.furuiduo.quote.sys.entity.SysPermission;
import com.furuiduo.quote.sys.entity.SysRole;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.repository.SysDepartmentRepository;
import com.furuiduo.quote.sys.repository.SysPermissionRepository;
import com.furuiduo.quote.sys.repository.SysRoleRepository;
import com.furuiduo.quote.sys.repository.SysUserRepository;
import com.furuiduo.quote.user.PasswordStrengthEvaluator;

@Component
public class DataSeeder implements ApplicationRunner {

  private static final String DEFAULT_AVATAR =
      "https://unpkg.com/@vbenjs/static-source@0.1.7/source/avatar-v1.webp";

  private final SysDepartmentRepository departmentRepository;
  private final SysPermissionRepository permissionRepository;
  private final SysRoleRepository roleRepository;
  private final SysUserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public DataSeeder(
      SysDepartmentRepository departmentRepository,
      SysPermissionRepository permissionRepository,
      SysRoleRepository roleRepository,
      SysUserRepository userRepository,
      PasswordEncoder passwordEncoder) {
    this.departmentRepository = departmentRepository;
    this.permissionRepository = permissionRepository;
    this.roleRepository = roleRepository;
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (departmentRepository.count() > 0) {
      return;
    }

    Map<String, SysDepartment> departments = seedDepartments();
    Map<String, SysPermission> permissions = seedPermissions();
    Map<String, SysRole> roles = seedRoles(permissions);
    seedUsers(departments, roles);
  }

  private Map<String, SysDepartment> seedDepartments() {
    List<String[]> rows =
        List.of(
            new String[] {"CS", "客服部", "1"},
            new String[] {"DOC", "单证部", "2"},
            new String[] {"OPS", "海外操作部", "3"},
            new String[] {"BKG", "订舱部", "4"},
            new String[] {"FIN", "财务部", "5"},
            new String[] {"TECH", "技术部", "6"},
            new String[] {"GMO", "总经办", "7"});

    Map<String, SysDepartment> map = new LinkedHashMap<>();
    for (String[] row : rows) {
      SysDepartment department = new SysDepartment();
      department.setCode(row[0]);
      department.setName(row[1]);
      department.setSort(Integer.parseInt(row[2]));
      map.put(row[0], departmentRepository.save(department));
    }
    return map;
  }

  private Map<String, SysPermission> seedPermissions() {
    record PermDef(String code, String name, PermissionType type, int sort) {}

    List<PermDef> defs =
        List.of(
            new PermDef(PermissionCodes.SYS_DEPT_VIEW, "查看部门", PermissionType.API, 10),
            new PermDef(PermissionCodes.SYS_DEPT_MANAGE, "管理部门", PermissionType.API, 11),
            new PermDef(PermissionCodes.SYS_USER_VIEW, "查看用户", PermissionType.API, 12),
            new PermDef(PermissionCodes.SYS_USER_MANAGE, "管理用户", PermissionType.API, 13),
            new PermDef(PermissionCodes.SYS_ROLE_VIEW, "查看角色", PermissionType.API, 14),
            new PermDef(PermissionCodes.SYS_ROLE_MANAGE, "管理角色", PermissionType.API, 15),
            new PermDef(
                PermissionCodes.SYS_OPERATION_LOG_VIEW, "操作日志-查看", PermissionType.API, 16),
            new PermDef(PermissionCodes.DASHBOARD_VIEW, "报价分析", PermissionType.MENU, 20),
            new PermDef(PermissionCodes.COST_ROAD_VIEW, "卡车成本-查看", PermissionType.API, 30),
            new PermDef(PermissionCodes.COST_ROAD_EDIT, "卡车成本-编辑", PermissionType.API, 31),
            new PermDef(PermissionCodes.COST_SEA_VIEW, "海运成本-查看", PermissionType.API, 32),
            new PermDef(PermissionCodes.COST_SEA_EDIT, "海运成本-编辑", PermissionType.API, 33),
            new PermDef(PermissionCodes.COST_FUMIGATION_VIEW, "熏蒸成本-查看", PermissionType.API, 34),
            new PermDef(PermissionCodes.COST_FUMIGATION_EDIT, "熏蒸成本-编辑", PermissionType.API, 35),
            new PermDef(
                PermissionCodes.COST_ROAD_TEMPLATE_VIEW, "卡车模板-查看", PermissionType.API, 36),
            new PermDef(
                PermissionCodes.COST_ROAD_TEMPLATE_EDIT, "卡车模板-编辑", PermissionType.API, 37),
            new PermDef(
                PermissionCodes.COST_ROAD_TEMPLATE_DELETE, "卡车模板-删除", PermissionType.API, 38),
            new PermDef(
                PermissionCodes.COST_SEA_TEMPLATE_VIEW, "海运模板-查看", PermissionType.API, 39),
            new PermDef(
                PermissionCodes.COST_SEA_TEMPLATE_EDIT, "海运模板-编辑", PermissionType.API, 40),
            new PermDef(
                PermissionCodes.COST_SEA_TEMPLATE_DELETE, "海运模板-删除", PermissionType.API, 41),
            new PermDef(
                PermissionCodes.COST_FUMIGATION_TEMPLATE_VIEW, "熏蒸模板-查看", PermissionType.API, 42),
            new PermDef(
                PermissionCodes.COST_FUMIGATION_TEMPLATE_EDIT, "熏蒸模板-编辑", PermissionType.API, 43),
            new PermDef(
                PermissionCodes.COST_FUMIGATION_TEMPLATE_DELETE, "熏蒸模板-删除", PermissionType.API, 44),
            new PermDef(PermissionCodes.QUOTE_VIEW, "报价单-查看", PermissionType.API, 50),
            new PermDef(PermissionCodes.QUOTE_CREATE, "报价单-新建", PermissionType.API, 51),
            new PermDef(PermissionCodes.QUOTE_EDIT, "报价单-编辑", PermissionType.API, 52),
            new PermDef(PermissionCodes.QUOTE_SUBMIT, "报价单-提交", PermissionType.API, 53),
            new PermDef(PermissionCodes.QUOTE_APPROVE, "报价单-审批", PermissionType.API, 54),
            new PermDef(PermissionCodes.QUOTE_EXPORT, "报价单-导出", PermissionType.API, 55),
            new PermDef(PermissionCodes.QUOTE_DELETE, "报价单-删除", PermissionType.API, 56),
            new PermDef(PermissionCodes.CUSTOMER_VIEW, "客户-查看", PermissionType.API, 45),
            new PermDef(PermissionCodes.CUSTOMER_CREATE, "客户-新建", PermissionType.API, 46),
            new PermDef(PermissionCodes.CUSTOMER_EDIT, "客户-编辑", PermissionType.API, 47),
            new PermDef(PermissionCodes.CUSTOMER_DELETE, "客户-删除", PermissionType.API, 48),
            new PermDef(PermissionCodes.CURRENCY_VIEW, "币种-查看", PermissionType.API, 49),
            new PermDef(PermissionCodes.CURRENCY_MANAGE, "币种-管理", PermissionType.API, 50),
            new PermDef(PermissionCodes.EXCHANGE_RATE_VIEW, "汇率-查看", PermissionType.API, 51),
            new PermDef(PermissionCodes.EXCHANGE_RATE_MANAGE, "汇率-管理", PermissionType.API, 52),
            new PermDef(PermissionCodes.REPORT_VIEW, "报表-查看", PermissionType.API, 60),
            new PermDef(PermissionCodes.REPORT_EXPORT, "报表-导出", PermissionType.API, 61));

    Map<String, SysPermission> map = new LinkedHashMap<>();
    for (PermDef def : defs) {
      SysPermission permission = new SysPermission();
      permission.setCode(def.code());
      permission.setName(def.name());
      permission.setType(def.type());
      permission.setSort(def.sort());
      map.put(def.code(), permissionRepository.save(permission));
    }
    return map;
  }

  private Map<String, SysRole> seedRoles(Map<String, SysPermission> permissions) {
    Map<String, SysRole> roles = new LinkedHashMap<>();

    roles.put(
        "super_admin",
        saveRole(
            "super_admin",
            "超级管理员",
            DataScope.ALL,
            allPermissionCodes(permissions)));
    roles.put(
        "admin",
        saveRole(
            "admin",
            "系统管理员",
            DataScope.ALL,
            codes(
                permissions,
                PermissionCodes.SYS_DEPT_VIEW,
                PermissionCodes.SYS_USER_VIEW,
                PermissionCodes.SYS_USER_MANAGE,
                PermissionCodes.SYS_ROLE_VIEW,
                PermissionCodes.SYS_OPERATION_LOG_VIEW,
                PermissionCodes.DASHBOARD_VIEW,
                PermissionCodes.COST_ROAD_VIEW,
                PermissionCodes.COST_ROAD_EDIT,
                PermissionCodes.COST_SEA_VIEW,
                PermissionCodes.COST_SEA_EDIT,
                PermissionCodes.COST_FUMIGATION_VIEW,
                PermissionCodes.COST_FUMIGATION_EDIT,
                PermissionCodes.COST_ROAD_TEMPLATE_VIEW,
                PermissionCodes.COST_ROAD_TEMPLATE_EDIT,
                PermissionCodes.COST_ROAD_TEMPLATE_DELETE,
                PermissionCodes.COST_SEA_TEMPLATE_VIEW,
                PermissionCodes.COST_SEA_TEMPLATE_EDIT,
                PermissionCodes.COST_SEA_TEMPLATE_DELETE,
                PermissionCodes.COST_FUMIGATION_TEMPLATE_VIEW,
                PermissionCodes.COST_FUMIGATION_TEMPLATE_EDIT,
                PermissionCodes.COST_FUMIGATION_TEMPLATE_DELETE,
                PermissionCodes.QUOTE_VIEW,
                PermissionCodes.QUOTE_CREATE,
                PermissionCodes.QUOTE_EDIT,
                PermissionCodes.QUOTE_SUBMIT,
                PermissionCodes.QUOTE_EXPORT,
                PermissionCodes.CUSTOMER_VIEW,
                PermissionCodes.CUSTOMER_CREATE,
                PermissionCodes.CUSTOMER_EDIT,
                PermissionCodes.CUSTOMER_DELETE,
                PermissionCodes.CURRENCY_VIEW,
                PermissionCodes.CURRENCY_MANAGE,
                PermissionCodes.EXCHANGE_RATE_VIEW,
                PermissionCodes.EXCHANGE_RATE_MANAGE,
                PermissionCodes.REPORT_VIEW,
                PermissionCodes.REPORT_EXPORT)));
    roles.put(
        "dept_manager",
        saveRole(
            "dept_manager",
            "部门主管",
            DataScope.DEPT,
            codes(
                permissions,
                PermissionCodes.DASHBOARD_VIEW,
                PermissionCodes.COST_ROAD_VIEW,
                PermissionCodes.COST_SEA_VIEW,
                PermissionCodes.COST_FUMIGATION_VIEW,
                PermissionCodes.COST_ROAD_TEMPLATE_VIEW,
                PermissionCodes.COST_SEA_TEMPLATE_VIEW,
                PermissionCodes.COST_FUMIGATION_TEMPLATE_VIEW,
                PermissionCodes.QUOTE_VIEW,
                PermissionCodes.QUOTE_CREATE,
                PermissionCodes.QUOTE_EDIT,
                PermissionCodes.QUOTE_SUBMIT,
                PermissionCodes.QUOTE_APPROVE,
                PermissionCodes.QUOTE_EXPORT,
                PermissionCodes.CUSTOMER_VIEW,
                PermissionCodes.CUSTOMER_CREATE,
                PermissionCodes.CUSTOMER_EDIT,
                PermissionCodes.CUSTOMER_DELETE,
                PermissionCodes.CURRENCY_VIEW,
                PermissionCodes.EXCHANGE_RATE_VIEW,
                PermissionCodes.REPORT_VIEW)));
    roles.put(
        "sales",
        saveRole(
            "sales",
            "销售/客服",
            DataScope.SELF,
            codes(
                permissions,
                PermissionCodes.DASHBOARD_VIEW,
                PermissionCodes.COST_ROAD_VIEW,
                PermissionCodes.COST_SEA_VIEW,
                PermissionCodes.COST_FUMIGATION_VIEW,
                PermissionCodes.COST_ROAD_TEMPLATE_VIEW,
                PermissionCodes.COST_SEA_TEMPLATE_VIEW,
                PermissionCodes.COST_FUMIGATION_TEMPLATE_VIEW,
                PermissionCodes.QUOTE_VIEW,
                PermissionCodes.QUOTE_CREATE,
                PermissionCodes.QUOTE_EDIT,
                PermissionCodes.QUOTE_SUBMIT,
                PermissionCodes.QUOTE_EXPORT,
                PermissionCodes.CUSTOMER_VIEW,
                PermissionCodes.CUSTOMER_CREATE,
                PermissionCodes.CUSTOMER_EDIT,
                PermissionCodes.CUSTOMER_DELETE,
                PermissionCodes.CURRENCY_VIEW,
                PermissionCodes.EXCHANGE_RATE_VIEW)));
    roles.put(
        "doc_clerk",
        saveRole(
            "doc_clerk",
            "单证员",
            DataScope.DEPT,
            codes(
                permissions,
                PermissionCodes.DASHBOARD_VIEW,
                PermissionCodes.COST_ROAD_VIEW,
                PermissionCodes.COST_SEA_VIEW,
                PermissionCodes.COST_FUMIGATION_VIEW,
                PermissionCodes.COST_ROAD_TEMPLATE_VIEW,
                PermissionCodes.COST_SEA_TEMPLATE_VIEW,
                PermissionCodes.COST_FUMIGATION_TEMPLATE_VIEW,
                PermissionCodes.QUOTE_VIEW,
                PermissionCodes.QUOTE_EXPORT,
                PermissionCodes.CUSTOMER_VIEW,
                PermissionCodes.CURRENCY_VIEW,
                PermissionCodes.EXCHANGE_RATE_VIEW)));
    roles.put(
        "overseas_operator",
        saveRole(
            "overseas_operator",
            "海外操作",
            DataScope.DEPT,
            codes(
                permissions,
                PermissionCodes.DASHBOARD_VIEW,
                PermissionCodes.COST_ROAD_VIEW,
                PermissionCodes.COST_SEA_VIEW,
                PermissionCodes.COST_FUMIGATION_VIEW,
                PermissionCodes.COST_ROAD_TEMPLATE_VIEW,
                PermissionCodes.COST_SEA_TEMPLATE_VIEW,
                PermissionCodes.COST_FUMIGATION_TEMPLATE_VIEW,
                PermissionCodes.QUOTE_VIEW,
                PermissionCodes.CUSTOMER_VIEW,
                PermissionCodes.CURRENCY_VIEW,
                PermissionCodes.EXCHANGE_RATE_VIEW)));
    roles.put(
        "booker",
        saveRole(
            "booker",
            "订舱员",
            DataScope.DEPT,
            codes(
                permissions,
                PermissionCodes.DASHBOARD_VIEW,
                PermissionCodes.COST_ROAD_VIEW,
                PermissionCodes.COST_SEA_VIEW,
                PermissionCodes.COST_SEA_EDIT,
                PermissionCodes.COST_FUMIGATION_VIEW,
                PermissionCodes.COST_FUMIGATION_EDIT,
                PermissionCodes.COST_ROAD_TEMPLATE_VIEW,
                PermissionCodes.COST_SEA_TEMPLATE_VIEW,
                PermissionCodes.COST_SEA_TEMPLATE_EDIT,
                PermissionCodes.COST_SEA_TEMPLATE_DELETE,
                PermissionCodes.COST_FUMIGATION_TEMPLATE_VIEW,
                PermissionCodes.COST_FUMIGATION_TEMPLATE_EDIT,
                PermissionCodes.COST_FUMIGATION_TEMPLATE_DELETE,
                PermissionCodes.QUOTE_VIEW)));
    roles.put(
        "finance",
        saveRole(
            "finance",
            "财务",
            DataScope.ALL,
            codes(
                permissions,
                PermissionCodes.DASHBOARD_VIEW,
                PermissionCodes.COST_ROAD_VIEW,
                PermissionCodes.COST_SEA_VIEW,
                PermissionCodes.COST_FUMIGATION_VIEW,
                PermissionCodes.COST_ROAD_TEMPLATE_VIEW,
                PermissionCodes.COST_SEA_TEMPLATE_VIEW,
                PermissionCodes.COST_FUMIGATION_TEMPLATE_VIEW,
                PermissionCodes.QUOTE_VIEW,
                PermissionCodes.QUOTE_APPROVE,
                PermissionCodes.QUOTE_EXPORT,
                PermissionCodes.CURRENCY_VIEW,
                PermissionCodes.CURRENCY_MANAGE,
                PermissionCodes.EXCHANGE_RATE_VIEW,
                PermissionCodes.EXCHANGE_RATE_MANAGE,
                PermissionCodes.REPORT_VIEW,
                PermissionCodes.REPORT_EXPORT)));
    roles.put(
        "viewer",
        saveRole(
            "viewer",
            "只读",
            DataScope.DEPT,
            codes(
                permissions,
                PermissionCodes.DASHBOARD_VIEW,
                PermissionCodes.COST_ROAD_VIEW,
                PermissionCodes.COST_SEA_VIEW,
                PermissionCodes.COST_FUMIGATION_VIEW,
                PermissionCodes.COST_ROAD_TEMPLATE_VIEW,
                PermissionCodes.COST_SEA_TEMPLATE_VIEW,
                PermissionCodes.COST_FUMIGATION_TEMPLATE_VIEW,
                PermissionCodes.QUOTE_VIEW,
                PermissionCodes.CURRENCY_VIEW,
                PermissionCodes.EXCHANGE_RATE_VIEW)));

    return roles;
  }

  private void seedUsers(Map<String, SysDepartment> departments, Map<String, SysRole> roles) {
    String rawPassword = "123456";
    String encodedPassword = passwordEncoder.encode(rawPassword);

    createUser(
        "vben",
        "系统管理员",
        departments.get("CS"),
        Set.of(roles.get("super_admin")),
        encodedPassword,
        rawPassword);
    createUser(
        "cs001", "客服-张三", departments.get("CS"), Set.of(roles.get("sales")), encodedPassword, rawPassword);
    createUser(
        "doc001",
        "单证-李四",
        departments.get("DOC"),
        Set.of(roles.get("doc_clerk")),
        encodedPassword,
        rawPassword);
    createUser(
        "ops001",
        "海外-王五",
        departments.get("OPS"),
        Set.of(roles.get("overseas_operator")),
        encodedPassword,
        rawPassword);
    createUser(
        "bkg001",
        "订舱-赵六",
        departments.get("BKG"),
        Set.of(roles.get("booker")),
        encodedPassword,
        rawPassword);
    createUser(
        "fin001",
        "财务-钱七",
        departments.get("FIN"),
        Set.of(roles.get("finance")),
        encodedPassword,
        rawPassword);
  }

  private void createUser(
      String username,
      String realName,
      SysDepartment department,
      Set<SysRole> userRoles,
      String encodedPassword,
      String rawPassword) {
    SysUser user = new SysUser();
    user.setUsername(username);
    user.setRealName(realName);
    user.setPasswordHash(encodedPassword);
    PasswordStrengthEvaluator.apply(user, rawPassword);
    user.setAvatar(DEFAULT_AVATAR);
    user.setHomePath("/workspace");
    user.setDepartment(department);
    user.setRoles(new HashSet<>(userRoles));
    userRepository.save(user);
  }

  private SysRole saveRole(
      String code, String name, DataScope dataScope, Set<SysPermission> permissionSet) {
    SysRole role = new SysRole();
    role.setCode(code);
    role.setName(name);
    role.setDataScope(dataScope);
    role.setPermissions(permissionSet);
    return roleRepository.save(role);
  }

  private Set<SysPermission> allPermissionCodes(Map<String, SysPermission> permissions) {
    return new HashSet<>(permissions.values());
  }

  private Set<SysPermission> codes(Map<String, SysPermission> permissions, String... codes) {
    return Arrays.stream(codes)
        .map(permissions::get)
        .collect(java.util.stream.Collectors.toCollection(HashSet::new));
  }
}
