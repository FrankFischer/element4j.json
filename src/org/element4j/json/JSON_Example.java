package org.element4j.json;
/* 
 * Copyright (c) 2011-2011 Frank Fischer.
 * 
 * This file is part of element4j (see <http://www.element4j.org>).
 * 
 * element4j is free software: you can redistribute it and/or modify it under the terms 
 * of the GNU Lesser General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 * 
 * element4j is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with element4j.
 * If not, see <http://www.gnu.org/licenses/>.
 */
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.zip.Deflater;
import java.math.BigInteger;
import java.nio.charset.Charset;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
/**
 * Presents some of the features of this JSON library.
 * 
 * @author  Frank Fischer
 * @since   2011
 * @version 0.1.20111010_0755
 */
public class JSON_Example {
  
  private static final Charset utf8 = Charset.forName("UTF-8");
  
  public JSON_Example() throws JSONException {
    System.out.println("starting");
    
    for (String json:new String[] {
        "true",
        "12.7",
        "\"Hello, world!\"",
      "{ \"Hello\": \"World, Hello\" }",
      "{ \"number\": 1 }",
      "{ \"Hello\": \"World\", \"Hi\": \"Frank\" }",
      "[ null, true, false , 123, 12.45, NaN, \"Hello\nWorld\", [\"A\", \"B\", \"C\"], {\"other Unicode codepages\":\"€ äöü ÄÖÜ ß\"} ]",
      "{ \"list\":[true, {\"one\":1}, 12.5], \"number\":3 }",
      "\"This is a really very long text and i sincerely hope that it will be long enough for the compression algorithm to be effective!!!!!!!                                                                                                                                                                                      \""
    })  test(json);
    
    String helloBytes = "$" + (new BigInteger("HalloBytes".getBytes())).toString(16) + "$";
    Object o          = JSONDeserializer.fromString("{ # this an example of a comment and a byte array\n \"bytes\": " + helloBytes + " }");
    byte[] bytes      = (byte[])((LinkedHashMap<?,?>)o).get("bytes");
    System.out.println("----------------------------------------------");
    System.out.println(helloBytes);
    System.out.println(JSONSerializer.toString(o));
    System.out.println(new String(bytes));
    System.out.println("----------------------------------------------");
    try {
      o = JSONDeserializer.fromString("[1:2]");
    } catch (JSONException e) {
      System.out.println("ERROR message : " + e.getMessage() );
      System.out.println("ERROR JSON1   : " + e.getJSON()[0] );
      System.out.println("ERROR JSON2   : " + e.getJSON()[1] );
    }
    System.out.println("----------------------------------------------");
    
    System.out.println(JSONSerializer.toString(new Object()));  // should lead to an error message
  }
  
  public static byte[] createAESKey(int keySize) {
    try {
      KeyGenerator kgen = KeyGenerator.getInstance("AES");
      kgen.init(keySize);
      SecretKey    key   = kgen.generateKey();
      byte[]       bytes = key.getEncoded();
      return bytes;
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
    return null;
  }
  
  private Cipher[] getCiphers(byte[] key) {
    SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
    try {
      Cipher ecipher = Cipher.getInstance("AES");
      Cipher dcipher = Cipher.getInstance("AES");
      ecipher.init(Cipher.ENCRYPT_MODE, keySpec);
      dcipher.init(Cipher.DECRYPT_MODE, keySpec);
      return new Cipher[] { ecipher, dcipher };
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  public void test(String s0) throws JSONException {
    System.out.println("in :<" + s0 + ">");
    Object o1 = JSONDeserializer.fromString(s0);
    String s1 = JSONSerializer.toString(o1);
    System.out.println("out:<" + s1 + ">");
    
    Cipher[] ciphers = getCiphers(createAESKey(128));
    
    for (int i=0; i<5; i++) {
      byte[] b  = null;
      Object o2 = null;
      switch (i) {
        
        case 0: b  = s1.getBytes(utf8);
                o2 = JSONDeserializer.fromString(s1); break;                                 // String
                
        case 1: b  = JSONSerializer.toByteArray(o1);                                         // byte[]
                o2 = JSONDeserializer.fromByteArray(b);  break;
                
        case 2: b  = JSONSerializer.toByteArray(o1, null, Deflater.BEST_COMPRESSION);        // byte[], compressed
                o2 = JSONDeserializer.fromByteArray(b, null, true);  break;
                
        case 3: b  = JSONSerializer.toByteArray(o1, ciphers[0], Deflater.NO_COMPRESSION);    // byte[], encrypted
                o2 = JSONDeserializer.fromByteArray(b, ciphers[1], false);  break;
                
        case 4: b  = JSONSerializer.toByteArray(o1, ciphers[0], Deflater.BEST_COMPRESSION);  // byte[], compressed and encrypted
                o2 = JSONDeserializer.fromByteArray(b, ciphers[1], true);  break;
      }
      String s2 = JSONSerializer.toString(o2);
      System.out.println(i + ": " + b.length + "   " + s1.equals(s2));
    }
    System.out.println("------------------------------------------------------------------------------------");
  }
  
  public static void main(String[] args) throws JSONException {
    new JSON_Example();
  }
}