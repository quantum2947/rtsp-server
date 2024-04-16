package com.rtsp.rtspserver.client;

import com.rtsp.rtspserver.server.RtspServer;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
public class RtspServerClient {
    public static void main(String[] args) {
        // start rtsp server
        RtspServer rtspServer = new RtspServer(new File("d:/Test/test_video.mp4"), 554);
        String rtspAddress = rtspServer.start();

        // start rtsp client
        StreamClient.pullRtspStream(rtspAddress);
    }
}
