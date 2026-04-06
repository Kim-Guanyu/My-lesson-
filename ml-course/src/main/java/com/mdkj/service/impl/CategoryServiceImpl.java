package com.mdkj.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.mdkj.dto.CategoryInsertDTO;
import com.mdkj.dto.CategoryPageDTO;
import com.mdkj.dto.CategorySimpleListVO;
import com.mdkj.dto.CategoryUpdateDTO;
import com.mdkj.exception.ServiceException;
import com.mdkj.util.ResultCode;
import com.mdkj.vo.PageVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.mdkj.entity.Category;
import com.mdkj.mapper.CategoryMapper;
import com.mdkj.service.CategoryService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static com.mdkj.entity.table.CategoryTableDef.CATEGORY;

/**
 * 课程类别表 服务层实现。
 *
 * @author Kim-Guanyu
 * @since v1.0.0
 */
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category>  implements CategoryService{
    @Override
    public boolean insert(CategoryInsertDTO dto) {
        String title = dto.getTitle();

        // 标题查重
        // select count(*) from category where title = ?
        if (QueryChain.of(mapper)
                .where(CATEGORY.TITLE.eq(title))
                .exists()) {
            throw new ServiceException(ResultCode.TITLE_REPEAT, "标题" + title + "重复");
        }

        // 组装实体类
        Category category = BeanUtil.copyProperties(dto, Category.class);
        category.setInfo(StrUtil.isEmpty(dto.getInfo()) ? "暂无描述。" : dto.getInfo());
        category.setCreated(LocalDateTime.now());
        category.setUpdated(LocalDateTime.now());

        // insert into category (title, info, idx, created, updated) values (?, ?, ?, ?, ?)
        if (mapper.insert(category) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库添加失败");
        }
        return true;
    }

    @Override
    public Category select(Long id) {
        // select * from category where id = ?
        Category category = mapper.selectOneWithRelationsById(id);
        if (ObjectUtil.isNull(category)) {
            throw new ServiceException(ResultCode.CATEGORY_NOT_FOUND, id + "号课程类别数据不存在");
        }
        return category;
    }

    @Override
    public List<CategorySimpleListVO> simpleList() {
        // select * from category order by idx asc, id desc
        return QueryChain.of(mapper)
                .orderBy(CATEGORY.IDX.asc(), CATEGORY.ID.desc())
                .withRelations()
                .listAs(CategorySimpleListVO.class);
    }

    @Override
    public PageVO<Category> page(CategoryPageDTO dto) {
        QueryChain<Category> queryChain = QueryChain.of(mapper)
                .orderBy(CATEGORY.IDX.asc(), CATEGORY.ID.desc());

        // title条件
        String title = dto.getTitle();
        if (ObjectUtil.isNotNull(title)) {
            queryChain.where(CATEGORY.TITLE.like(title));
        }

        // DB分页并转为VO
        Page<Category> result = queryChain.withRelations().page(new Page<>(dto.getPageNum(), dto.getPageSize()));
        PageVO<Category> pageVO = new PageVO<>();
        BeanUtil.copyProperties(result, pageVO);
        pageVO.setPageNum(result.getPageNumber());
        return pageVO;
    }

    @Override
    public boolean update(CategoryUpdateDTO dto) {
        String title = dto.getTitle();
        Long id = dto.getId();

        // 检查课程类别是否存在
        this.existsById(id);

        // 标题查重
        // select count(1) from category where title = ? and id <> ?
        if (QueryChain.of(mapper)
                .where(CATEGORY.TITLE.eq(title))
                .and(CATEGORY.ID.ne(id))
                .exists()) {
            throw new ServiceException(ResultCode.TITLE_REPEAT, "标题" + title + "重复");
        }

        // 组装实体类
        Category category = BeanUtil.copyProperties(dto, Category.class);
        category.setUpdated(LocalDateTime.now());

        // update category set title = ?, info = ?, idx = ?, updated = ? where id = ?
        if (!UpdateChain.of(category)
                .where(CATEGORY.ID.eq(category.getId()))
                .update()) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库修改失败");
        }
        return true;
    }

    @Override
    public boolean delete(Long id) {

        // 检查课程类别是否存在
        this.existsById(id);

        // delete from category where id = ?
        if (mapper.deleteById(id) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库删除失败");
        }
        return true;
    }

    @Override
    public boolean deleteBatch(List<Long> ids) {

        // 检查课程类别是否存在
        // select count(*) from category where id in (?, ?, ?)
        if (QueryChain.of(mapper)
                .where(CATEGORY.ID.in(ids))
                .count() < ids.size()) {
            throw new ServiceException(ResultCode.CATEGORY_NOT_FOUND, "至少一个课程类别数据不存在");
        }

        // delete from category where id in (?, ?, ?)
        if (mapper.deleteBatchByIds(ids) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库删除失败");
        }
        return true;
    }

    /**
     * 按主键检查课程类别是否存在，如果不存在则直接抛出异常
     *
     * @param id 课程类别主键
     */
    private void existsById(Long id) {
        // select count(*) from category where id = ?
        if (!QueryChain.of(mapper)
                .where(CATEGORY.ID.eq(id))
                .exists()) {
            throw new ServiceException(ResultCode.CATEGORY_NOT_FOUND, id + "号课程类别数据不存在");
        }
    }

}
