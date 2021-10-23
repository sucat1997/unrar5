import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class BufferedRandomAccessFile extends RandomAccessFile {

    private static final int DEFAULT_BUFFER_BIT_LEN = 10;         //1024个字节就是2的10次方,这个就是
    private static final int DEFAULT_BUFFER_SIZE = 1 << DEFAULT_BUFFER_BIT_LEN;  //设置默认值为1024字节,就是缓冲区大小

    protected byte buf[];      //缓冲区
    protected int bufbitlen;
    protected int bufsize;     // 缓冲区大小
    protected long bufmask;
    protected boolean bufdirty;
    protected int bufusedsize;
    protected long curpos;

    protected long bufstartpos;
    protected long bufendpos;
    protected long fileendpos;

    protected boolean append;
    protected String filename;
    protected long initfilelen;

    ///构造方法//
    public BufferedRandomAccessFile(String name) throws IOException {
        this(name, "r", DEFAULT_BUFFER_BIT_LEN);
    }

    public BufferedRandomAccessFile(File file) throws IOException, FileNotFoundException {
        this(file.getPath(), "r", DEFAULT_BUFFER_BIT_LEN);
    }

    public BufferedRandomAccessFile(String name, int bufbitlen) throws IOException {
        this(name, "r", bufbitlen);
    }

    public BufferedRandomAccessFile(File file, int bufbitlen) throws IOException, FileNotFoundException {
        this(file.getPath(), "r", bufbitlen);
    }

    public BufferedRandomAccessFile(String name, String mode) throws IOException {
        this(name, mode, DEFAULT_BUFFER_BIT_LEN);
    }

    public BufferedRandomAccessFile(File file, String mode) throws IOException, FileNotFoundException {
        this(file.getPath(), mode, DEFAULT_BUFFER_BIT_LEN);
    }

    public BufferedRandomAccessFile(File file, String mode, int bufbitlen) throws IOException, FileNotFoundException {
        this(file.getPath(), mode, bufbitlen);
    }

    /**
     * @param name      文件名
     * @param mode      打开文件的模式 , "r", "rw", "w+",,,
     * @param bufbitlen 缓存区大小, 默认bufbitlen = 10 ,就是缓存区大小为1024 ; 为9,则是,512, 依次类推
     * @throws IOException
     */
    public BufferedRandomAccessFile(String name, String mode, int bufbitlen) throws IOException {
        super(name, mode);
        this.init(name, mode, bufbitlen);
    }
    ///构造方法//

    /**
     * 构造方法调用的初始化
     *
     * @param name
     * @param mode
     * @param bufbitlen
     * @throws IOException
     */
    private void init(String name, String mode, int bufbitlen) throws IOException {
        if (mode.equals("r") == true) {   //是否追加
            this.append = false;
        } else {
            this.append = true;
        }

        this.filename = name;                         //文件名
        this.initfilelen = super.length();            //初始文件长度
        this.fileendpos = this.initfilelen - 1;       //文件结束位置的指针偏移量
        this.curpos = super.getFilePointer();         //获取当前的文件指针

        if (bufbitlen < 0) {
            throw new IllegalArgumentException("bufbitlen size must >= 0");
        }

        this.bufbitlen = bufbitlen;            //缓冲区大小的"指数"/ "位长"
        this.bufsize = 1 << bufbitlen;         //缓存区大小
        this.buf = new byte[this.bufsize];     //初始化缓冲区
        this.bufmask = ~((long) this.bufsize - 1L);   //用于优化计算缓冲区开始位置指针的变量
        this.bufdirty = false;
        this.bufusedsize = 0;                  //缓冲区使用的大小
        this.bufstartpos = -1;                 //缓冲区开始位置对应文件中的指针
        this.bufendpos = -1;                   //缓冲区结束为止对应文件中的指针, 这两个指针位置在seek()方法中被赋值
    }

    /**
     * bufdirty为真,把 buf[] 中尚未写入磁盘的数据,写入磁盘
     *
     * @throws IOException
     */
    private void flushbuf() throws IOException {
        if (this.bufdirty == true) {
            if (super.getFilePointer() != this.bufstartpos) {
                super.seek(this.bufstartpos);
            }
            super.write(this.buf, 0, this.bufusedsize);
            this.bufdirty = false;
        }
    }

    /**
     * 根据bufstartpos,填充buf[]
     *
     * @return
     * @throws IOException
     */
    private int fillbuf() throws IOException {
        super.seek(this.bufstartpos);
        this.bufdirty = false;
        return super.read(this.buf);
    }

    /**
     * 移动文件指针到pos位置, 并且把buf[] 映射填充至pos所在的文件块
     */
    public void seek(long pos) throws IOException {
        if ((pos < this.bufstartpos) || (pos > this.bufendpos)) { // seek pos not in buf
            this.flushbuf();
            if ((pos >= 0) && (pos <= this.fileendpos) && (this.fileendpos != 0)) { // seek pos in file (file length > 0)
                this.bufstartpos = pos & this.bufmask;
                this.bufusedsize = this.fillbuf();
            } else if (((pos == 0) && (this.fileendpos == 0)) || (pos == this.fileendpos + 1)) { // seek pos is append pos
                this.bufstartpos = pos;
                this.bufusedsize = 0;
            }
            this.bufendpos = this.bufstartpos + this.bufsize - 1;
        }
        this.curpos = pos;
    }

    /**
     * 读取当前文件POS位置所在的字节
     * bufstartpos、bufendpos代表BUF映射在当前文件的首/尾偏移地址。
     * curpos指当前类文件指针的偏移地址。
     *
     * @param pos 从给定的文件指针处开始读入一个字节
     * @return
     * @throws IOException
     */
    public byte read(long pos) throws IOException {
        if (pos < this.bufstartpos || pos > this.bufendpos) {
            this.flushbuf();
            this.seek(pos);
            if ((pos < this.bufstartpos) || (pos > this.bufendpos)) {
                throw new IOException();
            }
        }
        this.curpos = pos;
        return this.buf[(int) (pos - this.bufstartpos)];
    }

    public int read(byte b[]) throws IOException {
        return this.read(b, 0, b.length);
    }

    public int read(byte b[], int off, int len) throws IOException {
        long readendpos = this.curpos + len - 1;  //读入字节最远的位置
        if (readendpos <= this.bufendpos && readendpos <= this.fileendpos) { // read in buf 从缓冲中读取
            System.arraycopy(this.buf, (int) (this.curpos - this.bufstartpos), b, off, len);
        } else { // read b[] size > buf[]
            if (readendpos > this.fileendpos) { // read b[] part in file 如果输出的内容大于文件的长度
                len = (int) (this.length() - this.curpos + 1);        //从文件当前位置开始的文件所有的字节数
            }
            super.seek(this.curpos);         //移动文件指针到当前位置
            len = super.read(b, off, len);         //读取知道文件结尾的所有字节,保存到b中
            readendpos = this.curpos + len - 1;
        }
        this.seek(readendpos + 1);
        return len;
    }

    public void write(byte b[]) throws IOException {
        this.write(b, 0, b.length);
    }

    public void write(byte b[], int off, int len) throws IOException {
        long writeendpos = this.curpos + len - 1;
        if (writeendpos <= this.bufendpos) { // b[] in cur buf
            System.arraycopy(b, off, this.buf, (int) (this.curpos - this.bufstartpos), len);
            this.bufdirty = true;
            this.bufusedsize = (int) (writeendpos - this.bufstartpos + 1);//(int)(this.curpos - this.bufstartpos + len - 1);
        } else { // b[] not in cur buf
            super.seek(this.curpos);
            super.write(b, off, len);
        }
        if (writeendpos > this.fileendpos)
            this.fileendpos = writeendpos;
        this.seek(writeendpos + 1);
    }

    public boolean append(byte bw) throws IOException {
        return this.write(bw, this.fileendpos + 1);
    }

    public boolean write(byte bw) throws IOException {
        return this.write(bw, this.curpos);
    }

    public boolean write(byte bw, long pos) throws IOException {
        if ((pos >= this.bufstartpos) && (pos <= this.bufendpos)) { // write pos in buf
            this.buf[(int) (pos - this.bufstartpos)] = bw;
            this.bufdirty = true;

            if (pos == this.fileendpos + 1) { // write pos is append pos
                this.fileendpos++;
                this.bufusedsize++;
            }
        } else { // write pos not in buf
            this.seek(pos);
            if ((pos >= 0) && (pos <= this.fileendpos) && (this.fileendpos != 0)) { // write pos is modify file
                this.buf[(int) (pos - this.bufstartpos)] = bw;
            } else if (((pos == 0) && (this.fileendpos == 0)) || (pos == this.fileendpos + 1)) { // write pos is append pos
                this.buf[0] = bw;
                this.fileendpos++;
                this.bufusedsize = 1;
            } else {
                throw new IndexOutOfBoundsException();
            }
            this.bufdirty = true;
        }
        this.curpos = pos;
        return true;
    }

    public long length() throws IOException {
        return this.max(this.fileendpos + 1, this.initfilelen);
    }

    public void setLength(long newLength) throws IOException {
        if (newLength > 0) {
            this.fileendpos = newLength - 1;
        } else {
            this.fileendpos = 0;
        }
        super.setLength(newLength);
    }

    public long getFilePointer() throws IOException {
        return this.curpos;
    }

    private long max(long a, long b) {
        if (a > b) return a;
        return b;
    }

    public void close() throws IOException {
        this.flushbuf();
        super.close();
    }

    /**
     * 测试用例
     *
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        long readfilelen = 0;
        BufferedRandomAccessFile brafReadFile, brafWriteFile;

        String path = "C:\\Windows\\Fonts\\simsun.ttc";
        brafReadFile = new BufferedRandomAccessFile(path);
        readfilelen = brafReadFile.initfilelen;
        brafWriteFile = new BufferedRandomAccessFile(".\\STKAITI.001", "rw", 10);

        byte buf[] = new byte[1024];
        int readcount;

        long start = System.currentTimeMillis();

        while ((readcount = brafReadFile.read(buf)) != -1) {
            brafWriteFile.write(buf, 0, readcount);
        }

        brafWriteFile.close();
        brafReadFile.close();

        System.out.println("BufferedRandomAccessFile Copy & Write File: "
                + brafReadFile.filename
                + "    FileSize: "
                + java.lang.Integer.toString((int) readfilelen >> 1024)
                + " (KB)    "
                + "Spend: "
                + (double) (System.currentTimeMillis() - start) / 1000
                + "(s)");

        java.io.FileInputStream fdin = new java.io.FileInputStream(path);
        java.io.BufferedInputStream bis = new java.io.BufferedInputStream(fdin, 1024);
        java.io.DataInputStream dis = new java.io.DataInputStream(bis);

        java.io.FileOutputStream fdout = new java.io.FileOutputStream(".\\STKAITI.002");
        java.io.BufferedOutputStream bos = new java.io.BufferedOutputStream(fdout, 1024);
        java.io.DataOutputStream dos = new java.io.DataOutputStream(bos);

        start = System.currentTimeMillis();

        for (int i = 0; i < readfilelen; i++) {
            dos.write(dis.readByte());
        }

        dos.close();
        dis.close();

        System.out.println("DataBufferedios Copy & Write File: "
                + brafReadFile.filename
                + "    FileSize: "
                + java.lang.Integer.toString((int) readfilelen >> 1024)
                + " (KB)    "
                + "Spend: "
                + (double) (System.currentTimeMillis() - start) / 1000
                + "(s)");
    }
}