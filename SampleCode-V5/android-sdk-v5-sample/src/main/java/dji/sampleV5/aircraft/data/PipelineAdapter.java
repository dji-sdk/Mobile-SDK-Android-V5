package dji.sampleV5.aircraft.data;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import dji.sampleV5.aircraft.R;
import dji.sdk.keyvalue.value.common.ComponentIndexType;
import dji.v5.common.error.DJIPipeLineError;
import dji.v5.manager.mop.DataResult;
import dji.v5.manager.mop.Pipeline;
import dji.v5.manager.mop.PipelineManager;
import dji.v5.utils.common.BytesUtil;
import dji.v5.utils.common.DiskUtil;
import dji.v5.utils.common.LogUtils;

public class PipelineAdapter extends RecyclerView.Adapter<PipelineAdapter.ViewHolder> {
    private final List<Pair<ComponentIndexType, Pipeline>> data;
    private final LayoutInflater mInflater;
    private ViewHolder curHolder;

    private final OnDisconnectListener listener = new OnDisconnectListener() {
        @Override
        public void onDisconnect(Pipeline d) {
            if (data.contains(d)) {
                data.remove(d);
                notifyItemRemoved(data.indexOf(d));
            }

        }
    };

    public PipelineAdapter(Context context, List<Pair<ComponentIndexType, Pipeline>> data) {
        this.mInflater = LayoutInflater.from(context);
        this.data = data;
    }

    public void addItem(ComponentIndexType indexType, Pipeline action) {
        if (action == null || data == null || data.contains(action)) {
            return;
        }
        data.add(new Pair<>(indexType, action));
        notifyItemInserted(getItemCount() - 1);
    }

    public List<Pair<ComponentIndexType, Pipeline>> getData() {
        return data;
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        curHolder = new ViewHolder(mInflater.inflate(R.layout.adapter_pipeline_item, parent, false));
        return curHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.setData(data.get(position));
        holder.setListener(listener);
    }

