package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class SetmealServiceImp implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Transactional
    @Override
    public void save(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        //先插入setmeal表
        setmealMapper.insert(setmeal);
        //获取套餐id
        Long id = setmeal.getId();
        //再插入setmeal_dish表
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        for (SetmealDish setmealDish : setmealDishes) {
            setmealDish.setSetmealId(id);
        }
        setmealDishMapper.insertBach(setmealDishes);
    }

    /**
     * 根据套餐id查询
     * @param id
     * @return
     */
    @Override
    public SetmealDTO getByCategoryId(Long id) {
        //查询套餐表
        Setmeal setmeal = setmealMapper.getById(id);
        SetmealDTO setmealDTO = new SetmealDTO();
        //将setmeal靠本岛setmealDTO中
        BeanUtils.copyProperties(setmeal, setmealDTO);
        //查询与套餐相关的菜品表
        List<SetmealDish> setmealDishes = setmealDishMapper.getDishBySetmealId(id);
        setmealDTO.setSetmealDishes(setmealDishes);
        return setmealDTO;
    }

    /**
     * 修改套餐
     * @param setmealDTO
     */
    @Override
    @Transactional
    public void update(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        //修改setmeal表
        setmealMapper.update(setmeal);
        //删除与套餐相关的菜品数据
        setmealDishMapper.deleteBySetmealId(setmealDTO.getId());
        //批量插入setmealdish表
        setmealDishMapper.insertBach(setmealDTO.getSetmealDishes());
    }

    /**
     * 根据id修改套餐状态
     * @param status
     * @param id
     */
    @Override
    public void updateStatus(int status, Long id) {
        Setmeal setmeal = Setmeal.builder()
                        .status(status)
                        .id(id)
                        .build();
       setmealMapper.update(setmeal);
    }

    /**
     * 批量删除套餐信息
     * @param ids
     */
    @Override
    public void deleteBatch(List<Long> ids) {
        //起售中不能删除
        for (Long id : ids) {
            Setmeal setmeal = setmealMapper.getById(id);
            if (setmeal.getStatus() == StatusConstant.ENABLE) {
                //当前菜品起售中，不能删除
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        //删除套餐信息
        setmealMapper.deleteByIds(ids);
        //删除对应的菜品信息
        setmealDishMapper.deleteBySetmealIds(ids);
    }

    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }

}
