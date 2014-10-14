owl
=====

####用途

+  多数源的情况下，根据请求上下文路由到主、从库

+  若登陆用户则全部走主库，非登陆用户则写主库、读从库，主从库比例是1:N

+  简单的模型，不支持事务操作时连接处理

####使用

+	引用

maven依赖

	<dependency>
		<groupId>com.edwin</groupId>
		<artifactId>owl</artifactId>
		<version>0.0.1-SNAPSHOT</version>
	</dependency>

+	spring配置

数据源

	<!-- Master Main datasource -->
	<bean id="master" class="com.mchange.v2.c3p0.ComboPooledDataSource" destroy-method="close"> 
		<property name="jdbcUrl" value="jdbc.url" />
		<property name="user" value="test" /> 
		<property name="password" value="test" /> 
		<property name="driverClass" value="mysql.jdbc.driverClassName" /> 
	</bean>
	
	<!-- Slave Main datasource -->
	<bean id="slave" class="com.mchange.v2.c3p0.ComboPooledDataSource" destroy-method="close"> 
		<property name="jdbcUrl" value="jdbc.url" />
		<property name="user" value="test" /> 
		<property name="password" value="test" /> 
		<property name="driverClass" value="mysql.jdbc.driverClassName" /> 
	</bean>
	
	<!-- Read/Write Splitting -->
	<bean id="dataSource" class="com.edwin.owl.sql.EDataSource">
        <property name="writeDS" value="master"/>
        <property name="readDS">
            <map>
                <entry key="slave" value="10" />
            </map>
        </property>
    </bean>

####实现原理
根据执行Context中的isAuthenticatedMethod方法来判断用户是否登陆，已登陆用户读写都访问主库，未登陆用户读从库、写主库，isAuthenticatedMethod会在用户访问应用时解析cookie设置。通过包装C3P0连接池，从连接池中获取连接时返回EConnection对象，该对象持有Router对象，EConnection对象实现jdbc接口Connection，重写createStatement方法，返回实现了Statement接口的自定义EStatement对象，EStatement对象又重写了executeQuery方法，该方法使用SQL作为参数，在调用该方法时通过判断sql中的关键词Select/Update等来区分路由主从库。。。
