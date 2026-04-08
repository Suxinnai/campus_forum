package org.example.finishedbackend.entity.VO.response;

import lombok.Data;
import java.util.List;

@Data
public class CommentListVO {
    List<CommentVO> comments;
    Boolean hasMore;
    Integer total;
}
