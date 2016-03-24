package io.github.jasonli.mydaggerdemo.modules;

import dagger.Module;
import dagger.Provides;
import io.github.jasonli.mydaggerdemo.UserModel;

/**
 * 构建依赖
 * 我们使用@Module标识类型为module,并用@Provides标识提供依赖的方法
 */
@Module
public class ActivityModule {
    @Provides UserModel provideUserModel() {
        return new UserModel();
    }
}
