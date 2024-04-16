package com.rtsp.rtspserver.client;

import com.rtsp.rtspserver.server.RtspServer;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
public class RtspServerStarter {
    public static void main(String[] args) {
        RtspServer rtspServer = new RtspServer(new File("d:/Test/test_video.mp4"), 554);
        String address = rtspServer.start();
        log.info("Rtsp Server started, address [{}]", address);
    }
}
