package com.deliveryinsider.auth.admin.service;

import com.deliveryinsider.auth.admin.response.AdminUserPageResponse;
import com.deliveryinsider.auth.admin.response.AdminUserResponse;
import com.deliveryinsider.auth.admin.response.AdminUserSummaryResponse;
import com.deliveryinsider.auth.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserReadService {
    private final UserMapper userMapper;

    public AdminUserSummaryResponse summary() {
        return new AdminUserSummaryResponse(
            userMapper.countAll(),
            userMapper.countByStatus("ACTIVE"),
            userMapper.countPhoneVerified(),
            userMapper.countByRole("ADMIN")
        );
    }

    public AdminUserPageResponse findAll(int page, int size) {
        var items = userMapper.findAdminPage(size, page * size)
            .stream()
            .map(AdminUserResponse::from)
            .toList();
        return new AdminUserPageResponse(items, userMapper.countAll(), page, size);
    }
}
