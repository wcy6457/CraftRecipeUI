package com.github.wcy6457.creatRecipeUI.utlis;

import java.net.MalformedURLException;
import java.net.URL;

public enum PLAYER_HEAD {


    CHINA(new Object() {
        URL evaluate() {
            try {
                return new URL("https://textures.minecraft.net/texture/7f9bc035cdc80f1ab5e1198f29f3ad3fdd2b42d9a69aeb64de990681800b98dc");
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
    }.evaluate());

    private final URL url;

    PLAYER_HEAD(URL url) {
        this.url = url;
    }

    public URL getURL() {
        return url;
    }


}
