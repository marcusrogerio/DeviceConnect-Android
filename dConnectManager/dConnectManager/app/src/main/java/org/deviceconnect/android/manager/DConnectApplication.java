/*
 DConnectApplication.java
 Copyright (c) 2016 NTT DOCOMO,INC.
 Released under the MIT license
 http://opensource.org/licenses/mit-license.php
 */
package org.deviceconnect.android.manager;

import android.app.Application;

import org.deviceconnect.android.manager.keepalive.KeepAliveManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Device Connect Manager Application.
 *
 * @author NTT DOCOMO, INC.
 */
public class DConnectApplication extends Application {
    /** ドメイン名. */
    private static final String DCONNECT_DOMAIN = ".deviceconnect.org";

    /** デバイスプラグインに紐付くイベント判断用キー格納領域 */
    private final Map<String, String> mEventKeys = new ConcurrentHashMap<>();

    /** ローカルのドメイン名. */
    private static final String LOCALHOST_DCONNECT = "localhost" + DCONNECT_DOMAIN;

    /** WebSocket管理クラス. */
    private WebSocketInfoManager mWebSocketInfoManager;

    /** デバイスプラグイン管理クラス. */
    private DevicePluginManager mDevicePluginManager;

    /** KeepAlive管理クラス. */
    private KeepAliveManager mKeepAliveManager;

    @Override
    public void onCreate() {
        super.onCreate();

        mDevicePluginManager = new DevicePluginManager(this, LOCALHOST_DCONNECT);
        mDevicePluginManager.createDevicePluginList();

        mWebSocketInfoManager = new WebSocketInfoManager(this);

        mKeepAliveManager = new KeepAliveManager(this);
    }

    @Override
    public void onTerminate() {
        mWebSocketInfoManager = null;
        mDevicePluginManager = null;
        super.onTerminate();
    }

    public WebSocketInfoManager getWebSocketInfoManager() {
        return mWebSocketInfoManager;
    }

    public DevicePluginManager getDevicePluginManager() {
        return mDevicePluginManager;
    }

    public KeepAliveManager getKeepAliveManager() {
        return mKeepAliveManager;
    }

    /**
     * セッションキーとデバイスプラグインの紐付けを行う.
     * @param identifyKey appendPluginIdToSessionKey()加工後のセッションキー
     * @param serviceId プラグインID
     */
    public void setDevicePluginIdentifyKey(final String identifyKey, final String serviceId) {
        mEventKeys.put(identifyKey, serviceId);
    }

    /**
     * セッションキーに紐付いているデバイスプラグインIDを取得する.
     * @param identifyKey セッションキー
     * @return プラグインID、該当無しの場合はnull
     */
    public String getDevicePluginIdentifyKey(final String identifyKey) {
        if (mEventKeys.containsKey(identifyKey)) {
            return mEventKeys.get(identifyKey);
        } else {
            return null;
        }
    }

    /**
     * セッションキーに紐付いているデバイスプラグインIDを削除する.
     * @param identifyKey セッションキー
     * @return 削除成功でtrue, 該当無しの場合はfalse
     */
    public boolean removeDevicePluginIdentifyKey(final String identifyKey) {
        if (mEventKeys.containsKey(identifyKey)) {
            mEventKeys.remove(identifyKey);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Map登録されているKey取得.
     * @param sessionKey セッションキー
     * @return Map登録されているKey, 存在しない場合はnull.
     */
    public String getIdentifySessionKey(final String sessionKey) {
        String matchKey = null;
        for (Map.Entry<String, String> entry : mEventKeys.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(sessionKey)) {
                matchKey = key;
                break;
            }
        }
        return matchKey;
    }

    /**
     * Map登録されているKey取得.
     * @param plugin デバイスプラグイン
     * @return Map登録されているKey, 存在しない場合はnull.
     */
    public String getIdentifySessionKey(final DevicePlugin plugin) {
        String matchKey = null;
        for (Map.Entry<String, String> entry : mEventKeys.entrySet()) {
            String serviceId = entry.getValue();
            if (serviceId.contains(plugin.getServiceId())) {
                matchKey = entry.getKey();
                break;
            }
        }
        return matchKey;
    }

}
