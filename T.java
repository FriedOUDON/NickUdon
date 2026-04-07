import java.nio.charset.*;
public class T {
  public static void main(String[] a) {
    System.out.println("default=" + Charset.defaultCharset());
    System.out.println("native=" + System.getProperty("native.encoding"));
    System.out.println("stdout=" + System.getProperty("stdout.encoding"));
    System.out.println("stderr=" + System.getProperty("stderr.encoding"));
    java.io.Console c = System.console();
    System.out.println("console=" + (c == null ? "<none>" : c.charset()));
    System.out.println("?????");
  }
}
