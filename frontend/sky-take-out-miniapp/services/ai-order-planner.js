// services/ai-order-planner.js
// AI 智能点餐助手模块（MVP 版本）
// 功能：基于用户自然语言输入，自动推荐菜品组合
// 架构：确定性推荐算法 + LLM 自然语言解释
//
// 核心特性：
// - 不修改后端代码，仅使用现有 DishVO 数据
// - 本地自然语言意图解析（人数、预算、口味、餐段、忌口）
// - 基于规则的确定性推荐（非随机、可复现）
// - 支持一键加购到购物车
// - 可选 LLM 生成推荐理由说明
//
// 使用流程：
// 1. parseIntent(text)        → 解析用户输入的意图
// 2. loadCandidates()        → 加载候选菜品列表
// 3. generateRecommendations → 生成推荐方案
// 4. addToCart               → 执行加购（可选）
// 5. askExplanation          → 获取 LLM 解释（可选）

const http = require('./request.js');

// ===========================================================================
// 第一层：OrderIntentParser —— 轻量级确定性自然语言意图解析器
// ===========================================================================
// 将用户自然语言输入解析为结构化的点餐意图
// 例如："三个人吃午饭，预算 100 元左右，不要太辣"
// → { peopleCount: 3, budget: 100, taste: ['no_spicy'], meal: 'lunch', ... }

// ---------------------------------------------------------------------------
// 人数识别模式
// 支持格式：3个人 / 三人 / 三位 / 一个人 / 两个人 等
// ---------------------------------------------------------------------------
const PEOPLE_PATTERNS = [
  // 捕获组模式：提取数字（中文或阿拉伯数字）
  { re: /([一二两三四五六七八九十\d]+)\s*(?:个人|人|位|个)/, group: 1 },
  // 固定值模式：直接匹配特定表达
  { re: /(?:一个人|一人)/, value: 1 },
  { re: /(?:两个人|两人|两位)/, value: 2 },
  { re: /(?:三个人|三人|三位)/, value: 3 },
  { re: /(?:四个人|四人|四位)/, value: 4 },
];

// ---------------------------------------------------------------------------
// 预算识别模式
// 支持格式：预算 100 元 / 不超过 80 块 / 100 元以内 / 50 块钱左右
// ---------------------------------------------------------------------------
const BUDGET_PATTERNS = [
  /(?:预算|不超过|控制在|以内|左右|大约)?\s*(\d+)\s*(?:元|块|块钱)/,
  /(\d+)\s*(?:元|块|块钱)\s*(?:以内|左右|以下)?/,
];

// ---------------------------------------------------------------------------
// 口味偏好映射表
// 将中文口味描述映射为内部枚举值
// ---------------------------------------------------------------------------
const TASTE_MAP = [
  { keys: ['辣', '麻辣', '香辣', '重辣'], value: 'spicy' },       // 重辣
  { keys: ['微辣'], value: 'mild_spicy' },                          // 微辣
  { keys: ['不辣', '不要辣', '免辣'], value: 'no_spicy' },          // 不辣
  { keys: ['清淡', '清口', '清爽'], value: 'light' },               // 清淡
  { keys: ['甜', '酸甜'], value: 'sweet' },                         // 甜
  { keys: ['酸', '酸辣'], value: 'sour' },                          // 酸
  { keys: ['咸', '重口'], value: 'salty' },                         // 咸/重口
];

// ---------------------------------------------------------------------------
// 餐段识别映射表
// 将中文餐段描述映射为内部枚举值
// ---------------------------------------------------------------------------
const MEAL_MAP = [
  { keys: ['午餐', '午饭', '中午'], value: 'lunch' },       // 午餐
  { keys: ['晚餐', '晚饭', '晚上'], value: 'dinner' },      // 晚餐
  { keys: ['早餐', '早饭', '早上'], value: 'breakfast' },    // 早餐
  { keys: ['夜宵', '宵夜', '深夜'], value: 'supper' },       // 夜宵
  { keys: ['聚餐', '聚会', '多人'], value: 'gathering' },    // 聚餐
];

