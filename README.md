# 苍穹外卖 · cqwm

按课程 Day01、Day02 整理的本地学习项目。已完成登录与 JWT 鉴权、员工新增/分页/状态切换/编辑，以及分类新增/查询/编辑/状态切换/删除。菜品管理已支持新增、编辑、图片上传、口味维护、筛选分页、起售/停售以及单条/批量删除；套餐、订单的业务功能留待后续课程实现。

## 本地启动

1. 保持 MySQL 8.4 在本机 `127.0.0.1:3306` 运行。
2. 双击根目录 **启动项目.cmd**，在本地窗口输入 MySQL 的 `root` 密码。密码只在当前进程内使用，不写入项目文件。
3. 打开 <http://localhost:8080/#/login>。
4. 首次创建的课程账号是 **admin / 123456**。已有同名账号会保留原密码。
5. 停止服务时双击 **停止项目.cmd**。

启动脚本会按需构建项目、创建缺失的 11 张表、添加缺失的管理员账号并启动 Java 服务。启动前会检查端口，停止时会核对进程归属。日志位于 `logs/`，进程记录位于 `.local/`，均已排除上传。

前端静态资源由 Spring Boot 一起提供，首次运行不需要安装 Node 或配置 Nginx。`deploy/nginx.conf` 是可选反向代理配置，可用于课程中的 `http://localhost/#/login` 地址。

界面采用苹果产品落地页方向的黑白视觉：登录前为产品介绍与登录弹窗，登录后为轻薄顶部导航，包含工作台、数据统计、订单、套餐、菜品、分类、员工七个入口。使用大面积留白、居中大标题与蓝色胶囊按钮；窄屏提供可折叠导航。员工、分类和菜品管理接入真实业务；数据统计读取现有累计数量；订单、套餐页面明确标注尚未开放。

开发启动脚本优先读取 `sky-server/src/main/resources/static` 中的前端文件，修改 HTML、CSS 或 JavaScript 后刷新浏览器即可查看；打包后的 JAR 也包含完整静态页面。

## IDEA 中运行

首次请双击 **配置IDEA调试.cmd**，在本地输入 MySQL 密码。工具将受文件权限保护的本地连接配置保存在 Git 忽略的 `.local/idea-dev.properties`，并复用已有 OSS 凭证。在 IDEA 右上角选择 **苍穹外卖-IDEA断点调试**，点击小虫子 Debug。详情见 `docs/IDEA断点调试说明.md`。

- 打开根目录 `pom.xml`，重新加载 Maven 项目；项目 SDK 与 Maven 运行 JDK 使用 **JDK 21**。
- 入口：`sky-server` 中的 `com.sky.SkyApplication`。
- 默认激活 `dev` 配置，启动前在 IDEA 运行配置的环境变量中设置 `DB_PASSWORD` 和随机的 `SKY_JWT_ADMIN_SECRET`。
- 或将 `config/application-local.example.yml` 复制为 `config/application-local.yml`，仅在本地补充连接参数与密钥。该实际配置已被 Git 忽略，不会打包进 JAR。
- 可配置：`DB_HOST`、`DB_PORT`、`DB_NAME`、`DB_USER`、`DB_PASSWORD`、`SERVER_PORT`；默认使用 `sky_take_out` 数据库。
- 本地启动脚本固定使用本机 `root`、3306、8080；其他环境建议在 IDEA 中设置上面的变量后运行。

## 目录

```text
sky-common/src/main/java/com/sky
  constant / context / enumeration / exception
  json / properties / result / utils
sky-pojo/src/main/java/com/sky
  entity / dto / vo
sky-server/src/main/java/com/sky
  config / controller/admin / handler / interceptor
  mapper / service/impl / SkyApplication.java
sky-server/src/main/resources
  mapper / static / application.yml / application-dev.yml
sky-server/src/test
  java / resources
sql / scripts / config / deploy
```

`entity` 对应数据库中的全部 11 张表；`common` 包含截图中的异常类型、`JwtProperties`、`AliOssProperties`、`WeChatProperties`、统一响应和日期序列化。OSS、微信、Redis 的属性/基础配置已预留，登录与员工查询只依赖 MySQL，不要求先部署这些服务。

## 接口

