package org.example.finishedbackend.service.Impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.IOUtils;
import org.example.finishedbackend.entity.DTO.AccountDTO;
import org.example.finishedbackend.entity.DTO.ResourceDTO;
import org.example.finishedbackend.entity.VO.response.ResourceVO;
import org.example.finishedbackend.mapper.AccountMapper;
import org.example.finishedbackend.mapper.ResourceMapper;
import org.example.finishedbackend.service.ResourceService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ResourceServiceImpl extends ServiceImpl<ResourceMapper, ResourceDTO> implements ResourceService {

    @Resource
    MinioClient client;

    @Resource
    AccountMapper accountMapper;

    private final SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");

    @Override
    public String uploadResource(MultipartFile file, String title, String category,
            String description, int uploaderId) {
        String originalFilename = file.getOriginalFilename();
        String objectName = "/resource/" + format.format(new Date()) + "/"
                + UUID.randomUUID().toString().replace("-", "") + "_" + originalFilename;
        PutObjectArgs args;
        try {
            args = PutObjectArgs.builder()
                    .bucket("forum")
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .object(objectName)
                    .build();
            client.putObject(args);
            ResourceDTO dto = new ResourceDTO();
            dto.setTitle(title);
            dto.setCategory(category);
            dto.setFileUrl(objectName);
            dto.setFileName(originalFilename);
            dto.setFileSize(file.getSize());
            dto.setUploaderId(uploaderId);
            dto.setDownloadCount(0);
            dto.setDescription(description);
            dto.setCreateTime(new Date());
            if (this.save(dto)) {
                return "上传成功";
            }
            return null;
        } catch (Exception e) {
            log.error("资源上传失败：{}", e.getMessage());
            return null;
        }
    }

    @Override
    public List<ResourceVO> listResources(int page, String category) {
        Page<ResourceDTO> pageQuery = new Page<>(page, 10);
        if (category != null && !category.isEmpty()) {
            this.page(pageQuery, Wrappers.<ResourceDTO>query().eq("category", category)
                    .orderByDesc("create_time"));
        } else {
            this.page(pageQuery, Wrappers.<ResourceDTO>query().orderByDesc("create_time"));
        }
        return pageQuery.getRecords().stream().map(dto -> {
            AccountDTO account = accountMapper.selectById(dto.getUploaderId());
            return new ResourceVO(
                    dto.getId(),
                    dto.getTitle(),
                    dto.getCategory(),
                    dto.getFileName(),
                    dto.getFileSize(),
                    dto.getDownloadCount(),
                    dto.getDescription(),
                    account != null ? account.getUsername() : "未知用户",
                    account != null ? account.getAvatar() : null,
                    dto.getCreateTime());
        }).toList();
    }

    @Override
    public ResourceDTO downloadResource(int id, OutputStream stream) throws Exception {
        ResourceDTO dto = this.getById(id);
        if (dto == null)
            return null;
        // 增加下载次数
        this.update(Wrappers.<ResourceDTO>update()
                .setSql("download_count = download_count + 1")
                .eq("id", id));
        // 从 MinIO 获取文件流
        GetObjectArgs args = GetObjectArgs.builder()
                .bucket("forum")
                .object(dto.getFileUrl())
                .build();
        GetObjectResponse response = client.getObject(args);
        IOUtils.copy(response, stream);
        return dto;
    }
}
