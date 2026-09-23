package org.example.airentplatform.demos.web.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.example.airentplatform.demos.web.mapper.TokenSpuMapper;
import org.example.airentplatform.demos.web.pojo.Result;
import org.example.airentplatform.demos.web.pojo.TokenSpu;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 套餐的用户侧接口。
 *
 * <p>与 {@link admincontroller} 的 {@code /admin/readspu} 是「同一资源的两个视图」，
 * 刻意不合并成一个方法，因为两者的过滤条件本来就不同：
 * <ul>
 *   <li>这里 {@code /spu/list}：只返回已上架（status = 1）的套餐，供首页展示。
 *       已加入登录拦截器放行名单，<b>未登录也能浏览</b>。</li>
 *   <li>{@code /admin/readspu}：返回全部套餐（含下架），仅管理员可用，
 *       后台管理下架套餐时必须看得到它们。</li>
 * </ul>
 *
 * <p>注意：浏览放宽不代表交易放宽。下单接口 {@code POST /pay} 仍在登录拦截器的
 * 拦截范围内，未登录无法购买。
 */
@RestController
@RequestMapping("/spu")
public class spucontroller {

    @Autowired
    private TokenSpuMapper spuMapper;

    /**
     * 已上架套餐列表（公开，无需登录）。
     * 过滤放在服务端做，避免把下架套餐下发给前端。
     */
    @GetMapping("/list")
    public Result<List<TokenSpu>> list() {

        QueryWrapper<TokenSpu> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1);

        List<TokenSpu> spus = spuMapper.selectList(queryWrapper);
        return Result.success(spus);
    }
}
