export function buildTree(items, options = {}) {
  const {
    idKey = 'id',
    parentKey = 'parentId',
    childrenKey = 'children',
    rootValue = 0
  } = options

  const map = new Map()
  items.forEach(item => {
    map.set(item[idKey], {
      ...item,
      [childrenKey]: []
    })
  })

  const tree = []
  map.forEach(node => {
    const parentId = node[parentKey]
    if (parentId !== rootValue && map.has(parentId)) {
      map.get(parentId)[childrenKey].push(node)
      return
    }
    tree.push(node)
  })

  return tree
}

export function filterTreeWithLinkage(tree, keyword, fields = [], childrenKey = 'children') {
  const normalizedKeyword = String(keyword || '').trim().toLowerCase()
  if (!normalizedKeyword) {
    return tree
  }

  const matchNode = (node) => fields.some(field => String(node[field] || '').toLowerCase().includes(normalizedKeyword))

  const visit = (node) => {
    const children = Array.isArray(node[childrenKey]) ? node[childrenKey] : []
    const matchedChildren = children
      .map(child => visit(child))
      .filter(Boolean)

    if (matchNode(node)) {
      return {
        ...node,
        [childrenKey]: children
      }
    }

    if (matchedChildren.length) {
      return {
        ...node,
        [childrenKey]: matchedChildren
      }
    }

    return null
  }

  return tree
    .map(node => visit(node))
    .filter(Boolean)
}
