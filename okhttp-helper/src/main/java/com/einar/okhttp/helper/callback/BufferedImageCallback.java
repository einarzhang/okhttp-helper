package com.einar.okhttp.helper.callback;

import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import okhttp3.Response;

public abstract class BufferedImageCallback extends ResponseCallback<BufferedImage> {

    @Override
    protected BufferedImage responseToData(Response response) throws Exception {
        return ImageIO.read(response.body().byteStream());
    }
}
