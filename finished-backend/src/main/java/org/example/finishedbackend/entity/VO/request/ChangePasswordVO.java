package org.example.finishedbackend.entity.VO.request;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class ChangePasswordVO {
    @Length(min = 6)
    private String password;
    @Length(min = 6)
    private String newPassword;
}
