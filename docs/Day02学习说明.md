# Day02：员工管理与分类管理

## 课程对应

本合集中 Day02 对应 P16–P30；你提供的 P18 是“Day02-03 新增员工_功能测试”。实现依据为公开分集目录与同课程配套讲义，未声称完整观看视频或读取到字幕。

| 课程部分 | 当前实现 |
|---|---|
| 新增员工 | 表单、DTO、Controller、Service、Mapper；默认密码123456的MD5摘要、默认启用、创建与修改审计 |
| 新增员工代码完善 | 重复用户名提示、从JWT获取真实操作人、请求结束清理ThreadLocal |
| 员工分页查询 | 姓名搜索、分页、创建时间显示、长整数ID按字符串返回 |
| 启用禁用员工 | 切换状态、禁用后阻止登录及已签发令牌访问、保护管理员与当前登录账号 |
| 编辑员工 | 按ID读取回显、保存员工资料；不覆盖原密码与创建信息 |
| 导入分类管理 | 新增菜品/套餐分类、分类分页、名称和类型筛选、排序、启用禁用、编辑、删除关联检查 |

## 页面操作

1. 双击项目根目录“启动项目.cmd”，在本地输入MySQL root密码。
2. 打开 http://localhost:8080/#/login，以 admin / 123456 登录（已有账号以其原密码为准）。
3. “员工管理 → 新增员工”：填写用户名、姓名、手机号、性别、身份证号，保存后在列表中搜索。
4. 列表“编辑”会查询后端并回显完整资料；“启用/禁用”会再次确认。
5. “分类管理”中可分别新增菜品和套餐分类。新分类默认禁用；启用后会出现在按类型查询的可选分类中。
6. 分类删除会再次确认；已被菜品或套餐使用时返回说明，保留分类与关联数据。

员工与分类页面均提供姓名/名称搜索、重置、分页、空结果提示、服务端校验提示和保存时防重复点击。

## 代码阅读顺序

```text
sky-pojo/dto/EmployeeDTO
  → sky-server/controller/admin/EmployeeController
  → sky-server/service/EmployeeService
  → sky-server/service/impl/EmployeeServiceImpl
  → sky-server/mapper/EmployeeMapper
  → resources/mapper/EmployeeMapper.xml
```

分类按相同分层查看 CategoryDTO、CategoryPageQueryDTO、CategoryController、CategoryService、CategoryServiceImpl、CategoryMapper 及其 XML。

Day02 阶段由 Service 手动填写审计字段。项目现已完成 Day03 的 AutoFill 切面，统一处理时间和操作人；详见同目录下的 Day03公共字段自动填充.md。

实体类对应数据库，DTO接收表单，VO控制返回给前端的字段。员工响应不包含密码摘要。

## 调试接口

打开 http://localhost:8080/doc.html，先调用员工登录接口，将返回的token作为管理端请求头 `token`。Knife4j 可在“文档管理/全局参数”中添加该请求头。

启动后的Swagger定义：http://localhost:8080/v2/api-docs?group=管理端，可用于接口工具导入。

## 验证

`mvn package` 会运行17项集成测试并打包。

- 9项Day02测试：员工默认密码与审计、重名和参数校验、编辑与状态切换、管理员保护、未登录写入拦截、分类完整流程、两类关联删除保护、分类重名/类型校验、超出JavaScript精确范围的ID。
- 8项原有登录测试继续运行。
- 自动测试使用独立H2内存数据库，本地MySQL数据不会被测试清空。

## 参考

- 视频分集：https://www.bilibili.com/video/BV1TP411v7v6/?p=18
- 配套讲义镜像：https://github.com/shuhongfan/sky-take-out/blob/main/讲义/day02/苍穹外卖-day02.md
- 官方课程目录：https://tj.itcast.cn/news/20230704/1725460472.shtml
