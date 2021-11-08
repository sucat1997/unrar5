import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;

import java.io.IOException;

public class UnCompressUtil {

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
}
