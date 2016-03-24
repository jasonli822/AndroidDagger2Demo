package io.github.jasonli.mydaggerdemo.components;

import dagger.Component;
import io.github.jasonli.mydaggerdemo.MainActivity;
import io.github.jasonli.mydaggerdemo.modules.ActivityModule;

/**
 * 构建Injector
 * 有了提供依赖的组件(ActivityModule)，我们还要将依赖注入到需要的对象中。连接提供依赖和消费依赖对象的组件
 * 被称为Injector。Dagger2中。我们将其称为component。
 *
 * Component是一个使用@Component标识的Java interface。interface的inject方法需要一个消耗依赖的类型对象作为参数。
 * 注意：这里必须是真正消耗的类型MainActivity，而不可以写成其父类，比如Activity。
 */

@Component(modules = ActivityModule.class)
public interface ActivityComponent {
    void inject(MainActivity activity);
}
