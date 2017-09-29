package com.arseeds.idcard;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 资源工具类
 */
public class AssestUtils {

    private Context mContext;
    private String path = Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator+"tessdata/";
    public AssestUtils(Context mContext) {
        super();
        this.mContext = mContext;
    }
    public void init(){

        isFolderExists(path);
        try {
                String[] list = mContext.getAssets().list("dic");
                for (int i = 0; i < list.length; i++) {
                    assetsDataToSD(path+list[i],list[i]);
                }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public boolean isFolderExists(String strFolder) {
        File file = new File(strFolder);
        if (!file.exists()) {
            if (file.mkdirs()) {
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    public void assetsDataToSD(String path,String fileName) throws IOException {
        InputStream myInput;
        OutputStream myOutput = new FileOutputStream(path);
        myInput = mContext.getAssets().open("dic/"+fileName);
        byte[] buffer = new byte[1024];
        int length = myInput.read(buffer);
        while (length > 0) {
            myOutput.write(buffer, 0, length);
            length = myInput.read(buffer);
        }
        myOutput.flush();
        myInput.close();
        myOutput.close();
    }

}
