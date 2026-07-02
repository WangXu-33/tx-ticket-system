package com.rbac.base.modules.realtime.controller;

import cn.dev33.satoken.exception.NotLoginException;
import com.rbac.base.modules.rbac.entity.SysUser;
import com.rbac.base.modules.rbac.mapper.SysUserMapper;
import com.rbac.base.modules.realtime.dto.RealtimeConnection;
import com.rbac.base.modules.realtime.service.RealtimeAuthService;
import com.rbac.base.modules.realtime.service.RealtimeConnectionRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RealtimeSseControllerTest {

    @Test
    void connectRejectsMissingToken() {
        RealtimeSseController controller = new RealtimeSseController(
                mock(RealtimeAuthService.class),
                mock(SysUserMapper.class),
                mock(RealtimeConnectionRegistry.class)
        );

        assertThrows(NotLoginException.class, () -> controller.connect(null));
    }

    @Test
    void connectRegistersAuthenticatedUserWithDepartment() {
        RealtimeAuthService authService = mock(RealtimeAuthService.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        RealtimeConnectionRegistry registry = mock(RealtimeConnectionRegistry.class);
        RealtimeSseController controller = new RealtimeSseController(authService, userMapper, registry);
        SysUser user = new SysUser();
        user.setId(7L);
        user.setDeptId(20L);
        when(authService.resolveUserId("token-1")).thenReturn(7L);
        when(userMapper.selectById(7L)).thenReturn(user);

        SseEmitter emitter = controller.connect("token-1");

        ArgumentCaptor<RealtimeConnection> captor = ArgumentCaptor.forClass(RealtimeConnection.class);
        verify(registry).register(captor.capture());
        RealtimeConnection connection = captor.getValue();
        assertNotNull(emitter);
        assertEquals(emitter, connection.emitter());
        assertEquals(7L, connection.userId());
        assertEquals(20L, connection.deptId());
    }
}
