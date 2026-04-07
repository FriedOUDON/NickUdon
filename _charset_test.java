import java.nio.charset.*;
public class T { public static void main(String[] a) {
 System.out.println("default=" + Charset.defaultCharset());
 System.out.println("native=" + System.getProperty("native.encoding"));
 System.out.println("stdout=" + System.getProperty("stdout.encoding"));
 System.out.println("stderr=" + System.getProperty("stderr.encoding"));
 System.out.println("console=" + java.io.Console.charset());
 System.out.println("?????");
 }}
