Lint rule for android projects for searching expensive constructors (constructors are not code-free)

Add next code to dependencies block in build.gradle
```
lintChecks 'com.github.grishberg:clean-constructor-lint-rule:1.4.4'
```


Good clean constructor:
```
public class Foo {
    private final ClassA a;
    
    @Inject
    public Foo(ClassA classA){
        a = classA;
    }
}

public class ClassA {
   public void someMethod() {...}
}

```

Not so bad not expensive constructor:
```
public class Foo {
    private final ClassA a;
    public Foo(){
        a = new ClassA();
    }
}

public class ClassA {
   public void someMethod() {...}
}

```



Bad expensive constructor:

```

public class Foo {
    public Foo(){
        doSomeExpensiveWork();
    }
    
    private void doSomeExpensiveWork() {...}
}

```

Read more about good constructors in article [Constructors Must Be Code-Free. Yegor Bugaenko](https://www.yegor256.com/2015/05/07/ctors-must-be-code-free.html)
