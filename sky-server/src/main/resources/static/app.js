'use strict';
const byId = id => document.getElementById(id);
let session = null;
try { session = JSON.parse(sessionStorage.getItem('sky-session')); } catch (_) { sessionStorage.removeItem('sky-session'); }
let navigationVersion = 0;
let employeePage = 1;
let employeeName = '';
let employeeRequest = 0;
let categoryPage = 1; let categoryName = ''; let categoryType = ''; let categoryRequest = 0;

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
    closeAllDialogs();
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
  const categories = path === '/category';
  closeAllDialogs();
  byId('page-error').hidden = true;
  byId('dashboard-section').hidden = employees || categories;
  byId('category-section').hidden = !categories;
  byId('employee-section').hidden = !employees;
  byId('page-title').textContent = employees ? '员工管理' : categories ? '分类管理' : '工作台';
  document.querySelectorAll('[data-route]').forEach(link => link.classList.toggle('active', link.dataset.route === (employees ? 'employee' : categories ? 'category' : 'home')));
  try {
    const profile = await api('/admin/employee/profile');
    if (version !== navigationVersion || !session) return;
    byId('account-name').textContent = profile.name;
    byId('welcome-name').textContent = `${profile.name}，欢迎回来`;
    byId('profile-name').textContent = profile.name;
    byId('profile-username').textContent = profile.username;
    byId('profile-phone').textContent = profile.phone || '未填写';
    if (employees) { await loadEmployees(); return; }
    if (categories) { await loadCategories(); return; }
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
      const operations = document.createElement('td'); operations.className='row-actions';
      operations.append(actionButton('编辑', () => openEmployee(employee.id)));
      if (employee.username !== 'admin' && String(employee.id) !== String(session.id)) {
        const next = Number(employee.status) === 1 ? 0 : 1;
        operations.append(actionButton(next ? '启用' : '禁用', () => confirmAction(`${next ? '启用' : '禁用'}员工“${employee.name}”的账号？`,
          async () => { await api(`/admin/employee/status/${next}?id=${encodeURIComponent(employee.id)}`, {method:'POST'}); await loadEmployees(); }), !next));
      }
      row.append(operations);
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


function closeAllDialogs() { for (const id of ['editor-dialog','confirm-dialog']) { const dialog=byId(id); if(dialog.open) dialog.close(); } }
function actionButton(text, action, danger=false) { const button=document.createElement('button');button.type='button';button.className='table-action'+(danger?' danger':'');button.textContent=text;button.addEventListener('click',action);return button; }
let confirmationAction = null;
function confirmAction(text, action) { byId('confirm-text').textContent=text;byId('confirm-error').textContent='';confirmationAction=action;byId('confirm-dialog').showModal(); }
byId('confirm-cancel').addEventListener('click',()=>byId('confirm-dialog').close());
byId('confirm-accept').addEventListener('click',async()=>{const button=byId('confirm-accept');if(button.disabled)return;button.disabled=true;try{await confirmationAction();byId('confirm-dialog').close();}catch(error){byId('confirm-error').textContent=error.message;}finally{button.disabled=false;}});
let editingKind = 'employee'; let editingId = null;
function field(name,label,value='',options=null) {
  const wrapper=document.createElement('label');const title=document.createElement('span');title.textContent=label;wrapper.append(title);
  const control=document.createElement(options?'select':'input');control.name=name;control.required=true;
  if(options) for(const [key,text] of options){const option=document.createElement('option');option.value=key;option.textContent=text;control.append(option);}
  control.value=value ?? '';control.id='edit-'+name;
  if(name==='sort'){control.type='number';control.min='0';control.max='99999';control.step='1';}
  if(name==='username'){control.minLength=2;control.maxLength=32;}
  if(name==='name')control.maxLength=32;
  if(name==='phone'){control.maxLength=11;control.pattern='1[0-9]{10}';control.inputMode='tel';}
  if(name==='idNumber'){control.maxLength=18;control.pattern='([0-9]{15}|[0-9]{17}[0-9Xx])';}
  wrapper.append(control);return wrapper;
}
function showEditor(kind,data={}) {
  editingKind=kind;editingId=data.id || null;byId('editor-error').textContent='';
  byId('editor-title').textContent=(editingId?'编辑':'新增')+(kind==='employee'?'员工':Number(data.type)===2?'套餐分类':'菜品分类');
  byId('editor-fields').replaceChildren();
  const fields=kind==='employee' ? [field('username','用户名',data.username),field('name','员工姓名',data.name),field('phone','手机号',data.phone),field('sex','性别',data.sex ?? '1',[['1','男'],['0','女']]),field('idNumber','身份证号',data.idNumber)] : [field('name','分类名称',data.name),field('type','分类类型',String(data.type||1),[['1','菜品分类'],['2','套餐分类']]),field('sort','排序',data.sort??0)];
  byId('editor-fields').append(...fields);byId('editor-hint').textContent=kind==='employee'&&!editingId?'新增员工默认密码为 123456，账号默认启用。':kind==='category'&&!editingId?'新增分类默认禁用，可在列表中启用。':'';
  byId('editor-dialog').showModal();
}
async function openEmployee(id) {try{showEditor('employee',await api('/admin/employee/'+encodeURIComponent(id)));}catch(error){showError(error.message);}}
byId('add-employee').addEventListener('click',()=>showEditor('employee'));
byId('add-dish-category').addEventListener('click',()=>showEditor('category',{type:1}));
byId('add-setmeal-category').addEventListener('click',()=>showEditor('category',{type:2}));
for(const id of ['close-editor','cancel-editor'])byId(id).addEventListener('click',()=>byId('editor-dialog').close());
byId('editor-form').addEventListener('submit',async event=>{
  event.preventDefault();const button=byId('save-editor');if(button.disabled)return;button.disabled=true;byId('editor-error').textContent='';
  const data=Object.fromEntries(new FormData(event.currentTarget));if(editingId)data.id=editingId;
  if(editingKind==='category'){data.sort=Number(data.sort);data.type=Number(data.type);}
  try{await api('/admin/'+editingKind,{method:editingId?'PUT':'POST',body:JSON.stringify(data)});byId('editor-dialog').close();if(editingKind==='employee')await loadEmployees();else await loadCategories();}
  catch(error){byId('editor-error').textContent=error.message;}finally{button.disabled=false;}
});
async function loadCategories() {
  const request=++categoryRequest;const routeVersion=navigationVersion;byId('category-loading').hidden=false;byId('category-empty').hidden=true;byId('category-rows').replaceChildren();byId('category-previous').disabled=true;byId('category-next').disabled=true;byId('page-error').hidden=true;
  try{
    const query=new URLSearchParams({page:String(categoryPage),pageSize:'10',name:categoryName});if(categoryType)query.set('type',categoryType);
    const data=await api('/admin/category/page?'+query);if(request!==categoryRequest||routeVersion!==navigationVersion||!session)return;
    if(!data.records.length && categoryPage>1){categoryPage--;return loadCategories();}
    for(const category of data.records){
      const row=document.createElement('tr');
      for(const value of [category.name,Number(category.type)===1?'菜品分类':'套餐分类',category.sort]){const cell=document.createElement('td');cell.textContent=String(value);row.append(cell);}
      const cell=document.createElement('td');const badge=document.createElement('span');badge.className=Number(category.status)===1?'status-tag':'status-tag disabled';badge.textContent=Number(category.status)===1?'启用':'禁用';cell.append(badge);row.append(cell);
      const time=document.createElement('td');time.textContent=category.updateTime||'—';row.append(time);
      const operations=document.createElement('td');operations.className='row-actions';operations.append(actionButton('编辑',()=>showEditor('category',category)));
      const next=Number(category.status)===1?0:1;
      operations.append(actionButton(next?'启用':'禁用',()=>confirmAction(`${next?'启用':'禁用'}分类“${category.name}”？`,async()=>{await api(`/admin/category/status/${next}?id=${encodeURIComponent(category.id)}`,{method:'POST'});await loadCategories();}),!next));
      operations.append(actionButton('删除',()=>confirmAction(`确定删除分类“${category.name}”？已关联菜品或套餐的分类不能删除。`,async()=>{await api('/admin/category?id='+encodeURIComponent(category.id),{method:'DELETE'});await loadCategories();}),true));
      row.append(operations);byId('category-rows').append(row);
    }
    const total=Number(data.total);byId('category-empty').hidden=data.records.length!==0;byId('category-total').textContent=`共 ${total} 条`;byId('category-page').textContent=String(categoryPage);byId('category-previous').disabled=categoryPage<=1;byId('category-next').disabled=categoryPage*10>=total;
  }catch(error){if(request===categoryRequest && routeVersion===navigationVersion && session)showError(error.message);}finally{if(request===categoryRequest)byId('category-loading').hidden=true;}
}
byId('category-search').addEventListener('submit',event=>{event.preventDefault();categoryPage=1;categoryName=byId('category-name').value.trim();categoryType=byId('category-type').value;loadCategories();});
byId('reset-category').addEventListener('click',()=>{byId('category-name').value='';byId('category-type').value='';categoryName='';categoryType='';categoryPage=1;loadCategories();});
byId('category-previous').addEventListener('click',()=>{if(categoryPage>1){categoryPage--;loadCategories();}});
byId('category-next').addEventListener('click',()=>{categoryPage++;loadCategories();});
window.addEventListener('hashchange',renderRoute);renderRoute();
