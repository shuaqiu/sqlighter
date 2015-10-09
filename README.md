# sqlighter
a SQLite helper lib

## usage

1. Add the JitPack repository to your build file

  Add it in your build.gradle at the end of repositories:

  ```
  repositories {
    // ...
    maven { url "http://yomnn.wicp.net:9080/repository/internal" }
  }
  ```
  
1. Add the dependency in the form

  ```
  dependencies {
    // ...
    compile 'com.shuaqiu.sqlighter:core:1.0'
    apt 'com.shuaqiu.sqlighter:processor:1.0'
  }
  ```
