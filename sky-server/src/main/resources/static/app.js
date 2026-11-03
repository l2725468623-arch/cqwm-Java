'use strict';
const byId = id => document.getElementById(id);
let session = null;
try { session = JSON.parse(sessionStorage.getItem('sky-session')); } catch (_) { sessionStorage.removeItem('sky-session'); }
let navigationVersion = 0;
let employeePage = 1;
let employeeName = '';
let employeeRequest = 0;

function clearSession() { session = null; sessionStorage.removeItem('sky-session'); }
function showError(message) { byId('page-error').textContent = message; byId('page-error').hidden = false; }
async function api(path, options = {}) {
  const headers = new Headers(options.headers || {});
  if (session?.token) headers.set('token', session.token);
  if (options.body) headers.set('Content-Type', 'application/json');
  let response;
  try { response = await fetch(path, { ...options, headers }); }
  catch (_) { throw new Error('无法连接服务，请确认服务已启动后重试'); }
  if (response.status === 401) {
    clearSession();
    location.hash = '/login';
    throw new Error('登录已过期，请重新登录');
  }
  let result;
  try { result = await response.json(); }
  catch (_) { throw new Error('服务返回异常，请稍后重试'); }
  if (!response.ok || result.code !== 1) throw new Error(result.msg || '操作失败，请稍后重试');
  return result.data;
}

byId('toggle-password').addEventListener('click', () => {
  const visible = byId('password').type === 'password';
  byId('password').type = visible ? 'text' : 'password';
  byId('toggle-password').setAttribute('aria-label', visible ? '隐藏密码' : '显示密码');
  byId('toggle-password').setAttribute('aria-pressed', String(visible));
});
byId('login-form').addEventListener('submit', async event => {
  event.preventDefault();
  const submit = byId('login-submit');
  if (submit.disabled) return;
  byId('login-error').textContent = '';
  submit.disabled = true;
  submit.textContent = '登录中…';
  try {
    const data = await api('/admin/employee/login', {method: 'POST', body: JSON.stringify({
      username: byId('username').value.trim(), password: byId('password').value
    })});
    session = data;
    sessionStorage.setItem('sky-session', JSON.stringify(data));
    byId('password').value = '';
    location.hash = '/';
  } catch (error) { byId('login-error').textContent = error.message; }
  finally { submit.disabled = false; submit.textContent = '登录'; }
});
byId('logout-button').addEventListener('click', async () => {
  byId('logout-button').disabled = true;
  try { await api('/admin/employee/logout', {method: 'POST'}); }
  catch (_) { /* 本地仍清除当前标签页的令牌。 */ }
  finally {
    navigationVersion++;
    clearSession();
    byId('logout-button').disabled = false;
    location.hash = '/login';
  }
});

async function renderRoute() {
  const version = ++navigationVersion;
  const path = location.hash.slice(1) || '/';
  const login = !session?.token || path === '/login';
  byId('login-view').hidden = !login;
  byId('admin-view').hidden = login;
  document.title = login ? '苍穹外卖 · 登录' : '苍穹外卖 · 管理端';
  if (login) {
    if (path !== '/login') { location.hash = '/login'; return; }
    return;
  }
  const employees = path === '/employee';
  byId('page-error').hidden = true;
  byId('dashboard-section').hidden = employees;
  byId('employee-section').hidden = !employees;
  byId('page-title').textContent = employees ? '员工管理' : '工作台';
  document.querySelectorAll('[data-route]').forEach(link => link.classList.toggle('active', link.dataset.route === (employees ? 'employee' : 'home')));
  try {
    const profile = await api('/admin/employee/profile');
    if (version !== navigationVersion || !session) return;
    byId('account-name').textContent = profile.name;
    byId('welcome-name').textContent = `${profile.name}，欢迎回来`;
    byId('profile-name').textContent = profile.name;
    byId('profile-username').textContent = profile.username;
    byId('profile-phone').textContent = profile.phone || '未填写';
    if (employees) { await loadEmployees(); return; }
    byId('today-label').textContent = new Intl.DateTimeFormat('zh-CN', {timeZone:'Asia/Shanghai',year:'numeric',month:'long',day:'numeric',weekday:'long'}).format(new Date());
    const overview = await api('/admin/dashboard/overview');
    if (version !== navigationVersion || !session) return;
    for (const key of ['employee', 'category', 'dish', 'order']) byId(`metric-${key}`).textContent = Number(overview[`${key}Count`]).toLocaleString('zh-CN');
  } catch (error) { if (version === navigationVersion && session) showError(error.message); }
}

async function loadEmployees() {
  const request = ++employeeRequest;
  const routeVersion = navigationVersion;
  byId('employee-loading').hidden = false;
  byId('employee-empty').hidden = true;
  byId('employee-rows').replaceChildren();
  byId('previous-page').disabled = true;
  byId('next-page').disabled = true;
  byId('page-error').hidden = true;
  try {
    const query = new URLSearchParams({page:String(employeePage), pageSize:'10', name:employeeName});
    const data = await api(`/admin/employee/page?${query}`);
    if (request !== employeeRequest || routeVersion !== navigationVersion || !session) return;
    const total = Number(data.total);
    for (const employee of data.records) {
      const row = document.createElement('tr');
      for (const value of [employee.name, employee.username, employee.phone]) {
        const cell = document.createElement('td'); cell.textContent = value || '—'; row.append(cell);
      }
      const statusCell = document.createElement('td');
      const status = document.createElement('span');
      status.className = Number(employee.status) === 1 ? 'status-tag' : 'status-tag disabled';
      status.textContent = Number(employee.status) === 1 ? '正常' : '已禁用';
      statusCell.append(status); row.append(statusCell);
      const created = document.createElement('td'); created.textContent = employee.createTime || '—'; row.append(created);
      byId('employee-rows').append(row);
    }
    byId('employee-empty').hidden = data.records.length !== 0;
    byId('employee-total').textContent = `共 ${total} 条`;
    byId('page-number').textContent = String(employeePage);
    byId('previous-page').disabled = employeePage <= 1;
    byId('next-page').disabled = employeePage * 10 >= total;
  } catch (error) { if (request === employeeRequest && routeVersion === navigationVersion && session) showError(error.message); }
  finally { if (request === employeeRequest) byId('employee-loading').hidden = true; }
}
byId('employee-search').addEventListener('submit', event => {event.preventDefault(); employeePage = 1; employeeName = byId('employee-name').value.trim(); loadEmployees();});
byId('reset-search').addEventListener('click', () => {byId('employee-name').value = ''; employeeName = ''; employeePage = 1; loadEmployees();});
byId('previous-page').addEventListener('click', () => {if (employeePage > 1) {employeePage--; loadEmployees();}});
byId('next-page').addEventListener('click', () => {employeePage++; loadEmployees();});
window.addEventListener('hashchange', renderRoute);
renderRoute();
