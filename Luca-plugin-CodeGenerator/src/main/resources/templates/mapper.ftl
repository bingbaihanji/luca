package ${packageName};

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import ${entityFullClass};

/**
* ${tableRemarks!tableName} 的数据库操作 Mapper
*
* @author ${author}
* @date ${date}
* @see ${entityClass}
*/
@Mapper
public interface ${className} extends BaseMapper<${entityClass}> {
}
