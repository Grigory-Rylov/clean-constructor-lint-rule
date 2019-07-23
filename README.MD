Lint rule for android projects for searching non clean constructors (Constructors thad do some work inside instead initializing)

Add next code to dependencies block in build.gradle
```
lintChecks 'com.github.grishberg:clean-constructor-lint-rule:1.2.0'
```
