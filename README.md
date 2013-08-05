# 介绍

在JDBC层，实现数据库表中敏感信息的加密存储、解密展示。


# 实现方式

* 查询SQL解析结果缓存，命中则无需再次解析，否则进入下一步
* 从JDBC层面解析SQL，确定是否包含敏感字段以及其在SQL中的位置，并将结果放入缓存
* 对PreparedStatement进行动态代理，拦截setString/setObject方法，根据SQL解析结果决定是否加密
* 对ResultSet进行动态代理，拦截getString/getObject方法，根据SQL解析结果决定是否解密

# 测试用例
* org.n3r.sensitive.parser.SensitiveFieldsParserTest  SQL解析测试用例
* org.n3r.sensitive.proxy.SensitiveFieldsProxyTest  动态代理测试用例