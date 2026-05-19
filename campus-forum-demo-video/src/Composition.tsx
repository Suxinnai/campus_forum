import type { CSSProperties } from "react";
import {
  AbsoluteFill,
  Easing,
  Img,
  Sequence,
  interpolate,
  staticFile,
  useCurrentFrame,
} from "remotion";

export const VIDEO_FPS = 30;
export const VIDEO_DURATION_SECONDS = 300;
export const VIDEO_DURATION_IN_FRAMES = VIDEO_DURATION_SECONDS * VIDEO_FPS;

type SceneLayout =
  | "cover"
  | "architecture"
  | "single"
  | "dual"
  | "workflow"
  | "admin"
  | "closing";

type Metric = {
  label: string;
  value: string;
};

type Scene = {
  key: string;
  durationSeconds: number;
  eyebrow: string;
  title: string;
  subtitle: string;
  bullets: string[];
  script: string[];
  images: string[];
  layout: SceneLayout;
  accent: string;
  soft: string;
  metrics?: Metric[];
};

const scenes: Scene[] = [
  {
    key: "opening",
    durationSeconds: 22,
    eyebrow: "系统演示 00",
    title: "校园论坛系统",
    subtitle: "面向学生交流、校园生活与内容治理的一体化社区平台",
    bullets: ["学生端高频互动", "活动与资源聚合", "多角色后台治理"],
    script: [
      "本片用五分钟展示校园论坛系统的核心体验。",
      "系统围绕学生交流、校园活动、资源共享和后台管理建立完整闭环。",
    ],
    images: ["login.png", "home.png", "admin_overview.png"],
    layout: "cover",
    accent: "#0f766e",
    soft: "#dff7f1",
    metrics: [
      { value: "Vue 3", label: "前端交互" },
      { value: "Spring Boot 3", label: "服务端能力" },
      { value: "JWT", label: "身份与权限" },
    ],
  },
  {
    key: "architecture",
    durationSeconds: 38,
    eyebrow: "系统演示 01",
    title: "架构与能力边界",
    subtitle: "前后端分离，围绕论坛内容、活动、资源和治理模块组织 API",
    bullets: [
      "Vue 3 + Vite + Pinia 承载学生端与管理端",
      "Spring Security + JWT 处理登录态、路由与接口权限",
      "MyBatis Plus 访问 MySQL，Redis 支撑缓存与限流",
      "服务层按账号、帖子、评论、活动、资源和通知拆分",
    ],
    script: [
      "系统采用前后端分离架构，前端负责路由、状态和界面交互。",
      "后端以 Spring Boot 3 组织 REST API，并通过 JWT 统一识别用户身份。",
      "业务数据落在 MySQL，Redis 参与缓存和限流，降低高频访问压力。",
      "模块边界清晰，便于后续扩展更多校园服务。",
    ],
    images: [],
    layout: "architecture",
    accent: "#2563eb",
    soft: "#e0ecff",
    metrics: [
      { value: "10+", label: "业务控制器" },
      { value: "3 级", label: "后台角色" },
      { value: "REST", label: "接口风格" },
    ],
  },
  {
    key: "auth",
    durationSeconds: 36,
    eyebrow: "系统演示 02",
    title: "注册、登录与角色入口",
    subtitle: "从学生身份进入社区，并在路由层区分普通用户和管理用户",
    bullets: [
      "登录页提供清晰的校园视觉入口",
      "支持注册、忘记密码与会话持久化",
      "学生登录后进入社区首页",
      "管理员角色可进入独立后台工作区",
    ],
    script: [
      "用户首先进入登录页面，系统以简洁的校园视觉建立第一印象。",
      "登录成功后，令牌会写入本地存储，路由守卫据此放行学生端页面。",
      "当账号具有管理员、内容管理员或版主身份时，可以进入后台工作台。",
    ],
    images: ["login.png"],
    layout: "single",
    accent: "#7c3aed",
    soft: "#efe7ff",
    metrics: [
      { value: "注册", label: "账号创建" },
      { value: "重置", label: "密码恢复" },
      { value: "守卫", label: "访问控制" },
    ],
  },
  {
    key: "community",
    durationSeconds: 44,
    eyebrow: "系统演示 03",
    title: "社区首页与帖子详情",
    subtitle: "围绕话题流、标签筛选、Markdown 内容和实时互动形成核心体验",
    bullets: [
      "话题列表支持分类、置顶与卡片化浏览",
      "详情页展示正文、作者信息、评论和互动状态",
      "点赞、收藏、评论会同步写入用户行为",
      "推荐服务可利用浏览、点赞和评论行为优化内容分发",
    ],
    script: [
      "学生进入首页后，可以按分类浏览帖子，也能看到被置顶或高热度内容。",
      "帖子详情页支持富文本和 Markdown 展示，适合技术讨论、活动通知和学习分享。",
      "点赞、收藏与评论不仅改变页面状态，也会沉淀为行为数据。",
      "这些行为数据为后续的个性化推荐和通知提供基础。",
    ],
    images: ["home.png", "post_detail.png"],
    layout: "dual",
    accent: "#0284c7",
    soft: "#dff4ff",
    metrics: [
      { value: "Markdown", label: "内容表达" },
      { value: "评论", label: "交流反馈" },
      { value: "推荐", label: "行为闭环" },
    ],
  },
  {
    key: "interaction",
    durationSeconds: 42,
    eyebrow: "系统演示 04",
    title: "互动链路与内容沉淀",
    subtitle: "从浏览到互动，再到通知与推荐，系统记录完整社区行为",
    bullets: [
      "浏览帖子时记录 view 行为",
      "点赞与收藏通过统一 interact 接口切换状态",
      "评论写入后可触发更高权重的行为记录",
      "被点赞用户会收到通知，形成站内反馈",
    ],
    script: [
      "帖子互动采用统一接口处理，前端只需要传入帖子、类型和状态。",
      "服务端根据行为类型写入记录，并在必要时触发通知。",
      "对学生来说，这是自然的社区反馈；对系统来说，这是可分析的数据资产。",
    ],
    images: ["post_detail.png"],
    layout: "workflow",
    accent: "#db2777",
    soft: "#ffe4f1",
    metrics: [
      { value: "view", label: "浏览行为" },
      { value: "like", label: "点赞通知" },
      { value: "comment", label: "高权重反馈" },
    ],
  },
  {
    key: "campus-life",
    durationSeconds: 38,
    eyebrow: "系统演示 05",
    title: "校园活动、资源与快捷入口",
    subtitle: "把校园生活服务聚合到同一个社区入口，减少信息分散",
    bullets: [
      "活动中心展示时间、地点、组织方和名额",
      "活动详情以弹窗呈现，降低跳转成本",
      "资源模块支持课程资料与共享文件入口",
      "快捷入口、统计页面和反馈页补足日常使用场景",
    ],
    script: [
      "除了帖子交流，系统还承载校园活动和资源共享。",
      "活动列表突出时间、地点和人数限制，学生可以快速判断是否参与。",
      "资源、快捷入口、统计和反馈模块，让论坛从讨论区延伸为校园服务入口。",
    ],
    images: ["activity_detail.png", "banner.png"],
    layout: "dual",
    accent: "#ea580c",
    soft: "#ffead7",
    metrics: [
      { value: "活动", label: "报名场景" },
      { value: "资源", label: "资料共享" },
      { value: "反馈", label: "持续改进" },
    ],
  },
  {
    key: "admin",
    durationSeconds: 54,
    eyebrow: "系统演示 06",
    title: "管理后台与治理闭环",
    subtitle: "为管理员、内容管理员和版主提供可分工的内容治理能力",
    bullets: [
      "数据总览聚合用户、帖子、评论和敏感词统计",
      "用户管理支持角色调整、封禁和批量处理",
      "帖子管理支持审核、置顶、精华和删除",
      "公告、校历、活动、资源、反馈和敏感词统一维护",
    ],
    script: [
      "后台是系统长期运行的保障，它把数据观察和日常治理放在同一个工作区。",
      "超级管理员拥有完整权限，内容管理员和版主按职责处理对应内容。",
      "帖子、用户、公告、活动、资源、反馈与敏感词都能在后台闭环处理。",
      "这使平台不仅能发布内容，也能持续维护社区秩序和内容质量。",
    ],
    images: ["admin_overview.png", "admin_activities.png"],
    layout: "admin",
    accent: "#059669",
    soft: "#ddf8ea",
    metrics: [
      { value: "Dashboard", label: "数据监控" },
      { value: "RBAC", label: "角色分工" },
      { value: "Audit", label: "内容审计" },
    ],
  },
  {
    key: "closing",
    durationSeconds: 26,
    eyebrow: "系统演示 07",
    title: "从交流社区到校园服务平台",
    subtitle: "系统把学生端体验、业务服务和后台治理连接成可演进的毕业设计作品",
    bullets: [
      "功能覆盖社区互动、校园生活、资源共享和后台管理",
      "架构层面具备清晰的前后端边界",
      "后续可继续扩展推荐、消息中心和数据分析能力",
    ],
    script: [
      "最后回顾，校园论坛系统并不只是发帖工具。",
      "它把学生端体验、服务端业务和后台治理整合为一个可持续演进的平台。",
      "演示到此结束。",
    ],
    images: [
      "login.png",
      "home.png",
      "post_detail.png",
      "activity_detail.png",
      "admin_overview.png",
      "admin_activities.png",
    ],
    layout: "closing",
    accent: "#111827",
    soft: "#eef2f7",
    metrics: [
      { value: "5 分钟", label: "完整演示" },
      { value: "4 条线", label: "学生、内容、活动、治理" },
      { value: "可扩展", label: "毕业设计基础" },
    ],
  },
];

