<script setup>
import { Link as LinkIcon, ExternalLink, GraduationCap, School, Map as MapIcon, Calendar, Github, Globe } from 'lucide-vue-next';

const schoolLinks = [
  { name: '学校官网', url: 'https://www.cqytxy.edu.cn/', icon: School, desc: '在这里了解学校的最新动态。', color: '#3b82f6' },
  { name: '智慧校园', url: 'https://ehall.cqytxy.edu.cn/', icon: GraduationCap, desc: '一站式校园服务入口。', color: '#10b981' },
  { name: '教务系统', url: 'https://http-10-252-6-31-80.vpn.cqytxy.edu.cn/', icon: LinkIcon, desc: '选课、查分、评教等教务服务。', color: '#f59e0b' },
  { name: '数字图书馆', url: 'https://lib.cqytxy.edu.cn/', icon: MapIcon, desc: '查阅图书、借阅历史及学术资源库。', color: '#6366f1' },
  { name: '学校校历', url: 'https://www.cqytxy.edu.cn/ggfw1/xxxl.htm', icon: Calendar, desc: '最新学年校历安排及放假通知。', color: '#ec4899' },
];

const developerLinks = [
  { name: '稀土掘金', url: 'https://juejin.cn', icon: Globe, desc: '非常不错的开发者社区，适合学习。', color: '#1e80ff' },
  { name: 'GitHub', url: 'https://github.com', icon: Github, desc: '全球最大的开源代码托管平台。', color: '#333' },
  { name: 'LeetCode', url: 'https://leetcode.cn', icon: GraduationCap, desc: '在这里刷题锻炼算法逻辑。', color: '#ffa116' },
];

function openLink(url) {
  window.open(url, '_blank');
}
</script>

<template>
  <div class="quick-access ds-page">
    <div class="ds-page-header">
      <h2 class="page-title">快速访问</h2>
      <p class="page-desc">聚合常用的校园官方平台与作者推荐的优质学习资源。</p>
    </div>

    <div class="section-container">
      <h3 class="section-label">校园门户</h3>
      <div class="link-grid">
        <div v-for="item in schoolLinks" :key="item.name" class="link-card ds-card" @click="openLink(item.url)">
           <div class="card-icon" :style="{ background: item.color + '15', color: item.color }">
              <component :is="item.icon" :size="24" />
           </div>
           <div class="card-info">
              <div class="card-name">{{ item.name }}</div>
              <div class="card-desc">{{ item.desc }}</div>
           </div>
           <div class="card-go">
              <ExternalLink :size="16" />
           </div>
        </div>
      </div>
    </div>

    <div class="section-container" style="margin-top: 40px">
      <h3 class="section-label">优质推荐</h3>
      <div class="link-grid">
        <div v-for="item in developerLinks" :key="item.name" class="link-card ds-card" @click="openLink(item.url)">
           <div class="card-icon" :style="{ background: item.color + '15', color: item.color }">
              <component :is="item.icon" :size="24" />
           </div>
           <div class="card-info">
              <div class="card-name">{{ item.name }}</div>
              <div class="card-desc">{{ item.desc }}</div>
           </div>
           <div class="card-go">
              <ExternalLink :size="16" />
           </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
.quick-access {
  max-width: 1100px;
  margin: 32px auto;
}

.section-label {
  font-size: 18px;
  font-weight: 800;
  color: var(--el-text-color-primary);
  margin-bottom: 20px;
  padding-left: 12px;
  border-left: 4px solid var(--el-color-primary);
}

.link-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.link-card {
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 24px rgba(0,0,0,0.1);
    border-color: var(--el-color-primary-light-5);
    
    .card-go { opacity: 1; transform: translateX(0); }
  }
}

.card-icon {
  width: 52px; height: 52px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 14px;
  flex-shrink: 0;
}

.card-info {
  flex: 1;
  min-width: 0;
  .card-name { font-size: 16px; font-weight: 700; color: var(--el-text-color-primary); margin-bottom: 4px; }
  .card-desc { font-size: 13px; color: var(--el-text-color-secondary); line-height: 1.4; }
}

.card-go {
  position: absolute;
  right: 12px;
  top: 12px;
  color: var(--el-color-primary);
  opacity: 0;
  transform: translateX(10px);
  transition: all 0.3s ease;
}

html.dark {
  .link-card { background: var(--el-bg-color); }
}
</style>
