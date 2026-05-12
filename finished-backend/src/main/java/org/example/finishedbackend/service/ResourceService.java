package org.example.finishedbackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.finishedbackend.entity.DTO.ResourceDTO;
import org.example.finishedbackend.entity.VO.response.ResourceListVO;
import org.example.finishedbackend.entity.VO.response.ResourceVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
import java.util.List;

public interface ResourceService extends IService<ResourceDTO> {
    /**
     * 上传资源文件
     */
    String uploadResource(MultipartFile file, String title, String category,
            String description, int uploaderId);

    /**
     * 分页查询资源列表
     */
    ResourceListVO listResources(int page, String category);

    /**
     * 下载资源（将文件流写入 OutputStream）
     */
    ResourceDTO downloadResource(int id, OutputStream stream) throws Exception;

    void collectResource(int rid, int uid, boolean state);
    boolean hasCollected(int rid, int uid);
    List<ResourceVO> listCollectedResources(int uid);
    boolean deleteResource(int rid, int uid);
}