const sceneStartSeconds = scenes.reduce<number[]>((acc, scene, index) => {
  acc.push(index === 0 ? 0 : acc[index - 1] + scenes[index - 1].durationSeconds);
  return acc;
}, []);

const clampTiming = {
  extrapolateLeft: "clamp" as const,
  extrapolateRight: "clamp" as const,
};

const easeOut = Easing.bezier(0.16, 1, 0.3, 1);

const formatClock = (totalSeconds: number) => {
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
};

export const CampusForumDemo = () => {
  const frame = useCurrentFrame();

  return (
    <AbsoluteFill style={styles.stage}>
      <GlobalBackdrop frame={frame} />
      {scenes.map((scene, index) => (
        <Sequence
          key={scene.key}
          from={sceneStartSeconds[index] * VIDEO_FPS}
          durationInFrames={scene.durationSeconds * VIDEO_FPS}
          premountFor={VIDEO_FPS}
        >
          <DemoScene index={index} scene={scene} />
        </Sequence>
      ))}
      <GlobalTimeline frame={frame} />
    </AbsoluteFill>
  );
};

const GlobalBackdrop: React.FC<{ frame: number }> = ({ frame }) => {
  const drift = interpolate(frame, [0, VIDEO_DURATION_IN_FRAMES], [0, 140], clampTiming);

  return (
    <AbsoluteFill
      style={{
        ...styles.backdrop,
        backgroundPosition: `${drift}px ${drift * 0.6}px`,
      }}
    />
  );
};

