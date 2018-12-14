package com.qiudaoyu.service;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 创建时间: 2018/12/14
 * 类描述:
 *
 * @author 秋刀鱼
 * @version 1.0
 */
public class DataParcelable implements Parcelable {
    public static final Creator<DataParcelable> CREATOR = new Creator<DataParcelable>() {
        @Override
        public DataParcelable createFromParcel(Parcel in) {
            return new DataParcelable(in);
        }

        @Override
        public DataParcelable[] newArray(int size) {
            return new DataParcelable[size];
        }
    };
    int a;
    int b;
    String z;
    String d;

    public DataParcelable() {

    }

    protected DataParcelable(Parcel in) {
        a = in.readInt();
        b = in.readInt();
        z = in.readString();
        d = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(a);
        dest.writeInt(b);
        dest.writeString(z);
        dest.writeString(d);
    }
}
