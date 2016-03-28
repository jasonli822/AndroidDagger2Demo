# AndroidDagger2Demo
Android dagger2 demo

# 前言
最新学习Dagger,在网上看了有一些文章，感觉对Dagger的理解仍有很多不清楚的地方，现将自己的一些理解记录下来，以便查阅。

# Dagger2 使用介绍
> A fast dependency injector for Android and Java.

Dagger是一款Android/Java平台的依赖注入库。

Java的依赖注入库中，最有名的应该属Google的Guice,Spring也很有名，不过是专注于J2EE开发。这两个库的功能非常强大，但它们是通过在运行时读取注解来实现依赖注入的，依赖的生成和注入需要依靠Java的反射机制，这对于性能非常敏感的Android来说是一个硬伤(因为基于反射的DI非常占用资源和耗时)。基于此，Dagger应运而生。

Dagger同样使用注解来实现依赖注入，<font color="#FF0000">但它用APT(Android Process Tool)在编译时生成辅助类，这些类继承特定的父类或实现特定的接口，程序在运行时 Dagger加载这些辅助类，调用相应接口完成依赖生成和注入</font> (关于编译时生成辅助类，这点和Butter Knife的原理很相似，可以参考之前发布的这篇文章：[Butter Knife的使用介绍与原理分析](http://jasonli822.github.io/2016/03/17/ButterKnife-working-principle-analysis/))。

Dagger 对于程序的性能影响非常小，因此更加适合用于Android的应用开发。

[Dagger1](http://square.github.io/dagger/)是[Square](https://corner.squareup.com/)公司受到Guice启发创建的。

[Dagger2](https://github.com/google/dagger)是Dagger1的分支，由Google开发和维护。Dagger2是受到[AutoValue](https://github.com/google/auto)项目的启发。刚开始，<font color="#FF0000">Dagger2解决问题的基本思想是：利用生成和写的代码混合达到看似所有的产生和提供依赖的代码都是手写的样子。</font>

# Dagger2 原理分析
结合一个简单的例子，简单分析一下Dagger2的工作原理。 [示例代码下载](https://github.com/jasonli822/AndroidDagger2Demo)

前面讲到Dagger使用了APT(Android Process Tool)，关于Android-APT的介绍在这里(http://code.neenbedankt.com/gradle-android-apt-plugin/)。

android-apt是一个Gradle插件，协助Android Studio处理annotation processors，它有两个目的：
- 允许配置只在编译时作为注解处理器的依赖，而不添加到最后的APK或library
- 设置源路径，<font color="#FF0000">使注解处理器生成的代码能被Android Studio正确使用</font>

示例代码的build.gradle文件的配置如下：
```javascript
apply plugin: 'com.neenbedankt.android-apt'

buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'com.neenbedankt.gradle.plugins:android-apt:1.8'
    }
}

android {
  ...
}

dependencies {
    apt 'com.google.dagger:dagger-compiler:2.0.2'
    compile 'com.google.dagger:dagger:2.0.2'
    compile 'javax.annotation:jsr250-api:1.0'
    
    ...
}
```
如上所示，我们添加了编译和运行库，还有必不可少的apt插件，没有这个插件，dagger可能不会正常工作。

Dagger2工作原理浅析如下：
1.@Inject注入对象
```java
public class MainActivity extends AppCompatActivity {
    
    @Inject UserModel userModel;
    
}
```
加上这个注解@Inject，表示当前类MainActivity需要(依赖)注入这样一个类UserModel的对象(注意userModel不能为private)。
前面我们说到过Dagger不是通过反射机制，而是通过预编译技术，它的代价就是缺乏反射技术的灵活性，那么它要怎么知道UserModel类对象由谁提供出来呢？

2.Dagger2中，这个负责提供依赖的组件被称为Module，我们构建ActivityModule代码如下：

```java
@Module
public class ActivityModule {
    @Provides UserModel provideUserModel() {
        return new UserModel();
    }
}
```
可以看到，使用@Module标识类型为module,并用@Provides标识提供依赖的方法。**约定@Provides函数以provide作为前缀，@Module类作为后缀** (@Provides要包含在@Module注释的类中，所以只要函数中出现了@Provides就必须要在类上面加上@Module注解)
~~加上了注解@Provides，Dagger会去识别它的返回类型，当发现它的返回类型是UserModel，上面第一步的@Inject就回来调用它，完成注入。~~


3.构建Injector。有了依赖的组件,我们还需要将依赖注入到需要的对象中。连接提供依赖和消费依赖对象的组件称为Injector。Dagger2中，我们将其称为component。ActivityComponent代码如下：
```java
@Component(modules = ActivityModule.class)
public interface ActivityComponent {
    void inject(MainActivity activity);
}
```
可以看到，Component是一个使用@Component标识的Java interface。interface的inject方法需要一个消耗依赖的类型对象作为参数。
注意：这里必须是真正消耗依赖的类型MainActivity，而不是可以写成其父类，比如Activity。因为Dagger2在编译时生成依赖注入的代码，会到inject方法的参数类型中寻找可以注入的对象，但是实际上这些对象存在于MainActivity，而不是Activity中。如果函数声明参数为Activity，Dagger2会认为没有需要注入的对象。当真正在MainActvity中创建Component实例进行注入时，会直接执行按照Activity作为参数生成inject方法，导致所有注入都失败。

4.完成依赖注入， 最后我们需要在MainActivity中构建Injector对象，完成注入，这部分代码如下：
```java
public class MainActivity extends AppCompatActivity {
    private ActivityComponent mActivityComponent;

    @Inject UserModel userModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mActivityComponent = DaggerActivityComponent.builder().activityModule(new ActivityModule()).build();
        mActivityComponent.inject(this);

        ((TextView) findViewById(R.id.user_desc_line)).setText(userModel.id + "\n" + userModel.name + "\n" + userModel.gender);
    }
}
```
首先我们使用@Inject标志了需要依赖注入的对象userModel，之后<font color="#FF0000" style='text-decoration:underline;'>通过Dagger2生成的实现了我们提供的ActivityComponent接口类DaggerActivityComponent创建component,调用其inject方法完成注入</font>。

`mActivityComponent.inject(this);` 和ButterKnife里面的`ButterKnife.bind(this);`原理类似，当我们在MainActivity里调用`mActivityComponent.inject(this);`方法时，调用的是Dagger2生成的实现了我们提供的ActivityComponent接口类DaggerActivityComponent的inject方法，代码如下：
```java
 @Override
 public void inject(MainActivity activity) {  
   mainActivityMembersInjector.injectMembers(activity);
 }
```

`mainActivityMembersInjector.injectMembers(activity);`调用的是生成的辅助类`MainActivity_MembersInjector`里面的injectMembers(MainActivity instance)方法，代码如下：
```java
@Override
public void injectMembers(MainActivity instance) {  
  if (instance == null) {
    throw new NullPointerException("Cannot inject members into a null reference");
  }
  supertypeInjector.injectMembers(instance);
  instance.userModel = userModelProvider.get();
}
```

`instance.userModel = userModelProvider.get();`这段代码就是对当前MainActivity对象里面的userModel赋值，**这样就完成了对userModel的注入**。

`userModelProvider.get()` 调用的是辅助类`ActivityModule_ProvideUserModelFactory`里面的get方法，代码如下：
```java
@Generated("dagger.internal.codegen.ComponentProcessor")
public final class ActivityModule_ProvideUserModelFactory implements Factory<UserModel> {
  private final ActivityModule module;

  public ActivityModule_ProvideUserModelFactory(ActivityModule module) {  
    assert module != null;
    this.module = module;
  }

  @Override
  public UserModel get() {  
    UserModel provided = module.provideUserModel();
    if (provided == null) {
      throw new NullPointerException("Cannot return null from a non-@Nullable @Provides method");
    }
    return provided;
  }

  public static Factory<UserModel> create(ActivityModule module) {  
    return new ActivityModule_ProvideUserModelFactory(module);
  }
}
```
这里的module就是我们定义的ActivityModule，`UserModel provided = module.provideUserModel();`调用的就是我们定义好的ActivityModule里面的provideUserModel()方法，代码如下：
```java
@Module
public class ActivityModule {
    @Provides UserModel provideUserModel() {
        return new UserModel();
    }
}
```

通过上面的分析`mActivityComponent.inject(this);`方法完成了为MainActivity里面@Inject标记的对象userModel的注入，那么mActivityComponent对象如何实例化了？
```java
public class MainActivity extends AppCompatActivity {
    private ActivityComponent mActivityComponent;

    @Inject UserModel userModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mActivityComponent = DaggerActivityComponent.builder().activityModule(new ActivityModule()).build();
        mActivityComponent.inject(this);

        ((TextView) findViewById(R.id.user_desc_line)).setText(userModel.id + "\n" + userModel.name + "\n" + userModel.gender);
    }
}
```
`ActivityComponent`是一个我们定义的接口，我们需要使用Dagger2自动生成的实现了ActivityComponent这个接口的`DaggerActivityComponent`类来实例化，我们看看`DaggerActivityComponent`类的代码：
```java
@Generated("dagger.internal.codegen.ComponentProcessor")
public final class DaggerActivityComponent implements ActivityComponent {
  private Provider<UserModel> provideUserModelProvider;
  private MembersInjector<MainActivity> mainActivityMembersInjector;

  private DaggerActivityComponent(Builder builder) {  
    assert builder != null;
    initialize(builder);
  }

  public static Builder builder() {  
    return new Builder();
  }

  public static ActivityComponent create() {  
    return builder().build();
  }

  private void initialize(final Builder builder) {  
    this.provideUserModelProvider = ActivityModule_ProvideUserModelFactory.create(builder.activityModule);
    this.mainActivityMembersInjector = MainActivity_MembersInjector.create((MembersInjector) MembersInjectors.noOp(), provideUserModelProvider);
  }

  @Override
  public void inject(MainActivity activity) {  
    mainActivityMembersInjector.injectMembers(activity);
  }

  public static final class Builder {
    private ActivityModule activityModule;
  
    private Builder() {  
    }
  
    public ActivityComponent build() {  
      if (activityModule == null) {
        this.activityModule = new ActivityModule();
      }
      return new DaggerActivityComponent(this);
    }
  
    public Builder activityModule(ActivityModule activityModule) {  
      if (activityModule == null) {
        throw new NullPointerException("activityModule");
      }
      this.activityModule = activityModule;
      return this;
    }
  }
}
```
构造函数定义为private
```java
private DaggerActivityComponent(Builder builder) {  
  assert builder != null;
  initialize(builder);
}
```
所以不能在MainActivity里面通过new DaggerActivityComponent(Builder builder)的方式示例化，通过观察代码我们发现在`DaggerActivityComponent`方法里面有一个内部类`Builder`，这个内部类`Builder`里面有一个`builder`的方法返回的是一个`ActivityComponent`的对象，内部类`Builder`代码如下：

```java
public static final class Builder {
    private ActivityModule activityModule;
  
    private Builder() {  
    }
  
    public ActivityComponent build() {  
      if (activityModule == null) {
        this.activityModule = new ActivityModule();
      }
      return new DaggerActivityComponent(this);
    }
  
    public Builder activityModule(ActivityModule activityModule) {  
      if (activityModule == null) {
        throw new NullPointerException("activityModule");
      }
      this.activityModule = activityModule;
      return this;
    }
}
```

所以我们在MainActivity里面实例化ActivityComponent的代码如下：

```java
mActivityComponent = DaggerActivityComponent.builder().activityModule(new ActivityModule()).build();
```

`DaggerActivityComponent.builder()`先生成DaggerActivityComponent的内部类`Builder`的一个对象，调用`activityModule(new ActivityModule())`方法设置内部类`Builder`实例对象的activityMoudle属性，`.build()`通过调用内部类对象的build()方法得到一个`DaggerActivityComponent`的实例。
这里的`activityModule(new ActivityModule())`也可以不调用，因为build()方法里面有一个内部类`Builder`，这个内部类`Builder`里面
```java
public ActivityComponent build() {  
    if (activityModule == null) {
	   this.activityModule = new ActivityModule();
    }
    return new DaggerActivityComponent(this);
}
```
如果activityModule为null的话会创建一个。所以，这里mActivityComponent对象实例化，下面三种方法都可以：
```java
//mActivityComponent = DaggerActivityComponent.create();
//mActivityComponent = DaggerActivityComponent.builder().build();
mActivityComponent = DaggerActivityComponent.builder().activityModule(new ActivityModule()).build();
```

至此，我们使用Dagger实现了最简单的依赖注入。通过上面的分析，我基本上对Dagger的工作原理有了初步的了解，在这个基础上，再去学习Dagger的高级运用应该会好理解一些。




参考文章：
---
1. android-apt http://www.jianshu.com/p/2494825183c5
2. 使用Dagger 2进行依赖注入 http://codethink.me/2015/08/06/dependency-injection-with-dagger-2/?utm_source=tuicool&utm_medium=referral
3. 详解Dagger2 http://www.jcodecraeer.com/a/anzhuokaifa/androidkaifa/2015/0519/2892.html
4. Dagger源码解析 http://blog.csdn.net/ljx19900116/article/details/43482051
5. Android Dagger依赖注入框架浅析 http://www.tuicool.com/articles/Nf6Njy
