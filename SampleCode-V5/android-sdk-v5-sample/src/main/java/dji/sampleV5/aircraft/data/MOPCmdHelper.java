package dji.sampleV5.aircraft.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.Arrays;

import dji.v5.manager.mop.DataResult;
import dji.v5.manager.mop.Pipeline;
import dji.v5.utils.common.LogUtils;


/**
 * 利用MOP通道，实现自定的文件上传下载协议
 */
public class MOPCmdHelper {
    private static final byte CMD_REQ = 0x50;
    private static final byte CMD_ACK = 0x51;
    private static final byte CMD_TRANS_ACK = 0x52;
    private static final byte CMD_FILE_INFO = 0x60;
    private static final byte CMD_DOWNLOAD = 0x61;
    private static final byte CMD_FILE_DATA = 0x62;
    private static final byte CMD_FILE_TRANS_FAIL = 0x63;
    private static final byte CMD_FILE_TRANS_FAIL_ACK = 0x64;
    private static final byte CMD_0 = 0x00;
    private static final byte CMD_1 = 0x01;
    public static final int PACK_HEADER_SIZE = 8;
    public static final int PACK_FILE_INFO_SIZE = 53;
    public static final String UPLOAD_FILE = "uploadFile: ";

    private static final int FILE_NAME_LENGTH = 32;

    private static final String TAG = MOPCmdHelper.class.getSimpleName();

    public static byte[] getUploadFileHeader() {
        byte[] cmd = new byte[PACK_HEADER_SIZE];
        cmd[0] = CMD_REQ;
        cmd[1] = CMD_0;
        return cmd;
    }

    public static byte[] getDownloadFileHeader() {
        byte[] cmd = new byte[PACK_HEADER_SIZE];
        cmd[0] = CMD_REQ;
        cmd[1] = CMD_1;
        return cmd;
    }

    public static byte[] getDownloadFile() {
        byte[] cmd = new byte[PACK_HEADER_SIZE];
        cmd[0] = CMD_DOWNLOAD;
        cmd[1] = (byte) 0xFF;
        cmd[4] = (byte) 0x20;
        return cmd;
    }

    public static byte[] getFileDataHeader(int size, int flag, int seq) {
        byte[] cmd = new byte[PACK_HEADER_SIZE];
        cmd[0] = CMD_FILE_DATA;
        cmd[1] = (byte) flag;
        cmd[2] = (byte) seq;
        cmd[4] = (byte) (size & 0xff);
        cmd[5] = (byte) (size >> 8 & 0xff);
        cmd[6] = (byte) (size >> 16 & 0xff);
        cmd[7] = (byte) (size >> 24 & 0xff);
        return cmd;
    }

    public static boolean sendDownloadCmd(Pipeline p) {
        byte[] cmd = getDownloadFileHeader();
        DataResult result = p.writeData(cmd);
        if (result.getLength() > 0) {
            return parseCommonAck(p);
        }
        return false;
    }

    public static FileInfo sendDownloadFileReq(Pipeline p, String filename, PipelineAdapter.OnEventListener listener) {
        if (!sendDownloadCmd(p)) {
            return null;
        }
        byte[] header = getDownloadFile();
        byte[] chars = filename.getBytes();

        byte[] req = new byte[header.length + FILE_NAME_LENGTH];
        System.arraycopy(header, 0, req, 0, header.length);
        System.arraycopy(chars, 0, req, header.length, chars.length);

        if (chars.length < FILE_NAME_LENGTH) {
            req[header.length + chars.length] = '\0';
        }
        // 发送请求,要下载的文件
        DataResult result = p.writeData(req);
        if (result.getLength() > 0) {

            LogUtils.i(TAG, "sendDownloadFileReq ack: Success", "/MOP");
            // 读取文件信息
            byte[] fileInfoBuff = new byte[MOPCmdHelper.PACK_HEADER_SIZE + MOPCmdHelper.PACK_FILE_INFO_SIZE];
            int sum = 0;
            while (sum < fileInfoBuff.length) {
                // 能获取MD5等信息
                int len = p.readData(fileInfoBuff).getLength();

                if (len > 0) {
                    // 由于read不支持offset，这里处理fileInfoBuff的拼接，应该使用临时的byte[]来copyArray
                    sum += len;
                    LogUtils.e(TAG, "sendDownloadFileReq download:" + sum, "/MOP");
                } else {
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        LogUtils.e(TAG, e.getMessage());
                    }
                }
            }

            return FileInfo.parse(fileInfoBuff);

        } else {
            postResultTipEvent(TipEvent.DOWNLOAD, "request failure", null, listener);
        }