const GlobalTimeline: React.FC<{ frame: number }> = ({ frame }) => {
  const currentSecond = Math.min(VIDEO_DURATION_SECONDS, Math.floor(frame / VIDEO_FPS));
  const activeIndex = Math.min(
    scenes.length - 1,
    sceneStartSeconds.findIndex((start, index) => {
      const next = sceneStartSeconds[index + 1] ?? VIDEO_DURATION_SECONDS + 1;
      return currentSecond >= start && currentSecond < next;
    }),
  );
  const progress = interpolate(
    frame,
    [0, VIDEO_DURATION_IN_FRAMES - 1],
    [0, 100],
    clampTiming,
  );

  return (
    <div style={styles.timelineShell}>
      <div style={styles.timelineMeta}>
        <span>{scenes[activeIndex].title}</span>
        <span>
          {formatClock(currentSecond)} / {formatClock(VIDEO_DURATION_SECONDS)}
        </span>
      </div>
      <div style={styles.timelineTrack}>
        <div style={{ ...styles.timelineFill, width: `${progress}%` }} />
      </div>
    </div>
  );
};

const DemoScene: React.FC<{ scene: Scene; index: number }> = ({ scene, index }) => {
  const frame = useCurrentFrame();
  const duration = scene.durationSeconds * VIDEO_FPS;
  const enter = interpolate(frame, [0, 32], [0, 1], {
    easing: easeOut,
    ...clampTiming,
  });
  const exit = interpolate(frame, [duration - 24, duration], [1, 0], clampTiming);
  const opacity = Math.min(enter, exit);
  const translateY = interpolate(enter, [0, 1], [32, 0], clampTiming);
  const scriptIndex = Math.min(
    scene.script.length - 1,
    Math.floor((frame / Math.max(1, duration)) * scene.script.length),
  );

  return (
    <AbsoluteFill
      style={{
        ...styles.scene,
        opacity,
        transform: `translateY(${translateY}px)`,
      }}
    >
      <div style={{ ...styles.accentRule, background: scene.accent }} />
      <SceneHeader scene={scene} index={index} />
      {renderSceneVisual(scene, frame, duration)}
      <Caption text={scene.script[scriptIndex]} accent={scene.accent} />
    </AbsoluteFill>
  );
};

