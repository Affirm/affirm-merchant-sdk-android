package com.affirm.android;

import android.Manifest;
import android.net.Uri;
import android.webkit.PermissionRequest;
import android.webkit.WebView;

import androidx.annotation.NonNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;

@RunWith(MockitoJUnitRunner.class)
public class AffirmWebChromeClientTest {

    private static final String CAMERA = Manifest.permission.CAMERA;
    private static final String MICROPHONE = Manifest.permission.RECORD_AUDIO;
    private static final String AFFIRM_PAGE = "https://uk.affirm.com/products/checkout";

    @Mock
    AffirmWebChromeClient.Callbacks callbacks;
    @Mock
    WebView webview;

    @InjectMocks
    AffirmWebChromeClient affirmWebChromeClient;

    private int previousLogLevel;

    // Plain JUnit has no android.util.Log, and other tests may have turned logging on.
    @Before
    public void disableLogging() {
        previousLogLevel = AffirmLog.getLogLevel();
        AffirmLog.setLogLevel(Integer.MAX_VALUE);
    }

    @After
    public void restoreLogging() {
        AffirmLog.setLogLevel(previousLogLevel);
    }

    @Test
    public void onProgressChangedTest() {
        affirmWebChromeClient.onProgressChanged(webview, 80);
        Mockito.verify(callbacks, never()).chromeLoadCompleted();
    }

    @Test
    public void onProgressChangedWithOneHundredTest() {
        affirmWebChromeClient.onProgressChanged(webview, 100);
        Mockito.verify(callbacks).chromeLoadCompleted();
    }

    @Test
    public void onPermissionRequestGrantsWhenHostApproves() {
        final PermissionRequest request = mockMediaRequest(
                PermissionRequest.RESOURCE_VIDEO_CAPTURE,
                PermissionRequest.RESOURCE_AUDIO_CAPTURE);
        final AffirmWebChromeClient client =
                new AffirmWebChromeClient(new ImmediateMediaCallbacks(CAMERA, MICROPHONE));

        client.onPermissionRequest(request);

        final ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
        Mockito.verify(request).grant(captor.capture());
        Mockito.verify(request, never()).deny();
        org.junit.Assert.assertArrayEquals(new String[]{
                PermissionRequest.RESOURCE_VIDEO_CAPTURE,
                PermissionRequest.RESOURCE_AUDIO_CAPTURE
        }, captor.getValue());
    }

    @Test
    public void onPermissionRequestDeniesWhenHostRejects() {
        final PermissionRequest request = mockMediaRequest(
                PermissionRequest.RESOURCE_VIDEO_CAPTURE);
        final AffirmWebChromeClient client =
                new AffirmWebChromeClient(new ImmediateMediaCallbacks());

        client.onPermissionRequest(request);

        Mockito.verify(request).deny();
        Mockito.verify(request, never()).grant(any(String[].class));
    }

    @Test
    public void onPermissionRequestGrantsOnlyApprovedResources() {
        final PermissionRequest request = mockMediaRequest(
                PermissionRequest.RESOURCE_VIDEO_CAPTURE,
                PermissionRequest.RESOURCE_AUDIO_CAPTURE);
        final AffirmWebChromeClient client =
                new AffirmWebChromeClient(new ImmediateMediaCallbacks(CAMERA));

        client.onPermissionRequest(request);

        final ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
        Mockito.verify(request).grant(captor.capture());
        org.junit.Assert.assertArrayEquals(new String[]{
                PermissionRequest.RESOURCE_VIDEO_CAPTURE
        }, captor.getValue());
    }

    @Test
    public void onPermissionRequestDeniesNonMediaResources() {
        final PermissionRequest request = mockMediaRequest(
                PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID);

        affirmWebChromeClient.onPermissionRequest(request);

        Mockito.verify(request).deny();
        Mockito.verify(callbacks, never()).requestWebViewMediaPermissions(
                any(String[].class),
                any(AffirmWebChromeClient.MediaPermissionResultCallback.class));
    }

    @Test
    public void onPermissionRequestDeniesWhenAnotherRequestIsPending() {
        final PermissionRequest first = mockMediaRequest(
                PermissionRequest.RESOURCE_VIDEO_CAPTURE);
        final PermissionRequest second = mockMediaRequest(
                PermissionRequest.RESOURCE_AUDIO_CAPTURE);
        final HoldingMediaCallbacks holdingCallbacks = new HoldingMediaCallbacks();
        final AffirmWebChromeClient client = new AffirmWebChromeClient(holdingCallbacks);

        client.onPermissionRequest(first);
        client.onPermissionRequest(second);

        Mockito.verify(second).deny();
        Mockito.verify(first, never()).deny();
        holdingCallbacks.callback.onResult(new String[]{CAMERA});
        final ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
        Mockito.verify(first).grant(captor.capture());
        org.junit.Assert.assertArrayEquals(new String[]{
                PermissionRequest.RESOURCE_VIDEO_CAPTURE
        }, captor.getValue());
    }

