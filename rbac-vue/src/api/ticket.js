import request from '@/api/request'

export const ticketApi = {
  list: params => request.get('/ticket/list', { params }),
  detail: id => request.get(`/ticket/detail/${id}`),
  create: data => request.post('/ticket/create', data),
  receive: id => request.post(`/ticket/receive/${id}`),
  reply: data => request.post('/ticket/reply', data),
  assign: data => request.post('/ticket/assign', data),
  transfer: data => request.post('/ticket/transfer', data),
  resolve: data => request.post('/ticket/resolve', data),
  close: id => request.post(`/ticket/close/${id}`),
  handlerOptions: params => request.get('/ticket/handler-options', { params })
}

export const knowledgeApi = {
  list: params => request.get('/knowledge/list', { params }),
  detail: id => request.get(`/knowledge/detail/${id}`),
  save: data => request.post('/knowledge/save', data),
  draftFromTicket: ticketId => request.post(`/knowledge/draft-from-ticket/${ticketId}`),
  publish: id => request.post(`/knowledge/publish/${id}`),
  withdraw: id => request.post(`/knowledge/withdraw/${id}`)
}
