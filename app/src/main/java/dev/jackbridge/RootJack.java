package dev.jackbridge;

import android.os.IBinder;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Runs as root via app_process (see BridgeService.runRoot). It tells AudioService
 * that a wired headset is plugged in, the same call WiredAccessoryManager makes. This ROM
 * doesn't make that call, even though the kernel reports the jack (h2w=1).
 *
 * Usage: app_process / dev.jackbridge.RootJack in|both|off
 */
public class RootJack {
    static final int IN_WIRED_HEADSET = 0x80000010;
    static final int OUT_WIRED_HEADSET = 0x4;

    public static void main(String[] args) {
        String mode = args.length > 0 ? args[0] : "in";
        try {
            Class<?> sm = Class.forName("android.os.ServiceManager");
            IBinder b = (IBinder) sm.getMethod("getService", String.class).invoke(null, "audio");
            Object svc = Class.forName("android.media.IAudioService$Stub")
                    .getMethod("asInterface", IBinder.class).invoke(null, b);
            Class<?> ada = Class.forName("android.media.AudioDeviceAttributes");
            Constructor<?> ctor = ada.getConstructor(int.class, String.class, String.class);
            Method set = svc.getClass().getMethod("setWiredDeviceConnectionState", ada, int.class, String.class);

            if (mode.equals("off")) {
                set.invoke(svc, ctor.newInstance(IN_WIRED_HEADSET, "", "h2w"), 0, "jackbridge");
                set.invoke(svc, ctor.newInstance(OUT_WIRED_HEADSET, "", "h2w"), 0, "jackbridge");
            } else {
                if (mode.equals("both")) {
                    set.invoke(svc, ctor.newInstance(OUT_WIRED_HEADSET, "", "h2w"), 1, "jackbridge");
                }
                set.invoke(svc, ctor.newInstance(IN_WIRED_HEADSET, "", "h2w"), 1, "jackbridge");
            }
            System.out.println("OK " + mode);
        } catch (Throwable t) {
            Throwable c = t.getCause() != null ? t.getCause() : t;
            System.out.println("ERR " + c);
            System.exit(1);
        }
    }
}
