package me.kstep.downloader;

import java.io.IOException;

public class HttpDownloadException extends IOException {
    final public int code;
    final public int expectedCode;

    public HttpDownloadException(int code) {
	super();
	this.code = code;
        this.expectedCode = code == 200? 206: 200;
    }
    
    public HttpDownloadException(int code, int expectedCode) {
        super();
        this.code = code;
        this.expectedCode = expectedCode;
    }

    @Override
    public String getMessage() {
        return String.format("Invalid return code %d, expected %d", code, expectedCode);
    }
}