    @Override
    public int getItemCount() {
        return data == null ? 0 : data.size();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        for (int i = 0; i < getItemCount(); i++) {
            ViewHolder viewholder = (ViewHolder) recyclerView.findViewHolderForAdapterPosition(i);
            if (viewholder != null) {
                viewholder.destroy();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.destroy();
    }

    public static String getTime() {
        String patten = "yyyy-MM-dd HH:mm:ss.SSS";
        SimpleDateFormat format = new SimpleDateFormat(patten);
        return format.format(new Date());
    }

    //    @Override
    public void onDisconnect(Pipeline data) {
        this.data.remove(data);
        notifyItemRemoved(this.data.indexOf(data));
    }

    public void onReset(Pipeline pipeline) {
        this.data.remove(pipeline);
        curHolder.destroy();
        notifyItemRemoved(this.data.indexOf(pipeline));
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private String tag = LogUtils.getTag(this);
        private final TextView nameTv;
        private final TextView downloadTv;
        private final TextView uploadTv;
        private final TextView downloadLogTv;
        private final TextView uploadLogTv;
        private final TextView disconnectTv;
        private final TextView filenameTv;
        private final Switch autoDownloadSwitch;

        HandlerThread uploadThread;
        HandlerThread downloadThread;
        Handler uploadHandler;
        Handler downloadHandler;
        private WeakReference<OnDisconnectListener> listenerWeakReference;

        private boolean uploading;
        private boolean downloading;

        private String uploadFileInfoLog;
        private String uploadProgress;
        private String uploadResult;

        private String downloadFileInfoLog;
        private String downloadProgress;
        private String downloadResult;
        private int downloadPackCount;
        private int downloadSize;

        private int downloadSuccessCount;
        private int downloadCount;

        private int uploadSuccessCount;
        private int uploadCount;

        private String uploadFileName = "mopSample.log";
        private final OnEventListener listener = new OnEventListener() {
            @Override
            public void onTipEvent(MOPCmdHelper.TipEvent event) {
                handlerTipEvent(event);
            }

            @Override
            public void onFileInfoEvent(MOPCmdHelper.FileInfo event) {
                onEvent3BackgroundThread(event);
            }
        };

        public ViewHolder(View itemView) {
            super(itemView);
            nameTv = itemView.findViewById(R.id.tv_name);
            downloadTv = itemView.findViewById(R.id.tv_download);
            uploadTv = itemView.findViewById(R.id.tv_upload);
            downloadLogTv = itemView.findViewById(R.id.tv_download_log);
            uploadLogTv = itemView.findViewById(R.id.tv_upload_log);
            disconnectTv = itemView.findViewById(R.id.tv_disconnect);
            filenameTv = itemView.findViewById(R.id.et_file_name);
            autoDownloadSwitch = itemView.findViewById(R.id.switch_auto_download);
        }

        public void setData(Pair<ComponentIndexType, Pipeline> pair) {
            destroy();
            Pipeline pipeline = pair.second;
            uploadThread = new HandlerThread("upload");
            downloadThread = new HandlerThread("download");
            uploadThread.start();
            downloadThread.start();
            uploadHandler = new Handler(uploadThread.getLooper());
            downloadHandler = new Handler(downloadThread.getLooper());
            View.OnClickListener localListener = v -> {
                int id = v.getId();
                if (id == R.id.tv_upload) {
                    showDialog(itemView.getContext(), pipeline);
                } else if (id == R.id.tv_download) {
                    downloadHandler.post(() -> {
                        resetDownInfo();
                        downloadFile(pipeline, filenameTv.getText().toString());
                    });
                } else if (id == R.id.tv_disconnect) {
                    destroy();
                    disconnect(pair.first, pipeline);
                }
            };

            String title = String.format("Id=%d, MOPType = %s, trans_type=%s", pipeline.getId(), pipeline.getPipelineDeviceType(),
                    pipeline.getTransmissionControlType());
            nameTv.setText(title);
            downloadTv.setOnClickListener(localListener);
            uploadTv.setOnClickListener(localListener);
            disconnectTv.setOnClickListener(localListener);
        }

        public void setListener(OnDisconnectListener listener) {
            listenerWeakReference = new WeakReference<>(listener);
        }

        private void disconnect(ComponentIndexType indexType, Pipeline pipeline) {
            PipelineManager.getInstance().disconnectPipeline(indexType, pipeline.getId(), pipeline.getPipelineDeviceType(),
                    pipeline.getTransmissionControlType());
            if (listenerWeakReference != null && listenerWeakReference.get() != null) {
                listenerWeakReference.get().onDisconnect(pipeline);
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        public void destroy() {
            if (uploadHandler != null && uploadThread != null) {
                uploadHandler.removeCallbacksAndMessages(null);
                uploadThread.quitSafely();
            }
            if (downloadHandler != null && downloadThread != null) {
                downloadHandler.removeCallbacksAndMessages(null);
                downloadThread.quitSafely();
            }
            resetDownInfo();
            resetUploadInfo();
        }

        private void resetDownInfo() {
            downloadPackCount = 0;
            downloadSize = 0;

            downloadFileInfoLog = "";
            downloadProgress = "";
            downloadResult = "";
            downloading = false;
        }

        private void resetUploadInfo() {
            uploadFileInfoLog = "";
            uploadProgress = "";
            uploadResult = "";
            uploading = false;
        }

        private void uploadFile(Pipeline data) {
            if (data == null) {
                return;
            }
            if (uploading) {
                toast("uploading");
                return;
            }
            uploading = true;
            long time = System.currentTimeMillis();

            InputStream inputStream = null;
            FileOutputStream out = null;
            ByteArrayOutputStream outputStream = null;
            try {
                byte[] buff = new byte[3072];
                inputStream = itemView.getContext().getAssets().open("mop/" + uploadFileName);
                File tmp = new File(itemView.getContext().getCacheDir(), "mop.tmp");
                out = new FileOutputStream(tmp);
                outputStream = new ByteArrayOutputStream();
                int len;
                while ((len = inputStream.read(buff, 0, 3072)) > 0) {
                    outputStream.write(buff, 0, len);
                    out.write(buff, 0, len);
                }
                MOPCmdHelper.sendUploadFileReq(data, uploadFileName, outputStream.toByteArray(), time, MOPCmdHelper.getMD5(tmp), listener);

            } catch (IOException e) {
                LogUtils.e(tag, e.getMessage());
            } finally {
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    if (out != null) {
                        out.close();
                    }
                    if (outputStream != null) {
                        outputStream.close();
                    }
                } catch (IOException e) {
                    LogUtils.e(tag, e.getMessage());
                }
            }

            uploadCount++;
            updateUploadUI();
            uploading = false;
        }

        private void downloadFile(Pipeline pipeline, String filename) {
            if (pipeline == null) {
                return;
            }

            if (downloading) {
                toast("downloading");
                return;
            }
            downloading = true;
            long time = System.currentTimeMillis();

            // 获取文件信息
            MOPCmdHelper.FileInfo fileInfo = MOPCmdHelper.sendDownloadFileReq(pipeline, filename, listener);
            if (fileInfo == null || !fileInfo.isExist()) {
                LogUtils.e(tag, "downloadFile fail", "/MOP");
                downloading = false;
                return;
            }
            downloadFileInfoLog = fileInfo.toString();
            updateDownloadUI();

            RandomAccessFile stream = null;
            try {
                LogUtils.i(tag, " fileInfo=" + fileInfo);
                File file = DiskUtil.getDiskCacheDir(itemView.getContext(), fileInfo.getFilename());
                if (file.exists()) {
                    file.delete();
                }
                file.createNewFile();
                stream = new RandomAccessFile(file, "rw");
            } catch (IOException e) {
                LogUtils.e(tag, e.getMessage());
            }

            while (true) {
                // 开始读取文件数据
                byte[] headBuff = new byte[MOPCmdHelper.PACK_HEADER_SIZE];
                DataResult readData = pipeline.readData(headBuff);
                if (readData.getError() != null && readData.getError().errorCode().equals(DJIPipeLineError.CLOSING)) {
                    LogUtils.e(tag, "Pipeline is closing,finish down");
                    return;
                }
                if (readData.getLength() < MOPCmdHelper.PACK_HEADER_SIZE) {
                    LogUtils.e(tag, "readData.getLength()=" + readData.getLength() + " <8,jump over!result=" + readData);
                    continue;
                }
                // 这个包带有的文件字节
                MOPCmdHelper.FileTransResult result = MOPCmdHelper.parseFileDataCmd(headBuff);
                if (result == null) {
                    LogUtils.e(tag, "FileTransResult=null ,finish down");
                    downloading = false;
                    return;
                }
                if (result.isSuccess()) {
                    int length = result.getLength();
                    int sum = 0;
                    int readLength = length;
                    while (sum < length) {
                        byte[] dataBuff = new byte[readLength];
                        DataResult dataResult = pipeline.readData(dataBuff);
                        int len = dataResult.getLength();
                        if (len > 0) {
                            downloadPackCount++;
                            downloadSize += len;
                            byte[] subArray = BytesUtil.subArray(dataBuff, 0, 3);
                            sum += len;
                            readLength -= len;
                            String tmp = BytesUtil.toHexStringLowercase(subArray);
                            LogUtils.i(tag, "pack seq:" + downloadPackCount + ", data:" + tmp + ", length:" + len);
                            LogUtils.i(tag, filename + " download : " + sum);
                            downloadLogTv.post(() -> {
                                downloadProgress = String.format("have downloadPack = %d, downloadSize:%d/%d, useTime:%d(ms)", downloadPackCount,
                                        downloadSize, fileInfo.getFileLength(), System.currentTimeMillis() - time);
                                updateDownloadUI();
                            });
                            try {
                                if (stream != null) {
                                    stream.write(dataBuff, 0, len);
                                }
                            } catch (IOException e) {
                                LogUtils.e(tag, e.getMessage());
                            }
                        } else if (dataResult.getError() != null && dataResult.getError().errorCode().equals(DJIPipeLineError.CLOSING)) {
                            LogUtils.e(tag, "Pipeline is closing,finish down");
                            return;
                        }
                    }
                } else {
                    // 解析出错位置
                    int position = MOPCmdHelper.parseFileFailIndex(pipeline);
                    // ack
                    MOPCmdHelper.sendTransFileFailAck(pipeline, position);
                    // 确认是否能接着传
                    if (MOPCmdHelper.parseCommonAck(pipeline)) {
                        try {
                            if (stream != null) {
                                stream.seek(position);
                            }
                        } catch (IOException e) {
                            LogUtils.e(tag, e.getMessage());
                        }
                    } else {
                        try {
                            if (stream != null) {
                                stream.close();
                            }
                        } catch (IOException e) {
                            LogUtils.e(tag, e.getMessage());
                        }
                        downloadLogTv.post(() -> downloadLogTv.setText("transfer failure"));
                        downloading = false;
                        return;
                    }

                }
                if (MOPCmdHelper.isFileEnd(headBuff)) {
                    LogUtils.i(tag, "Download success ,updateDownloadUI");
                    downloadResult = "Download success";
                    updateDownloadUI();
                    break;
                }
            }
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException e) {
                LogUtils.e(tag, e.getMessage());
            }
            downloading = false;

            boolean result = verifyMd5(fileInfo, DiskUtil.getDiskCacheDir(itemView.getContext(), fileInfo.getFilename()));
            downloadResult = "verify md5 :" + result;
            downloadCount++;
            if (downloadSize == fileInfo.getFileLength() && result) {
                downloadSuccessCount++;
                MOPCmdHelper.sendTransAck(pipeline, true);
            } else {
                MOPCmdHelper.sendTransAck(pipeline, false);
            }
            updateDownloadUI();
            if (autoDownloadSwitch.isChecked()) {
                downloadTv.postDelayed(() -> downloadTv.performClick(), 500);
            }
        }

        private boolean verifyMd5(MOPCmdHelper.FileInfo fileInfo, File file) {
            String md5 = BytesUtil.toHexStringLowercase(fileInfo.getMd5());
            String md5_1 = BytesUtil.toHexStringLowercase(MOPCmdHelper.getMD5(file));
            String log = String.format("MOPCmdHelper.FileInfo_md5: %s, file_md5:%s", md5, md5_1);
            LogUtils.e("PipelineAdapter", log, "/MOP");
            return md5.equals(md5_1);
        }

        private void toast(String text) {
            itemView.post(() -> Toast.makeText(itemView.getContext(), text, Toast.LENGTH_SHORT).show());
        }

        public void handlerTipEvent(MOPCmdHelper.TipEvent event) {
            switch (event.getType()) {
                case MOPCmdHelper.TipEvent.UPLOAD:
                    if (event.getResult() != null) {
                        uploadResult = event.getResult();
                        if (uploadResult.contains("Success")) {
                            // hard code
                            uploadSuccessCount++;
                        }
                    }
                    if (event.getProgress() != null) {
                        uploadProgress = event.getProgress();
                    }
                    updateUploadUI();
                    break;
                case MOPCmdHelper.TipEvent.DOWNLOAD:
                    if (event.getResult() != null) {
                        downloadResult = event.getResult();
                    }
                    if (event.getProgress() != null) {
                        downloadProgress = event.getProgress();
                    }
                    downloadLogTv.post(() -> downloadLogTv.setText(downloadLogTv.getText().toString() + "\n" + event.getResult()));
                    break;
                default:
                    break;
            }

        }

        public void onEvent3BackgroundThread(MOPCmdHelper.FileInfo event) {
            uploadFileInfoLog = event.toString();
            updateUploadUI();
        }

        private void updateUploadUI() {
            StringBuilder sb = new StringBuilder("Upload:" + "\n")
                    .append(uploadFileInfoLog == null ? "" : uploadFileInfoLog).append("\n")
                    .append(uploadProgress == null ? "" : uploadProgress).append("\n")
                    .append(uploadResult == null ? "" : uploadResult).append("\n")
                    .append("成功/次数：" + uploadSuccessCount + "/" + uploadCount);

            uploadLogTv.post(() -> uploadLogTv.setText(sb.toString()));
        }

        private void updateDownloadUI() {
            StringBuilder sb = new StringBuilder("Download:" + "\n")
                    .append(downloadFileInfoLog == null ? "" : downloadFileInfoLog).append("\n")
                    .append(downloadProgress == null ? "" : downloadProgress).append("\n")
                    .append(downloadResult == null ? "" : downloadResult).append("\n")
                    .append("成功/次数：" + downloadSuccessCount + "/" + downloadCount);
            downloadLogTv.post(() -> downloadLogTv.setText(sb.toString()));
        }


        private void showDialog(Context context, Pipeline pipeline) {
            View root = LayoutInflater.from(context).inflate(R.layout.dialog_mop_upload, null, false);
            new AlertDialog.Builder(context)
                    .setTitle("Select File Size")
                    .setView(root)
                    .setPositiveButton("Confirm", (dialog, which) -> {
                        dialog.dismiss();
                        RadioGroup group = root.findViewById(R.id.group_file);
                        int checkedRadioButtonId = group.getCheckedRadioButtonId();
                        if (checkedRadioButtonId == R.id.rb_1) {
                            uploadFileName = "mopSample.log";
                        } else if (checkedRadioButtonId == R.id.rb_2) {
                            uploadFileName = "mopSample.jpeg";
                        } else if (checkedRadioButtonId == R.id.rb_3) {
                            uploadFileName = "mopSample.mp4";
                        }
                        uploadHandler.post(() -> {
                            resetUploadInfo();
                            uploadFile(pipeline);
                        });
                    })
                    .show();
        }
    }


    public interface OnDisconnectListener {
        void onDisconnect(Pipeline data);
    }

    public interface OnEventListener {
        void onTipEvent(MOPCmdHelper.TipEvent event);

        void onFileInfoEvent(MOPCmdHelper.FileInfo event);
    }
}
