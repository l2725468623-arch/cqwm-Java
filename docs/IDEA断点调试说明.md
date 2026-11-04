# 在 IDEA 中运行并打断点

## 第一次配置

1. 打开桌面 `cqwm`，双击 **配置IDEA调试.cmd**。
2. 在本地窗口输入 MySQL 的 root 密码。工具先验证 `sky_take_out` 数据库连接，再生成配置；不会初始化或改写业务表。
3. 工具将数据库密码、随机 JWT 密钥及已保存的 OSS 凭证写入 `.local/idea-dev.properties`。这是本地明文配置，文件权限限制为当前 Windows 用户和 SYSTEM；`.local/` 已被 Git 忽略。不要分享这个文件。重新配置云存储后，再运行一次本工具即可同步。
4. 如果之前用“启动项目.cmd”启动了网站，先双击 **停止项目.cmd**，避免8080端口冲突。
5. 在 IDEA 打开项目根目录的 `pom.xml`，等待 Maven 加载完成。项目 SDK 使用已有 **JDK 21**。
6. IDEA 右上角运行配置选择 **苍穹外卖-IDEA断点调试**，点击旁边的 **小虫子 Debug**。该配置的主类是 `com.sky.SkyApplication`，模块是 `sky-server`，工作目录是项目根目录。

若未看到配置，重新打开项目；也可以在“编辑配置”中新建 Application，按项目中的 `.run/idea-debug.run.xml` 填写。配置文件不会包含密码或云密钥。

## 如何跟着代码走

点击左侧行号旁的空白区域，出现红点就表示设置了断点。用 **Debug** 启动后，浏览器访问 `http://localhost:8080/#/login`，登录并执行对应操作，程序才会在相应接口停住。

比如观察“新增菜品”：

1. 在 `DishController.java` 的 `service.save(dto);` 这一行设置断点。
2. 浏览器进入“菜品管理”，填写信息后点击“保存菜品”。
3. IDEA 停住后，在 Variables 中展开 `dto`，查看前端传来的名称、分类、价格、图片标识与口味。
4. 按 **F7** 进入 `DishServiceImpl.save()`，观察分类检查、组装实体、保存菜品、保存口味的顺序。
5. 在 `AutoFillAspect` 里设置断点，可观察新增时创建时间、修改时间、创建人和修改人自动赋值。
6. SQL 由 MyBatis 根据 Mapper 接口及 XML 执行；接口本身没有可逐行执行的 Java 方法体，观察 SQL 参数及执行前后的实体即可。

流程：`浏览器请求 → JWT拦截器 → Controller → Service → Mapper（自动填充切面）→ MyBatis执行SQL → 数据库 → 返回结果`。

分页查询可在 `DishController.page()` 设置断点，然后点击页面“查询”；修改可在 `update()` 设置断点，删除可在 `delete()` 设置断点。

## 调试快捷键

| 按键（默认 Windows 键位） | 用途 |
| --- | --- |
| Shift+F9 | 以 Debug 模式启动当前配置 |
| F8 | 执行当前行，停到下一行，不进入调用方法内部 |
| F7 | 尝试进入当前调用方法 |
| Shift+F8 | 执行完当前方法，返回调用它的位置 |
| F9 | 继续运行，到下一个断点或请求结束 |
| Ctrl+F8 | 添加或取消当前行断点 |
| Alt+F8 | 对表达式求值，适合观察变量或计算结果 |

键位可被自定义；如果功能键由系统接管，可能需要配合 Fn 使用。也可以直接点击 Debug 工具栏对应按钮。

暂停在断点时，浏览器一直加载是正常的：后端请求正在等待你继续执行。按 F9 后页面会继续收到响应。

如果断点没有命中，先确认点击的是 Debug、断点没有被静音、浏览器访问的是8080，以及操作触发的是该方法对应的接口。

## 已验证与限制

已检查 IDEA 运行配置 XML、本地配置脚本语法、配置文件的 Git 忽略规则，并编译验证相关代码。还通过同一个 `SkyApplication` 类在独立 H2 数据库中检查 IDEA 所需的主类、类路径和本地配置导入。

真实 MySQL 的 IDEA 启动仍需要你在本地工具输入密码；没有读取聊天记录中的密码，也没有替你操作 IDEA 的 Debug 按钮。

官方参考：[Application 运行配置](https://www.jetbrains.com/help/idea/run-debug-configuration-java-application.html)、[启动调试会话](https://www.jetbrains.com/help/idea/starting-the-debugger-session.html)。
