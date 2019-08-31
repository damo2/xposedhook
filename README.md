

# xposedhook

##### xposed 是什么

xposed 可以在不修改APK的情况下影响程序运行(修改系统)的框架服务，当APP调用系统API时，首先经过xposed，基于xposed的模块可以在App调用这些api的时候干一些事情，或者修改返回的结果，这样app在运行的时候效果就会改变，这就是钩子，专业术语hook。xposed是个强大的钩子框架。



##### 使用

- 4.4.4以上版本手机（免root）  
  1、[下载VirtualXposed](https://github.com/damo2/xposedhook/raw/master/testapk/VirtualXposed_AOSP_0.17.3.apk)安装，里面自带Xposed-Installer。  

  2、安装 hook 模块和想要监控修改的app 到 VirtualXposed里面。

  ​	打开virtualXposed如下图所示：

  ​	点击底下按钮可将手机中的app复制到 VirtualXposed 环境里面。

  ​	上滑可看到安装到 VirtualXposed 里的程序。

  ​	

​		<img src="https://github.com/damo2/xposedhook/raw/master/testapk/image/img1.webp" width="180"/><img src="https://github.com/damo2/xposedhook/raw/master/testapk/image/img2.webp" width="180"/><img src="https://github.com/damo2/xposedhook/raw/master/testapk/image/img3.webp" width="180"/>



​		3、打开 Xposed-Installer 启动 hook 模块，选择模块->勾选hook->重启设备，如果不生效就手动重启手机。

<img src="https://github.com/damo2/xposedhook/raw/master/testapk/image/img4.webp" width="180"/><img src="https://github.com/damo2/xposedhook/raw/master/testapk/image/img5.webp" width="180"/><img src="https://github.com/damo2/xposedhook/raw/master/testapk/image/img6.webp" width="180"/>



- 4.4.4以下版本手机

[下载 Xposed-Installer]("https://github.com/damo2/xposedhook/blob/master/testapk/de.robv.android.xposed.installer_v33_36570c.apk ") 安装。然后执行上面第三步。







##### 编写 hook 。

[github地址]: “https://github.com/damo2/xposedhook”



一、写一个测试app，点击按钮给 tvContent 添加文本。

```kotlin
package com.damo.xposedtest

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.damo.xposedtest.Utils.getWord
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btnSetValue.setOnClickListener {
            tvContent.text = "hello word"
        }
    }
}
```

二、编写 hook 。

1. 添加 库，2种方式

   - 引用 jar 包 [XposedBridgeApi-54](https://github.com/damo2/xposedhook/raw/master/app/lib/XposedBridgeApi-54.jar)。

   - 使用 gradle 。

   ```
   provided 'de.robv.android.xposed:api:82'
   //如果需要引入文档，方便查看的话
   provided 'de.robv.android.xposed:api:82:sources'
   ```

   

2. AndroidManifest 配置

   ```
   <application
           android:allowBackup="true"
           android:icon="@mipmap/ic_launcher"
           android:label="@string/app_name"
           android:supportsRtl="true">
           <!-- 1、标识自己是否为一个Xposed模块 -->
           <meta-data
                   android:name="xposedmodule"
                   android:value="true"/>
           <!-- 2、Xposed模块的描述信息 -->
           <meta-data
                   android:name="xposeddescription"
                   android:value="hook例子"/>
           <!-- 3、支持Xposed框架的最低版本 -->
           <meta-data
                   android:name="xposedminversion"
                   android:value="54"/>
       </application>
   
   ```

   

3. 编写Hook修改添加的值

   ```java
   package com.damo.xposedhook
   
   import android.widget.TextView
   import de.robv.android.xposed.IXposedHookLoadPackage
   import de.robv.android.xposed.XC_MethodHook
   import de.robv.android.xposed.XposedBridge
   import de.robv.android.xposed.XposedHelpers
   import de.robv.android.xposed.callbacks.XC_LoadPackage
   
   class HookNew : IXposedHookLoadPackage {
       @Throws(Throwable::class)
       override fun handleLoadPackage(loadPackageParam: XC_LoadPackage.LoadPackageParam) {
       	//想要修改APP的包名
           if (loadPackageParam.packageName == "com.damo.xposedtest") {
               XposedBridge.log("Hook start  com.damo.xposedtest.Test")
               XposedHelpers.findAndHookMethod(
                   TextView::class.java,
                   "setText",
                   CharSequence::class.java,
                   object : XC_MethodHook() {
                       @Throws(Throwable::class)
                       override fun beforeHookedMethod(param: XC_MethodHook.MethodHookParam?) {
                           super.beforeHookedMethod(param)
                           param!!.args[0] = "hook后内容2"
                       }
   
                       @Throws(Throwable::class)
                       override fun afterHookedMethod(param: XC_MethodHook.MethodHookParam?) {
                           super.afterHookedMethod(param)
                           param!!.result = "hook后返回内容2"
                       }
                   })
           }
       }
   }
   
   ```

   

4. 在assets目录下创建xposed_init文件并添加HookNew文件所在完整目录

   <img src="https://github.com/damo2/xposedhook/raw/master/testapk/image/img8.webp" width="500"/>

5. 打包安装，启动模块，重启。