const SceneHeader: React.FC<{ scene: Scene; index: number }> = ({ scene, index }) => {
  return (
    <div style={styles.sceneHeader}>
      <div>
        <div style={{ ...styles.eyebrow, color: scene.accent }}>{scene.eyebrow}</div>
        <h1 style={styles.title}>{scene.title}</h1>
        <p style={styles.subtitle}>{scene.subtitle}</p>
      </div>
      <div style={styles.chapterBadge}>
        {String(index + 1).padStart(2, "0")} / {String(scenes.length).padStart(2, "0")}
      </div>
    </div>
  );
};

const renderSceneVisual = (scene: Scene, frame: number, duration: number) => {
  if (scene.layout === "cover") return <CoverScene scene={scene} frame={frame} />;
  if (scene.layout === "architecture") return <ArchitectureScene scene={scene} frame={frame} />;
  if (scene.layout === "workflow") return <WorkflowScene scene={scene} frame={frame} />;
  if (scene.layout === "admin") return <AdminScene scene={scene} frame={frame} />;
  if (scene.layout === "closing") return <ClosingScene scene={scene} frame={frame} />;

  return <ContentScene scene={scene} frame={frame} duration={duration} />;
};

const CoverScene: React.FC<{ scene: Scene; frame: number }> = ({ scene, frame }) => {
  const cardShift = interpolate(frame, [0, 240], [0, -26], clampTiming);

  return (
    <main style={styles.coverMain}>
      <div style={styles.coverCopy}>
        <MetricGrid metrics={scene.metrics ?? []} accent={scene.accent} />
        <BulletList bullets={scene.bullets} accent={scene.accent} frame={frame} />
      </div>
      <div style={styles.coverMosaic}>
        {scene.images.map((image, index) => (
          <ImageCard
            key={image}
            image={image}
            label={["登录入口", "社区首页", "管理后台"][index]}
            accent={scene.accent}
            style={{
              position: "absolute",
              width: index === 1 ? 760 : 620,
              height: index === 1 ? 430 : 350,
              left: [80, 330, 0][index],
              top: [30, 210, 500][index] + cardShift * (index + 1) * 0.18,
              zIndex: [2, 4, 3][index],
            }}
          />
        ))}
      </div>
    </main>
  );
};

