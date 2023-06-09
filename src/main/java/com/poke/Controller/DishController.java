package com.poke.Controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.util.BeanUtil;
import com.poke.DTO.DishDTO;
import com.poke.PoJo.Category;
import com.poke.PoJo.Dish;
import com.poke.PoJo.DishFlavor;
import com.poke.Util.R;
import com.poke.service.CategoryService;
import com.poke.service.DishFlavorService;
import com.poke.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@RestController
@Slf4j
@RequestMapping("/dish")
public class DishController {

    @Autowired
    private DishService dishService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private RedisTemplate redisTemplate;

    @GetMapping("/page")
    public R<Page> page(Integer page,Integer pageSize,String name){
        log.info("page = {},pageSize = {}" ,page,pageSize);

        /*
        * 由于Page返回的不满足分类的展示，
        * 创建两个Page*/
        Page<Dish> pageInfo = new Page(page,pageSize);
        Page<DishDTO> page1 = new Page();

        LambdaQueryWrapper<Dish> qw = new LambdaQueryWrapper<>();
        qw.like(name != null,Dish::getName,name)
                .orderByAsc(Dish::getSort);
        dishService.page(pageInfo,qw);

        //将pageInfo的属性拷贝拷贝page1，不拷贝records属性也就是list<Dish>
        //records属性用于保存展示的数据
        BeanUtils.copyProperties(pageInfo,page1,"records");

        //获得到List<Dish>属性
        List<Dish> list = pageInfo.getRecords();
        //新建一个List<DishDTO> ，用于保存数据
        List<DishDTO> list1 =new ArrayList<>();
        //遍历，List<Dish>，将dish数据拷贝到DishDTO上，并为CategoryName属性赋值，并加入到List<DishDTO>集合中
        for(Dish dish : list){
            DishDTO d = new DishDTO();
            BeanUtils.copyProperties(dish,d);
            Long categoryId = dish.getCategoryId();
            Category byId = categoryService.getById(categoryId);
            d.setCategoryName(byId.getName());
            list1.add(d);
        }
        //将Page<DishDTO>中的records属性赋值
        page1.setRecords(list1);
        return R.success(page1);
    }

    @PostMapping
    public R<String> save(@RequestBody DishDTO dishDTO){
        log.info("添加Dish={}",dishDTO);
        dishService.saveWithFlavor(dishDTO);

        //清理缓存 1 全部清理 2.精确清理
//        Set keys = redisTemplate.keys("dish_*");
//        redisTemplate.delete(keys);
        String key = "dish_" + dishDTO.getCategoryId() +"_"+dishDTO.getStatus();
        redisTemplate.delete(key);
        return R.success("添加成功");
    }
    @GetMapping("/{id}")
    public R<DishDTO> getId(@PathVariable Long id){
        return R.success(dishService.getByIdWithFlavor(id));
    }

    @PutMapping
    public R<String> update(@RequestBody DishDTO dishDTO){
        log.info("获取到修改的数据={}",dishDTO.toString());
        dishService.updateWithFlavor(dishDTO);
        //清理缓存 1 全部清理 2.精确清理
//        Set keys = redisTemplate.keys("dish_*");
//        redisTemplate.delete(keys);
        String key = "dish_" + dishDTO.getCategoryId() +"_"+dishDTO.getStatus();
        redisTemplate.delete(key);
        return R.success("添加成功");
    }
//    dish?ids=1397849739276890114
    @DeleteMapping
    public R<String> delete(@RequestParam List<Long> ids){
        log.info("删除列表={}",ids);
        dishService.removeBeachWithDishFlavorById(ids);
        return R.success("删除成功");
    }
//    http://localhost:8080/dish/status/0?ids=1397850140982161409

