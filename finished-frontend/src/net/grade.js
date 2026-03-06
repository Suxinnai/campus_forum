/**
 * 成绩查询模块 API
 */
import { get } from '@/net/api.js'

export const queryGrade = (studentId, success) =>
    get(`/api/grade/query?studentId=${studentId}`, success)