const ArchitectureScene: React.FC<{ scene: Scene; frame: number }> = ({ scene, frame }) => {
  const nodes = [
    "学生端 Vue 页面",
    "路由守卫 + Pinia",
    "Axios REST 请求",
    "Spring Security + JWT",
    "Service 业务层",
    "MyBatis Plus",
    "MySQL + Redis",
  ];

  return (
    <main style={styles.twoColumnMain}>
      <section style={styles.copyColumn}>
        <MetricGrid metrics={scene.metrics ?? []} accent={scene.accent} />
        <BulletList bullets={scene.bullets} accent={scene.accent} frame={frame} />
      </section>
      <section style={styles.archPanel}>
        {nodes.map((node, index) => {
          const reveal = interpolate(frame, [index * 14, index * 14 + 24], [0, 1], {
            easing: easeOut,
            ...clampTiming,
          });
          return (
            <div
              key={node}
              style={{
                ...styles.archNode,
                borderColor: index % 2 === 0 ? scene.accent : "#94a3b8",
                opacity: reveal,
                transform: `translateX(${interpolate(reveal, [0, 1], [24, 0])}px)`,
              }}
            >
              <span style={{ ...styles.archIndex, background: index % 2 === 0 ? scene.accent : "#334155" }}>
                {index + 1}
              </span>
              <span>{node}</span>
              {index < nodes.length - 1 ? <span style={styles.archArrow}>-</span> : null}
            </div>
          );
        })}
      </section>
    </main>
  );
};

const ContentScene: React.FC<{ scene: Scene; frame: number; duration: number }> = ({
  scene,
  frame,
  duration,
}) => {
  const isDual = scene.layout === "dual";

  return (
    <main style={styles.twoColumnMain}>
      <section style={styles.copyColumn}>
        <MetricGrid metrics={scene.metrics ?? []} accent={scene.accent} />
        <BulletList bullets={scene.bullets} accent={scene.accent} frame={frame} />
      </section>
      <section style={isDual ? styles.dualImageGrid : styles.singleImageWrap}>
        {scene.images.map((image, index) => {
          const zoom = interpolate(frame, [0, duration], [1.015, 1.065], clampTiming);
          return (
            <ImageCard
              key={image}
              image={image}
              label={isDual ? ["主界面", "关键详情"][index] : "系统界面"}
              accent={scene.accent}
              style={{
                width: isDual ? 610 : 860,
                height: isDual ? 600 : 610,
                transform: `scale(${zoom})`,
              }}
            />
          );
        })}
      </section>
    </main>
  );
};

const WorkflowScene: React.FC<{ scene: Scene; frame: number }> = ({ scene, frame }) => {
  const steps = ["浏览帖子", "点赞 / 收藏", "发表评论", "通知用户", "推荐加权"];

  return (
    <main style={styles.twoColumnMain}>
      <section style={styles.copyColumn}>
        <MetricGrid metrics={scene.metrics ?? []} accent={scene.accent} />
        <BulletList bullets={scene.bullets} accent={scene.accent} frame={frame} />
      </section>
      <section style={styles.workflowPanel}>
        <ImageCard
          image={scene.images[0]}
          label="帖子详情"
          accent={scene.accent}
          style={{ width: 840, height: 430 }}
        />
        <div style={styles.workflowSteps}>
          {steps.map((step, index) => {
            const active = interpolate(frame, [40 + index * 34, 66 + index * 34], [0, 1], {
              easing: easeOut,
              ...clampTiming,
            });
            return (
              <div
                key={step}
                style={{
                  ...styles.workflowStep,
                  background: `linear-gradient(135deg, ${scene.soft}, #ffffff)`,
                  borderColor: index % 2 === 0 ? scene.accent : "#cbd5e1",
                  transform: `translateY(${interpolate(active, [0, 1], [18, 0])}px)`,
                  opacity: active,
                }}
              >
                <span style={{ ...styles.stepNumber, background: scene.accent }}>{index + 1}</span>
                {step}
              </div>
            );
          })}
        </div>
      </section>
    </main>
  );
};

const AdminScene: React.FC<{ scene: Scene; frame: number }> = ({ scene, frame }) => {
  return (
    <main style={styles.adminMain}>
      <section style={styles.adminCopy}>
        <MetricGrid metrics={scene.metrics ?? []} accent={scene.accent} />
        <BulletList bullets={scene.bullets} accent={scene.accent} frame={frame} />
      </section>
      <section style={styles.adminImages}>
        {scene.images.map((image, index) => (
          <ImageCard
            key={image}
            image={image}
            label={index === 0 ? "数据总览" : "活动与内容维护"}
            accent={scene.accent}
            style={{
              position: "absolute",
              width: 700,
              height: 470,
              left: index === 0 ? 20 : 330,
              top: index === 0 ? 18 : 300,
              zIndex: index === 0 ? 3 : 2,
              transform: `translateY(${interpolate(frame, [0, 300], [0, index === 0 ? -12 : 12], clampTiming)}px)`,
            }}
          />
        ))}
      </section>
    </main>
  );
};

