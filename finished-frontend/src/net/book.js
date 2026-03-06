/**
 * 图书模块 API
 */
import { get } from '@/net/api.js'

export const getBookList = (keyword, category, success) =>
    get(`/api/book/list?page=1&keyword=${keyword}&category=${category}`, success)

export const getBookCategories = (success) =>
    get('/api/book/categories', success)
