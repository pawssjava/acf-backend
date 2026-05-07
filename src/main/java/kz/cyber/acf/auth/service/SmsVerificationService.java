package kz.cyber.acf.auth.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SmsVerificationService {

    private static final String HARDCODED_CODE = "1111";
    private final Map<String, Boolean> pending = new ConcurrentHashMap<>();

    public void sendCode(String phone) {
        pending.put(phone, true);
    }

    public boolean verifyCode(String phone, String code) {
        return pending.containsKey(phone) && HARDCODED_CODE.equals(code);
    }

    public void invalidate(String phone) {
        pending.remove(phone);
    }
}
