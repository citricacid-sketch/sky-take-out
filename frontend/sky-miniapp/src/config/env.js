/**
 * 苍穹外卖 - 环境配置
 *
 * 根据 uni-app 运行平台自动切换 API 基础地址：
 * - h5  : 使用 /user 走 vite 代理（同源，无需 CORS）
 * - mp  : 使用完整 URL，微信要求合法域名；开发期可开启「不校验合法域名」
 *
 * 如需修改后端地址，请统一修改此文件。
 */

// uni-app 编译期注入的目标平台（h5 / mp-weixin / mp-alipay ...）
const PLATFORM = process.env.UNI_PLATFORM

// 后端服务地址（开发者本机）
const BACKEND_HOST = '192.168.3.8'
const BACKEND_PORT = 8080
const BACKEND_BASE = `http://${BACKEND_HOST}:${BACKEND_PORT}`

// H5 用相对路径走代理；小程序/APP 必须用完整 URL
const BASE_URL = PLATFORM === 'h5' ? '' : BACKEND_BASE

export { BASE_URL, PLATFORM, BACKEND_BASE, BACKEND_HOST, BACKEND_PORT }
