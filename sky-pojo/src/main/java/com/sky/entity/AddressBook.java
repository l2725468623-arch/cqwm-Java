package com.sky.entity;
import java.io.Serializable;
import lombok.*;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressBook implements Serializable {
    private Long id;
    private Long userId;
    private String consignee;
    private String sex;
    private String phone;
    private String provinceCode;
    private String provinceName;
    private String cityCode;
    private String cityName;
    private String districtCode;
    private String districtName;
    private String detail;
    private String label;
    private Integer isDefault;
}
