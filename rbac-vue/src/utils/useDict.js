import { ref } from 'vue'
import request from '@/api/request'

/**
 * 字典工具 Hook
 */
export function useDict() {
  const dictMap = ref({})

  const flattenDict = (items = []) => {
    const result = []
    items.forEach((item) => {
      result.push(item)
      if (item.children?.length) {
        result.push(...flattenDict(item.children))
      }
    })
    return result
  }

  /**
   * 根据类型获取字典列表
   * @param typeCode 字典类型编码
   */
  const getDict = async (typeCode) => {
    if (dictMap.value[typeCode]) return dictMap.value[typeCode]
    
    const res = await request.get('/system/dict/data/list', { params: { typeCode } })
    dictMap.value[typeCode] = res.data
    return res.data
  }

  const getDictOptions = async (typeCode, options = {}) => {
    const { onlyEnabled = true } = options
    const list = flattenDict(await getDict(typeCode))
    return list
      .filter(item => !onlyEnabled || item.status === 1)
      .map(item => ({
        label: item.label,
        value: item.value,
        raw: item
      }))
  }

  return {
    getDict,
    getDictOptions,
    dictMap
  }
}