    @Test
    public void onPermissionRequestCanceledDropsPendingGrant() {
        final PermissionRequest request = mockMediaRequest(
                PermissionRequest.RESOURCE_VIDEO_CAPTURE);
        final HoldingMediaCallbacks holdingCallbacks = new HoldingMediaCallbacks();
        final AffirmWebChromeClient client = new AffirmWebChromeClient(holdingCallbacks);

        client.onPermissionRequest(request);
        client.onPermissionRequestCanceled(request);
        holdingCallbacks.callback.onResult(new String[]{CAMERA, MICROPHONE});

        Mockito.verify(request, never()).grant(any(String[].class));
        Mockito.verify(request, never()).deny();
    }

    @Test
    public void onPermissionRequestDeniesNonHttpsOrigin() {
        final PermissionRequest request = mockMediaRequestFrom("http",
                PermissionRequest.RESOURCE_VIDEO_CAPTURE);

        affirmWebChromeClient.onPermissionRequest(request);

        Mockito.verify(request).deny();
        Mockito.verify(callbacks, never()).requestWebViewMediaPermissions(
                any(String[].class),
                any(AffirmWebChromeClient.MediaPermissionResultCallback.class));
    }

    @Test
    public void onPermissionRequestDeniesMissingOrigin() {
        final PermissionRequest request = Mockito.mock(PermissionRequest.class);
        Mockito.when(request.getResources()).thenReturn(
                new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});

        affirmWebChromeClient.onPermissionRequest(request);

        Mockito.verify(request).deny();
    }

    @Test
    public void onPermissionRequestDeniesOutsideAnAffirmPage() {
        final PermissionRequest request = mockMediaRequest(
                PermissionRequest.RESOURCE_VIDEO_CAPTURE);
        final ImmediateMediaCallbacks hostCallbacks = new ImmediateMediaCallbacks(CAMERA);
        hostCallbacks.page = "https://sdk.onfido.com/demo";
        final AffirmWebChromeClient client = new AffirmWebChromeClient(hostCallbacks);

        client.onPermissionRequest(request);

        Mockito.verify(request).deny();
        Mockito.verify(request, never()).grant(any(String[].class));
        org.junit.Assert.assertFalse(hostCallbacks.asked);
    }

    @Test
    public void onPermissionRequestDeniesWithoutAPage() {
        final PermissionRequest request = mockMediaRequest(
                PermissionRequest.RESOURCE_VIDEO_CAPTURE);
        final ImmediateMediaCallbacks hostCallbacks = new ImmediateMediaCallbacks(CAMERA);
        hostCallbacks.page = null;
        final AffirmWebChromeClient client = new AffirmWebChromeClient(hostCallbacks);

        client.onPermissionRequest(request);

        Mockito.verify(request).deny();
        org.junit.Assert.assertFalse(hostCallbacks.asked);
    }

    @Test
    public void defaultCallbacksDenyMediaRequests() {
        final PermissionRequest request = mockMediaRequest(
                PermissionRequest.RESOURCE_VIDEO_CAPTURE);
        final AffirmWebChromeClient client =
                new AffirmWebChromeClient(() -> { });

        client.onPermissionRequest(request);

        Mockito.verify(request).deny();
    }

    private static PermissionRequest mockMediaRequest(String... resources) {
        return mockMediaRequestFrom("https", resources);
    }

    private static PermissionRequest mockMediaRequestFrom(String scheme, String... resources) {
        final Uri origin = Mockito.mock(Uri.class);
        Mockito.when(origin.getScheme()).thenReturn(scheme);
        final PermissionRequest request = Mockito.mock(PermissionRequest.class);
        Mockito.when(request.getOrigin()).thenReturn(origin);
        Mockito.when(request.getResources()).thenReturn(resources);
        return request;
    }

    private static final class ImmediateMediaCallbacks
            implements AffirmWebChromeClient.Callbacks {
        private final String[] grantedPermissions;
        String page = AFFIRM_PAGE;
        boolean asked;

        ImmediateMediaCallbacks(String... grantedPermissions) {
            this.grantedPermissions = grantedPermissions;
        }

        @Override
        public void chromeLoadCompleted() {
        }

        @Override
        public String mainFrameUrl() {
            return page;
        }

        @Override
        public void requestWebViewMediaPermissions(
                @NonNull String[] androidPermissions,
                @NonNull AffirmWebChromeClient.MediaPermissionResultCallback resultCallback) {
            asked = true;
            resultCallback.onResult(grantedPermissions);
        }
    }

    private static final class HoldingMediaCallbacks
            implements AffirmWebChromeClient.Callbacks {
        AffirmWebChromeClient.MediaPermissionResultCallback callback;

        @Override
        public void chromeLoadCompleted() {
        }

        @Override
        public String mainFrameUrl() {
            return AFFIRM_PAGE;
        }

        @Override
        public void requestWebViewMediaPermissions(
                @NonNull String[] androidPermissions,
                @NonNull AffirmWebChromeClient.MediaPermissionResultCallback resultCallback) {
            this.callback = resultCallback;
        }
    }
}