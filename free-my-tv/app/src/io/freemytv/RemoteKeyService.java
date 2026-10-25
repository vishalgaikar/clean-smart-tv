package io.freemytv;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/**
 * Sees the remote's keys before any app does, so the app buttons work from inside Netflix, Disney and the rest. The
 * system waits for it on every key (up to half a second), so it runs in a process of its own and does next to nothing.
 */
public class RemoteKeyService extends AccessibilityService {
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = getServiceInfo();
        if (info != null) {
            info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
            setServiceInfo(info);
        }
        AppLog.w("RemoteKeyService connected! flags=" + (info != null ? info.flags : 0));
        for (int k : Prefs.KEYS) {
            AppLog.w("Key " + k + " -> '" + Prefs.keyApp(k) + "'");
        }
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        int scanCode = event.getScanCode();
        AppLog.w("RemoteKeyService onKeyEvent: keyCode=" + keyCode + " scanCode=" + scanCode + " action=" + event.getAction());
        boolean consumed = RemoteKeys.onKey(this, event);
        AppLog.w("RemoteKeyService consumed=" + consumed + " for keyCode=" + keyCode + " scanCode=" + scanCode);
        return consumed;
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override public void onInterrupt() {}
}
