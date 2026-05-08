package kz.cyber.acf.auth.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SmsVerificationService {

    private static final String HARDCODED_CODE = "1111";

    private final Map<String, Boolean> pending  = new ConcurrentHashMap<>();
    private final Map<String, Boolean> verified = new ConcurrentHashMap<>();

    public void sendCode(String phone) {
        pending.put(phone, true);
        verified.remove(phone); // reset if re-sending
    }

    public boolean verifyCode(String phone, String code) {
        if (pending.containsKey(phone) && HARDCODED_CODE.equals(code)) {
            pending.remove(phone);
            verified.put(phone, true);
            return true;
        }
        return false;
    }

    public boolean isVerified(String phone) {
        return verified.containsKey(phone);
    }

    public void invalidate(String phone) {
        pending.remove(phone);
        verified.remove(phone);
    }
}