- `POST /admin/employee/login`：`{"username":"admin","password":"123456"}`。
- `GET /admin/employee/profile`：当前员工信息。
- `GET /admin/employee/page?page=1&pageSize=10&name=`：分页和姓名搜索。
- `POST /admin/employee`：新增员工，默认密码 `123456`，按课程格式保存 MD5 摘要。
- `POST /admin/employee/status/{status}?id=`：启用（1）或禁用（0）员工。
- `GET /admin/employee/{id}`：编辑前回显员工信息，不返回密码。
- `PUT /admin/employee`：编辑员工资料，保留密码、账号状态和创建信息。
- `POST /admin/employee/logout`：退出接口；前端清除当前标签页的令牌。
- `POST /admin/category`、`PUT /admin/category`：新增、编辑分类。
- `GET /admin/category/page?page=1&pageSize=10&name=&type=`：分类分页和类型筛选。
- `POST /admin/category/status/{status}?id=`：启用或禁用分类。
- `DELETE /admin/category?id=`：删除分类，关联菜品或套餐时拒绝删除。
- `GET /admin/category/list?type=`：按类型查询启用的分类。
- `GET /admin/category/{id}`：查询单个分类。
- `POST /admin/dish`、`PUT /admin/dish`：新增、编辑菜品及口味。
- `GET /admin/dish/page?page=1&pageSize=10&name=&categoryId=&status=`：菜品筛选分页。
- `GET /admin/dish/{id}`：详情及口味回显。
- `DELETE /admin/dish?ids=1,2`：单条或批量删除；起售中或关联套餐时拒绝整批删除。
- `POST /admin/dish/status/{status}?id=`：起售（1）或停售（0）。
- `GET /admin/dashboard/overview`：员工、分类、菜品、订单数量。
- `GET /health`：后端与数据库连通性。
- <http://localhost:8080/doc.html>：Knife4j 接口文档。

登录外的管理端接口要求 `token` 请求头。令牌有效期两小时，随机密钥随启动脚本每次运行重新生成；账号被禁用后旧令牌也不能继续访问。退出采用课程的无状态 JWT 方式，不建立服务端令牌黑名单。

## 构建与验证

```shell
mvn package
```

42 项自动测试使用独立 H2 内存库，验证登录鉴权、员工新增/编辑/状态、分类增删改查、关联删除保护、默认密码摘要、真实操作人记录、大整数 ID、重名和无效输入，并覆盖菜品与口味保存、筛选分页、编辑回显、售卖状态、删除保护及事务回滚。不会修改本机 MySQL 的业务数据。

Day02 功能与课程章节、操作步骤、测试说明见 `docs/Day02学习说明.md`。原来放在 Controller 包中的练习类 `employeeDTO` 保留为兼容类；正式请求对象使用 `sky-pojo` 下的 `com.sky.dto.EmployeeDTO`。

为方便跟随原版课程，保留 Spring Boot 2.7、`javax` 和课程的 MD5 密码格式，具体补丁与 Lombok 版本已适配本机 JDK 21。该项目用于本地课程学习。

## 参考资源

- [课程数据库结构镜像](https://github.com/Danyhug/heima_sky_take_out/blob/main/sky_take_out.sql)
- [Spring Boot 2.7.18 文档](https://docs.spring.io/spring-boot/docs/2.7.18/reference/html/)
- 登录图片与标志的来源见 `sky-server/src/main/resources/static/assets/SOURCES.md`。

## Day03 公共字段自动填充

已加入 AutoFill 注解与 AutoFillAspect 切面，员工、分类、菜品的新增、编辑和状态切换统一填充公共字段。操作说明见 docs/Day03公共字段自动填充.md。Service 中的重复赋值已移除。

## 阿里云 OSS 图片存储

已配置 lay-java（广州），提供登录后图片上传与私有图片临时链接。先双击配置云存储.cmd在本地录入凭证并验证连接，再重新启动项目。详见 docs/云存储使用说明.md。
## 菜品管理

新增、编辑（含口味）、图片上传、筛选分页、单条和批量删除已完成。新增默认停售；起售中或关联套餐的菜品不能删除。使用步骤与接口见 `docs/Day03菜品管理说明.md`。

## 产品风格前端

登录页、工作台及管理列表已统一更新。素材、字体后备方案与验证说明见 `docs/苹果风格界面说明.md`。新增 WebP 主视觉随 JAR 一起打包，页面不依赖远程图片或字体服务。
