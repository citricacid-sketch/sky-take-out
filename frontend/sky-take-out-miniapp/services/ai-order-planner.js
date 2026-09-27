// services/ai-order-planner.js
// AI Ordering Assistant MVP: deterministic recommendation + LLM explanation.
// Does NOT modify the backend. Uses real DishVO data only.

const http = require('./request.js');

// ---------------------------------------------------------------------------
// OrderIntentParser – lightweight deterministic NL intent parser
// ---------------------------------------------------------------------------
const PEOPLE_PATTERNS = [
  { re: /([一二两三四五六七八九十\d]+)\s*(?:个人|人|位|个)/, group: 1 },
  { re: /(?:一个人|一人)/, value: 1 },
  { re: /(?:两个人|两人|两位)/, value: 2 },
  { re: /(?:三个人|三人|三位)/, value: 3 },
  { re: /(?:四个人|四人|四位)/, value: 4 },
];

const BUDGET_PATTERNS = [
  /(?:预算|不超过|控制在|以内|左右|大约)?\s*(\d+)\s*(?:元|块|块钱)/,
  /(\d+)\s*(?:元|块|块钱)\s*(?:以内|左右|以下)?/,
];

const TASTE_MAP = [
  { keys: ['辣', '麻辣', '香辣', '重辣'], value: 'spicy' },
  { keys: ['微辣'], value: 'mild_spicy' },
  { keys: ['不辣', '不要辣', '免辣'], value: 'no_spicy' },
  { keys: ['清淡', '清口', '清爽'], value: 'light' },
  { keys: ['甜', '酸甜'], value: 'sweet' },
  { keys: ['酸', '酸辣'], value: 'sour' },
  { keys: ['咸', '重口'], value: 'salty' },
];

const MEAL_MAP = [
  { keys: ['午餐', '午饭', '中午'], value: 'lunch' },
  { keys: ['晚餐', '晚饭', '晚上'], value: 'dinner' },
  { keys: ['早餐', '早饭', '早上'], value: 'breakfast' },
  { keys: ['夜宵', '宵夜', '深夜'], value: 'supper' },
  { keys: ['聚餐', '聚会', '多人'], value: 'gathering' },
];

const EXCLUDE_PATTERNS = [
  /不要\s*([^\s,，、]+)/g,
  /不吃\s*([^\s,，、]+)/g,
  /别放\s*([^\s,，、]+)/g,
  /不要放\s*([^\s,，、]+)/g,
];

function parsePeople(text) {
  if (!text) return null;
  for (const p of PEOPLE_PATTERNS) {
    if (p.value !== undefined && p.re.test(text)) return p.value;
    const m = text.match(p.re);
    if (m && m[p.group]) {
      const n = parseInt(m[p.group].replace(/[两二]/g, '2').replace(/[三三]/g, '3').replace(/[四四]/g, '4'), 10);
      if (!isNaN(n) && n > 0 && n <= 20) return n;
    }
  }
  return null;
}

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

function parseTaste(text) {
  if (!text) return [];
  const out = [];
  for (const t of TASTE_MAP) {
    if (t.keys.some((k) => text.includes(k))) out.push(t.value);
  }
  return out;
}

function parseMeal(text) {
  if (!text) return null;
  for (const m of MEAL_MAP) {
    if (m.keys.some((k) => text.includes(k))) return m.value;
  }
  return null;
}

function parseExclude(text) {
  if (!text) return [];
  const out = [];
  for (const re of EXCLUDE_PATTERNS) {
    let m;
    // reset lastIndex for global regex
    re.lastIndex = 0;
    while ((m = re.exec(text)) !== null) {
      out.push(m[1]);
    }
  }
  return out;
}

function parseIntent(text) {
  return {
    peopleCount: parsePeople(text),
    budget: parseBudget(text),
    taste: parseTaste(text),
    meal: parseMeal(text),
    exclude: parseExclude(text),
    rawText: text || '',
  };
}

// ---------------------------------------------------------------------------
// CandidateLoader – fetch real DishVO list with component-lifecycle caching
// ---------------------------------------------------------------------------
let cachedCandidates = null;
let cacheTime = 0;
const CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes

async function loadCandidates(forceRefresh = false) {
  const now = Date.now();
  if (!forceRefresh && cachedCandidates && now - cacheTime < CACHE_TTL_MS) {
    return cachedCandidates;
  }
  // Fetch all dishes without category filter
  const dishes = (await http.get('/user/dish/list', { silent: true }).catch(() => [])) || [];
  // Only on-sale dishes
  const candidates = dishes.filter((d) => d && d.status === 1);
  cachedCandidates = candidates;
  cacheTime = now;
  return candidates;
}

function clearCandidateCache() {
  cachedCandidates = null;
  cacheTime = 0;
}

// ---------------------------------------------------------------------------
// RecommendationEngine – deterministic rule-based recommendation
// ---------------------------------------------------------------------------
function matchTaste(dish, tastePrefs) {
  if (!tastePrefs || tastePrefs.length === 0) return 0;
  const haystack = `${dish.name || ''} ${dish.description || ''} ${dish.categoryName || ''} ${(dish.flavors || []).map((f) => f.name + ' ' + f.value).join(' ')}`.toLowerCase();
  let score = 0;
  for (const pref of tastePrefs) {
    if (pref === 'spicy' && /辣|麻辣|香辣/.test(haystack)) score += 2;
    else if (pref === 'mild_spicy' && /微辣/.test(haystack)) score += 2;
    else if (pref === 'no_spicy' && !/辣/.test(haystack)) score += 1;
    else if (pref === 'light' && /清淡|清口|蒸|煮|白/.test(haystack)) score += 2;
    else if (pref === 'sweet' && /甜|蜜|糖/.test(haystack)) score += 2;
    else if (pref === 'sour' && /酸|醋/.test(haystack)) score += 2;
    else if (pref === 'salty' && /咸|酱|腌/.test(haystack)) score += 1;
  }
  return score;
}

