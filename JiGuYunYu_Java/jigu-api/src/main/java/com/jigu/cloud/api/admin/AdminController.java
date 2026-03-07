package com.jigu.cloud.api.admin;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台公共入口（预留聚合接口）。
 * <p>
 * 各管理模块已拆分为独立 Controller，此处仅作为公共 prefix 占位。
 * 实际业务请参阅 {@link UserAdminController}、{@link ArtifactAdminController} 等。
 */
@RestController
@RequestMapping(path = "/api/v1/admin", produces = MediaType.APPLICATION_JSON_VALUE)
public class AdminController {

    // 各管理模块已独立拆分，此 Controller 保持最小化。
}
