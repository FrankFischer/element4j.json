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
import java.nio.charset.Charset;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Deflater;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.IOException;

import javax.crypto.CipherOutputStream;
import javax.crypto.Cipher;
/**
 * Serializes a JSON structure to a byte array (UTF-8 encoded) or string.
 * 
 * @author  Frank Fischer
 * @since   2011
 * @version 0.1.20111010_0755
 */
public class JSONSerializer extends AbstractJSONSerializer {
  
  private static final Charset               utf8     = Charset.forName("UTF-8");
  private static       JSONSerializer        instance = null;
  private static       ByteArrayOutputStream bos;
  private              OutputStream          os2, os3;
  
  private JSONSerializer() { }
  
  private static void init() {
    instance = new JSONSerializer();
  }
  
  // -----------------------------------------------------------------------------------------------
  
  public static String toString (Object o) {
    return new String(toByteArray(o), utf8);
  }
  
  public static byte[] toByteArray (Object o) {
    return toByteArray(o, null, Deflater.NO_COMPRESSION);
  }
  
  public static byte[] toByteArray (Object o, Cipher cipher, int compressionLevel) { 
    bos = new ByteArrayOutputStream();
    toOutputStream(bos, o, cipher, compressionLevel);
    return bos.toByteArray();
  }
  
  public static void toOutputStream(OutputStream os, Object o) {
    toOutputStream(os, o, null, Deflater.NO_COMPRESSION);
  }
  
  public static void toOutputStream(OutputStream os, Object o, Cipher cipher, int compressionLevel) {
    if (instance == null)  init();
    instance._toOutputStream(os, o, cipher, compressionLevel);
  }
  
  // -----------------------------------------------------------------------------------------------
  
  private void  _toOutputStream(OutputStream os1, Object o, Cipher cipher, int compressionLevel) {
    
    os2 = (cipher           == null                   ) ? os1 : new CipherOutputStream(os1, cipher);
    os3 = (compressionLevel == Deflater.NO_COMPRESSION) ? os2 : new DeflaterOutputStream(os2, new Deflater(compressionLevel));
    
    serialize(o);
    
    try {
      os3.close();
      os2.close();
      os1.close();
    } catch (IOException e) { error(e.toString()); }
  }
  
  // -----------------------------------------------------------------------------------------------
  
  @Override protected Object error(String text) {
    throw new RuntimeException(text + " after " + ((bos == null) ? "???" : new String(toByteArray(bos.toByteArray()), utf8)));
  }
  
  @Override protected void write(int b) {
    try {
      os3.write(b);
    } catch (IOException e) { error(e.toString()); }
  }
}