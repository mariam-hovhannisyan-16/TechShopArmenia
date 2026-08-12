package am.techshop.common.event;

import java.util.List;

public record AdminUserRegisteredEvent(
        String newUserName,
        String newUserEmail,
        List<Long> adminIds
) {}
