package com.edwin.owl.sql;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.sql.DataSource;

import lombok.Setter;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;

import com.edwin.owl.component.Router;
import com.edwin.owl.exception.DSException;
import com.google.common.collect.Lists;

/**
 * JDBC DataSource的封装代理，便于拦截sql请求进行一系列处理
 * 
 * @author jinming.wu
 * @date 2014-9-29
 */
public class EDataSource implements DataSource, InitializingBean, BeanFactoryAware {

    // spring ioc 容器
    @Setter
    private BeanFactory         beanFactory;

    // 写库
    @Setter
    private String              writeDS;

    // 读库（多个）
    @Setter
    private Map<String, String> readDS;

    // 写库引用
    private DataSource          _writeDS;

    private Router              router;

    private boolean             isInit = false;

    public PrintWriter getLogWriter() throws SQLException {
        return _writeDS.getLogWriter();
    }

    public void setLogWriter(PrintWriter out) throws SQLException {
        _writeDS.setLogWriter(out);
    }

    public void setLoginTimeout(int seconds) throws SQLException {
        _writeDS.setLoginTimeout(seconds);
    }

    public int getLoginTimeout() throws SQLException {
        return _writeDS.getLoginTimeout();
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    public Connection getConnection() throws SQLException {
        return new EConnection(router);
    }

    public Connection getConnection(String userName, String password) throws SQLException {

        EConnection conn = new EConnection(router);
        conn.setUserName(userName);
        conn.setPassword(password);

        return conn;
    }

    public void afterPropertiesSet() throws Exception {
        init();
    }

    private void init() throws DSException {

        if (!isInit) {
            _writeDS = (DataSource) beanFactory.getBean(writeDS);
            if (_writeDS == null) {
                throw new DSException("Can not find write data source!");
            }

            List<DataSource> readDSList = Lists.newArrayList();
            for (Entry<String, String> dsEntry : readDS.entrySet()) {
                DataSource ds = (DataSource) beanFactory.getBean(dsEntry.getKey());
                Integer rate = Integer.parseInt(dsEntry.getValue());
                for (int i = 0; i < rate; i++) {
                    readDSList.add(ds);
                }
            }

            router = new Router(_writeDS, readDSList);

            isInit = true;
        }
    }
}
