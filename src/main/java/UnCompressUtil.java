import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;

import java.io.IOException;

public class UnCompressUtil {

    /**
     * 将rar格式的文件解压到指定路径
     * @param rarDir 原压缩文件路径
     * @param outputDir 解压目录
     * @throws IOException
     */
    public static void unRar5AndOver5(String rarDir,String outputDir) throws IOException {
        //RandomAccessFile randomAccessFile = null;
        BufferedRandomAccessFile randomAccessFile = null;
        IInArchive inArchive = null;

        // 第一个参数是需要解压的压缩包路径，第二个参数参考JdkAPI文档的RandomAccessFile
        //randomAccessFile = new RandomAccessFile(rarDir, "r");
        randomAccessFile = new BufferedRandomAccessFile(rarDir, "r");
        inArchive = SevenZip.openInArchive(null, new RandomAccessFileInStream(randomAccessFile));

        int[] in = new int[inArchive.getNumberOfItems()];
        for (int i = 0; i < in.length; i++) {
            in[i] = i;
        }
        inArchive.extract(in, false, new ExtractCallback(inArchive, "366", outputDir));
        inArchive.close();
        randomAccessFile.close();
    }



    /**
     * 从zip压缩包中解压一个指定的文件到指定的路径
     * @param targetFileName 目标文件名称
     * @param srcPath 原压缩文件路径
     * @param destPath 解压目录
     * @throws Exception
     */
    public static void decompress(String targetFileName, String srcPath, String destPath) throws Exception {
        File file = new File(srcPath);
        if (!file.exists()) {
            throw new RuntimeException(srcPath + "不存在");
        }

        ZipFile zf = new ZipFile(file);
        Enumeration entries = zf.entries();
        ZipEntry entry;
        while (entries.hasMoreElements()) {
            entry = (ZipEntry) entries.nextElement();
            String name = entry.getName();
            if (!name.endsWith(targetFileName)) {
                continue;
            }

            File dir = new File(destPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File f = new File(dir + File.separator + targetFileName);
            if (!f.exists()) {
                f.createNewFile();
            }

            InputStream is = zf.getInputStream(entry);
            FileOutputStream fos = new FileOutputStream(f);

            int count;
            byte[] buffer = new byte[1024 * 8];
            while (true) {
                count = is.read(buffer);
                if (count == -1) {
                    break;
                }

                fos.write(buffer, 0, count);
            }

            is.close();
            fos.close();
        }
    }
}
