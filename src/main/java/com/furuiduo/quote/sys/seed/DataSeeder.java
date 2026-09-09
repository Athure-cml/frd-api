package com.furuiduo.quote.sys.seed;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.furuiduo.quote.sys.PermissionCodes;
import com.furuiduo.quote.sys.entity.DataScope;
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
@Order(50)
public class DataSeeder implements ApplicationRunner {

  /** 新建库默认超级管理员账号（仅 user 表为空时初始化）。 */
  public static final String BOOTSTRAP_ADMIN_USERNAME = "Arture";

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
    if (userRepository.count() > 0) {
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
      SysDepartment department =
          departmentRepository
              .findByCode(row[0])
              .orElseGet(
                  () -> {
                    SysDepartment created = new SysDepartment();
                    created.setCode(row[0]);
                    created.setName(row[1]);
                    created.setSort(Integer.parseInt(row[2]));
                    return departmentRepository.save(created);
                  });
      map.put(row[0], department);
    }
    return map;
  }

  private Map<String, SysPermission> seedPermissions() {
    return PermissionCatalog.ensureAll(permissionRepository);
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
                PermissionCodes.SYS_ANNOUNCEMENT_VIEW,
                PermissionCodes.SYS_ANNOUNCEMENT_MANAGE,
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
        BOOTSTRAP_ADMIN_USERNAME,
        "系统管理员",
        departments.get("GMO"),
        Set.of(roles.get("super_admin")),
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
    if (userRepository.existsByUsername(username)) {
      return;
    }
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
    return roleRepository
        .findByCode(code)
        .orElseGet(
            () -> {
              SysRole role = new SysRole();
              role.setCode(code);
              role.setName(name);
              role.setDataScope(dataScope);
              role.setPermissions(permissionSet);
              return roleRepository.save(role);
            });
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
