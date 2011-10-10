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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.zip.InflaterInputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
/**
 * Deserializes a JSON structure from a byte array or a string.
 * 
 * @author  Frank Fischer
 * @since   2011
 * @version 0.1.20111010_0755
 */
public class JSONDeserializer extends AbstractJSONDeserializer {
  
  private static final Charset              utf8 = Charset.forName("UTF-8");
  private static       JSONDeserializer     instance;
  private static       ByteArrayInputStream bis;
  private              InputStream          is2, is3;
  
  private JSONDeserializer() { super(); }
  
  private static void init2() { instance = new JSONDeserializer(); }
  
  // -------------------------------------------------------------------------------------------------------------------
  
  public static Object fromString(String s) throws JSONException {
    return fromByteArray(s.getBytes(utf8));
  }
  
  public static Object fromByteArray(byte[] b) throws JSONException {
    return fromByteArray(b, null, false);
  }
  
  public static Object fromByteArray(byte[] b, Cipher cipher, boolean decompress) throws JSONException {
    bis = new ByteArrayInputStream(b);
    return fromInputStream(bis, cipher, decompress);
  }
  
  public static Object fromInputStream(InputStream is) throws JSONException {
    return fromInputStream(is, null, false);
  }
  
  public static Object fromInputStream(InputStream is, Cipher cipher, boolean decompress) throws JSONException {
    if (instance == null)  init2();
    return instance._fromInputStream(is, cipher, decompress);
  }
  
  // -------------------------------------------------------------------------------------------------------------------
  
  private Object _fromInputStream(InputStream is1, Cipher cipher, boolean decompress) throws JSONException {
    is2 = (cipher   == null) ? is1 : new CipherInputStream(is1, cipher);
    is3  = (!decompress    ) ? is2 : new InflaterInputStream(is2);
    Object o = deserialize();
    try {
      is3.close();
      is2.close();
      is1.close();
    } catch (IOException e) { error(e.toString()); }
    return o;
  }
  
  private String[] locateError() {
    if (!bis.markSupported())  return new String[] { "", "" }; 
    int av = bis.available();
    bis.reset();
    
    ByteArrayOutputStream bos1 = new ByteArrayOutputStream();
    while (bis.available() > av+1)  bos1.write(bis.read());
    
    ByteArrayOutputStream bos2 = new ByteArrayOutputStream();
    while (bis.available() > 0)  bos2.write(bis.read());
    
    try {
      return new String[] { bos1.toString("UTF-8"), bos2.toString("UTF-8") };
    } catch (UnsupportedEncodingException e) {
      return new String[] { e.toString(), "" };
    }
  }
  
  // ------------------------------------------------------------------------------------------------------------------
  
  @Override protected Object error(String text) throws JSONException {
    throw new JSONException(text, locateError());
  }
  
  @Override protected int getBytes(byte[] b) throws JSONException {
    try {
      return is3.read(b);
    } catch (IOException e) { error(e.toString()); return 0; }
  }
}