const ClosingScene: React.FC<{ scene: Scene; frame: number }> = ({ scene, frame }) => {
  return (
    <main style={styles.closingMain}>
      <section style={styles.closingCopy}>
        <MetricGrid metrics={scene.metrics ?? []} accent={scene.accent} />
        <BulletList bullets={scene.bullets} accent={scene.accent} frame={frame} />
      </section>
      <section style={styles.closingMosaic}>
        {scene.images.map((image, index) => {
          const row = Math.floor(index / 3);
          const col = index % 3;
          const reveal = interpolate(frame, [index * 8, index * 8 + 26], [0, 1], {
            easing: easeOut,
            ...clampTiming,
          });
          return (
            <ImageCard
              key={image}
              image={image}
              label={["登录", "首页", "详情", "活动", "总览", "维护"][index]}
              accent={scene.accent}
              style={{
                position: "absolute",
                width: 300,
                height: 185,
                left: 20 + col * 320,
                top: 30 + row * 230,
                opacity: reveal,
                transform: `translateY(${interpolate(reveal, [0, 1], [22, 0])}px)`,
              }}
            />
          );
        })}
      </section>
    </main>
  );
};

const MetricGrid: React.FC<{ metrics: Metric[]; accent: string }> = ({ metrics, accent }) => {
  return (
    <div style={styles.metricGrid}>
      {metrics.map((metric) => (
        <div key={metric.label} style={{ ...styles.metric, borderTopColor: accent }}>
          <strong>{metric.value}</strong>
          <span>{metric.label}</span>
        </div>
      ))}
    </div>
  );
};

const BulletList: React.FC<{ bullets: string[]; accent: string; frame: number }> = ({
  bullets,
  accent,
  frame,
}) => {
  return (
    <div style={styles.bulletList}>
      {bullets.map((bullet, index) => {
        const reveal = interpolate(frame, [18 + index * 18, 44 + index * 18], [0, 1], {
          easing: easeOut,
          ...clampTiming,
        });
        return (
          <div
            key={bullet}
            style={{
              ...styles.bullet,
              opacity: reveal,
              transform: `translateX(${interpolate(reveal, [0, 1], [-24, 0])}px)`,
            }}
          >
            <span style={{ ...styles.bulletMark, background: accent }} />
            <span>{bullet}</span>
          </div>
        );
      })}
    </div>
  );
};

const ImageCard: React.FC<{
  image: string;
  label: string;
  accent: string;
  style?: CSSProperties;
}> = ({ image, label, accent, style }) => {
  return (
    <div style={{ ...styles.imageCard, ...style }}>
      <div style={styles.browserBar}>
        <span style={{ ...styles.browserDot, background: "#ef4444" }} />
        <span style={{ ...styles.browserDot, background: "#f59e0b" }} />
        <span style={{ ...styles.browserDot, background: "#22c55e" }} />
        <span style={styles.browserPath}>campus-forum.local</span>
        <span style={{ ...styles.browserLabel, color: accent }}>{label}</span>
      </div>
      <Img src={staticFile(image)} style={styles.screenshot} />
    </div>
  );
};

const Caption: React.FC<{ text: string; accent: string }> = ({ text, accent }) => {
  return (
    <div style={styles.captionWrap}>
      <div style={{ ...styles.captionAccent, background: accent }} />
      <p style={styles.captionText}>{text}</p>
    </div>
  );
};