// ---------------------------------------------------------------------------
// 忌口/排除项识别模式
// 识别用户明确表示不要的食材或口味
// ---------------------------------------------------------------------------
const EXCLUDE_PATTERNS = [
  /不要\s*([^\s,，、]+)/g,     // "不要葱" → 提取 "葱"
  /不吃\s*([^\s,，、]+)/g,     // "不吃辣" → 提取 "辣"
  /别放\s*([^\s,，、]+)/g,     // "别放香菜" → 提取 "香菜"
  /不要放\s*([^\s,，、]+)/g,   // "不要放蒜" → 提取 "蒜"
];

// ---------------------------------------------------------------------------
// 各字段解析函数
// ---------------------------------------------------------------------------

/**
 * 从文本中提取用餐人数
 * 支持中文数字（一二两三四五六七八九十）和阿拉伯数字
 * 有效范围：1-20 人
 *
 * @param {string} text - 用户输入文本
 * @returns {number|null} 解析出的人数，未识别返回 null
 */
function parsePeople(text) {
  if (!text) return null;
  for (const p of PEOPLE_PATTERNS) {
    // 固定值模式：直接返回预设值
    if (p.value !== undefined && p.re.test(text)) return p.value;
    // 捕获组模式：提取匹配的数字并转换
    const m = text.match(p.re);
    if (m && m[p.group]) {
      // 中文数字 → 阿拉伯数字转换（两/二 → 2，三 → 3，四 → 4）
      const n = parseInt(m[p.group].replace(/[两二]/g, '2').replace(/[三三]/g, '3').replace(/[四四]/g, '4'), 10);
      if (!isNaN(n) && n > 0 && n <= 20) return n; // 有效性校验
    }
  }
  return null;
}

/**
 * 从文本中提取预算金额
 * 有效范围：1-10000 元
 *
 * @param {string} text - 用户输入文本
 * @returns {number|null} 解析出的预算金额（元），未识别返回 null
 */
function parseBudget(text) {
  if (!text) return null;
  for (const re of BUDGET_PATTERNS) {
    const m = text.match(re);
    if (m && m[1]) {
      const n = parseInt(m[1], 10);
      if (!isNaN(n) && n > 0 && n <= 10000) return n;
    }
  }
  return null;
}

/**
 * 从文本中提取口味偏好（可多个）
 * 按 TASTE_MAP 顺序匹配，可能同时命中多个偏好
 *
 * @param {string} text - 用户输入文本
 * @returns {string[]} 口味偏好枚举值数组，如 ['spicy', 'light']
 */
function parseTaste(text) {
  if (!text) return [];
  const out = [];
  for (const t of TASTE_MAP) {
    // 只要包含任一关键词，就记录该口味偏好
    if (t.keys.some((k) => text.includes(k))) out.push(t.value);
  }
  return out;
}

/**
 * 从文本中提取餐段（单选）
 * 按 MEAL_MAP 顺序匹配，返回第一个命中的餐段
 *
 * @param {string} text - 用户输入文本
 * @returns {string|null} 餐段枚举值，如 'lunch'，未识别返回 null
 */
function parseMeal(text) {
  if (!text) return null;
  for (const m of MEAL_MAP) {
    if (m.keys.some((k) => text.includes(k))) return m.value;
  }
  return null;
}

/**
 * 从文本中提取忌口/排除项（可多个）
 * 使用全局正则循环匹配，提取所有"不要/不吃/别放"后的食材名
 *
 * @param {string} text - 用户输入文本
 * @returns {string[]} 忌口食材名称数组，如 ['葱', '香菜']
 */
function parseExclude(text) {
  if (!text) return [];
  const out = [];
  for (const re of EXCLUDE_PATTERNS) {
    let m;
    // 全局正则需要重置 lastIndex，防止连续调用时从上次位置继续匹配
    re.lastIndex = 0;
    while ((m = re.exec(text)) !== null) {
      out.push(m[1]);
    }
  }
  return out;
}

