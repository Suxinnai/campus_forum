package org.example.finishedbackend.entity.VO.response;

import lombok.Data;
import java.util.Date;

@Data
public class ReportVO {
    Integer id;
    Integer commentId;
    String commentContent;
    String commentAuthor;
    Integer reporterUid;
    String reporterName;
    String reason;
    String status;
    Date time;
}