function isExcluded(dish, excludeTerms) {
  if (!excludeTerms || excludeTerms.length === 0) return false;
  const haystack = `${dish.name || ''} ${dish.description || ''} ${dish.categoryName || ''}`.toLowerCase();
  return excludeTerms.some((term) => term && haystack.includes(term.toLowerCase()));
}

function suggestQuantity(dish, peopleCount) {
  // Staple / rice dishes scale with people
  const name = (dish.name || '').toLowerCase();
  if (/饭|面|粥|粉|饺|包|馒/.test(name)) {
    return Math.max(1, peopleCount || 1);
  }
  return 1;
}

function generateRecommendations(intent, candidates) {
  const { peopleCount = 1, budget, taste = [], exclude = [] } = intent;
  if (!candidates || candidates.length === 0) return [];

  // Filter by exclusion and hard budget ceiling (no single dish > budget)
  let pool = candidates.filter((d) => {
    if (isExcluded(d, exclude)) return false;
    if (budget && Number(d.price) > budget) return false;
    return true;
  });

  // Score each dish
  const scored = pool.map((d) => ({
    dish: d,
    tasteScore: matchTaste(d, taste),
    price: Number(d.price) || 0,
  }));

  // Sort by taste match desc, then price asc
  scored.sort((a, b) => {
    if (b.tasteScore !== a.tasteScore) return b.tasteScore - a.tasteScore;
    return a.price - b.price;
  });

  // Greedy combination: pick dishes while staying under budget
  const picked = [];
  let subtotal = 0;
  const maxItems = Math.min(6, peopleCount <= 2 ? 3 : peopleCount <= 4 ? 5 : 6);

  for (const item of scored) {
    if (picked.length >= maxItems) break;
    const qty = suggestQuantity(item.dish, peopleCount);
    const lineTotal = item.price * qty;
    if (budget && subtotal + lineTotal > budget) continue; // skip if over budget
    const requiresFlavor = Array.isArray(item.dish.flavors) && item.dish.flavors.length > 0;
    picked.push({
      dishId: item.dish.id,
      dish: item.dish,
      quantity: qty,
      unitPrice: item.price,
      lineTotal,
      matchedReasons: buildReasons(item.dish, item.tasteScore, taste, peopleCount),
      requiresFlavor,
    });
    subtotal += lineTotal;
  }

  return picked;
}

function buildReasons(dish, tasteScore, tastePrefs, peopleCount) {
  const reasons = [];
  if (tasteScore > 0) reasons.push('符合口味偏好');
  if (/饭|面|粥|粉|饺|包|馒/.test(dish.name || '')) {
    if (peopleCount > 1) reasons.push(`适合${peopleCount}人份`);
  }
  if (reasons.length === 0) reasons.push('价格适合预算');
  return reasons;
}

// ---------------------------------------------------------------------------
// PriceCalculator
// ---------------------------------------------------------------------------
function calcTotal(recommendations) {
  return recommendations.reduce((sum, r) => sum + (Number(r.unitPrice) || 0) * (r.quantity || 1), 0);
}

function formatPrice(n) {
  return Number(n || 0).toFixed(2);
}

// ---------------------------------------------------------------------------
// CartExecutor – batch add to cart using existing API
// Safety: only adds items WITHOUT flavor requirements. Items requiring flavor
// selection are returned in `needsFlavor` for the UI to handle separately.
// ---------------------------------------------------------------------------
async function addToCart(recommendations) {
  if (!recommendations || recommendations.length === 0) {
    return { success: [], failed: [], needsFlavor: [] };
  }
  const success = [];
  const failed = [];
  const needsFlavor = [];
  for (const rec of recommendations) {
    // Safety: skip items that require user to select a flavor/spec
    if (rec.requiresFlavor) {
      needsFlavor.push(rec);
      continue;
    }
    if (!rec.dishId) {
      failed.push({ ...rec, reason: '缺少菜品 ID' });
      continue;
    }
    // Loop quantity times because backend add() increments by 1 per call
    // Guard: max 10 per dish to prevent abuse
    const qty = Math.min(Math.max(1, rec.quantity || 1), 10);
    let ok = true;
    for (let i = 0; i < qty; i++) {
      try {
        await http.post('/user/shoppingCart/add', { dishId: rec.dishId, dishFlavor: '' });
      } catch (e) {
        failed.push({ ...rec, reason: '加购失败' });
        ok = false;
        break; // stop on first failure for this dish
      }
    }
    if (ok) success.push(rec);
  }
  return { success, failed, needsFlavor };
}

// ---------------------------------------------------------------------------
// AI Explanation – ask LLM for a friendly summary (text only, not executable)
// ---------------------------------------------------------------------------
async function askExplanation(intent, recommendations) {
  const total = calcTotal(recommendations);
  const names = recommendations.map((r) => `${r.dish.name}×${r.quantity}`).join('、');
  const prompt = `用户条件：${intent.rawText || '无具体要求'}。已推荐：${names}。预计总价 ${total.toFixed(2)} 元。请用 1-2 句话简要说明推荐理由，语气友好，不要提及具体价格计算。`;
  try {
    const reply = await http.post('/user/chat', { message: prompt }, { silent: true });
    return (typeof reply === 'string' && reply.trim()) ? reply : null;
  } catch (e) {
    return null;
  }
}

module.exports = {
  parseIntent,
  loadCandidates,
  clearCandidateCache,
  generateRecommendations,
  calcTotal,
  formatPrice,
  addToCart,
  askExplanation,
};