    @PostMapping("/status/{status}")
    public R<String> updateStatus(@PathVariable Integer status,@RequestParam String ids){
        log.info("状态为={},ids为={}",status,ids);
        //转list，因为用String时Mybatis会报丢失精度问题，业务无法处理
        List<String> list = Arrays.asList(ids.split(","));
        LambdaUpdateWrapper<Dish> lambdaUpdateWrapper = new LambdaUpdateWrapper<>();
        lambdaUpdateWrapper.set(Dish::getStatus,status)
                        .in(Dish::getId,list);
       dishService.update(lambdaUpdateWrapper);
       return R.success("状态修改成功");

    }

//    @GetMapping("/list")
//    public R<List<Dish>> list(Dish dish){
//        LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper();
//        lambdaQueryWrapper.eq(Dish::getCategoryId,dish.getCategoryId())
//                .eq(Dish::getStatus,1)
//                .orderByAsc(Dish::getSort)
//                .orderByDesc(Dish::getUpdateTime);
//        List<Dish> list = dishService.list(lambdaQueryWrapper);
//        return R.success(list);
//    }

    //重写list，返回有口味数据

//    @GetMapping("/list")
//    public R<List<DishDTO>> list(Dish dish){
//        LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper();
//        lambdaQueryWrapper.eq(Dish::getCategoryId,dish.getCategoryId())
//                .eq(Dish::getStatus,1)
//                .orderByAsc(Dish::getSort)
//                .orderByDesc(Dish::getUpdateTime);
//        List<Dish> list = dishService.list(lambdaQueryWrapper);
//        List<DishDTO> dtoList = null;
////           不要for循环，因为在我们新建list后，若声明为空，则无法使用add().必须新建对象，若新建了对象，但是重新给他分配了地址，而且该地址存储值为null，则无法使用add添加元素
//         dtoList = list.stream().map((item)->{
//            DishDTO dishDTO = new DishDTO();
//            BeanUtils.copyProperties(item,dishDTO);
//            //获取口味DishFlavor
//            Long id = item.getId();
//            LambdaQueryWrapper<DishFlavor> dishFlavorLambdaQueryWrapper = new LambdaQueryWrapper<>();
//            dishFlavorLambdaQueryWrapper.eq(DishFlavor::getDishId,id);
//            List<DishFlavor> list1 = dishFlavorService.list(dishFlavorLambdaQueryWrapper);
//            dishDTO.setFlavors(list1);
//
//            //添加CategoryName
//
//            Category category = categoryService.getById(dish.getCategoryId());
//            dishDTO.setCategoryName(category.getName());
//
//           return dishDTO;
//       }).collect(Collectors.toList());
//        log.info("dtoList中值为={}",dtoList);
//        return R.success(dtoList);
//    }

    @GetMapping("/list")
    public R<List<DishDTO>> list(Dish dish){
        List<DishDTO> dtoList = null;

        //构造Key
        String key = "dish_" +dish.getCategoryId() + "_" + dish.getStatus();

        dtoList = (List<DishDTO>) redisTemplate.opsForValue().get(key);


        //缓存中存在
        if(!CollectionUtils.isEmpty(dtoList))
            return R.success(dtoList);

        //缓存中不存在
        LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper();
        lambdaQueryWrapper.eq(Dish::getCategoryId,dish.getCategoryId())
                .eq(Dish::getStatus,1)
                .orderByAsc(Dish::getSort)
                .orderByDesc(Dish::getUpdateTime);
        List<Dish> list = dishService.list(lambdaQueryWrapper);
        dtoList = list.stream().map((item)->{
            DishDTO dishDTO = new DishDTO();
            BeanUtils.copyProperties(item,dishDTO);
            //获取口味DishFlavor
            Long id = item.getId();
            LambdaQueryWrapper<DishFlavor> dishFlavorLambdaQueryWrapper = new LambdaQueryWrapper<>();
            dishFlavorLambdaQueryWrapper.eq(DishFlavor::getDishId,id);
            List<DishFlavor> list1 = dishFlavorService.list(dishFlavorLambdaQueryWrapper);
            dishDTO.setFlavors(list1);

            //添加CategoryName

            Category category = categoryService.getById(dish.getCategoryId());
            dishDTO.setCategoryName(category.getName());

           return dishDTO;
       }).collect(Collectors.toList());

        //添加到缓存
        redisTemplate.opsForValue().set(key,dtoList,60, TimeUnit.MINUTES);
        log.info("dtoList中值为={}",dtoList);
        return R.success(dtoList);
    }
}
