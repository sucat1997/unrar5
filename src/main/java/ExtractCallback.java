import net.sf.sevenzipjbinding.*;

import java.io.*;

public class ExtractCallback implements IArchiveExtractCallback {
    private int index;
    private String packageName;
    private IInArchive inArchive;
    private String ourDir;

    public ExtractCallback(IInArchive inArchive, String packageName, String ourDir) {
        this.inArchive = inArchive;
        this.packageName = packageName;
        this.ourDir = ourDir;
    }

    @Override
    public void setCompleted(long arg0) throws SevenZipException {
    }

    @Override
    public void setTotal(long arg0) throws SevenZipException {
    }

    @Override
    public ISequentialOutStream getStream(int index, ExtractAskMode extractAskMode) throws SevenZipException {
        this.index = index;
        final String path = (String) inArchive.getProperty(index, PropID.PATH);
        final boolean isFolder = (Boolean) inArchive.getProperty(index, PropID.IS_FOLDER);
        final String[] oldPath = {""};
        return new ISequentialOutStream() {
            public int write(byte[] data) throws SevenZipException {
                try {
                    if (!isFolder) {
//                        System.out.println(path);
                        File file = new File(ourDir+"\\" + path);
                        if (path.equals(oldPath[0])){
                            save2File(file, data,true);
                        }else{
                            save2File(file, data,false);
                        }
                        oldPath[0] = path;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return data.length;
            }
        };
    }
    public boolean save2File(File file, byte[] msg,boolean append) {
        OutputStream fos = null;
        try {
            File parent = file.getParentFile();
            boolean bool;
            if ((!parent.exists()) && (!parent.mkdirs())) {
                return false;
            }
//            fos = new FileOutputStream(file);
            fos = new FileOutputStream(file,append);//是否追加
            fos.write(msg);
            fos.flush();
            return true;
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            File parent;
            return false;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }
    }

    @Override
    public void prepareOperation(ExtractAskMode arg0) throws SevenZipException {
    }

    @Override
    public void setOperationResult(ExtractOperationResult extractOperationResult) throws SevenZipException {
        String path = (String) inArchive.getProperty(index, PropID.PATH);
        boolean isFolder = (Boolean) inArchive.getProperty(index, PropID.IS_FOLDER);
//        if(ZipUtils.checkOnlyGetDir(path) && !isFolder){
//            if (extractOperationResult != ExtractOperationResult.OK) {
//                StringBuilder sb = new StringBuilder();
//                sb.append("解压").append(packageName).append("包的").append(path).append("文件");
//                sb.append("失败！");
//                log.error(sb.toString());
//            }
//        }
    }

    public static boolean save2File(File file, byte[] msg) {
        OutputStream fos = null;
        try {
            File parent = file.getParentFile();
            boolean bool;
            if ((!parent.exists()) && (!parent.mkdirs())) {
                return false;
            }
            fos = new FileOutputStream(file);
            fos.write(msg);
            fos.flush();
            return true;
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            File parent;
            return false;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
