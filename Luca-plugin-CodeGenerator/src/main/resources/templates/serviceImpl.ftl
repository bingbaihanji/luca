package ${packageName};

import ${entityFullClass};
import ${mapperFullClass};
import ${serviceInterfaceFullClass};
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
* ${tableRemarks!tableName} 的业务逻辑实现
*
* @author ${author}
* @date ${date}
*/
@Service
public class ${className} extends ServiceImpl<${mapperClass}, ${entityClass}>
implements ${serviceInterfaceClass} {
}
