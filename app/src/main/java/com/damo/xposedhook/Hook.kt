package com.damo.xposedhook

import android.widget.TextView
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Created by wr
 * Date: 2019/8/28  15:43
 * mail: 1902065822@qq.com
 * describe:
 */
class Hook : IXposedHookLoadPackage {

    @Throws(Throwable::class)
    override fun handleLoadPackage(loadPackageParam: XC_LoadPackage.LoadPackageParam) {

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

            //            XposedHelpers.findAndHookMethod("com.damo.xposedtest.Utils", loadPackageParam.classLoader, "getWord",
            //                    String.class, new XC_MethodHook() {
            //                        @Override
            //                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            //                            super.beforeHookedMethod(param);
            //                            param.args[0] = "hook后内容1";
            //                        }
            //
            //                        @Override
            //                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
            //                            super.afterHookedMethod(param);
            //                            param.setResult("hook后返回内容1");
            //                        }
            //                    });
        }

    }
}
