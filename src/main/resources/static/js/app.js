(() => {
  'use strict';

  const TOKEN_KEY = 'pse.token';
  const EMAIL_KEY = 'pse.email';

  const state = {
    token: localStorage.getItem(TOKEN_KEY) || '',
    email: localStorage.getItem(EMAIL_KEY) || '',
    authTab: 'login',
    currentProductId: null
  };

  const $ = (id) => document.getElementById(id);
  const priceFmt = new Intl.NumberFormat('en-US', { maximumFractionDigits: 0 });

  function esc(value) {
    return String(value == null ? '' : value)
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#39;');
  }

  function money(value) {
    if (value == null || value === '') return '—';
    return priceFmt.format(Number(value));
  }

  async function api(path, options = {}) {
    const headers = Object.assign({}, options.headers || {});
    if (options.body) headers['Content-Type'] = 'application/json';
    if (state.token) headers.Authorization = 'Bearer ' + state.token;
    const res = await fetch(path, Object.assign({}, options, { headers }));
    if (res.status === 204) return null;
    const data = await res.json().catch(() => null);
    if (!res.ok) {
      const message = data && (data.error || (data.errors && Object.values(data.errors).join(', ')));
      throw new Error(message || ('Request failed (' + res.status + ')'));
    }
    return data;
  }

  function syncAccountUi() {
    $('who').textContent = state.token ? state.email : '';
    $('logout').classList.toggle('hidden', !state.token);
    $('auth-toggle').classList.toggle('hidden', !!state.token);
  }

  function setSession(response) {
    state.token = response.token;
    state.email = response.email;
    localStorage.setItem(TOKEN_KEY, state.token);
    localStorage.setItem(EMAIL_KEY, state.email);
    syncAccountUi();
  }

  function clearSession() {
    state.token = '';
    state.email = '';
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(EMAIL_KEY);
    syncAccountUi();
  }

  function searchParams() {
    const params = new URLSearchParams();
    const q = $('q').value.trim();
    if (q) params.set('q', q);
    if ($('category').value) params.set('category', $('category').value);
    if ($('sortBy').value) params.set('sortBy', $('sortBy').value);
    if ($('minPrice').value) params.set('minPrice', $('minPrice').value);
    if ($('maxPrice').value) params.set('maxPrice', $('maxPrice').value);
    params.set('size', '24');
    return params;
  }

  async function runSearch() {
    const results = $('results');
    results.innerHTML = '<p class="summary">Searching…</p>';
    $('detail').classList.add('hidden');
    try {
      const data = await api('/api/search?' + searchParams().toString());
      $('summary').textContent = data.total + ' product' + (data.total === 1 ? '' : 's') + ' found';
      if (!data.results || data.results.length === 0) {
        results.innerHTML = '<div class="panel"><h2>No matches</h2><p class="hint">Try a broader query or clear the price filters.</p></div>';
        return;
      }
      results.innerHTML = data.results.map((product) => {
        const thumb = product.imageUrl
          ? '<img src="' + esc(product.imageUrl) + '" alt="" loading="lazy" onerror="this.parentNode.textContent=\'No image\'"/>'
          : 'No image';
        return '<article class="card" data-id="' + esc(product.id) + '">'
          + '<div class="thumb">' + thumb + '</div>'
          + '<div class="body">'
          + '<span class="brand-name">' + esc(product.brand || product.category || '') + '</span>'
          + '<h3>' + esc(product.name) + '</h3>'
          + '<span class="price">' + money(product.price) + '</span>'
          + '<span class="meta"><span>' + esc(product.sellerName || 'no seller') + '</span>'
          + '<span class="badge ' + esc(product.availability || '') + '">' + esc((product.availability || '').replaceAll('_', ' ')) + '</span></span>'
          + '</div></article>';
      }).join('');
    } catch (error) {
      $('summary').textContent = '';
      results.innerHTML = '<div class="panel"><h2>Search failed</h2><p class="error">' + esc(error.message) + '</p></div>';
    }
  }

  async function openProduct(productId) {
    state.currentProductId = productId;
    $('detail').classList.remove('hidden');
    $('detail-body').innerHTML = '<div class="panel"><p class="summary">Loading product…</p></div>';
    $('detail').scrollIntoView({ behavior: 'smooth', block: 'start' });
    try {
      const data = await api('/api/products/' + encodeURIComponent(productId));
      renderProduct(data.product, data.priceHistory || []);
    } catch (error) {
      $('detail-body').innerHTML = '<div class="panel"><h2>Could not load product</h2><p class="error">'
        + esc(error.message) + '</p></div>';
    }
  }

  function renderProduct(product, history) {
    const rows = (product.prices || []).map((option) => {
      const link = option.url
        ? '<a href="' + esc(option.url) + '" target="_blank" rel="noopener noreferrer">Visit</a>'
        : '—';
      return '<tr><td>' + esc(option.sellerName) + '</td>'
        + '<td class="price-cell">' + money(option.price) + '</td>'
        + '<td><span class="badge ' + esc(option.availability || '') + '">'
        + esc(String(option.availability || '').replaceAll('_', ' ')) + '</span></td>'
        + '<td>' + link + '</td></tr>';
    }).join('');

    const historyRows = history.slice(0, 12).map((entry) =>
      '<tr><td>' + money(entry.price) + '</td><td>' + esc((entry.recordedAt || '').replace('T', ' ')) + '</td></tr>'
    ).join('');

    const image = product.imageUrl
      ? '<img src="' + esc(product.imageUrl) + '" alt="" style="width:100%;border-radius:12px" onerror="this.remove()"/>'
      : '';

    $('detail-body').innerHTML =
      '<div class="detail-grid">'
      + '<div class="panel">' + image
      + '<h2>' + esc(product.name) + '</h2>'
      + '<p class="brand-name">' + esc(product.brand || '') + '</p>'
      + '<h3>Cheapest</h3><p class="price">' + money(product.cheapestPrice) + '</p>'
      + '<h3>Average</h3><p>' + money(product.averagePrice) + '</p>'
      + '</div>'
      + '<div class="panel">'
      + '<h3>Compare sellers</h3>'
      + '<table><thead><tr><th>Seller</th><th>Price</th><th>Availability</th><th></th></tr></thead>'
      + '<tbody>' + (rows || '<tr><td colspan="4">No seller prices yet.</td></tr>') + '</tbody></table>'
      + '<h3>Price history</h3>'
      + '<table><thead><tr><th>Price</th><th>Recorded</th></tr></thead>'
      + '<tbody>' + (historyRows || '<tr><td colspan="2">No history yet.</td></tr>') + '</tbody></table>'
      + '<h3>Create a price alert</h3>'
      + (state.token
        ? '<form class="alert-form" id="alert-form">'
          + '<input id="alert-price" type="number" min="0.01" step="0.01" placeholder="Target price" required/>'
          + '<button class="primary" type="submit">Notify me</button></form>'
          + '<p id="alert-msg" class="notice"></p>'
        : '<p class="hint">Sign in to create an alert. Alerts are delivered by e-mail.</p>')
      + '</div></div>';

    const form = $('alert-form');
    if (form) {
      form.addEventListener('submit', async (event) => {
        event.preventDefault();
        const targetPrice = Number($('alert-price').value);
        try {
          await api('/api/price-alerts', {
            method: 'POST',
            body: JSON.stringify({ productId: product.productId, targetPrice })
          });
          $('alert-msg').textContent = 'Alert created. We will e-mail you when the price drops.';
          form.reset();
        } catch (error) {
          $('alert-msg').textContent = '';
          $('alert-msg').classList.remove('notice');
          $('alert-msg').classList.add('error');
          $('alert-msg').textContent = error.message;
        }
      });
    }
  }

  function openAuth(tab) {
    state.authTab = tab || 'login';
    $('auth-modal').classList.remove('hidden');
    $('auth-error').textContent = '';
    document.querySelectorAll('.tab').forEach((el) => {
      el.classList.toggle('active', el.dataset.tab === state.authTab);
    });
    $('name-field').classList.toggle('hidden', state.authTab !== 'register');
    $('auth-submit').textContent = state.authTab === 'register' ? 'Create account' : 'Sign in';
    $('auth-password').autocomplete = state.authTab === 'register' ? 'new-password' : 'current-password';
  }

  function closeAuth() {
    $('auth-modal').classList.add('hidden');
    $('auth-error').textContent = '';
  }

  async function submitAuth(event) {
    event.preventDefault();
    const payload = {
      email: $('auth-email').value.trim(),
      password: $('auth-password').value
    };
    if (state.authTab === 'register') payload.displayName = $('auth-name').value.trim();
    const path = state.authTab === 'register' ? '/api/auth/register' : '/api/auth/login';
    $('auth-error').textContent = '';
    try {
      const response = await api(path, { method: 'POST', body: JSON.stringify(payload) });
      setSession(response);
      closeAuth();
      $('auth-form').reset();
      $('who').textContent = state.email;
    } catch (error) {
      $('auth-error').textContent = error.message;
    }
  }

  function wire() {
    $('search-form').addEventListener('submit', (event) => {
      event.preventDefault();
      runSearch();
    });

    $('results').addEventListener('click', (event) => {
      const card = event.target.closest('.card');
      if (card) openProduct(card.dataset.id);
    });

    $('close-detail').addEventListener('click', () => {
      $('detail').classList.add('hidden');
      $('results').scrollIntoView({ behavior: 'smooth', block: 'start' });
    });

    $('auth-toggle').addEventListener('click', () => openAuth('login'));
    $('close-auth').addEventListener('click', closeAuth);
    $('auth-modal').addEventListener('click', (event) => {
      if (event.target === $('auth-modal')) closeAuth();
    });
    $('logout').addEventListener('click', clearSession);
    $('auth-form').addEventListener('submit', submitAuth);
    document.querySelectorAll('.tab').forEach((el) => {
      el.addEventListener('click', () => openAuth(el.dataset.tab));
    });

    syncAccountUi();
    if (state.token) {
      api('/api/auth/me').catch(() => clearSession());
    }
    runSearch();
  }

  document.addEventListener('DOMContentLoaded', wire);
})();
