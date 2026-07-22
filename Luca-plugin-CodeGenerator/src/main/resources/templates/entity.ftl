package ${packageName};

<#list imports as imp>
    import ${imp};
</#list>
<#if useLombok>
    import lombok.Data;
</#if>
import com.baomidou.mybatisplus.annotation.TableName;
<#if hasPk>
    import com.baomidou.mybatisplus.annotation.TableId;
    import com.baomidou.mybatisplus.annotation.IdType;
</#if>
import com.baomidou.mybatisplus.annotation.TableField;

/**
* ${tableRemarks!tableName}
*
* @author ${author}
* @date ${date}
*/
<#if useLombok>
    @Data
</#if>
@TableName("${tableName}")
public class ${className} {
<#list columns as col>

    <#if col.remarks?has_content>
        /** ${col.remarks} */
    </#if>
    <#if col.primaryKey>
        @TableId(value = "${col.columnName}", type = IdType.AUTO)
    <#else>
        @TableField("${col.columnName}")
    </#if>
    private ${col.javaShortType} ${col.javaField};
</#list>
<#if !useLombok>

    // 无 Lombok 时需手动生成 getter/setter（此处省略，请使用 IDE 自动生成）
</#if>
}
