/**
 * 数据统计模块 API
 */
import { get } from '@/net/api.js'

export const getHotTrend = (success) =>
    get('/api/stat/hot-trend', success)

export const getCategoryPie = (success) =>
    get('/api/stat/category-pie', success)

export const getKeywordCloud = (success) =>
    get('/api/stat/keyword-cloud', success)
