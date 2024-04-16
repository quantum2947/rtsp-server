package com.rtsp.rtspserver.client;

import lombok.extern.slf4j.Slf4j;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;

import javax.swing.*;

import static org.bytedeco.ffmpeg.global.avutil.av_log_set_level;

@Slf4j
public class StreamClient {
    public static void main(String[] args) {
        pullRtspStream("rtsp://192.168.31.209:554");
    }

    public static void pullRtspStream(String rtspAddress) {
        av_log_set_level(avutil.AV_LOG_INFO);
        try {
            FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(rtspAddress);
            if (rtspAddress.startsWith("rtsp://")) {
                grabber.setFormat("rtsp");
                grabber.setOption("allowed_media_types", "video");
                grabber.setOption("rtsp_transport", "udp");
                grabber.setOption("stimeout", "300000");
                grabber.setOption("buffer_size", "2048000");
                grabber.setOption("reorder_queue_size", "1024");
            }
            grabber.start();
            CanvasFrame canvasFrame = new CanvasFrame("Video Capture");
            canvasFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            while (true) {
                Frame frame = grabber.grabImage();
                if (frame == null) {
                    break;
                }
                canvasFrame.showImage(frame);
            }

            grabber.close();
            canvasFrame.dispose();
        } catch (FrameGrabber.Exception e) {
            log.error("start rtsp client failed {}", e.getMessage());
        }
    }
}