const styles: Record<string, CSSProperties> = {
  stage: {
    background: "#f7fafc",
    color: "#111827",
    fontFamily:
      '"Microsoft YaHei", "PingFang SC", "Noto Sans CJK SC", "Inter", Arial, sans-serif',
    overflow: "hidden",
  },
  backdrop: {
    backgroundColor: "#f7fafc",
    backgroundImage:
      "linear-gradient(120deg, rgba(15,118,110,0.08), rgba(255,255,255,0) 34%), repeating-linear-gradient(90deg, rgba(15,23,42,0.045) 0, rgba(15,23,42,0.045) 1px, transparent 1px, transparent 56px), repeating-linear-gradient(0deg, rgba(15,23,42,0.035) 0, rgba(15,23,42,0.035) 1px, transparent 1px, transparent 56px)",
    backgroundSize: "100% 100%, 56px 56px, 56px 56px",
  },
  scene: {
    padding: "70px 96px 118px",
  },
  accentRule: {
    position: "absolute",
    left: 0,
    top: 0,
    bottom: 0,
    width: 12,
  },
  sceneHeader: {
    position: "relative",
    display: "flex",
    justifyContent: "space-between",
    alignItems: "flex-start",
    gap: 48,
    zIndex: 5,
  },
  eyebrow: {
    fontSize: 23,
    fontWeight: 800,
    letterSpacing: 1.5,
    marginBottom: 16,
  },
  title: {
    margin: 0,
    fontSize: 76,
    lineHeight: 1.05,
    fontWeight: 900,
    letterSpacing: 0,
  },
  subtitle: {
    margin: "22px 0 0",
    maxWidth: 980,
    color: "#475569",
    fontSize: 31,
    lineHeight: 1.45,
    fontWeight: 600,
  },
  chapterBadge: {
    minWidth: 136,
    height: 58,
    border: "1px solid rgba(15,23,42,0.12)",
    borderRadius: 8,
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    color: "#334155",
    fontSize: 24,
    fontWeight: 800,
    background: "rgba(255,255,255,0.78)",
  },
  coverMain: {
    position: "relative",
    display: "grid",
    gridTemplateColumns: "620px 1fr",
    gap: 48,
    height: "calc(100% - 178px)",
    marginTop: 42,
  },
  coverCopy: {
    alignSelf: "end",
    paddingBottom: 70,
  },
  coverMosaic: {
    position: "relative",
    minHeight: 700,
  },
  twoColumnMain: {
    display: "grid",
    gridTemplateColumns: "620px 1fr",
    gap: 54,
    height: "calc(100% - 186px)",
    marginTop: 38,
    alignItems: "center",
  },
  copyColumn: {
    display: "flex",
    flexDirection: "column",
    gap: 34,
  },
  metricGrid: {
    display: "grid",
    gridTemplateColumns: "repeat(3, minmax(0, 1fr))",
    gap: 14,
  },
  metric: {
    background: "rgba(255,255,255,0.88)",
    border: "1px solid rgba(15,23,42,0.08)",
    borderTop: "6px solid",
    borderRadius: 8,
    padding: "20px 18px 18px",
    boxShadow: "0 18px 40px rgba(15,23,42,0.08)",
  },
  bulletList: {
    display: "flex",
    flexDirection: "column",
    gap: 18,
  },
  bullet: {
    display: "grid",
    gridTemplateColumns: "18px 1fr",
    gap: 16,
    alignItems: "start",
    color: "#1f2937",
    fontSize: 28,
    lineHeight: 1.45,
    fontWeight: 700,
  },
  bulletMark: {
    width: 12,
    height: 12,
    borderRadius: 999,
    marginTop: 14,
  },
  singleImageWrap: {
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
  },
  dualImageGrid: {
    display: "grid",
    gridTemplateColumns: "1fr 1fr",
    gap: 26,
    alignItems: "center",
  },
  imageCard: {
    position: "relative",
    borderRadius: 16,
    overflow: "hidden",
    background: "#ffffff",
    border: "1px solid rgba(15,23,42,0.12)",
    boxShadow: "0 26px 70px rgba(15,23,42,0.18)",
  },
  browserBar: {
    height: 48,
    display: "flex",
    alignItems: "center",
    gap: 8,
    padding: "0 18px",
    background: "#f8fafc",
    borderBottom: "1px solid rgba(15,23,42,0.08)",
  },
  browserDot: {
    width: 12,
    height: 12,
    borderRadius: 999,
    flexShrink: 0,
  },
  browserPath: {
    marginLeft: 12,
    padding: "7px 14px",
    borderRadius: 6,
    background: "#eef2f7",
    color: "#64748b",
    fontSize: 15,
    fontWeight: 700,
  },
  browserLabel: {
    marginLeft: "auto",
    fontSize: 16,
    fontWeight: 900,
  },
  screenshot: {
    width: "100%",
    height: "calc(100% - 48px)",
    objectFit: "contain",
    display: "block",
    background: "#ffffff",
  },
  archPanel: {
    display: "grid",
    gridTemplateColumns: "1fr",
    gap: 16,
    padding: 36,
    borderRadius: 18,
    background: "rgba(255,255,255,0.9)",
    border: "1px solid rgba(15,23,42,0.1)",
    boxShadow: "0 26px 70px rgba(15,23,42,0.12)",
  },
  archNode: {
    position: "relative",
    display: "flex",
    alignItems: "center",
    gap: 20,
    minHeight: 72,
    padding: "0 26px",
    borderRadius: 10,
    border: "2px solid",
    background: "#ffffff",
    fontSize: 28,
    fontWeight: 850,
    color: "#1e293b",
  },
  archIndex: {
    width: 40,
    height: 40,
    borderRadius: 999,
    color: "#ffffff",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    fontSize: 20,
    fontWeight: 900,
  },
  archArrow: {
    marginLeft: "auto",
    color: "#94a3b8",
    fontSize: 34,
    fontWeight: 900,
  },
  workflowPanel: {
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
    gap: 34,
  },
  workflowSteps: {
    display: "grid",
    gridTemplateColumns: "repeat(5, minmax(0, 1fr))",
    gap: 14,
    width: 940,
  },
  workflowStep: {
    minHeight: 120,
    borderRadius: 10,
    border: "2px solid",
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
    justifyContent: "center",
    gap: 12,
    textAlign: "center",
    color: "#1f2937",
    fontSize: 22,
    fontWeight: 900,
    boxShadow: "0 16px 34px rgba(15,23,42,0.08)",
  },
  stepNumber: {
    width: 36,
    height: 36,
    borderRadius: 999,
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    color: "#ffffff",
    fontSize: 18,
    fontWeight: 900,
  },
  adminMain: {
    display: "grid",
    gridTemplateColumns: "560px 1fr",
    gap: 36,
    height: "calc(100% - 186px)",
    marginTop: 34,
    alignItems: "center",
  },
  adminCopy: {
    display: "flex",
    flexDirection: "column",
    gap: 30,
  },
  adminImages: {
    position: "relative",
    height: 800,
  },
  closingMain: {
    display: "grid",
    gridTemplateColumns: "700px 1fr",
    gap: 46,
    height: "calc(100% - 186px)",
    marginTop: 44,
    alignItems: "center",
  },
  closingCopy: {
    display: "flex",
    flexDirection: "column",
    gap: 34,
  },
  closingMosaic: {
    position: "relative",
    height: 520,
  },
  captionWrap: {
    position: "absolute",
    left: 96,
    right: 96,
    bottom: 36,
    display: "grid",
    gridTemplateColumns: "10px 1fr",
    minHeight: 70,
    overflow: "hidden",
    borderRadius: 8,
    background: "rgba(255,255,255,0.9)",
    boxShadow: "0 18px 44px rgba(15,23,42,0.12)",
    border: "1px solid rgba(15,23,42,0.08)",
  },
  captionAccent: {
    width: 10,
  },
  captionText: {
    margin: 0,
    padding: "17px 24px",
    color: "#111827",
    fontSize: 30,
    lineHeight: 1.28,
    fontWeight: 800,
  },
  timelineShell: {
    position: "absolute",
    left: 96,
    right: 96,
    top: 28,
    zIndex: 20,
  },
  timelineMeta: {
    display: "flex",
    justifyContent: "space-between",
    color: "#475569",
    fontSize: 17,
    fontWeight: 800,
    marginBottom: 9,
  },
  timelineTrack: {
    height: 6,
    borderRadius: 999,
    overflow: "hidden",
    background: "rgba(15,23,42,0.12)",
  },
  timelineFill: {
    height: "100%",
    borderRadius: 999,
    background: "linear-gradient(90deg, #0f766e, #2563eb, #ea580c)",
  },
};
