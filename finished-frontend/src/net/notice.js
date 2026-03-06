/**
 * 通知公告模块 API
 */
import { get } from '@/net/api.js'

export const getNoticeList = (success) =>
    get('/api/notice/list', success)

export const getNoticeDetail = (id, success) =>
    get(`/api/notice/detail?id=${id}`, success)
