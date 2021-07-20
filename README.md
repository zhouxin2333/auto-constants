# auto-constants

**auto-constants**项目是一个为`Java Bean`自动生成属性名常量类的工具，同时支持`JPA`规范下的表名和表字段名常量

* [举个例子](#举个例子)
  * [本工程使用](#本工程使用)  
  * [第三方库中类的导入](#第三方库中类的导入)
* [如何使用](#如何使用)
  

### 举个例子
#### 本工程使用
```
public class User{
    private String name;
    private Long age;
}
```

只需要加一个注解`@AutoConstants`

```
@AutoConstants
public class User{
    private String name;
    private Long age;
}
```

编译后你将会得到一个新常量类`User_`

```
public class User_{
    public final static String name = "name";
    public final static String age = "age";
}
```

另外的，如果是一个实体类，例如
```
@Entity
@AutoConstants
public class Person{
    @Column
    private String name;
    @Column(name = "nick_name")
    private String nickName;
}
```
编译后还是会得到一个新常量类`Person_`

```
public class Person_{
    public static final String TABLE = "person";
    public final static String name = "name";
    public static final String COLUMN_name = "name";
    public final static String age = "nickName";
    public static final String COLUMN_nickName = "nick_name";
}
```
#### 第三方库中类的导入
如果遇到是第三方类库的情况，还可以使用`@AutoConstants`中的`extra`方法，将第三方类导入。
推荐单独使用一个配置类去做相应的导入

```
@AutoConstants(extra = {XXX.class, XXX1.class})
public class AutoConstantConfig {

}
```

编译成功后自动会生成对应的`XXX_.java`和`XXX1_.java`

### 如何使用
添加以下依赖即可使用
```
<dependency>
     <groupId>cool.zhouxin</groupId>
     <artifactId>auto-constants</artifactId>
     <version>0.0.2</version>
</dependency>
```

