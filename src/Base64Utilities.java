//package lv.polarisit.base64enc;
//import jargs.gnu.CmdLineParser;
//import jargs.gnu.CmdLineParser.Option;
import java.io.IOException;
//import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
//import java.util.Arrays;
//import java.util.List;
import org.apache.commons.codec.binary.Base64;
public class Base64Utilities {
         static String encodedString ;
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		Base64Utilities o = new Base64Utilities();
          String op = args[0];
          String fileName = args[1];
          System.out.println("args: "+op+" "+fileName);
          
          if(op.equals("-e"))o.testEnc(fileName);
          else if(op.equals("-d"))o.testDec(fileName);
        }
        public Base64Utilities(){}
        public void testEnc(String fileName) throws Exception{
            byte[] bytes = readUsingFiles(fileName);
            String s = encodeB64(bytes);
            System.out.println("encoded : " + s);
            byte[] b = decodeB64(s);
            System.out.println("decoded : " + new String(b));
	}
        public void testDec(String fileName) throws Exception{
            byte[] bytes = readUsingFiles(fileName);
            String s = new String(bytes).replaceAll("(\\\\r\\\\n|\\\\n)","");  // remove pseudo end of lines

            System.out.println("decoding: "+s);
            byte[] b = decodeB64(s);
            System.out.println("decoded : " + new String(b));
	}
        
        
static String encodeB64(byte[] in){
    Base64 b64 = new Base64();
    String encoded = b64.encodeToString(in);
    return encoded;
}       
        
static byte[] decodeB64(String in){
	in = in.replaceAll("(\\\\r\\\\n|\\\\n|\\\\u003d)",""); // remove potentially extra characters
	//in = in.replaceAll("\u003d","");
    Base64 b64 = new Base64();
    byte[] b2 = b64.decode(in);
    return b2;
}         
        
        
        private static byte[] readUsingFiles(String fileName) throws IOException {
        Path path = Paths.get(fileName);
        System.out.println("Input file "+path.toAbsolutePath().toString());
        //read file to byte array
        byte[] bytes = Files.readAllBytes(path);
        //read file to String list
        //List<String> allLines = Files.readAllLines(path, StandardCharsets.UTF_8);
        return bytes;
        }

}
