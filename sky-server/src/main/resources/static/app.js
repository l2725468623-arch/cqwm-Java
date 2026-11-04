'use strict';
const byId = id => document.getElementById(id);
let session = null;
try { session = JSON.parse(sessionStorage.getItem('sky-session')); } catch (_) { sessionStorage.removeItem('sky-session'); }
let signInDestination = '/';
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
  if (options.body && !(options.body instanceof FormData)) headers.set('Content-Type', 'application/json');
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
    byId('sign-in-dialog').close();
    location.hash = signInDestination;
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
  document.title = login ? '苍穹外卖 · 让好生意从容发生' : '苍穹外卖 · 管理端';
  document.body.classList.toggle('is-public', login);
  if (!login) byId('sign-in-dialog').close();
  if (login) {
    closeSidebar();
    closeAllDialogs();
    if (path !== '/login') { location.hash = '/login'; return; }
    return;
  }
  window.scrollTo({top:0,behavior:'instant'});
  const route = ({'/':'home','/statistics':'statistics','/order':'order','/setmeal':'setmeal','/dish':'dish','/category':'category','/employee':'employee'})[path];
  if (!route) { location.hash='/'; return; }
  const feature = ['order','setmeal'].includes(route);
  const dishes = route === 'dish';
  const statistics = route === 'statistics';
  const employees = path === '/employee';
  const categories = path === '/category';
  closeAllDialogs();
  closeSidebar();
  byId('statistics-section').hidden = !statistics;
  byId('feature-section').hidden = !feature;
  byId('dish-section').hidden = !dishes;
  byId('page-error').hidden = true;
  byId('dashboard-section').hidden = route !== 'home';
  byId('category-section').hidden = !categories;
  byId('employee-section').hidden = !employees;
  byId('page-title').textContent = {'home': '工作台', 'statistics': '数据统计', 'order': '订单管理', 'setmeal': '套餐管理', 'dish': '菜品管理', 'category': '分类管理', 'employee': '员工管理'}[route];
  document.querySelectorAll('[data-route]').forEach(link => { const active=link.dataset.route===route;link.classList.toggle('active',active);if(active)link.setAttribute('aria-current','page');else link.removeAttribute('aria-current'); });
  try {
    const profile = await api('/admin/employee/profile');
    if (version !== navigationVersion || !session) return;
    byId('account-name').textContent = profile.name;
    byId('welcome-name').textContent = `${profile.name}，欢迎回来。`;
    byId('profile-name').textContent = profile.name;
    byId('profile-username').textContent = profile.username;
    byId('profile-phone').textContent = profile.phone || '未填写';
    if (employees) { await loadEmployees(); return; }
    if (categories) { await loadCategories(); return; }
    if (statistics) { await loadStatistics(version); return; }
    if (feature) { await showFeature(route,version); return; }
    if (dishes) { await loadDishCategoryFilter(); await loadDishes(); return; }
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


function closeAllDialogs() { for (const id of ['editor-dialog','confirm-dialog','dish-editor']) { const dialog=byId(id); if(dialog.open) dialog.close(); } }
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
function closeSidebar() { document.body.classList.remove('sidebar-open');byId('sidebar-scrim').hidden=true;byId('menu-toggle').setAttribute('aria-expanded','false');byId('menu-toggle').setAttribute('aria-label','打开导航'); }
byId('menu-toggle').addEventListener('click',()=>{const open=!document.body.classList.contains('sidebar-open');if(!open){closeSidebar();return;}document.body.classList.add('sidebar-open');byId('sidebar-scrim').hidden=false;byId('menu-toggle').setAttribute('aria-expanded','true');byId('menu-toggle').setAttribute('aria-label','关闭导航');});
byId('sidebar-scrim').addEventListener('click',closeSidebar);
document.addEventListener('keydown',event=>{if(event.key==='Escape')closeSidebar();});
async function loadStatistics(version=navigationVersion) {
  const button=byId('refresh-statistics');button.disabled=true;byId('statistics-updated').textContent='正在更新…';
  byId('page-error').hidden=true;
  try{const data=await api('/admin/dashboard/overview');if(version!==navigationVersion||!session)return;
    for(const key of ['employee','category','dish','order'])byId('statistics-'+key).textContent=Number(data[key+'Count']).toLocaleString('zh-CN');
    byId('statistics-updated').textContent='更新于 '+new Intl.DateTimeFormat('zh-CN',{timeZone:'Asia/Shanghai',hour:'2-digit',minute:'2-digit'}).format(new Date());
  }catch(error){if(version===navigationVersion&&session){showError(error.message);byId('statistics-updated').textContent='数据更新失败，请重试。';}}finally{button.disabled=false;}
}
byId('refresh-statistics').addEventListener('click',()=>loadStatistics());
async function showFeature(route,version) {
  byId('oss-upload').hidden=route==='order';
  const content={dish:['菜品管理','维护菜品、口味和售卖状态。','菜品管理尚未开放','可以先建立菜品分类，为整理菜单做好准备。','当前菜品总数','dish'],setmeal:['套餐管理','组合餐厅美味，管理套餐内容与售卖状态。','套餐管理尚未开放','可以先维护套餐分类，整理餐厅的套餐结构。',null,null],order:['订单管理','集中查看订单，跟进每一份餐点。','订单处理尚未开放','当前可查看订单累计数量，具体订单处理功能暂未开放。','当前订单总数','order']}[route];
  byId('feature-title').textContent=content[0];byId('feature-description').textContent=content[1];byId('feature-message').textContent=content[2];byId('feature-guidance').textContent=content[3];
  byId('feature-symbol').textContent=route==='order'?'↗':route==='setmeal'?'＋':'◉';
  byId('feature-category-link').hidden=route==='order';byId('feature-count-line').hidden=!content[4];byId('feature-count-label').textContent=content[4]||'';byId('feature-count').textContent='—';
  if(content[5])try{const data=await api('/admin/dashboard/overview');if(version===navigationVersion&&session)byId('feature-count').textContent=Number(data[content[5]+'Count']).toLocaleString('zh-CN');}catch(error){if(version===navigationVersion&&session)showError(error.message);}
}
byId('oss-choose').addEventListener('click',()=>byId('oss-file').click());
byId('oss-file').addEventListener('change',()=>{
  const file=byId('oss-file').files[0];byId('oss-submit').disabled=!file;byId('oss-filename').textContent=file?file.name:'尚未选择图片';byId('oss-feedback').textContent='';
  if(file&&file.size>5*1024*1024){byId('oss-feedback').textContent='图片大小不能超过5MB';byId('oss-submit').disabled=true;}
});
byId('oss-upload-form').addEventListener('submit',async event=>{
  event.preventDefault();const file=byId('oss-file').files[0];if(!file)return;
  byId('oss-submit').disabled=true;byId('oss-feedback').textContent='正在上传，请稍候…';
  try{
    const form=new FormData();form.append('file',file);const result=await api('/admin/common/upload',{method:'POST',body:form});
    byId('oss-object-key').value=result.objectKey;byId('oss-preview').src=result.url;byId('oss-open').href=result.url;byId('oss-result').hidden=false;
    byId('oss-feedback').textContent='上传成功，图片已保存到云端。';
    byId('oss-file').value='';byId('oss-filename').textContent='尚未选择图片';
  }catch(error){byId('oss-feedback').textContent=error.message;}
  finally{byId('oss-submit').disabled=!byId('oss-file').files[0];}
});
byId('oss-copy').addEventListener('click',async()=>{try{await navigator.clipboard.writeText(byId('oss-object-key').value);byId('oss-feedback').textContent='图片标识已复制。';}catch(_){byId('oss-object-key').select();byId('oss-feedback').textContent='请复制选中的图片标识。';}});


let dishPage=1;let dishPageSize=10;let dishRequest=0;let dishFilters={name:'',categoryId:'',status:''};
let dishRecords=[];const dishSelected=new Set();const dishImageCache=new Map();
let dishEditingId=null;let dishImageKey='';let dishEditorVersion=0;let dishUploading=false;
async function imageUrl(key){
  if(!key)return '';
  if(key.startsWith('https://'))return key;
  const cached=dishImageCache.get(key);if(cached && cached.until>Date.now())return cached.url;
  const result=await api('/admin/common/file-url?'+new URLSearchParams({objectKey:key}));
  dishImageCache.set(key,{url:result.url,until:Date.now()+Math.max(1,Number(result.expiresInSeconds)-60)*1000});return result.url;
}
function setImage(image,key){
  image.hidden=true;if(!key)return;
  imageUrl(key).then(url=>{if(image.isConnected&&url){image.src=url;image.hidden=false;}}).catch(()=>{image.hidden=true;});
}
function categoryOptions(select,categories,placeholder){
  select.replaceChildren();const initial=document.createElement('option');initial.value='';initial.textContent=placeholder;select.append(initial);
  for(const category of categories){const option=document.createElement('option');option.value=String(category.id);option.textContent=category.name;select.append(option);}
}
async function loadDishCategoryFilter(){const options=await api('/admin/category/list?type=1');categoryOptions(byId('dish-category'),options,'全部分类');byId('dish-category').value=dishFilters.categoryId;}
function updateDishSelection(){
  byId('dish-batch-delete').disabled=!dishSelected.size;byId('dish-batch-delete').textContent=dishSelected.size?`批量删除（${dishSelected.size}）`:'批量删除';
  byId('dish-select-all').checked=dishRecords.length>0 && dishSelected.size===dishRecords.length;byId('dish-select-all').indeterminate=dishSelected.size>0 && dishSelected.size<dishRecords.length;
}
function deleteDishes(ids){confirmAction(`确定删除选中的${ids.length}道菜品？起售中或关联套餐的菜品不能删除。`,async()=>{await api('/admin/dish?'+new URLSearchParams({ids:ids.join(',')}),{method:'DELETE'});await loadDishes();});}
async function loadDishes(){
  const request=++dishRequest;const routeVersion=navigationVersion;byId('dish-loading').hidden=false;byId('dish-empty').hidden=true;byId('dish-rows').replaceChildren();
  byId('dish-previous').disabled=true;byId('dish-next').disabled=true;byId('page-error').hidden=true;dishRecords=[];dishSelected.clear();updateDishSelection();
  try{
    const params=new URLSearchParams({page:String(dishPage),pageSize:String(dishPageSize)});for(const [key,value] of Object.entries(dishFilters))if(value!=='')params.set(key,value);
    const result=await api('/admin/dish/page?'+params);if(request!==dishRequest||routeVersion!==navigationVersion||!session)return;
    if(!result.records.length && dishPage>1){dishPage--;return loadDishes();}
    dishRecords=result.records;
    for(const dish of dishRecords){
      const row=document.createElement('tr');const selectCell=document.createElement('td');const checkbox=document.createElement('input');checkbox.type='checkbox';checkbox.setAttribute('aria-label','选择菜品'+dish.name);checkbox.dataset.dishId=String(dish.id);
      checkbox.addEventListener('change',()=>{if(checkbox.checked)dishSelected.add(String(dish.id));else dishSelected.delete(String(dish.id));updateDishSelection();});selectCell.append(checkbox);row.append(selectCell);
      const imageCell=document.createElement('td');const image=document.createElement('img');image.className='dish-thumb';image.alt=dish.name;image.referrerPolicy='no-referrer';image.hidden=true;image.addEventListener('error',()=>{image.hidden=true;});imageCell.append(image);row.append(imageCell);
      for(const value of [dish.name,dish.categoryName||'未分类','¥'+Number(dish.price).toFixed(2)]){const cell=document.createElement('td');cell.textContent=value;row.append(cell);}
      const statusCell=document.createElement('td');const badge=document.createElement('span');badge.className=Number(dish.status)===1?'status-tag':'status-tag disabled';badge.textContent=Number(dish.status)===1?'起售中':'已停售';statusCell.append(badge);row.append(statusCell);
      const time=document.createElement('td');time.textContent=dish.updateTime||'—';row.append(time);
      const operations=document.createElement('td');operations.className='row-actions';operations.append(actionButton('编辑',()=>openDishEditor(dish.id)));
      const next=Number(dish.status)===1?0:1;operations.append(actionButton(next?'起售':'停售',()=>confirmAction(`确定${next?'起售':'停售'}“${dish.name}”？`,async()=>{await api(`/admin/dish/status/${next}?id=${encodeURIComponent(dish.id)}`,{method:'POST'});await loadDishes();})));
      const remove=actionButton('删除',()=>deleteDishes([String(dish.id)]),true);if(Number(dish.status)===1){remove.disabled=true;remove.title='请先停售，再删除菜品';}operations.append(remove);row.append(operations);byId('dish-rows').append(row);setImage(image,dish.image);
    }
    const total=Number(result.total);byId('dish-total').textContent=`共 ${total} 条`;byId('dish-page').textContent=String(dishPage);byId('dish-previous').disabled=dishPage<=1;byId('dish-next').disabled=dishPage*dishPageSize>=total;byId('dish-empty').hidden=dishRecords.length>0;updateDishSelection();
  }catch(error){if(request===dishRequest&&routeVersion===navigationVersion&&session)showError(error.message);}finally{if(request===dishRequest)byId('dish-loading').hidden=true;}
}
function addFlavor(name='',values=''){
  if(byId('dish-flavors').children.length>=20){byId('dish-editor-error').textContent='最多添加20组口味';return;}
  const row=document.createElement('div');row.className='dish-flavor-row';
  const nameLabel=document.createElement('label');nameLabel.textContent='口味名称';const nameInput=document.createElement('input');nameInput.className='flavor-name';nameInput.required=true;nameInput.maxLength=32;nameInput.placeholder='如：辣度';nameInput.value=name;nameLabel.append(nameInput);
  const valueLabel=document.createElement('label');valueLabel.textContent='口味选项';const valueInput=document.createElement('input');valueInput.className='flavor-values';valueInput.required=true;valueInput.placeholder='不辣，微辣，中辣';valueInput.value=values;valueLabel.append(valueInput);
  const remove=document.createElement('button');remove.type='button';remove.className='icon-close';remove.textContent='×';remove.setAttribute('aria-label','删除此口味');remove.addEventListener('click',()=>row.remove());row.append(nameLabel,valueLabel,remove);byId('dish-flavors').append(row);
}
async function openDishEditor(id=null){
  const version=++dishEditorVersion;try{
    const [categories,detail]=await Promise.all([api('/admin/category/list?type=1'),id?api('/admin/dish/'+encodeURIComponent(id)):Promise.resolve({status:0,flavors:[]})]);
    if(version!==dishEditorVersion||!session)return;
    dishEditingId=id;dishImageKey=detail.image||'';dishUploading=false;byId('dish-form').reset();byId('dish-editor-error').textContent='';byId('dish-editor-title').textContent=id?'编辑菜品':'新增菜品';
    categoryOptions(byId('dish-edit-category'),categories,'请选择菜品分类');
    if(detail.categoryId && !categories.some(c=>String(c.id)===String(detail.categoryId))){const option=document.createElement('option');option.value=String(detail.categoryId);option.textContent=(detail.categoryName||'原分类')+'（已禁用，请重新选择）';option.disabled=true;byId('dish-edit-category').append(option);}
    byId('dish-edit-category').value=detail.categoryId?String(detail.categoryId):'';byId('dish-category-hint').hidden=categories.length>0;
    byId('dish-edit-name').value=detail.name||'';byId('dish-edit-price').value=detail.price??'';byId('dish-edit-description').value=detail.description||'';byId('dish-edit-status').value=String(detail.status??0);
    byId('dish-edit-image').hidden=true;if(dishImageKey){const url=await imageUrl(dishImageKey).catch(()=>null);if(version!==dishEditorVersion||!session)return;if(url){byId('dish-edit-image').src=url;byId('dish-edit-image').hidden=false;}}
    byId('dish-flavors').replaceChildren();for(const flavor of detail.flavors||[]){let values;try{values=JSON.parse(flavor.value).join('，');}catch(_){values=flavor.value;}addFlavor(flavor.name,values);}
    byId('dish-save').disabled=false;byId('dish-upload').disabled=false;byId('dish-upload').textContent='上传图片';byId('dish-editor').showModal();
  }catch(error){if(version===dishEditorVersion&&session)showError(error.message);}
}
byId('dish-add').addEventListener('click',()=>openDishEditor());
byId('dish-add-flavor').addEventListener('click',()=>addFlavor());
for(const id of ['dish-close','dish-cancel'])byId(id).addEventListener('click',()=>byId('dish-editor').close());
byId('dish-editor').addEventListener('close',()=>{dishEditorVersion++;});
byId('dish-upload').addEventListener('click',()=>byId('dish-image-file').click());
byId('dish-image-file').addEventListener('change',async()=>{
  const file=byId('dish-image-file').files[0];if(!file)return;const version=dishEditorVersion;
  if(file.size>5*1024*1024){byId('dish-editor-error').textContent='图片大小不能超过5MB';return;}
  dishUploading=true;byId('dish-save').disabled=true;byId('dish-upload').disabled=true;byId('dish-upload').textContent='上传中…';byId('dish-editor-error').textContent='';
  try{const form=new FormData();form.append('file',file);const result=await api('/admin/common/upload',{method:'POST',body:form});if(version!==dishEditorVersion)return;dishImageKey=result.objectKey;dishImageCache.set(result.objectKey,{url:result.url,until:Date.now()+Math.max(1,Number(result.expiresInSeconds)-60)*1000});byId('dish-edit-image').src=result.url;byId('dish-edit-image').hidden=false;}
  catch(error){if(version===dishEditorVersion)byId('dish-editor-error').textContent=error.message;}
  finally{if(version===dishEditorVersion){dishUploading=false;byId('dish-save').disabled=false;byId('dish-upload').disabled=false;byId('dish-upload').textContent='上传图片';byId('dish-image-file').value='';}}
});
byId('dish-form').addEventListener('submit',async event=>{
  event.preventDefault();if(dishUploading||byId('dish-save').disabled)return;byId('dish-editor-error').textContent='';
  if(!dishImageKey){byId('dish-editor-error').textContent='请先上传菜品图片';return;}
  const dto={name:byId('dish-edit-name').value.trim(),categoryId:byId('dish-edit-category').value,price:byId('dish-edit-price').value,image:dishImageKey,description:byId('dish-edit-description').value.trim(),status:Number(byId('dish-edit-status').value),flavors:[]};
  for(const row of byId('dish-flavors').children){const options=row.querySelector('.flavor-values').value.split(/[,，]/).map(x=>x.trim()).filter(Boolean);dto.flavors.push({name:row.querySelector('.flavor-name').value.trim(),value:JSON.stringify(options)});}
  if(dishEditingId)dto.id=dishEditingId;const version=dishEditorVersion;byId('dish-save').disabled=true;
  try{await api('/admin/dish',{method:dishEditingId?'PUT':'POST',body:JSON.stringify(dto)});if(version!==dishEditorVersion)return;byId('dish-editor').close();await loadDishes();}
  catch(error){if(version===dishEditorVersion)byId('dish-editor-error').textContent=error.message;}
  finally{if(version===dishEditorVersion)byId('dish-save').disabled=false;}
});
byId('dish-search').addEventListener('submit',event=>{event.preventDefault();dishPage=1;dishFilters={name:byId('dish-name').value.trim(),categoryId:byId('dish-category').value,status:byId('dish-status').value};loadDishes();});
byId('dish-reset').addEventListener('click',()=>{byId('dish-search').reset();dishFilters={name:'',categoryId:'',status:''};dishPage=1;loadDishes();});
byId('dish-page-size').addEventListener('change',()=>{dishPageSize=Number(byId('dish-page-size').value);dishPage=1;loadDishes();});
byId('dish-previous').addEventListener('click',()=>{if(dishPage>1){dishPage--;loadDishes();}});
byId('dish-next').addEventListener('click',()=>{dishPage++;loadDishes();});
byId('dish-select-all').addEventListener('change',()=>{dishSelected.clear();for(const box of byId('dish-rows').querySelectorAll('input[type=checkbox]')){box.checked=byId('dish-select-all').checked;if(box.checked)dishSelected.add(box.dataset.dishId);}updateDishSelection();});
byId('dish-batch-delete').addEventListener('click',()=>{if(dishSelected.size)deleteDishes([...dishSelected]);});

// Product presentation and the real login form share one session and route system.
document.querySelectorAll('[data-open-signin]').forEach(button => {
  button.addEventListener('click', () => {
    signInDestination = button.dataset.signinRoute || '/';
    byId('login-error').textContent = '';
    byId('sign-in-dialog').showModal();
  });
});
byId('close-signin').addEventListener('click', () => byId('sign-in-dialog').close());
byId('sign-in-dialog').addEventListener('click', event => {
  const bounds = event.currentTarget.getBoundingClientRect();
  if (event.target === event.currentTarget &&
      (event.clientX < bounds.left || event.clientX > bounds.right || event.clientY < bounds.top || event.clientY > bounds.bottom)) {
    event.currentTarget.close();
  }
});
document.querySelectorAll('[data-product-scroll]').forEach(button => {
  button.addEventListener('click', () => byId(button.dataset.productScroll).scrollIntoView({
    behavior: matchMedia('(prefers-reduced-motion: reduce)').matches ? 'instant' : 'smooth', block:'start'
  }));
});

window.addEventListener('hashchange',renderRoute);renderRoute();
