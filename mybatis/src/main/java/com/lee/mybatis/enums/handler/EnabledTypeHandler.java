package com.lee.mybatis.enums.handler;

import com.lee.mybatis.enums.Enabled;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.apache.ibatis.type.TypeHandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Enabled类型处理器
 */
@MappedJdbcTypes(JdbcType.BIGINT) // 数据库中的数据类型
@MappedTypes(value = Enabled.class) // 转换后的数据类型
public class EnabledTypeHandler implements TypeHandler<Enabled> {
    private final Map<Integer, Enabled> enabledMap = new HashMap<>();

    public EnabledTypeHandler() {
        for (Enabled enabled: Enabled.values()) {
            enabledMap.put(enabled.getValue(), enabled);
        }
    }

    /**
     * 用于定义在Mybatis设置参数时该如何把Java类型的参数转换为对应的数据库类型
     * 用于定义设置参数时，该如何把Java类型的参数转换为对应的数据库类型
     * @param ps 当前的PreparedStatement对象
     * @param i 当前参数的位置
     * @param parameter 当前参数的Java对象
     * @param jdbcType 当前参数的数据库类型
     */
    @Override
    public void setParameter(PreparedStatement ps, int i, Enabled parameter, JdbcType jdbcType) throws SQLException {
        ps.setInt(i, parameter.getValue());
    }

    /**
     * 用于在Mybatis获取数据结果集时如何把数据库类型转换为对应的Java类型
     * 用于定义通过字段名称获取字段数据时，如何把数据库类型转换为对应的Java类型
     * @param rs 当前的结果集
     * @param columnName 当前的字段名称
     * @return 转换后的Java对象
     */
    @Override
    public Enabled getResult(ResultSet rs, String columnName) throws SQLException {
        int anInt = rs.getInt(columnName);
        return enabledMap.get(anInt);
    }

    /**
     * 用于在Mybatis通过字段位置获取字段数据时把数据库类型转换为对应的Java类型
     * 用于定义通过字段索引获取字段数据时，如何把数据库类型转换为对应的Java类型
     * @param rs 当前的结果集
     * @param columnIndex 当前字段的位置
     * @return 转换后的Java对象
     */
    @Override
    public Enabled getResult(ResultSet rs, int columnIndex) throws SQLException {
        int anInt = rs.getInt(columnIndex);
        return enabledMap.get(anInt);
    }

    /**
     * 用于Mybatis在调用存储过程后把数据库类型的数据转换为对应的Java类型
     * 用定义调用存储过程后，如何把数据库类型转换为对应的Java类型
     * @param cs 当前的CallableStatement执行后的CallableStatement
     * @param columnIndex 当前输出参数的位置
     * @return 转换后的Java对象
     */
    @Override
    public Enabled getResult(CallableStatement cs, int columnIndex) throws SQLException {
        int anInt = cs.getInt(columnIndex);
        return enabledMap.get(anInt);
    }
}