/**
 * 综合意图解析入口函数
 * 将用户自然语言转换为结构化点餐意图对象
 *
 * @param {string} text - 用户输入的自然语言文本
 * @returns {object} 结构化意图对象
 * @returns {number|null} returns.peopleCount - 用餐人数
 * @returns {number|null} returns.budget - 预算金额（元）
 * @returns {string[]} returns.taste - 口味偏好数组
 * @returns {string|null} returns.meal - 餐段枚举值
 * @returns {string[]} returns.exclude - 忌口食材数组
 * @returns {string} returns.rawText - 原始输入文本（保留供后续分析）
 *
 * @example
 * parseIntent("三个人午餐预算 100 元，不吃辣")
 * // → { peopleCount: 3, budget: 100, taste: ['no_spicy'], meal: 'lunch', exclude: ['辣'], rawText: "..." }
 */
function parseIntent(text) {
  return {
    peopleCount: parsePeople(text),  // 人数
    budget: parseBudget(text),      // 预算
    taste: parseTaste(text),        // 口味偏好
    meal: parseMeal(text),          // 餐段
    exclude: parseExclude(text),    // 忌口
    rawText: text || '',            // 原始文本
  };
}

// ===========================================================================
// 第二层：CandidateLoader —— 候选菜品加载器（带组件级生命周期缓存）
// ===========================================================================
// 从后端获取在售菜品列表，作为推荐的候选池
// 使用内存缓存减少重复请求，缓存有效期 5 分钟

let cachedCandidates = null; // 缓存的候选菜品数组
let cacheTime = 0;           // 缓存写入时的时间戳
const CACHE_TTL_MS = 5 * 60 * 1000; // 缓存有效期：5 分钟

/**
 * 加载候选菜品列表
 * 优先返回缓存数据，缓存过期或强制刷新时从后端重新获取
 *
 * 数据来源：GET /user/dish/list（不传分类参数，获取全量菜品）
 * 过滤条件：仅保留 status === 1（在售状态）的菜品
 *
 * @param {boolean} [forceRefresh=false] - 强制刷新缓存（忽略缓存有效期）
 * @returns {Promise<Array<DishVO>>} 在售菜品列表
 */
async function loadCandidates(forceRefresh = false) {
  const now = Date.now();
  // 缓存命中：未强制刷新 且 缓存存在 且 未过期
  if (!forceRefresh && cachedCandidates && now - cacheTime < CACHE_TTL_MS) {
    return cachedCandidates;
  }
  // 从后端获取全量菜品列表（静默请求，不弹错误 toast）
  const dishes = (await http.get('/user/dish/list', { silent: true }).catch(() => [])) || [];
  // 过滤：仅保留在售菜品（status === 1）
  const candidates = dishes.filter((d) => d && d.status === 1);
  // 写入缓存
  cachedCandidates = candidates;
  cacheTime = now;
  return candidates;
}

/**
 * 清除候选菜品缓存
 * 在菜品数据变更（如管理员修改菜单）后调用，强制下次重新加载
 */
function clearCandidateCache() {
  cachedCandidates = null;
  cacheTime = 0;
}

// ===========================================================================
// 第三层：RecommendationEngine —— 确定性规则推荐引擎
// ===========================================================================
// 基于用户意图和候选菜品，通过确定性算法生成推荐方案
// 算法特点：
// - 口味匹配评分（根据菜品名称、描述、分类、口味标签综合打分）
// - 忌口过滤（排除用户明确不要的食材）
// - 预算约束（单菜不超过总预算，累计不超过预算）
// - 主食数量按人数缩放（饭/面/粥等按人数推荐）
// - 贪心选择（按口味匹配度降序 + 价格升序，逐道选取）

/**
 * 计算菜品与用户口味偏好的匹配分数
 * 匹配分数越高，说明菜品越符合用户口味
 *
 * 评分规则：
 * - 口味标签直接匹配：+2 分
 * - 菜品名称/描述包含口味关键词：+1~2 分
 * - "不辣"偏好但菜品不含辣：+1 分（反向加分）
 *
 * @param {DishVO} dish - 菜品对象
 * @param {string[]} tastePrefs - 用户口味偏好数组
 * @returns {number} 匹配分数（0 表示无匹配）
 */
