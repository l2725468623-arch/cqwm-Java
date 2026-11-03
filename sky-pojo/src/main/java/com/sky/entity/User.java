package com.sky.entity;
import java.io.Serializable;
import lombok.*;
import java.time.LocalDateTime;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {
    private Long id;
    private String openid;
    private String name;
    private String phone;
    private String sex;
    private String idNumber;
    private String avatar;
    private LocalDateTime createTime;
}
