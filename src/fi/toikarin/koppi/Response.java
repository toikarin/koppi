package fi.toikarin.koppi;

import android.os.Parcel;
import android.os.Parcelable;

public class Response implements Parcelable {
    private boolean ok;
    private int count;
    private String msg;

    public static final Parcelable.Creator<Response> CREATOR =
        new Creator<Response>() {
            @Override
            public Response[] newArray(int size) {
                return new Response[size];
            }

            @Override
            public Response createFromParcel(Parcel source) {
                return new Response(source);
            }
        };

    public Response(boolean ok, int count, String msg) {
        this.ok = ok;
        this.count = count;
        this.msg = msg;
    }

    private Response(Parcel src) {
        readFromParcel(src);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (ok ? 1 : 0));
        dest.writeInt(count);
        dest.writeString(msg);
    }

    private void readFromParcel(Parcel src) {
        ok = src.readByte() == 1;
        count = src.readInt();
        msg = src.readString();
    }

    public boolean isOk() {
        return ok;
    }

    public int getCount() {
        return count;
    }

    public String getMessage() {
        return msg;
    }
}
