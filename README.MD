## Lint rule for android projects for searching expensive constructors (constructors are not code-free)
[ ![Download](https://api.bintray.com/packages/grigory-rylov/android/clean-constructor-lint-rule/images/download.svg?version=1.4.5) ](https://bintray.com/grigory-rylov/android/clean-constructor-lint-rule/1.4.5/link)

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

## Usage

Add in project
```
allprojects {
  repositories {
    ...
    jcenter()
  }
}
```

Add dependency:

```
lintChecks 'com.github.grishberg:clean-constructor-lint-rule:1.4.5'
```

## License

```
    Copyright 2019 Grigory Rylov

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
```