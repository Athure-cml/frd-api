package com.furuiduo.quote.sys.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "角色保存请求")
public record RoleSaveRequest(
    @NotBlank @Size(max = 64) @Schema(description = "角色编码") String code,
    @NotBlank @Size(max = 64) @Schema(description = "角色名称") String name,
    @NotBlank @Schema(description = "数据权限：ALL|DEPT|SELF") String dataScope,
    @NotNull @Schema(description = "状态：1 启用，0 停用") Integer status,
    @Size(max = 255) @Schema(description = "备注") String remark,
    @Schema(description = "权限码列表") List<String> permissionCodes) {}
