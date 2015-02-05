/*
 ChromeCastApplication.java
 Copyright (c) 2014 NTT DOCOMO,INC.
 Released under the MIT license
 http://opensource.org/licenses/mit-license.php
 */
package org.deviceconnect.android.deviceplugin.chromecast.core;

import java.util.ArrayList;
import org.deviceconnect.android.deviceplugin.chromecast.BuildConfig;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.Cast.ApplicationConnectionResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

/**
 * Chromecast Application クラス.
 * <p>
 * アプリケーションIDに対応したReceiverアプリのコントロール
 * </p>
 * @author NTT DOCOMO, INC.
 */
public class ChromeCastApplication implements
    GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener {
    
    /** 出力するログのタグ名. */
    private static final String TAG = ChromeCastApplication.class.getSimpleName();
    /** 選択したデバイスの情報. */
    private CastDevice mSelectedDevice;
    /** GoogleAPIClient. */
    private GoogleApiClient mApiClient;
    /** Castのリスナー. */
    private Cast.Listener mCastListener;
    /** コンテキスト. */
    private Context mContext;
    /** アプリID. */
    private String mAppId;
    /** ChromecastAttach/Detach用のコールバック群. */
    private ArrayList<Callbacks> mCallbacks;
    /** Application接続フラグ. */
    private boolean mIsApplicationDisconnected = false;

    /**
     * ChromecastAttach/Detachイベントを通知するコールバックのインターフェース.
     * @author NTT DOCOMO, INC.
     */
    public interface Callbacks {
        /**
         * Chromecast Applicationにアタッチする.
         */
        void onAttach();

        /**
         * Chromecast Applicationにデタッチする.
         */
        void onDetach();
    }

    /**
     * コンストラクタ.
     * 
     * @param context  コンテキスト
     * @param appId    ReceiverアプリのアプリケーションID
     */
    public ChromeCastApplication(final Context context, final String appId) {
        this.mContext = context;
        this.mAppId = appId;
        this.mSelectedDevice = null;
        mCallbacks = new ArrayList<Callbacks>();
    }
    
    @Override
    public void onConnected(final Bundle connectionHint) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onConnected");
        }
        if (mApiClient == null) {
            return;
        }

        try {
            if (connectionHint != null && connectionHint.getBoolean(Cast.EXTRA_APP_NO_LONGER_RUNNING)) {
                teardown();
            } else {
                launchApplication();
            }
        } catch (Exception e) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onConnectionSuspended(final int cause) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onConnectionSuspended$cause: " + cause);
        }
    }

    @Override
    public void onConnectionFailed(final ConnectionResult result) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onConnectionFailed$result: " + result.toString());
        }
        mApiClient = null;
        connect();
    }

    /**
     * GoogleApiClientを取得する.
     * 
     * @return  GoogleApiClient
     */
    public GoogleApiClient getGoogleApiClient() {
        return mApiClient;
    }

    /**
     * コールバックを登録する.
     * 
     * @param   callbacks 追加するコールバック
     */
    public void addCallbacks(final Callbacks callbacks) {
        this.mCallbacks.add(callbacks);
    }

    /**
     * Chromecastデバイスをセットする.
     * 
     * @param selectedDevice 選択したChromecastのデバイス
     */
    public void setSelectedDevice(final CastDevice selectedDevice) {
        this.mSelectedDevice = selectedDevice;
    }
    
    /**
     * Chromecastデバイスを取得する.
     * 
     * @return CastDevice
     */
    public CastDevice getSelectedDevice() {
        return mSelectedDevice;
    }
    
    /**
     * GooglePlayServiceに接続し、Receiverアプリケーションを起動する.
     * 
     */
    public void connect() {
        
        try {
            if (mApiClient != null && mIsApplicationDisconnected) {
                mIsApplicationDisconnected = false;
                launchApplication();
            }
            
            if (mApiClient == null) {
                mIsApplicationDisconnected = false;
                
                mCastListener = new Cast.Listener() {
                    @Override
                    public void onApplicationDisconnected(final int statusCode) {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "onApplicationDisconnected$statusCode: " + statusCode);
                        }
                        mIsApplicationDisconnected = true;
                    }
                    @Override
                    public void onApplicationStatusChanged() {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "onApplicationStatusChanged");
                        }
                    }
                };
                
                Cast.CastOptions.Builder apiOptionsBuilder = 
                        Cast.CastOptions.builder(mSelectedDevice, mCastListener);
                mApiClient = new GoogleApiClient.Builder(this.mContext)
                        .addApi(Cast.API, apiOptionsBuilder.build())
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this)
                        .build();
                mApiClient.connect();
            }
            
        } catch (Exception e) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Receiverアプリケーションを終了し、GooglePlayServiceから切断し、再接続する.
     * 
     */
    public void reconnect() {
        stopApplication(true);
    }
    
    /**
     * Receiverアプリケーションを終了し、GooglePlayServiceから切断する.
     * 
     */
    public void teardown() {
        stopApplication(false);
    }
    
    /**
     * Receiverアプリケーションを起動する.
     * 
     */
    private void launchApplication() {
        if (mApiClient != null && mApiClient.isConnected()) {
            Cast.CastApi.launchApplication(mApiClient, mAppId, false)
                .setResultCallback(new ResultCallback<Cast.ApplicationConnectionResult>() {
                @Override
                public void onResult(final ApplicationConnectionResult result) {
                    Status status = result.getStatus();
                    if (status.isSuccess()) {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "launchApplication$onResult: Success");
                        }
                        for (int i = 0; i < mCallbacks.size(); i++) {
                            mCallbacks.get(i).onAttach();
                        }
                    } else {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "launchApplication$onResult: Fail");
                        }
                        teardown();
                    }
                }
            });
        }
    }
    
    /**
     * Receiverアプリケーションを停止する.
     * <p>
     * 停止後、再接続することもできる
     * </p>
     * @param isReconnect 再接続するか否か
     */
    private void stopApplication(final boolean isReconnect) {
        
        if (mApiClient != null && mApiClient.isConnected()) {
            Cast.CastApi.stopApplication(mApiClient).setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(final Status result) {
                    if (result.getStatus().isSuccess()) {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "stopApplication$onResult: Success");
                        }
                        
                        for (int i = 0; i < mCallbacks.size(); i++) {
                            mCallbacks.get(i).onDetach();
                        }
                        mApiClient.disconnect();
                        mApiClient = null;
                        
                        if (isReconnect) {
                            connect();
                        }
                    } else {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "stopApplication$onResult: Fail");
                        }
                    }
                }
            });
        }
    }
}