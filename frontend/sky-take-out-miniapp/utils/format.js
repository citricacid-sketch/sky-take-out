// utils/format.js
// Formatting helpers. Kept tiny and side-effect free.

function formatPrice(value) {
  if (value === null || value === undefined || value === '') return '0.00';
  const num = Number(value);
  if (isNaN(num)) return '0.00';
  return num.toFixed(2);
}

function formatCount(value) {
  const num = Number(value) || 0;
  if (num >= 10000) return (num / 10000).toFixed(1) + 'w';
  if (num >= 1000) return (num / 1000).toFixed(1) + 'k';
  return String(num);
}

function formatSales(value) {
  const num = Number(value) || 0;
  if (num > 9999) return '9999+';
  if (num > 999) return '999+';
  return num > 0 ? String(num) : '';
}

// Backend may return flavors[i].value as a JSON string like '["热饮","冷饮"]'.
// Normalize it into an array of option strings.
function parseFlavorOptions(value) {
  if (Array.isArray(value)) return value.map(String);
  if (typeof value === 'string') {
    const trimmed = value.trim();
    if (!trimmed) return [];
    try {
      const parsed = JSON.parse(trimmed);
      if (Array.isArray(parsed)) return parsed.map(String);
      return [String(parsed)];
    } catch (e) {
      return trimmed.split(/[,，]/).map((s) => s.trim()).filter(Boolean);
    }
  }
  return [];
}

module.exports = {
  formatPrice,
  formatCount,
  formatSales,
  parseFlavorOptions,
};
