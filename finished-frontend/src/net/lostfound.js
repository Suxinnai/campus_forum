/**
 * 失物招领模块 API
 */
import { get, post } from '@/net/api.js'

export const getLostFoundList = (type, success) =>
    get(`/api/lost-found/list?page=1&type=${type}`, success)

export const publishLostFound = (data, success) =>
    post('/api/lost-found/publish', data, success)

export const closeLostFound = (id, success) =>
    get(`/api/lost-found/close?id=${id}`, success)
