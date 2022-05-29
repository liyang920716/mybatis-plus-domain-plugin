# mybatis-plus-domain-plugin

## 1.SpringBoot配置

### maven

```xml

<dependency>
    <groupId>io.github.liyang920716</groupId>
    <artifactId>mybatis-plus-domain-plugin-spring-boot-starter</artifactId>
    <version>1.1</version>
</dependency>
```

### application.yml

```xml
domain:
        domain-name: http://www.baidu.com #域名
        enable: true #默认为false不开启
```

## 2.使用方法

### 实体类

```java

@Data
@TableName(value = "test")
@Domain
public class TestEntity implements Serializable {

    @TableId
    private Integer id;
    private String name;
    @DomainField
    private String url;

}
```

@Domain注解标记该类需要处理

@DomainField标记该属性需要被处理

正常情况下@Domain和@DomainField存在同一类

### 使用Mybatis-Plus通用BaseMapper

BaseMapper.select*

BaseMapper.updateById

BaseMapper.update(entity, ew)

以上情况都可以被处理

### 使用XML

```java

@Mapper
@Repository
public interface TestMapper extends BaseMapper<TestEntity> {

    TestEntity select(TestEntity testEntity);

    void update(TestEntity testEntity);

    void update(@Param("name") String name, @DomainParam @Param("name") String url);

}
```

@DomainParam适用于非实体类参数，如需要被处理则需要加入该注解

## 3.操作结果

插入：前端传入http://www.baidu.com/123.jpg，那么存储到数据库后是${baseUrl}/123.jpg

查询：存储到数据库后是${baseUrl}/123.jpg，那么展示给前端的是http://www.baidu.com/123.jpg

修改：前端传入http://www.baidu.com/123.jpg，那么存储到数据库后是${baseUrl}/123.jpg