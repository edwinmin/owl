package com.edwin.owl.component;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import javax.sql.DataSource;

/**
 * 主、从库路由器
 * 
 * @author jinming.wu
 * @date 2014-10-9
 */
public class Router {

    /** 支持单个写库 */
    private DataSource       writeDS;

    /** 从库列表 */
    private List<DataSource> readDSList;

    /** 从库数量 */
    private int              readDSSize;

    /** 获取执行context方法 */
    private Method           getContextMethod      = null;

    /** 从context获取路由依据 */
    private Method           isAuthenticatedMethod = null;

    public Router(DataSource writeDS, List<DataSource> readDSList) {

        this.writeDS = writeDS;
        this.readDSList = readDSList;
        readDSSize = readDSList.size();

        init();
    }

    /**
     * 根据执行上下文获取isAuthenticatedMethod方法
     */
    private void init() {

        try {
            Class<?> contextHolderClass = Class.forName("com.open.tool.tracker.ExecutionContextHolder");
            getContextMethod = contextHolderClass.getDeclaredMethod("getTrackerContext", new Class[] {});
            getContextMethod.setAccessible(true);

            Class<?> contextClass = Class.forName("com.open.tool.tracker.TrackerContext");
            isAuthenticatedMethod = contextClass.getDeclaredMethod("isAuthenticated", new Class[] {});
            isAuthenticatedMethod.setAccessible(true);

        } catch (Exception e) {
            return;
        }
    }

    public Statement getStatement(String sql) throws SQLException {

        Connection conn = getConnection(sql, false);
        if (conn == null) {
            throw new SQLException("connection is null.");
        }

        return conn.createStatement();
    }

    public Connection getConnection(String sql, boolean isForceWrite) throws SQLException {

        Connection conn = null;
        DataSource currentDS = null;

        if (getWriteFlag() || isForceWrite) {
            currentDS = this.writeDS;
        } else {
            DSOperation op = getOP(sql);
            if (op == DSOperation.WRITE) {
                currentDS = this.writeDS;
            } else if (op == DSOperation.READ) {
                int index = (int) (Math.random() * this.readDSSize);
                currentDS = this.readDSList.get(index);
            }
        }
        if (currentDS != null) {
            conn = currentDS.getConnection();
        }
        if (conn != null) {
            return conn;
        }
        return null;
    }

    public Boolean getWriteFlag() {

        try {
            Object context = getContextMethod.invoke(null);
            if (context != null) return (Boolean) this.isAuthenticatedMethod.invoke(context);
        } catch (Exception e) {
        }
        return false;
    }

    private DSOperation getOP(String sql) {
        if (StringUtils.startsWithIgnoreCaseAndWs(sql, "select") && !StringUtils.endsWithIgnoreCase(sql, "update")) {
            return DSOperation.READ;
        } else {
            return DSOperation.WRITE;
        }
    }

}
