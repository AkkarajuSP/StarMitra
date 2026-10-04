package com.starmitra.modules.identity.application;

import com.starmitra.modules.identity.persistence.SystemRoleRepository;
import com.starmitra.modules.identity.persistence.UserSystemRoleEntity;
import com.starmitra.modules.identity.persistence.UserSystemRoleRepository;
import com.starmitra.platform.error.ApiException;
import com.starmitra.platform.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SystemRoleService implements SystemRoleContract {

    private final SystemRoleRepository roles;
    private final UserSystemRoleRepository userRoles;

    public SystemRoleService(SystemRoleRepository roles, UserSystemRoleRepository userRoles) {
        this.roles = roles;
        this.userRoles = userRoles;
    }

    @Override
    @Transactional
    public void grantSystemRole(UUID userId, String roleName) {
        var role = roles.findByName(roleName)
                .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_FAILED, "Unknown system role"));
        var key = new UserSystemRoleEntity.Pk(userId, role.getId());
        if (!userRoles.existsById(key)) {
            userRoles.save(new UserSystemRoleEntity(userId, role.getId()));
        }
    }
}
