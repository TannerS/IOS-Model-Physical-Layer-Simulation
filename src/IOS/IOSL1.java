package IOS;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IOSL1 
{
    public static void main(String[] args) 
    {
        // get hashmap to map 4b/5b
        Map<Integer, Integer> five_four_b = new HashMap<Integer, Integer>();
        // set up hashmap
        five_four_b = setup5B4BHash(five_four_b);
        // needed
        InputStream input_stream = null;
        OutputStream output_stream = null;
        Socket socket = null;
        // needed
        float base_line = 0;
        // size of preamble
        final int PREAMBLE_SIZE = 64;
        // size of all possible bytes we can recieve from server
        final int ALL_BYTES_SIZE = 320;
        // size of the final bytes to send to server
        final int OUTPUT_DATA_SIZE = 32;
        // size of each half byte
        final int HALFBYTE_SIZE = 64;
        
        try 
        {
            // connect to server and open streams
            socket = new Socket("codebank.xyz", 38002);
            // get in stream
            input_stream = socket.getInputStream();
            System.out.println("Connection Successful");
            // get baseline
            base_line = getPreamble(input_stream, PREAMBLE_SIZE);
            System.out.println("Preamble recieved: " + base_line);
            // originally, I tried doing this in just bytes
            int[] all_bytes = new int[ALL_BYTES_SIZE];
            // data to be sent to server
            byte[] output_data = new byte[OUTPUT_DATA_SIZE];
            // data to be used to get half bytes before 4b/5b conversion
            int half_bytes[] = new int[HALFBYTE_SIZE];
            // get all bytes (bits)
            for(int i = 0; i < ALL_BYTES_SIZE; ++i)
                all_bytes[i] = input_stream.read();
            // pass in array of all bytes and convert them to NRZI version 
            NRZI (all_bytes, (all_bytes[0] == 1 ? true : false), ALL_BYTES_SIZE, base_line);
            // temp variable to be used for combining the bits
            int temp = 0;
            // a int to hold the prev value for the byte ocnversion
            int prev = 0;
            // get ever 5 bits and combine them into a new array 
            for(int i = 0; i < ALL_BYTES_SIZE; i++)
            {
                // use the temp var and shift them and xor them with the signal
                // to concatenate the bits into 1
                temp = shiftByte2Left(temp, 1);
                temp = xorBytes(temp,all_bytes[i]);
                // if every 4 bits (4,9, etc since index 0)
                if( (i + 1) % 5 == 0  && (i != 0))
                {
                    // temp will be getting shfit and xor with hat ever
                    // all_bytes[i] setting it into one byte at the end
                    // also get 4b value
                    all_bytes[i] = five_four_b.get(temp);
                    System.out.println("5B: " + temp + " 4B: "+ all_bytes[i] );
                    // byte done, reset temp
                    temp = 0;
                }
            }
            temp = 0;
            for(int i = 0, j = 0; i < ALL_BYTES_SIZE; i++)
            {
                if(j < 32)
                {
                    if( (i + 1) % 10 == 0  && (i != 0))
                    {
                        System.out.println("halfbyte (first 4b): " + all_bytes[i-5]);
                        temp = shiftByte2Left(all_bytes[i-5], 4);
                        System.out.println("halfbyte (first 4b) shifted : " + temp);
                        System.out.println("halfbyte (second 4b): " + all_bytes[i]);
                        temp = xorBytes(temp, all_bytes[i]);
                        System.out.println("whole byte: " + temp + " at j: " + j);
                        output_data[j] = (byte) temp;
                        //System.out.println("output: " + output_data[j] );
                        j++;
                    }
                }
                else 
                    break;
            }

            output_stream = socket.getOutputStream();
            output_stream.write(output_data);
            System.out.println("Is good?: " + input_stream.read());
        }
        catch (IOException ex) 
        {
            System.out.println("Error"); 
            Logger.getLogger(IOSL1.class.getName()).log(Level.SEVERE, null, ex);
        }      
    }   
    // used to determine the baseline
    static float getPreamble(final InputStream in, final int SIZE) throws IOException
    {
        float pream = 0;
        for(int i = 0; i < SIZE; i++)
            pream += in.read();
        pream /= SIZE; 
        
        return pream;
    }
    // convert the signals based on NRZI
    static void NRZI (int[] bits, boolean signal, int size, float base_line)
    {
        for(int i = 0; i < size; i++)
        { 
            // get signal based on high or low
            bits[i] = highOrLowCheck(bits[i], base_line);
            // bit is high, signal is low
            // bit stays the same and signal slips
            if(bits[i] == 1 && !signal)
                signal = true;
            // bit is 1 and signal is high
            // bit flips and signal reminds the same
            else if(bits[i] == 1 && signal)
                bits[i] = 0;
            //bit is low and signal is high
            // bit is flipped && signal is flipped
            else if(bits[i] == 0 && signal)
            {
                bits[i] = 1;
                signal = false;
            }
        }
    }
    // xor two bytes 
    // used to concatenate
    static int xorBytes(int first, int second)
    {
        // xor both bytes
        return first ^ second;
    }
    // check if signal is a high or low based on baseline
    static int highOrLowCheck(int signal, float base)
    {
        return((signal >= base) ? 1 : 0);
    }
    // shitfs buts to left 
    static byte shiftByte2Left(int original_byte, final int shift_size)
    {
        // shift byte to the left
        return  (byte) (original_byte << shift_size);
    }
    // set up hashmap with 4b/5b values
    static Map<Integer, Integer> setup5B4BHash(Map<Integer, Integer> map)
    {
        // hashmap with int values of the 4b/5b tanssision 
        if(map == null)
            return null;
        map.put(30, 0);
        map.put(9, 1);
        map.put(20, 2);
        map.put(21, 3);
        map.put(10, 4);       
        map.put(11, 5);        
        map.put(14, 6);        
        map.put(15, 7);        
        map.put(18, 8);
        map.put(19, 9);
        map.put(22, 0xA);
        map.put(23, 0xB);
        map.put(26, 0xC);
        map.put(27, 0xD);       
        map.put(28, 0xE);        
        map.put(29, 0xF);                                   
        return map;
    }
}
