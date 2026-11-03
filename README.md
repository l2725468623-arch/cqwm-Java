# 苍穹外卖 · cqwm

按课程第一天的环境搭建和登录流程整理的本地学习项目。已完成登录界面、员工登录、JWT 鉴权、退出、员工查询和工作台真实数据读取。菜品、套餐、订单的新增修改等业务留待后续课程实现。

## 本地启动

1. 保持 MySQL 8.4 在本机 `127.0.0.1:3306` 运行。
2. 双击根目录 **启动项目.cmd**，在本地窗口输入 MySQL 的 `root` 密码。密码只在当前进程内使用，不写入项目文件。
3. 打开 <http://localhost:8080/#/login>。
4. 首次创建的课程账号是 **admin / 123456**。已有同名账号会保留原密码。
5. 停止服务时双击 **停止项目.cmd**。

启动脚本会按需构建项目、创建缺失的 11 张表、添加缺失的管理员账号并启动 Java 服务。启动前会检查端口，停止时会核对进程归属。日志位于 `logs/`，进程记录位于 `.local/`，均已排除上传。

前端静态资源由 Spring Boot 一起提供，首次运行不需要安装 Node 或配置 Nginx。`deploy/nginx.conf` 是可选反向代理配置，可用于课程中的 `http://localhost/#/login` 地址。

## IDEA 中运行

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
- `POST /admin/employee/logout`：退出接口；前端清除当前标签页的令牌。
- `GET /admin/dashboard/overview`：员工、分类、菜品、订单数量。
- `GET /health`：后端与数据库连通性。
- <http://localhost:8080/doc.html>：Knife4j 接口文档。

登录外的管理端接口要求 `token` 请求头。令牌有效期两小时，随机密钥随启动脚本每次运行重新生成；账号被禁用后旧令牌也不能继续访问。退出采用课程的无状态 JWT 方式，不建立服务端令牌黑名单。

## 构建与验证

```shell
mvn package
```

自动集成测试使用独立 H2 内存库，验证真实 Controller → Service → Mapper 调用，以及错误密码、禁用账号、过期/缺失令牌、参数校验、SQL 参数化和响应不泄露密码。不会修改本机 MySQL 的业务数据。

为方便跟随原版课程，保留 Spring Boot 2.7、`javax` 和课程的 MD5 密码格式，具体补丁与 Lombok 版本已适配本机 JDK 21。该项目用于本地课程学习。

## 参考资源

- [课程数据库结构镜像](https://github.com/Danyhug/heima_sky_take_out/blob/main/sky_take_out.sql)
- [Spring Boot 2.7.18 文档](https://docs.spring.io/spring-boot/docs/2.7.18/reference/html/)
- 登录图片与标志的来源见 `sky-server/src/main/resources/static/assets/SOURCES.md`。
