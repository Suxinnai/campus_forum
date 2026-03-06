/**
 * 论坛模块 API
 */
import { get, post } from '@/net/api.js'

// 天气
export const getWeather = (success, failure) =>
    get('/api/forum/weather?longitude=106.55&latitude=29.56', success, failure)

// 帖子类型
export const getTopicTypes = (success) =>
    get('/api/forum/types', success)

// 帖子列表
export const getTopicList = (page, type, success) =>
    get(`/api/forum/list-topic?page=${page}&type=${type}`, success)

// 置顶帖子
export const getTopTopics = (success) =>
    get('/api/forum/top-topic', success)

// 帖子详情
export const getTopicDetail = (tid, success) =>
    get(`/api/forum/topic?tid=${tid}`, success)

// 点赞/收藏
export const interactTopic = (tid, type, state, success) =>
    get(`/api/forum/interact?tid=${tid}&type=${type}&state=${state}`, success)

// 收藏列表
export const getCollects = (success) =>
    get('/api/forum/collects', success)

// 更新帖子
export const updateTopic = (data, success) =>
    post('/api/forum/update-topic', data, success)

// 评论列表
export const getComments = (tid, page, success) =>
    get(`/api/forum/comments?tid=${tid}&page=${page}`, success)

// 删除评论
export const deleteComment = (id, success) =>
    get(`/api/forum/delete-comment?id=${id}`, success)
