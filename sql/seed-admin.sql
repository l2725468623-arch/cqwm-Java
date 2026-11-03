USE `sky_take_out`;
-- 课程初始账号：admin / 123456。已有同名账号时保持原账号不变。
INSERT INTO `employee`
  (`name`,`username`,`password`,`phone`,`sex`,`id_number`,`status`,`create_time`,`update_time`)
SELECT '管理员','admin','e10adc3949ba59abbe56e057f20f883e',
       '13800000000','1','000000000000000000',1,NOW(),NOW()
WHERE NOT EXISTS (SELECT 1 FROM `employee` WHERE `username`='admin');