function matchTaste(dish, tastePrefs) {
  if (!tastePrefs || tastePrefs.length === 0) return 0;
  // 构建匹配用的文本池（名称 + 描述 + 分类 + 口味标签）
  const haystack = `${dish.name || ''} ${dish.description || ''} ${dish.categoryName || ''} ${(dish.flavors || []).map((f) => f.name + ' ' + f.value).join(' ')}`.toLowerCase();
  let score = 0;
  for (const pref of tastePrefs) {
    if (pref === 'spicy' && /辣|麻辣|香辣/.test(haystack)) score += 2;
    else if (pref === 'mild_spicy' && /微辣/.test(haystack)) score += 2;
    else if (pref === 'no_spicy' && !/辣/.test(haystack)) score += 1; // 不辣偏好：不含辣即加分
    else if (pref === 'light' && /清淡|清口|蒸|煮|白/.test(haystack)) score += 2;
    else if (pref === 'sweet' && /甜|蜜|糖/.test(haystack)) score += 2;
    else if (pref === 'sour' && /酸|醋/.test(haystack)) score += 2;
    else if (pref === 'salty' && /咸|酱|腌/.test(haystack)) score += 1;
  }
  return score;
}

/**
 * 判断菜品是否被用户忌口排除
 * 检查菜品名称、描述、分类中是否包含用户指定的忌口食材
 *
 * @param {DishVO} dish - 菜品对象
 * @param {string[]} excludeTerms - 忌口食材数组
 * @returns {boolean} true 表示该菜品应被排除
 */
function isExcluded(dish, excludeTerms) {
  if (!excludeTerms || excludeTerms.length === 0) return false;
  const haystack = `${dish.name || ''} ${dish.description || ''} ${dish.categoryName || ''}`.toLowerCase();
  return excludeTerms.some((term) => term && haystack.includes(term.toLowerCase()));
}

/**
 * 根据人数推荐菜品数量
 * 主食类（饭/面/粥/粉/饺/包/馒）按人数推荐（N 人 → N 份）
 * 非主食类固定推荐 1 份
 *
 * @param {DishVO} dish - 菜品对象
 * @param {number} peopleCount - 用餐人数
 * @returns {number} 推荐数量
 */
function suggestQuantity(dish, peopleCount) {
  const name = (dish.name || '').toLowerCase();
  // 主食类：名称包含饭/面/粥/粉/饺/包/馒
  if (/饭|面|粥|粉|饺|包|馒/.test(name)) {
    return Math.max(1, peopleCount || 1); // 主食数量 = 人数（至少 1）
  }
  return 1; // 非主食固定 1 份
}

/**
 * 核心推荐算法：根据用户意图生成推荐菜品组合
 *
 * 算法步骤：
 * 1. 过滤：排除忌口菜品 + 超出预算的菜品
 * 2. 评分：为每道菜计算口味匹配分数
 * 3. 排序：按口味匹配度降序，同分按价格升序
 * 4. 贪心选取：从高到低逐道选取，累计金额不超过预算上限
 * 5. 数量调整：主食按人数缩放数量
 *
 * 最大推荐数量：
 * - 1-2 人：最多 3 道菜
 * - 3-4 人：最多 5 道菜
 * - 5+ 人：最多 6 道菜
 *
 * @param {object} intent - 解析后的意图对象（parseIntent 返回值）
 * @param {number} [intent.peopleCount=1] - 用餐人数
 * @param {number} [intent.budget] - 预算上限（元）
 * @param {string[]} [intent.taste=[]] - 口味偏好
 * @param {string[]} [intent.exclude=[]] - 忌口食材
 * @param {Array<DishVO>} candidates - 候选菜品列表
 * @returns {Array<object>} 推荐结果数组
 * @returns {number} returns[].dishId - 菜品 ID
 * @returns {DishVO} returns[].dish - 完整菜品对象
 * @returns {number} returns[].quantity - 推荐数量
 * @returns {number} returns[].unitPrice - 单价
 * @returns {number} returns[].lineTotal - 行项小计（单价 × 数量）
 * @returns {string[]} returns[].matchedReasons - 推荐理由标签
 * @returns {boolean} returns[].requiresFlavor - 是否需要用户选择口味规格
 */
