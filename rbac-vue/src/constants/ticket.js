export const TICKET_STATUS = {
  PENDING: 'pending',
  PROCESSING: 'processing',
  WAITING_CUSTOMER: 'waiting_customer',
  TRANSFERRED: 'transferred',
  RESOLVED: 'resolved',
  CLOSED: 'closed',
  REJECTED: 'rejected'
}

export const TICKET_STATUS_OPTIONS = [
  { value: TICKET_STATUS.PENDING, label: '待受理', color: 'orange' },
  { value: TICKET_STATUS.PROCESSING, label: '处理中', color: 'blue' },
  { value: TICKET_STATUS.WAITING_CUSTOMER, label: '待客户补充', color: 'purple' },
  { value: TICKET_STATUS.TRANSFERRED, label: '已转派', color: 'cyan' },
  { value: TICKET_STATUS.RESOLVED, label: '已解决', color: 'green' },
  { value: TICKET_STATUS.CLOSED, label: '已关闭', color: 'default' },
  { value: TICKET_STATUS.REJECTED, label: '已驳回', color: 'red' }
]

export const TICKET_PRIORITY_OPTIONS = [
  { value: 'low', label: '低', color: 'default' },
  { value: 'normal', label: '普通', color: 'blue' },
  { value: 'high', label: '高', color: 'orange' },
  { value: 'urgent', label: '紧急', color: 'red' }
]

export const TICKET_ACTION_TEXT = {
  created: '创建工单',
  received: '接单',
  replied: '回复',
  assigned: '分派',
  transferred: '转派',
  resolved: '解决',
  closed: '关闭',
  rejected: '驳回'
}

export const ticketStatusText = value => TICKET_STATUS_OPTIONS.find(item => item.value === value)?.label || value
export const ticketStatusColor = value => TICKET_STATUS_OPTIONS.find(item => item.value === value)?.color || 'default'
export const ticketPriorityText = value => TICKET_PRIORITY_OPTIONS.find(item => item.value === value)?.label || value
export const ticketPriorityColor = value => TICKET_PRIORITY_OPTIONS.find(item => item.value === value)?.color || 'default'
export const ticketActionText = value => TICKET_ACTION_TEXT[value] || value
export const isTicketTerminal = value => [TICKET_STATUS.CLOSED, TICKET_STATUS.REJECTED].includes(value)
