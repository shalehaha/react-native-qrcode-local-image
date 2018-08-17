package com.remobile.qrcodeLocalImage;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
//import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.LuminanceSource;

import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;


import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Hashtable;


public class RCTQRCodeLocalImage extends ReactContextBaseJavaModule {
    public RCTQRCodeLocalImage(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "RCTQRCodeLocalImage";
    }

    @ReactMethod
    public void decode(String path, Callback callback) {
        Hashtable<DecodeHintType, String> hints = new Hashtable<DecodeHintType, String>();
        hints.put(DecodeHintType.CHARACTER_SET, "utf-8"); // 设置二维码内容的编码           
        BitmapFactory.Options options = new BitmapFactory.Options();     
        //options.inPreferredConfig = Bitmap.Config.RGB_565; 
        options.inJustDecodeBounds = true; // 先获取原大小
        options.inJustDecodeBounds = false; // 获取新的大小
        int divider = 100;
        int sampleSize =options.outHeight;       
        options.inSampleSize = 1;
        Bitmap scanBitmap = null;
        if (path.startsWith("http://")||path.startsWith("https://")) {
            scanBitmap = this.getbitmap(path);
        } else {
            scanBitmap = BitmapFactory.decodeFile(path,options);
        }

        while(divider<=800){
        sampleSize =options.outHeight / divider;           
        if(sampleSize<=0)
            sampleSize = 1;   
        options.inSampleSize = sampleSize;
        
        if (path.startsWith("http://")||path.startsWith("https://")) {
            scanBitmap = this.getbitmap(path);
        } else {            
            scanBitmap = BitmapFactory.decodeFile(path,options);
        }

        if(sampleSize == 0){
            sampleSize = 1;
        }
        if (scanBitmap == null) {
            callback.invoke("cannot load image");
            return;
        }        
        RGBLuminanceSource source = new RGBLuminanceSource(scanBitmap);        
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        QRCodeReader reader = new QRCodeReader();
        try {
            Result result = reader.decode(bitmap, hints);
            if (result == null) {
                callback.invoke("image format error");
                 return;
            } else {
                callback.invoke(null, result.toString());
                 return;
            }

        } catch (Exception e) {
            divider = divider +100;            
        }
        }
        callback.invoke("decode error");
    }

    public static Bitmap getbitmap(String imageUri) {
        Bitmap bitmap = null;
        try {
            URL myFileUrl = new URL(imageUri);
            HttpURLConnection conn = (HttpURLConnection) myFileUrl.openConnection();
            conn.setDoInput(true);
            conn.connect();
            InputStream is = conn.getInputStream();
            bitmap = BitmapFactory.decodeStream(is);
            is.close();
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            bitmap = null;
        } catch (IOException e) {
            e.printStackTrace();
            bitmap = null;
        }
        return bitmap;
    }

    class RGBLuminanceSource extends LuminanceSource {
    private final byte[] luminances;

    // Bitmap loadBitmap(String path) throws FileNotFoundException {
    // Bitmap bitmap = BitmapFactory.decodeFile(path);
    // if (bitmap == null) {
    //     throw new FileNotFoundException("Couldn't open " + path);
    // }
    // return bitmap;
    // }

    // public RGBLuminanceSource(String path) throws FileNotFoundException {
    //     if(path!=null){
    //     try{
    //     Bitmap bitmap = BitmapFactory.decodeFile(path);
    //     }catch(Exception ex){
            
    //     }
    // if (bitmap == null) {
    //     throw new FileNotFoundException("Couldn't open " + path);
    // }
    // //return bitmap;
    // this(bitmap);
    //     }
    // // this(loadBitmap(path));
    // }

    public RGBLuminanceSource(Bitmap bitmap) {
    super(bitmap.getWidth(), bitmap.getHeight());
    int width = bitmap.getWidth();
    int height = bitmap.getHeight();
    int[] pixels = new int[width * height];
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
    // In order to measure pure decoding speed, we convert the entire image
    // to a greyscale array
    // up front, which is the same as the Y channel of the
    // YUVLuminanceSource in the real app.
    luminances = new byte[width * height];
    for (int y = 0; y < height; y++) {
        int offset = y * width;
        for (int x = 0; x < width; x++) {
        int pixel = pixels[offset + x];
        int r = (pixel >> 16) & 0xff;
        int g = (pixel >> 8) & 0xff;
        int b = pixel & 0xff;
        if (r == g && g == b) {
            // Image is already greyscale, so pick any channel.
            luminances[offset + x] = (byte) r;
        } else {
            // Calculate luminance cheaply, favoring green.
            luminances[offset + x] = (byte) ((r + g + g + b) >> 2);
        }
        }
    }
    }

    @Override
    public byte[] getRow(int y, byte[] row) {
    if (y < 0 || y >= getHeight()) {
        throw new IllegalArgumentException(
            "Requested row is outside the image: " + y);
    }
    int width = getWidth();
    if (row == null || row.length < width) {
        row = new byte[width];
    }
    System.arraycopy(luminances, y * width, row, 0, width);
    return row;
    }

    // Since this class does not support cropping, the underlying byte array
    // already contains
    // exactly what the caller is asking for, so give it to them without a copy.
    @Override
    public byte[] getMatrix() {
    return luminances;
    }

    

}
}