function generateRecommendations(intent, candidates) {
  const { peopleCount = 1, budget, taste = [], exclude = [] } = intent;
  if (!candidates || candidates.length === 0) return [];

  // 第一步：过滤候选池（排除忌口 + 超出预算的菜品）
  let pool = candidates.filter((d) => {
    if (isExcluded(d, exclude)) return false;
    if (budget && Number(d.price) > budget) return false; // 单菜价格不能超过总预算
    return true;
  });

  // 第二步：为每道菜计算口味匹配分数
  const scored = pool.map((d) => ({
    dish: d,
    tasteScore: matchTaste(d, taste),
    price: Number(d.price) || 0,
  }));

  // 第三步：排序（口味匹配度降序，同分按价格升序）
  scored.sort((a, b) => {
    if (b.tasteScore !== a.tasteScore) return b.tasteScore - a.tasteScore;
    return a.price - b.price;
  });

  // 第四步：贪心选取（预算约束下的最优组合）
  const picked = [];
  let subtotal = 0; // 当前累计金额
  // 根据人数确定最大推荐数量
  const maxItems = Math.min(6, peopleCount <= 2 ? 3 : peopleCount <= 4 ? 5 : 6);

  for (const item of scored) {
    if (picked.length >= maxItems) break; // 达到最大数量上限
    const qty = suggestQuantity(item.dish, peopleCount); // 推荐数量
    const lineTotal = item.price * qty; // 行项小计
    if (budget && subtotal + lineTotal > budget) continue; // 超出预算则跳过
    const requiresFlavor = Array.isArray(item.dish.flavors) && item.dish.flavors.length > 0;
    picked.push({
      dishId: item.dish.id,
      dish: item.dish,
      quantity: qty,
      unitPrice: item.price,
      lineTotal,
      matchedReasons: buildReasons(item.dish, item.tasteScore, taste, peopleCount),
      requiresFlavor, // 需要用户选择口味规格的菜品标记
    });
    subtotal += lineTotal;
  }

  return picked;
}

/**
 * 构建推荐理由标签
 * 根据菜品匹配情况生成人类可读的推荐理由
 *
 * @param {DishVO} dish - 菜品对象
 * @param {number} tasteScore - 口味匹配分数
 * @param {string[]} tastePrefs - 用户口味偏好
 * @param {number} peopleCount - 用餐人数
 * @returns {string[]} 推荐理由数组
 */
function buildReasons(dish, tasteScore, tastePrefs, peopleCount) {
  const reasons = [];
  if (tasteScore > 0) reasons.push('符合口味偏好'); // 口味匹配
  if (/饭|面|粥|粉|饺|包|馒/.test(dish.name || '')) {
    if (peopleCount > 1) reasons.push(`适合${peopleCount}人份`); // 主食份量提示
  }
  if (reasons.length === 0) reasons.push('价格适合预算'); // 兜底理由
  return reasons;
}

// ===========================================================================
// 第四层：PriceCalculator —— 价格计算工具
// ===========================================================================

/**
 * 计算推荐方案的总价
 * @param {Array<object>} recommendations - generateRecommendations 返回的推荐数组
 * @returns {number} 总价格（元）
 */
function calcTotal(recommendations) {
  return recommendations.reduce((sum, r) => sum + (Number(r.unitPrice) || 0) * (r.quantity || 1), 0);
}

/**
 * 格式化价格为两位小数字符串
 * @param {number} n - 价格数值
 * @returns {string} 格式化后的价格，如 "99.00"
 */
function formatPrice(n) {
  return Number(n || 0).toFixed(2);
}

// ===========================================================================
// 第五层：CartExecutor —— 购物车执行器（批量加购）
// ===========================================================================
// 将推荐方案批量添加到购物车
// 安全策略：
// - 仅添加不需要选择口味规格的菜品
// - 需要选择口味的菜品单独返回给 UI 层处理
// - 每道菜最多加购 10 份（防止滥用）
// - 某道菜加购失败时停止该品的后续添加

