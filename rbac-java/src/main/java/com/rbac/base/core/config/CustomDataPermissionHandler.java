package com.rbac.base.core.config;

import com.baomidou.mybatisplus.extension.plugins.handler.DataPermissionHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.springframework.stereotype.Component;

@Component
public class CustomDataPermissionHandler implements DataPermissionHandler {

    @Override
    public Expression getSqlSegment(Expression where, String mappedStatementId) {
        String sqlFilter = DataScopeContextHolder.getSqlFilter();
        if (sqlFilter == null || sqlFilter.isEmpty()) {
            return where; // 如果线程中没有数据权限SQL，直接返回原始where条件
        }

        try {
            // DataScopeAspect 只负责生成 SQL 片段，真正拼接到 MyBatis-Plus 查询发生在这里。
            // 将字符串形式的过滤条件转换为 JSqlParser 的 Expression
            Expression dataScopeExpression = CCJSqlParserUtil.parseCondExpression("(" + sqlFilter + ")");
            
            // 如果原本有 where 条件，则用 AND 拼接
            if (where != null) {
                return CCJSqlParserUtil.parseCondExpression("(" + where.toString() + ") AND (" + dataScopeExpression.toString() + ")");
            } else {
                return dataScopeExpression;
            }
        } catch (Exception e) {
            throw new RuntimeException("动态拼接数据权限SQL失败", e);
        }
    }
}
