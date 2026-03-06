/**
 * 表白墙模块 API
 */
import { get, post } from '@/net/api.js'

export const getConfessionList = (success) =>
    get('/api/confession/list?page=1', success)

export const publishConfession = (data, success) =>
    post('/api/confession/publish', data, success)

export const likeConfession = (id, success) =>
    post(`/api/confession/like?id=${id}`, {}, success)
