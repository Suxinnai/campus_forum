package org.example.finishedbackend.service.admin;

import jakarta.annotation.Resource;
import org.example.finishedbackend.entity.DTO.AccountDTO;
import org.example.finishedbackend.entity.DTO.TopicDTO;
import org.example.finishedbackend.service.AccountService;
import org.springframework.stereotype.Service;

@Service
public class AdminGuardService {

    @Resource
    AccountService accountService;

    public AccountDTO currentAccount(int uid) {
        return accountService.findAccountById(uid);
    }

    public boolean isAdmin(int uid) {
        AccountDTO acc = currentAccount(uid);
        return acc != null && "admin".equals(acc.getRole());
    }

    public boolean canManageTopic(int uid, TopicDTO topic) {
        AccountDTO acc = currentAccount(uid);
        if (acc == null) return false;
        String role = acc.getRole();
        if ("admin".equals(role) || "content_admin".equals(role)) return true;
        return "moderator".equals(role)
                && acc.getModeratorType() != null
                && acc.getModeratorType().equals(topic.getType());
    }
}
