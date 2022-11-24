import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Ex1 {
    public static void main(String[] args) {
    String outPutToFile = readInputFromTxt.readInputAndReturnAns("input.txt");
    try{
        FileWriter writer = new FileWriter("output.txt");
        writer.write(outPutToFile);
        writer.close();
    } catch (IOException e) {
        e.printStackTrace();
    }


    }
}

