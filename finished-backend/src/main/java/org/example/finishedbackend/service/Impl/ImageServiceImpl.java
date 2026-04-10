package org.example.finishedbackend.service.Impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.finishedbackend.entity.DTO.AccountDTO;
import org.example.finishedbackend.entity.DTO.AccountDetailsDTO;
import org.example.finishedbackend.entity.DTO.StoreImageDTO;
import org.example.finishedbackend.mapper.AccountDetailsMapper;
import org.example.finishedbackend.mapper.AccountMapper;
import org.example.finishedbackend.mapper.ImageStoreMapper;
import org.example.finishedbackend.service.ImageService;
import org.example.finishedbackend.utils.CacheUtils;
import org.example.finishedbackend.utils.Const;
import org.example.finishedbackend.utils.FlowUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class ImageServiceImpl extends ServiceImpl<ImageStoreMapper, StoreImageDTO> implements ImageService {

    @Value("${app.image-storage-path:./uploads}")
    private String storagePath;

    @Resource
    AccountMapper mapper;

    @Resource
    AccountDetailsMapper detailsMapper;

    @Resource
    FlowUtils flowUtils;

    @Resource
    CacheUtils cacheUtils;

    private final SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");

    @Override
    public String uploadAvatar(MultipartFile file, int id) throws IOException {
        String imageName = UUID.randomUUID().toString().replace("-", "") + ".jpg";
        String relativePath = "/avatar/" + imageName;
        Path target = Paths.get(storagePath + relativePath);
        Files.createDirectories(target.getParent());
        file.transferTo(target);

        String oldAvatar = mapper.selectById(id).getAvatar();
        deleteOldFile(oldAvatar);

        if (mapper.update(null, Wrappers.<AccountDTO>update().set("avatar", relativePath).eq("id", id)) > 0) {
            // 头像更新后，清除帖子预览缓存，确保前端展示最新头像
            cacheUtils.deleteCachePattern(Const.FORUM_TOPIC_PREVIEW_CACHE + "*");
            return relativePath;
        }
        return null;
    }

    @Override
    public String uploadCover(MultipartFile file, int id) throws IOException {
        String imageName = UUID.randomUUID().toString().replace("-", "") + ".jpg";
        String relativePath = "/cover/" + imageName;
        Path target = Paths.get(storagePath + relativePath);
        Files.createDirectories(target.getParent());
        file.transferTo(target);

        AccountDetailsDTO details = detailsMapper.selectById(id);
        if (details == null) {
            details = new AccountDetailsDTO();
            details.setId(id);
            detailsMapper.insert(details);
        } else {
            deleteOldFile(details.getCover());
        }
        details.setCover(relativePath);
        detailsMapper.updateById(details);
        return relativePath;
    }

    @Override
    public String uploadImage(MultipartFile file, int id) throws IOException {
        String key = Const.FORUM_IMAGE_COUNTER + id;
        if (!flowUtils.limitPeriodCounterCheck(key, 20, 3600))
            return null;

        String imageName = UUID.randomUUID().toString().replace("-", "") + ".jpg";
        Date date = new Date();
        String relativePath = "/cache/" + format.format(date) + "/" + imageName;
        Path target = Paths.get(storagePath + relativePath);
        Files.createDirectories(target.getParent());
        file.transferTo(target);

        if (this.save(new StoreImageDTO(relativePath, id, date))) {
            return relativePath;
        }
        return null;
    }

    @Override
    public void fetchImage(OutputStream stream, String image) throws Exception {
        Path path = Paths.get(storagePath + image);
        if (!Files.exists(path)) {
            throw new FileNotFoundException("图片不存在: " + image);
        }
        Files.copy(path, stream);
    }

    private void deleteOldFile(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) return;
        try {
            Files.deleteIfExists(Paths.get(storagePath + relativePath));
        } catch (IOException e) {
            log.warn("删除旧头像失败: {}", e.getMessage());
        }
    }
}