/**
 * 批量将推荐菜品添加到购物车
 *
 * 处理逻辑：
 * 1. 遍历推荐列表
 * 2. 跳过需要口味规格的菜品（返回给 UI 让用户手动选择）
 * 3. 跳过无效的菜品 ID
 * 4. 按数量循环调用后端 add 接口（每次 +1）
 * 5. 单道菜加购失败时记录并停止该品后续添加
 *
 * @param {Array<object>} recommendations - 推荐结果数组
 * @returns {Promise<object>} 加购结果
 * @returns {Array<object>} returns.success - 成功加购的菜品
 * @returns {Array<object>} returns.failed - 加购失败的菜品（含 reason 字段）
 * @returns {Array<object>} returns.needsFlavor - 需要用户选择口味的菜品（未加购）
 */
async function addToCart(recommendations) {
  if (!recommendations || recommendations.length === 0) {
    return { success: [], failed: [], needsFlavor: [] };
  }
  const success = [];
  const failed = [];
  const needsFlavor = [];
  for (const rec of recommendations) {
    // 安全检查：跳过需要用户选择口味/规格的菜品
    if (rec.requiresFlavor) {
      needsFlavor.push(rec);
      continue;
    }
    // 安全检查：跳过缺少菜品 ID 的异常数据
    if (!rec.dishId) {
      failed.push({ ...rec, reason: '缺少菜品 ID' });
      continue;
    }
    // 后端 add() 每次只 +1，因此需循环调用 quantity 次
    // 安全限制：每道菜最多加购 10 份
    const qty = Math.min(Math.max(1, rec.quantity || 1), 10);
    let ok = true;
    for (let i = 0; i < qty; i++) {
      try {
        await http.post('/user/shoppingCart/add', { dishId: rec.dishId, dishFlavor: '' });
      } catch (e) {
        failed.push({ ...rec, reason: '加购失败' });
        ok = false;
        break; // 首次失败即停止该品的后续添加
      }
    }
    if (ok) success.push(rec);
  }
  return { success, failed, needsFlavor };
}

// ===========================================================================
// 第六层：AI Explanation —— LLM 自然语言解释生成
// ===========================================================================
// 调用后端 AI 接口，为用户生成推荐方案的友好文字说明
// 注意：LLM 生成的是纯文本说明，不包含任何可执行指令（安全隔离）

/**
 * 请求 LLM 生成推荐方案的自然语言解释
 * 将用户条件和推荐结果拼接为 prompt，发送给后端 chat 接口
 *
 * prompt 设计：
 * - 包含用户原始需求（rawText）
 * - 包含推荐菜品名称和数量
 * - 包含预计总价
 * - 要求语气友好，不涉及具体价格计算细节
 *
 * @param {object} intent - 用户意图对象
 * @param {Array<object>} recommendations - 推荐结果数组
 * @returns {Promise<string|null>} LLM 返回的文字说明，失败返回 null
 */
async function askExplanation(intent, recommendations) {
  const total = calcTotal(recommendations);
  const names = recommendations.map((r) => `${r.dish.name}×${r.quantity}`).join('、');
  // 构造 prompt：将上下文信息传递给 LLM
  const prompt = `用户条件：${intent.rawText || '无具体要求'}。已推荐：${names}。预计总价 ${total.toFixed(2)} 元。请用 1-2 句话简要说明推荐理由，语气友好，不要提及具体价格计算。`;
  try {
    const reply = await http.post('/user/chat', { message: prompt }, { silent: true });
    return (typeof reply === 'string' && reply.trim()) ? reply : null;
  } catch (e) {
    return null; // LLM 调用失败时静默降级，不影响推荐结果展示
  }
}

// 导出接口
module.exports = {
  parseIntent,              // 自然语言意图解析
  loadCandidates,           // 加载候选菜品（带缓存）
  clearCandidateCache,      // 清除候选菜品缓存
  generateRecommendations,  // 生成推荐方案
  calcTotal,                // 计算总价
  formatPrice,              // 格式化价格
  addToCart,                // 批量加购到购物车
  askExplanation,           // 获取 LLM 解释
};
