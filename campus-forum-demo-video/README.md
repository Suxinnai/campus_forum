# Campus Forum Demo Video

这是基于当前 `campus_forum` 系统制作的 5 分钟 Remotion 演示视频工程。

## 内容结构

- 00:00-00:22 系统定位与整体入口
- 00:22-01:00 架构与能力边界
- 01:00-01:36 注册、登录与角色入口
- 01:36-02:20 社区首页与帖子详情
- 02:20-03:02 互动链路与内容沉淀
- 03:02-03:40 校园活动、资源与快捷入口
- 03:40-04:34 管理后台与治理闭环
- 04:34-05:00 总结收尾

## 使用素材

视频复用了主项目 `docs/images` 中的真实系统截图，并已复制到本项目 `public` 目录，供 Remotion 通过 `staticFile()` 引用。

## 命令

```bash
npm run dev
npm run still
npm run render
npm run lint
```

渲染输出路径：

```text
out/campus-forum-demo.mp4
```

当前版本为无配音演示片，画面内置中文字幕和章节进度，适合答辩展示或后续叠加真人讲解音轨。
