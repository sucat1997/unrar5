import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
      //解压rar测试
      UnCompressUtil.unRar5AndOver5("C:\\Users\\89755\\Desktop\\1h.第一季.rar","C:\\Users\\89755\\Desktop\\1h.第一季\\");

      //解压zip压缩包中指定文件到指定路径测试
      UnCompressUtil.decompress("A77AF133402D4B19B78897D95A2675AC.jpg","C:\\Users\\Administrator\\Desktop\\pg测试数据\\bcp\\aa.zip"
      	,"C:\\Users\\Administrator\\Desktop\\pg测试数据\\back");
    }
}
