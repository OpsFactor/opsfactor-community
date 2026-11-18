package com.opsfactor.community.web.restcontroller;

import com.opsfactor.community.platform.scheduler.facade.TaskSchedulingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/** An administrator querying one job still passes their own identity into the owner-filtered lookup. */
class ProcessStatusTaskLookupTest {

    @AfterEach
    void clearContext() {

        SecurityContextHolder.clearContext();

    }

    @Test
    void exactLookupNeverFallsBackToTheGlobalHistory() {

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "requester", "unused", AuthorityUtils.createAuthorityList("ROLE_ADMIN")));
        TaskSchedulingService service = mock(TaskSchedulingService.class);
        ProcessStatusController controller = new ProcessStatusController();
        ReflectionTestUtils.setField(controller, "taskSchedulingService", service);
        when(service.getTaskSchedulingDTOList("opaque-job-id", "requester")).thenReturn(List.of());

        assertEquals(List.of(), controller.getScheduledTaskHistory("opaque-job-id"));
        verify(service).getTaskSchedulingDTOList("opaque-job-id", "requester");
        verify(service, never()).getTaskSchedulingDTOList();

    }

}
