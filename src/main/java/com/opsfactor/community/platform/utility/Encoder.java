package com.opsfactor.community.platform.utility;

import java.util.Base64;

public abstract class Encoder {

    public static String encode(String string) {
        return new String(Base64.getEncoder().encode(string.getBytes()));
    }

    public static String decode(String string) {
        return new String(Base64.getDecoder().decode(string.getBytes()));
    }
}
