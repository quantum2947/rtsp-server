

## rtsp-server

is a rtsp server implemented using netty and javacv and can also be a camera emulator. It can convert video files into rtsp stream and send rtsp stream to client. 


Any type of video file is supported .

#### Usage:

Start a netty rtsp server using your local video file:

```java
public class RtspServerStarter {
    public static void main(String[] args) {
        RtspServer rtspServer = new RtspServer(new File("d:/Test/test_video.mp4"), 554);
        String address = rtspServer.start();
        log.info("Rtsp Server started, address [{}]", address);
    }
}  
// eg. server address [rtsp://192.168.31.209:554]
```

Start a rtsp client to connect the created rtsp server:

```java
public class RtspClient {
    public static void main(String[] args) {
        pullRtspStream("rtsp://192.168.31.209:554");
    }
{
```

Then you can see the video preview window:

![image-20240417010953383](https://github.com/quantum2947/rtsp-server/blob/master/pic/image-20240417010953383.png)


##### Settings:

| Parameter  | Description                 | example                |
| ---------- | --------------------------- | ---------------------- |
| filePath   | Your local video file path. | d:/Test/test_video.mp4 |
| serverPort | Rtsp server port            | 554                    |
| rtpPort    | Rtp port                    | 34532                  |
| rtcpPort   | Rtcp port                   | 34533                  |
