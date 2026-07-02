package com.rbac.base.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rbac.base.core.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_file")
public class SysFile extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 文件名称 (原名) */
    private String fileName;
    
    /** 文件后缀 */
    private String fileSuffix;
    
    /** 文件大小 (字节) */
    private Long fileSize;
    
    /** 文件URL */
    private String url;
    
    /** 存储引擎 (LOCAL, ALIYUN, TENCENT) */
    private String storageType;
}
