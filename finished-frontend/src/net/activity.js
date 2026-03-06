/**
 * 活动模块 API
 */
import { get, post } from '@/net/api.js'

export const getActivityList = (success) =>
    get('/api/activity/list', success)

export const joinActivity = (id, success) =>
    post(`/api/activity/join?id=${id}`, {}, success)