        return null;
    }

    public static int sendTransFileFailReq(Pipeline pipeline, long length) {
        byte[] buff = new byte[12];
        buff[0] = CMD_FILE_TRANS_FAIL;
        buff[1] = (byte) 0xFF;
        buff[7] = 0x04;
        buff[8] = (byte) (length >> 0 & 0xff);
        buff[9] = (byte) (length >> 8 & 0xff);
        buff[10] = (byte) (length >> 16 & 0xff);
        buff[11] = (byte) (length >> 24 & 0xff);

        int len = getInt(buff, PACK_HEADER_SIZE, 4);
        int result = pipeline.writeData(buff).getLength();
        LogUtils.i(TAG, "sendTransFileFailReq:" + result, "/MOP");
        LogUtils.i(TAG, "len:" + len, "/MOP");
        return result;
    }

    public static int sendTransFileFailAck(Pipeline pipeline, long length) {
        byte[] buff = new byte[12];
        buff[0] = CMD_FILE_TRANS_FAIL_ACK;
        buff[1] = (byte) 0xFF;
        buff[7] = 0x04;
        buff[8] = (byte) (length >> 0 & 0xff);
        buff[9] = (byte) (length >> 8 & 0xff);
        buff[10] = (byte) (length >> 16 & 0xff);
        buff[11] = (byte) (length >> 24 & 0xff);
        int result = pipeline.writeData(buff).getLength();
        LogUtils.i(TAG, "sendTransFileFailAck:" + result, "/MOP");
        return result;
    }

    public static int parseTransFileFailAck(Pipeline p) {
        byte[] buff = new byte[12];
        int size;
        // 读取回包
        while (((size = p.readData(buff).getLength()) < 0)) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LogUtils.e(TAG, e.getMessage());
            }
            LogUtils.i(TAG, "parseTransFileFailAck wait ack: " + size, "/MOP");
        }
        return getInt(buff, PACK_HEADER_SIZE, 4);
    }


    public static boolean parseCommonAck(Pipeline p) {
        // 读取文件下载的信息
        byte[] buff = new byte[PACK_HEADER_SIZE];
        int size;
        // 读取回包
        while ((size = p.readData(buff).getLength()) < 0) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                LogUtils.e(TAG, e.getMessage());
                Thread.currentThread().interrupt();
            }
            LogUtils.e(TAG, "sendDownloadFileReq wait ack: " + size, "/MOP");
        }
        return (buff[0] == CMD_ACK) && (buff[1] == CMD_0);
    }

    public static boolean parseUploadAck(Pipeline p) {
        // 读取文件下载的信息
        byte[] buff = new byte[PACK_HEADER_SIZE];
        int size;
        // 读取回包
        while ((size = p.readData(buff).getLength()) < 0) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                LogUtils.e(TAG, e.getMessage());
                Thread.currentThread().interrupt();
            }
            LogUtils.i(TAG, "upload ack: " + size, "/MOP");
        }
        return (buff[0] == CMD_TRANS_ACK) && (buff[1] == CMD_0);
    }

    public static FileTransResult parseFileDataCmd(byte[] buff) {
        if (buff[0] == CMD_FILE_DATA) {
            int length = getInt(buff, 4, 4);
            return new FileTransResult(true, length);
        }
        if (buff[0] == CMD_FILE_TRANS_FAIL) {
            int length = getInt(buff, 4, 4);
            return new FileTransResult(false, length);
        }
        LogUtils.e(TAG, "parseFileDataCmd error", "/MOP");
        return null;
    }

    public static int sendTransAck(Pipeline p, boolean result) {
        byte[] buff = new byte[PACK_HEADER_SIZE];
        buff[0] = CMD_TRANS_ACK;
        buff[1] = result ? CMD_0 : CMD_1;
        return p.writeData(buff).getLength();
    }

    public static int sendAck(Pipeline p, byte cmd) {
        byte[] buff = new byte[PACK_HEADER_SIZE];
        buff[0] = CMD_ACK;
        buff[1] = cmd;
        return p.writeData(buff).getLength();
    }

    public static boolean isFileEnd(byte[] buff) {
        return buff[0] == CMD_FILE_DATA && buff[1] == CMD_1;
    }

    public static int sendUploadFileReq(Pipeline data, String filename, byte[] buff, long time, byte[] md5,
                                        PipelineAdapter.OnEventListener listener) {
        FileInfo fileInfo = new FileInfo();
        fileInfo.filename = filename;
        fileInfo.fileLength = buff.length;
        fileInfo.md5 = md5;
        LogUtils.i(TAG, "sendUploadFileReq fileInfo:" + fileInfo, "/MOP");
        listener.onFileInfoEvent(fileInfo);

        DataResult dataResult = data.writeData(getUploadFileHeader());
        int result = dataResult.getLength();
        if (result < 0) {
            postResultTipEvent(TipEvent.UPLOAD, "Upload Failure: " + dataResult.toString(), null, listener);
            return result;
        }
        if (parseCommonAck(data)) {
            // 发送md5等
            byte[] fileHeader = fileInfo.getHeader();
            DataResult writeData = data.writeData(fileHeader);
            result = writeData.getLength();
            LogUtils.i(TAG, "sendUploadFileReq send md5:" + writeData.toString(), "/MOP");
            if (result > 0) {
                if (parseCommonAck(data)) {
                    // 上传文件
                    uploadFile(data, buff, time, listener);
                    // 上传完的ack
                    if (parseUploadAck(data)) {
                        postResultTipEvent(TipEvent.UPLOAD, "Upload Success", null, listener);
                    }
                } else {
                    postResultTipEvent(TipEvent.UPLOAD, "Upload Failure", null, listener);
                }
            }
        }
        return result;
    }

    private static void postResultTipEvent(int type, String result, String progress, PipelineAdapter.OnEventListener listener) {
        TipEvent event = new TipEvent(type);
        event.result = result;
        event.progress = progress;
        listener.onTipEvent(event);
    }

    private static int uploadFile(Pipeline pipeline, byte[] buff, long time, PipelineAdapter.OnEventListener listener) {
        int size = 3072;
        byte[] header;
        int hadWrote = 0;
        int seq = 0;
        if (buff.length <= size) {
            header = getFileDataHeader(buff.length, CMD_1, seq);
            int result = writeData(pipeline, buff, time, header, hadWrote, buff.length, listener);
            LogUtils.i(TAG, UPLOAD_FILE + result, "/MOP", ",seq:", seq);
            return result;
        } else {
            while (buff.length - hadWrote > size) {
                header = getFileDataHeader(size, CMD_0, seq);
                int result = writeData(pipeline, buff, time, header, hadWrote, size, listener);

                hadWrote += size;
                LogUtils.i(TAG, UPLOAD_FILE + hadWrote + " result:" + result, ",seq:", seq);
                seq++;
            }
            int length = buff.length - hadWrote;
            header = getFileDataHeader(length, CMD_1, seq);
            int result = writeData(pipeline, buff, time, header, hadWrote, length, listener);
            hadWrote += result;
            LogUtils.i(TAG, UPLOAD_FILE + length, "/MOP");
        }

        String progress = String.format("uploadSize:%d, useTime:%d(ms)", hadWrote, System.currentTimeMillis() - time);
        postResultTipEvent(TipEvent.UPLOAD, null, progress, listener);
        return 0;
    }

    private static int writeData(Pipeline pipeline, byte[] buff, long time, byte[] header, int hadWrote, int length,
                                 PipelineAdapter.OnEventListener listener) {
        byte[] d = new byte[length + header.length];
        System.arraycopy(header, 0, d, 0, header.length);
        System.arraycopy(buff, hadWrote, d, header.length, length);
        DataResult dataResult = pipeline.writeData(d);
        int result = dataResult.getLength();

        String progress = String.format("uploadSize:%d, useTime:%d(ms)", hadWrote, System.currentTimeMillis() - time);
        postResultTipEvent(TipEvent.UPLOAD, null, progress, listener);

        if (result < 0) {
            LogUtils.e(TAG, "writeData miss: " + dataResult.toString());
            // 发送上传出错的req
            sendTransFileFailReq(pipeline, hadWrote);
            // 读取对端返回的已读长度
            int len = parseTransFileFailAck(pipeline);
            if (len == hadWrote) {
                sendAck(pipeline, CMD_0);
                return writeData(pipeline, buff, time, header, hadWrote, length, listener);
            } else {
                LogUtils.e(TAG, "writeData error: " + len);
                sendAck(pipeline, CMD_1);
            }
        }
        return result;
    }

    public static int parseFileFailIndex(Pipeline pipeline) {
        byte[] buff = new byte[4];
        pipeline.readData(buff);
        return getInt(buff, 0, 4);
    }


    public static byte[] getMD5(File file) {
        byte[] buffer = new byte[8192];
        byte[] desc = new byte[16];
        try (InputStream ins = new FileInputStream(file)) {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            int len;
            while ((len = ins.read(buffer)) != -1) {
                md5.update(buffer, 0, len);
            }
            byte[] source = md5.digest();
            System.arraycopy(source, 0, desc, 0, desc.length);
        } catch (Exception e) {
            LogUtils.e(TAG, e.getMessage());
        }
        return desc;
    }

    public static int getInt(byte[] bytes, final int offset, int length) {
        if (null == bytes) {
            return 0;
        }
        final int bytesLen = bytes.length;
        if (bytesLen == 0 || offset < 0 || bytesLen <= offset) {
            return 0;
        }
        if (length > bytesLen - offset) {
            length = bytesLen - offset;
        }

        int value = 0;
        for (int i = length + offset - 1; i >= offset; i--) {
            value = (value << 8 | (bytes[i] & 0xff));
        }
        return value;
    }

    public static class FileInfo {
        private boolean isExist;
        private int fileLength;
        private String filename;
        private byte[] md5;

        public static FileInfo parse(byte[] data) {
            if (data[0] != CMD_FILE_INFO) {
                return null;
            }

            StringBuilder sb = new StringBuilder("mop_cmd_file:");
            for (int i = 0; i < data.length; i++) {
                sb.append(data[i] + ",");
            }

            FileInfo info = new FileInfo();
            info.isExist = data[8] == 1;
            info.fileLength = getInt(data, 9, 4);
            byte[] d = new byte[32];
            System.arraycopy(data, 13, d, 0, 32);
            info.filename = getString(d);
            byte[] md5 = new byte[16];
            System.arraycopy(data, 45, md5, 0, 16);
            info.md5 = md5;
            return info;
        }

        public byte[] getHeader() {
            byte[] buff = new byte[61];
            buff[0] = CMD_FILE_INFO;
            buff[1] = (byte) 0xFF;
            buff[4] = 0x35;
            buff[8] = 0;
            buff[9] = (byte) (fileLength & 0xff);
            buff[10] = (byte) (fileLength >> 8 & 0xff);
            buff[11] = (byte) (fileLength >> 16 & 0xff);
            buff[12] = (byte) (fileLength >> 24 & 0xff);

            byte[] chars = filename.getBytes();
            System.arraycopy(chars, 0, buff, 13, chars.length);
            System.arraycopy(md5, 0, buff, 45, 16);
            if (chars.length < 32) {
                buff[13 + chars.length] = '\0';
            }
            return buff;
        }

        public static String getString(byte[] bytes) {
            if (null == bytes) {
                return "";
            }
            // 去除NULL字符
            byte zero = 0x00;
            byte no = (byte) 0xFF;
            for (int i = 0; i < bytes.length; i++) {
                if (bytes[i] == zero || bytes[i] == no) {
                    bytes = readBytes(bytes, 0, i);
                    break;
                }
            }
            return new String(bytes, Charset.forName("GBK"));
        }

        public static byte[] readBytes(byte[] source, int from, int length) {
            byte[] result = new byte[length];
            System.arraycopy(source, from, result, 0, length);
            /**
             for (int i = 0; i < length; i++) {
             result[i] = source[from + i];
             }
             */
            return result;
        }

        @Override
        public String toString() {
            return "FileInfo{" +
                    "isExist=" + isExist +
                    ", fileLength=" + fileLength +
                    ", filename='" + filename + '\'' +
                    ", md5=" + Arrays.toString(md5) +
                    '}';
        }

        public boolean isExist() {
            return isExist;
        }

        public int getFileLength() {
            return fileLength;
        }

        public String getFilename() {
            return filename;
        }

        public byte[] getMd5() {
            return md5;
        }
    }

    public interface ProcessCallback {
        void callback(int length);
    }

    public static class FileTransResult {
        // true代表读了多少数据(length)，false重新读写，并根据之后length个字节锁代表的int值去移动游标到指定位置
        private boolean success;
        private int length;

        public FileTransResult(boolean success, int length) {
            this.success = success;
            this.length = length;
        }

        public boolean isSuccess() {
            return success;
        }

        public int getLength() {
            return length;
        }
    }

    public static class TipEvent {
        public static final int UPLOAD = 0;
        public static final int DOWNLOAD = 1;

        public TipEvent(int type) {
            this.type = type;
        }

        private int type;
        private String state;
        private String result;
        private String progress;


        public int getType() {
            return type;
        }

        public String getState() {
            return state;
        }

        public String getResult() {
            return result;
        }

        public String getProgress() {
            return progress;
        }
    }
}
