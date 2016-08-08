package com.okhttplib.progress;

import android.os.Message;

import com.okhttplib.bean.ProgressMessage;
import com.okhttplib.callback.ProgressCallback;
import com.okhttplib.handler.OkMainHandler;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;


public class ProgressRequestBody extends RequestBody {

    private final RequestBody originalRequestBody;
    private final ProgressCallback progressCallback;
    private BufferedSink bufferedSink;

    public ProgressRequestBody(RequestBody originalRequestBody, ProgressCallback progressCallback) {
        this.progressCallback = progressCallback;
        this.originalRequestBody = originalRequestBody;
    }

    @Override
    public MediaType contentType() {
        return originalRequestBody.contentType();
    }

    @Override
    public long contentLength() throws IOException {
        return originalRequestBody.contentLength();
    }

    @Override
    public void writeTo(BufferedSink originalSink) throws IOException {
        if (bufferedSink == null) {
            bufferedSink = Okio.buffer(sink(originalSink));
        }
        originalRequestBody.writeTo(bufferedSink);
        bufferedSink.flush();
    }


    private Sink sink(Sink originalSink) {
        return new ForwardingSink(originalSink) {
            long bytesWritten = 0L;
            long contentLength = 0L;
            @Override
            public void write(Buffer source, long byteCount) throws IOException {
                super.write(source, byteCount);
                if (contentLength == 0) {
                    contentLength = contentLength();
                }
                bytesWritten += byteCount;
                if(null != progressCallback){
                    progressCallback.onProgressAsync(bytesWritten,contentLength,bytesWritten == contentLength);
                    //主线程回调
                    Message msg = new ProgressMessage(OkMainHandler.PROGRESS_CALLBACK,
                            progressCallback,
                            bytesWritten,
                            contentLength,
                            bytesWritten == contentLength)
                            .build();
                    OkMainHandler.getInstance().sendMessage(msg);
                }
            }
        };
    }


}