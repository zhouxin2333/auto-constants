# auto-constants

Project Auto-Constants is a java library that automatically plugs into your editor and build tools, spicing up your java.   

Never write another class of constants for entity field name again, with one annotation your class has a class of field name constants automatically.

For example:   
Here is a entity `User`:

```
    public class User{
        private String name;
        private Long age;
    }
```

Then just add a annotation `@AutoConstants`


```
    @AutoConstants
    public class User{
        private String name;
        private Long age;
    }
```

And after compiler, you will get another class named `User_`

```
    public class User_{
        private final static String NAME = "name";
        private final static String AGE = "age";
    }
```

That's easy, isn't it!
