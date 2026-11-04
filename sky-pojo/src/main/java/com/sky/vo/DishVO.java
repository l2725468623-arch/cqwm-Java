package com.sky.vo;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter
public class DishVO extends Dish {
    private String categoryName;
    private List<DishFlavor> flavors;
}
