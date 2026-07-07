import request from '@/api/request'

export const ticketApi = {
  list: params => request.get('/ticket/list', { params }),
  meta: () => request.get('/ticket/meta'),
  statistics: () => request.get('/ticket/statistics'),
  detail: id => request.get(`/ticket/detail/${id}`),
  create: data => request.post('/ticket/create', data),
  systemList: params => request.get('/ticket/system/list', { params }),
  saveSystem: data => request.post('/ticket/system/save', data),
  toggleSystem: id => request.post(`/ticket/system/toggle/${id}`),
  receive: id => request.post(`/ticket/receive/${id}`),
  reply: data => request.post('/ticket/reply', data),
  approve: data => request.post('/ticket/approve', data),
  returnForSupplement: data => request.post('/ticket/return', data),
  cancel: data => request.post('/ticket/cancel', data),
  assign: data => request.post('/ticket/assign', data),
  transfer: data => request.post('/ticket/transfer', data),
  merge: data => request.post('/ticket/merge', data),
  resolve: data => request.post('/ticket/resolve', data),
  close: id => request.post(`/ticket/close/${id}`),
  reject: data => request.post('/ticket/reject', data),
  reopen: data => request.post('/ticket/reopen', data),
  evaluate: data => request.post('/ticket/evaluate', data),
  warnSla: data => request.post('/ticket/sla-warn', data),
  aiPrecheck: data => request.post('/ticket/ai/precheck', data),
  aiDiagnose: data => request.post('/ticket/ai/diagnose', data),
  handlerOptions: params => request.get('/ticket/handler-options', { params })
}

export const knowledgeApi = {
  list: params => request.get('/knowledge/list', { params }),
  detail: id => request.get(`/knowledge/detail/${id}`),
  save: data => request.post('/knowledge/save', data),
  draftFromTicket: ticketId => request.post(`/knowledge/draft-from-ticket/${ticketId}`),
  submitReview: id => request.post(`/knowledge/submit-review/${id}`),
  rejectReview: (id, data) => request.post(`/knowledge/reject-review/${id}`, data),
  publish: id => request.post(`/knowledge/publish/${id}`),
  withdraw: id => request.post(`/knowledge/withdraw/${id}`)
}
