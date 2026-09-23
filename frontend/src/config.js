/**
 * 前端可配置项
 */

// 作品分类。后端 ai_production.sortid 是纯数字，没有字典表，
// 这里给的是占位名称，按你数据库里 sortid 的真实语义改掉即可。
export const CATEGORIES = [
  { id: 1, name: '古体诗' },
  { id: 2, name: '近体诗' },
  { id: 3, name: '现代诗' },
  { id: 4, name: '词曲' },
  { id: 5, name: '其他' },
]

// 单次 AI 写诗消耗的算力，与后端 aicontroller 里硬编码的 50 保持一致（仅用于文案提示）
export const POEM_COST = 50

// 「全部」分类时扫描的 sortid 范围
export const SORT_ID_RANGE = [1, 2, 3, 4, 5, 6]
