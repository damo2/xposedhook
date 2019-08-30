package com.damo.xposedhook;

import android.os.Build;
import android.preference.PreferenceActivity;
import android.widget.TextView;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.protocol.HTTP;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;

/**
 * Created by wr
 * Date: 2019/8/29  20:50
 * mail: 1902065822@qq.com
 * describe:
 */
public class HookNew implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        //只监控淘宝
//        if (lpparam.packageName.equals("com.taobao.taobao")) {
            XposedBridge.log("start HookNew  taobao 包");
            XposedBridge.log("start HookNew  HttpURLConnection");
            final Class<?> httpUrlConnection = findClass("java.net.HttpURLConnection", lpparam.classLoader);
            hookAllConstructors(httpUrlConnection, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.args.length != 1 || param.args[0].getClass() != URL.class) {
                        return;
                    }
                    XposedBridge.log("HttpURLConnection: " + param.args[0]);
                }
            });

            XC_MethodHook ResponseHook = new XC_MethodHook() {

                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    HttpURLConnection urlConn = (HttpURLConnection) param.thisObject;
                    if (urlConn != null) {
                        StringBuilder sb = new StringBuilder();
                        int code = urlConn.getResponseCode();
                        if (code == 200) {
                            Map<String, List<String>> properties = urlConn.getHeaderFields();
                            if (properties != null && properties.size() > 0) {
                                for (Map.Entry<String, List<String>> entry : properties.entrySet()) {
                                    sb.append(entry.getKey() + ": " + entry.getValue() + ", ");
                                }
                            }
                        }
                        XposedBridge.log("RESPONSE: method=" + urlConn.getRequestMethod() + " " + "URL=" + urlConn.getURL().toString() + " " + "Params=" + sb.toString());
                    }

                }
            };

            XposedBridge.log("start HookNew  OkHttpClient");
            try {
                final Class<?> okHttpClient = findClass("com.android.okhttp.OkHttpClient", lpparam.classLoader);
                if (okHttpClient != null) {
                    findAndHookMethod(okHttpClient, "open", URI.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            URI uri = null;
                            if (param.args[0] != null) {
                                uri = (URI) param.args[0];
                            }
                            XposedBridge.log("OkHttpClient: " + uri.toString() + "");
                        }
                    });
                }
            } catch (Error e) {
            }
            try {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                    findAndHookMethod("libcore.net.http.HttpURLConnectionImpl", lpparam.classLoader, "getOutputStream", ResponseHook);
                } else {
                    findAndHookMethod("com.android.okhttp.internal.http.HttpURLConnectionImpl", lpparam.classLoader, "getOutputStream", ResponseHook);
                    findAndHookMethod("com.android.okhttp.internal.http.HttpURLConnectionImpl", lpparam.classLoader, "getInputStream", ResponseHook);
                }
            } catch (Error e) {
            }


            XposedBridge.log("start HookNew  OutputStream");
            findAndHookMethod("java.io.OutputStream", lpparam.classLoader, "write", byte[].class, int.class, int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    OutputStream os = (OutputStream) param.thisObject;
                    if (!os.toString().contains("internal.http")) {
                        return;
                    }
                    String print = new String((byte[]) param.args[0]);
                    XposedBridge.log("DATA" + print.toString());
                    Pattern pt = Pattern.compile("(\\w+=.*)");
                    Matcher match = pt.matcher(print);
                    if (match.matches()) {
                        XposedBridge.log("POST DATA: " + print.toString());
                    }
                }
            });


            findAndHookMethod("java.io.OutputStream", lpparam.classLoader, "write", byte[].class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    OutputStream os = (OutputStream) param.thisObject;
                    if (!os.toString().contains("internal.http")) {
                        return;
                    }
                    String print = new String((byte[]) param.args[0]);
                    XposedBridge.log("DATA: " + print.toString());
                    Pattern pt = Pattern.compile("(\\w+=.*)");
                    Matcher match = pt.matcher(print);
                    if (match.matches()) {
                        XposedBridge.log("POST DATA: " + print.toString());
                    }
                }
            });


            XposedBridge.log("start HookNew  apache.http");
            /* Hook org.apache.http 包中的 HttpPost 请求 */
            findAndHookMethod("org.apache.http.impl.client.AbstractHttpClient",
                    lpparam.classLoader, "execute", HttpUriRequest.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (!param.args[0].getClass().getCanonicalName().contains("HttpPost")) {
                                return;
                            }
                            HttpPost request = (HttpPost) param.args[0];
                            String url = request.getURI().toString();
                            String test1 = request.getMethod();

                            XposedBridge.log("HttpPost——getURI:" + url);
                            XposedBridge.log("HttpPost——getMethod:" + test1);

                            Header[] headers = request.getAllHeaders();
                            if (headers != null) {
                                for (int i = 0; i < headers.length; i++) {
                                    XposedBridge.log("headers:" + headers[i].getName() + ":" + headers[i].getValue());
                                }
                            }
                            HttpPost httpPost = (HttpPost) request;

                            HttpEntity entity = httpPost.getEntity();
                            String contentType = null;
                            if (entity.getContentType() != null) {
                                contentType = entity.getContentType().getValue();
                                if (URLEncodedUtils.CONTENT_TYPE.equals(contentType)) {

                                    try {
                                        byte[] data = new byte[(int) entity.getContentLength()];
                                        entity.getContent().read(data);
                                        String content = new String(data, HTTP.UTF_8);
                                        XposedBridge.log("HTTP POST Content : " + content);
                                    } catch (IllegalStateException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    } catch (IOException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    }

                                } else if (contentType.startsWith(HTTP.UTF_8)) {
                                    try {
                                        byte[] data = new byte[(int) entity.getContentLength()];
                                        entity.getContent().read(data);
                                        String content = new String(data, contentType.substring(contentType.lastIndexOf("=") + 1));
                                        XposedBridge.log("HTTP POST Content : " + content);
                                    } catch (IllegalStateException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    } catch (IOException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    }

                                }
                            } else {
                                byte[] data = new byte[(int) entity.getContentLength()];
                                try {
                                    entity.getContent().read(data);
                                    String content = new String(data, HTTP.UTF_8);
                                    XposedBridge.log("HTTP POST Content : " + content);
                                } catch (IllegalStateException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
//        }




        //test
        if (lpparam.packageName.equals("com.damo.xposedtest")) {
            XposedBridge.log("Hook start  com.damo.xposedtest.Test");
            XposedHelpers.findAndHookMethod(TextView.class, "setText", CharSequence.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    param.args[0] = "hook后内容2";
                }
                @Override
                protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    param.setResult("hook后返回内容2");
                }
            });
            XposedHelpers.findAndHookMethod("com.damo.xposedtest.Utils", lpparam.classLoader, "getWord",
                    String.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            param.args[0] = "hook后内容1";
                        }
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            super.afterHookedMethod(param);
                            param.setResult("hook后返回内容1");
                        }
                    });
        }
    }


}
