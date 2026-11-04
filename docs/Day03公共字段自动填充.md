# Day03：公共字段自动填充

这部分把每个 Service 都要重复写的“记录时间、记录操作人”集中到一个切面里。

## 三个关键位置

- `sky-server/src/main/java/com/sky/annotation/AutoFill.java`：自定义注解，相当于给 Mapper 方法贴上“需要自动填写”的标签。
- `sky-server/src/main/java/com/sky/aspect/AutoFillAspect.java`：执行 SQL 前，读取标签并填写公共字段。
- `EmployeeMapper`、`CategoryMapper`：在新增、编辑、状态切换方法上标记注解。

`OperationType` 和 `AutoFillConstant` 继续使用 `sky-common` 中已有的定义。

## 会填写什么

| 操作 | createTime | createUser | updateTime | updateUser |
|---|---|---|---|---|
| 新增 INSERT | 当前时间 | 当前登录员工ID | 同一个当前时间 | 当前登录员工ID |
| 修改 UPDATE | 保留原值 | 保留原值 | 当前时间 | 当前登录员工ID |

例如新增员工：

```text
EmployeeServiceImpl.save()
  → EmployeeMapper.insert(employee)，带 INSERT 标签
  → AutoFillAspect 在 SQL 前填写四个字段
  → MyBatis 把完整 employee 对象写入数据库
```

## Mapper 写法

```java
@AutoFill(OperationType.INSERT)
int insert(Employee employee);

@AutoFill(OperationType.UPDATE)
int updateProfile(Employee employee);
```

切面通过实体的 setter 填值，因此被标记方法的第一个参数必须是实体对象，并且提供相应的时间、操作人 setter。查询和删除方法不加这个注解。

员工状态修改已改成传入 Employee 对象，分类状态修改继续传入 Category 对象，让它们也经过统一填充。Service 中相应的重复赋值已移除。

## 当前操作人从哪里来

登录后的 JWT 拦截器读取员工ID，放进 `BaseContext`。切面通过 `BaseContext.getCurrentId()` 取出这个ID。请求结束由拦截器清理。

没有当前操作人时，切面会阻止写入，避免保存一条操作人为空的记录。以后从定时任务等非请求入口调用已标记方法，需要先设置明确的操作人，并在 finally 中清理。

## 验证

`mvn package` 已通过：21项测试，0失败，0错误。

新增4项测试通过真实的 Spring/MyBatis 代理调用验证：新增覆盖不可信的审计字段、编辑与状态切换保留创建信息、分类的完整填充流程、缺少操作人时阻止写入且不影响查询。原有17项登录及员工/分类测试继续通过。

测试使用独立 H2 内存库。这次没有启动服务，也没有修改本机 MySQL 数据。
