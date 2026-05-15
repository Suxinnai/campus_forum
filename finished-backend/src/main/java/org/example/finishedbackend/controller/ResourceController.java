package org.example.finishedbackend.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.example.finishedbackend.entity.DTO.AccountDTO;
import org.example.finishedbackend.entity.DTO.ResourceDTO;
import org.example.finishedbackend.entity.RestBean;
import org.example.finishedbackend.entity.VO.response.ResourceListVO;
import org.example.finishedbackend.entity.VO.response.ResourceVO;
import org.example.finishedbackend.service.AccountService;
import org.example.finishedbackend.service.ResourceService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/resource")
public class ResourceController {

    @Resource
    ResourceService resourceService;

    @Resource
    AccountService accountService;

    /**
     * 上传资源
     */
    @PostMapping("/upload")
    public RestBean<String> upload(@RequestParam("file") MultipartFile file,
            @RequestParam String title,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String description,
            @RequestAttribute("id") int id) {
        if (file.getSize() > 1024 * 1024 * 50) {
            return RestBean.failure(400, "文件大小不允许超过 50MB");
        }
        log.info("用户 {} 正在上传资源: {}", id, title);
        String result = resourceService.uploadResource(file, title, category, description, id);
        return result != null ? RestBean.success(result) : RestBean.failure(400, "资源上传失败");
    }

    /**
     * 分页查询资源列表
     */
    @GetMapping("/list")
    public RestBean<ResourceListVO> list(@RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String category) {
        ResourceListVO list = resourceService.listResources(page, category);
        return RestBean.success(list, "获取资源列表成功");
    }

    /**
     * 下载资源
     */
    @GetMapping("/download/{id}")
    public void download(@PathVariable Integer id, @RequestAttribute("id") int uid, HttpServletResponse response) {
        try {
            log.info("用户 {} 正在下载资源 ID: {}", uid, id);
            ResourceDTO dto = resourceService.getById(id);
            if (dto == null || !canDownload(dto, uid)) {
                response.setStatus(404);
                return;
            }
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition",
                    "attachment;filename=" + URLEncoder.encode(dto.getFileName(), StandardCharsets.UTF_8));
            resourceService.downloadResource(id, uid, response.getOutputStream());
        } catch (Exception e) {
            log.error("资源下载失败：{}", e.getMessage());
            response.setStatus(500);
        }
    }

    @PostMapping("/collect")
    public RestBean<Void> collect(@RequestParam int rid,
            @RequestParam boolean state,
            @RequestAttribute("id") int uid) {
        resourceService.collectResource(rid, uid, state);
        return RestBean.success(null);
    }

    @GetMapping("/is-collected")
    public RestBean<Boolean> isCollected(@RequestParam int rid,
            @RequestAttribute("id") int uid) {
        return RestBean.success(resourceService.hasCollected(rid, uid), null);
    }

    @GetMapping("/my-collects")
    public RestBean<List<ResourceVO>> myCollects(@RequestAttribute("id") int uid) {
        return RestBean.success(resourceService.listCollectedResources(uid), null);
    }

    @PostMapping("/delete")
    public RestBean<Void> delete(@RequestParam int id,
            @RequestAttribute("id") int uid) {
        boolean ok = resourceService.deleteResource(id, uid);
        return ok ? RestBean.success(null) : RestBean.failure(403, "无权删除该资源");
    }

    private boolean canDownload(ResourceDTO dto, int uid) {
        if ("approved".equals(dto.getStatus())) return true;
        if (dto.getUploaderId() != null && dto.getUploaderId() == uid) return true;
        AccountDTO account = accountService.findAccountById(uid);
        return account != null && "admin".equals(account.getRole());
    }
}
