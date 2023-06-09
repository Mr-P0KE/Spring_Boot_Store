package com.poke.Comon;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

/**
 * 自定义元数据对象处理器
 */
@Component
@Slf4j
public class MyMetaObjectHandler implements MetaObjectHandler {
    /**
     * 插入操作，自动填充
     * @param metaObject
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        log.info("公共字段自动填充[insert]...");
        log.info(metaObject.toString());

        if (metaObject.hasGetter("checkoutTime"))
            metaObject.setValue("checkoutTime", LocalDateTime.now());
        if (metaObject.hasGetter("createTime"))
            metaObject.setValue("createTime", LocalDateTime.now());
        if (metaObject.hasGetter("updateTime"))
            metaObject.setValue("updateTime", LocalDateTime.now());
        if (metaObject.hasGetter("createUser"))
            metaObject.setValue("createUser", BaseContext.getCurrentId());
        if (metaObject.hasGetter("updateUser"))
            metaObject.setValue("updateUser", BaseContext.getCurrentId());
        if (metaObject.hasGetter("orderTime"))
            metaObject.setValue("orderTime", LocalDateTime.now());
    }

    /**
     * 更新操作，自动填充
     * @param metaObject
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        log.info("公共字段自动填充update...");
        log.info(metaObject.toString());

        long id = Thread.currentThread().getId();
        log.info("线程id为：{}",id);

        metaObject.setValue("updateTime",LocalDateTime.now());
        metaObject.setValue("updateUser",BaseContext.getCurrentId());
    }

}
