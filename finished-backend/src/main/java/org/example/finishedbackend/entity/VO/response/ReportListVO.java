package org.example.finishedbackend.entity.VO.response;

import lombok.Data;
import java.util.List;

@Data
public class ReportListVO {
    List<ReportVO> reports;
    Integer total;
}
