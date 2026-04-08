package org.example.finishedbackend.service.Impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.finishedbackend.entity.DTO.AccountDTO;
import org.example.finishedbackend.entity.DTO.ResourceDTO;
import org.example.finishedbackend.entity.VO.response.ResourceVO;
import org.example.finishedbackend.mapper.AccountMapper;
import org.example.finishedbackend.mapper.ResourceMapper;
import org.example.finishedbackend.service.ResourceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ResourceServiceImpl extends ServiceImpl<ResourceMapper, ResourceDTO> implements ResourceService {

    @Value("${app.image-storage-path:./uploads}")
    private String storagePath;

    @Resource
    AccountMapper accountMapper;

    private final SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");

    @Override
    public String uploadResource(MultipartFile file, String title, String category,
            String description, int uploaderId) {
        String originalFilename = file.getOriginalFilename();
        String relativePath = "/resource/" + format.format(new Date()) + "/"
                + UUID.randomUUID().toString().replace("-", "") + "_" + originalFilename;
        try {
            Path target = Paths.get(storagePath + relativePath);
            Files.createDirectories(target.getParent());
            file.transferTo(target);

            ResourceDTO dto = new ResourceDTO();
            dto.setTitle(title);
            dto.setCategory(category);
            dto.setFileUrl(relativePath);
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
        this.update(Wrappers.<ResourceDTO>update()
                .setSql("download_count = download_count + 1")
                .eq("id", id));
        Path path = Paths.get(storagePath + dto.getFileUrl());
        Files.copy(path, stream);
        return dto;
    }
}
