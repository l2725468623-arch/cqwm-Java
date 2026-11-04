package com.sky.dto;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.*;
import lombok.Data;
@Data
public class DishDTO {
    @Positive private Long id;
    @NotBlank(message="请输入菜品名称") @Size(max=32) private String name;
    @NotNull(message="请选择菜品分类") @Positive private Long categoryId;
    @NotNull(message="请输入价格") @DecimalMin(value="0.01",message="价格必须大于0")
    @Digits(integer=8,fraction=2,message="价格最多保留两位小数") private BigDecimal price;
    @NotBlank(message="请上传菜品图片")
    @Pattern(regexp="sky-take-out/images/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(png|jpg|webp)",message="请使用已上传图片的标识")
    private String image;
    @Size(max=255) private String description;
    @Min(0) @Max(1) private Integer status;
    @Valid @Size(max=20) private List<@NotNull DishFlavorDTO> flavors=new ArrayList<>();
}
