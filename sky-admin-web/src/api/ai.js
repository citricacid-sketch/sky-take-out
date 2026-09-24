import request from './request'

/**
 * AI 数据助手提问
 * @param {string} question
 */
export function aiAsk(question) {
  return request({
    url: '/admin/ai/ask',
    method: 'post',
    data: { question },
  })
}
