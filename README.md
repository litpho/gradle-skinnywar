gradle-skinnywar
================

A Gradle plugin to provide skinny war functionality

### Installation

Build script snippet for use in all Gradle versions:

```
buildscript {
  repositories {
    mavenCentral()
  }
  dependencies {
    classpath 'com.github.litpho.:gradle-skinnywar:0.0.1-SNAPSHOT'
  }
}

ear {
  apply plugin: 'com.github.litpho.gradle-skinnywar'
}
```

Build script snippet for new, incubating, plugin mechanism introduced in Gradle 2.1:

```
plugins {
  id 'com.github.litpho.gradle-skinnywar' version '0.0.1-SNAPSHOT'
}
```

### Usage
When you apply this plugin to an ear file, all war files will be stripped of dependencies already existing in the earlib configuration. 
