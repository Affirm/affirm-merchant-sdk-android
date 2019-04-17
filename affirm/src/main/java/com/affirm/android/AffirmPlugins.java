package com.affirm.android;

import android.app.Application;
import android.content.Context;
import android.os.Build;

import com.affirm.android.model.AffirmAdapterFactory;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import androidx.annotation.NonNull;
import okhttp3.ConnectionSpec;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.TlsVersion;

class AffirmPlugins {

    private static final Object LOCK = new Object();
    private static AffirmPlugins mInstance;
    private final Affirm.Configuration mConfiguration;

    private AffirmHttpClient mRestClient;
    private Gson mGson;

    AffirmPlugins(@NonNull Affirm.Configuration configuration) {
        mConfiguration = configuration;
    }

    static void initialize(@NonNull Affirm.Configuration configuration) {
        AffirmPlugins.set(new AffirmPlugins(configuration));
    }

    private static void set(@NonNull AffirmPlugins plugins) {
        synchronized (LOCK) {
            if (mInstance != null) {
                throw new IllegalStateException("AffirmPlugins is already initialized");
            }
            mInstance = plugins;
        }
    }

    public static AffirmPlugins get() {
        synchronized (LOCK) {
            return mInstance;
        }
    }

    static void reset() {
        synchronized (LOCK) {
            mInstance = null;
        }
    }

    String publicKey() {
        return mConfiguration.publicKey;
    }

    String merchantName() {
        return mConfiguration.merchantName;
    }

    Affirm.Environment environment() {
        return mConfiguration.environment;
    }

    String environmentName() {
        return mConfiguration.environment.name();
    }

    String baseUrl() {
        return mConfiguration.environment.baseUrl;
    }

    String trackerBaseUrl() {
        return mConfiguration.environment.trackerBaseUrl;
    }

    synchronized Gson gson() {
        if (mGson == null) {
            mGson =
                    new GsonBuilder()
                            .registerTypeAdapterFactory(AffirmAdapterFactory.create()).create();
        }
        return mGson;
    }

    synchronized AffirmHttpClient restClient() {
        if (mRestClient == null) {
            OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
            //add it as the first interceptor
            clientBuilder.interceptors().add(0, new Interceptor() {
                @Override
                @NonNull
                public Response intercept(@NonNull Chain chain) throws IOException {
                    final Request request = chain.request()
                            .newBuilder()
                            .addHeader("Accept", "application/json")
                            .addHeader("Content-Type", "application/json")
                            .addHeader("Affirm-User-Agent", "Affirm-Android-SDK")
                            .addHeader("Affirm-User-Agent-Version", BuildConfig.VERSION_NAME)
                            .build();

                    return chain.proceed(request);
                }
            });
            clientBuilder.connectTimeout(5, TimeUnit.SECONDS);
            clientBuilder.readTimeout(30, TimeUnit.SECONDS);
            clientBuilder.followRedirects(false);

            mRestClient = AffirmHttpClient.createClient(enableTls12OnPreLollipop(clientBuilder));
        }
        return mRestClient;
    }

    /**
     * Enables TLS v1.2 when creating SSLSockets.
     * <p/>
     * For some reason, android supports TLS v1.2 from API 16, but enables it by
     * default only from API 20.
     *
     * @link https://developer.android.com/reference/javax/net/ssl/SSLSocket.html
     * @see SSLSocketFactory
     */
    private class Tls12SocketFactory extends SSLSocketFactory {
        private final String[] tlsV12Only = {"TLSv1.2"};

        final SSLSocketFactory delegate;

        Tls12SocketFactory(SSLSocketFactory base) {
            this.delegate = base;
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return delegate.getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return delegate.getSupportedCipherSuites();
        }

        @Override
        public Socket createSocket(Socket s, String host, int port, boolean autoClose)
                throws IOException {
            return patch(delegate.createSocket(s, host, port, autoClose));
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException {
            return patch(delegate.createSocket(host, port));
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort)
                throws IOException {
            return patch(delegate.createSocket(host, port, localHost, localPort));
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            return patch(delegate.createSocket(host, port));
        }

        @Override
        public Socket createSocket(InetAddress address,
                                   int port,
                                   InetAddress localAddress,
                                   int localPort) throws IOException {
            return patch(delegate.createSocket(address, port, localAddress, localPort));
        }

        private Socket patch(Socket s) {
            if (s instanceof SSLSocket) {
                ((SSLSocket) s).setEnabledProtocols(tlsV12Only);
            }
            return s;
        }
    }

    private OkHttpClient.Builder enableTls12OnPreLollipop(OkHttpClient.Builder client) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {

            Context context = null;
            try {
                context = getApplicationUsingReflection();
                ProviderInstaller.installIfNeeded(context);
            } catch (GooglePlayServicesRepairableException e) {

                // Indicates that Google Play services is out of date, disabled, etc.

                // Prompt the user to install/update/enable Google Play services.
                if (context != null) {
                    GoogleApiAvailability.getInstance()
                            .showErrorNotification(context,
                                    e.getConnectionStatusCode());
                }

                return client;

            } catch (GooglePlayServicesNotAvailableException e) {
                // Indicates a non-recoverable error; the ProviderInstaller is not able
                // to install an up-to-date Provider.
                AffirmLog.e("Could not install providers", e);
                return client;
            } catch (Exception e) {
                AffirmLog.e("Could not install providers", e);
            }

            try {
                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                        TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init((KeyStore) null);
                TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
                if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                    throw new IllegalStateException("Unexpected default trust managers:"
                            + Arrays.toString(trustManagers));
                }
                X509TrustManager trustManager = (X509TrustManager) trustManagers[0];

                SSLContext sc = SSLContext.getInstance("TLSv1.2");
                sc.init(null, null, null);
                client.sslSocketFactory(
                        new Tls12SocketFactory(sc.getSocketFactory()), trustManager);

                ConnectionSpec cs = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                        .tlsVersions(TlsVersion.TLS_1_2)
                        .build();

                List<ConnectionSpec> specs = new ArrayList<>();
                specs.add(cs);
                specs.add(ConnectionSpec.COMPATIBLE_TLS);
                specs.add(ConnectionSpec.CLEARTEXT);

                client.connectionSpecs(specs);
            } catch (Exception exc) {
                AffirmLog.e("Error while setting TLS 1.2", exc);
            }
        }

        return client;
    }

    private Application getApplicationUsingReflection() throws Exception {
        return (Application) Class.forName("android.app.ActivityThread")
                .getMethod("currentApplication").invoke(null, (Object[]) null);
    }
}
