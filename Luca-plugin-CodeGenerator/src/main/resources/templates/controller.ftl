package ${packageName};

import ${entityFullClass};
import ${serviceInterfaceFullClass};
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
* ${tableRemarks!tableName} 控制器
*
* @author ${author}
* @date ${date}
*/
@RestController
@RequestMapping("/${urlPrefix}")
public class ${className} {

@Autowired
private ${serviceInterfaceClass} ${serviceField};

/**
* 分页查询
*/
@GetMapping("/page")
public Page<${entityClass}> page(
@RequestParam(defaultValue = "1") Integer pageNum,
@RequestParam(defaultValue = "10") Integer pageSize) {
return ${serviceField}.page(new Page<>(pageNum, pageSize));
}

/**
* 根据 ID 查询详情
*/
@GetMapping("/{id}")
public ${entityClass} getById(@PathVariable ${pkType} id) {
return ${serviceField}.getById(id);
}

/**
* 新增
*/
@PostMapping
public boolean save(@RequestBody ${entityClass} entity) {
return ${serviceField}.save(entity);
}

/**
* 修改
*/
@PutMapping
public boolean update(@RequestBody ${entityClass} entity) {
return ${serviceField}.updateById(entity);
}


/**
* 批量删除
*/
@DeleteMapping("/batch")
public boolean removeBatch(@RequestBody List<${pkType}> ids) {
return ${serviceField}.removeByIds(ids);
}
}
